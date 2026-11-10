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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.creator.tiktokstudio.core.InstagramService
import com.creator.tiktokstudio.core.Localization
import com.creator.tiktokstudio.data.*
import com.creator.tiktokstudio.ui.components.*
import com.creator.tiktokstudio.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun buildInstagramImageRequest(context: Context, url: String): ImageRequest {
    val cleanUrl = url.replace("&amp;", "&")
    return ImageRequest.Builder(context)
        .data(cleanUrl)
        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
        .addHeader("Referer", "https://www.instagram.com/")
        .crossfade(true)
        .build()
}

val InstagramGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF833AB4), // Purple
        Color(0xFFE1306C), // Deep Pink
        Color(0xFFFD1D1D), // Crimson Red
        Color(0xFFFCB045)  // Warm Orange
    )
)

@Composable
fun InstagramStudioScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = Localization.current
    val coroutineScope = rememberCoroutineScope()
    val igPagerState = rememberPagerState(pageCount = { 2 })

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Tab Selector with Fluid Spring Glass Pill Indicator
        SpringPillTabBar(
            selectedTab = igPagerState.currentPage,
            onTabSelected = { page ->
                coroutineScope.launch {
                    igPagerState.animateScrollToPage(page)
                }
            },
            tabs = listOf(
                strings.igTabDownloader to Icons.Default.Download,
                strings.igTabInspector to Icons.Default.Search
            ),
            activeGradient = InstagramGradient,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Swipeable Pages
        HorizontalPager(
            state = igPagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> InstagramDownloaderSection()
                1 -> InstagramInspectorSection()
            }
        }
    }
}

