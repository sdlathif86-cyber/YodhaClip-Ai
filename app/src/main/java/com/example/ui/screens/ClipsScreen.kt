package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AspectRatioOption
import com.example.data.CaptionStyleOption
import com.example.data.StudioSettings
import com.example.data.VideoClipEntity
import com.example.ui.Formatters
import com.example.ui.components.ClipPreviewDialog
import com.example.ui.components.StudioEmptyStateCard
import com.example.ui.components.StudioPillBadge
import com.example.ui.components.StudioSurfaceCard
import com.example.ui.components.VideoThumbnailImage
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RoyalViolet

private enum class ClipFilterMode {
    ALL, FAVORITES, RATIO_9_16, RATIO_1_1, RATIO_16_9
}

@Composable
fun ClipsScreen(
    clips: List<VideoClipEntity>,
    settings: StudioSettings,
    onNavigateToUpload: () -> Unit,
    onToggleFavorite: (VideoClipEntity) -> Unit,
    onDeleteClip: (Long) -> Unit,
    onShowMessage: (String) -> Unit,
    onEditClip: (VideoClipEntity, String, Long, Long, AspectRatioOption, CaptionStyleOption) -> Unit = { _, _, _, _, _, _ -> },
    onExportClipMp4: (VideoClipEntity) -> Unit = {},
    exportingClipId: Long? = null,
    exportClipProgress: Int = 0,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf(ClipFilterMode.ALL) }
    var activePreviewClipId by remember { mutableStateOf<Long?>(null) }

    val activeClip = remember(clips, activePreviewClipId) {
        clips.firstOrNull { it.id == activePreviewClipId }
    }

    if (activeClip != null) {
        ClipPreviewDialog(
            clip = activeClip,
            loopPlayback = settings.loopClipPreviews,
            onDismiss = { activePreviewClipId = null },
            onToggleFavorite = { onToggleFavorite(activeClip) },
            onDelete = { onDeleteClip(activeClip.id) },
            onShareError = onShowMessage,
            onEditClip = onEditClip,
            onExportMp4 = onExportClipMp4,
            isExporting = exportingClipId == activeClip.id,
            exportProgress = if (exportingClipId == activeClip.id) exportClipProgress else 0
        )
    }

    val filteredClips = remember(clips, searchQuery, filterMode) {
        clips.filter { clip ->
            val matchesQuery = searchQuery.isBlank() ||
                clip.title.contains(searchQuery, ignoreCase = true) ||
                clip.projectTitle.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (filterMode) {
                ClipFilterMode.ALL -> true
                ClipFilterMode.FAVORITES -> clip.isFavorite
                ClipFilterMode.RATIO_9_16 -> clip.aspectRatio == AspectRatioOption.PORTRAIT_9_16.code
                ClipFilterMode.RATIO_1_1 -> clip.aspectRatio == AspectRatioOption.SQUARE_1_1.code
                ClipFilterMode.RATIO_16_9 -> clip.aspectRatio == AspectRatioOption.LANDSCAPE_16_9.code
            }
            matchesQuery && matchesFilter
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 165.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 960.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.clips_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.clips_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (clips.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.clips_search_hint)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalViolet,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clips_search_input")
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val chipColors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        FilterChip(
                            selected = filterMode == ClipFilterMode.ALL,
                            onClick = { filterMode = ClipFilterMode.ALL },
                            label = { Text("All (${clips.size})") },
                            colors = chipColors
                        )
                        FilterChip(
                            selected = filterMode == ClipFilterMode.FAVORITES,
                            onClick = { filterMode = ClipFilterMode.FAVORITES },
                            label = { Text("Favorites") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = chipColors
                        )
                        FilterChip(
                            selected = filterMode == ClipFilterMode.RATIO_9_16,
                            onClick = { filterMode = ClipFilterMode.RATIO_9_16 },
                            label = { Text("9:16") },
                            colors = chipColors
                        )
                        FilterChip(
                            selected = filterMode == ClipFilterMode.RATIO_1_1,
                            onClick = { filterMode = ClipFilterMode.RATIO_1_1 },
                            label = { Text("1:1") },
                            colors = chipColors
                        )
                        FilterChip(
                            selected = filterMode == ClipFilterMode.RATIO_16_9,
                            onClick = { filterMode = ClipFilterMode.RATIO_16_9 },
                            label = { Text("16:9") },
                            colors = chipColors
                        )
                    }
                }
            }

            if (clips.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    StudioEmptyStateCard(
                        title = stringResource(R.string.clips_empty_title),
                        description = stringResource(R.string.clips_empty_desc),
                        ctaLabel = stringResource(R.string.home_cta_upload),
                        ctaIcon = Icons.Rounded.ContentCut,
                        onCtaClick = onNavigateToUpload,
                        ctaTestTag = "clips_empty_upload_button",
                        showArtwork = true
                    )
                }
            } else if (filteredClips.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    StudioSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No clips match this filter",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    filterMode = ClipFilterMode.ALL
                                }
                            ) {
                                Text("Show All Clips")
                            }
                        }
                    }
                }
            } else {
                items(filteredClips, key = { it.id }) { clip ->
                    val isExportingThis = exportingClipId == clip.id
                    ClipGridCard(
                        clip = clip,
                        isExporting = isExportingThis,
                        exportProgress = if (isExportingThis) exportClipProgress else 0,
                        onOpenPreview = { activePreviewClipId = clip.id },
                        onToggleFavorite = { onToggleFavorite(clip) },
                        onExportMp4 = { onExportClipMp4(clip) },
                        onDelete = { onDeleteClip(clip.id) }
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ClipGridCard(
    clip: VideoClipEntity,
    isExporting: Boolean,
    exportProgress: Int,
    onOpenPreview: () -> Unit,
    onToggleFavorite: () -> Unit,
    onExportMp4: () -> Unit,
    onDelete: () -> Unit
) {
    StudioSurfaceCard(
        onClick = onOpenPreview,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("clip_card_${clip.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(138.dp)
            ) {
                VideoThumbnailImage(
                    thumbnailPath = clip.thumbnailPath,
                    contentDescription = clip.title,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    ObsidianBlack.copy(alpha = 0.72f)
                                )
                            )
                        )
                )

                // Play emblem
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(RoyalViolet.copy(alpha = 0.86f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play ${clip.title}",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Aspect ratio tag
                StudioPillBadge(
                    text = clip.aspectRatio,
                    containerColor = ObsidianBlack.copy(alpha = 0.78f),
                    contentColor = Color.White,
                    useMonoFont = true,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )

                // Favorite star button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = if (clip.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                        contentDescription = if (clip.isFavorite) "Unfavorite clip" else "Favorite clip",
                        tint = if (clip.isFavorite) AmberHighlight else Color.White
                    )
                }

                // Time range badge
                StudioPillBadge(
                    text = "${Formatters.formatDuration(clip.startMs)}–${Formatters.formatDuration(clip.endMs)}",
                    containerColor = ObsidianBlack.copy(alpha = 0.82f),
                    contentColor = Color.White,
                    useMonoFont = true,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = clip.title,
                    style = MaterialTheme.typography.titleSmall,
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
                if (isExporting) {
                    LinearProgressIndicator(
                        progress = { exportProgress / 100f },
                        color = CyberMagenta,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenPreview,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("clip_edit_${clip.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit ${clip.title}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onExportMp4,
                        enabled = !isExporting,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("clip_download_${clip.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "Download MP4 for ${clip.title}",
                            tint = EmeraldReady,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("clip_delete_${clip.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete ${clip.title}",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
