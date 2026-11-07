package com.creator.tiktokstudio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

// ==============================================================================
// 1. Real 3D Faceted Diamond Icon (Identical to TikTok Live Diamond)
// ==============================================================================
@Composable
fun RealDiamondIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Canvas(modifier = modifier.size(size)) {
        if (this.size.width <= 0f || this.size.height <= 0f || this.size.width.isNaN() || this.size.height.isNaN()) return@Canvas
        val w = this.size.width
        val h = this.size.height

        // Facet coordinates
        val pTopL = Offset(w * 0.28f, h * 0.12f)
        val pTopR = Offset(w * 0.72f, h * 0.12f)
        val pMidL = Offset(w * 0.04f, h * 0.42f)
        val pMidR = Offset(w * 0.96f, h * 0.42f)
        val pMidCL = Offset(w * 0.36f, h * 0.42f)
        val pMidCR = Offset(w * 0.64f, h * 0.42f)
        val pBottom = Offset(w * 0.50f, h * 0.96f)

        // Diamond Primary Gradient (Cyan -> Brilliant Sky Blue -> Royal Blue)
        val diamondGrad = Brush.linearGradient(
            colors = listOf(Color(0xFF00F2FE), Color(0xFF4FACFE), Color(0xFF0052D4)),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )

        // 1. Base Outline & Pavilion
        val basePath = Path().apply {
            moveTo(pTopL.x, pTopL.y)
            lineTo(pTopR.x, pTopR.y)
            lineTo(pMidR.x, pMidR.y)
            lineTo(pBottom.x, pBottom.y)
            lineTo(pMidL.x, pMidL.y)
            close()
        }
        drawPath(basePath, diamondGrad)

        // 2. Crown Table Facet (Top Center trapezoid)
        val tablePath = Path().apply {
            moveTo(pTopL.x, pTopL.y)
            lineTo(pTopR.x, pTopR.y)
            lineTo(pMidCR.x, pMidCR.y)
            lineTo(pMidCL.x, pMidCL.y)
            close()
        }
        drawPath(
            tablePath,
            Brush.linearGradient(
                colors = listOf(Color(0xFFE0F7FA), Color(0xFF80DEEA)),
                start = pTopL,
                end = pMidCR
            )
        )

        // 3. Upper Left Triangle
        val upperL = Path().apply {
            moveTo(pTopL.x, pTopL.y)
            lineTo(pMidCL.x, pMidCL.y)
            lineTo(pMidL.x, pMidL.y)
            close()
        }
        drawPath(upperL, Brush.linearGradient(listOf(Color(0xFFB2EBF2), Color(0xFF26C6DA))))

        // 4. Upper Right Triangle
        val upperR = Path().apply {
            moveTo(pTopR.x, pTopR.y)
            lineTo(pMidR.x, pMidR.y)
            lineTo(pMidCR.x, pMidCR.y)
            close()
        }
        drawPath(upperR, Brush.linearGradient(listOf(Color(0xFF4DD0E1), Color(0xFF0097A7))))

        // 5. Lower Center Pavilion Kite
        val lowerC = Path().apply {
            moveTo(pMidCL.x, pMidCL.y)
            lineTo(pMidCR.x, pMidCR.y)
            lineTo(pBottom.x, pBottom.y)
            close()
        }
        drawPath(
            lowerC,
            Brush.linearGradient(
                colors = listOf(Color(0xFF00E5FF), Color(0xFF0091EA)),
                start = Offset(w * 0.5f, h * 0.42f),
                end = pBottom
            )
        )

        // 6. Facet Highlights & Crisp Edges
        val edgeColor = Color.White.copy(alpha = 0.55f)
        val stroke = Stroke(width = w * 0.045f, cap = StrokeCap.Round)
        drawPath(basePath, edgeColor, style = stroke)
        drawLine(edgeColor, pTopL, pMidCL, strokeWidth = w * 0.035f)
        drawLine(edgeColor, pTopR, pMidCR, strokeWidth = w * 0.035f)
        drawLine(edgeColor, pMidCL, pMidCR, strokeWidth = w * 0.035f)
        drawLine(edgeColor, pMidL, pMidCL, strokeWidth = w * 0.035f)
        drawLine(edgeColor, pMidR, pMidCR, strokeWidth = w * 0.035f)
        drawLine(edgeColor, pMidCL, pBottom, strokeWidth = w * 0.035f)
        drawLine(edgeColor, pMidCR, pBottom, strokeWidth = w * 0.035f)

        // 7. Internal White Specular Glint (Top Table Sheen)
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = w * 0.07f,
            center = Offset(w * 0.42f, h * 0.28f)
        )
    }
}