// ==============================================================================
// 1. Instagram Downloader Section (Reels, Posts, Carousels & Stories)
// ==============================================================================
@Composable
fun InstagramDownloaderSection() {
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
    var extractedPost by remember { mutableStateOf<InstagramPostInfo?>(null) }
    var downloadProgress by remember { mutableStateOf(DownloadProgressState()) }

    // Auto clipboard detection
    LaunchedEffect(Unit) {
        val clip = clipboardManager.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()?.trim().orEmpty()
            if (text.isNotBlank() && (text.contains("instagram.com") || text.startsWith("@")) && text != inputUrl) {
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
                    borderColor = Color(0xFFE1306C).copy(alpha = 0.6f),
                    backgroundColor = Color(0x33E1306C)
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
                            RealInstagramIcon(size = 20.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تم رصد رابط إنستغرام في الحافظة",
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("لصق وفحص", fontSize = 11.sp, color = Color.White)
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
                placeholder = strings.igInputPlaceholderDownloader,
                label = "رابط الريلز أو المنشور أو الستوري من إنستغرام",
                actionButtonText = strings.igFetchBtn,
                onActionClick = {
                    if (inputUrl.isBlank()) {
                        Toast.makeText(context, "الرجاء إدخال أو لصق رابط إنستغرام", Toast.LENGTH_SHORT).show()
                        return@FloatingSearchPill
                    }
                    isExtracting = true
                    showProcessingBox = true
                    extractedPost = null

                    coroutineScope.launch {
                        val res = InstagramService.extractPostOrReel(inputUrl)
                        // Allow processing steps box to finish smoothly
                        delay(2500L)
                        isExtracting = false
                        res.onSuccess { post ->
                            extractedPost = post
                            Toast.makeText(context, "تم استخراج الميديا بنجاح! 🎯", Toast.LENGTH_SHORT).show()
                        }.onFailure { err ->
                            Toast.makeText(context, err.message ?: strings.igExtractErrorFallback, Toast.LENGTH_LONG).show()
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
                activeGradient = InstagramGradient,
                accentColor = Color(0xFFE1306C),
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
                    borderColor = Color(0xFFE1306C).copy(alpha = 0.5f),
                    backgroundColor = DarkSurfaceCard
                ) {
                    // Author Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AsyncImage(
                            model = buildInstagramImageRequest(context, post.authorAvatar),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Brush.linearGradient(listOf(Color(0xFF833AB4), Color(0xFFFCB045))), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "@${post.authorUsername}",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (post.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    RealVerifiedBadgeIcon(size = 14.dp)
                                }
                            }
                            Text(
                                text = post.authorName,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x33E1306C))
                                .border(1.dp, Color(0xFFE1306C), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (post.isVideo) "Reel / Video 🎬" else "Photo 🖼️",
                                color = Color(0xFFFCB045),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Media Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = buildInstagramImageRequest(context, post.thumbnailUrl),
                            contentDescription = "Media Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (post.isVideo) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x99000000))
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    if (post.caption.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = post.caption,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MetricPill(icon = "❤️", label = "إعجابات", value = "${post.likesCount}")
                        MetricPill(icon = "💬", label = "تعليقات", value = "${post.commentsCount}")
                        MetricPill(icon = "👁️", label = "المشاهدات", value = "${post.viewsCount}")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Download Button
                    Button(
                        onClick = {
                            val downloadUrl = if (post.isVideo && post.videoUrl.isNotBlank()) post.videoUrl else post.thumbnailUrl
                            if (downloadUrl.isBlank()) {
                                Toast.makeText(context, "رابط الميديا غير متوفر", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val fileName = "Instagram_${post.shortcode}_${System.currentTimeMillis()}"

                            coroutineScope.launch {
                                InstagramService.downloadMediaFile(
                                    context = context,
                                    url = downloadUrl,
                                    suggestedFileName = fileName,
                                    isVideo = post.isVideo,
                                    onProgress = { p -> downloadProgress = p }
                                ).onSuccess {
                                    Toast.makeText(context, strings.igDownloadVideoSuccessToast, Toast.LENGTH_LONG).show()
                                }.onFailure { e ->
                                    Toast.makeText(context, "فشل التحميل: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .bounceClick(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(InstagramGradient)
                                .clip(RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (post.isVideo) "تحميل الفيديو الأصلي Full HD (بدون لوقو)" else "تحميل الصورة بدقة أصلية HD",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    if (downloadProgress.isDownloading) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFFE1306C),
                            trackColor = DarkSurfaceElevated
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "جاري التحميل بسرعة ${String.format("%.1f", downloadProgress.speedMBs)} MB/s (${(downloadProgress.progress * 100).toInt()}%)",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 2. Instagram Profile Inspector Section
// ==============================================================================
@Composable
fun InstagramInspectorSection() {
    val context = LocalContext.current
    val strings = Localization.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    var usernameInput by remember { mutableStateOf("") }
    var sessionIdInput by remember { mutableStateOf("") }
    var isSessionModeExpanded by remember { mutableStateOf(false) }
    var isInspecting by remember { mutableStateOf(false) }
    var showProcessingBox by remember { mutableStateOf(false) }
    var inspectedProfile by remember { mutableStateOf<InstagramProfileInfo?>(null) }
    var avatarDownloadProgress by remember { mutableStateOf(DownloadProgressState()) }
    var isInspectingPrivacyOnly by remember { mutableStateOf(false) }

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
            val res = InstagramService.inspectProfile(
                usernameOrUrl = clean,
                sessionId = if (isSessionModeExpanded && sessionIdInput.isNotBlank()) sessionIdInput else null
            )
            delay(2500L)
            isInspecting = false
            res.onSuccess { prof ->
                inspectedProfile = prof
                Toast.makeText(context, "تم فحص الحساب بنجاح! 🎯", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(context, err.message ?: "تعذر جلب بيانات البروفايل", Toast.LENGTH_LONG).show()
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
        // Floating Search Pill for Inspector
        item {
            FloatingSearchPill(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                placeholder = strings.igInputPlaceholderInspector,
                label = "فحص حسابات إنستغرام الشامل باليوزر (@username)",
                actionButtonText = strings.igInspectBtn,
                onActionClick = { triggerInspect(usernameInput) },
                onPasteClick = {
                    val clip = clipboardManager.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        usernameInput = clip.getItemAt(0).text?.toString()?.trim().orEmpty()
                    }
                },
                onClearClick = { usernameInput = "" },
                isLoading = isInspecting,
                activeGradient = InstagramGradient,
                accentColor = Color(0xFFE1306C),
                leadingIcon = Icons.Default.Search
            )
        }

        // Expandable Session Mode Pill / Card
        item {
            GlassCard(
                borderColor = if (isSessionModeExpanded) Color(0xFF00F2FE).copy(alpha = 0.5f) else GlassBorder,
                backgroundColor = DarkSurfaceCard,
                cornerRadius = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .bounceClick { isSessionModeExpanded = !isSessionModeExpanded }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isSessionModeExpanded) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isSessionModeExpanded) Color(0xFF00F2FE) else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "فحص متقدم بالجلسة (تاريخ الأسماء، المحظورين، والمفضلين)",
                        color = if (isSessionModeExpanded) Color(0xFF00F2FE) else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (isSessionModeExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (isSessionModeExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = sessionIdInput,
                        onValueChange = { sessionIdInput = it },
                        placeholder = {
                            Text(
                                strings.igSessionIdPlaceholder,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F2FE),
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated
                        ),
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF00F2FE), modifier = Modifier.size(18.dp))
                        },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.igPrivacyNoticeText,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
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

        // Inspected Profile Card
        if (inspectedProfile != null) {
            val prof = inspectedProfile!!
            item {
                GlassCard(
                    borderColor = Color(0xFF833AB4).copy(alpha = 0.5f),
                    backgroundColor = DarkSurfaceCard
                ) {
                    // Profile Header & Avatar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AsyncImage(
                                model = buildInstagramImageRequest(context, prof.hdProfilePicUrl),
                                contentDescription = "Profile Avatar",
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .border(
                                        2.dp,
                                        Brush.linearGradient(listOf(Color(0xFF833AB4), Color(0xFFE1306C), Color(0xFFFCB045))),
                                        CircleShape
                                    ),
                                contentScale = ContentScale.Crop
                            )
                            if (prof.isVerified) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .padding(1.dp)
                                ) {
                                    RealVerifiedBadgeIcon(size = 20.dp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "@${prof.username}",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Text(
                                text = prof.fullName,
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (prof.isPrivate) Color(0x33FF5252) else Color(0x3300E676))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (prof.isPrivate) "حساب خاص 🔒" else "حساب عام 🌐",
                                    color = if (prof.isPrivate) Color(0xFFFF5252) else Color(0xFF00E676),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (prof.biography.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = prof.biography,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3 Big Stats Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileStatItem(
                            modifier = Modifier.weight(1f),
                            value = formatCountCompact(prof.followersCount),
                            label = "المتابعون"
                        )
                        ProfileStatItem(
                            modifier = Modifier.weight(1f),
                            value = formatCountCompact(prof.followingCount),
                            label = "يتابع"
                        )
                        ProfileStatItem(
                            modifier = Modifier.weight(1f),
                            value = formatCountCompact(prof.postsCount),
                            label = "المنشورات"
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Download HD Avatar Button
                    OutlinedButton(
                        onClick = {
                            if (prof.hdProfilePicUrl.isBlank()) return@OutlinedButton
                            val fileName = "Instagram_Avatar_${prof.username}_${System.currentTimeMillis()}.jpg"
                            coroutineScope.launch {
                                InstagramService.downloadMediaFile(
                                    context = context,
                                    url = prof.hdProfilePicUrl,
                                    suggestedFileName = fileName,
                                    isVideo = false,
                                    onProgress = { avatarDownloadProgress = it }
                                ).onSuccess {
                                    Toast.makeText(context, "تم حفظ صورة البروفايل HD في المعرض! 🖼️", Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    Toast.makeText(context, "فشل حفظ الصورة", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE1306C))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFFE1306C), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.igDownloadHdAvatarBtn, color = Color(0xFFE1306C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 1. Account Identity & History Card (Deep OSINT)
            item {
                GlassCard(
                    borderColor = Color(0xFF00F2FE).copy(alpha = 0.5f),
                    backgroundColor = DarkSurfaceCard
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color(0xFF00F2FE), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.igHistoryTitle, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val userPkDisplay = if (prof.userPk.isNotBlank()) prof.userPk else "غير معروف"
                        OsintInfoBox(
                            modifier = Modifier.weight(1f),
                            label = strings.igUserIdLabel,
                            value = userPkDisplay,
                            icon = Icons.Default.Badge,
                            tint = Color(0xFF00F2FE),
                            onClick = if (prof.userPk.isNotBlank()) {
                                {
                                    val clip = android.content.ClipData.newPlainText("User PK", prof.userPk)
                                    clipboardManager.setPrimaryClip(clip)
                                    Toast.makeText(context, "تم نسخ معرف الحساب: ${prof.userPk} 📋", Toast.LENGTH_SHORT).show()
                                }
                            } else null
                        )
                        OsintInfoBox(
                            modifier = Modifier.weight(1f),
                            label = strings.igDateJoinedLabel,
                            value = if (prof.dateJoined.isNotBlank()) prof.dateJoined else "غير متوفر",
                            icon = Icons.Default.Schedule,
                            tint = Color(0xFFFF007A)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OsintInfoBox(
                            modifier = Modifier.weight(1f),
                            label = strings.igAccountTypeLabel,
                            value = prof.accountType,
                            icon = Icons.Default.Category,
                            tint = Color(0xFFFCB045)
                        )
                        OsintInfoBox(
                            modifier = Modifier.weight(1f),
                            label = strings.igCategoryLabel,
                            value = if (prof.categoryName.isNotBlank()) prof.categoryName else "شخصي / عام",
                            icon = Icons.Default.Star,
                            tint = Color(0xFF00E676)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.igFormerUsernamesLabel + ":", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (prof.formerUsernamesCount > 0) "${prof.formerUsernamesCount} مرات" else "0 (أو يتطلب وضع الجلسة)",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }

                    if (prof.bioLinks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceElevated)
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF00F2FE), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.igBioLinksLabel, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            prof.bioLinks.forEach { link ->
                                Text(
                                    text = "• $link",
                                    color = Color(0xFF00F2FE),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 2. Top Connected & Interacting Friends Card
            item {
                GlassCard(
                    borderColor = Color(0xFFE1306C).copy(alpha = 0.5f),
                    backgroundColor = DarkSurfaceCard
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFFE1306C), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.igTopFriendsTitle, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Text(
                        text = strings.igTopFriendsSubtitle,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.igClickToInspectHint,
                        color = Color(0xFF00F2FE),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (prof.topConnectedFriends.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            prof.topConnectedFriends.forEach { friend ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DarkSurfaceElevated)
                                        .bounceClick { triggerInspect(friend.username) }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (friend.avatarUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = buildInstagramImageRequest(context, friend.avatarUrl),
                                            contentDescription = friend.username,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, Color(0xFFE1306C), CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF833AB4).copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = friend.username.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "@${friend.username}",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = friend.fullName,
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    val badgeColor = when {
                                        friend.relationType.contains("محادثة") -> Color(0xFF00C853)
                                        friend.relationType.contains("مقرب") -> Color(0xFFFFD700)
                                        friend.relationType.contains("ارتباط") -> Color(0xFF00F2FE)
                                        friend.relationType.contains("تعاون") || friend.relationType.contains("وسم") -> Color(0xFFFF9800)
                                        else -> Color(0xFFE1306C)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(badgeColor.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = friend.relationType,
                                                color = badgeColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${friend.interactionCount} ${strings.igInteractionsCountSuffix}",
                                            color = Color(0xFF00F2FE),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Inspect",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = strings.igNoFriendsFound,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            // 3. Privacy Session Data Card (if session data returned)
            if (prof.privacyData != null) {
                val pData = prof.privacyData!!
                item {
                    GlassCard(
                        borderColor = Color(0xFF00E676).copy(alpha = 0.5f),
                        backgroundColor = DarkSurfaceCard
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.igPrivacySessionTitle, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        // Close Friends
                        if (pData.closeFriends.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(strings.igCloseFriendsTitle + " (${pData.closeFriends.size})", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            pData.closeFriends.forEach { cf ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = buildInstagramImageRequest(context, cf.avatarUrl),
                                        contentDescription = cf.username,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Color(0xFF00E676), CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("@${cf.username} (${cf.fullName})", color = TextPrimary, fontSize = 12.sp)
                                }
                            }
                        }

                        // Blocked Users
                        if (pData.blockedUsers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(strings.igBlockedUsersTitle + " (${pData.blockedUsers.size})", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            pData.blockedUsers.forEach { bu ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("@${bu.username} (${bu.fullName})", color = TextPrimary, fontSize = 12.sp)
                                }
                            }
                        }

                        // Direct Message Friends
                        if (pData.directFriends.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(strings.igDirectFriendsTitle + " (${pData.directFriends.size})", color = Color(0xFF00F2FE), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            pData.directFriends.take(6).forEach { df ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Forum, contentDescription = null, tint = Color(0xFF00F2FE), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("@${df.username} (${df.fullName})", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(df.lastMessageSnippet, color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Latest Posts preview
            if (prof.latestPosts.isNotEmpty()) {
                item {
                    Text(
                        text = strings.igLatestPostsHeader,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(prof.latestPosts) { post ->
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
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = buildInstagramImageRequest(context, post.thumbnailUrl),
                                    contentDescription = "Post Thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (post.isVideo) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = post.caption.ifBlank { "منشور إنستغرام" },
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("❤️ ${post.likesCount}", color = TextSecondary, fontSize = 11.sp)
                                    Text("💬 ${post.commentsCount}", color = TextSecondary, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    val dlUrl = if (post.isVideo && post.videoUrl.isNotBlank()) post.videoUrl else post.thumbnailUrl
                                    if (dlUrl.isBlank()) return@Button
                                    coroutineScope.launch {
                                        InstagramService.downloadMediaFile(
                                            context = context,
                                            url = dlUrl,
                                            suggestedFileName = "Instagram_${post.shortcode}_${System.currentTimeMillis()}",
                                            isVideo = post.isVideo,
                                            onProgress = {}
                                        ).onSuccess {
                                            Toast.makeText(context, "تم تنزيل المقطع بنجاح! 📥", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.bounceClick(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricPill(
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

@Composable
fun ProfileStatItem(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
            .bounceClick()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, color = Color(0xFFFCB045), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
    }
}

fun formatCountCompact(count: Long): String {
    return when {
        count >= 1_000_000_000 -> String.format("%.1fB", count / 1_000_000_000.0)
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

@Composable
fun OsintInfoBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceElevated)
            .border(0.5.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .then(
                if (onClick != null) Modifier.bounceClick { onClick() } else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = TextSecondary, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
