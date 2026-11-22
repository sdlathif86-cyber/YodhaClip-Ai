package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.ai.ShortClipSuggestion
import com.example.ai.SubtitleLanguage
import com.example.data.AspectRatioOption
import com.example.data.CaptionStyleOption
import com.example.data.ExportedShortResult
import com.example.data.InspectedVideoInfo
import com.example.ui.Formatters
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RoyalViolet
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YouTubeShortPreviewDialog(
    clip: ShortClipSuggestion,
    originalVideo: InspectedVideoInfo?,
    youtubeThumbnailUrl: String?,
    subtitleLanguage: SubtitleLanguage,
    exportedFile: ExportedShortResult?,
    isExporting: Boolean,
    exportProgressPercent: Int,
    loopPlayback: Boolean,
    onLanguageChange: (SubtitleLanguage) -> Unit,
    onRequestUploadOriginal: () -> Unit,
    onDownloadMp4: () -> Unit,
    onDownloadSrt: () -> Unit,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val subtitleLines = remember(clip, subtitleLanguage) { clip.subtitlesFor(subtitleLanguage) }
    var activeSubtitleIndex by remember(clip.id, subtitleLanguage) { mutableIntStateOf(0) }

    LaunchedEffect(subtitleLines) {
        if (subtitleLines.size > 1) {
            while (true) {
                delay(2600L)
                activeSubtitleIndex = (activeSubtitleIndex + 1) % subtitleLines.size
            }
        }
    }

    val currentSubtitle = subtitleLines.getOrNull(activeSubtitleIndex).orEmpty()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StudioPillBadge(
                                text = "#${clip.rank} • ${clip.viralScore}% Viral",
                                containerColor = EmeraldReady.copy(alpha = 0.16f),
                                contentColor = EmeraldReady,
                                useMonoFont = true
                            )
                            StudioPillBadge(
                                text = "9:16 SHORTS",
                                useMonoFont = true
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = clip.hookTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("yt_preview_dialog_close")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close 9:16 preview",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Subtitle language tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubtitleLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = subtitleLanguage == lang,
                            onClick = { onLanguageChange(lang) },
                            label = { Text(lang.nativeLabel, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 9:16 Video Player OR Storyboard preview when original file not uploaded yet
                if (originalVideo != null) {
                    val safeEnd = clip.endMs.coerceAtMost(originalVideo.durationMs)
                    val safeStart = clip.startMs.coerceAtMost((safeEnd - 1000L).coerceAtLeast(0L))
                    VideoPreviewPlayer(
                        videoUri = originalVideo.uri,
                        durationMs = originalVideo.durationMs,
                        aspectRatio = AspectRatioOption.PORTRAIT_9_16,
                        captionStyle = CaptionStyleOption.NONE,
                        clipStartMs = safeStart,
                        clipEndMs = safeEnd,
                        autoPlay = true,
                        loopPlayback = loopPlayback,
                        customCaptionText = currentSubtitle
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(ObsidianBlack)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .height(270.dp)
                                .aspectRatio(9f / 16f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.2.dp, RoyalViolet, RoundedCornerShape(16.dp))
                        ) {
                            if (!youtubeThumbnailUrl.isNullOrBlank()) {
                                SubcomposeAsyncImage(
                                    model = youtubeThumbnailUrl,
                                    contentDescription = clip.hookTitle,
                                    contentScale = ContentScale.Crop,
                                    loading = {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp,
                                                color = RoyalViolet
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                ObsidianBlack.copy(alpha = 0.35f),
                                                ObsidianBlack.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                            )
                            StudioPillBadge(
                                text = "${Formatters.formatDuration(clip.startMs)}–${Formatters.formatDuration(clip.endMs)}",
                                containerColor = ObsidianBlack.copy(alpha = 0.82f),
                                contentColor = Color.White,
                                useMonoFont = true,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            )
                            if (currentSubtitle.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(RoyalViolet.copy(alpha = 0.92f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = currentSubtitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onRequestUploadOriginal()
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CyberMagenta),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Original MP4 to Play & Export")
                    }
                }

                // AI Hashtags + Copy
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "AI Hashtags & Subtitles (${subtitleLanguage.label})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = {
                                val payload = "${clip.hookTitle}\n\n${clip.hashtags.joinToString(" ")}"
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("YodhaClip Short", payload))
                                onMessage("Copied AI title & hashtags!")
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy title and hashtags",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        clip.hashtags.forEach { tag ->
                            StudioPillBadge(
                                text = tag,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Full timed subtitle lines preview
                StudioSurfaceCard(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subtitleLines.forEachIndexed { idx, line ->
                            Text(
                                text = "${idx + 1}. $line",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (isExporting) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Converting to 9:16 MP4… $exportProgressPercent%",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberMagenta
                        )
                        LinearProgressIndicator(
                            progress = { (exportProgressPercent / 100f).coerceIn(0f, 1f) },
                            color = RoyalViolet,
                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (exportedFile != null) {
                    StudioSurfaceCard(
                        borderColor = EmeraldReady.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "MP4 Downloaded",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldReady
                                )
                                Text(
                                    text = exportedFile.locationLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "video/mp4"
                                            putExtra(Intent.EXTRA_STREAM, exportedFile.contentUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share 9:16 Short MP4"))
                                    } catch (_: Exception) {
                                        onMessage("Unable to open share sheet for exported MP4.")
                                    }
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "Share exported MP4",
                                    tint = EmeraldReady
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GradientPrimaryButton(
                        text = if (isExporting) "Exporting…" else "Download 9:16 MP4",
                        icon = Icons.Rounded.Download,
                        enabled = !isExporting,
                        onClick = onDownloadMp4,
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedButton(
                        onClick = onDownloadSrt,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Subtitles,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(".SRT", maxLines = 1)
                    }
                }
            }
        }
    }
}
