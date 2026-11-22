package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AspectRatioOption
import com.example.data.CaptionStyleOption
import com.example.data.StudioSettings
import com.example.data.VideoMediaInspector
import com.example.ui.Formatters
import com.example.ui.StagedClipDraft
import com.example.ui.UploadAiHighlightSuggestion
import com.example.ui.UploadStudioState
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.StagedClipEditDialog
import com.example.ui.components.StudioPillBadge
import com.example.ui.components.StudioSurfaceCard
import com.example.ui.components.UploadAiHighlightCard
import com.example.ui.components.VideoPreviewPlayer
import com.example.ui.components.VideoThumbnailImage
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.RoyalViolet

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UploadScreen(
    uploadState: UploadStudioState,
    settings: StudioSettings,
    onMp4Picked: (Uri, Boolean) -> Unit,
    onProjectTitleChange: (String) -> Unit,
    onAspectRatioChange: (AspectRatioOption) -> Unit,
    onCaptionStyleChange: (CaptionStyleOption) -> Unit,
    onTrimRangeChange: (Long, Long) -> Unit,
    onManualClipTitleChange: (String) -> Unit,
    onPreviewTrim: () -> Unit,
    onConsumePreviewSeek: () -> Unit,
    onAddManualClip: () -> Unit,
    onGenerateSmartClips: () -> Unit,
    onRemoveStagedClip: (String) -> Unit,
    onClearSelection: () -> Unit,
    onSaveProjectAndClips: () -> Unit,
    onOpenYouTubeShorts: () -> Unit,
    onManualClipSubtitleChange: (String) -> Unit = {},
    onNudgeTrimStart: (Long) -> Unit = {},
    onNudgeTrimEnd: (Long) -> Unit = {},
    onPreviewStagedClip: (String?) -> Unit = {},
    onUpdateStagedClip: (String, String, String, Long, Long) -> Unit = { _, _, _, _, _ -> },
    onExportStagedClipMp4: (StagedClipDraft) -> Unit = {},
    onExportAllStagedClipsMp4: () -> Unit = {},
    onAiTranscriptChange: (String) -> Unit = {},
    onAiTranscriptFilePicked: (Uri) -> Unit = {},
    onAiConsentChange: (Boolean) -> Unit = {},
    onAnalyzeAiHighlights: () -> Unit = {},
    onApproveAiHighlight: (UploadAiHighlightSuggestion) -> Unit = {},
    onDismissAiHighlight: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var editingDraftId by remember { mutableStateOf<String?>(null) }

    // Zero-permission file selector supporting MP4, MOV & WebM
    val videoFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onMp4Picked(uri, true)
        }
    }

    // Zero-permission Android Photo Picker for videos
    val videoGalleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onMp4Picked(uri, false)
        }
    }

    // Optional local transcript file picker (.txt / .srt)
    val transcriptFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onAiTranscriptFilePicked(uri)
        }
    }

    val openVideoFilePicker: () -> Unit = {
        videoFilePicker.launch(VideoMediaInspector.SUPPORTED_VIDEO_MIME_TYPES)
    }
    val openGalleryPicker: () -> Unit = {
        videoGalleryPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
        )
    }
    val openTranscriptPicker: () -> Unit = {
        transcriptFilePicker.launch(arrayOf("text/plain", "application/x-subrip", "text/*", "*/*"))
    }

    val editingDraft = remember(uploadState.stagedClips, editingDraftId) {
        uploadState.stagedClips.firstOrNull { it.localId == editingDraftId }
    }
    val activeVideo = uploadState.inspectedVideo
    if (editingDraft != null && activeVideo != null) {
        StagedClipEditDialog(
            draft = editingDraft,
            maxVideoDurationMs = activeVideo.durationMs,
            onDismiss = { editingDraftId = null },
            onSave = { title, subtitle, startMs, endMs ->
                onUpdateStagedClip(editingDraft.localId, title, subtitle, startMs, endMs)
            }
        )
    }

    val activePreviewDraft = remember(uploadState.stagedClips, uploadState.activePreviewClipId) {
        uploadState.stagedClips.firstOrNull { it.localId == uploadState.activePreviewClipId }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val isDesktopOrTabletWide = maxWidth >= 840.dp && activeVideo != null && !uploadState.isInspecting

        if (isDesktopOrTabletWide && activeVideo != null) {
            // Responsive 2-Pane Desktop / Tablet Video Editing Dashboard
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1280.dp)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Pane: Sticky Video Preview + Active Clip Mode + Specs
                Column(
                    modifier = Modifier
                        .weight(0.46f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    UploadScreenHeader(
                        hasVideo = true,
                        onClearSelection = onClearSelection
                    )
                    ActiveVideoPreviewSection(
                        uploadState = uploadState,
                        activePreviewDraft = activePreviewDraft,
                        loopPlayback = settings.loopClipPreviews,
                        onConsumePreviewSeek = onConsumePreviewSeek,
                        onExitClipPreview = { onPreviewStagedClip(null) },
                        onChangeVideo = openVideoFilePicker
                    )
                }

                // Right Pane: Timeline Trim, Editable Subtitles, AI Highlights, Staged Export Queue
                LazyColumn(
                    modifier = Modifier
                        .weight(0.54f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (uploadState.errorMessage != null) {
                        item { UploadErrorBanner(uploadState.errorMessage) }
                    }
                    item {
                        ProjectDetailsCard(
                            uploadState = uploadState,
                            onProjectTitleChange = onProjectTitleChange,
                            onAspectRatioChange = onAspectRatioChange,
                            onCaptionStyleChange = onCaptionStyleChange
                        )
                    }
                    item {
                        TimelineClipBuilderCard(
                            uploadState = uploadState,
                            settings = settings,
                            onTrimRangeChange = onTrimRangeChange,
                            onNudgeTrimStart = onNudgeTrimStart,
                            onNudgeTrimEnd = onNudgeTrimEnd,
                            onManualClipTitleChange = onManualClipTitleChange,
                            onManualClipSubtitleChange = onManualClipSubtitleChange,
                            onPreviewTrim = onPreviewTrim,
                            onAddManualClip = onAddManualClip,
                            onGenerateSmartClips = onGenerateSmartClips
                        )
                    }
                    item {
                        UploadAiHighlightCard(
                            transcriptInput = uploadState.aiTranscriptInput,
                            consentGranted = uploadState.aiConsentGranted,
                            aiState = uploadState.aiHighlightState,
                            onTranscriptChange = onAiTranscriptChange,
                            onImportTranscriptClick = openTranscriptPicker,
                            onConsentChange = onAiConsentChange,
                            onAnalyzeClick = onAnalyzeAiHighlights,
                            onApproveSuggestion = onApproveAiHighlight,
                            onDismissSuggestion = onDismissAiHighlight
                        )
                    }
                    if (uploadState.stagedClips.isNotEmpty()) {
                        item {
                            StagedQueueHeader(
                                count = uploadState.stagedClips.size,
                                isBatchExporting = uploadState.isBatchExportingDrafts,
                                batchStatus = uploadState.batchExportStatus,
                                onExportAllMp4 = onExportAllStagedClipsMp4
                            )
                        }
                        items(uploadState.stagedClips, key = { it.localId }) { draft ->
                            StagedClipDraftCard(
                                draft = draft,
                                isPreviewing = uploadState.activePreviewClipId == draft.localId,
                                isExporting = uploadState.exportingDraftId == draft.localId,
                                exportProgress = if (uploadState.exportingDraftId == draft.localId) uploadState.exportDraftProgress else 0,
                                exportBusy = uploadState.exportingDraftId != null || uploadState.isBatchExportingDrafts,
                                onPreview = {
                                    onPreviewStagedClip(
                                        if (uploadState.activePreviewClipId == draft.localId) null else draft.localId
                                    )
                                },
                                onEdit = { editingDraftId = draft.localId },
                                onExportMp4 = { onExportStagedClipMp4(draft) },
                                onRemove = { onRemoveStagedClip(draft.localId) }
                            )
                        }
                    }
                    item {
                        SaveProjectPrimaryCta(
                            uploadState = uploadState,
                            onSaveProjectAndClips = onSaveProjectAndClips
                        )
                    }
                }
            }
        } else {
            // Mobile-First Single-Column Responsive Studio
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 880.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    UploadScreenHeader(
                        hasVideo = activeVideo != null,
                        onClearSelection = onClearSelection
                    )
                }

                // Quick switch banner to YouTube Video to Shorts AI workflow
                item {
                    StudioSurfaceCard(
                        onClick = onOpenYouTubeShorts,
                        borderColor = CyberMagenta.copy(alpha = 0.45f),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_open_yt_shorts_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoFixHigh,
                                    contentDescription = null,
                                    tint = CyberMagenta,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.yt_shorts_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Fetch YouTube details, AI 5/10/20 clips & 9:16 MP4 export",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            StudioPillBadge(
                                text = "OPEN",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                useMonoFont = true
                            )
                        }
                    }
                }

                if (uploadState.errorMessage != null) {
                    item { UploadErrorBanner(uploadState.errorMessage) }
                }

                if (uploadState.isInspecting) {
                    item {
                        StudioSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CircularProgressIndicator(color = RoyalViolet)
                                Text(
                                    text = "Inspecting local video (MP4 / MOV / WebM) & extracting preview frame…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else if (activeVideo == null) {
                    // Clean MP4 / MOV / WebM Upload Dropzone
                    item {
                        val dashColor = RoyalViolet.copy(alpha = 0.7f)
                        Surface(
                            onClick = openVideoFilePicker,
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawRoundRect(
                                        color = dashColor,
                                        cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                                        style = Stroke(
                                            width = 2.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(
                                                floatArrayOf(18f, 12f),
                                                0f
                                            )
                                        )
                                    )
                                }
                                .testTag("upload_dropzone_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_upload_studio_1795160682953),
                                    contentDescription = stringResource(R.string.upload_dropzone_title),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 188.dp, height = 140.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                StudioPillBadge(
                                    text = "MP4 • MOV • WEBM • 100% LOCAL",
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    useMonoFont = true
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = stringResource(R.string.upload_dropzone_title),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.upload_dropzone_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))

                                GradientPrimaryButton(
                                    text = stringResource(R.string.upload_btn_select_mp4),
                                    icon = Icons.Rounded.VideoFile,
                                    onClick = openVideoFilePicker,
                                    testTag = "upload_select_mp4_button",
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = openGalleryPicker,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("upload_browse_gallery_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VideoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = stringResource(R.string.upload_btn_browse_gallery))
                                }
                            }
                        }
                    }

                    // Privacy-First Local Video Studio Checklist
                    item {
                        StudioSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Local Video Editing Studio",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                UploadFeatureRow(
                                    icon = Icons.Rounded.PlayArrow,
                                    title = "MP4, MOV & WebM Timeline Preview",
                                    subtitle = "Select start & end timestamps, preview individual clips, and frame in 9:16 Shorts."
                                )
                                UploadFeatureRow(
                                    icon = Icons.Rounded.Subtitles,
                                    title = "Editable Subtitles & Local MP4 Export",
                                    subtitle = "Add clip titles and custom captions, then trim and download MP4 files on-device."
                                )
                                UploadFeatureRow(
                                    icon = Icons.Rounded.Security,
                                    title = "Private & Free — No Paid APIs Required",
                                    subtitle = "Videos stay strictly on your device. Optional AI transcript highlights require your consent."
                                )
                            }
                        }
                    }
                } else {
                    item {
                        ActiveVideoPreviewSection(
                            uploadState = uploadState,
                            activePreviewDraft = activePreviewDraft,
                            loopPlayback = settings.loopClipPreviews,
                            onConsumePreviewSeek = onConsumePreviewSeek,
                            onExitClipPreview = { onPreviewStagedClip(null) },
                            onChangeVideo = openVideoFilePicker
                        )
                    }

                    item {
                        ProjectDetailsCard(
                            uploadState = uploadState,
                            onProjectTitleChange = onProjectTitleChange,
                            onAspectRatioChange = onAspectRatioChange,
                            onCaptionStyleChange = onCaptionStyleChange
                        )
                    }

                    item {
                        TimelineClipBuilderCard(
                            uploadState = uploadState,
                            settings = settings,
                            onTrimRangeChange = onTrimRangeChange,
                            onNudgeTrimStart = onNudgeTrimStart,
                            onNudgeTrimEnd = onNudgeTrimEnd,
                            onManualClipTitleChange = onManualClipTitleChange,
                            onManualClipSubtitleChange = onManualClipSubtitleChange,
                            onPreviewTrim = onPreviewTrim,
                            onAddManualClip = onAddManualClip,
                            onGenerateSmartClips = onGenerateSmartClips
                        )
                    }

                    item {
                        UploadAiHighlightCard(
                            transcriptInput = uploadState.aiTranscriptInput,
                            consentGranted = uploadState.aiConsentGranted,
                            aiState = uploadState.aiHighlightState,
                            onTranscriptChange = onAiTranscriptChange,
                            onImportTranscriptClick = openTranscriptPicker,
                            onConsentChange = onAiConsentChange,
                            onAnalyzeClick = onAnalyzeAiHighlights,
                            onApproveSuggestion = onApproveAiHighlight,
                            onDismissSuggestion = onDismissAiHighlight
                        )
                    }

                    if (uploadState.stagedClips.isNotEmpty()) {
                        item {
                            StagedQueueHeader(
                                count = uploadState.stagedClips.size,
                                isBatchExporting = uploadState.isBatchExportingDrafts,
                                batchStatus = uploadState.batchExportStatus,
                                onExportAllMp4 = onExportAllStagedClipsMp4
                            )
                        }

                        items(uploadState.stagedClips, key = { it.localId }) { draft ->
                            StagedClipDraftCard(
                                draft = draft,
                                isPreviewing = uploadState.activePreviewClipId == draft.localId,
                                isExporting = uploadState.exportingDraftId == draft.localId,
                                exportProgress = if (uploadState.exportingDraftId == draft.localId) uploadState.exportDraftProgress else 0,
                                exportBusy = uploadState.exportingDraftId != null || uploadState.isBatchExportingDrafts,
                                onPreview = {
                                    onPreviewStagedClip(
                                        if (uploadState.activePreviewClipId == draft.localId) null else draft.localId
                                    )
                                },
                                onEdit = { editingDraftId = draft.localId },
                                onExportMp4 = { onExportStagedClipMp4(draft) },
                                onRemove = { onRemoveStagedClip(draft.localId) }
                            )
                        }
                    }

                    item {
                        SaveProjectPrimaryCta(
                            uploadState = uploadState,
                            onSaveProjectAndClips = onSaveProjectAndClips
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun UploadScreenHeader(
    hasVideo: Boolean,
    onClearSelection: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.upload_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.upload_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (hasVideo) {
            TextButton(
                onClick = onClearSelection,
                modifier = Modifier.testTag("upload_clear_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear selected video",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset")
            }
        }
    }
}

@Composable
private fun UploadErrorBanner(message: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveVideoPreviewSection(
    uploadState: UploadStudioState,
    activePreviewDraft: StagedClipDraft?,
    loopPlayback: Boolean,
    onConsumePreviewSeek: () -> Unit,
    onExitClipPreview: () -> Unit,
    onChangeVideo: () -> Unit
) {
    val video = uploadState.inspectedVideo ?: return
    val previewCaption = activePreviewDraft?.subtitleText?.takeIf { it.isNotBlank() }
        ?: uploadState.manualClipSubtitle.takeIf { it.isNotBlank() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (activePreviewDraft != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, CyberMagenta.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_active_clip_preview_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Previewing Clip: ${activePreviewDraft.title}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${Formatters.formatDuration(activePreviewDraft.startMs)} – ${Formatters.formatDuration(activePreviewDraft.endMs)} (${activePreviewDraft.aspectRatio.label})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    TextButton(onClick = onExitClipPreview) {
                        Text("Full Video")
                    }
                }
            }
        }

        VideoPreviewPlayer(
            videoUri = video.uri,
            durationMs = video.durationMs,
            aspectRatio = activePreviewDraft?.aspectRatio ?: uploadState.selectedAspectRatio,
            captionStyle = activePreviewDraft?.captionStyle ?: uploadState.selectedCaptionStyle,
            customCaptionText = previewCaption,
            clipStartMs = activePreviewDraft?.startMs ?: 0L,
            clipEndMs = activePreviewDraft?.endMs ?: video.durationMs,
            loopPlayback = loopPlayback,
            seekTriggerMs = uploadState.previewSeekTriggerMs,
            onSeekTriggerConsumed = onConsumePreviewSeek
        )

        StudioSurfaceCard(modifier = Modifier.fillMaxWidth()) {
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
                            text = video.fileName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        onClick = onChangeVideo,
                        modifier = Modifier.testTag("upload_change_mp4_button")
                    ) {
                        Text(stringResource(R.string.upload_btn_change_video))
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioPillBadge(text = video.formatLabel, useMonoFont = true)
                    StudioPillBadge(
                        text = Formatters.formatDuration(video.durationMs),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        useMonoFont = true
                    )
                    StudioPillBadge(
                        text = Formatters.formatResolution(video.width, video.height),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        useMonoFont = true
                    )
                    if (video.fileSizeBytes > 0L) {
                        StudioPillBadge(
                            text = Formatters.formatFileSize(video.fileSizeBytes),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            useMonoFont = true
                        )
                    }
                    StudioPillBadge(
                        text = "LOCAL ONLY",
                        containerColor = EmeraldReady.copy(alpha = 0.16f),
                        contentColor = EmeraldReady,
                        useMonoFont = true
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectDetailsCard(
    uploadState: UploadStudioState,
    onProjectTitleChange: (String) -> Unit,
    onAspectRatioChange: (AspectRatioOption) -> Unit,
    onCaptionStyleChange: (CaptionStyleOption) -> Unit
) {
    StudioSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = uploadState.projectTitle,
                onValueChange = onProjectTitleChange,
                label = { Text(stringResource(R.string.upload_project_name_label)) },
                placeholder = { Text(stringResource(R.string.upload_project_name_placeholder)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalViolet,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_project_title_input")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.upload_aspect_ratio_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AspectRatioOption.entries.forEach { option ->
                        val selected = uploadState.selectedAspectRatio == option
                        FilterChip(
                            selected = selected,
                            onClick = { onAspectRatioChange(option) },
                            label = {
                                Text(
                                    text = "${option.label} • ${option.subtitle.substringBefore(' ')}",
                                    maxLines = 1
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("upload_ratio_${option.code}")
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.upload_caption_style_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CaptionStyleOption.entries.forEach { option ->
                        val selected = uploadState.selectedCaptionStyle == option
                        FilterChip(
                            selected = selected,
                            onClick = { onCaptionStyleChange(option) },
                            label = { Text(option.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineClipBuilderCard(
    uploadState: UploadStudioState,
    settings: StudioSettings,
    onTrimRangeChange: (Long, Long) -> Unit,
    onNudgeTrimStart: (Long) -> Unit,
    onNudgeTrimEnd: (Long) -> Unit,
    onManualClipTitleChange: (String) -> Unit,
    onManualClipSubtitleChange: (String) -> Unit,
    onPreviewTrim: () -> Unit,
    onAddManualClip: () -> Unit,
    onGenerateSmartClips: () -> Unit
) {
    val video = uploadState.inspectedVideo ?: return

    StudioSurfaceCard(
        borderColor = RoyalViolet.copy(alpha = 0.5f),
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
                        text = stringResource(R.string.upload_clip_builder_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(
                    onClick = onPreviewTrim,
                    modifier = Modifier.testTag("upload_preview_trim_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Preview Trim")
                }
            }

            AnimatedVisibility(visible = settings.showBeginnerTips) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.upload_beginner_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Trim Timecode Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudioPillBadge(
                    text = "IN ${Formatters.formatDuration(uploadState.trimStartMs)}",
                    useMonoFont = true
                )
                StudioPillBadge(
                    text = "LEN ${Formatters.formatDuration((uploadState.trimEndMs - uploadState.trimStartMs).coerceAtLeast(0L))}",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    useMonoFont = true
                )
                StudioPillBadge(
                    text = "OUT ${Formatters.formatDuration(uploadState.trimEndMs)}",
                    useMonoFont = true
                )
            }

            val maxMs = video.durationMs.coerceAtLeast(1000L).toFloat()
            val startVal = uploadState.trimStartMs.toFloat().coerceIn(0f, maxMs)
            val endVal = uploadState.trimEndMs.toFloat().coerceIn(startVal, maxMs)

            RangeSlider(
                value = startVal..endVal,
                onValueChange = { range ->
                    onTrimRangeChange(
                        range.start.toLong(),
                        range.endInclusive.toLong()
                    )
                },
                valueRange = 0f..maxMs,
                colors = SliderDefaults.colors(
                    thumbColor = CyberMagenta,
                    activeTrackColor = RoyalViolet,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Precision 1-second timestamp nudge buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { onNudgeTrimStart(-1_000L) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("upload_in_minus")
                ) {
                    Text("IN -1s", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                OutlinedButton(
                    onClick = { onNudgeTrimStart(1_000L) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("upload_in_plus")
                ) {
                    Text("IN +1s", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                OutlinedButton(
                    onClick = { onNudgeTrimEnd(-1_000L) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("upload_out_minus")
                ) {
                    Text("OUT -1s", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                OutlinedButton(
                    onClick = { onNudgeTrimEnd(1_000L) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("upload_out_plus")
                ) {
                    Text("OUT +1s", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }

            OutlinedTextField(
                value = uploadState.manualClipTitle,
                onValueChange = onManualClipTitleChange,
                label = { Text(stringResource(R.string.upload_clip_title_label)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_clip_title_input")
            )

            OutlinedTextField(
                value = uploadState.manualClipSubtitle,
                onValueChange = onManualClipSubtitleChange,
                label = { Text("Editable Subtitles (Live Overlay Preview)") },
                placeholder = { Text("Type custom subtitles or hook text for this clip…") },
                minLines = 2,
                maxLines = 3,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Subtitles,
                        contentDescription = null,
                        tint = RoyalViolet
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_clip_subtitle_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onAddManualClip,
                    enabled = !uploadState.isGeneratingClips,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    border = BorderStroke(1.dp, RoyalViolet),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("upload_add_manual_clip_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Clip",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onGenerateSmartClips,
                    enabled = !uploadState.isGeneratingClips,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    border = BorderStroke(1.dp, CyberMagenta.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    modifier = Modifier
                        .weight(1.25f)
                        .height(48.dp)
                        .testTag("upload_smart_cut_button")
                ) {
                    if (uploadState.isGeneratingClips) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = CyberMagenta
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AutoFixHigh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Auto-Split (${settings.smartClipSeconds}s)",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StagedQueueHeader(
    count: Int,
    isBatchExporting: Boolean,
    batchStatus: String?,
    onExportAllMp4: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${stringResource(R.string.upload_staged_clips_title)} ($count)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            OutlinedButton(
                onClick = onExportAllMp4,
                enabled = !isBatchExporting,
                border = BorderStroke(1.dp, EmeraldReady.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldReady),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(40.dp)
                    .testTag("upload_export_all_mp4_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBatchExporting) "Exporting…" else "Export All MP4",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
        if (batchStatus != null) {
            Text(
                text = batchStatus,
                style = MaterialTheme.typography.labelSmall,
                color = AmberHighlight
            )
        }
    }
}

@Composable
private fun SaveProjectPrimaryCta(
    uploadState: UploadStudioState,
    onSaveProjectAndClips: () -> Unit
) {
    val clipCountLabel = if (uploadState.stagedClips.isEmpty()) {
        "Save Project + 1 Trimmed Clip"
    } else {
        "Save Project & ${uploadState.stagedClips.size} Clip(s)"
    }
    GradientPrimaryButton(
        text = if (uploadState.isSaving) "Saving to Studio…" else clipCountLabel,
        icon = Icons.Rounded.Save,
        enabled = !uploadState.isSaving && !uploadState.isGeneratingClips && !uploadState.isBatchExportingDrafts,
        onClick = onSaveProjectAndClips,
        testTag = "upload_save_project_button",
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun UploadFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StagedClipDraftCard(
    draft: StagedClipDraft,
    isPreviewing: Boolean,
    isExporting: Boolean,
    exportProgress: Int,
    exportBusy: Boolean,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onExportMp4: () -> Unit,
    onRemove: () -> Unit
) {
    StudioSurfaceCard(
        borderColor = if (isPreviewing) CyberMagenta else MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("staged_clip_card_${draft.localId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VideoThumbnailImage(
                    thumbnailPath = draft.thumbnailPath,
                    contentDescription = draft.title,
                    modifier = Modifier
                        .size(width = 78.dp, height = 58.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = draft.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StudioPillBadge(
                            text = "${Formatters.formatDuration(draft.startMs)} - ${Formatters.formatDuration(draft.endMs)}",
                            useMonoFont = true
                        )
                        StudioPillBadge(
                            text = draft.aspectRatio.label,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            useMonoFont = true
                        )
                        if (draft.isAiSuggested) {
                            StudioPillBadge(
                                text = "AI",
                                containerColor = CyberMagenta.copy(alpha = 0.2f),
                                contentColor = CyberMagenta,
                                useMonoFont = true
                            )
                        }
                    }
                    if (draft.subtitleText.isNotBlank()) {
                        Text(
                            text = "Subtitles: \"${draft.subtitleText}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (isExporting) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { (exportProgress.coerceIn(0, 100) / 100f) },
                        color = EmeraldReady,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "Trimming & exporting MP4 locally… $exportProgress%",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldReady
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    border = BorderStroke(
                        1.dp,
                        if (isPreviewing) CyberMagenta else MaterialTheme.colorScheme.outline
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("staged_preview_${draft.localId}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPreviewing) "Playing" else "Preview",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onEdit,
                    border = BorderStroke(1.dp, RoyalViolet.copy(alpha = 0.65f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("staged_edit_${draft.localId}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onExportMp4,
                    enabled = !exportBusy,
                    border = BorderStroke(1.dp, EmeraldReady.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldReady),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(40.dp)
                        .testTag("staged_download_${draft.localId}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "MP4",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("staged_delete_${draft.localId}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Remove staged clip",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
