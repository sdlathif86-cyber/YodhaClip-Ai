package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ai.LiveHighlightPreset
import com.example.ai.SubtitleLanguage
import com.example.ui.Formatters
import com.example.ui.LiveClipApprovalStatus
import com.example.ui.LiveConnectionStatus
import com.example.ui.LiveDetectedClip
import com.example.ui.LiveStreamShortsUiState
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.LiveStreamExoPlayerView
import com.example.ui.components.StudioSurfaceCard
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RoyalViolet
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveStreamShortsScreen(
    state: LiveStreamShortsUiState,
    onBack: () -> Unit,
    onStreamUrlChange: (String) -> Unit,
    onStreamTitleChange: (String) -> Unit,
    onAuthorizationChange: (Boolean) -> Unit,
    onPresetChange: (LiveHighlightPreset) -> Unit,
    onRollingWindowChange: (Int) -> Unit,
    onAutoDetectChange: (Boolean) -> Unit,
    onSubtitleLanguageChange: (SubtitleLanguage) -> Unit,
    onConvertTo9x16Change: (Boolean) -> Unit,
    onLiveContextNoteChange: (String) -> Unit,
    onConnectStream: () -> Unit,
    onDisconnectStream: () -> Unit,
    onPlayerLiveReady: () -> Unit,
    onPlayerStreamError: (String) -> Unit,
    onScanLiveMoment: (Bitmap?, Boolean) -> Unit,
    onApproveClip: (LiveDetectedClip) -> Unit,
    onRejectClip: (String) -> Unit,
    onExportClipMp4: (LiveDetectedClip) -> Unit,
    onExportClipSrt: (LiveDetectedClip) -> Unit,
    onClearBuffer: () -> Unit
) {
    var frameGrabber by remember { mutableStateOf<(() -> Bitmap?)?>(null) }

    // Near-real-time periodic AI highlight scan ONLY when a real authorized stream is live & connected
    LaunchedEffect(state.isLiveConnected, state.autoDetectEnabled) {
        if (!state.isLiveConnected || !state.autoDetectEnabled) return@LaunchedEffect
        while (true) {
            delay(25_000L)
            if (!state.isAiScanning) {
                val frame = frameGrabber?.invoke()
                onScanLiveMoment(frame, false)
            }
        }
    }

    val visibleClips = remember(state.detectedClips) {
        state.detectedClips.filter { it.approvalStatus != LiveClipApprovalStatus.REJECTED }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("live_shorts_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live Stream to Shorts",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Authorized HLS / RTMP • Rolling Buffer • Near Real-Time AI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Live Processing Status Telemetry Card
            item {
                StudioSurfaceCard(
                    borderColor = when (state.connectionStatus) {
                        LiveConnectionStatus.LIVE_BUFFERING -> NeonEmerald.copy(alpha = 0.65f)
                        LiveConnectionStatus.CONNECTING -> CyberMagenta.copy(alpha = 0.65f)
                        LiveConnectionStatus.ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.65f)
                        LiveConnectionStatus.DISCONNECTED -> RoyalViolet.copy(alpha = 0.4f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_processing_status_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Live Processing Status",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = state.connectionStatus.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = when (state.connectionStatus) {
                                    LiveConnectionStatus.LIVE_BUFFERING -> NeonEmerald
                                    LiveConnectionStatus.CONNECTING -> CyberMagenta
                                    LiveConnectionStatus.ERROR -> MaterialTheme.colorScheme.error
                                    LiveConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LiveMetricTile(
                                label = "Buffer",
                                value = "${state.bufferSnapshot.totalDurationMs / 1000L}s / ${state.rollingWindowSeconds}s",
                                modifier = Modifier.weight(1f)
                            )
                            LiveMetricTile(
                                label = "Disk Ring",
                                value = "${state.bufferSnapshot.segmentCount} seg (${Formatters.formatFileSize(state.bufferSnapshot.totalSizeBytes)})",
                                modifier = Modifier.weight(1f)
                            )
                            LiveMetricTile(
                                label = "Review Queue",
                                value = "${state.pendingReviewCount} pending • ${state.approvedCount} saved",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (state.isAiScanning) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(15.dp),
                                    strokeWidth = 2.dp,
                                    color = CyberMagenta
                                )
                                Text(
                                    text = state.lastAiScanSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ElectricViolet
                                )
                            }
                        } else {
                            Text(
                                text = state.lastAiScanSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Authorized Stream Source Card
            item {
                StudioSurfaceCard(
                    borderColor = RoyalViolet.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_source_config_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "1. Authorized Live Stream Input (HLS / RTMP)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = state.streamUrlInput,
                            onValueChange = onStreamUrlChange,
                            label = { Text("Authorized Stream URL (.m3u8 or rtmp://)") },
                            placeholder = { Text("https://your-server/live/stream.m3u8 or rtmp://...") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("live_stream_url_input")
                        )

                        if (state.streamUrlInput.isNotBlank()) {
                            Text(
                                text = "Detected Protocol: ${state.detectedProtocol.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricViolet
                            )
                        }

                        OutlinedTextField(
                            value = state.streamTitleInput,
                            onValueChange = onStreamTitleChange,
                            label = { Text("Stream / Game Session Title (Optional)") },
                            placeholder = { Text("BGMI Rank Push Live • Finals") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "AI Highlight Focus Preset",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LiveHighlightPreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = state.highlightPreset == preset,
                                    onClick = { onPresetChange(preset) },
                                    label = { Text(preset.label, maxLines = 1) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RoyalViolet.copy(alpha = 0.35f),
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("live_preset_${preset.name.lowercase()}")
                                )
                            }
                        }

                        Text(
                            text = "Rolling Video Buffer Window",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(60, 90, 120, 180).forEach { sec ->
                                FilterChip(
                                    selected = state.rollingWindowSeconds == sec,
                                    onClick = { onRollingWindowChange(sec) },
                                    label = { Text("${sec}s", maxLines = 1) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = state.authorizationConfirmed,
                                onCheckedChange = onAuthorizationChange,
                                colors = CheckboxDefaults.colors(checkedColor = RoyalViolet),
                                modifier = Modifier.testTag("live_authorization_checkbox")
                            )
                            Text(
                                text = "I own this live stream or have explicit broadcaster authorization to monitor and clip it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (!state.connectionError.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = state.connectionError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state.connectedStreamUrl == null) {
                                GradientPrimaryButton(
                                    text = "Connect Authorized Stream",
                                    icon = Icons.Rounded.LiveTv,
                                    testTag = "live_connect_button",
                                    onClick = onConnectStream,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                OutlinedButton(
                                    onClick = onDisconnectStream,
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("live_disconnect_button")
                                ) {
                                    Icon(
                                        Icons.Rounded.StopCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Disconnect Stream", maxLines = 1)
                                }
                            }
                            if (state.bufferSnapshot.segmentCount > 0) {
                                OutlinedButton(
                                    onClick = onClearBuffer,
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    modifier = Modifier.height(50.dp)
                                ) {
                                    Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear rolling buffer", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Live Player & Instant Capture Controls
            item {
                StudioSurfaceCard(
                    borderColor = CyberMagenta.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "2. Live Monitor & AI Highlight Trigger",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (state.isLiveConnected) {
                                Text(
                                    text = Formatters.formatDuration(state.streamUptimeSeconds * 1000L),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonEmerald
                                )
                            }
                        }

                        LiveStreamExoPlayerView(
                            streamUrl = state.connectedStreamUrl,
                            protocol = state.detectedProtocol,
                            connectionStatus = state.connectionStatus,
                            onLiveReady = onPlayerLiveReady,
                            onStreamError = onPlayerStreamError,
                            onFrameGrabberReady = { grabber -> frameGrabber = grabber }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Detect Exciting Moments (Every 25s)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Only scans while a real authorized stream is live",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.autoDetectEnabled,
                                onCheckedChange = onAutoDetectChange,
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RoyalViolet)
                            )
                        }

                        OutlinedTextField(
                            value = state.liveContextNote,
                            onValueChange = onLiveContextNoteChange,
                            label = { Text("Live Commentary / Hype Note (Optional)") },
                            placeholder = { Text("e.g. 1v4 clutch final circle, chat going crazy!") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SubtitleLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = state.subtitleLanguage == lang,
                                    onClick = { onSubtitleLanguageChange(lang) },
                                    label = { Text("${lang.label} (${lang.nativeLabel})") }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Convert Approved Clips to 9:16 Vertical",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = state.convertTo9x16,
                                onCheckedChange = onConvertTo9x16Change,
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RoyalViolet)
                            )
                        }

                        GradientPrimaryButton(
                            text = if (state.isLiveConnected) {
                                "Capture Live Highlight Now"
                            } else {
                                "Connect Live Stream to Capture"
                            },
                            icon = Icons.Rounded.FlashOn,
                            enabled = state.isLiveConnected && !state.isAiScanning,
                            testTag = "live_capture_moment_button",
                            onClick = {
                                val frame = frameGrabber?.invoke()
                                onScanLiveMoment(frame, true)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Review & Approve Queue
            item {
                Text(
                    text = "3. Review & Approve Live Clips (${visibleClips.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (visibleClips.isEmpty()) {
                item {
                    StudioSurfaceCard(
                        borderColor = RoyalViolet.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "No Live Highlights Captured Yet",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "YodhaClip AI never generates fake live clips. Connect your authorized HLS (.m3u8) or RTMP stream above; detected gaming highlights and reactions will appear here for one-tap review and approval.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(visibleClips, key = { it.id }) { clip ->
                    val isExporting = state.exportingClipId == clip.id
                    LiveDetectedClipCard(
                        clip = clip,
                        subtitleLanguage = state.subtitleLanguage,
                        isExporting = isExporting,
                        exportProgress = if (isExporting) state.exportProgressPercent else 0,
                        onApprove = { onApproveClip(clip) },
                        onReject = { onRejectClip(clip.id) },
                        onExportMp4 = { onExportClipMp4(clip) },
                        onExportSrt = { onExportClipSrt(clip) }
                    )
                }
            }

            // Honest Infrastructure / External Server Requirements Note
            item {
                StudioSurfaceCard(
                    borderColor = ElectricViolet.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Open-Source Stream Ingest Notes",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "• HLS (.m3u8): Full on-device playback + rolling segment disk buffer + downloadable clip assembly.\n• RTMP (rtmp://): Real Media3 RTMP playback + live AI frame scanning; for continuous multi-hour RTMP-to-MP4 disk archiving, pair with a free open-source MediaMTX or Nginx-RTMP + FFmpeg relay exposing an authorized HLS endpoint.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun LiveMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LiveDetectedClipCard(
    clip: LiveDetectedClip,
    subtitleLanguage: SubtitleLanguage,
    isExporting: Boolean,
    exportProgress: Int,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onExportMp4: () -> Unit,
    onExportSrt: () -> Unit
) {
    val isApproved = clip.approvalStatus == LiveClipApprovalStatus.APPROVED_SAVED

    StudioSurfaceCard(
        borderColor = if (isApproved) NeonEmerald.copy(alpha = 0.65f) else CyberMagenta.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_clip_card_${clip.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = clip.hookTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${clip.momentCategory} • ${clip.durationMs / 1000L}s • ${clip.viralScore}% Viral",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberMagenta
                    )
                }
                Text(
                    text = if (isApproved) "APPROVED" else "PENDING",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isApproved) NeonEmerald else ElectricViolet
                )
            }

            Text(
                text = clip.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = clip.hashtags.joinToString(" "),
                style = MaterialTheme.typography.labelSmall,
                color = ElectricViolet
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .padding(8.dp)
            ) {
                Text(
                    text = "${subtitleLanguage.label} Captions: ${clip.subtitlesFor(subtitleLanguage).joinToString(" • ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isExporting) {
                LinearProgressIndicator(
                    progress = { exportProgress / 100f },
                    color = CyberMagenta,
                    modifier = Modifier.fillMaxWidth().height(5.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isApproved) {
                    GradientPrimaryButton(
                        text = "Approve & Save",
                        icon = Icons.Rounded.CheckCircle,
                        testTag = "live_approve_${clip.id}",
                        onClick = onApprove,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = onReject,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Dismiss clip", modifier = Modifier.size(18.dp))
                    }
                } else {
                    OutlinedButton(
                        onClick = onExportMp4,
                        enabled = !isExporting && clip.localClipPath != null,
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export MP4", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onExportSrt,
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Rounded.Subtitles, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("SRT", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
