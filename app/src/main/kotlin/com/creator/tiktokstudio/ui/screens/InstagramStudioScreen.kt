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
import com.creator.tiktokstudio.core.InstagramService
import com.creator.tiktokstudio.core.Localization
import com.creator.tiktokstudio.data.*
import com.creator.tiktokstudio.ui.components.*
import com.creator.tiktokstudio.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var selectedIgTab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepDarkBackground)
    ) {
        // Tab Selector (0: Downloader, 1: Profile Inspector)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurfaceElevated)
                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IgTabItem(
                modifier = Modifier.weight(1f),
                title = strings.igTabDownloader,
                icon = Icons.Default.Download,
                isSelected = selectedIgTab == 0,
                onClick = { selectedIgTab = 0 }
            )
            IgTabItem(
                modifier = Modifier.weight(1f),
                title = strings.igTabInspector,
                icon = Icons.Default.Search,
                isSelected = selectedIgTab == 1,
                onClick = { selectedIgTab = 1 }
            )
        }

        Crossfade(
            targetState = selectedIgTab,
            label = "IgTabCrossfade",
            modifier = Modifier.fillMaxSize()
        ) { tabIndex ->
            when (tabIndex) {
                0 -> InstagramDownloaderSection()
                1 -> InstagramInspectorSection()
            }
        }
    }
}

@Composable
fun IgTabItem(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) InstagramGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = if (isSelected) Color.White else TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
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

        // Input Card
        item {
            GlassCard(
                borderColor = Color(0xFFE1306C).copy(alpha = 0.3f),
                backgroundColor = DarkSurfaceCard
            ) {
                Text(
                    text = "رابط الريلز أو المنشور أو الستوري من إنستغرام",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    placeholder = {
                        Text(
                            strings.igInputPlaceholderDownloader,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE1306C),
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (inputUrl.isNotBlank()) {
                                IconButton(onClick = { inputUrl = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                                }
                            }
                            IconButton(onClick = {
                                val clip = clipboardManager.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    inputUrl = clip.getItemAt(0).text?.toString()?.trim().orEmpty()
                                }
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = Color(0xFFFCB045))
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (inputUrl.isBlank()) {
                            Toast.makeText(context, "الرجاء إدخال أو لصق رابط إنستغرام", Toast.LENGTH_SHORT).show()
                            return@Button
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isExtracting
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(InstagramGradient)
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isExtracting) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري فك تشفير وسائط إنستغرام...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.igFetchBtn, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
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
                            model = post.authorAvatar,
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
                            model = post.thumbnailUrl,
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
                            .height(48.dp),
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

    var usernameInput by remember { mutableStateOf("") }
    var isInspecting by remember { mutableStateOf(false) }
    var showProcessingBox by remember { mutableStateOf(false) }
    var inspectedProfile by remember { mutableStateOf<InstagramProfileInfo?>(null) }
    var avatarDownloadProgress by remember { mutableStateOf(DownloadProgressState()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search Input Card
        item {
            GlassCard(
                borderColor = Color(0xFF833AB4).copy(alpha = 0.4f),
                backgroundColor = DarkSurfaceCard
            ) {
                Text(
                    text = "فحص حسابات إنستغرام الشامل باليوزر (@username)",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    placeholder = {
                        Text(
                            strings.igInputPlaceholderInspector,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF833AB4),
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    ),
                    leadingIcon = {
                        RealInstagramIcon(size = 20.dp)
                    },
                    trailingIcon = {
                        if (usernameInput.isNotBlank()) {
                            IconButton(onClick = { usernameInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (usernameInput.isBlank()) {
                            Toast.makeText(context, "الرجاء إدخال اسم المستخدم أو الرابط", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isInspecting = true
                        showProcessingBox = true
                        inspectedProfile = null

                        coroutineScope.launch {
                            val res = InstagramService.inspectProfile(usernameInput)
                            delay(2500L)
                            isInspecting = false
                            res.onSuccess { prof ->
                                inspectedProfile = prof
                                Toast.makeText(context, "تم فحص الحساب بنجاح! 🎯", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "تعذر جلب بيانات البروفايل", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isInspecting
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(InstagramGradient)
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isInspecting) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري تحليل البروفايل بالخوارزمية...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.igInspectBtn, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
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
                                model = prof.hdProfilePicUrl,
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
                        modifier = Modifier.fillMaxWidth(),
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
                                    model = post.thumbnailUrl,
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
