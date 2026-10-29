package com.creator.tiktokstudio.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.creator.tiktokstudio.ui.components.*
import com.creator.tiktokstudio.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

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

    // Reactive Webcast Event States (Updated strictly via SharedFlow - Zero Polling)
    var battleTimeLeft by remember { mutableIntStateOf(285) }
    var punishmentTimeLeft by remember { mutableIntStateOf(180) }
    var isPkActive by remember { mutableStateOf(false) }
    var battleMode by remember { mutableStateOf("1v1") }
    var isPunishment by remember { mutableStateOf(false) }
    var hostScore by remember { mutableLongStateOf(0L) }
    var opponentScore by remember { mutableLongStateOf(0L) }
    var gloveTimeLeft by remember { mutableIntStateOf(0) }
    var mistTimeLeft by remember { mutableIntStateOf(0) }
    var isMistActive by remember { mutableStateOf(false) }
    var totalViewers by remember { mutableLongStateOf(0L) }
    var totalLikes by remember { mutableLongStateOf(0L) }
    var totalDiamonds by remember { mutableLongStateOf(0L) }
    var topContributors by remember { mutableStateOf<List<LiveContributor>>(emptyList()) }
    var liveCommentsList by remember { mutableStateOf<List<LiveComment>>(emptyList()) }
    var lastGiftCombo by remember { mutableStateOf<WebcastLiveEvent.Gift?>(null) }

    // Connect & Manage Real-time WebSocket LifeCycle
    DisposableEffect(roomData) {
        val current = roomData
        if (current != null && !current.isLiveOffline) {
            hostScore = current.hostScore
            opponentScore = current.opponentScore
            battleTimeLeft = current.battleTimeLeftSec
            punishmentTimeLeft = current.punishmentTimeLeftSec
            isPkActive = current.isPkActive
            battleMode = current.battleMode
            isPunishment = current.isPunishmentTime
            gloveTimeLeft = current.hostGloveTimeLeft
            mistTimeLeft = current.magicMistTimeLeft
            isMistActive = current.magicMistActive
            totalViewers = current.viewerCount
            totalLikes = current.likesCount
            totalDiamonds = current.totalDiamonds
            topContributors = current.hostTopContributors
            liveCommentsList = current.liveComments

            LiveStreamService.startRealtimeLiveSession(current)
        }
        onDispose {
            LiveStreamService.stopRealtimeLiveSession()
        }
    }

    // Collect Instant Push Events from Kotlin SharedFlow (0ms Delay)
    LaunchedEffect(roomData) {
        val current = roomData
        if (current == null || current.isLiveOffline) return@LaunchedEffect

        LiveStreamService.eventsFlow.collect { event ->
            when (event) {
                is WebcastLiveEvent.Comment -> {
                    liveCommentsList = (listOf(LiveComment(event.id, event.nickname, event.avatar, event.text)) + liveCommentsList).take(30)
                }
                is WebcastLiveEvent.Gift -> {
                    lastGiftCombo = event
                    coroutineScope.launch {
                        delay(3500L)
                        if (lastGiftCombo?.id == event.id) {
                            lastGiftCombo = null
                        }
                    }
                    totalDiamonds = event.totalDiamonds
                    liveCommentsList = (listOf(
                        LiveComment(
                            id = event.id,
                            nickname = event.nickname,
                            avatar = event.avatar,
                            text = "أرسل ${event.giftName} ×${event.comboCount}",
                            isGift = true,
                            giftName = event.giftName,
                            giftIconUrl = event.iconUrl,
                            comboCount = event.comboCount
                        )
                    ) + liveCommentsList).take(30)
                }
                is WebcastLiveEvent.Like -> {
                    totalLikes = event.totalLikes
                }
                is WebcastLiveEvent.ViewerCount -> {
                    totalViewers = event.count
                }
                is WebcastLiveEvent.BattleScore -> {
                    hostScore = event.hostScore
                    opponentScore = event.opponentScore
                    if (event.totalDiamonds > 0L) totalDiamonds = event.totalDiamonds
                }
                is WebcastLiveEvent.GloveState -> {
                    gloveTimeLeft = event.timeLeftSec
                }
                is WebcastLiveEvent.MagicMistState -> {
                    isMistActive = event.isActive
                    mistTimeLeft = event.timeLeftSec
                }
                is WebcastLiveEvent.BattleTimerSync -> {
                    isPkActive = event.isBattleActive
                    battleTimeLeft = event.battleTimeLeftSec
                    isPunishment = event.isPunishment
                    punishmentTimeLeft = event.punishmentTimeLeftSec
                }
                is WebcastLiveEvent.TopContributorsUpdate -> {
                    topContributors = event.hostContributors
                }
                is WebcastLiveEvent.ConnectionState -> {}
            }
        }
    }

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
                    Toast.makeText(context, "تم الاتصال بالبث المباشر (WebSocket 0ms)! 🔴", Toast.LENGTH_SHORT).show()
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
                    // Top Live Info Bar (Isolated Composable)
                    item {
                        LiveRoomHeaderBar(
                            hostAvatar = currentRoom.hostAvatar,
                            hostName = currentRoom.hostName,
                            isCleanMode = isCleanMode,
                            onToggleCleanMode = { isCleanMode = !isCleanMode },
                            strings = strings
                        )
                    }

                    // Section 4: The 3 Real-Time Telemetry Cards Directly Above Video
                    item {
                        TopTelemetryBar(
                            viewerCount = totalViewers,
                            likesCount = totalLikes,
                            diamondsCount = totalDiamonds,
                            strings = strings
                        )
                    }

                    // Section 5: PK Battle Status Bar (Displayed when PK Battle is Active)
                    if (isPkActive) {
                        item {
                            PkBattleScoreBar(
                                hostScore = hostScore,
                                opponentScore = opponentScore,
                                isMistActive = isMistActive,
                                mistTimeLeft = mistTimeLeft,
                                gloveTimeLeft = gloveTimeLeft,
                                isPunishment = isPunishment,
                                battleTimeLeft = battleTimeLeft,
                                punishmentTimeLeft = punishmentTimeLeft,
                                battleMode = battleMode,
                                opponentName = currentRoom.opponentName.ifBlank { "منافس الجولة" },
                                strings = strings
                            )
                        }
                    }

                    // Section 6: Full 9:16 Portrait Video Player & Overlays
                    item {
                        LivePlayerWithOverlays(
                            streamUrl = currentRoom.streamUrl,
                            opponentStreamUrl = currentRoom.opponentStreamUrl,
                            isPkActive = isPkActive,
                            hostName = currentRoom.hostName,
                            opponentName = currentRoom.opponentName,
                            opponentAvatar = currentRoom.opponentAvatar,
                            heartClickCount = heartClickCount,
                            onHeartClick = {
                                heartClickCount++
                            },
                            lastComboEvent = lastGiftCombo
                        )
                    }

                    // Section 7: Top MVP Contributors (Hidden in Clean Mode)
                    if (!isCleanMode) {
                        if (topContributors.isNotEmpty() || currentRoom.hostTopContributors.isNotEmpty()) {
                            item {
                                TopContributorsSection(
                                    contributors = if (topContributors.isNotEmpty()) topContributors else currentRoom.hostTopContributors,
                                    strings = strings
                                )
                            }
                        }

                        // Section 8: Live Comments & Real-Time Gifts Feed
                        if (liveCommentsList.isNotEmpty() || currentRoom.liveComments.isNotEmpty()) {
                            item {
                                LiveCommentsSection(
                                    comments = if (liveCommentsList.isNotEmpty()) liveCommentsList else currentRoom.liveComments,
                                    strings = strings
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// Isolated 60FPS UI Components (Custom 3D / Vector Canvas Icons)
// ==============================================================================

@Composable
fun TopTelemetryBar(
    viewerCount: Long,
    likesCount: Long,
    diamondsCount: Long,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { clip = true },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Card 1: Viewers Now
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = DarkSurfaceCard,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RealEyeViewerIcon(size = 20.dp, isPulsing = true)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = formatMetric(viewerCount),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = strings.viewersCountSuffix,
                        color = TextSecondary,
                        fontSize = 9.5.sp
                    )
                }
            }
        }

        // Card 2: Cumulative Likes
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = DarkSurfaceCard,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RealHeartLikeIcon(size = 20.dp, isPulsing = true)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = formatMetric(likesCount),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = strings.likesCountSuffix,
                        color = TextSecondary,
                        fontSize = 9.5.sp
                    )
                }
            }
        }

        // Card 3: Total Support / Diamonds
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = DarkSurfaceCard,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RealDiamondIcon(size = 20.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = formatMetric(diamondsCount),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00F2FE),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "الألماس",
                        color = TextSecondary,
                        fontSize = 9.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LiveRoomHeaderBar(
    hostAvatar: String,
    hostName: String,
    isCleanMode: Boolean,
    onToggleCleanMode: () -> Unit,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { clip = true },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            AsyncImage(
                model = hostAvatar,
                contentDescription = null,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, DangerRed, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = hostName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DangerRed
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE",
                                color = TextPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Clean Mode Toggle Button
        Button(
            onClick = onToggleCleanMode,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCleanMode) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceElevated
            ),
            border = BorderStroke(1.dp, if (isCleanMode) NeonCyan else GlassBorder),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = if (isCleanMode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = null,
                tint = if (isCleanMode) NeonCyan else TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isCleanMode) strings.normalModeBtn else strings.cleanModeBtn,
                fontSize = 11.sp,
                color = if (isCleanMode) NeonCyan else TextSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PkBattleScoreBar(
    hostScore: Long,
    opponentScore: Long,
    isMistActive: Boolean,
    mistTimeLeft: Int,
    gloveTimeLeft: Int,
    isPunishment: Boolean,
    battleTimeLeft: Int,
    punishmentTimeLeft: Int,
    battleMode: String,
    opponentName: String,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val mistFogAlpha by rememberInfiniteTransition(label = "mistFog").animateFloat(
        initialValue = 0.60f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mistAlpha"
    )

    val timeLeft = if (!isPunishment) battleTimeLeft else punishmentTimeLeft
    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val timerStr = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.5.dp,
                Brush.horizontalGradient(listOf(DangerRed, GoldVip, NeonCyan)),
                RoundedCornerShape(16.dp)
            )
            .graphicsLayer { clip = true },
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
                    RealBattleSwordsIcon(size = 18.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (!isPunishment) "جولة $battleMode الحماسية" else strings.punishmentTimeBadge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (!isPunishment) GoldVip else DangerRed
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x33000000),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        RealClockIcon(size = 14.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = timerStr,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Split-Score Bar with Magic Mist & Glowing Gloves
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clip(RoundedCornerShape(15.dp))
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
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RealDiamondIcon(size = 14.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isMistActive) "???" else formatMetric(hostScore),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Opponent Side (Blue)
                    Box(
                        modifier = Modifier
                            .weight(1f - hostFraction)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFF2979FF), Color(0xFF00B0FF)))
                            )
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isMistActive) "???" else formatMetric(opponentScore),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            RealDiamondIcon(size = 14.dp)
                        }
                    }
                }

                // Middle Collision Blades
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    RealBattleSwordsIcon(size = 16.dp)
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
                            RealDiamondIcon(size = 14.dp)
                            Spacer(modifier = Modifier.width(6.dp))
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
                        border = BorderStroke(1.dp, DangerRed)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            RealBoxingGloveIcon(size = 16.dp, isFlaming = true)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("×3", fontWeight = FontWeight.Bold, color = DangerRed, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${gloveTimeLeft}s", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Text("القفاز خامل", color = TextMuted, fontSize = 10.sp)
                }

                // Winner Crown on Punishment Time
                if (isPunishment) {
                    val isHostWinner = hostScore >= opponentScore
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GoldVip.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, GoldVip)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            RealCrownIcon(size = 16.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHostWinner) "الفائز: المضيف WIN" else "الفائز: المنافس WIN",
                                color = GoldVip,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Text(opponentName, color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LivePlayerWithOverlays(
    streamUrl: String,
    opponentStreamUrl: String?,
    isPkActive: Boolean,
    hostName: String,
    opponentName: String,
    opponentAvatar: String,
    heartClickCount: Int,
    onHeartClick: () -> Unit,
    lastComboEvent: WebcastLiveEvent.Gift?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f) // Full 9:16 Portrait Aspect Ratio
            .clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, GlassBorder, RoundedCornerShape(20.dp))
            .graphicsLayer { clip = true },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Hardware SurfaceView ExoPlayer in full 9:16
            LiveVideoPlayer(
                streamUrl = streamUrl,
                opponentStreamUrl = opponentStreamUrl,
                isPkActive = isPkActive,
                hostName = hostName,
                opponentName = opponentName,
                modifier = Modifier.fillMaxSize()
            )

            // Opponent camera PIP overlay if in PK and single stream
            if (isPkActive && opponentStreamUrl.isNullOrBlank() && opponentAvatar.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(width = 115.dp, height = 160.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, Color(0xFF2979FF), RoundedCornerShape(14.dp))
                        .background(Color(0xDD111827))
                        .graphicsLayer { clip = true }
                ) {
                    AsyncImage(
                        model = opponentAvatar,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xAA000000))
                            .align(Alignment.BottomCenter)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = opponentName,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Real-Time Genuine Combo Strike Overlay (Only active when real gift is received)
            AnimatedVisibility(
                visible = lastComboEvent != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                if (lastComboEvent != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Black.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, GoldVip),
                        modifier = Modifier.graphicsLayer { clip = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            RealWebcastGiftBadge(
                                giftName = lastComboEvent.giftName,
                                iconUrl = lastComboEvent.iconUrl,
                                size = 28.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "COMBO ×${lastComboEvent.comboCount}",
                                    fontWeight = FontWeight.Bold,
                                    color = GoldVip,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${lastComboEvent.nickname}: ${lastComboEvent.giftName}",
                                    color = TextPrimary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 60FPS Floating Hearts Burst particles
            if (heartClickCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 48.dp)
                ) {
                    FloatingHeartParticle(id = heartClickCount)
                }
            }

            // Tap Heart Action Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
            ) {
                FloatingActionButton(
                    onClick = onHeartClick,
                    containerColor = NeonPink,
                    shape = CircleShape,
                    modifier = Modifier.size(52.dp)
                ) {
                    RealHeartLikeIcon(size = 28.dp, isPulsing = false)
                }
            }
        }
    }
}

