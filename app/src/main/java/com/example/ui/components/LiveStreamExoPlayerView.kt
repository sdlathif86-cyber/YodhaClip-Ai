package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.view.TextureView
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.SensorsOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.rtmp.RtmpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.LiveStreamProtocol
import com.example.ui.LiveConnectionStatus
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RoyalViolet

/**
 * Real Jetpack Media3 ExoPlayer live stream viewer supporting authorized HLS (.m3u8)
 * and RTMP (rtmp:// / rtmps://) streams on a TextureView so live frames can be captured
 * honestly for Gemini AI highlight detection when the stream is actively rendering.
 */
@OptIn(UnstableApi::class)
@Composable
fun LiveStreamExoPlayerView(
    streamUrl: String?,
    protocol: LiveStreamProtocol,
    connectionStatus: LiveConnectionStatus,
    onLiveReady: () -> Unit,
    onStreamError: (String) -> Unit,
    onFrameGrabberReady: ((() -> Bitmap?)?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    onLiveReady()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val friendly = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        "Could not connect to live stream server. Verify the URL is live and reachable."
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                        "Live stream endpoint returned an HTTP/playlist error (offline or unauthorized)."
                    else -> error.localizedMessage ?: "Live stream playback failed (code ${error.errorCode})."
                }
                onStreamError(friendly)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            onFrameGrabberReady(null)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(streamUrl, protocol) {
        if (streamUrl.isNullOrBlank()) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            return@LaunchedEffect
        }
        try {
            val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
            val mediaSource = when (protocol) {
                LiveStreamProtocol.RTMP -> {
                    val rtmpFactory = RtmpDataSource.Factory()
                    ProgressiveMediaSource.Factory(rtmpFactory).createMediaSource(mediaItem)
                }
                LiveStreamProtocol.HLS -> {
                    val httpFactory = DefaultHttpDataSource.Factory()
                        .setAllowCrossProtocolRedirects(true)
                        .setConnectTimeoutMs(12_000)
                        .setReadTimeoutMs(15_000)
                    HlsMediaSource.Factory(httpFactory).createMediaSource(mediaItem)
                }
                LiveStreamProtocol.UNKNOWN -> null
            }
            if (mediaSource != null) {
                exoPlayer.setMediaSource(mediaSource)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            } else {
                onStreamError("Unsupported stream protocol. Use HLS (.m3u8) or RTMP (rtmp://).")
            }
        } catch (e: Exception) {
            onStreamError(e.localizedMessage ?: "Failed to initialize live stream player.")
        }
    }

    LaunchedEffect(textureViewRef, streamUrl) {
        if (textureViewRef != null && !streamUrl.isNullOrBlank()) {
            onFrameGrabberReady {
                try {
                    val tv = textureViewRef
                    if (tv != null && tv.isAvailable) {
                        tv.getBitmap(480, 270)
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
            }
        } else {
            onFrameGrabberReady(null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(18.dp))
            .background(ObsidianBlack)
            .border(
                width = 1.2.dp,
                color = when (connectionStatus) {
                    LiveConnectionStatus.LIVE_BUFFERING -> NeonEmerald.copy(alpha = 0.7f)
                    LiveConnectionStatus.CONNECTING -> ElectricViolet.copy(alpha = 0.7f)
                    LiveConnectionStatus.ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    LiveConnectionStatus.DISCONNECTED -> RoyalViolet.copy(alpha = 0.35f)
                },
                shape = RoundedCornerShape(18.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!streamUrl.isNullOrBlank()) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        // Use TextureView so we can inspect live frames cleanly when connected
                        val tv = TextureView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                        exoPlayer.setVideoTextureView(tv)
                        textureViewRef = tv
                        player = exoPlayer
                    }
                },
                update = { pv ->
                    pv.player = exoPlayer
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (streamUrl.isNullOrBlank() || connectionStatus == LiveConnectionStatus.DISCONNECTED) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SensorsOff,
                    contentDescription = "No stream connected",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "No Authorized Live Stream Connected",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Paste an authorized HLS (.m3u8) or RTMP URL above to monitor the live feed and activate the rolling buffer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (connectionStatus == LiveConnectionStatus.CONNECTING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBlack.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        color = CyberMagenta,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Connecting to ${protocol.label} live stream…",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }

        // Top-left live status badge overlay
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(50))
                .background(ObsidianBlack.copy(alpha = 0.78f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when (connectionStatus) {
                            LiveConnectionStatus.LIVE_BUFFERING -> NeonEmerald
                            LiveConnectionStatus.CONNECTING -> CyberMagenta
                            LiveConnectionStatus.ERROR -> MaterialTheme.colorScheme.error
                            LiveConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
            )
            Icon(
                imageVector = Icons.Rounded.Sensors,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = connectionStatus.label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}