// ==============================================================================
// 2. Real Eye / Viewer Icon with Animated Live Pulsing Dot
// ==============================================================================
@Composable
fun RealEyeViewerIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    isPulsing: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eyePulse")
    val pulseScale by if (isPulsing) {
        infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val pulseAlpha by if (isPulsing) {
        infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 0.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Canvas(modifier = modifier.size(size)) {
        if (this.size.width <= 0f || this.size.height <= 0f || this.size.width.isNaN() || this.size.height.isNaN()) return@Canvas
        val w = this.size.width
        val h = this.size.height

        // Outer Eye Sclera Path
        val eyePath = Path().apply {
            moveTo(w * 0.05f, h * 0.50f)
            cubicTo(w * 0.25f, h * 0.15f, w * 0.75f, h * 0.15f, w * 0.95f, h * 0.50f)
            cubicTo(w * 0.75f, h * 0.85f, w * 0.25f, h * 0.85f, w * 0.05f, h * 0.50f)
            close()
        }

        // Fill eye background with subtle glass tint
        drawPath(eyePath, Color(0x3300F2FE))

        // Eye Outline stroke
        drawPath(
            eyePath,
            brush = Brush.horizontalGradient(listOf(Color(0xFF00F2FE), Color(0xFF4FACFE))),
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        )

        // Central Iris
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF00F2FE), Color(0xFF0052D4)),
                center = Offset(w * 0.50f, h * 0.50f),
                radius = w * 0.22f
            ),
            radius = w * 0.20f,
            center = Offset(w * 0.50f, h * 0.50f)
        )

        // Pupil (Live animated dot inside)
        drawCircle(
            color = Color.White,
            radius = w * 0.08f,
            center = Offset(w * 0.46f, h * 0.44f)
        )

        // Pulsing Live Broadcast indicator at top-right of eye
        drawCircle(
            color = Color(0xFFFF1744).copy(alpha = pulseAlpha),
            radius = (w * 0.14f) * pulseScale,
            center = Offset(w * 0.88f, h * 0.20f)
        )
        drawCircle(
            color = Color(0xFFFF1744),
            radius = w * 0.09f,
            center = Offset(w * 0.88f, h * 0.20f)
        )
    }
}

