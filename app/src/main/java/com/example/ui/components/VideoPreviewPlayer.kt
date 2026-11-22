package com.example.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.AspectRatioOption
import com.example.data.CaptionStyleOption
import com.example.ui.Formatters
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.ElectricAmethyst
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RoyalViolet
import kotlinx.coroutines.delay

@Composable
fun VideoPreviewPlayer(
    videoUri: String,
    durationMs: Long,
    aspectRatio: AspectRatioOption,
    captionStyle: CaptionStyleOption,
    modifier: Modifier = Modifier,
    clipStartMs: Long = 0L,
    clipEndMs: Long? = null,
    autoPlay: Boolean = false,
    loopPlayback: Boolean = true,
    seekTriggerMs: Long? = null,
    customCaptionText: String? = null,
    highlightWords: List<String> = emptyList(),
    onSeekTriggerConsumed: (() -> Unit)? = null
) {
    val effectiveEndMs = (clipEndMs ?: durationMs).coerceAtLeast(clipStartMs + 500L)
    var retryCounter by remember(videoUri) { mutableIntStateOf(0) }
    var videoViewRef by remember(videoUri, retryCounter) { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember(videoUri, retryCounter) { mutableStateOf<MediaPlayer?>(null) }
    var isPrepared by remember(videoUri, retryCounter) { mutableStateOf(false) }
    var isPlaying by remember(videoUri, retryCounter) { mutableStateOf(autoPlay) }
    var isMuted by remember { mutableStateOf(false) }
    var currentPosMs by remember(videoUri, clipStartMs, retryCounter) { mutableLongStateOf(clipStartMs) }
    var playbackError by remember(videoUri, retryCounter) { mutableStateOf<String?>(null) }

    LaunchedEffect(seekTriggerMs, isPrepared) {
        val target = seekTriggerMs ?: return@LaunchedEffect
        val view = videoViewRef
        if (view != null && isPrepared) {
            view.seekTo(target.toInt())
            currentPosMs = target
            if (!view.isPlaying) {
                view.start()
                isPlaying = true
            }
            onSeekTriggerConsumed?.invoke()
        }
    }

    LaunchedEffect(isPlaying, isPrepared, clipStartMs, effectiveEndMs, loopPlayback) {
        while (isPlaying && isPrepared) {
            val view = videoViewRef
            if (view != null) {
                val pos = view.currentPosition.toLong()
                currentPosMs = pos
                if (clipEndMs != null && pos >= effectiveEndMs) {
                    if (loopPlayback) {
                        view.seekTo(clipStartMs.toInt())
                        currentPosMs = clipStartMs
                        if (!view.isPlaying) view.start()
                    } else {
                        view.pause()
                        view.seekTo(clipStartMs.toInt())
                        currentPosMs = clipStartMs
                        isPlaying = false
                    }
                }
            }
            delay(180L)
        }
    }

    LaunchedEffect(isMuted, mediaPlayerRef) {
        val vol = if (isMuted) 0f else 1f
        try {
            mediaPlayerRef?.setVolume(vol, vol)
        } catch (_: IllegalStateException) {
        }
    }

    DisposableEffect(videoUri, retryCounter) {
        onDispose {
            try {
                videoViewRef?.stopPlayback()
            } catch (_: Exception) {
            }
            mediaPlayerRef = null
            videoViewRef = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(ObsidianBlack)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.medium
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 210.dp, max = 310.dp)
                .background(ObsidianBlack)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            val previewFrameModifier = when (aspectRatio) {
                AspectRatioOption.PORTRAIT_9_16 -> Modifier
                    .height(280.dp)
                    .aspectRatio(9f / 16f)
                AspectRatioOption.SQUARE_1_1 -> Modifier
                    .size(240.dp)
                AspectRatioOption.LANDSCAPE_16_9 -> Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .aspectRatio(16f / 9f)
            }

            Box(
                modifier = previewFrameModifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0E0B18))
                    .border(
                        width = 1.dp,
                        color = RoyalViolet.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (playbackError != null) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BrokenImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = playbackError ?: "Preview unavailable",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = { retryCounter++ }) {
                            Text("Retry Preview")
                        }
                    }
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            FrameLayout(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                val vv = VideoView(ctx).apply {
                                    layoutParams = FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        Gravity.CENTER
                                    )
                                    setOnPreparedListener { mp ->
                                        mediaPlayerRef = mp
                                        mp.isLooping = loopPlayback && clipEndMs == null
                                        val vol = if (isMuted) 0f else 1f
                                        mp.setVolume(vol, vol)
                                        isPrepared = true
                                        seekTo(clipStartMs.toInt())
                                        if (autoPlay) {
                                            start()
                                            isPlaying = true
                                        }
                                    }
                                    setOnCompletionListener {
                                        if (loopPlayback) {
                                            seekTo(clipStartMs.toInt())
                                            start()
                                            isPlaying = true
                                        } else {
                                            isPlaying = false
                                            currentPosMs = clipStartMs
                                        }
                                    }
                                    setOnErrorListener { _, _, _ ->
                                        playbackError = "Unable to play this local MP4 URI. Tap Retry or re-select the file."
                                        isPlaying = false
                                        true
                                    }
                                    tag = "$videoUri#$retryCounter"
                                    setVideoURI(Uri.parse(videoUri))
                                }
                                videoViewRef = vv
                                addView(vv)
                            }
                        },
                        update = { container ->
                            val vv = container.getChildAt(0) as? VideoView
                            val expectedTag = "$videoUri#$retryCounter"
                            if (vv != null && vv.tag != expectedTag) {
                                vv.tag = expectedTag
                                isPrepared = false
                                playbackError = null
                                vv.setVideoURI(Uri.parse(videoUri))
                            }
                        }
                    )

                    if (!isPrepared) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianBlack.copy(alpha = 0.72f))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = RoyalViolet
                            )
                            Text(
                                text = "Loading preview…",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }

                    // Live animated multilingual subtitle with highlighted keywords or studio caption style
                    if (!customCaptionText.isNullOrBlank()) {
                        AnimatedContent(
                            targetState = customCaptionText,
                            transitionSpec = {
                                (fadeIn() + scaleIn(initialScale = 0.92f)) togetherWith fadeOut()
                            },
                            label = "animated_shorts_caption",
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 10.dp, vertical = 12.dp)
                        ) { captionLine ->
                            val highlightedSet = remember(highlightWords) {
                                highlightWords
                                    .flatMap { it.split(" ") }
                                    .map { it.trim().lowercase().trim(',', '.', '!', '?', ':', ';') }
                                    .filter { it.length >= 2 }
                                    .toSet()
                            }
                            val annotated = remember(captionLine, highlightedSet) {
                                buildAnnotatedString {
                                    val tokens = captionLine.split(" ")
                                    tokens.forEachIndexed { idx, word ->
                                        val clean = word.lowercase().trim(',', '.', '!', '?', ':', ';')
                                        val isKeyWord = highlightedSet.contains(clean) ||
                                            (highlightedSet.isEmpty() && idx == tokens.lastIndex && word.length >= 4)
                                        if (isKeyWord) {
                                            withStyle(
                                                SpanStyle(
                                                    color = AmberHighlight,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            ) {
                                                append(word)
                                            }
                                        } else {
                                            withStyle(
                                                SpanStyle(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            ) {
                                                append(word)
                                            }
                                        }
                                        if (idx < tokens.lastIndex) append(" ")
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianBlack.copy(alpha = 0.82f))
                                    .border(1.2.dp, CyberMagenta.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = annotated,
                                    style = MaterialTheme.typography.labelLarge,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else if (captionStyle != CaptionStyleOption.NONE) {
                        CaptionOverlayPreview(
                            style = captionStyle,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 10.dp, vertical = 14.dp)
                        )
                    }
                }

                StudioPillBadge(
                    text = aspectRatio.code,
                    containerColor = ObsidianBlack.copy(alpha = 0.78f),
                    contentColor = Color.White,
                    useMonoFont = true,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            val sliderMin = clipStartMs.toFloat()
            val sliderMax = effectiveEndMs.coerceAtLeast(clipStartMs + 500L).toFloat()
            val sliderPos = currentPosMs.toFloat().coerceIn(sliderMin, sliderMax)

            Slider(
                value = sliderPos,
                onValueChange = { newValue ->
                    currentPosMs = newValue.toLong()
                    try {
                        videoViewRef?.seekTo(newValue.toInt())
                    } catch (_: Exception) {
                    }
                },
                valueRange = sliderMin..sliderMax,
                colors = SliderDefaults.colors(
                    thumbColor = CyberMagenta,
                    activeTrackColor = RoyalViolet,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = {
                            val view = videoViewRef ?: return@IconButton
                            if (playbackError != null) return@IconButton
                            if (isPlaying) {
                                view.pause()
                                isPlaying = false
                            } else {
                                if (currentPosMs >= effectiveEndMs - 200L) {
                                    view.seekTo(clipStartMs.toInt())
                                    currentPosMs = clipStartMs
                                }
                                view.start()
                                isPlaying = true
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("video_play_pause_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(listOf(RoyalViolet, ElectricAmethyst))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause preview" else "Play preview",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val view = videoViewRef ?: return@IconButton
                            view.seekTo(clipStartMs.toInt())
                            currentPosMs = clipStartMs
                            if (!isPlaying) {
                                view.start()
                                isPlaying = true
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay,
                            contentDescription = "Restart preview",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) {
                                Icons.AutoMirrored.Rounded.VolumeOff
                            } else {
                                Icons.AutoMirrored.Rounded.VolumeUp
                            },
                            contentDescription = if (isMuted) "Unmute video" else "Mute video",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${Formatters.formatDuration((currentPosMs - clipStartMs).coerceAtLeast(0L))} / ${Formatters.formatDuration((effectiveEndMs - clipStartMs).coerceAtLeast(0L))}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CaptionOverlayPreview(
    style: CaptionStyleOption,
    modifier: Modifier = Modifier
) {
    when (style) {
        CaptionStyleOption.BOLD_VIOLET -> {
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(RoyalViolet.copy(alpha = 0.92f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = style.sampleText,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        CaptionStyleOption.NEON_GLOW -> {
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianBlack.copy(alpha = 0.78f))
                    .border(1.2.dp, CyberMagenta, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = style.sampleText,
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberMagenta,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        CaptionStyleOption.MINIMAL_CLEAN -> {
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ObsidianBlack.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = style.sampleText,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        CaptionStyleOption.NONE -> Unit
    }
}
