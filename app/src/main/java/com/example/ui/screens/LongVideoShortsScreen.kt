package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.VerifiedUser
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ai.LongVideoClipProposal
import com.example.ai.SubtitleLanguage
import com.example.data.AspectRatioOption
import com.example.data.CaptionStyleOption
import com.example.data.SmartCropMode
import com.example.ui.Formatters
import com.example.ui.InlineYouTubeState
import com.example.ui.LongMomentsState
import com.example.ui.LongTranscriptState
import com.example.ui.LongVideoShortsUiState
import com.example.ui.VideoInputMode
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.LongClipEditDialog
import com.example.ui.components.StudioPillBadge
import com.example.ui.components.StudioSurfaceCard
import com.example.ui.components.VideoPreviewPlayer
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RoyalViolet
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LongVideoShortsScreen(
    state: LongVideoShortsUiState,
    onBack: () -> Unit,
    onVideoPicked: (Uri, Boolean) -> Unit,
    onClearVideo: () -> Unit,
    onTranscriptChange: (String) -> Unit,
    onTranscriptFilePicked: (Uri) -> Unit,
    onGenerateTranscript: () -> Unit,
    onMomentCountChange: (Int) -> Unit,
    onSubtitleLanguageChange: (SubtitleLanguage) -> Unit,
    onConvertTo9x16Change: (Boolean) -> Unit,
    onSmartCropChange: (SmartCropMode) -> Unit,
    onDetectMoments: () -> Unit,
    onToggleClipSelection: (String) -> Unit,
    onSelectAllClips: () -> Unit,
    onClearSelection: () -> Unit,
    onUpdateClipProposal: (
        clipId: String,
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
    onRegenerateClipMetadata: (LongVideoClipProposal) -> Unit,
    onExportSingleMp4: (LongVideoClipProposal) -> Unit,
    onExportSelectedBatchMp4: () -> Unit,
    onExportSubtitlesSrt: (LongVideoClipProposal) -> Unit,
    onSaveSelectedToLibrary: () -> Unit,
    onOpenDownloads: () -> Unit,
    onInputModeChange: (VideoInputMode) -> Unit = {},
    onYouTubeUrlChange: (String) -> Unit = {},
    onYouTubeOwnershipConfirmed: (Boolean) -> Unit = {},
    onAnalyzeYouTubeLink: () -> Unit = {},
    onTargetDurationChange: (Int, Boolean) -> Unit = { _, _ -> },
    onRemovePausesChange: (Boolean) -> Unit = {},
    onBackgroundMusicPicked: (Uri) -> Unit = {},
    onClearBackgroundMusic: () -> Unit = {},
    onShowMessage: (String) -> Unit = {}
) {
    var editingProposalId by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onVideoPicked(uri, false)
    }

    val mp4DocumentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onVideoPicked(uri, true)
    }

    val transcriptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onTranscriptFilePicked(uri)
    }

    val audioMusicPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onBackgroundMusicPicked(uri)
    }

    val analysisResult = (state.momentsState as? LongMomentsState.Success)?.result
    val proposals = analysisResult?.proposals.orEmpty()
    val editingProposal = proposals.firstOrNull { it.id == editingProposalId }

    if (editingProposal != null && state.selectedVideo != null) {
        LongClipEditDialog(
            proposal = editingProposal,
            videoUri = state.selectedVideo.uri,
            totalVideoDurationMs = state.selectedVideo.durationMs,
            subtitleLanguage = state.subtitleLanguage,
            convertTo9x16 = state.convertTo9x16,
            isRegenerating = state.regeneratingClipId == editingProposal.id,
            onDismiss = { editingProposalId = null },
            onSaveChanges = { title, s, e, crop, tags, hook, seoDesc, thumbText, captionLines ->
                onUpdateClipProposal(
                    editingProposal.id,
                    title,
                    s,
                    e,
                    crop,
                    tags,
                    hook,
                    seoDesc,
                    thumbText,
                    captionLines
                )
            },
            onRegenerateTitleHashtags = { onRegenerateClipMetadata(editingProposal) },
            onExportMp4 = {
                editingProposalId = null
                onExportSingleMp4(editingProposal)
            }
        )
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("long_shorts_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Create Viral Shorts",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Upload MP4 • Gallery • Authorized YouTube Link • 9:16 AI Studio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenDownloads,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("long_open_downloads_button")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Exports", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }

            // Open-source engine badge
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RoyalViolet.copy(alpha = 0.16f))
                        .border(1.dp, RoyalViolet.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MovieCreation,
                        contentDescription = null,
                        tint = ElectricViolet,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Engine: ${state.engineLabel} • Firebase AI Logic (Zero Exposed Keys)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // STEP 1: Three Video Input Options (Upload MP4, Gallery, YouTube Link)
            item {
                StudioSurfaceCard(
                    borderColor = RoyalViolet.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("long_step1_upload_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1. Video Input — Choose Source",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (state.selectedVideo != null) {
                                IconButton(onClick = onClearVideo) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Remove video")
                                }
                            }
                        }

                        // Three Input Option Chips: Option A, Option B, Option C
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VideoInputMode.entries.forEach { mode ->
                                val icon = when (mode) {
                                    VideoInputMode.FILE_MANAGER -> Icons.Rounded.CloudUpload
                                    VideoInputMode.GALLERY -> Icons.Rounded.PhotoLibrary
                                    VideoInputMode.YOUTUBE_LINK -> Icons.Rounded.Link
                                }
                                FilterChip(
                                    selected = state.inputMode == mode,
                                    onClick = { onInputModeChange(mode) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = mode.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RoyalViolet.copy(alpha = 0.4f),
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = CyberMagenta
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("long_input_mode_${mode.name.lowercase()}")
                                )
                            }
                        }

                        // Option C: YouTube Link Analyzer (Official oEmbed + Authorization Check)
                        if (state.inputMode == VideoInputMode.YOUTUBE_LINK) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                                    .padding(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = state.youtubeUrlInput,
                                    onValueChange = onYouTubeUrlChange,
                                    label = { Text("YouTube Video URL") },
                                    placeholder = { Text("https://www.youtube.com/watch?v=...") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("long_youtube_url_input")
                                )

                                GradientPrimaryButton(
                                    text = "Analyze YouTube Video",
                                    icon = Icons.Rounded.Link,
                                    enabled = state.youtubeState !is InlineYouTubeState.Loading,
                                    testTag = "long_analyze_youtube_button",
                                    onClick = onAnalyzeYouTubeLink,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                when (val yt = state.youtubeState) {
                                    is InlineYouTubeState.Loading -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = CyberMagenta
                                            )
                                            Text(
                                                text = "Validating link & fetching official YouTube details…",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ElectricViolet
                                            )
                                        }
                                    }
                                    is InlineYouTubeState.Loaded -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            AsyncImage(
                                                model = yt.metadata.thumbnailUrl,
                                                contentDescription = yt.metadata.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(width = 96.dp, height = 56.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = yt.metadata.title,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${yt.metadata.channelTitle} • ${yt.metadata.apiSource}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = NeonEmerald,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    is InlineYouTubeState.Error -> {
                                        Text(
                                            text = yt.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    InlineYouTubeState.Idle -> Unit
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = state.youtubeOwnershipConfirmed,
                                        onCheckedChange = onYouTubeOwnershipConfirmed,
                                        colors = CheckboxDefaults.colors(checkedColor = RoyalViolet),
                                        modifier = Modifier.testTag("long_youtube_ownership_checkbox")
                                    )
                                    Text(
                                        text = "I own this video or have explicit permission to edit it. (YodhaClip AI never bypasses YouTube restrictions — attach your original MP4 below to render clips.)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (state.isUploading) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = CyberMagenta
                                    )
                                    Text(
                                        text = "${state.uploadStageLabel} (${state.uploadProgressPercent}%)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { state.uploadProgressPercent / 100f },
                                    color = CyberMagenta,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }

                        if (state.selectedVideo == null) {
                            val hintText = when (state.inputMode) {
                                VideoInputMode.FILE_MANAGER ->
                                    "Option A: Select any large MP4 video from your device File Manager with streaming disk reads."
                                VideoInputMode.GALLERY ->
                                    "Option B: Open your Android mobile gallery to pick your original recorded video."
                                VideoInputMode.YOUTUBE_LINK ->
                                    "Attach the original MP4 file for your authorized YouTube video to run multimodal AI & export 9:16 Shorts."
                            }
                            Text(
                                text = hintText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val canAttachFile = state.inputMode != VideoInputMode.YOUTUBE_LINK || state.youtubeOwnershipConfirmed
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GradientPrimaryButton(
                                    text = if (state.inputMode == VideoInputMode.GALLERY) "Open Gallery" else "Upload MP4 (Files)",
                                    icon = if (state.inputMode == VideoInputMode.GALLERY) Icons.Rounded.PhotoLibrary else Icons.Rounded.CloudUpload,
                                    enabled = canAttachFile,
                                    testTag = "long_pick_gallery_button",
                                    onClick = {
                                        if (state.inputMode == VideoInputMode.GALLERY) {
                                            galleryPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                            )
                                        } else {
                                            mp4DocumentPicker.launch(arrayOf("video/mp4", "video/quicktime", "video/webm", "video/*"))
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = {
                                        if (state.inputMode == VideoInputMode.GALLERY) {
                                            mp4DocumentPicker.launch(arrayOf("video/mp4", "video/quicktime", "video/webm", "video/*"))
                                        } else {
                                            galleryPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                            )
                                        }
                                    },
                                    enabled = canAttachFile,
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("long_pick_files_button")
                                ) {
                                    Icon(
                                        imageVector = if (state.inputMode == VideoInputMode.GALLERY) Icons.Rounded.CloudUpload else Icons.Rounded.PhotoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (state.inputMode == VideoInputMode.GALLERY) "File Manager" else "Gallery",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            VideoPreviewPlayer(
                                videoUri = state.selectedVideo.uri,
                                durationMs = state.selectedVideo.durationMs,
                                aspectRatio = if (state.convertTo9x16) AspectRatioOption.PORTRAIT_9_16 else AspectRatioOption.LANDSCAPE_16_9,
                                captionStyle = CaptionStyleOption.NONE,
                                loopPlayback = false
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = state.selectedVideo.fileName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${Formatters.formatDuration(state.selectedVideo.durationMs)} • ${Formatters.formatFileSize(state.selectedVideo.fileSizeBytes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonEmerald
                                )
                            }
                        }
                    }
                }
            }

            // STEP 2: AI Speech Transcription & Scene/Speaker Extraction
            item {
                StudioSurfaceCard(
                    borderColor = ElectricViolet.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("long_step2_transcript_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "2. AI Video & Speech Analysis",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = { transcriptPicker.launch(arrayOf("text/*", "application/x-subrip")) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("long_import_transcript_button")
                            ) {
                                Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(".SRT/.TXT", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }

                        GradientPrimaryButton(
                            text = "Generate AI Speech Transcript",
                            icon = Icons.Rounded.GraphicEq,
                            enabled = state.selectedVideo != null && state.transcriptState !is LongTranscriptState.Processing,
                            testTag = "long_generate_transcript_button",
                            onClick = onGenerateTranscript,
                            modifier = Modifier.fillMaxWidth()
                        )

                        when (val tState = state.transcriptState) {
                            is LongTranscriptState.Processing -> {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "${tState.stage} (${tState.percent}%)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ElectricViolet
                                    )
                                    LinearProgressIndicator(
                                        progress = { tState.percent / 100f },
                                        color = CyberMagenta,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                            is LongTranscriptState.Ready -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = NeonEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${tState.detectedLanguage} • ${tState.summary}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeonEmerald
                                    )
                                }
                            }
                            is LongTranscriptState.Error -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
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
                                        text = tState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            LongTranscriptState.Idle -> Unit
                        }

                        OutlinedTextField(
                            value = state.transcriptText,
                            onValueChange = onTranscriptChange,
                            label = { Text("Timestamped Transcript (Editable)") },
                            placeholder = { Text("Tap Generate AI Speech Transcript above, or paste/import your transcript…") },
                            minLines = 3,
                            maxLines = 6,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("long_transcript_input")
                        )
                    }
                }
            }

            // STEP 3: Automatic Clip Generator & Automatic Shorts Editor Settings
            item {
                StudioSurfaceCard(
                    borderColor = CyberMagenta.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("long_step3_detect_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "3. AI Automatic Clip & Shorts Editor",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Clip Count (10 / 20 / 30)
                        Text(
                            text = "Number of Shorts Moments",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(10, 20, 30).forEach { count ->
                                FilterChip(
                                    selected = state.requestedMomentCount == count,
                                    onClick = { onMomentCountChange(count) },
                                    label = { Text("$count Moments", maxLines = 1) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RoyalViolet.copy(alpha = 0.35f),
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("long_count_$count")
                                )
                            }
                        }

                        // Target Clip Duration (15s / 30s / 45s / 60s / Custom)
                        Text(
                            text = "Target Clip Duration (${state.targetClipDurationSec}s)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(15, 30, 45, 60).forEach { sec ->
                                FilterChip(
                                    selected = !state.isCustomDuration && state.targetClipDurationSec == sec,
                                    onClick = { onTargetDurationChange(sec, false) },
                                    label = { Text("${sec}s") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RoyalViolet.copy(alpha = 0.35f),
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("long_duration_${sec}s")
                                )
                            }
                            FilterChip(
                                selected = state.isCustomDuration,
                                onClick = { onTargetDurationChange(state.targetClipDurationSec, true) },
                                label = { Text("Custom") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberMagenta.copy(alpha = 0.3f),
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("long_duration_custom")
                            )
                        }

                        if (state.isCustomDuration) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Custom Duration: ${state.targetClipDurationSec} seconds (10s–90s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberMagenta
                                )
                                Slider(
                                    value = state.targetClipDurationSec.toFloat(),
                                    onValueChange = { onTargetDurationChange(it.roundToInt(), true) },
                                    valueRange = 10f..90f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = CyberMagenta,
                                        activeTrackColor = RoyalViolet
                                    ),
                                    modifier = Modifier.testTag("long_custom_duration_slider")
                                )
                            }
                        }

                        // Natural Conversation Flow & Pause Trimming toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Remove Pauses & Preserve Sentences",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Trim dead-air pauses and avoid cutting sentences mid-way",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.removePausesAndKeepSentences,
                                onCheckedChange = onRemovePausesChange,
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RoyalViolet),
                                modifier = Modifier.testTag("long_remove_pauses_switch")
                            )
                        }

                        // Language Selection for Captions + Viral Content Kit
                        Text(
                            text = "Viral Kit & Animated Captions Language",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SubtitleLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = state.subtitleLanguage == lang,
                                    onClick = { onSubtitleLanguageChange(lang) },
                                    label = { Text("${lang.label} (${lang.nativeLabel})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberMagenta.copy(alpha = 0.28f),
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("long_lang_${lang.code}")
                                )
                            }
                        }

                        // Automatic 9:16 Vertical Conversion & Subject Tracking
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "9:16 Vertical + Smart Subject Tracking",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Smart center crop or speaker-focused 9:16 encoding",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.convertTo9x16,
                                onCheckedChange = onConvertTo9x16Change,
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RoyalViolet),
                                modifier = Modifier.testTag("long_9x16_switch")
                            )
                        }

                        if (state.convertTo9x16) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SmartCropMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = state.defaultSmartCrop == mode,
                                        onClick = { onSmartCropChange(mode) },
                                        label = { Text(mode.label, maxLines = 1) }
                                    )
                                }
                            }
                        }

                        // Optional Authorized Background Music Track
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = AmberHighlight,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.backgroundMusicName ?: "Optional Background Music",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (state.backgroundMusicUri != null) {
                                        "Authorized audio will mix cleanly into exported MP4"
                                    } else {
                                        "Attach a royalty-free / authorized audio file (MP3/M4A)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (state.backgroundMusicUri != null) {
                                IconButton(
                                    onClick = onClearBackgroundMusic,
                                    modifier = Modifier.testTag("long_clear_music_button")
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Remove music")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { audioMusicPicker.launch(arrayOf("audio/*")) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(38.dp)
                                        .testTag("long_pick_music_button")
                                ) {
                                    Text("Add Audio", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            }
                        }

                        GradientPrimaryButton(
                            text = "Generate ${state.requestedMomentCount} Viral Shorts (~${state.targetClipDurationSec}s)",
                            icon = Icons.Rounded.AutoAwesome,
                            enabled = state.selectedVideo != null && state.momentsState !is LongMomentsState.Processing,
                            testTag = "long_detect_moments_button",
                            onClick = onDetectMoments,
                            modifier = Modifier.fillMaxWidth()
                        )

                        when (val mState = state.momentsState) {
                            is LongMomentsState.Processing -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = CyberMagenta
                                        )
                                        Text(
                                            text = mState.stage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ElectricViolet
                                        )
                                    }
                                    LinearProgressIndicator(
                                        color = CyberMagenta,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                            is LongMomentsState.Error -> {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = mState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    OutlinedButton(
                                        onClick = onDetectMoments,
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        Text("Retry AI Detection")
                                    }
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }

            // STEP 4: Scene & Speaker Analysis Summary + Viral Shorts Cards
            if (proposals.isNotEmpty() && analysisResult != null) {
                item {
                    StudioSurfaceCard(
                        borderColor = NeonEmerald.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "4. ${proposals.size} Generated Shorts • ${analysisResult.detectedTopic}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (analysisResult.overallSummary.isNotBlank()) {
                                Text(
                                    text = analysisResult.overallSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (analysisResult.speakerChanges.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.RecordVoiceOver,
                                            contentDescription = null,
                                            tint = ElectricViolet,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Detected Speaker Changes",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ElectricViolet
                                        )
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        analysisResult.speakerChanges.take(5).forEach { sp ->
                                            StudioPillBadge(
                                                text = sp,
                                                containerColor = RoyalViolet.copy(alpha = 0.22f),
                                                contentColor = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            if (analysisResult.importantScenes.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Important Scenes Detected",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AmberHighlight
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        analysisResult.importantScenes.take(5).forEach { scene ->
                                            StudioPillBadge(
                                                text = scene,
                                                containerColor = AmberHighlight.copy(alpha = 0.16f),
                                                contentColor = AmberHighlight
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onSelectAllClips,
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) { Text("Select All", maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                                OutlinedButton(
                                    onClick = onClearSelection,
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) { Text("Clear (${state.selectedClipIds.size})", maxLines = 1, style = MaterialTheme.typography.labelSmall) }
                            }

                            if (state.isBatchExporting && state.batchExportStatus != null) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "${state.batchExportStatus} (${state.exportProgressPercent}%)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CyberMagenta
                                    )
                                    LinearProgressIndicator(
                                        progress = { state.exportProgressPercent / 100f },
                                        color = CyberMagenta,
                                        modifier = Modifier.fillMaxWidth().height(6.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GradientPrimaryButton(
                                    text = "Export ${state.selectedClipIds.size} MP4s",
                                    icon = Icons.Rounded.Download,
                                    enabled = state.selectedClipIds.isNotEmpty() && !state.isBatchExporting,
                                    testTag = "long_batch_export_button",
                                    onClick = onExportSelectedBatchMp4,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = onSaveSelectedToLibrary,
                                    enabled = !state.isSavingToLibrary,
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("long_save_library_button")
                                ) {
                                    if (state.isSavingToLibrary) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Text("Save Clips", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }

                items(proposals, key = { it.id }) { clip ->
                    val isSelected = clip.id in state.selectedClipIds
                    val isExportingThis = state.exportingClipId == clip.id
                    val exportedResult = state.exportedMp4Map[clip.id]

                    LongMomentProposalCard(
                        proposal = clip,
                        subtitleLanguage = state.subtitleLanguage,
                        isSelected = isSelected,
                        isExporting = isExportingThis,
                        exportProgress = if (isExportingThis) state.exportProgressPercent else 0,
                        exportedLabel = exportedResult?.locationLabel,
                        onToggleSelect = { onToggleClipSelection(clip.id) },
                        onOpenTimelineEditor = { editingProposalId = clip.id },
                        onCopyViralKit = {
                            val viralKit = buildString {
                                appendLine("Title: ${clip.hookTitle}")
                                if (clip.openingHook.isNotBlank()) appendLine("Hook: ${clip.openingHook}")
                                if (clip.thumbnailText.isNotBlank()) appendLine("Thumbnail Text: ${clip.thumbnailText}")
                                if (clip.seoDescription.isNotBlank()) appendLine("Description: ${clip.seoDescription}")
                                appendLine("Hashtags: ${clip.hashtags.joinToString(" ")}")
                            }
                            clipboard.setText(AnnotatedString(viralKit))
                            onShowMessage("Copied Viral Kit for Clip #${clip.rank} to clipboard")
                        },
                        onExportMp4 = { onExportSingleMp4(clip) },
                        onExportSrt = { onExportSubtitlesSrt(clip) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LongMomentProposalCard(
    proposal: LongVideoClipProposal,
    subtitleLanguage: SubtitleLanguage,
    isSelected: Boolean,
    isExporting: Boolean,
    exportProgress: Int,
    exportedLabel: String?,
    onToggleSelect: () -> Unit,
    onOpenTimelineEditor: () -> Unit,
    onCopyViralKit: () -> Unit,
    onExportMp4: () -> Unit,
    onExportSrt: () -> Unit
) {
    val scoreColor = when {
        proposal.viralScore >= 88 -> NeonEmerald
        proposal.viralScore >= 75 -> CyberMagenta
        else -> ElectricViolet
    }

    StudioSurfaceCard(
        borderColor = if (isSelected) CyberMagenta.copy(alpha = 0.75f) else RoyalViolet.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("long_clip_card_${proposal.rank}")
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = RoyalViolet)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#${proposal.rank} • ${proposal.hookTitle}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${Formatters.formatDuration(proposal.startMs)} → ${Formatters.formatDuration(proposal.endMs)} (${proposal.durationMs / 1000L}s) • ${proposal.recommendedCropMode.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(scoreColor.copy(alpha = 0.18f))
                        .border(1.dp, scoreColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${proposal.viralScore}% Viral",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = scoreColor
                    )
                }
            }

            // Full AI Viral Content Kit panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RoyalViolet.copy(alpha = 0.12f))
                    .border(1.dp, RoyalViolet.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Viral Content Kit (${subtitleLanguage.label})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AmberHighlight
                    )
                    IconButton(
                        onClick = onCopyViralKit,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("long_copy_viral_kit_${proposal.rank}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy viral kit",
                            tint = ElectricViolet,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (proposal.openingHook.isNotBlank()) {
                    Text(
                        text = "Hook: \"${proposal.openingHook}\"",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (proposal.thumbnailText.isNotBlank()) {
                    Text(
                        text = "Thumbnail Text: ${proposal.thumbnailText}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CyberMagenta
                    )
                }
                if (proposal.seoDescription.isNotBlank()) {
                    Text(
                        text = "SEO Description: ${proposal.seoDescription}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (proposal.pauseTrimNote.isNotBlank()) {
                    Text(
                        text = "Flow: ${proposal.pauseTrimNote}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonEmerald
                    )
                }
                Text(
                    text = proposal.hashtags.joinToString(" "),
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricViolet,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (proposal.highlightWords.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Highlighted Words:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    proposal.highlightWords.take(5).forEach { kw ->
                        StudioPillBadge(
                            text = kw,
                            containerColor = AmberHighlight.copy(alpha = 0.18f),
                            contentColor = AmberHighlight
                        )
                    }
                }
            }

            // Subtitle preview in selected language
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${subtitleLanguage.label} (${subtitleLanguage.nativeLabel}) Animated Captions:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = CyberMagenta
                )
                proposal.subtitlesFor(subtitleLanguage).forEach { line ->
                    Text(
                        text = "• $line",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (isExporting) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Rendering 9:16 MP4… $exportProgress%",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberMagenta
                    )
                    LinearProgressIndicator(
                        progress = { exportProgress / 100f },
                        color = CyberMagenta,
                        modifier = Modifier.fillMaxWidth().height(5.dp)
                    )
                }
            } else if (exportedLabel != null) {
                Text(
                    text = "Saved: $exportedLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonEmerald
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenTimelineEditor,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("long_edit_clip_${proposal.rank}")
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit & Captions", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onExportMp4,
                    enabled = !isExporting,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("MP4", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onExportSrt,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Icon(Icons.Rounded.Subtitles, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("SRT", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