// ==============================================================================
// 3. Real 3D Heart Like Icon with Tactile Pulse Animation
// ==============================================================================
@Composable
fun RealHeartLikeIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    isPulsing: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heartScalePulse")
    val pulseScale by if (isPulsing) {
        infiniteTransition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "heartPulse"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val finalModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else {
        modifier
    }

    Canvas(
        modifier = finalModifier
            .size(size)
            .scale(pulseScale)
    ) {
        if (this.size.width <= 0f || this.size.height <= 0f || this.size.width.isNaN() || this.size.height.isNaN()) return@Canvas
        val w = this.size.width
        val h = this.size.height

        val heartPath = Path().apply {
            moveTo(w * 0.5f, h * 0.88f)
            cubicTo(w * 0.15f, h * 0.62f, w * 0.02f, h * 0.38f, w * 0.06f, h * 0.20f)
            cubicTo(w * 0.10f, h * 0.04f, w * 0.35f, h * 0.02f, w * 0.50f, h * 0.25f)
            cubicTo(w * 0.65f, h * 0.02f, w * 0.90f, h * 0.04f, w * 0.94f, h * 0.20f)
            cubicTo(w * 0.98f, h * 0.38f, w * 0.85f, h * 0.62f, w * 0.50f, h * 0.88f)
            close()
        }

        // 3D TikTok Crimson-to-Rose Gradient
        val heartGrad = Brush.linearGradient(
            colors = listOf(Color(0xFFFF0844), Color(0xFFFF4E50), Color(0xFFFF8E72)),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
        drawPath(heartPath, heartGrad)

        // Specular highlight curve on left lobe
        val glossPath = Path().apply {
            moveTo(w * 0.22f, h * 0.16f)
            cubicTo(w * 0.30f, h * 0.12f, w * 0.40f, h * 0.16f, w * 0.42f, h * 0.26f)
        }
        drawPath(
            glossPath,
            color = Color.White.copy(alpha = 0.70f),
            style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        )
    }
}

// ==============================================================================
// 4. Real Glowing 3D Boxing Glove Icon (For PK Gloves Multiplier)
// ==============================================================================
@Composable
fun RealBoxingGloveIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    isFlaming: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gloveFlame")
    val flamePulse by if (isFlaming) {
        infiniteTransition.animateFloat(
            initialValue = 0.90f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "flamePulse"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Canvas(modifier = modifier.size(size).scale(flamePulse)) {
        if (this.size.width <= 0f || this.size.height <= 0f || this.size.width.isNaN() || this.size.height.isNaN()) return@Canvas
        val w = this.size.width
        val h = this.size.height

        // Outer Fiery Glow if active
        if (isFlaming) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x99FF1744), Color(0x33FF9100), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.75f
                ),
                radius = w * 0.7f,
                center = Offset(w * 0.5f, h * 0.5f)
            )
        }

        // Glove Fist Body
        val fistPath = Path().apply {
            moveTo(w * 0.30f, h * 0.90f)
            lineTo(w * 0.70f, h * 0.90f)
            lineTo(w * 0.75f, h * 0.65f)
            cubicTo(w * 0.95f, h * 0.55f, w * 0.95f, h * 0.15f, w * 0.55f, h * 0.10f)
            cubicTo(w * 0.25f, h * 0.10f, w * 0.10f, h * 0.35f, w * 0.12f, h * 0.55f)
            cubicTo(w * 0.12f, h * 0.70f, w * 0.25f, h * 0.75f, w * 0.30f, h * 0.90f)
            close()
        }

        // Crimson/Amber Glove Gradient
        drawPath(
            fistPath,
            Brush.linearGradient(
                colors = listOf(Color(0xFFFF1744), Color(0xFFD50000), Color(0xFFFF6D00)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        // Thumb section
        val thumbPath = Path().apply {
            moveTo(w * 0.15f, h * 0.45f)
            cubicTo(w * 0.05f, h * 0.50f, w * 0.05f, h * 0.68f, w * 0.25f, h * 0.68f)
            close()
        }
        drawPath(thumbPath, Color(0xFFFF5252))

        // Wrist Band (Golden strap)
        val wristPath = Path().apply {
            moveTo(w * 0.25f, h * 0.82f)
            lineTo(w * 0.75f, h * 0.82f)
            lineTo(w * 0.72f, h * 0.94f)
            lineTo(w * 0.28f, h * 0.94f)
            close()
        }
        drawPath(
            wristPath,
            Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA000)))
        )

        // Laces / Highlights
        drawLine(
            Color.White.copy(alpha = 0.7f),
            Offset(w * 0.40f, h * 0.82f),
            Offset(w * 0.60f, h * 0.82f),
            strokeWidth = w * 0.05f
        )
    }
}

