package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AspectRatioOption
import com.example.data.VideoClipEntity
import com.example.data.VideoProjectEntity
import com.example.ui.Formatters
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.StudioEmptyStateCard
import com.example.ui.components.StudioPillBadge
import com.example.ui.components.StudioSurfaceCard
import com.example.ui.components.VideoThumbnailImage
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RoyalViolet

@Composable
fun ProjectsScreen(
    projects: List<VideoProjectEntity>,
    clips: List<VideoClipEntity>,
    onNavigateToUpload: () -> Unit,
    onOpenProject: (Long) -> Unit,
    onDeleteProject: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedAspectFilter by remember { mutableStateOf<String?>(null) }
    var projectPendingDelete by remember { mutableStateOf<VideoProjectEntity?>(null) }

    val clipCounts = remember(clips) { clips.groupingBy { it.projectId }.eachCount() }
    val clipTotalDurations = remember(clips) {
        clips.groupBy { it.projectId }.mapValues { (_, list) -> list.sumOf { it.durationMs } }
    }
    val filteredProjects = remember(projects, searchQuery, selectedAspectFilter) {
        projects.filter { project ->
            val matchesQuery = searchQuery.isBlank() ||
                project.title.contains(searchQuery, ignoreCase = true) ||
                project.fileName.contains(searchQuery, ignoreCase = true)
            val matchesRatio = selectedAspectFilter == null ||
                project.aspectRatio == selectedAspectFilter
            matchesQuery && matchesRatio
        }
    }

    if (projectPendingDelete != null) {
        val target = projectPendingDelete!!
        AlertDialog(
            onDismissRequest = { projectPendingDelete = null },
            title = { Text("Delete Project?") },
            text = {
                Text(
                    "Remove \"${target.title}\" and all ${clipCounts[target.id] ?: 0} saved clips from YodhaClip AI? Your original MP4 file on device will not be deleted."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProject(target.id)
                        projectPendingDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectPendingDelete = null }) {
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.projects_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.projects_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    GradientPrimaryButton(
                        text = "New MP4",
                        icon = Icons.Rounded.Add,
                        onClick = onNavigateToUpload,
                        testTag = "projects_new_upload_button",
                        modifier = Modifier.width(136.dp)
                    )
                }
            }

            if (projects.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.projects_search_hint)) },
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
                            .testTag("projects_search_input")
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedAspectFilter == null,
                            onClick = { selectedAspectFilter = null },
                            label = { Text("All (${projects.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        AspectRatioOption.entries.forEach { option ->
                            FilterChip(
                                selected = selectedAspectFilter == option.code,
                                onClick = {
                                    selectedAspectFilter =
                                        if (selectedAspectFilter == option.code) null else option.code
                                },
                                label = { Text(option.code) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            if (projects.isEmpty()) {
                item {
                    StudioEmptyStateCard(
                        title = stringResource(R.string.projects_empty_title),
                        description = stringResource(R.string.projects_empty_desc),
                        ctaLabel = stringResource(R.string.home_cta_upload),
                        ctaIcon = Icons.Rounded.CloudUpload,
                        onCtaClick = onNavigateToUpload,
                        ctaTestTag = "projects_empty_upload_button",
                        showArtwork = true
                    )
                }
            } else if (filteredProjects.isEmpty()) {
                item {
                    StudioSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No matching projects",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Try clearing your search or aspect ratio filter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedAspectFilter = null
                                }
                            ) {
                                Text("Clear Filters")
                            }
                        }
                    }
                }
            } else {
                items(filteredProjects, key = { it.id }) { project ->
                    ProjectWorkspaceCard(
                        project = project,
                        clipCount = clipCounts[project.id] ?: 0,
                        totalClipDurationMs = clipTotalDurations[project.id] ?: 0L,
                        onOpen = { onOpenProject(project.id) },
                        onDelete = { projectPendingDelete = project }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun ProjectWorkspaceCard(
    project: VideoProjectEntity,
    clipCount: Int,
    totalClipDurationMs: Long,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    StudioSurfaceCard(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_card_${project.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
            ) {
                VideoThumbnailImage(
                    thumbnailPath = project.thumbnailPath,
                    contentDescription = project.title,
                    modifier = Modifier.fillMaxSize()
                )
                StudioPillBadge(
                    text = project.aspectRatio,
                    containerColor = ObsidianBlack.copy(alpha = 0.8f),
                    contentColor = Color.White,
                    useMonoFont = true,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                )
                StudioPillBadge(
                    text = Formatters.formatDuration(project.durationMs),
                    containerColor = ObsidianBlack.copy(alpha = 0.8f),
                    contentColor = Color.White,
                    useMonoFont = true,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StudioPillBadge(text = "$clipCount clips")
                        if (totalClipDurationMs > 0L) {
                            StudioPillBadge(
                                text = "Clips ${Formatters.formatDuration(totalClipDurationMs)}",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                useMonoFont = true
                            )
                        }
                        StudioPillBadge(
                            text = project.exportQuality,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            useMonoFont = true
                        )
                    }
                    Text(
                        text = "${Formatters.formatDate(project.createdAt)} • ${Formatters.formatFileSize(project.fileSizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onOpen,
                        modifier = Modifier.testTag("project_resume_${project.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume", maxLines = 1)
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("project_delete_${project.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete ${project.title}",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
