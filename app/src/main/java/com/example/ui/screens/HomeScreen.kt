package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.StudioSettings
import com.example.data.VideoClipEntity
import com.example.data.VideoProjectEntity
import com.example.ui.Formatters
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.StudioEmptyStateCard
import com.example.ui.components.StudioPillBadge
import com.example.ui.components.StudioSurfaceCard
import com.example.ui.components.VideoThumbnailImage
import com.example.ui.components.YodhaClipLogo
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.ElectricAmethyst
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RoyalViolet

@Composable
fun HomeScreen(
    projects: List<VideoProjectEntity>,
    clips: List<VideoClipEntity>,
    downloadCount: Int,
    settings: StudioSettings,
    onNavigateToUpload: () -> Unit,
    onNavigateToLongVideoShorts: () -> Unit,
    onNavigateToLiveStreamShorts: () -> Unit,
    onNavigateToYouTubeShorts: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToClips: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenProject: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalDurationMs = projects.sumOf { it.durationMs }
    val clipsCountByProject = clips.groupingBy { it.projectId }.eachCount()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 880.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Branded Top Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    YodhaClipLogo(
                        showTagline = true,
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        onClick = onNavigateToUpload,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, RoyalViolet.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("header_upload_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudUpload,
                                contentDescription = stringResource(R.string.home_cta_upload),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.nav_upload),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Hero Studio Banner Card
            item {
                StudioSurfaceCard(
                    borderColor = RoyalViolet.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_studio_hero_1795160683135),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            ObsidianBlack.copy(alpha = 0.58f),
                                            ObsidianBlack.copy(alpha = 0.88f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(EmeraldReady.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldReady)
                                )
                                Text(
                                    text = stringResource(R.string.home_hero_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldReady,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = stringResource(R.string.home_hero_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.home_hero_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD6CEEE)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GradientPrimaryButton(
                                    text = stringResource(R.string.home_cta_create_shorts),
                                    icon = Icons.Rounded.AutoAwesome,
                                    onClick = onNavigateToLongVideoShorts,
                                    testTag = "home_create_shorts_button",
                                    modifier = Modifier.weight(1.3f)
                                )

                                OutlinedButton(
                                    onClick = onNavigateToUpload,
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = ObsidianBlack.copy(alpha = 0.45f),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("home_upload_button")
                                ) {
                                    Text(
                                        text = stringResource(R.string.home_cta_upload),
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // MAIN MODULE 1: Long Video to Shorts AI
            item {
                StudioSurfaceCard(
                    onClick = onNavigateToLongVideoShorts,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    borderColor = RoyalViolet.copy(alpha = 0.65f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_long_video_shorts_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StudioPillBadge(
                                text = "MODULE 1 • 10 / 20 / 30 MOMENTS",
                                containerColor = RoyalViolet.copy(alpha = 0.2f),
                                contentColor = ElectricAmethyst,
                                useMonoFont = true
                            )
                            Icon(
                                imageVector = Icons.Rounded.MovieCreation,
                                contentDescription = null,
                                tint = ElectricAmethyst,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Long Video to Shorts AI",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Upload gallery MP4, generate AI speech transcripts, detect 10/20/30 viral moments (15–60s), edit timelines, add Telugu/Hindi/English captions & export 9:16 MP4s.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        GradientPrimaryButton(
                            text = "Open Long Video to Shorts",
                            icon = Icons.Rounded.MovieCreation,
                            onClick = onNavigateToLongVideoShorts,
                            testTag = "home_long_video_shorts_button",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // MAIN MODULE 2: Live Stream to Shorts AI
            item {
                StudioSurfaceCard(
                    onClick = onNavigateToLiveStreamShorts,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    borderColor = EmeraldReady.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_live_stream_shorts_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StudioPillBadge(
                                text = "MODULE 2 • HLS & RTMP LIVE BUFFER",
                                containerColor = EmeraldReady.copy(alpha = 0.16f),
                                contentColor = EmeraldReady,
                                useMonoFont = true
                            )
                            Icon(
                                imageVector = Icons.Rounded.LiveTv,
                                contentDescription = null,
                                tint = EmeraldReady,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Live Stream to Shorts AI",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Connect an authorized HLS (.m3u8) or RTMP stream, run a rolling video buffer, detect gaming highlights & reactions in near real time, and review/approve clips.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = onNavigateToLiveStreamShorts,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, EmeraldReady.copy(alpha = 0.65f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("home_live_stream_shorts_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LiveTv,
                                contentDescription = null,
                                tint = EmeraldReady,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Open Live Stream Studio",
                                color = EmeraldReady,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Featured YouTube Video to Shorts AI Studio Card
            item {
                StudioSurfaceCard(
                    onClick = onNavigateToYouTubeShorts,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    borderColor = CyberMagenta.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_yt_shorts_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StudioPillBadge(
                                text = stringResource(R.string.home_yt_shorts_badge),
                                containerColor = CyberMagenta.copy(alpha = 0.16f),
                                contentColor = CyberMagenta,
                                useMonoFont = true
                            )
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = CyberMagenta,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.home_yt_shorts_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.home_yt_shorts_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        GradientPrimaryButton(
                            text = stringResource(R.string.home_yt_shorts_cta),
                            icon = Icons.Rounded.AutoAwesome,
                            onClick = onNavigateToYouTubeShorts,
                            testTag = "home_yt_shorts_button",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Live Studio Metrics Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudioMetricTile(
                        icon = Icons.Rounded.FolderOpen,
                        label = stringResource(R.string.home_stat_projects),
                        value = projects.size.toString(),
                        accent = RoyalViolet,
                        onClick = onNavigateToProjects,
                        modifier = Modifier.weight(1f)
                    )
                    StudioMetricTile(
                        icon = Icons.Rounded.ContentCut,
                        label = stringResource(R.string.home_stat_clips),
                        value = clips.size.toString(),
                        accent = CyberMagenta,
                        onClick = onNavigateToClips,
                        modifier = Modifier.weight(1f)
                    )
                    StudioMetricTile(
                        icon = Icons.Rounded.DownloadDone,
                        label = "Exports",
                        value = downloadCount.toString(),
                        accent = EmeraldReady,
                        onClick = onNavigateToDownloads,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_downloads_metric")
                    )
                }
            }

            // Beginner 3-Step Quickstart Guide
            item {
                AnimatedVisibility(visible = settings.showBeginnerTips) {
                    StudioSurfaceCard(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = CyberMagenta,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.home_beginner_guide_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = stringResource(R.string.home_beginner_guide_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                            )

                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                if (maxWidth < 460.dp) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        BeginnerStepChip(
                                            title = stringResource(R.string.home_step_1_title),
                                            desc = stringResource(R.string.home_step_1_desc),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        BeginnerStepChip(
                                            title = stringResource(R.string.home_step_2_title),
                                            desc = stringResource(R.string.home_step_2_desc),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        BeginnerStepChip(
                                            title = stringResource(R.string.home_step_3_title),
                                            desc = stringResource(R.string.home_step_3_desc),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        BeginnerStepChip(
                                            title = stringResource(R.string.home_step_1_title),
                                            desc = stringResource(R.string.home_step_1_desc),
                                            modifier = Modifier.weight(1f)
                                        )
                                        BeginnerStepChip(
                                            title = stringResource(R.string.home_step_2_title),
                                            desc = stringResource(R.string.home_step_2_desc),
                                            modifier = Modifier.weight(1f)
                                        )
                                        BeginnerStepChip(
                                            title = stringResource(R.string.home_step_3_title),
                                            desc = stringResource(R.string.home_step_3_desc),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.home_quick_actions),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionCard(
                            title = "Upload MP4",
                            subtitle = "Files & Gallery picker",
                            icon = Icons.Rounded.CloudUpload,
                            accent = RoyalViolet,
                            onClick = onNavigateToUpload,
                            testTag = "quick_action_upload",
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            title = "YouTube Link",
                            subtitle = "Authorized AI Shorts",
                            icon = Icons.Rounded.AutoAwesome,
                            accent = CyberMagenta,
                            onClick = onNavigateToYouTubeShorts,
                            testTag = "quick_action_youtube",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionCard(
                            title = "My Clips",
                            subtitle = "${clips.size} total clips created",
                            icon = Icons.Rounded.ContentCut,
                            accent = ElectricAmethyst,
                            onClick = onNavigateToClips,
                            testTag = "quick_action_clips",
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            title = "Settings",
                            subtitle = "${settings.exportQuality.code} • ${settings.smartClipSeconds}s",
                            icon = Icons.Rounded.Settings,
                            accent = EmeraldReady,
                            onClick = onNavigateToSettings,
                            testTag = "quick_action_settings",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Recent Projects Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.home_recent_projects),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (projects.isNotEmpty()) {
                        TextButton(onClick = onNavigateToProjects) {
                            Text(text = stringResource(R.string.home_view_all))
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (projects.isEmpty()) {
                item {
                    StudioEmptyStateCard(
                        title = stringResource(R.string.home_empty_projects_title),
                        description = stringResource(R.string.home_empty_projects_desc),
                        ctaLabel = stringResource(R.string.home_cta_upload),
                        ctaIcon = Icons.Rounded.MovieCreation,
                        onCtaClick = onNavigateToUpload,
                        ctaTestTag = "home_empty_upload_button",
                        showArtwork = true
                    )
                }
            } else {
                items(projects.take(3), key = { it.id }) { project ->
                    RecentProjectRowCard(
                        project = project,
                        clipCount = clipsCountByProject[project.id] ?: 0,
                        onClick = { onOpenProject(project.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun StudioMetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    StudioSurfaceCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BeginnerStepChip(
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    StudioSurfaceCard(
        onClick = onClick,
        modifier = modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RecentProjectRowCard(
    project: VideoProjectEntity,
    clipCount: Int,
    onClick: () -> Unit
) {
    StudioSurfaceCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_project_card_${project.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 92.dp, height = 68.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                VideoThumbnailImage(
                    thumbnailPath = project.thumbnailPath,
                    contentDescription = project.title,
                    modifier = Modifier.fillMaxSize()
                )
                StudioPillBadge(
                    text = Formatters.formatDuration(project.durationMs),
                    containerColor = ObsidianBlack.copy(alpha = 0.78f),
                    contentColor = Color.White,
                    useMonoFont = true,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                )
            }

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
                    StudioPillBadge(
                        text = project.aspectRatio,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${Formatters.formatDate(project.createdAt)} • ${Formatters.formatFileSize(project.fileSizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