// ==============================================================================
// 5. Real 3D Golden Regal Crown Icon (For PK Winner / MVP)
// ==============================================================================
@Composable
fun RealCrownIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        if (this.size.width <= 0f || this.size.height <= 0f || this.size.width.isNaN() || this.size.height.isNaN()) return@Canvas
        val w = this.size.width
        val h = this.size.height

        val crownPath = Path().apply {
            moveTo(w * 0.10f, h * 0.82f)
            lineTo(w * 0.90f, h * 0.82f)
            lineTo(w * 0.92f, h * 0.35f)
            lineTo(w * 0.70f, h * 0.55f)
            lineTo(w * 0.50f, h * 0.20f)
            lineTo(w * 0.30f, h * 0.55f)
            lineTo(w * 0.08f, h * 0.35f)
            close()
        }

        // 3D Metallic Gold Gradient
        val goldGrad = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFFF8F00)),
            startY = 0f,
            endY = h
        )
        drawPath(crownPath, goldGrad)

        // Crown Base Rim
        val rimPath = Path().apply {
            moveTo(w * 0.08f, h * 0.80f)
            lineTo(w * 0.92f, h * 0.80f)
            lineTo(w * 0.90f, h * 0.92f)
            lineTo(w * 0.10f, h * 0.92f)
            close()
        }
        drawPath(rimPath, Brush.horizontalGradient(listOf(Color(0xFFFF8F00), Color(0xFFFFD54F), Color(0xFFFF8F00))))

        // Crown Jewels on the three tips
        drawCircle(Color(0xFFFF1744), radius = w * 0.07f, center = Offset(w * 0.50f, h * 0.20f)) // Center Ruby
        drawCircle(Color(0xFF00E5FF), radius = w * 0.06f, center = Offset(w * 0.08f, h * 0.35f)) // Left Sapphire
        drawCircle(Color(0xFF00E5FF), radius = w * 0.06f, center = Offset(w * 0.92f, h * 0.35f)) // Right Sapphire

        // Highlights
        drawLine(
            Color.White.copy(alpha = 0.8f),
            Offset(w * 0.25f, h * 0.86f),
            Offset(w * 0.75f, h * 0.86f),
            strokeWidth = w * 0.04f,
            cap = StrokeCap.Round
        )
    }
}

// ==============================================================================
// 6. Real 3D Metallic Trophy / Medal Icon (Rank 1 Gold, Rank 2 Silver, Rank 3 Bronze)
// ==============================================================================
@Composable
fun RealTrophyIcon(
    rank: Int,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    val (primaryColors, ribbonColor) = when (rank) {
        1 -> Pair(
            listOf(Color(0xFFFFF176), Color(0xFFFFD54F), Color(0xFFFF8F00)),
            Color(0xFFFF1744) // Gold with Red Ribbon
        )
        2 -> Pair(
            listOf(Color(0xFFFFFFFF), Color(0xFFCFD8DC), Color(0xFF90A4AE)),
            Color(0xFF2979FF) // Silver with Blue Ribbon
        )
        else -> Pair(
            listOf(Color(0xFFFFCC80), Color(0xFFBCAAA4), Color(0xFF8D6E63)),
            Color(0xFF00B0FF) // Bronze with Cyan Ribbon
        )
    }

    Canvas(modifier = modifier.size(size)) {
        if (this.size.width <= 0f || this.size.height <= 0f || this.size.width.isNaN() || this.size.height.isNaN()) return@Canvas
        val w = this.size.width
        val h = this.size.height

        // Hanging Ribbon
        val ribbonL = Path().apply {
            moveTo(w * 0.35f, h * 0.05f)
            lineTo(w * 0.50f, h * 0.40f)
            lineTo(w * 0.25f, h * 0.40f)
            close()
        }
        val ribbonR = Path().apply {
            moveTo(w * 0.65f, h * 0.05f)
            lineTo(w * 0.75f, h * 0.40f)
            lineTo(w * 0.50f, h * 0.40f)
            close()
        }
        drawPath(ribbonL, ribbonColor)
        drawPath(ribbonR, ribbonColor.copy(alpha = 0.85f))

        // Medal Medallion (Circle)
        drawCircle(
            brush = Brush.radialGradient(primaryColors, center = Offset(w * 0.5f, h * 0.62f), radius = w * 0.38f),
            radius = w * 0.35f,
            center = Offset(w * 0.5f, h * 0.62f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.6f),
            radius = w * 0.35f,
            center = Offset(w * 0.5f, h * 0.62f),
            style = Stroke(width = w * 0.05f)
        )

        // Embossed Star in center of medal
        val starCenter = Offset(w * 0.5f, h * 0.62f)
        drawCircle(color = Color.White.copy(alpha = 0.9f), radius = w * 0.12f, center = starCenter)
    }
}

