package com.creator.tiktokstudio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creator.tiktokstudio.core.Localization
import com.creator.tiktokstudio.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun ProcessingStepsBox(
    modifier: Modifier = Modifier,
    targetDurationMs: Long = 3000L,
    onCompleted: (() -> Unit)? = null
) {
    val strings = Localization.current

    // Digital timer from 0 to 3000ms
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var hasTriggeredCallback by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (true) {
            val now = System.currentTimeMillis()
            val current = (now - startTime).coerceAtMost(targetDurationMs)
            elapsedMs = current
            if (current >= targetDurationMs) {
                if (!hasTriggeredCallback) {
                    hasTriggeredCallback = true
                    onCompleted?.invoke()
                }
                break
            }
            delay(16L) // ~60 FPS
        }
    }

    val progress = (elapsedMs.toFloat() / targetDurationMs.toFloat()).coerceIn(0f, 1f)
    val elapsedSeconds = elapsedMs / 1000f

    // Animations for Canvas Sine Waves & Rotating Icons
    val infiniteTransition = rememberInfiniteTransition(label = "processingWaves")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Pre-evaluate theme colors in Composable scope before Canvas draw scope
    val cyanColor = NeonCyan
    val pinkColor = NeonPink
    val trackBg = DarkSurfaceElevated
    val cardBg = DarkSurfaceCard

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(
                BorderStroke(
                    1.5.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.9f),
                            NeonPink.copy(alpha = 0.9f),
                            GoldVip.copy(alpha = 0.8f)
                        )
                    )
                ),
                RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Status Indicator & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen.copy(alpha = pulseAlpha))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.algoBoxTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = strings.algoBoxSubtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Sine Waves Animation (Cyan #00F2FE and Pink #FE0979)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(trackBg)
                    .border(1.dp, GlassBorder.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (size.width <= 0f || size.height <= 0f || size.width.isNaN() || size.height.isNaN()) return@Canvas
                    val width = size.width
                    val height = size.height
                    val midY = height / 2f

                    // Subtle matrix points
                    val cols = 12
                    val stepX = width / cols
                    for (i in 0..cols) {
                        drawCircle(
                            color = cyanColor.copy(alpha = 0.14f),
                            radius = 2.dp.toPx(),
                            center = Offset(i * stepX, midY)
                        )
                    }

                    // 1. Cyan #00F2FE Sine Wave
                    val cyanPath = Path()
                    val amplitude1 = height * 0.28f
                    val waveLength1 = width * 0.55f

                    for (x in 0..width.toInt() step 4) {
                        val xf = x.toFloat()
                        val angle = (xf / waveLength1) * 2f * PI.toFloat() + wavePhase
                        val y = midY + amplitude1 * sin(angle)
                        if (x == 0) cyanPath.moveTo(xf, y) else cyanPath.lineTo(xf, y)
                    }
                    drawPath(
                        path = cyanPath,
                        color = cyanColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 2. Pink #FE0979 Sine Wave
                    val pinkPath = Path()
                    val amplitude2 = height * 0.22f
                    val waveLength2 = width * 0.45f

                    for (x in 0..width.toInt() step 4) {
                        val xf = x.toFloat()
                        val angle = (xf / waveLength2) * 2f * PI.toFloat() + wavePhase + (PI / 2).toFloat()
                        val y = midY + amplitude2 * sin(angle)
                        if (x == 0) pinkPath.moveTo(xf, y) else pinkPath.lineTo(xf, y)
                    }
                    drawPath(
                        path = pinkPath,
                        color = pinkColor.copy(alpha = 0.9f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Circular Digital 3.0s Timer & Progress Sweep Ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(92.dp)
            ) {
                Canvas(modifier = Modifier.size(86.dp)) {
                    if (size.width <= 0f || size.height <= 0f || size.width.isNaN() || size.height.isNaN()) return@Canvas
                    // Track circle
                    drawArc(
                        color = trackBg,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Progress sweep arc
                    val sweepBrush = Brush.sweepGradient(
                        listOf(cyanColor, pinkColor, GoldVip, cyanColor)
                    )
                    drawArc(
                        brush = sweepBrush,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Digital Timer Counter (0.0s -> 3.0s & 0% -> 100%)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (progress >= 1.0f) {
                        Text(
                            text = "100%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = SuccessGreen
                            )
                        )
                        Text(text = "✨", fontSize = 12.sp)
                    } else {
                        Text(
                            text = String.format(Locale.US, "%.1fs", elapsedSeconds),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = NeonPink,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4 Sequential Processing Stages (each completes within 0.75s)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stage 1: 0.0s - 0.75s
                ProcessingStepItem(
                    text = strings.procStep1,
                    isCompleted = elapsedMs >= 750L,
                    isActive = elapsedMs in 0L..749L,
                    spinAngle = spinAngle
                )

                // Stage 2: 0.75s - 1.50s
                ProcessingStepItem(
                    text = strings.procStep2,
                    isCompleted = elapsedMs >= 1500L,
                    isActive = elapsedMs in 750L..1499L,
                    spinAngle = spinAngle
                )

                // Stage 3: 1.50s - 2.25s
                ProcessingStepItem(
                    text = strings.procStep3,
                    isCompleted = elapsedMs >= 2250L,
                    isActive = elapsedMs in 1500L..2249L,
                    spinAngle = spinAngle
                )

                // Stage 4: 2.25s - 3.00s
                ProcessingStepItem(
                    text = if (progress >= 1f) strings.procStepCompleted else strings.procStep4,
                    isCompleted = elapsedMs >= 3000L,
                    isActive = elapsedMs in 2250L..2999L,
                    spinAngle = spinAngle
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Progress Gradient Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(trackBg)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progress)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(NeonCyan, NeonPink, GoldVip)
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun ProcessingStepItem(
    text: String,
    isCompleted: Boolean,
    isActive: Boolean,
    spinAngle: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        when {
            isCompleted -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
            isActive -> {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(spinAngle)
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = TextMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = when {
                    isCompleted -> TextPrimary
                    isActive -> NeonCyan
                    else -> TextMuted
                },
                fontWeight = if (isCompleted || isActive) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 12.5.sp
            ),
            modifier = Modifier.weight(1f)
        )
    }
}
