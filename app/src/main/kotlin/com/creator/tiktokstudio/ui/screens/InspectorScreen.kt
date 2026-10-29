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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                "جاري الاتصال بخوادم تيك توك وسحب بيانات الرابط...",
                "جاري استخراج مواصفات الفيديو ومعدل الإطارات والأبعاد...",
                "جاري تحليل حساب الناشر وإحصائيات التفاعل...",
                "جاري استخراج وترتيب التعليقات الأكثر إعجاباً..."
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
                            text = "تحليل الفيديوهات، الصور، القصص، والحسابات مع أكثر التعليقات إعجاباً",
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
                                    coroutineScope.launch {
                                        val res = TikTokDownloaderService.inspectTikTokUrl(urlInput)
                                        isInspecting = false
                                        res.onSuccess {
                                            inspectionResult = it
                                            Toast.makeText(context, "تم الفحص والتحليل بنجاح! 🎯", Toast.LENGTH_SHORT).show()
                                        }.onFailure {
                                            Toast.makeText(context, it.message ?: "فشل الفحص", Toast.LENGTH_SHORT).show()
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
                    text = "رابط الفيديو، الصورة، القصة، أو الحساب (@username)",
                    style = MaterialTheme.typography.labelLarge.copy(color = NeonCyan),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = {
                        Text(
                            "ضع أي رابط تيك توك هنا (فيديو، صورة، قصة، أو بروفايل)...",
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
                        coroutineScope.launch {
                            val res = TikTokDownloaderService.inspectTikTokUrl(urlInput)
                            isInspecting = false
                            res.onSuccess {
                                inspectionResult = it
                                Toast.makeText(context, "تم الفحص الشامل بنجاح! 🚀", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                Toast.makeText(context, it.message ?: "فشل الفحص", Toast.LENGTH_SHORT).show()
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
                            "جاري الفحص العميق واستخراج البيانات...",
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

                // Pulsing indicator subtitle
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
                        text = "جاري الحفظ بجودة عالية بدون علامة مائية...",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // If no inspection yet, show informative guide card
        if (inspectionResult == null && !isInspecting) {
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
                    InspectorFeatureRow(icon = Icons.Default.Movie, title = "فيديوهات فائقة الدقة", desc = "استخراج جودة 4K/FHD والأبعاد وحجم الملف الصافي")
                    Spacer(modifier = Modifier.height(8.dp))
                    InspectorFeatureRow(icon = Icons.Default.PhotoLibrary, title = "ألبومات الصور (Photo Mode)", desc = "فحص كافة صور الألبوم وتنزيلها دفعة واحدة بجودة أصلية")
                    Spacer(modifier = Modifier.height(8.dp))
                    InspectorFeatureRow(icon = Icons.Default.HistoryToggleOff, title = "قصص تيك توك (Stories)", desc = "تحليل وتنزيل القصص قبل اختفائها بدقة كاملة")
                    Spacer(modifier = Modifier.height(8.dp))
                    InspectorFeatureRow(icon = Icons.Default.Person, title = "فحص الحسابات والبروفايل", desc = "إحصائيات المتابعين، مجموع اللايكات، والنبذة التعريفية")
                    Spacer(modifier = Modifier.height(8.dp))
                    InspectorFeatureRow(icon = Icons.Default.Favorite, title = "🔥 التعليقات الأكثر إعجاباً", desc = "جلب التعليقات الأعلى تفاعلاً مع إمكانية نسخها بنقرة واحدة")
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
                            textColor = NeonCyan,
                            borderColor = NeonCyan
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

                    // Profile Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
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
                                    text = "الوصف والهاشتاقات:",
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

            // 3. Bento Grid of Media Specs & Metrics
            if (data.contentType != TikTokContentType.PROFILE) {
                item {
                    Text(
                        text = "مواصفات وإحصائيات المحتوى (Specs & Analytics)",
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
                            value = "${data.fileSizeMB} MB | ${data.durationSec} ث",
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
                                text = "ألبوم صور (${data.photos.size} صور عالية الدقة)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            )
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        Toast.makeText(context, "جاري تحميل جميع صور الألبوم...", Toast.LENGTH_SHORT).show()
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
                    }
                }
            }

            // 5. Quick Download Actions (for Video or Audio)
            if (data.contentType != TikTokContentType.PROFILE) {
                item {
                    GlassCard(borderColor = NeonCyan.copy(alpha = 0.5f)) {
                        Text(
                            text = "تحميل المحتوى الصافي بدون علامة مائية:",
                            style = MaterialTheme.typography.labelMedium.copy(color = NeonCyan),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        // Video Download Button
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
                                "تحميل الفيديو بدون علامة مائية (FHD)",
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
                            text = "🔥 التعليقات الأكثر إعجاباً (Top Liked Comments)",
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
                                    .size(38.dp)
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