// ==============================================================================
// 7. Real Webcast Gift Badge (Real Official Webcast CDN / Asset Image)
// ==============================================================================
@Composable
fun RealWebcastGiftBadge(
    giftName: String,
    modifier: Modifier = Modifier,
    iconUrl: String? = null,
    size: Dp = 32.dp
) {
    // Official High-Definition TikTok Webcast CDN Asset Mappings
    val officialCdnUrl = when (giftName.lowercase()) {
        "rose", "وردة", "وردة تيك توك" -> "https://p16-webcast.tiktokcdn.com/img/maliva/webcast-va/47ea09b304cbe200d7da00f7b9ebc7ad~tplv-obj.png"
        "lion", "أسد", "أسد تيك توك" -> "https://p16-webcast.tiktokcdn.com/img/maliva/webcast-va/7069389456073165595~tplv-obj.png"
        "universe", "مجرة", "universe 🌌" -> "https://p16-webcast.tiktokcdn.com/img/maliva/webcast-va/7069389476100983578~tplv-obj.png"
        "whale", "حوت", "whale 🐳" -> "https://p16-webcast.tiktokcdn.com/img/maliva/webcast-va/7074218320498707227~tplv-obj.png"
        "sports car", "سيارة", "car" -> "https://p16-webcast.tiktokcdn.com/img/maliva/webcast-va/7069389484833508122~tplv-obj.png"
        else -> iconUrl ?: "https://p16-webcast.tiktokcdn.com/img/maliva/webcast-va/47ea09b304cbe200d7da00f7b9ebc7ad~tplv-obj.png"
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = officialCdnUrl,
            contentDescription = giftName,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentScale = ContentScale.Fit
        )
    }
}

// ==============================================================================
// 8. Real Crossed Battle Swords Icon (For PK Battle / 1v1 / 2v2)
// ==============================================================================
@Composable
fun RealBattleSwordsIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Canvas(modifier = modifier.size(size)) {
        if (this.size.width <= 0f || this.size.height <= 0f || this.size.width.isNaN() || this.size.height.isNaN()) return@Canvas
        val w = this.size.width
        val h = this.size.height

        // Sword 1 (Red/NeonPink)
        drawLine(
            brush = Brush.linearGradient(listOf(Color(0xFFFF0055), Color(0xFFFF5252))),
            start = Offset(w * 0.15f, h * 0.15f),
            end = Offset(w * 0.85f, h * 0.85f),
            strokeWidth = w * 0.12f,
            cap = StrokeCap.Round
        )

        // Sword 2 (Cyan/Blue)
        drawLine(
            brush = Brush.linearGradient(listOf(Color(0xFF00F2FE), Color(0xFF2979FF))),
            start = Offset(w * 0.85f, h * 0.15f),
            end = Offset(w * 0.15f, h * 0.85f),
            strokeWidth = w * 0.12f,
            cap = StrokeCap.Round
        )

        // Center Collision Spark
        drawCircle(
            color = Color.White,
            radius = w * 0.15f,
            center = Offset(w * 0.5f, h * 0.5f)
        )
    }
}

