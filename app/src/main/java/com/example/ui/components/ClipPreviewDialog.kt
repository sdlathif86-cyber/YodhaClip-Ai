package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.AspectRatioOption
import com.example.data.CaptionStyleOption
import com.example.data.VideoClipEntity
import com.example.ui.Formatters
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.RoyalViolet

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClipPreviewDialog(
    clip: VideoClipEntity,
    loopPlayback: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onShareError: (String) -> Unit,
    onEditClip: ((VideoClipEntity, String, Long, Long, AspectRatioOption, CaptionStyleOption) -> Unit)? = null,
    onExportMp4: ((VideoClipEntity) -> Unit)? = null,
    isExporting: Boolean = false,
    exportProgress: Int = 0
) {
    val context = LocalContext.current
    var isEditing by remember(clip.id) { mutableStateOf(false) }
    var editTitle by remember(clip.id, clip.title) { mutableStateOf(clip.title) }
    var editStartMs by remember(clip.id, clip.startMs) { mutableLongStateOf(clip.startMs) }
    var editEndMs by remember(clip.id, clip.endMs) { mutableLongStateOf(clip.endMs) }
    var editAspect by remember(clip.id, clip.aspectRatio) {
        mutableStateOf(AspectRatioOption.fromCode(clip.aspectRatio))
    }
    var editCaptionStyle by remember(clip.id, clip.captionStyle) {
        mutableStateOf(CaptionStyleOption.fromCode(clip.captionStyle))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = clip.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = clip.projectTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onEditClip != null) {
                            IconButton(
                                onClick = { isEditing = !isEditing },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("clip_dialog_edit_toggle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit clip",
                                    tint = if (isEditing) CyberMagenta else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("clip_dialog_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (clip.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                                contentDescription = if (clip.isFavorite) "Remove favorite" else "Mark favorite",
                                tint = if (clip.isFavorite) AmberHighlight else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("clip_dialog_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close clip preview",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioPillBadge(
                        text = "${Formatters.formatDuration(editStartMs)} - ${Formatters.formatDuration(editEndMs)}",
                        useMonoFont = true
                    )
                    StudioPillBadge(
                        text = Formatters.formatDuration((editEndMs - editStartMs).coerceAtLeast(1000L)),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        useMonoFont = true
                    )
                    StudioPillBadge(
                        text = editAspect.code,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                VideoPreviewPlayer(
                    videoUri = clip.videoUri,
                    durationMs = editEndMs.coerceAtLeast(1000L),
                    aspectRatio = editAspect,
                    captionStyle = editCaptionStyle,
                    clipStartMs = editStartMs,
                    clipEndMs = editEndMs,
                    autoPlay = true,
                    loopPlayback = loopPlayback
                )

                if (isEditing && onEditClip != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("Clip Title") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clip_edit_title_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { editStartMs = (editStartMs - 1_000L).coerceAtLeast(0L) },
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) { Text("-1s In", maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                            OutlinedButton(
                                onClick = { editStartMs = (editStartMs + 1_000L).coerceAtMost(editEndMs - 1_000L) },
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) { Text("+1s In", maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                            OutlinedButton(
                                onClick = { editEndMs = (editEndMs - 1_000L).coerceAtLeast(editStartMs + 1_000L) },
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) { Text("-1s Out", maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                            OutlinedButton(
                                onClick = { editEndMs += 1_000L },
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) { Text("+1s Out", maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AspectRatioOption.entries.forEach { option ->
                                FilterChip(
                                    selected = editAspect == option,
                                    onClick = { editAspect = option },
                                    label = { Text(option.code) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RoyalViolet.copy(alpha = 0.35f)
                                    )
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CaptionStyleOption.entries.forEach { style ->
                                FilterChip(
                                    selected = editCaptionStyle == style,
                                    onClick = { editCaptionStyle = style },
                                    label = { Text(style.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberMagenta.copy(alpha = 0.25f)
                                    )
                                )
                            }
                        }

                        GradientPrimaryButton(
                            text = "Save Clip Changes",
                            icon = Icons.Rounded.Save,
                            testTag = "clip_edit_save_button",
                            onClick = {
                                onEditClip(clip, editTitle, editStartMs, editEndMs, editAspect, editCaptionStyle)
                                isEditing = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (isExporting) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Exporting MP4 to Downloads/YodhaClipAI… ($exportProgress%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberMagenta
                        )
                        LinearProgressIndicator(
                            progress = { exportProgress / 100f },
                            color = CyberMagenta,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (onExportMp4 != null) {
                    GradientPrimaryButton(
                        text = if (isExporting) "Rendering MP4 ($exportProgress%)…" else "Download MP4",
                        icon = Icons.Rounded.Download,
                        enabled = !isExporting,
                        onClick = { onExportMp4(clip) },
                        testTag = "clip_dialog_download_mp4_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "video/mp4"
                                    putExtra(Intent.EXTRA_STREAM, Uri.parse(clip.videoUri))
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "${clip.title} (${Formatters.formatDuration(clip.startMs)}–${Formatters.formatDuration(clip.endMs)}) • Edited in YodhaClip AI"
                                    )
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Share Clip Video")
                                )
                            } catch (_: Exception) {
                                onShareError("Unable to open share sheet for this video file.")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("clip_dialog_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = stringResource(R.string.clip_share_action),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.clip_share_action), maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("clip_dialog_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.clip_delete_action),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Delete",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Done")
                }
            }
        }
    }
}
