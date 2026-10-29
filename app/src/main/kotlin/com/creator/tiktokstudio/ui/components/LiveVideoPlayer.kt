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
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.creator.tiktokstudio.core.LiveStreamService
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
private fun buildUltraLowLatencyPlayer(context: Context, streamUrl: String): ExoPlayer {
    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", LiveStreamService.BROWSER_USER_AGENT)
                .header("Referer", "https://www.tiktok.com/")
                .header("Origin", "https://www.tiktok.com")
                .build()
            chain.proceed(req)
        }
        .build()

    val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
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

    return ExoPlayer.Builder(context)
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
    val context = LocalContext.current

    // Host Player instance
    val hostPlayer = remember(context, streamUrl) {
        buildUltraLowLatencyPlayer(context, streamUrl)
    }

    DisposableEffect(hostPlayer) {
        onDispose {
            hostPlayer.release()
        }
    }

    // Opponent Player instance (for Split-Screen PK battles)
    val hasOpponentStream = isPkActive && !opponentStreamUrl.isNullOrBlank()
    val opponentPlayer = remember(context, opponentStreamUrl, hasOpponentStream) {
        if (hasOpponentStream && opponentStreamUrl != null) {
            buildUltraLowLatencyPlayer(context, opponentStreamUrl)
        } else {
            null
        }
    }

    DisposableEffect(opponentPlayer) {
        onDispose {
            opponentPlayer?.release()
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .graphicsLayer { clip = true }
    ) {
        if (hasOpponentStream && opponentPlayer != null) {
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
                            PlayerView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                player = hostPlayer
                                useController = false
                                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                            }
                        },
                        update = { playerView ->
                            if (playerView.player != hostPlayer) {
                                playerView.player = hostPlayer
                            }
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
                            PlayerView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                player = opponentPlayer
                                useController = false
                                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                            }
                        },
                        update = { playerView ->
                            if (playerView.player != opponentPlayer) {
                                playerView.player = opponentPlayer
                            }
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
        } else {
            // Fullscreen Single Stream
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        player = hostPlayer
                        useController = false
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    }
                },
                update = { playerView ->
                    if (playerView.player != hostPlayer) {
                        playerView.player = hostPlayer
                    }
                }
            )
        }
    }
}