@Composable
fun TopContributorsSection(
    contributors: List<LiveContributor>,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .graphicsLayer { clip = true },
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RealTrophyIcon(rank = 1, size = 18.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.topContributorsTitle,
                    fontWeight = FontWeight.Bold,
                    color = GoldVip,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                contributors.take(3).forEach { c ->
                    ContributorPill(
                        contributor = c,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun LiveCommentsSection(
    comments: List<LiveComment>,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .graphicsLayer { clip = true },
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RealChatBubbleIcon(size = 18.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.liveChatTitle,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            comments.take(6).forEach { comment ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (comment.isGift) {
                        RealWebcastGiftBadge(
                            giftName = comment.giftName ?: "Rose",
                            iconUrl = comment.giftIconUrl,
                            size = 24.dp
                        )
                    } else {
                        AsyncImage(
                            model = comment.avatar,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
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
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RealTrophyIcon(rank = contributor.rank, size = 16.dp)
            Spacer(modifier = Modifier.height(4.dp))
            AsyncImage(
                model = contributor.avatar,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, GoldVip, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = contributor.nickname,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                RealDiamondIcon(size = 11.dp)
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = formatMetric(contributor.score),
                    fontSize = 10.sp,
                    color = GoldVip,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun FloatingHeartParticle(id: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "heartRise$id")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -140f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offsetY"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .offset(y = offsetY.dp)
            .graphicsLayer { this.alpha = alpha }
    ) {
        RealHeartLikeIcon(size = 32.dp, isPulsing = false)
    }
}

private fun formatMetric(num: Long): String {
    return when {
        num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0)
        num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0)
        else -> num.toString()
    }
}
