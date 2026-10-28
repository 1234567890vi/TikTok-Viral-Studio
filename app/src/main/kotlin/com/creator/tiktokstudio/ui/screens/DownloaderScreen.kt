package com.creator.tiktokstudio.ui.screens

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creator.tiktokstudio.core.NativeEngine
import com.creator.tiktokstudio.core.TikTokDownloaderService
import com.creator.tiktokstudio.data.DownloadProgressState
import com.creator.tiktokstudio.data.DownloadedMedia
import com.creator.tiktokstudio.data.TikTokVideoInfo
import com.creator.tiktokstudio.ui.components.CircularDownloadProgress
import com.creator.tiktokstudio.ui.components.GlassCard
import com.creator.tiktokstudio.ui.components.NeonBadge
import com.creator.tiktokstudio.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DownloaderScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var urlInput by remember { mutableStateOf("") }
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
        // App Header Banner
        item {
            GlassCard(
                borderColor = NeonCyan.copy(alpha = 0.4f),
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
                                Brush.linearGradient(listOf(NeonCyan, NeonPink))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = DeepDarkBackground,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "تحميل",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "تحميل مباشر بدون علامة مائية بدقة 4K / Full HD",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                }
            }
        }

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
                                        text = "تم التقاط رابط تيك توك من الحافظة!",
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
                                    coroutineScope.launch {
                                        val res = TikTokDownloaderService.extractMediaInfo(urlInput)
                                        isExtracting = false
                                        res.onSuccess { extractedVideo = it }
                                            .onFailure {
                                                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("لصق وفحص", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    text = "رابط الفيديو من تيك توك",
                    style = MaterialTheme.typography.labelLarge.copy(color = NeonCyan),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = {
                        Text(
                            "ضع رابط الفيديو هنا (مثال: https://vm.tiktok.com/...)",
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
                        isExtracting = true
                        coroutineScope.launch {
                            val res = TikTokDownloaderService.extractMediaInfo(urlInput)
                            isExtracting = false
                            res.onSuccess {
                                extractedVideo = it
                                Toast.makeText(context, "تم استخراج الفيديو بنجاح!", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                Toast.makeText(context, it.message ?: "تعذر جلب البيانات", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan
                    ),
                    enabled = !isExtracting
                ) {
                    if (isExtracting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = DeepDarkBackground,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "جاري فك التشفير واستخراج الروابط...",
                            color = DeepDarkBackground,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = DeepDarkBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "فحص واستخراج الفيديو الخارق",
                            color = DeepDarkBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Extracted Video Preview Card
        if (extractedVideo != null) {
            item {
                val video = extractedVideo!!
                GlassCard(
                    borderColor = NeonCyan.copy(alpha = 0.5f),
                    backgroundColor = DarkSurfaceCard
                ) {
                    Text(
                        text = "معاينة الفيديو والبيانات المستخرجة",
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

                        NeonBadge(text = "بدون علامة مائية", textColor = NeonCyan)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

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
                        MetricItem(icon = Icons.Outlined.Visibility, value = "${video.viewsCount / 1000}K", label = "مشاهدة")
                        MetricItem(icon = Icons.Outlined.FavoriteBorder, value = "${video.likesCount / 1000}K", label = "إعجاب")
                        MetricItem(icon = Icons.Outlined.ChatBubbleOutline, value = "${video.commentsCount}", label = "تعليق")
                        MetricItem(icon = Icons.Outlined.Share, value = "${video.sharesCount}", label = "مشاركة")
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
                                        Toast.makeText(context, "تم حفظ الفيديو في Movies/TikTokStudio", Toast.LENGTH_LONG).show()
                                    }.onFailure {
                                        Toast.makeText(context, "فشل التحميل: ${it.message}", Toast.LENGTH_SHORT).show()
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
                            Text("تحميل فيديو HD", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                                        Toast.makeText(context, "تم حفظ الصوت في Music/TikTokStudio", Toast.LENGTH_LONG).show()
                                    }.onFailure {
                                        Toast.makeText(context, "فشل التحميل: ${it.message}", Toast.LENGTH_SHORT).show()
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
                            Text("تحميل صوت MP3", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                        text = if (downloadProgress.isCompleted) "اكتمل التحميل بنجاح!" else "جاري التحميل بأقصى سرعة NDK...",
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
                        text = "التحميلات السابقة (الاستوديو المدمج)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "${downloadedHistory.size} ملفات",
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
                                    text = "لا توجد ملفات محملة حتى الآن",
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
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة المقطع عبر"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = NeonCyan)
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
