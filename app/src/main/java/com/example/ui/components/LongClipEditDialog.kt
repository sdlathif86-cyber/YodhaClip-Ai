package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.LongVideoClipProposal
import com.example.ai.SubtitleLanguage
import com.example.data.AspectRatioOption
import com.example.data.CaptionStyleOption
import com.example.data.SmartCropMode
import com.example.ui.Formatters
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RoyalViolet
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LongClipEditDialog(
    proposal: LongVideoClipProposal,
    videoUri: String,
    totalVideoDurationMs: Long,
    subtitleLanguage: SubtitleLanguage,
    convertTo9x16: Boolean,
    isRegenerating: Boolean,
    onDismiss: () -> Unit,
    onSaveChanges: (
        title: String,
        startMs: Long,
        endMs: Long,
        cropMode: SmartCropMode,
        hashtags: List<String>,
        openingHook: String,
        seoDescription: String,
        thumbnailText: String,
        subtitleLines: List<String>
    ) -> Unit,
    onRegenerateTitleHashtags: () -> Unit,
    onExportMp4: () -> Unit
) {
    var titleInput by remember(proposal.id, proposal.hookTitle) { mutableStateOf(proposal.hookTitle) }
    var hookInput by remember(proposal.id, proposal.openingHook) { mutableStateOf(proposal.openingHook) }
    var seoDescInput by remember(proposal.id, proposal.seoDescription) { mutableStateOf(proposal.seoDescription) }
    var thumbTextInput by remember(proposal.id, proposal.thumbnailText) { mutableStateOf(proposal.thumbnailText) }
    var hashtagsInput by remember(proposal.id, proposal.hashtags) {
        mutableStateOf(proposal.hashtags.joinToString(" "))
    }
    var captionsInput by remember(proposal.id, subtitleLanguage, proposal.subtitlesFor(subtitleLanguage)) {
        mutableStateOf(proposal.subtitlesFor(subtitleLanguage).joinToString("\n"))
    }
    var startMs by remember(proposal.id) { mutableLongStateOf(proposal.startMs) }
    var endMs by remember(proposal.id) { mutableLongStateOf(proposal.endMs) }
    var cropMode by remember(proposal.id) { mutableStateOf(proposal.recommendedCropMode) }

    val safeTotalMs = totalVideoDurationMs.coerceAtLeast(5_000L)
    val clipDurationSec = ((endMs - startMs).coerceAtLeast(1_000L) / 1000L)
    val inIdealShortsRange = clipDurationSec in 15L..60L
    val liveCaptionPreview = captionsInput.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()

    fun commitEdits() {
        val parsedTags = hashtagsInput
            .split(Regex("[\\s,]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { if (it.startsWith("#")) it else "#$it" }
        val parsedCaptions = captionsInput
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
        onSaveChanges(
            titleInput,
            startMs,
            endMs,
            cropMode,
            parsedTags,
            hookInput,
            seoDescInput,
            thumbTextInput,
            parsedCaptions
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Edit Clip #${proposal.rank} • Timeline & Captions",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${proposal.category} • ${proposal.viralScore}% Viral Score",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberMagenta
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close editor"
                        )
                    }
                }

                VideoPreviewPlayer(
                    videoUri = videoUri,
                    durationMs = safeTotalMs,
                    aspectRatio = if (convertTo9x16) AspectRatioOption.PORTRAIT_9_16 else AspectRatioOption.LANDSCAPE_16_9,
                    captionStyle = CaptionStyleOption.BOLD_VIOLET,
                    clipStartMs = startMs,
                    clipEndMs = endMs,
                    loopPlayback = true,
                    customCaptionText = liveCaptionPreview,
                    highlightWords = proposal.highlightWords
                )

                // Editable Clip Timeline RangeSlider + Fine Nudge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${Formatters.formatDuration(startMs)} → ${Formatters.formatDuration(endMs)}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${clipDurationSec}s ${if (inIdealShortsRange) "• Ideal 15–60s" else "• Custom"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (inIdealShortsRange) NeonEmerald else ElectricViolet
                    )
                }

                RangeSlider(
                    value = startMs.toFloat()..endMs.toFloat(),
                    onValueChange = { range ->
                        val s = range.start.roundToLong().coerceIn(0L, safeTotalMs - 3_000L)
                        val e = range.endInclusive.roundToLong().coerceIn(s + 3_000L, safeTotalMs)
                        startMs = s
                        endMs = e
                    },
                    valueRange = 0f..safeTotalMs.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = CyberMagenta,
                        activeTrackColor = RoyalViolet,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    modifier = Modifier.testTag("long_clip_timeline_slider")
                )

                // Fine 1-second trim nudge buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { startMs = (startMs - 1_000L).coerceAtLeast(0L) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) { Text("-1s In", maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                    OutlinedButton(
                        onClick = { startMs = (startMs + 1_000L).coerceAtMost(endMs - 3_000L) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) { Text("+1s In", maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                    OutlinedButton(
                        onClick = { endMs = (endMs - 1_000L).coerceAtLeast(startMs + 3_000L) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) { Text("-1s Out", maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                    OutlinedButton(
                        onClick = { endMs = (endMs + 1_000L).coerceAtMost(safeTotalMs) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) { Text("+1s Out", maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                }

                // Smart Vertical Crop selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Crop,
                        contentDescription = null,
                        tint = ElectricViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Smart 9:16 Crop & Subject Tracking",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SmartCropMode.entries.forEach { mode ->
                        FilterChip(
                            selected = cropMode == mode,
                            onClick = { cropMode = mode },
                            label = { Text(mode.label, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalViolet.copy(alpha = 0.3f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = captionsInput,
                    onValueChange = { captionsInput = it },
                    label = { Text("Editable Captions (${subtitleLanguage.label} — one cue per line)") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("long_clip_captions_input")
                )

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("YouTube Shorts Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = hookInput,
                    onValueChange = { hookInput = it },
                    label = { Text("Short Opening Hook (First 3s)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = thumbTextInput,
                    onValueChange = { thumbTextInput = it },
                    label = { Text("Suggested Thumbnail Text") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = seoDescInput,
                    onValueChange = { seoDescInput = it },
                    label = { Text("SEO-Friendly Description") },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = hashtagsInput,
                    onValueChange = { hashtagsInput = it },
                    label = { Text("Hashtags (space separated)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = onRegenerateTitleHashtags,
                    enabled = !isRegenerating,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    if (isRegenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = CyberMagenta
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = CyberMagenta,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = if (isRegenerating) "  Regenerating Viral Kit…" else "  AI Refresh Viral Kit (${subtitleLanguage.label})",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            commitEdits()
                            onExportMp4()
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(" Save & Export", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    GradientPrimaryButton(
                        text = "Apply Edits",
                        icon = Icons.Rounded.Save,
                        testTag = "long_clip_apply_edits_button",
                        onClick = {
                            commitEdits()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
