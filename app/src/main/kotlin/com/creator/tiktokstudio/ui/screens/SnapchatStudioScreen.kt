package com.creator.tiktokstudio.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.creator.tiktokstudio.core.SnapchatService
import com.creator.tiktokstudio.data.*
import com.creator.tiktokstudio.ui.components.*
import com.creator.tiktokstudio.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun buildSnapchatImageRequest(context: Context, url: String): ImageRequest {
    val cleanUrl = url.replace("&amp;", "&")
    return ImageRequest.Builder(context)
        .data(cleanUrl)
        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
        .addHeader("Referer", "https://www.snapchat.com/")
        .crossfade(true)
        .build()
}

val SnapchatGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFFC00), // Electric Yellow
        Color(0xFFFFDF00),
        Color(0xFFFFB800), // Amber Gold
        Color(0xFFF59E0B)
    )
)

@Composable
fun SnapchatStudioScreen(
    modifier: Modifier = Modifier
) {
    val strings = Localization.current
    val coroutineScope = rememberCoroutineScope()
    val snapPagerState = rememberPagerState(pageCount = { 2 })

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Tab Selector with Fluid Spring Glass Pill Indicator
        SpringPillTabBar(
            selectedTab = snapPagerState.currentPage,
            onTabSelected = { page ->
                coroutineScope.launch {
                    snapPagerState.animateScrollToPage(page)
                }
            },
            tabs = listOf(
                strings.snapTabDownloader to Icons.Default.Download,
                strings.snapTabInspector to Icons.Default.Search
            ),
            activeGradient = SnapchatGradient,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Swipeable Pages
        HorizontalPager(
            state = snapPagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> SnapchatDownloaderSection()
                1 -> SnapchatInspectorSection()
            }
        }
    }
}

