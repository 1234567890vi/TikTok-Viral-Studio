package com.creator.tiktokstudio.ui.screens

import android.content.ClipData
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
import com.creator.tiktokstudio.core.TikTokDownloaderService
import com.creator.tiktokstudio.data.*
import com.creator.tiktokstudio.ui.components.CircularDownloadProgress
import com.creator.tiktokstudio.ui.components.GlassCard
import com.creator.tiktokstudio.ui.components.NeonBadge
import com.creator.tiktokstudio.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InspectorScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var urlInput by remember { mutableStateOf("") }
    var detectedClipboardUrl by remember { mutableStateOf<String?>(null) }
    var isInspecting by remember { mutableStateOf(false) }
    var inspectionStatusText by remember { mutableStateOf("جاري فحص وتفكيك الرابط...") }
    var inspectionResult by remember { mutableStateOf<TikTokDeepInspectionData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf(DownloadProgressState()) }

    // Auto-clipboard detection on entry
    LaunchedEffect(Unit) {
        try {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString() ?: ""
                if (text.contains("tiktok.com") && text != urlInput) {
                    detectedClipboardUrl = text
                }
            }
        } catch (_: Exception) {}
    }

    // Dynamic status text animation while inspecting
    LaunchedEffect(isInspecting) {
        if (isInspecting) {
            val messages = listOf(
                "جاري تتبع التحويلات وفك الرابط الأصلي...",
                "جاري الاتصال بخوادم تيك توك وسحب بيانات المنشور...",
                "جاري استخراج مواصفات الفيديو ومعدل الإطارات والأبعاد...",
                "جاري جلب وترتيب التعليقات الأكثر إعجاباً من API..."
            )
            var index = 0
            while (isInspecting) {
                inspectionStatusText = messages[index % messages.size]
                delay(1200)
                index++
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
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
                            tint = DeepDarkBackground,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "الفحص الشامل 🔍 (TikTok Deep Inspector)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "تحليل الفيديوهات، القصص (Story)، الصور، والحسابات ببيانات حقيقية 100%",
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
                                        text = "تم التقاط رابط من الحافظة!",
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
                                    isInspecting = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        val res = TikTokDownloaderService.inspectTikTokUrl(urlInput)
                                        isInspecting = false
                                        res.onSuccess {
                                            inspectionResult = it
                                            errorMessage = null
                                            Toast.makeText(context, "تم الفحص والتحليل بنجاح! 🎯", Toast.LENGTH_SHORT).show()
                                        }.onFailure {
                                            errorMessage = it.localizedMessage ?: "فشل فحص الرابط"
                                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("فحص فوري", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Search Input Box
        item {
            GlassCard {
                Text(
                    text = "رابط الفيديو، القصة (Story)، ألبوم الصور، أو الحساب (@username)",
                    style = MaterialTheme.typography.labelLarge.copy(color = NeonCyan),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        if (errorMessage != null) errorMessage = null
                    },
                    placeholder = {
                        Text(
                            "ضع أي رابط تيك توك هنا (فيديو، قصة، صورة، أو بروفايل)...",
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
                                    Icon(Icons.Default.Clear, contentDescription = "مسح", tint = TextMuted)
                                }
                            }
                            IconButton(onClick = {
                                val clip = clipboardManager.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString() ?: ""
                                    if (text.isNotBlank()) urlInput = text
                                }
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "لصق", tint = NeonCyan)
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (urlInput.isBlank()) {
                            Toast.makeText(context, "الرجاء إدخال أو لصق رابط تيك توك", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isInspecting = true
                        errorMessage = null
                        coroutineScope.launch {
                            val res = TikTokDownloaderService.inspectTikTokUrl(urlInput)
                            isInspecting = false
                            res.onSuccess {
                                inspectionResult = it
                                errorMessage = null
                                Toast.makeText(context, "تم الفحص الشامل بنجاح! 🚀", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                errorMessage = it.localizedMessage ?: "فشل فحص الرابط"
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
                    if (isInspecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = DeepDarkBackground,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "جاري فك الرابط والاتصال بـ API...",
                            color = DeepDarkBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = DeepDarkBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "بدء الفحص الشامل والتحليل العميق 🔍",
                            color = DeepDarkBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                if (isInspecting) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = inspectionStatusText,
                        color = NeonCyan,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Error Card (if any real failure occurs)
        if (errorMessage != null) {
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
                                text = "تنبيه أثناء الفحص",
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
                        text = "جاري الحفظ بدون علامة مائية...",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // If no inspection yet, show informative guide card
        if (inspectionResult == null && !isInspecting && errorMessage == null) {
            item {
                GlassCard(
                    borderColor = NeonCyan.copy(alpha = 0.2f),
                    backgroundColor = DarkSurfaceCard
                ) {
                    Text(
                        text = "ما الذي يوفره لك الفحص الشامل؟",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InspectorFeatureRow(icon = Icons.Default.Movie, title = "فيديوهات فائقة الدقة", desc = "استخراج جودة FHD والأبعاد وحجم الملف الصافي")
                    Spacer(modifier = Modifier.height(8.dp))
                    InspectorFeatureRow(icon = Icons.Default.HistoryToggleOff, title = "قصص تيك توك (Stories)", desc = "تمييز القصص تلقائياً وإظهار إحصائياتها الدقيقة الحقيقية")
                    Spacer(modifier = Modifier.height(8.dp))
                    InspectorFeatureRow(icon = Icons.Default.PhotoLibrary, title = "ألبومات الصور (Photo Mode)", desc = "فحص كافة صور الألبوم وتنزيلها بجودة أصلية")
                    Spacer(modifier = Modifier.height(8.dp))
                    InspectorFeatureRow(icon = Icons.Default.Person, title = "صور الحسابات (Avatars)", desc = "عرض صور البروفايل وصور المعلقين بدقة عالية عبر Coil")
                    Spacer(modifier = Modifier.height(8.dp))
                    InspectorFeatureRow(icon = Icons.Default.Favorite, title = "🔥 التعليقات الحقيقية الأكثر إعجاباً", desc = "جلب التعليقات الحقيقية من API مرتبة حسب الإعجابات")
                }
            }
        }

        // RESULT VIEW
        if (inspectionResult != null) {
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
                            text = "${data.contentType.badgeIcon} ${data.contentType.titleArabic}",
                            textColor = if (data.contentType == TikTokContentType.STORY) NeonPink else NeonCyan,
                            borderColor = if (data.contentType == TikTokContentType.STORY) NeonPink else NeonCyan
                        )
                        if (data.isVerified) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("موثق", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                        Toast.makeText(context, "جاري حفظ صورة البروفايل...", Toast.LENGTH_SHORT).show()
                                        val saveRes = TikTokDownloaderService.downloadImageFile(
                                            context = context,
                                            downloadUrl = data.authorAvatar,
                                            title = "avatar_${data.authorUniqueId}",
                                            onProgress = { downloadProgress = it }
                                        )
                                        saveRes.onSuccess {
                                            Toast.makeText(context, "تم حفظ صورة البروفايل في المعرض! 🖼️", Toast.LENGTH_SHORT).show()
                                        }.onFailure {
                                            Toast.makeText(context, "تعذر حفظ الصورة: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "حفظ الصورة",
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
                            StatBox(label = "متابع", value = formatCounter(data.followerCount))
                            StatBox(label = "يتابع", value = formatCounter(data.followingCount))
                            StatBox(label = "إعجابات", value = formatCounter(data.authorTotalLikes))
                            StatBox(label = "فيديوهات", value = formatCounter(data.authorVideoCount))
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
                                contentDescription = "غلاف المنشور",
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
                                    text = "${data.contentType.badgeIcon} ${data.contentType.titleArabic}",
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
                                    text = "الوصف / العنوان:",
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
                                    Toast.makeText(context, "تم نسخ النص! 📋", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = TextMuted)
                            }
                        }
                    }
                }
            }

            // 3. Bento Grid of Media Specs & Real Metrics
            if (data.contentType != TikTokContentType.PROFILE) {
                item {
                    Text(
                        text = "مواصفات وإحصائيات المحتوى الحقيقية (100% Real Analytics)",
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
                                    text = "تاريخ وتوقيت النشر الدقيق",
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
                            label = "المشاهدات",
                            accentColor = NeonCyan
                        )
                        BentoMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Favorite,
                            value = formatCounter(data.likesCount),
                            label = "الإعجابات",
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
                            label = "التعليقات",
                            accentColor = NeonPurple
                        )
                        BentoMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.BookmarkBorder,
                            value = formatCounter(data.collectCount),
                            label = "المحفوظات",
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
                            label = "الدقة والأبعاد",
                            accentColor = NeonCyan
                        )
                        BentoMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.SdCard,
                            value = if (data.fileSizeMB > 0) "${data.fileSizeMB} MB | ${data.durationSec} ث" else "${data.durationSec} ثانية",
                            label = "الحجم والمدة",
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
                                text = "ألبوم صور (${data.photos.size} صور)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            )
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        Toast.makeText(context, "جاري تحميل كافة صور الألبوم...", Toast.LENGTH_SHORT).show()
                                        for (photo in data.photos) {
                                            TikTokDownloaderService.downloadImageFile(
                                                context = context,
                                                downloadUrl = photo.url,
                                                title = "photo_${data.authorUniqueId}_${photo.index}",
                                                onProgress = { downloadProgress = it }
                                            )
                                        }
                                        Toast.makeText(context, "تم حفظ كافة الصور في الاستوديو! 📸", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("تحميل الكل 📥", color = DeepDarkBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                        contentDescription = "صورة ${photo.index}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xCC000000))
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
                            text = "تحميل المحتوى الصافي بدون علامة مائية:",
                            style = MaterialTheme.typography.labelMedium.copy(color = NeonCyan),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        // Video / Story Download Button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val targetUrl = if (data.videoHdUrl.isNotBlank()) data.videoHdUrl else data.videoSdUrl
                                    if (targetUrl.isBlank()) {
                                        Toast.makeText(context, "رابط الفيديو غير متوفر", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                                    Toast.makeText(context, "بدأ التحميل بدون علامة مائية...", Toast.LENGTH_SHORT).show()
                                    val dlRes = TikTokDownloaderService.downloadMediaFile(
                                        context = context,
                                        downloadUrl = targetUrl,
                                        title = data.title.ifBlank { "TikTok_${data.authorUniqueId}" },
                                        isVideo = true,
                                        onProgress = { downloadProgress = it }
                                    )
                                    dlRes.onSuccess {
                                        Toast.makeText(context, "تم حفظ الفيديو بنجاح في المعرض! 🎬", Toast.LENGTH_LONG).show()
                                    }.onFailure {
                                        Toast.makeText(context, "فشل التحميل: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = DeepDarkBackground)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (data.contentType == TikTokContentType.STORY) "تحميل القصة بدون علامة مائية ⏱️" else "تحميل الفيديو بدون علامة مائية (FHD)",
                                color = DeepDarkBackground,
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
                                        Toast.makeText(context, "جاري استخراج وتحميل الصوت MP3...", Toast.LENGTH_SHORT).show()
                                        val dlRes = TikTokDownloaderService.downloadMediaFile(
                                            context = context,
                                            downloadUrl = data.audioUrl,
                                            title = "${data.audioTitle}_${data.authorUniqueId}",
                                            isVideo = false,
                                            onProgress = { downloadProgress = it }
                                        )
                                        dlRes.onSuccess {
                                            Toast.makeText(context, "تم حفظ المقطع الصوتي MP3! 🎵", Toast.LENGTH_LONG).show()
                                        }.onFailure {
                                            Toast.makeText(context, "فشل حفظ الصوت: ${it.message}", Toast.LENGTH_SHORT).show()
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
                                    "تحميل الصوت الأصلي فقط (MP3)",
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
                            text = "🔥 التعليقات الحقيقية الأكثر إعجاباً (Top Liked Comments)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonPink
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        NeonBadge(text = "${data.topComments.size} تعليق", textColor = NeonPink, borderColor = NeonPink)
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
                                            Toast.makeText(context, "تم نسخ التعليق! 📋", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "نسخ",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("نسخ التعليق", color = NeonCyan, fontSize = 11.sp)
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
                            text = "لا توجد تعليقات عامة متاحة لهذا المنشور حالياً.",
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
fun InspectorFeatureRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(DarkSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(desc, color = TextSecondary, fontSize = 11.sp)
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
        count >= 1_000_000_000 -> String.format("%.1fB", count / 1_000_000_000.0)
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
