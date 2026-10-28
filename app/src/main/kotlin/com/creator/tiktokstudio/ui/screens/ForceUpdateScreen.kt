package com.creator.tiktokstudio.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creator.tiktokstudio.core.RemoteConfig
import com.creator.tiktokstudio.core.RemoteUpdateManager
import com.creator.tiktokstudio.ui.components.CircularDownloadProgress
import com.creator.tiktokstudio.ui.components.GlassCard
import com.creator.tiktokstudio.ui.components.NeonBadge
import com.creator.tiktokstudio.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ForceUpdateScreen(
    config: RemoteConfig?,
    modifier: Modifier = Modifier
) {
    // Strictly block hardware and gesture back button
    BackHandler(enabled = true) {
        // Prevent bypassing update screen
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val downloadState by RemoteUpdateManager.downloadState.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulseRocket")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepDarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(NeonPink, NeonCyan, NeonPurple)
                        )
                    ),
                    RoundedCornerShape(26.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = Color(0xF513131D)),
            shape = RoundedCornerShape(26.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Pulsing Rocket Icon
                item {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(listOf(NeonPink, NeonCyan, NeonPink))
                            )
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }
                }

                // Update Title & Badge
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NeonBadge(
                                text = "تحديث إجباري رسمي",
                                textColor = NeonPink,
                                borderColor = NeonPink
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            NeonBadge(
                                text = "v${config?.latestVersionName ?: "1.0.1"}",
                                textColor = NeonCyan,
                                borderColor = NeonCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = config?.updateTitleAr ?: "🚀 تحديث إجباري جديد متاح!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = config?.updateMessageAr ?: "لضمان استمرار عمل محرك التحميل الخارق بدقة 4K بدون انقطاع، يجب تحديث التطبيق الآن.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Changelog Card
                item {
                    GlassCard(
                        borderColor = NeonCyan.copy(alpha = 0.3f),
                        backgroundColor = DarkSurfaceElevated,
                        cornerRadius = 16.dp
                    ) {
                        Text(
                            text = "✨ ما الجديد في هذا التحديث:",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val changelogs = config?.changelogAr?.takeIf { it.isNotEmpty() } ?: listOf(
                            "تحديث محرك فك تشفير روابط تيك توك بدون علامة مائية",
                            "تسريع عمليات التحميل المتعددة C++ NDK",
                            "تحسين استقرار واجهات الـ Compose وثبات 120Hz"
                        )

                        changelogs.forEach { itemText ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("⚡", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = itemText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextPrimary,
                                        lineHeight = 18.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Progress Indicator during download
                if (downloadState.isDownloading) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularDownloadProgress(
                                progress = downloadState.progress,
                                speedMBs = downloadState.speedMBs,
                                downloadedBytes = (downloadState.downloadedMB * 1024 * 1024).toLong(),
                                totalBytes = (downloadState.totalMB * 1024 * 1024).toLong()
                            )

                            LinearProgressIndicator(
                                progress = { downloadState.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = NeonCyan,
                                trackColor = DarkSurfaceElevated
                            )
                        }
                    }
                }

                // Action Button (Download / Install)
                item {
                    Spacer(modifier = Modifier.height(6.dp))

                    if (!downloadState.isDownloading && !downloadState.isReadyToInstall) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    RemoteUpdateManager.startInAppDownloadAndInstall(context)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = TextPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "تحديث الآن مجاناً (APK مباشر)",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    } else if (downloadState.isReadyToInstall) {
                        Button(
                            onClick = {
                                RemoteUpdateManager.launchApkInstaller(context)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = DeepDarkBackground)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "تثبيت التحديث الآن ⚡",
                                color = DeepDarkBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = { /* In progress */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                            enabled = false
                        ) {
                            Text(
                                text = "جاري التحميل بأقصى سرعة... ${(downloadState.progress * 100).toInt()}%",
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
