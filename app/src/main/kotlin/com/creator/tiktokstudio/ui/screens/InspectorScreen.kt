package com.creator.tiktokstudio.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.creator.tiktokstudio.core.*
import com.creator.tiktokstudio.data.*
import com.creator.tiktokstudio.ui.components.ProcessingStepsBox
import com.creator.tiktokstudio.ui.components.CircularDownloadProgress
import com.creator.tiktokstudio.ui.components.GlassCard
import com.creator.tiktokstudio.ui.components.NeonBadge
import com.creator.tiktokstudio.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun TikTokContentType.getLocalizedTitle(strings: AppStrings): String = when (this) {
    TikTokContentType.VIDEO -> strings.typeVideo
    TikTokContentType.PHOTO_ALBUM -> strings.typePhotos
    TikTokContentType.STORY -> strings.typeStory
    TikTokContentType.PROFILE -> strings.typeProfile
}

@Composable
fun InspectorScreen(
    modifier: Modifier = Modifier,
    initialUrl: String? = null
) {
    val context = LocalContext.current
    val strings = Localization.current
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    val coroutineScope = rememberCoroutineScope()

    var urlInput by remember { mutableStateOf(initialUrl ?: "") }
    var detectedClipboardUrl by remember { mutableStateOf<String?>(null) }
    var isInspecting by remember { mutableStateOf(false) }
    var inspectionResult by remember { mutableStateOf<TikTokDeepInspectionData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf(DownloadProgressState()) }

    // Auto-clipboard detection
    LaunchedEffect(Unit) {
        val clip = clipboardManager.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()?.trim() ?: ""
            if (text.isNotBlank() && (text.contains("tiktok.com") || text.startsWith("@") || (!text.contains(" ") && text.length in 3..30)) && text != urlInput) {
                detectedClipboardUrl = text
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Banner
        item {
            GlassCard(
                borderColor = NeonCyan.copy(alpha = 0.5f),
                backgroundColor = DarkSurfaceCard
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(NeonCyan, NeonPurple))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = OnAccentDark,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = strings.inspectorHeaderTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = strings.inspectorHeaderSubtitle,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }
            }
        }

        // Clipboard Catch Banner
        if (detectedClipboardUrl != null) {
            item {
                AnimatedVisibility(
                    visible = detectedClipboardUrl != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    GlassCard(
                        borderColor = NeonPink.copy(alpha = 0.7f),
                        backgroundColor = Color(0x28FE0979)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    tint = NeonPink,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = strings.clipboardDetectedTitle,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = detectedClipboardUrl!!,
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    urlInput = detectedClipboardUrl!!
                                    detectedClipboardUrl = null
                                    val normalized = InputUtils.normalizeTikTokInput(urlInput, InputTargetType.INSPECTOR)
                                    isInspecting = true
                                    inspectionResult = null
                                    errorMessage = null
                                    coroutineScope.launch {
                                        val start = System.currentTimeMillis()
                                        val res = TikTokDownloaderService.inspectTikTokUrl(normalized)
                                        val elapsed = System.currentTimeMillis() - start
                                        if (elapsed < 3000L) {
                                            delay(3000L - elapsed)
                                        }
                                        delay(250L)
                                        isInspecting = false
                                        res.onSuccess {
                                            inspectionResult = it
                                            errorMessage = null
                                            Toast.makeText(context, strings.inspectionSuccessToast, Toast.LENGTH_SHORT).show()
                                        }.onFailure {
                                            val rawMsg = it.localizedMessage ?: ""
                                            errorMessage = when {
                                                rawMsg.contains("USER_NOT_FOUND") || rawMsg.contains("غير موجود") || rawMsg.contains("not found") -> strings.userNotFoundOrPrivate
                                                rawMsg.isNotBlank() -> rawMsg
                                                else -> strings.inspectErrorFallback
                                            }
                                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(strings.instantInspectBtn, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Search Input Box
        item {
            GlassCard {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        if (errorMessage != null) errorMessage = null
                    },
                    placeholder = {
                        Text(
                            strings.inspectorInputPlaceholder,
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (urlInput.isNotEmpty()) {
                                IconButton(onClick = { urlInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = strings.clearBtnTooltip, tint = TextMuted)
                                }
                            }
                            IconButton(onClick = {
                                val clip = clipboardManager.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString() ?: ""
                                    if (text.isNotBlank()) urlInput = text
                                }
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = strings.pasteBtnTooltip, tint = NeonCyan)
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (urlInput.isBlank()) {
                            Toast.makeText(context, strings.emptyUrlToast, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val normalized = InputUtils.normalizeTikTokInput(urlInput, InputTargetType.INSPECTOR)
                        isInspecting = true
                        inspectionResult = null
                        errorMessage = null
                        coroutineScope.launch {
                            val start = System.currentTimeMillis()
                            val res = TikTokDownloaderService.inspectTikTokUrl(normalized)
                            val elapsed = System.currentTimeMillis() - start
                            if (elapsed < 3000L) {
                                delay(3000L - elapsed)
                            }
                            delay(250L)
                            isInspecting = false
                            res.onSuccess {
                                inspectionResult = it
                                errorMessage = null
                                Toast.makeText(context, strings.inspectionSuccessToast, Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                val rawMsg = it.localizedMessage ?: ""
                                errorMessage = when {
                                    rawMsg.contains("USER_NOT_FOUND") || rawMsg.contains("غير موجود") || rawMsg.contains("not found") -> strings.userNotFoundOrPrivate
                                    rawMsg.isNotBlank() -> rawMsg
                                    else -> strings.inspectErrorFallback
                                }
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan
                    ),
                    enabled = !isInspecting
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = OnAccentDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        strings.startDeepInspectionBtn,
                        color = OnAccentDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Live Animated Algorithm Execution Box (Active during Inspection)
        if (isInspecting) {
            item {
                AnimatedVisibility(
                    visible = isInspecting,
                    enter = fadeIn(tween(400)) + expandVertically(),
                    exit = fadeOut(tween(300)) + shrinkVertically()
                ) {
                    ProcessingStepsBox(
                        modifier = Modifier.fillMaxWidth(),
                        targetDurationMs = 3000L
                    )
                }
            }
        }

        // Error Card (if any real failure occurs)
        if (errorMessage != null && !isInspecting) {
            item {
                GlassCard(
                    borderColor = ErrorRed.copy(alpha = 0.8f),
                    backgroundColor = Color(0x33FF3D00)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.inspectionAlertTitle,
                                style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }
                }
            }
        }

        // Active Download Progress indicator
        if (downloadProgress.isDownloading) {
            item {
                GlassCard(borderColor = NeonCyan) {
                    CircularDownloadProgress(
                        progress = downloadProgress.progress,
                        speedMBs = downloadProgress.speedMBs,
                        downloadedBytes = downloadProgress.downloadedBytes,
                        totalBytes = downloadProgress.totalBytes
                    )
                    Text(
                        text = strings.savingWatermarkFreeText,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // RESULT VIEW
        if (inspectionResult != null && !isInspecting) {
            val data = inspectionResult!!

            // 1. Author & Content Header Card
            item {
                GlassCard(
                    borderColor = NeonCyan.copy(alpha = 0.6f),
                    backgroundColor = DarkSurfaceCard
                ) {
                    // Content Type Badge Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeonBadge(
                            text = "${data.contentType.badgeIcon} ${data.contentType.getLocalizedTitle(strings)}",
                            textColor = if (data.contentType == TikTokContentType.STORY) NeonPink else NeonCyan,
                            borderColor = if (data.contentType == TikTokContentType.STORY) NeonPink else NeonCyan
                        )
                        if (data.isVerified) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.verifiedShort, color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Profile Row with Coil Image Loader
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                                .border(2.dp, Brush.linearGradient(listOf(NeonCyan, NeonPink)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(36.dp)
                            )
                            if (data.authorAvatar.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(data.authorAvatar)
                                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36")
                                        .addHeader("Referer", "https://www.tiktok.com/")
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = data.authorName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = data.authorName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "@${data.authorUniqueId}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = NeonPink)
                            )
                            if (data.authorBio.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = data.authorBio,
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Save Avatar Button
                        if (data.authorAvatar.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        Toast.makeText(context, strings.savingAvatarToast, Toast.LENGTH_SHORT).show()
                                        val saveRes = TikTokDownloaderService.downloadImageFile(
                                            context = context,
                                            downloadUrl = data.authorAvatar,
                                            title = "avatar_${data.authorUniqueId}",
                                            onProgress = { downloadProgress = it }
                                        )
                                        saveRes.onSuccess {
                                            Toast.makeText(context, strings.avatarSavedSuccessToast, Toast.LENGTH_SHORT).show()
                                        }.onFailure {
                                            Toast.makeText(context, "${strings.downloadFailedToast}: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = strings.saveAvatarTooltip,
                                    tint = NeonCyan
                                )
                            }
                        }
                    }

                    // Profile Stats (if Profile or followers present)
                    if (data.followerCount > 0 || data.contentType == TikTokContentType.PROFILE) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceElevated)
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatBox(label = strings.followersLabel, value = formatCounter(data.followerCount))
                            StatBox(label = strings.followingLabel, value = formatCounter(data.followingCount))
                            StatBox(label = strings.totalLikesLabel, value = formatCounter(data.authorTotalLikes))
                            StatBox(label = strings.videosCountLabel, value = formatCounter(data.authorVideoCount))
                        }
                    }
                }
            }

            // Cover Image Preview (if present)
            if (data.thumbnailUrl.isNotBlank() && data.contentType != TikTokContentType.PROFILE) {
                item {
                    GlassCard(
                        borderColor = NeonCyan.copy(alpha = 0.35f),
                        backgroundColor = DarkSurfaceCard
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(data.thumbnailUrl)
                                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36")
                                    .addHeader("Referer", "https://www.tiktok.com/")
                                    .crossfade(true)
                                    .build(),
                                contentDescription = data.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Content type badge overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xCC0F0F13))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${data.contentType.badgeIcon} ${data.contentType.getLocalizedTitle(strings)}",
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 2. Title & Caption Card
            if (data.title.isNotBlank() && data.contentType != TikTokContentType.PROFILE) {
                item {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.captionLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(color = NeonCyan)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = data.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clip = ClipData.newPlainText("TikTok Title", data.title)
                                    clipboardManager.setPrimaryClip(clip)
                                    Toast.makeText(context, strings.captionCopiedToast, Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = strings.copyCaptionTooltip, tint = TextMuted)
                            }
                        }
                    }
                }
            }

            // 3. Bento Grid of Media Specs & Real Metrics
            if (data.contentType != TikTokContentType.PROFILE) {
                item {
                    Text(
                        text = strings.realAnalyticsHeader,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Bento Row 1: Published Date Card (Full Width)
                item {
                    GlassCard(
                        borderColor = NeonPink.copy(alpha = 0.4f),
                        backgroundColor = DarkSurfaceCard
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = NeonPink, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = strings.publishDateLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                )
                                Text(
                                    text = data.createTimeFormatted,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                // Bento Row 2: Views, Likes, Comments, Saves (2x2 Grid)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Visibility,
                            value = formatCounter(data.viewsCount),
                            label = strings.viewsLabel,
                            accentColor = NeonCyan
                        )
                        BentoMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Favorite,
                            value = formatCounter(data.likesCount),
                            label = strings.likesLabel,
                            accentColor = NeonPink
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.ChatBubbleOutline,
                            value = formatCounter(data.commentsCount),
                            label = strings.commentsLabel,
                            accentColor = NeonPurple
                        )
                        BentoMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.BookmarkBorder,
                            value = formatCounter(data.collectCount),
                            label = strings.bookmarksLabel,
                            accentColor = GoldVip
                        )
                    }
                }

                // Bento Row 3: Resolution & Size
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.HighQuality,
                            value = data.resolution,
                            label = strings.resolutionLabel,
                            accentColor = NeonCyan
                        )
                        BentoMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.SdCard,
                            value = if (data.fileSizeMB > 0) "${data.fileSizeMB} MB | ${data.durationSec} ${strings.secondsSuffix}" else "${data.durationSec} ${strings.secondsSuffix}",
                            label = strings.sizeDurationLabel,
                            accentColor = SuccessGreen
                        )
                    }
                }
            }

            // 4. Photo Album Section (if photos present)
            if (data.photos.isNotEmpty()) {
                item {
                    GlassCard(borderColor = NeonCyan) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.photoAlbumHeader} (${data.photos.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            )
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        Toast.makeText(context, strings.savingAllPhotosToast, Toast.LENGTH_SHORT).show()
                                        for (photo in data.photos) {
                                            TikTokDownloaderService.downloadImageFile(
                                                context = context,
                                                downloadUrl = photo.url,
                                                title = "photo_${data.authorUniqueId}_${photo.index}",
                                                onProgress = { downloadProgress = it }
                                            )
                                        }
                                        Toast.makeText(context, strings.allPhotosSavedToast, Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(strings.downloadAllPhotosBtn, color = OnAccentDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(data.photos, key = { it.index }) { photo ->
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DarkSurfaceElevated)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(photo.url)
                                            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36")
                                            .addHeader("Referer", "https://www.tiktok.com/")
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "${strings.photoAlbumHeader} ${photo.index}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xCC0F0F13))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "#${photo.index}",
                                            color = TextPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Quick Download Actions (for Video, Story, or Audio)
            if (data.contentType != TikTokContentType.PROFILE) {
                item {
                    GlassCard(borderColor = NeonCyan.copy(alpha = 0.5f)) {
                        Text(
                            text = strings.pureMediaDownloadTitle,
                            style = MaterialTheme.typography.labelMedium.copy(color = NeonCyan),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        // Video / Story Download Button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val targetUrl = if (data.videoHdUrl.isNotBlank()) data.videoHdUrl else data.videoSdUrl
                                    if (targetUrl.isBlank()) {
                                        Toast.makeText(context, strings.downloadFailedToast, Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                                    Toast.makeText(context, strings.downloadInProgressTitle, Toast.LENGTH_SHORT).show()
                                    val dlRes = TikTokDownloaderService.downloadMediaFile(
                                        context = context,
                                        downloadUrl = targetUrl,
                                        title = data.title.ifBlank { "TikTok_${data.authorUniqueId}" },
                                        isVideo = true,
                                        onProgress = { downloadProgress = it }
                                    )
                                    dlRes.onSuccess {
                                        Toast.makeText(context, strings.saveVideoSuccessToast, Toast.LENGTH_LONG).show()
                                    }.onFailure {
                                        Toast.makeText(context, "${strings.downloadFailedToast}: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = OnAccentDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (data.contentType == TikTokContentType.STORY) strings.downloadStoryBtn else strings.downloadVideoFhdBtn,
                                color = OnAccentDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Audio Download Button (if audioUrl present)
                        if (data.audioUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        Toast.makeText(context, strings.savingAudioToast, Toast.LENGTH_SHORT).show()
                                        val dlRes = TikTokDownloaderService.downloadMediaFile(
                                            context = context,
                                            downloadUrl = data.audioUrl,
                                            title = "${data.audioTitle}_${data.authorUniqueId}",
                                            isVideo = false,
                                            onProgress = { downloadProgress = it }
                                        )
                                        dlRes.onSuccess {
                                            Toast.makeText(context, strings.audioSavedToast, Toast.LENGTH_LONG).show()
                                        }.onFailure {
                                            Toast.makeText(context, "${strings.downloadFailedToast}: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, NeonPink)
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = NeonPink)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    strings.downloadOriginalAudioBtn,
                                    color = NeonPink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // 6. TOP LIKED COMMENTS SECTION
            if (data.topComments.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings.topCommentsHeader,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonPink
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        NeonBadge(text = "${data.topComments.size} ${strings.commentsPillSuffix}", textColor = NeonPink, borderColor = NeonPink)
                    }
                }

                items(data.topComments, key = { it.id }) { comment ->
                    GlassCard(
                        borderColor = GlassBorder,
                        backgroundColor = DarkSurfaceCard
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceElevated)
                                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = comment.authorName.take(1),
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (comment.authorAvatar.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(comment.authorAvatar)
                                            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36")
                                            .addHeader("Referer", "https://www.tiktok.com/")
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = comment.authorName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = comment.authorName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        )
                                        Text(
                                            text = "@${comment.authorUniqueId}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }

                                    // Like counter pill
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NeonPink.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = NeonPink,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = formatCounter(comment.diggCount),
                                            color = NeonPink,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = comment.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                                    fontSize = 13.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (comment.timeAgoArabic.isNotBlank()) {
                                        Text(
                                            text = comment.timeAgoArabic,
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }

                                    // One-tap Copy Comment Button
                                    TextButton(
                                        onClick = {
                                            val clip = ClipData.newPlainText("Comment", comment.text)
                                            clipboardManager.setPrimaryClip(clip)
                                            Toast.makeText(context, strings.commentCopiedToast, Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = strings.copyCommentBtn,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(strings.copyCommentBtn, color = NeonCyan, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (data.contentType != TikTokContentType.PROFILE) {
                item {
                    GlassCard(
                        borderColor = GlassBorder.copy(alpha = 0.3f),
                        backgroundColor = DarkSurfaceCard
                    ) {
                        Text(
                            text = strings.noCommentsFound,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BentoMetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceCard)
            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text = label, color = TextSecondary, fontSize = 10.sp)
    }
}

fun formatCounter(count: Long): String {
    return when {
        count >= 1_000_000_000 -> String.format(java.util.Locale.US, "%.1fB", count / 1_000_000_000.0)
        count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