// ==============================================================================
// 9. Real Clock / Stopwatch Timer Icon (For PK Countdown)
// ==============================================================================
@Composable
fun RealClockIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    Canvas(modifier = modifier.size(size)) {
        if (this.size.width <= 0f || this.size.height <= 0f || this.size.width.isNaN() || this.size.height.isNaN()) return@Canvas
        val w = this.size.width
        val h = this.size.height

        // Dial rim
        drawCircle(
            color = Color(0xFFFFD700),
            radius = w * 0.42f,
            center = Offset(w * 0.5f, h * 0.54f),
            style = Stroke(width = w * 0.10f)
        )

        // Top button
        drawLine(
            color = Color(0xFFFFD700),
            start = Offset(w * 0.5f, h * 0.05f),
            end = Offset(w * 0.5f, h * 0.14f),
            strokeWidth = w * 0.12f,
            cap = StrokeCap.Round
        )

        // Minute & Hour hands
        drawLine(
            color = Color.White,
            start = Offset(w * 0.5f, h * 0.54f),
            end = Offset(w * 0.5f, h * 0.28f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF00F2FE),
            start = Offset(w * 0.5f, h * 0.54f),
            end = Offset(w * 0.72f, h * 0.54f),
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round
        )
    }
}

// ==============================================================================
// 10. Real Chat Bubble Icon
// ==============================================================================
@Composable
fun RealChatBubbleIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    Canvas(modifier = modifier.size(size)) {
        if (this.size.width <= 0f || this.size.height <= 0f || this.size.width.isNaN() || this.size.height.isNaN()) return@Canvas
        val w = this.size.width
        val h = this.size.height

        val bubblePath = Path().apply {
            moveTo(w * 0.15f, h * 0.20f)
            lineTo(w * 0.85f, h * 0.20f)
            cubicTo(w * 0.95f, h * 0.20f, w * 0.95f, h * 0.70f, w * 0.85f, h * 0.70f)
            lineTo(w * 0.45f, h * 0.70f)
            lineTo(w * 0.25f, h * 0.92f)
            lineTo(w * 0.28f, h * 0.70f)
            lineTo(w * 0.15f, h * 0.70f)
            cubicTo(w * 0.05f, h * 0.70f, w * 0.05f, h * 0.20f, w * 0.15f, h * 0.20f)
            close()
        }

        drawPath(
            bubblePath,
            Brush.linearGradient(listOf(Color(0xFF00F2FE), Color(0xFF4FACFE)))
        )

        // 3 Chat message dots
        drawCircle(Color.White, radius = w * 0.05f, center = Offset(w * 0.35f, h * 0.45f))
        drawCircle(Color.White, radius = w * 0.05f, center = Offset(w * 0.50f, h * 0.45f))
        drawCircle(Color.White, radius = w * 0.05f, center = Offset(w * 0.65f, h * 0.45f))
    }
}

