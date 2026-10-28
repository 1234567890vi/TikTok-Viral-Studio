package com.creator.tiktokstudio.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creator.tiktokstudio.core.RemoteUpdateManager
import com.creator.tiktokstudio.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: (isUpdateRequired: Boolean) -> Unit
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "splashAnimations")

    // 1. Animated Floating Orbs in Background
    val orb1OffsetX by infiniteTransition.animateFloat(
        initialValue = -70f,
        targetValue = 70f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1X"
    )
    val orb1OffsetY by infiniteTransition.animateFloat(
        initialValue = -120f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(4800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1Y"
    )

    val orb2OffsetX by infiniteTransition.animateFloat(
        initialValue = 80f,
        targetValue = -60f,
        animationSpec = infiniteRepeatable(
            animation = tween(4400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2X"
    )
    val orb2OffsetY by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = -40f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2Y"
    )

    // 2. Central Logo Pulsing & Glow animations
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    // 3. Bottom Loading Bar shimmer progress
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )

    var remoteUpdateRequired by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Run remote config check in parallel during splash duration
        remoteUpdateRequired = RemoteUpdateManager.checkForRemoteUpdate(context)
        delay(2200)
        onSplashFinished(remoteUpdateRequired)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDarkBackground)
            .clickable { onSplashFinished(remoteUpdateRequired) },
        contentAlignment = Alignment.Center
    ) {
        // --- Floating Ambient Neon Orbs ---
        // Cyan Orb (#00F2FE)
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = orb1OffsetX.dp, y = orb1OffsetY.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.35f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        // Pink Orb (#FE0979)
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = orb2OffsetX.dp, y = orb2OffsetY.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonPink.copy(alpha = 0.30f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        // --- Center Content: Cinematic Glowing Emblem & Typography ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Central Logo with multi-layered neon rings
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                // Outermost radiating ambient glow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(NeonCyan.copy(alpha = glowAlpha), Color.Transparent)
                            )
                        )
                )

                // Rotating sweep-gradient border ring
                Box(
                    modifier = Modifier
                        .size(126.dp)
                        .rotate(ringRotation)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(NeonCyan, NeonPink, NeonPurple, NeonCyan)
                            )
                        )
                )

                // Inner dark glassmorphic badge container
                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .clip(CircleShape)
                        .background(Color(0xF00F0F16))
                        .border(
                            1.5.dp,
                            Brush.linearGradient(listOf(NeonCyan, NeonPink)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Download & Play icon fusion
                    Box(contentAlignment = Alignment.Center) {
                        // Offset Cyan Shadow
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = NeonCyan.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(56.dp)
                                .offset(x = (-2).dp, y = (-1).dp)
                        )
                        // Offset Pink Shadow
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = NeonPink.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(56.dp)
                                .offset(x = 2.dp, y = 1.dp)
                        )
                        // Crisp White Main Arrow
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Brand Name with glowing gradient feel
            Text(
                text = "Creator & TikTok Studio",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle: Clean & Elegant
            Text(
                text = "تحميل مباشر بدون علامة مائية بدقة 4K",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = NeonCyan,
                    letterSpacing = 0.2.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "بنية هجينة فائقة السرعة [C++ NDK + Python]",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMuted,
                    fontSize = 12.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Modern glowing progress bar at bottom
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x332A2A38))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.45f)
                        .offset(x = (shimmerProgress * 110).dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(NeonCyan, NeonPink, NeonCyan)
                            )
                        )
                )
            }
        }
    }
}
