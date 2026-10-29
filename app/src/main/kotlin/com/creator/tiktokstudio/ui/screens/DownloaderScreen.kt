package com.creator.tiktokstudio.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.creator.tiktokstudio.core.Localization
import com.creator.tiktokstudio.core.NativeEngine
import com.creator.tiktokstudio.core.TikTokDownloaderService
import com.creator.tiktokstudio.data.DownloadProgressState
import com.creator.tiktokstudio.data.DownloadedMedia
import com.creator.tiktokstudio.data.TikTokVideoInfo
import com.creator.tiktokstudio.ui.components.ProcessingStepsBox
import com.creator.tiktokstudio.ui.components.CircularDownloadProgress
import com.creator.tiktokstudio.ui.components.GlassCard
import com.creator.tiktokstudio.ui.components.NeonBadge
import com.creator.tiktokstudio.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DownloaderScreen(
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
    var isExtracting by remember { mutableStateOf(false) }
    var extractedVideo by remember { mutableStateOf<TikTokVideoInfo?>(null) }
    var downloadProgress by remember { mutableStateOf(DownloadProgressState()) }
    val downloadedHistory = remember { mutableStateListOf<DownloadedMedia>() }

    // Auto-clipboard detection on composition
    LaunchedEffect(Unit) {
        val clip = clipboardManager.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            if (text.contains("tiktok.com") && text != urlInput) {
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
        // Auto-Clipboard Detection Pill
        if (detectedClipboardUrl != null) {
            item {
                AnimatedVisibility(
                    visible = detectedClipboardUrl != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    GlassCard(
                        borderColor = NeonPink.copy(alpha = 0.6f),
                        backgroundColor = Color(0x33FE0979)
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
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = strings.clipboardDetectedTitle,
                                        style = MaterialTheme.typography.labelLarge.copy(color = TextPrimary)
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
                                    isExtracting = true
                                    extractedVideo = null
                                    coroutineScope.launch {
                                        val start = System.currentTimeMillis()
                                        val res = TikTokDownloaderService.extractMediaInfo(urlInput)
                                        val elapsed = System.currentTimeMillis() - start
                                        if (elapsed < 3000L) {
                                            delay(3000L - elapsed)
                                        }
                                        delay(250L)
                                        isExtracting = false
                                        res.onSuccess {
                                            extractedVideo = it
                                            Toast.makeText(context, strings.videoExtractSuccessToast, Toast.LENGTH_SHORT).show()
                                        }.onFailure {
                                            Toast.makeText(context, it.message ?: strings.downloadFailedToast, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(strings.clipboardPasteInspectBtn, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // URL Input and Paste Box
        item {
            GlassCard {
                Text(
                    text = strings.urlInputLabel,
                    style = MaterialTheme.typography.labelLarge.copy(color = NeonCyan),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = {
                        Text(
                            strings.urlInputPlaceholder,
                            color = TextMuted,
                            fontSize = 13.sp
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
                        isExtracting = true
                        extractedVideo = null
                        coroutineScope.launch {
                            val start = System.currentTimeMillis()
                            val res = TikTokDownloaderService.extractMediaInfo(urlInput)
                            val elapsed = System.currentTimeMillis() - start
                            if (elapsed < 3000L) {
                                delay(3000L - elapsed)
                            }
                            delay(250L)
                            isExtracting = false
                            res.onSuccess {
                                extractedVideo = it
                                Toast.makeText(context, strings.videoExtractSuccessToast, Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                Toast.makeText(context, it.message ?: strings.downloadFailedToast, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    enabled = !isExtracting
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = OnAccentDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        strings.fetchVideoBtn,
                        color = OnAccentDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Live Animated Algorithm Execution Box (Active during Extraction)
        if (isExtracting) {
            item {
                AnimatedVisibility(
                    visible = isExtracting,
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

        // Extracted Video Preview Card
        if (extractedVideo != null && !isExtracting) {
            item {
                AnimatedVisibility(
                    visible = extractedVideo != null && !isExtracting,
                    enter = fadeIn(tween(500)) + expandVertically()
                ) {
                    val video = extractedVideo!!
                    GlassCard(
                        borderColor = NeonCyan.copy(alpha = 0.5f),
                        backgroundColor = DarkSurfaceCard
                    ) {
                        Text(
                            text = strings.previewCardTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Author info row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceElevated)
                                    .border(1.dp, NeonCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                                if (video.authorAvatar.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(video.authorAvatar)
                                            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36")
                                            .addHeader("Referer", "https://www.tiktok.com/")
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = video.authorName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.authorName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "@${video.authorId}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = NeonPink)
                                )
                            }

                            NeonBadge(text = strings.noWatermarkBadge, textColor = NeonCyan)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Title
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Cover thumbnail preview
                        if (video.thumbnailUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(video.thumbnailUrl)
                                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36")
                                        .addHeader("Referer", "https://www.tiktok.com/")
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = video.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Metrics Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceElevated)
                                .padding(vertical = 10.dp, horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            MetricItem(icon = Icons.Outlined.Visibility, value = "${video.viewsCount / 1000}K", label = strings.viewsLabel)
                            MetricItem(icon = Icons.Outlined.FavoriteBorder, value = "${video.likesCount / 1000}K", label = strings.likesLabel)
                            MetricItem(icon = Icons.Outlined.ChatBubbleOutline, value = "${video.commentsCount}", label = strings.commentsLabel)
                            MetricItem(icon = Icons.Outlined.Share, value = "${video.sharesCount}", label = strings.sharesLabel)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Download Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Download Full HD Video
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val res = TikTokDownloaderService.downloadMediaFile(
                                            context = context,
                                            downloadUrl = video.videoHdUrl,
                                            title = video.title,
                                            isVideo = true,
                                            onProgress = { downloadProgress = it }
                                        )
                                        res.onSuccess {
                                            downloadedHistory.add(0, it)
                                            Toast.makeText(context, strings.saveVideoSuccessToast, Toast.LENGTH_LONG).show()
                                        }.onFailure {
                                            Toast.makeText(context, "${strings.downloadFailedToast}: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                                enabled = !downloadProgress.isDownloading
                            ) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = TextPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.downloadVideoHdBtn, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            // Download Audio MP3
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val res = TikTokDownloaderService.downloadMediaFile(
                                            context = context,
                                            downloadUrl = video.audioUrl,
                                            title = "${video.title}_Audio",
                                            isVideo = false,
                                            onProgress = { downloadProgress = it }
                                        )
                                        res.onSuccess {
                                            downloadedHistory.add(0, it)
                                            Toast.makeText(context, strings.saveAudioSuccessToast, Toast.LENGTH_LONG).show()
                                        }.onFailure {
                                            Toast.makeText(context, "${strings.downloadFailedToast}: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                                border = BorderStroke(1.dp, NeonCyan),
                                enabled = !downloadProgress.isDownloading
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.downloadAudioMp3Btn, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Circular Download Progress Section
        if (downloadProgress.isDownloading || downloadProgress.isCompleted) {
            item {
                GlassCard(
                    borderColor = if (downloadProgress.isCompleted) SuccessGreen else NeonCyan,
                    backgroundColor = DarkSurfaceCard
                ) {
                    Text(
                        text = if (downloadProgress.isCompleted) strings.downloadCompletedTitle else strings.downloadInProgressTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (downloadProgress.isCompleted) SuccessGreen else NeonCyan
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    CircularDownloadProgress(
                        progress = downloadProgress.progress,
                        speedMBs = downloadProgress.speedMBs,
                        downloadedBytes = downloadProgress.downloadedBytes,
                        totalBytes = downloadProgress.totalBytes
                    )

                    Text(
                        text = downloadProgress.fileName,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Download History Gallery
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.downloadHistoryTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "${downloadedHistory.size} ${strings.filesCountSuffix}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (downloadedHistory.isEmpty()) {
                    GlassCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = strings.noDownloadsYet,
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        items(downloadedHistory) { item ->
            GlassCard(
                borderColor = GlassBorder,
                backgroundColor = DarkSurfaceCard
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (item.isVideo) NeonPink.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.isVideo) Icons.Default.Videocam else Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = if (item.isVideo) NeonPink else NeonCyan
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = NativeEngine.formatBitrate(item.sizeBytes),
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }

                    // Share button
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = if (item.isVideo) "video/mp4" else "audio/mpeg"
                                putExtra(Intent.EXTRA_STREAM, Uri.parse(item.fileUriString))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, strings.shareChooserTitle))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = strings.shareChooserTitle, tint = NeonCyan)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.labelLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp))
    }
}
