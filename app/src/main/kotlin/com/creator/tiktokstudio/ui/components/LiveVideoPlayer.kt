package com.creator.tiktokstudio.ui.components

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.creator.tiktokstudio.core.LiveStreamService

@OptIn(UnstableApi::class)
private fun buildUltraLowLatencyPlayer(context: Context, streamUrl: String): ExoPlayer? {
    if (streamUrl.isBlank() || (!streamUrl.startsWith("http://") && !streamUrl.startsWith("https://"))) {
        return null
    }
    return try {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(LiveStreamService.BROWSER_USER_AGENT)
            .setConnectTimeoutMs(6000)
            .setReadTimeoutMs(10000)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to "https://www.tiktok.com/",
                    "Origin" to "https://www.tiktok.com"
                )
            )

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        // Ultra-Low Latency Buffering Settings
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                500,  // minBufferMs = 500
                2000, // maxBufferMs = 2000
                250,  // bufferForPlaybackMs = 250
                500   // bufferForPlaybackAfterRebufferMs = 500
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                val mediaItem = MediaItem.Builder()
                    .setUri(streamUrl)
                    .setLiveConfiguration(
                        MediaItem.LiveConfiguration.Builder()
                            .setTargetOffsetMs(500)
                            .setMaxPlaybackSpeed(1.04f)
                            .setMinPlaybackSpeed(0.96f)
                            .build()
                    )
                    .build()
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
    } catch (_: Throwable) {
        null
    }
}

@OptIn(UnstableApi::class)
@Composable
fun LiveVideoPlayer(
    streamUrl: String,
    modifier: Modifier = Modifier,
    opponentStreamUrl: String? = null,
    isPkActive: Boolean = false,
    hostName: String = "",
    opponentName: String = ""
) {
    val isHostValid = streamUrl.isNotBlank() && (streamUrl.startsWith("http://") || streamUrl.startsWith("https://"))
    val hasOpponentStream = isPkActive && !opponentStreamUrl.isNullOrBlank() &&
        (opponentStreamUrl.startsWith("http://") || opponentStreamUrl.startsWith("https://"))

    if (!isHostValid) {
        Box(
            modifier = modifier.background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "لا يوجد بث مباشر نشط حالياً",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        return
    }

    val context = LocalContext.current

    // Host Player instance
    val hostPlayer = remember(context, streamUrl) {
        buildUltraLowLatencyPlayer(context, streamUrl)
    }

    DisposableEffect(hostPlayer) {
        onDispose {
            try {
                hostPlayer?.release()
            } catch (_: Throwable) {}
        }
    }

    // Opponent Player instance (for Split-Screen PK battles)
    val opponentPlayer = remember(context, opponentStreamUrl, hasOpponentStream) {
        if (hasOpponentStream && opponentStreamUrl != null) {
            buildUltraLowLatencyPlayer(context, opponentStreamUrl)
        } else {
            null
        }
    }

    DisposableEffect(opponentPlayer) {
        onDispose {
            try {
                opponentPlayer?.release()
            } catch (_: Throwable) {}
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .graphicsLayer { clip = true }
    ) {
        if (hasOpponentStream && opponentPlayer != null && hostPlayer != null) {
            // Split-Screen PK Battle (60FPS Side-by-Side Surface Rendering)
            Row(modifier = Modifier.fillMaxSize()) {
                // Host Video Panel (Left / Primary)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, Color(0xFF00F2FE).copy(alpha = 0.5f))
                        .graphicsLayer { clip = true }
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            try {
                                PlayerView(ctx).apply {
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    ).apply {
                                        gravity = android.view.Gravity.CENTER
                                    }
                                    player = hostPlayer
                                    useController = false
                                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                }
                            } catch (_: Throwable) {
                                FrameLayout(ctx)
                            }
                        },
                        update = { playerView ->
                            try {
                                if (playerView.player != hostPlayer) {
                                    playerView.player = hostPlayer
                                }
                            } catch (_: Throwable) {}
                        }
                    )
                    // Host Neon Tag
                    if (hostName.isNotBlank()) {
                        Text(
                            text = hostName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00F2FE),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Opponent Video Panel (Right / Secondary)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, Color(0xFFFF0055).copy(alpha = 0.5f))
                        .graphicsLayer { clip = true }
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            try {
                                PlayerView(ctx).apply {
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    ).apply {
                                        gravity = android.view.Gravity.CENTER
                                    }
                                    player = opponentPlayer
                                    useController = false
                                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                }
                            } catch (_: Throwable) {
                                FrameLayout(ctx)
                            }
                        },
                        update = { playerView ->
                            try {
                                if (playerView.player != opponentPlayer) {
                                    playerView.player = opponentPlayer
                                }
                            } catch (_: Throwable) {}
                        }
                    )
                    // Opponent Neon Tag
                    if (opponentName.isNotBlank()) {
                        Text(
                            text = opponentName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF0055),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        } else if (hostPlayer != null) {
            // Fullscreen Single Stream
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    try {
                        PlayerView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            ).apply {
                                gravity = android.view.Gravity.CENTER
                            }
                            player = hostPlayer
                            useController = false
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        }
                    } catch (_: Throwable) {
                        FrameLayout(ctx)
                    }
                },
                update = { playerView ->
                    try {
                        if (playerView.player != hostPlayer) {
                            playerView.player = hostPlayer
                        }
                    } catch (_: Throwable) {}
                }
            )
        } else {
            // Placeholder when player could not be built
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "تعذر تحميل مشغل الفيديو",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}
