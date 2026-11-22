package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.VideoLibrary
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.ai.ShortClipSuggestion
import com.example.ai.SubtitleLanguage
import com.example.data.ExportedShortResult
import com.example.data.StudioSettings
import com.example.data.YouTubeVideoMetadata
import com.example.ui.Formatters
import com.example.ui.MetadataFetchState
import com.example.ui.ShortsAiState
import com.example.ui.YouTubeShortsUiState
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.StudioPillBadge
import com.example.ui.components.StudioSurfaceCard
import com.example.ui.components.YouTubeShortPreviewDialog
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RoyalViolet

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YouTubeShortsScreen(
    state: YouTubeShortsUiState,
    settings: StudioSettings,
    onBack: () -> Unit,
    onUrlChange: (String) -> Unit,
    onFetchMetadata: () -> Unit,
    onOwnershipConfirmedChange: (Boolean) -> Unit,
    onOriginalVideoPicked: (android.net.Uri, Boolean) -> Unit,
    onClearOriginalVideo: () -> Unit,
    onTranscriptChange: (String) -> Unit,
    onTranscriptFilePicked: (android.net.Uri) -> Unit,
    onClipCountChange: (Int) -> Unit,
    onSubtitleLanguageChange: (SubtitleLanguage) -> Unit,
    onConvertTo9x16Change: (Boolean) -> Unit,
    onAnalyzeWithAi: () -> Unit,
    onToggleClipSelection: (String) -> Unit,
    onSelectAllClips: () -> Unit,
    onSelectTopFiveClips: () -> Unit,
    onClearSelection: () -> Unit,
    onDownloadSingleMp4: (ShortClipSuggestion) -> Unit,
    onDownloadSelectedBatch: () -> Unit,
    onDownloadSubtitlesSrt: (ShortClipSuggestion) -> Unit,
    onSaveSelectedToLibrary: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var previewingClip by remember { mutableStateOf<ShortClipSuggestion?>(null) }

    val mp4Picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onOriginalVideoPicked(uri, true)
    }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onOriginalVideoPicked(uri, false)
    }

    val transcriptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onTranscriptFilePicked(uri)
    }

    val metadata = (state.metadataState as? MetadataFetchState.Success)?.metadata

    if (previewingClip != null) {
        val clip = previewingClip!!
        YouTubeShortPreviewDialog(
            clip = clip,
            originalVideo = state.originalVideo,
            youtubeThumbnailUrl = metadata?.thumbnailUrl,
            subtitleLanguage = state.subtitleLanguage,
            exportedFile = state.exportedMp4Map[clip.id],
            isExporting = state.exportingClipId == clip.id,
            exportProgressPercent = state.exportProgressPercent,
            loopPlayback = settings.loopClipPreviews,
            onLanguageChange = onSubtitleLanguageChange,
            onRequestUploadOriginal = { mp4Picker.launch(arrayOf("video/mp4", "video/quicktime", "video/webm", "video/*")) },
            onDownloadMp4 = { onDownloadSingleMp4(clip) },
            onDownloadSrt = { onDownloadSubtitlesSrt(clip) },
            onMessage = onShowMessage,
            onDismiss = { previewingClip = null }
        )
    }

    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
    )

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
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("yt_shorts_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.yt_shorts_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.yt_shorts_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StudioPillBadge(
                        text = "GEMINI AI",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        useMonoFont = true
                    )
                }
            }

            // STEP 1: YouTube URL + Fetch Official Metadata
            item {
                StudioSurfaceCard(
                    borderColor = RoyalViolet.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StepTitleHeader(
                            step = "STEP 1",
                            title = "YouTube URL & Official Metadata",
                            subtitle = "Uses official YouTube APIs for permitted metadata only."
                        )

                        OutlinedTextField(
                            value = state.urlInput,
                            onValueChange = onUrlChange,
                            label = { Text(stringResource(R.string.yt_url_label)) },
                            placeholder = { Text(stringResource(R.string.yt_url_placeholder)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Link,
                                    contentDescription = null,
                                    tint = RoyalViolet
                                )
                            },
                            trailingIcon = {
                                if (state.urlInput.isNotEmpty()) {
                                    IconButton(onClick = { onUrlChange("") }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Clear URL"
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoyalViolet,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("yt_url_input")
                        )

                        GradientPrimaryButton(
                            text = if (state.metadataState is MetadataFetchState.Loading) {
                                "Fetching Official Details…"
                            } else {
                                stringResource(R.string.yt_fetch_details_btn)
                            },
                            icon = Icons.Rounded.Link,
                            enabled = state.metadataState !is MetadataFetchState.Loading,
                            onClick = onFetchMetadata,
                            testTag = "yt_fetch_details_button",
                            modifier = Modifier.fillMaxWidth()
                        )

                        when (val metaState = state.metadataState) {
                            is MetadataFetchState.Loading -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = RoyalViolet
                                    )
                                    Text(
                                        text = "Fetching official YouTube title & thumbnail…",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            is MetadataFetchState.Error -> {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = metaState.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        TextButton(onClick = onFetchMetadata) {
                                            Text("Retry Fetch")
                                        }
                                    }
                                }
                            }

                            is MetadataFetchState.Success -> {
                                YouTubeMetadataPreviewBlock(metadata = metaState.metadata)
                            }

                            MetadataFetchState.Idle -> Unit
                        }
                    }
                }
            }

            // STEP 2: Authorized Original Video Upload + 9:16 Toggle
            item {
                StudioSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StepTitleHeader(
                            step = "STEP 2",
                            title = "Original Video Upload & Rights",
                            subtitle = "Supports videos you own or have permission to edit."
                        )

                        // Ownership / Permission confirmation checkbox
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(
                                1.dp,
                                if (state.ownershipConfirmed) EmeraldReady.copy(alpha = 0.55f)
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = state.ownershipConfirmed,
                                    onCheckedChange = onOwnershipConfirmedChange,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = EmeraldReady,
                                        checkmarkColor = ObsidianBlack
                                    ),
                                    modifier = Modifier.testTag("yt_ownership_checkbox")
                                )
                                Text(
                                    text = stringResource(R.string.yt_ownership_confirm_label),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (state.isInspectingOriginal) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = RoyalViolet
                                )
                                Text(
                                    text = "Inspecting original MP4 video…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (state.originalVideo == null) {
                            // Clear compliance notice asking user to upload original file
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AmberHighlight.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, AmberHighlight.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = null,
                                        tint = AmberHighlight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.yt_original_file_unavailable_notice),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GradientPrimaryButton(
                                    text = "Upload MP4",
                                    icon = Icons.Rounded.VideoFile,
                                    onClick = { mp4Picker.launch(arrayOf("video/mp4", "video/quicktime", "video/webm", "video/*")) },
                                    testTag = "yt_upload_original_mp4_button",
                                    modifier = Modifier.weight(1.15f)
                                )
                                OutlinedButton(
                                    onClick = {
                                        galleryPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .weight(0.95f)
                                        .height(48.dp)
                                        .testTag("yt_upload_gallery_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VideoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gallery", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        } else {
                            val original = state.originalVideo
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldReady.copy(alpha = 0.10f),
                                border = BorderStroke(1.dp, EmeraldReady.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = EmeraldReady,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = original.fileName,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        TextButton(onClick = onClearOriginalVideo) {
                                            Text("Change")
                                        }
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        StudioPillBadge(
                                            text = Formatters.formatDuration(original.durationMs),
                                            useMonoFont = true
                                        )
                                        StudioPillBadge(
                                            text = Formatters.formatResolution(original.width, original.height),
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurface,
                                            useMonoFont = true
                                        )
                                        if (original.fileSizeBytes > 0L) {
                                            StudioPillBadge(
                                                text = Formatters.formatFileSize(original.fileSizeBytes),
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                useMonoFont = true
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 9:16 Vertical Shorts Conversion Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = "Auto-Convert to 9:16 Vertical Shorts",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Center-crop & format MP4 exports for YouTube Shorts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.convertTo9x16,
                                onCheckedChange = onConvertTo9x16Change,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = RoyalViolet
                                ),
                                modifier = Modifier.testTag("yt_convert_9x16_switch")
                            )
                        }
                    }
                }
            }

            // STEP 3: AI Transcript Analysis, 5/10/20 Clips & Telugu/Hindi/English Subtitles
            item {
                StudioSurfaceCard(
                    borderColor = CyberMagenta.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StepTitleHeader(
                                step = "STEP 3",
                                title = "AI Transcript & Moment Finder",
                                subtitle = "Gemini AI finds hooks, titles, hashtags & subtitles.",
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    transcriptPicker.launch(arrayOf("text/plain", "text/*", "application/x-subrip"))
                                },
                                modifier = Modifier.testTag("yt_import_transcript_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import .SRT")
                            }
                        }

                        OutlinedTextField(
                            value = state.transcriptInput,
                            onValueChange = onTranscriptChange,
                            label = { Text(stringResource(R.string.yt_transcript_label)) },
                            placeholder = { Text(stringResource(R.string.yt_transcript_placeholder)) },
                            minLines = 3,
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoyalViolet,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("yt_transcript_input")
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Clip Suggestions Count",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(5, 10, 20).forEach { count ->
                                    FilterChip(
                                        selected = state.requestedClipCount == count,
                                        onClick = { onClipCountChange(count) },
                                        label = { Text("$count Clips") },
                                        colors = chipColors,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("yt_count_$count")
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Subtitle Language (Live Switchable)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SubtitleLanguage.entries.forEach { lang ->
                                    FilterChip(
                                        selected = state.subtitleLanguage == lang,
                                        onClick = { onSubtitleLanguageChange(lang) },
                                        label = { Text(lang.nativeLabel, maxLines = 1) },
                                        colors = chipColors,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("yt_lang_${lang.code}")
                                    )
                                }
                            }
                        }

                        GradientPrimaryButton(
                            text = if (state.aiState is ShortsAiState.Loading) {
                                "Analyzing Transcript & Moments…"
                            } else {
                                "Analyze & Suggest ${state.requestedClipCount} Shorts"
                            },
                            icon = Icons.Rounded.AutoAwesome,
                            enabled = state.aiState !is ShortsAiState.Loading,
                            onClick = onAnalyzeWithAi,
                            testTag = "yt_analyze_ai_button",
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (state.aiState is ShortsAiState.Loading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = CyberMagenta
                                )
                                Text(
                                    text = "Gemini AI is detecting best moments, viral titles, hashtags & Telugu/Hindi/English subtitles…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (state.aiState is ShortsAiState.Error) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = state.aiState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    TextButton(onClick = onAnalyzeWithAi) {
                                        Text("Retry AI Analysis")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // STEP 4: AI Results, Clip Selection, Batch 9:16 MP4 Downloads & Save to My Clips
            if (state.aiState is ShortsAiState.Success) {
                val analysis = state.aiState.result

                item {
                    StudioSurfaceCard(
                        borderColor = EmeraldReady.copy(alpha = 0.45f),
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
                                Text(
                                    text = "AI Transcript Analysis",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                StudioPillBadge(
                                    text = analysis.detectedTopic,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Text(
                                text = analysis.transcriptSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Selection toolbar
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StudioPillBadge(
                                    text = "${state.selectedClipIds.size} / ${analysis.suggestions.size} Selected",
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    useMonoFont = true
                                )
                                TextButton(onClick = onSelectTopFiveClips) { Text("Top 5") }
                                TextButton(onClick = onSelectAllClips) { Text("Select All") }
                                TextButton(onClick = onClearSelection) { Text("Clear") }
                            }

                            if (state.isBatchExporting && state.batchExportStatus != null) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "${state.batchExportStatus} (${state.exportProgressPercent}%)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CyberMagenta
                                    )
                                    LinearProgressIndicator(
                                        progress = { (state.exportProgressPercent / 100f).coerceIn(0f, 1f) },
                                        color = RoyalViolet,
                                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GradientPrimaryButton(
                                    text = if (state.isBatchExporting) {
                                        "Exporting…"
                                    } else {
                                        "Download ${state.selectedClipIds.size} MP4s"
                                    },
                                    icon = Icons.Rounded.Download,
                                    enabled = state.selectedClipIds.isNotEmpty() && !state.isBatchExporting,
                                    onClick = onDownloadSelectedBatch,
                                    testTag = "yt_batch_download_button",
                                    modifier = Modifier.weight(1.2f)
                                )
                                OutlinedButton(
                                    onClick = onSaveSelectedToLibrary,
                                    enabled = state.selectedClipIds.isNotEmpty() && !state.isSavingToLibrary,
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                    border = BorderStroke(1.dp, RoyalViolet),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("yt_save_to_library_button")
                                ) {
                                    if (state.isSavingToLibrary) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = RoyalViolet
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Save,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (state.isSavingToLibrary) "Saving…" else "Save Clips",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                items(analysis.suggestions, key = { it.id }) { clip ->
                    SuggestedShortClipCard(
                        clip = clip,
                        isSelected = clip.id in state.selectedClipIds,
                        subtitleLanguage = state.subtitleLanguage,
                        isExporting = state.exportingClipId == clip.id,
                        exportProgressPercent = state.exportProgressPercent,
                        exportedFile = state.exportedMp4Map[clip.id],
                        onToggleSelected = { onToggleClipSelection(clip.id) },
                        onPreview = { previewingClip = clip },
                        onDownloadMp4 = { onDownloadSingleMp4(clip) },
                        onDownloadSrt = { onDownloadSubtitlesSrt(clip) },
                        onCopyMeta = {
                            val payload = "${clip.hookTitle}\n\n${clip.hashtags.joinToString(" ")}"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("YodhaClip Short", payload))
                            onShowMessage("Copied AI title & hashtags for #${clip.rank}")
                        },
                        onShareExported = { exported ->
                            try {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "video/mp4"
                                    putExtra(Intent.EXTRA_STREAM, exported.contentUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share 9:16 MP4"))
                            } catch (_: Exception) {
                                onShowMessage("Unable to share exported MP4 file.")
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(18.dp)) }
        }
    }
}

@Composable
private fun StepTitleHeader(
    step: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        StudioPillBadge(
            text = step,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            useMonoFont = true
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun YouTubeMetadataPreviewBlock(metadata: YouTubeVideoMetadata) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("yt_metadata_preview_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .background(ObsidianBlack)
            ) {
                SubcomposeAsyncImage(
                    model = metadata.thumbnailUrl,
                    contentDescription = metadata.title,
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 2.5.dp,
                                color = RoyalViolet
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Thumbnail preview unavailable",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                StudioPillBadge(
                    text = metadata.apiSource,
                    containerColor = ObsidianBlack.copy(alpha = 0.82f),
                    contentColor = EmeraldReady,
                    useMonoFont = true,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                )
                if (metadata.durationMs != null) {
                    StudioPillBadge(
                        text = Formatters.formatDuration(metadata.durationMs),
                        containerColor = ObsidianBlack.copy(alpha = 0.85f),
                        contentColor = Color.White,
                        useMonoFont = true,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = metadata.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Channel: ${metadata.channelTitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (metadata.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        metadata.tags.take(4).forEach { tag ->
                            StudioPillBadge(
                                text = "#$tag",
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestedShortClipCard(
    clip: ShortClipSuggestion,
    isSelected: Boolean,
    subtitleLanguage: SubtitleLanguage,
    isExporting: Boolean,
    exportProgressPercent: Int,
    exportedFile: ExportedShortResult?,
    onToggleSelected: () -> Unit,
    onPreview: () -> Unit,
    onDownloadMp4: () -> Unit,
    onDownloadSrt: () -> Unit,
    onCopyMeta: () -> Unit,
    onShareExported: (ExportedShortResult) -> Unit
) {
    val subtitles = clip.subtitlesFor(subtitleLanguage)
    StudioSurfaceCard(
        borderColor = if (isSelected) RoyalViolet else MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("yt_clip_card_${clip.rank}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelected() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = RoyalViolet,
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.testTag("yt_clip_select_${clip.rank}")
                    )
                    StudioPillBadge(
                        text = "#${clip.rank} • ${clip.viralScore}%",
                        containerColor = EmeraldReady.copy(alpha = 0.16f),
                        contentColor = EmeraldReady,
                        useMonoFont = true
                    )
                    StudioPillBadge(
                        text = "${Formatters.formatDuration(clip.startMs)}–${Formatters.formatDuration(clip.endMs)}",
                        useMonoFont = true
                    )
                }

                IconButton(
                    onClick = onCopyMeta,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy title and hashtags",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = clip.hookTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = clip.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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

            // Subtitle preview in selected language (English / Hindi / Telugu)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${subtitleLanguage.label} (${subtitleLanguage.nativeLabel}) Subtitles:",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberMagenta
                    )
                    subtitles.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (isExporting) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Exporting 9:16 MP4… $exportProgressPercent%",
                        style = MaterialTheme.typography.labelSmall,
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
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldReady.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Saved: ${exportedFile.fileName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldReady,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onShareExported(exportedFile) }) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null,
                                tint = EmeraldReady,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", color = EmeraldReady)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    border = BorderStroke(1.dp, RoyalViolet),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("9:16", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onDownloadMp4,
                    enabled = !isExporting,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    border = BorderStroke(1.dp, CyberMagenta.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .weight(1.15f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("MP4", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onDownloadSrt,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .weight(0.95f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Subtitles,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SRT", maxLines = 1)
                }
            }
        }
    }
}