// ==============================================================================
// 11. Real TikTok Downloader Logo (3D Chromatic Musical Note + Download Arrow)
// ==============================================================================
@Composable
fun TikTokDownloaderLogo(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF161528), Color(0xFF090812)),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(
                BorderStroke(
                    1.2.dp,
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF00F2FE), Color(0xFFFE0979))
                    )
                ),
                RoundedCornerShape(size * 0.28f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(size * 0.14f)
        ) {
            if (this.size.width <= 0f || this.size.height <= 0f || this.size.width.isNaN() || this.size.height.isNaN()) return@Canvas
            val w = this.size.width
            val h = this.size.height

            fun createNotePath(dx: Float, dy: Float): Path = Path().apply {
                val headCenterX = w * 0.40f + dx
                val headCenterY = h * 0.50f + dy
                val headRadiusX = w * 0.22f
                val headRadiusY = h * 0.17f
                val stemLeft = w * 0.52f + dx
                val stemRight = w * 0.64f + dx
                val stemTop = h * 0.12f + dy
                val flagEndX = w * 0.88f + dx
                val flagEndY = h * 0.30f + dy

                moveTo(stemLeft, headCenterY)
                cubicTo(
                    headCenterX - headRadiusX, headCenterY + headRadiusY * 0.2f,
                    headCenterX - headRadiusX, headCenterY + headRadiusY,
                    headCenterX, headCenterY + headRadiusY
                )
                cubicTo(
                    headCenterX + headRadiusX, headCenterY + headRadiusY,
                    stemRight, headCenterY + headRadiusY * 0.4f,
                    stemRight, headCenterY
                )
                lineTo(stemRight, stemTop)
                cubicTo(
                    stemRight + w * 0.12f, stemTop,
                    flagEndX, stemTop + h * 0.08f,
                    flagEndX, flagEndY
                )
                lineTo(flagEndX - w * 0.08f, flagEndY)
                cubicTo(
                    flagEndX - w * 0.08f, stemTop + h * 0.12f,
                    stemLeft + w * 0.10f, stemTop + h * 0.08f,
                    stemLeft, stemTop + h * 0.12f
                )
                lineTo(stemLeft, headCenterY)
                close()
            }

            fun createArrowPath(dx: Float, dy: Float): Path = Path().apply {
                val cx = w * 0.50f + dx
                val shaftTop = h * 0.58f + dy
                val shaftWidth = w * 0.07f
                val arrowHeadY = h * 0.86f + dy
                val arrowWingL = cx - w * 0.18f
                val arrowWingR = cx + w * 0.18f
                val arrowWingTop = h * 0.74f + dy

                moveTo(cx - shaftWidth, shaftTop)
                lineTo(cx + shaftWidth, shaftTop)
                lineTo(cx + shaftWidth, arrowWingTop)
                lineTo(arrowWingR, arrowWingTop)
                lineTo(cx, arrowHeadY)
                lineTo(arrowWingL, arrowWingTop)
                lineTo(cx - shaftWidth, arrowWingTop)
                close()
            }

            fun createTrayPath(dx: Float, dy: Float): Path = Path().apply {
                val trayY = h * 0.94f + dy
                moveTo(w * 0.25f + dx, trayY)
                lineTo(w * 0.75f + dx, trayY)
            }

            val glitch = w * 0.045f
            val cyanColor = Color(0xFF00F2FE)
            val pinkColor = Color(0xFFFE0979)

            // 1. Cyan Glitch
            drawPath(createNotePath(-glitch, -glitch * 0.5f), cyanColor)
            drawPath(createArrowPath(-glitch, -glitch * 0.5f), cyanColor)
            drawPath(createTrayPath(-glitch, -glitch * 0.5f), cyanColor, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round))

            // 2. Pink Glitch
            drawPath(createNotePath(glitch, glitch * 0.5f), pinkColor)
            drawPath(createArrowPath(glitch, glitch * 0.5f), pinkColor)
            drawPath(createTrayPath(glitch, glitch * 0.5f), pinkColor, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round))

            // 3. Crisp White Core
            drawPath(createNotePath(0f, 0f), Color.White)
            drawPath(createArrowPath(0f, 0f), Color.White)
            drawPath(createTrayPath(0f, 0f), Color.White, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round))
        }
    }
}

// ==============================================================================
// 12. Real Instagram Camera Vector Icon (Official Gradient: Purple -> Pink -> Orange)
// ==============================================================================
@Composable
fun RealInstagramIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        if (this.size.width <= 0f || this.size.height <= 0f || this.size.width.isNaN() || this.size.height.isNaN()) return@Canvas
        val w = this.size.width
        val h = this.size.height

        val igBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF833AB4), // Purple
                Color(0xFFFD1D1D), // Pink/Red
                Color(0xFFFCB045)  // Orange/Yellow
            ),
            start = Offset(0f, h),
            end = Offset(w, 0f)
        )

        val strokeWidth = w * 0.095f
        val cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.28f, h * 0.28f)

        // Outer rounded rectangle
        drawRoundRect(
            brush = igBrush,
            topLeft = Offset(strokeWidth * 0.5f, strokeWidth * 0.5f),
            size = Size(w - strokeWidth, h - strokeWidth),
            cornerRadius = cornerRadius,
            style = Stroke(width = strokeWidth)
        )

        // Center lens circle
        val centerPoint = Offset(w * 0.5f, h * 0.5f)
        val lensRadius = w * 0.22f
        drawCircle(
            brush = igBrush,
            radius = lensRadius,
            center = centerPoint,
            style = Stroke(width = strokeWidth)
        )

        // Top right flash dot
        val flashCenter = Offset(w * 0.74f, h * 0.26f)
        val flashRadius = w * 0.055f
        drawCircle(
            brush = igBrush,
            radius = flashRadius,
            center = flashCenter,
            style = Fill
        )
    }
}

