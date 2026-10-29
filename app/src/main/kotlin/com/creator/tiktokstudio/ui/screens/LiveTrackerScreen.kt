package com.creator.tiktokstudio.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.creator.tiktokstudio.core.*
import com.creator.tiktokstudio.ui.components.LiveVideoPlayer
import com.creator.tiktokstudio.ui.components.ProcessingStepsBox
import com.creator.tiktokstudio.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

private val DangerRed = Color(0xFFFF1744)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackerScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = Localization.current
    val coroutineScope = rememberCoroutineScope()

    var inputUrl by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var roomData by remember { mutableStateOf<LiveRoomData?>(null) }
    var isCleanMode by remember { mutableStateOf(false) }

    // Floating hearts particles state
    var heartClickCount by remember { mutableIntStateOf(0) }

    // Live Battle Ticker State
    var battleTimeLeft by remember { mutableIntStateOf(240) }
    var punishmentTimeLeft by remember { mutableIntStateOf(180) }
    var isPkActive by remember { mutableStateOf(true) }
    var isPunishment by remember { mutableStateOf(false) }
    var hostScore by remember { mutableLongStateOf(24800L) }
    var opponentScore by remember { mutableLongStateOf(21950L) }
    var gloveTimeLeft by remember { mutableIntStateOf(45) }
    var mistTimeLeft by remember { mutableIntStateOf(28) }
    var isMistActive by remember { mutableStateOf(true) }

    // Live countdown loop
    LaunchedEffect(roomData) {
        if (roomData == null) return@LaunchedEffect
        battleTimeLeft = roomData!!.battleTimeLeftSec
        punishmentTimeLeft = roomData!!.punishmentTimeLeftSec
        isPkActive = roomData!!.isPkActive
        isPunishment = roomData!!.isPunishmentTime
        hostScore = roomData!!.hostScore
        opponentScore = roomData!!.opponentScore
        gloveTimeLeft = roomData!!.hostGloveTimeLeft
        mistTimeLeft = roomData!!.magicMistTimeLeft
        isMistActive = roomData!!.magicMistActive

        while (true) {
            delay(1000L)
            if (isPkActive) {
                if (battleTimeLeft > 0) {
                    battleTimeLeft--
                    // Random small score increments simulating live tapping
                    hostScore += Random.nextInt(20, 150) * if (gloveTimeLeft > 0) 3 else 1
                    opponentScore += Random.nextInt(15, 130)

                    if (gloveTimeLeft > 0) gloveTimeLeft--
                    if (mistTimeLeft > 0) {
                        mistTimeLeft--
                    } else {
                        isMistActive = false
                    }
                } else {
                    isPkActive = false
                    isPunishment = true
                }
            } else if (isPunishment) {
                if (punishmentTimeLeft > 0) {
                    punishmentTimeLeft--
                } else {
                    // Reset or round over
                    isPunishment = false
                    isPkActive = true
                    battleTimeLeft = 300
                    punishmentTimeLeft = 180
                }
            }
        }
    }

    // Floating heart button pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "liveAnims")
    val glovePulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glovePulse"
    )

    val mistFogAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mistAlpha"
    )

    fun startTracking() {
        if (inputUrl.isBlank()) {
            Toast.makeText(context, strings.emptyUrlToast, Toast.LENGTH_SHORT).show()
            return
        }
        val normalized = InputUtils.normalizeTikTokInput(inputUrl, InputTargetType.LIVE_TRACKER)
        isProcessing = true
        roomData = null

        coroutineScope.launch {
            val start = System.currentTimeMillis()
            val result = LiveStreamService.fetchLiveRoom(normalized)
            val elapsed = System.currentTimeMillis() - start
            if (elapsed < 3000L) {
                delay(3000L - elapsed)
            }
            delay(250L)
            isProcessing = false
            result.onSuccess {
                roomData = it
                if (it.isLiveOffline) {
                    Toast.makeText(context, strings.liveOfflineNotice, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "تم الاتصال بالبث والجولة بنجاح! 🔴", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                val rawMsg = it.localizedMessage ?: ""
                val err = when {
                    rawMsg.contains("USER_NOT_FOUND") || rawMsg.contains("غير موجود") || rawMsg.contains("not found") -> strings.userNotFoundOrPrivate
                    rawMsg.isNotBlank() -> rawMsg
                    else -> strings.inspectErrorFallback
                }
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Default load demo room if not loaded
    LaunchedEffect(Unit) {
        if (roomData == null && !isProcessing) {
            val res = LiveStreamService.fetchLiveRoom("@tiktok_hero_live")
            res.onSuccess { roomData = it }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepDarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section 1: Live Input & Connect Bar (Hidden in Clean Mode)
            if (!isCleanMode) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(DangerRed)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.liveHeaderTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.liveHeaderSubtitle,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = inputUrl,
                                onValueChange = { inputUrl = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(strings.liveInputPlaceholder, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.LiveTv, contentDescription = null, tint = NeonCyan)
                                },
                                trailingIcon = {
                                    if (inputUrl.isNotBlank()) {
                                        IconButton(onClick = { inputUrl = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted)
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { startTracking() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isProcessing
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = TextPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.startLiveTrackingBtn,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: 3-Second Processing Steps Box (Active when connecting)
            if (isProcessing) {
                item {
                    ProcessingStepsBox(
                        modifier = Modifier.fillMaxWidth(),
                        targetDurationMs = 3000L
                    )
                }
            }

            // Section 3: Live Room Player & Battle Display
            val currentRoom = roomData
            if (currentRoom != null && !isProcessing) {
                if (currentRoom.isLiveOffline) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.5.dp, GoldVip.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xF01A1828))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = GoldVip.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, GoldVip.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WifiOff,
                                            contentDescription = null,
                                            tint = GoldVip,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = strings.liveOfflineNotice,
                                            color = GoldVip,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                AsyncImage(
                                    model = currentRoom.hostAvatar,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .border(2.5.dp, Brush.sweepGradient(listOf(NeonCyan, NeonPink, GoldVip)), CircleShape),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = currentRoom.hostName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "@${currentRoom.hostUniqueId}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Medium
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSurfaceElevated)
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = formatMetric(currentRoom.followerCount),
                                            fontWeight = FontWeight.Bold,
                                            color = NeonCyan,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = strings.followersLabel,
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(26.dp)
                                            .background(GlassBorder)
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = formatMetric(currentRoom.likesCount),
                                            fontWeight = FontWeight.Bold,
                                            color = NeonPink,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = strings.likesLabel,
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(26.dp)
                                            .background(GlassBorder)
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "OFFLINE",
                                            fontWeight = FontWeight.Bold,
                                            color = TextMuted,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "حالة البث",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Button(
                                    onClick = {
                                        roomData = LiveStreamService.createDemoRoom(currentRoom.hostUniqueId)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = OnAccentDark)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.interactiveDemoBtn,
                                        color = OnAccentDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Top Live Info & Clean Mode Bar
                    item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = currentRoom.hostAvatar,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, DangerRed, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentRoom.hostName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = DangerRed
                                    ) {
                                        Text(
                                            text = "LIVE 🔴",
                                            color = TextPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "👥 ${formatMetric(currentRoom.viewerCount)} ${strings.viewersCountSuffix} • ❤️ ${formatMetric(currentRoom.likesCount)} ${strings.likesCountSuffix}",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Clean Mode Toggle Button
                        Button(
                            onClick = { isCleanMode = !isCleanMode },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCleanMode) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceElevated
                            ),
                            border = BorderStroke(1.dp, if (isCleanMode) NeonCyan else GlassBorder),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isCleanMode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = if (isCleanMode) NeonCyan else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCleanMode) strings.normalModeBtn else strings.cleanModeBtn,
                                fontSize = 11.sp,
                                color = if (isCleanMode) NeonCyan else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Section 4: PK Battle Status Pill & Countdown
                item {
                    val statusText = if (isPkActive) strings.battleActiveBadge else strings.punishmentTimeBadge
                    val timeLeft = if (isPkActive) battleTimeLeft else punishmentTimeLeft
                    val minutes = timeLeft / 60
                    val seconds = timeLeft % 60
                    val timerStr = String.format(Locale.US, "%02d:%02d", minutes, seconds)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.5.dp,
                                Brush.horizontalGradient(listOf(DangerRed, GoldVip, NeonCyan)),
                                RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color(0xF0181528))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isPkActive) Icons.Default.FlashOn else Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = if (isPkActive) GoldVip else DangerRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = statusText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isPkActive) GoldVip else DangerRed
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x33000000),
                                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                                ) {
                                    Text(
                                        text = "⏳ $timerStr",
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Interactive Split-Score Bar with Magic Mist & Glowing Gloves
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DarkSurfaceElevated)
                            ) {
                                val totalScore = (hostScore + opponentScore).coerceAtLeast(1L)
                                val hostFraction = (hostScore.toFloat() / totalScore.toFloat()).coerceIn(0.15f, 0.85f)

                                Row(modifier = Modifier.fillMaxSize()) {
                                    // Host Side (Red)
                                    Box(
                                        modifier = Modifier
                                            .weight(hostFraction)
                                            .fillMaxHeight()
                                            .background(
                                                Brush.horizontalGradient(listOf(Color(0xFFFF1744), Color(0xFFFF5252)))
                                            )
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = if (isMistActive) strings.magicMistHiddenScore else hostScore.toString(),
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    // Opponent Side (Blue)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f - hostFraction)
                                            .fillMaxHeight()
                                            .background(
                                                Brush.horizontalGradient(listOf(Color(0xFF2979FF), Color(0xFF00B0FF)))
                                            )
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = if (isMistActive) strings.magicMistHiddenScore else opponentScore.toString(),
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                // Middle Spark Divider
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⚡", fontSize = 14.sp)
                                }

                                // Magic Mist Animated Cloud Fog
                                if (isMistActive) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    listOf(
                                                        Color(0xEE4A148C),
                                                        Color(0xDD311B92),
                                                        Color(0xEE4A148C)
                                                    )
                                                ),
                                                alpha = mistFogAlpha
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("☁️", fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${strings.magicMistBadge} (${mistTimeLeft}s)",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Active Gloves & Badges Row
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Host Glove Active Indicator
                                if (gloveTimeLeft > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DangerRed.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, DangerRed),
                                        modifier = Modifier.scale(glovePulseScale)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text("🥊 ×3", fontWeight = FontWeight.Bold, color = DangerRed, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${gloveTimeLeft}s", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                } else {
                                    Text("قفاز خامل", color = TextMuted, fontSize = 10.sp)
                                }

                                // Winner Crown on Punishment Time
                                if (isPunishment) {
                                    val isHostWinner = hostScore >= opponentScore
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = GoldVip.copy(alpha = 0.25f),
                                        border = BorderStroke(1.dp, GoldVip)
                                    ) {
                                        Text(
                                            text = if (isHostWinner) "👑 الفائز: المضيف WIN" else "👑 الفائز: الخصم WIN",
                                            color = GoldVip,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Text(currentRoom.opponentName, color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Section 5: 60FPS Hardware Accelerated Video Player (Split-Screen during PK)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.5.dp, GlassBorder, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Hardware SurfaceView ExoPlayer
                            LiveVideoPlayer(
                                streamUrl = currentRoom.streamUrl,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Split-screen simulated opponent camera overlay on right side
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(width = 110.dp, height = 150.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, Color(0xFF2979FF), RoundedCornerShape(12.dp))
                                    .background(Color(0xDD111827))
                            ) {
                                AsyncImage(
                                    model = currentRoom.opponentAvatar,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xAA000000))
                                        .align(Alignment.BottomCenter)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentRoom.opponentName,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // 60FPS Floating Hearts Burst particles
                            if (heartClickCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 20.dp, bottom = 40.dp)
                                ) {
                                    FloatingHeart(label = heartClickCount.toString())
                                }
                            }

                            // Tap Heart Action Button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                            ) {
                                FloatingActionButton(
                                    onClick = {
                                        heartClickCount++
                                        hostScore += (Random.nextInt(15, 60) * if (gloveTimeLeft > 0) 3 else 1)
                                    },
                                    containerColor = NeonPink,
                                    shape = CircleShape,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Icon(Icons.Default.Favorite, contentDescription = strings.sendLikeBtn, tint = Color.White)
                                }
                            }
                        }
                    }
                }

                // Section 6: Top 3 MVP Contributors (Hidden in Clean Mode)
                if (!isCleanMode) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🏆 ${strings.topContributorsTitle}",
                                    fontWeight = FontWeight.Bold,
                                    color = GoldVip,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    currentRoom.hostTopContributors.forEach { c ->
                                        ContributorPill(
                                            contributor = c,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 7: Live Comments & Gifts Feed
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "💬 ${strings.liveChatTitle}",
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                currentRoom.liveComments.forEach { comment ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = comment.avatar,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = comment.nickname,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (comment.isGift) GoldVip else NeonCyan
                                            )
                                            Text(
                                                text = comment.text,
                                                fontSize = 11.sp,
                                                color = TextPrimary
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
    }
}
}

@Composable
private fun ContributorPill(
    contributor: LiveContributor,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceElevated,
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val badge = when (contributor.rank) {
                1 -> "🥇"
                2 -> "🥈"
                else -> "🥉"
            }
            Text(badge, fontSize = 12.sp)
            AsyncImage(
                model = contributor.avatar,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = contributor.nickname,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "💎 ${contributor.score}",
                fontSize = 9.sp,
                color = GoldVip,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FloatingHeart(label: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "heartRise$label")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -120f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offsetY"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .offset(y = offsetY.dp)
            .graphicsLayer { this.alpha = alpha }
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = NeonPink,
            modifier = Modifier.size(32.dp)
        )
    }
}

private fun formatMetric(num: Long): String {
    return when {
        num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0)
        num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0)
        else -> num.toString()
    }
}
