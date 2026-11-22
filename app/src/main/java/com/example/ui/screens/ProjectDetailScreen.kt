package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AspectRatioOption
import com.example.data.CaptionStyleOption
import com.example.data.StudioSettings
import com.example.data.VideoClipEntity
import com.example.data.VideoProjectEntity
import com.example.ui.Formatters
import com.example.ui.components.ClipPreviewDialog
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.StudioPillBadge
import com.example.ui.components.StudioSurfaceCard
import com.example.ui.components.VideoPreviewPlayer
import com.example.ui.components.VideoThumbnailImage
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.RoyalViolet

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectDetailScreen(
    project: VideoProjectEntity?,
    clips: List<VideoClipEntity>,
    settings: StudioSettings,
    onBack: () -> Unit,
    onAddClip: (VideoProjectEntity, String, Long, Long, AspectRatioOption, CaptionStyleOption) -> Unit,
    onToggleFavorite: (VideoClipEntity) -> Unit,
    onDeleteClip: (Long) -> Unit,
    onDeleteProject: (Long) -> Unit,
    onShowMessage: (String) -> Unit,
    onEditClip: (VideoClipEntity, String, Long, Long, AspectRatioOption, CaptionStyleOption) -> Unit = { _, _, _, _, _, _ -> },
    onExportClipMp4: (VideoClipEntity) -> Unit = {},
    exportingClipId: Long? = null,
    exportClipProgress: Int = 0,
    modifier: Modifier = Modifier
) {
    if (project == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Project not found",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onBack) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    val durationMs = project.durationMs.coerceAtLeast(1000L)
    var trimStartMs by remember(project.id) { mutableLongStateOf(0L) }
    var trimEndMs by remember(project.id) {
        mutableLongStateOf(minOf(durationMs, (settings.smartClipSeconds * 1000L).coerceAtLeast(2000L)))
    }
    var newClipTitle by remember(project.id, clips.size) {
        mutableStateOf("Clip #${clips.size + 1}")
    }
    var seekTriggerMs by remember { mutableStateOf<Long?>(null) }
    var previewingClip by remember { mutableStateOf<VideoClipEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (previewingClip != null) {
        val currentClip = clips.firstOrNull { it.id == previewingClip!!.id } ?: previewingClip!!
        ClipPreviewDialog(
            clip = currentClip,
            loopPlayback = settings.loopClipPreviews,
            onDismiss = { previewingClip = null },
            onToggleFavorite = { onToggleFavorite(currentClip) },
            onDelete = { onDeleteClip(currentClip.id) },
            onShareError = onShowMessage,
            onEditClip = onEditClip,
            onExportMp4 = onExportClipMp4,
            isExporting = exportingClipId == currentClip.id,
            exportProgress = if (exportingClipId == currentClip.id) exportClipProgress else 0
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Project?") },
            text = {
                Text("Remove \"${project.title}\" and its ${clips.size} saved clips from YodhaClip AI?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteProject(project.id)
                        onBack()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 880.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("project_detail_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back to projects",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = project.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = project.fileName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("project_detail_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.project_detail_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Live Video Preview Player
            item {
                VideoPreviewPlayer(
                    videoUri = project.videoUri,
                    durationMs = durationMs,
                    aspectRatio = AspectRatioOption.fromCode(project.aspectRatio),
                    captionStyle = CaptionStyleOption.fromCode(project.captionStyle),
                    loopPlayback = settings.loopClipPreviews,
                    seekTriggerMs = seekTriggerMs,
                    onSeekTriggerConsumed = { seekTriggerMs = null }
                )
            }

            // Technical Metadata Strip
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioPillBadge(text = "MP4", useMonoFont = true)
                    StudioPillBadge(
                        text = Formatters.formatDuration(project.durationMs),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        useMonoFont = true
                    )
                    StudioPillBadge(
                        text = Formatters.formatResolution(project.width, project.height),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        useMonoFont = true
                    )
                    StudioPillBadge(
                        text = project.aspectRatio,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        useMonoFont = true
                    )
                    if (project.fileSizeBytes > 0L) {
                        StudioPillBadge(
                            text = Formatters.formatFileSize(project.fileSizeBytes),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            useMonoFont = true
                        )
                    }
                }
            }

            // Quick Cut New Clip Card
            item {
                StudioSurfaceCard(
                    borderColor = RoyalViolet.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCut,
                                    contentDescription = null,
                                    tint = CyberMagenta,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.project_detail_add_clip),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            TextButton(onClick = { seekTriggerMs = trimStartMs }) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Preview")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StudioPillBadge(
                                text = "IN ${Formatters.formatDuration(trimStartMs)}",
                                useMonoFont = true
                            )
                            StudioPillBadge(
                                text = "LEN ${Formatters.formatDuration((trimEndMs - trimStartMs).coerceAtLeast(0L))}",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                useMonoFont = true
                            )
                            StudioPillBadge(
                                text = "OUT ${Formatters.formatDuration(trimEndMs)}",
                                useMonoFont = true
                            )
                        }

                        RangeSlider(
                            value = trimStartMs.toFloat().coerceIn(0f, durationMs.toFloat())..
                                trimEndMs.toFloat().coerceIn(trimStartMs.toFloat(), durationMs.toFloat()),
                            onValueChange = { range ->
                                trimStartMs = range.start.toLong()
                                trimEndMs = range.endInclusive.toLong()
                                    .coerceAtLeast((trimStartMs + 500L).coerceAtMost(durationMs))
                            },
                            valueRange = 0f..durationMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = CyberMagenta,
                                activeTrackColor = RoyalViolet,
                                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        OutlinedTextField(
                            value = newClipTitle,
                            onValueChange = { newClipTitle = it },
                            label = { Text("New Clip Title") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("project_detail_clip_title_input")
                        )

                        GradientPrimaryButton(
                            text = "Save Trimmed Clip",
                            icon = Icons.Rounded.Add,
                            onClick = {
                                onAddClip(
                                    project,
                                    newClipTitle,
                                    trimStartMs,
                                    trimEndMs,
                                    AspectRatioOption.fromCode(project.aspectRatio),
                                    CaptionStyleOption.fromCode(project.captionStyle)
                                )
                            },
                            testTag = "project_detail_save_clip_button",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Project Clips List
            item {
                Text(
                    text = "${stringResource(R.string.project_detail_clips_header)} (${clips.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (clips.isEmpty()) {
                item {
                    StudioSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No clips yet for this project. Use the trim slider above to create your first highlight!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                items(clips, key = { it.id }) { clip ->
                    ProjectClipItemRow(
                        clip = clip,
                        isExporting = exportingClipId == clip.id,
                        exportProgress = if (exportingClipId == clip.id) exportClipProgress else 0,
                        exportBusy = exportingClipId != null,
                        onPreview = { previewingClip = clip },
                        onExportMp4 = { onExportClipMp4(clip) },
                        onToggleFavorite = { onToggleFavorite(clip) },
                        onDelete = { onDeleteClip(clip.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }
        }
    }
}

@Composable
private fun ProjectClipItemRow(
    clip: VideoClipEntity,
    isExporting: Boolean,
    exportProgress: Int,
    exportBusy: Boolean,
    onPreview: () -> Unit,
    onExportMp4: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    StudioSurfaceCard(
        onClick = onPreview,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_clip_row_${clip.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VideoThumbnailImage(
                    thumbnailPath = clip.thumbnailPath,
                    contentDescription = clip.title,
                    modifier = Modifier
                        .size(width = 78.dp, height = 58.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = clip.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StudioPillBadge(
                            text = "${Formatters.formatDuration(clip.startMs)} - ${Formatters.formatDuration(clip.endMs)}",
                            useMonoFont = true
                        )
                        StudioPillBadge(
                            text = Formatters.formatDuration(clip.durationMs),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            useMonoFont = true
                        )
                    }
                }

                IconButton(
                    onClick = onExportMp4,
                    enabled = !exportBusy,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("project_clip_download_${clip.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = "Download MP4",
                        tint = EmeraldReady
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (clip.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                        contentDescription = "Toggle favorite",
                        tint = if (clip.isFavorite) AmberHighlight else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete clip",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (isExporting) {
                LinearProgressIndicator(
                    progress = { exportProgress.coerceIn(0, 100) / 100f },
                    color = EmeraldReady,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}
