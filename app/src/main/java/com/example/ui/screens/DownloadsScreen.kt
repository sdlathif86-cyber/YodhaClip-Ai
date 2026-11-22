package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ExportedDownloadEntity
import com.example.ui.Formatters
import com.example.ui.components.StudioEmptyStateCard
import com.example.ui.components.StudioSurfaceCard
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RoyalViolet

private enum class DownloadFilter(val label: String) {
    ALL("All Exports"),
    MP4("MP4 Clips"),
    SRT("Subtitles (.SRT)")
}

@Composable
fun DownloadsScreen(
    downloads: List<ExportedDownloadEntity>,
    onBack: () -> Unit,
    onDeleteDownload: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onOpenLongVideoShorts: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var filter by remember { mutableStateOf(DownloadFilter.ALL) }

    val filtered = remember(downloads, filter) {
        when (filter) {
            DownloadFilter.ALL -> downloads
            DownloadFilter.MP4 -> downloads.filter { it.mimeType.startsWith("video/") }
            DownloadFilter.SRT -> downloads.filter { !it.mimeType.startsWith("video/") }
        }
    }
    val totalBytes = remember(downloads) { downloads.sumOf { it.sizeBytes } }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("downloads_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Download Manager",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${downloads.size} exported file(s) • ${Formatters.formatFileSize(totalBytes)} in Downloads/YodhaClipAI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (downloads.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onClearHistory,
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("downloads_clear_all_button")
                        ) {
                            Text("Clear", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DownloadFilter.entries.forEach { mode ->
                        FilterChip(
                            selected = filter == mode,
                            onClick = { filter = mode },
                            label = { Text(mode.label, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalViolet.copy(alpha = 0.35f),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("downloads_filter_${mode.name.lowercase()}")
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    StudioEmptyStateCard(
                        title = "No Exported Files Yet",
                        description = "Export 9:16 MP4 clips or Telugu/Hindi/English .SRT subtitles from Long Video to Shorts, Live Stream to Shorts, or YouTube to Shorts to track and share them here.",
                        ctaLabel = "Open Long Video to Shorts",
                        ctaIcon = Icons.Rounded.DownloadDone,
                        onCtaClick = onOpenLongVideoShorts
                    )
                }
            } else {
                items(filtered, key = { it.id }) { item ->
                    val isVideo = item.mimeType.startsWith("video/")
                    StudioSurfaceCard(
                        borderColor = if (isVideo) RoyalViolet.copy(alpha = 0.4f) else CyberMagenta.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("download_item_${item.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isVideo) RoyalViolet.copy(alpha = 0.2f)
                                            else CyberMagenta.copy(alpha = 0.2f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isVideo) ElectricViolet.copy(alpha = 0.45f)
                                            else CyberMagenta.copy(alpha = 0.45f),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isVideo) Icons.Rounded.Movie else Icons.Rounded.Subtitles,
                                        contentDescription = null,
                                        tint = if (isVideo) ElectricViolet else CyberMagenta,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.fileName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.sourceModule} • ${item.aspectRatio} • ${Formatters.formatFileSize(item.sizeBytes)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonEmerald,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.locationLabel} • ${Formatters.formatDate(item.createdAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = item.mimeType
                                                putExtra(Intent.EXTRA_STREAM, Uri.parse(item.contentUri))
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share ${item.fileName}"))
                                        } catch (_: Exception) {
                                            onShowMessage("File saved in ${item.locationLabel}")
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    modifier = Modifier.weight(1f).height(42.dp)
                                ) {
                                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Share", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { onDeleteDownload(item.id) },
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    modifier = Modifier.height(42.dp)
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove entry", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Remove", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}