// ==============================================================================
// 1. Snapchat Downloader Section (Spotlight & Public Stories)
// ==============================================================================
@Composable
fun SnapchatDownloaderSection() {
    val context = LocalContext.current
    val strings = Localization.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    var inputUrl by remember { mutableStateOf("") }
    var detectedClipboard by remember { mutableStateOf<String?>(null) }
    var isExtracting by remember { mutableStateOf(false) }
    var showProcessingBox by remember { mutableStateOf(false) }
    var extractedPost by remember { mutableStateOf<SnapchatPostInfo?>(null) }
    var downloadProgress by remember { mutableStateOf(DownloadProgressState()) }

    // Auto clipboard detection
    LaunchedEffect(Unit) {
        val clip = clipboardManager.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()?.trim().orEmpty()
            if (text.isNotBlank() && (text.contains("snapchat.com") || text.contains("t.snapchat.com")) && text != inputUrl) {
                detectedClipboard = text
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Clipboard Pill
        if (detectedClipboard != null) {
            item {
                GlassCard(
                    borderColor = Color(0xFFFFFC00).copy(alpha = 0.6f),
                    backgroundColor = Color(0x33FFB800)
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
                            RealSnapchatIcon(size = 20.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تم رصد رابط سناب شات في الحافظة",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = {
                                inputUrl = detectedClipboard ?: ""
                                detectedClipboard = null
                            },
                            modifier = Modifier.bounceClick(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("لصق وتحميل", fontSize = 11.sp, color = Color(0xFF111111), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Input Floating Search Pill
        item {
            FloatingSearchPill(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                placeholder = strings.snapInputPlaceholderDownloader,
                label = "رابط سناب أضواء أو قصة عامة (Spotlight / Story)",
                actionButtonText = strings.snapFetchBtn,
                onActionClick = {
                    if (inputUrl.isBlank()) {
                        Toast.makeText(context, "الرجاء إدخال أو لصق رابط سناب شات", Toast.LENGTH_SHORT).show()
                        return@FloatingSearchPill
                    }
                    isExtracting = true
                    showProcessingBox = true
                    extractedPost = null

                    coroutineScope.launch {
                        val res = SnapchatService.extractVideoOrStory(inputUrl)
                        delay(2500L)
                        isExtracting = false
                        res.onSuccess { post ->
                            extractedPost = post
                            Toast.makeText(context, "تم استخراج السناب بنجاح! ⚡", Toast.LENGTH_SHORT).show()
                        }.onFailure { err ->
                            Toast.makeText(context, err.message ?: strings.snapExtractErrorFallback, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onPasteClick = {
                    val clip = clipboardManager.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        inputUrl = clip.getItemAt(0).text?.toString()?.trim().orEmpty()
                    }
                },
                onClearClick = { inputUrl = "" },
                isLoading = isExtracting,
                activeGradient = SnapchatGradient,
                accentColor = Color(0xFFFFB800),
                leadingIcon = Icons.Default.Download
            )
        }

        // 3-Second Processing Box
        if (showProcessingBox && isExtracting) {
            item {
                ProcessingStepsBox(
                    modifier = Modifier.fillMaxWidth(),
                    targetDurationMs = 3000L,
                    onCompleted = {
                        showProcessingBox = false
                    }
                )
            }
        }

        // Extracted Post Result Card
        if (extractedPost != null) {
            val post = extractedPost!!
            item {
                GlassCard(
                    borderColor = Color(0xFFFFFC00).copy(alpha = 0.5f),
                    backgroundColor = DarkSurfaceCard
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Author Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (post.authorAvatar.isNotBlank()) {
                                AsyncImage(
                                    model = buildSnapchatImageRequest(context, post.authorAvatar),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, Color(0xFFFFFC00), CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                RealSnapchatIcon(size = 44.dp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = post.authorDisplayName.ifBlank { "Snapchat Creator" },
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                                if (post.authorUsername.isNotBlank()) {
                                    Text(
                                        text = "@${post.authorUsername}",
                                        color = Color(0xFFFFFC00),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            // Spotlight Pill Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x33FFFC00))
                                    .border(1.dp, Color(0xFFFFFC00).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = strings.snapSpotlightBadge,
                                    color = Color(0xFFFFFC00),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Title / Description
                        if (post.title.isNotBlank() || post.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = post.description.ifBlank { post.title },
                                color = TextPrimary,
                                fontSize = 13.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Media Preview
                        if (post.thumbnailUrl.isNotBlank() || post.videoUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = buildSnapchatImageRequest(context, post.thumbnailUrl.ifBlank { post.videoUrl }),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (post.isVideo) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .border(1.5.dp, Color(0xFFFFFC00), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color(0xFFFFFC00),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Statistics Row
                        if (post.likesCount > 0 || post.commentsCount > 0 || post.sharesCount > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x22FFFFFF))
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                SnapStatPill(icon = "❤️", label = "إعجابات", value = formatSnapCount(post.likesCount))
                                SnapStatPill(icon = "💬", label = "تعليقات", value = formatSnapCount(post.commentsCount))
                                SnapStatPill(icon = "↗️", label = "مشاركات", value = formatSnapCount(post.sharesCount))
                            }
                        }

                        // Download Progress
                        if (downloadProgress.isDownloading) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFFFFFC00),
                                trackColor = Color(0x33FFFFFF)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${(downloadProgress.progress * 100).toInt()}%",
                                    color = Color(0xFFFFFC00),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "%.2f MB/s".format(downloadProgress.speedMBs),
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Download Button
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (downloadProgress.isDownloading) return@Button
                                val mediaToDownload = post.videoUrl.ifBlank { post.thumbnailUrl }
                                if (mediaToDownload.isBlank()) {
                                    Toast.makeText(context, "رابط الوسائط غير متوفر", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                coroutineScope.launch {
                                    Toast.makeText(context, "بدأ تحميل وسائط سناب شات...", Toast.LENGTH_SHORT).show()
                                    val dlRes = SnapchatService.downloadMediaFile(
                                        context = context,
                                        url = mediaToDownload,
                                        isVideo = post.isVideo,
                                        title = post.title.ifBlank { "Snapchat_${post.id}" }
                                    ) { progress ->
                                        downloadProgress = progress
                                    }
                                    dlRes.onSuccess {
                                        Toast.makeText(context, strings.snapDownloadSuccessToast, Toast.LENGTH_LONG).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "فشل التحميل: ${err.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .bounceClick(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(SnapchatGradient, RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFFFF9C4), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = null,
                                        tint = Color(0xFF111111),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.snapDownloadMediaBtn,
                                        color = Color(0xFF111111),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 2. Snapchat Inspector Section (OSINT, Stories & Highlights)
// ==============================================================================
@Composable
fun SnapchatInspectorSection() {
    val context = LocalContext.current
    val strings = Localization.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    var usernameInput by remember { mutableStateOf("") }
    var isInspecting by remember { mutableStateOf(false) }
    var showProcessingBox by remember { mutableStateOf(false) }
    var inspectedProfile by remember { mutableStateOf<SnapchatProfileInfo?>(null) }
    var avatarDownloadProgress by remember { mutableStateOf(DownloadProgressState()) }

    fun triggerInspect(targetUsername: String) {
        val clean = targetUsername.trim()
        if (clean.isBlank()) {
            Toast.makeText(context, "الرجاء إدخال اسم المستخدم أو الرابط", Toast.LENGTH_SHORT).show()
            return
        }
        usernameInput = clean
        isInspecting = true
        showProcessingBox = true
        inspectedProfile = null

        coroutineScope.launch {
            val res = SnapchatService.inspectProfile(clean)
            delay(2500L)
            isInspecting = false
            res.onSuccess { prof ->
                inspectedProfile = prof
                Toast.makeText(context, "تم فحص حساب سناب شات بنجاح! 👻", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(context, err.message ?: "تعذر جلب بيانات الحساب", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Floating Search Pill for Snap Inspector
        item {
            FloatingSearchPill(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                placeholder = strings.snapInputPlaceholderInspector,
                label = "فحص حسابات سناب شات المباشر باليوزر (@username)",
                actionButtonText = strings.snapInspectBtn,
                onActionClick = { triggerInspect(usernameInput) },
                onPasteClick = {
                    val clip = clipboardManager.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        usernameInput = clip.getItemAt(0).text?.toString()?.trim().orEmpty()
                    }
                },
                onClearClick = { usernameInput = "" },
                isLoading = isInspecting,
                activeGradient = SnapchatGradient,
                accentColor = Color(0xFFFFB800),
                leadingIcon = Icons.Default.Search
            )
        }

        // 3-Second Processing Box
        if (showProcessingBox && isInspecting) {
            item {
                ProcessingStepsBox(
                    modifier = Modifier.fillMaxWidth(),
                    targetDurationMs = 3000L,
                    onCompleted = {
                        showProcessingBox = false
                    }
                )
            }
        }

        // Inspected Profile Header Card
        if (inspectedProfile != null) {
            val prof = inspectedProfile!!
            item {
                GlassCard(
                    borderColor = Color(0xFFFFFC00).copy(alpha = 0.5f),
                    backgroundColor = DarkSurfaceCard
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile Avatar / Bitmoji with Golden Glow
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A))
                                .border(
                                    BorderStroke(
                                        2.5.dp,
                                        Brush.linearGradient(
                                            listOf(Color(0xFFFFFC00), Color(0xFFFFB800), Color(0xFFF59E0B))
                                        )
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (prof.profilePictureUrl.isNotBlank() || prof.bitmojiAvatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = buildSnapchatImageRequest(
                                        context,
                                        prof.profilePictureUrl.ifBlank { prof.bitmojiAvatarUrl }
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                RealSnapchatIcon(size = 64.dp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Display Name & Official Verified Star
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = prof.displayName.ifBlank { prof.username },
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                fontSize = 17.sp
                            )
                            if (prof.isVerified) {
                                Spacer(modifier = Modifier.width(6.dp))
                                RealSnapchatStarBadge(size = 18.dp)
                            }
                        }

                        // Username Pill
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "@${prof.username}",
                            color = Color(0xFFFFFC00),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Category & Location Badges
                        if (prof.categoryStringId.isNotBlank() || prof.address.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (prof.categoryStringId.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0x33FFB800))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = prof.categoryStringId,
                                            color = Color(0xFFFFB800),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                if (prof.address.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0x22FFFFFF))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "📍 ${prof.address}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Bio
                        if (prof.bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = prof.bio,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }

                        // Website Link
                        if (prof.websiteUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "🔗 ${prof.websiteUrl}",
                                color = Color(0xFF00F2FE),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Download Avatar & Snapcode Action Buttons
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // HD Avatar Download
                            Button(
                                onClick = {
                                    val picUrl = prof.profilePictureUrl.ifBlank { prof.bitmojiAvatarUrl }
                                    if (picUrl.isBlank()) {
                                        Toast.makeText(context, "رابط الصورة الشخصية غير متوفر", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    coroutineScope.launch {
                                        Toast.makeText(context, "جاري حفظ الصورة الشخصية...", Toast.LENGTH_SHORT).show()
                                        val res = SnapchatService.downloadMediaFile(
                                            context = context,
                                            url = picUrl,
                                            isVideo = false,
                                            title = "${prof.username}_Avatar"
                                        ) { avatarDownloadProgress = it }
                                        res.onSuccess {
                                            Toast.makeText(context, "تم حفظ الصورة الشخصية في الاستوديو بنجاح! 👤", Toast.LENGTH_SHORT).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, "فشل التحميل: ${err.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .bounceClick(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFC00)),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFFC00).copy(alpha = 0.5f))
                            ) {
                                Text("صورة البروفايل HD", fontSize = 11.sp, color = Color(0xFFFFFC00), fontWeight = FontWeight.Bold)
                            }

                            // Snapcode SVG Download
                            Button(
                                onClick = {
                                    if (prof.snapcodeImageUrl.isBlank()) {
                                        Toast.makeText(context, "رابط السناب كود غير متوفر", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    coroutineScope.launch {
                                        Toast.makeText(context, "جاري حفظ رمز Snapcode...", Toast.LENGTH_SHORT).show()
                                        val res = SnapchatService.downloadMediaFile(
                                            context = context,
                                            url = prof.snapcodeImageUrl,
                                            isVideo = false,
                                            title = "${prof.username}_Snapcode"
                                        ) { }
                                        res.onSuccess {
                                            Toast.makeText(context, "تم حفظ رمز السناب كود بنجاح! 🟨", Toast.LENGTH_SHORT).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, "فشل التحميل: ${err.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .bounceClick(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFB800)),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.5f))
                            ) {
                                Text("رمز Snapcode 🟨", fontSize = 11.sp, color = Color(0xFFFFB800), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Stats Bento Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SnapBentoMetric(
                        modifier = Modifier.weight(1f),
                        title = strings.snapSubscribersLabel,
                        value = formatSnapCount(prof.subscriberCount),
                        badge = if (prof.isVerified) "⭐ موثق" else "عام",
                        accentColor = Color(0xFFFFFC00)
                    )
                    SnapBentoMetric(
                        modifier = Modifier.weight(1f),
                        title = strings.snapStoriesCountLabel,
                        value = "${prof.publicStories.size}",
                        badge = "⏱️ نشطة",
                        accentColor = Color(0xFF00F2FE)
                    )
                    SnapBentoMetric(
                        modifier = Modifier.weight(1f),
                        title = strings.snapHighlightsCountLabel,
                        value = "${prof.curatedHighlights.size + prof.spotlightHighlights.size}",
                        badge = "⚡ أضواء",
                        accentColor = Color(0xFFFFB800)
                    )
                }
            }

            // Public Stories Section
            if (prof.publicStories.isNotEmpty()) {
                item {
                    Text(
                        text = strings.snapPublicStoriesTitle,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(prof.publicStories) { storyItem ->
                            SnapMediaCard(
                                item = storyItem,
                                onDownload = {
                                    coroutineScope.launch {
                                        Toast.makeText(context, "جاري تحميل القصة...", Toast.LENGTH_SHORT).show()
                                        val res = SnapchatService.downloadMediaFile(
                                            context = context,
                                            url = storyItem.mediaUrl,
                                            isVideo = storyItem.isVideo,
                                            title = "${prof.username}_Story_${storyItem.id}"
                                        ) {}
                                        res.onSuccess {
                                            Toast.makeText(context, "تم حفظ القصة بنجاح! ⏱️", Toast.LENGTH_SHORT).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, "فشل التحميل: ${err.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Spotlight Snaps Section
            if (prof.spotlightHighlights.isNotEmpty()) {
                item {
                    Text(
                        text = strings.snapSpotlightTitle,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(prof.spotlightHighlights) { spItem ->
                            SnapMediaCard(
                                item = spItem,
                                onDownload = {
                                    coroutineScope.launch {
                                        Toast.makeText(context, "جاري تحميل سناب الأضواء...", Toast.LENGTH_SHORT).show()
                                        val res = SnapchatService.downloadMediaFile(
                                            context = context,
                                            url = spItem.mediaUrl,
                                            isVideo = spItem.isVideo,
                                            title = "${prof.username}_Spotlight_${spItem.id}"
                                        ) {}
                                        res.onSuccess {
                                            Toast.makeText(context, "تم حفظ سناب الأضواء بنجاح! ⚡", Toast.LENGTH_SHORT).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, "فشل التحميل: ${err.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Curated Highlights Section
            if (prof.curatedHighlights.isNotEmpty()) {
                item {
                    Text(
                        text = strings.snapHighlightsTitle,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(prof.curatedHighlights) { hlItem ->
                            SnapMediaCard(
                                item = hlItem,
                                onDownload = {
                                    coroutineScope.launch {
                                        Toast.makeText(context, "جاري تحميل المقتطف المميز...", Toast.LENGTH_SHORT).show()
                                        val res = SnapchatService.downloadMediaFile(
                                            context = context,
                                            url = hlItem.mediaUrl,
                                            isVideo = hlItem.isVideo,
                                            title = "${prof.username}_Highlight_${hlItem.id}"
                                        ) {}
                                        res.onSuccess {
                                            Toast.makeText(context, "تم حفظ المقتطف بنجاح! ⭐", Toast.LENGTH_SHORT).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, "فشل التحميل: ${err.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 3. Components & Widgets for Snapchat Studio
// ==============================================================================
@Composable
fun SnapBentoMetric(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    badge: String,
    accentColor: Color
) {
    GlassCard(
        modifier = modifier,
        borderColor = accentColor.copy(alpha = 0.4f),
        backgroundColor = DarkSurfaceCard,
        cornerRadius = 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 10.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SnapMediaCard(
    item: SnapchatMediaItem,
    onDownload: () -> Unit
) {
    val context = LocalContext.current

    GlassCard(
        modifier = Modifier
            .width(160.dp)
            .height(260.dp),
        borderColor = Color(0xFFFFFC00).copy(alpha = 0.35f),
        backgroundColor = DarkSurfaceCard,
        cornerRadius = 14.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = buildSnapchatImageRequest(context, item.thumbnailUrl.ifBlank { item.mediaUrl }),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (item.isVideo) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFFFFFC00),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                // Story Type Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (item.storyType == "Spotlight") "⚡ أضواء" else if (item.storyType == "Story") "⏱️ قصة" else "⭐ مقتطف",
                        color = Color(0xFFFFFC00),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Download Button
            Button(
                onClick = onDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .bounceClick(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFFC00)),
                contentPadding = PaddingValues(horizontal = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        tint = Color(0xFF111111),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "تحميل",
                        color = Color(0xFF111111),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

fun formatSnapCount(count: Long): String {
    return when {
        count >= 1_000_000_000 -> "%.1fB".format(count / 1_000_000_000.0)
        count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0)
        count >= 1_000 -> "%.1fK".format(count / 1_000.0)
        else -> "$count"
    }
}

@Composable
fun SnapStatPill(
    icon: String,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = "$icon $value", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(text = label, color = TextSecondary, fontSize = 10.sp)
    }
}