// ==============================================================================
// 13. Instagram Studio 3D Badge Logo
// ==============================================================================
@Composable
fun InstagramStudioLogo(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF833AB4),
                        Color(0xFFE1306C),
                        Color(0xFFFD1D1D),
                        Color(0xFFFCB045)
                    ),
                    start = Offset(0f, size.value * 2),
                    end = Offset(size.value * 2, 0f)
                )
            )
            .border(
                BorderStroke(
                    1.2.dp,
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFFFDF00), Color(0xFFFF007F))
                    )
                ),
                RoundedCornerShape(size * 0.28f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(size * 0.20f)
        ) {
            if (this.size.width <= 0f || this.size.height <= 0f || this.size.width.isNaN() || this.size.height.isNaN()) return@Canvas
            val w = this.size.width
            val h = this.size.height
            val strokeWidth = w * 0.11f
            val cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.30f, h * 0.30f)

            // Crisp White Camera body
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(strokeWidth * 0.5f, strokeWidth * 0.5f),
                size = Size(w - strokeWidth, h - strokeWidth),
                cornerRadius = cornerRadius,
                style = Stroke(width = strokeWidth)
            )

            // Center lens
            drawCircle(
                color = Color.White,
                radius = w * 0.24f,
                center = Offset(w * 0.5f, h * 0.5f),
                style = Stroke(width = strokeWidth)
            )

            // Flash dot
            drawCircle(
                color = Color.White,
                radius = w * 0.065f,
                center = Offset(w * 0.74f, h * 0.26f),
                style = Fill
            )
        }
    }
}

// ==============================================================================
// 14. Real Verified Badge Icon (Authentic Blue Rosette + White Checkmark)
// ==============================================================================
@Composable
fun RealVerifiedBadgeIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    Canvas(modifier = modifier.size(size)) {
        if (this.size.width <= 0f || this.size.height <= 0f || this.size.width.isNaN() || this.size.height.isNaN()) return@Canvas
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val r = w * 0.48f

        // Draw 8-point scalloped star / rosette
        val rosettePath = Path()
        val numPoints = 16
        for (i in 0 until numPoints) {
            val angle = (i * 2.0 * Math.PI / numPoints) - (Math.PI / 2.0)
            val currentR = if (i % 2 == 0) r else r * 0.86f
            val px = (cx + currentR * kotlin.math.cos(angle)).toFloat()
            val py = (cy + currentR * kotlin.math.sin(angle)).toFloat()
            if (i == 0) rosettePath.moveTo(px, py) else rosettePath.lineTo(px, py)
        }
        rosettePath.close()

        // Authentic Verified Blue Gradient
        drawPath(
            rosettePath,
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF29B6F6), Color(0xFF0095F6), Color(0xFF1976D2)),
                center = Offset(cx, cy),
                radius = r
            )
        )

        // Crisp White Checkmark
        val checkPath = Path().apply {
            moveTo(w * 0.28f, h * 0.52f)
            lineTo(w * 0.44f, h * 0.68f)
            lineTo(w * 0.74f, h * 0.35f)
        }
        drawPath(
            checkPath,
            color = Color.White,
            style = Stroke(
                width = w * 0.12f,
                cap = StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}
