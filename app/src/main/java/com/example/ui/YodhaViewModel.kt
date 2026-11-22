package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.LongVideoAiService
import com.example.ai.SubtitleLanguage
import com.example.data.AspectRatioOption
import com.example.data.CaptionStyleOption
import com.example.data.ExportQualityOption
import com.example.data.ExportedDownloadEntity
import com.example.data.InspectedVideoInfo
import com.example.data.SettingsDataStore
import com.example.data.ShortsMp4Exporter
import com.example.data.SmartCropMode
import com.example.data.StudioSettings
import com.example.data.StudioStorageBreakdown
import com.example.data.StudioStorageManager
import com.example.data.VideoClipEntity
import com.example.data.VideoMediaInspector
import com.example.data.VideoProjectEntity
import com.example.data.YodhaDatabase
import com.example.data.YodhaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class StagedClipDraft(
    val localId: String = UUID.randomUUID().toString(),
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val aspectRatio: AspectRatioOption,
    val captionStyle: CaptionStyleOption,
    val thumbnailPath: String?,
    val subtitleText: String = "",
    val isAiSuggested: Boolean = false,
    val aiReason: String? = null
) {
    val durationMs: Long
        get() = (endMs - startMs).coerceAtLeast(0L)
}

data class UploadAiHighlightSuggestion(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val viralScore: Int,
    val reason: String,
    val suggestedSubtitle: String,
    val approved: Boolean = false
) {
    val durationMs: Long
        get() = (endMs - startMs).coerceAtLeast(0L)
}

sealed interface UploadAiState {
    data object Idle : UploadAiState
    data object Loading : UploadAiState
    data class Success(val suggestions: List<UploadAiHighlightSuggestion>) : UploadAiState
    data class Error(val message: String) : UploadAiState
}

data class UploadStudioState(
    val inspectedVideo: InspectedVideoInfo? = null,
    val isInspecting: Boolean = false,
    val isGeneratingClips: Boolean = false,
    val isSaving: Boolean = false,
    val projectTitle: String = "",
    val selectedAspectRatio: AspectRatioOption = AspectRatioOption.PORTRAIT_9_16,
    val selectedCaptionStyle: CaptionStyleOption = CaptionStyleOption.BOLD_VIOLET,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 15_000L,
    val manualClipTitle: String = "Highlight #1",
    val manualClipSubtitle: String = "",
    val previewSeekTriggerMs: Long? = null,
    val activePreviewClipId: String? = null,
    val stagedClips: List<StagedClipDraft> = emptyList(),
    val exportingDraftId: String? = null,
    val exportDraftProgress: Int = 0,
    val isBatchExportingDrafts: Boolean = false,
    val batchExportStatus: String? = null,
    val aiTranscriptInput: String = "",
    val aiConsentGranted: Boolean = false,
    val aiHighlightState: UploadAiState = UploadAiState.Idle,
    val errorMessage: String? = null
)

class YodhaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: YodhaRepository = YodhaRepository(
        dao = YodhaDatabase.getInstance(application).yodhaDao(),
        settingsStore = SettingsDataStore(application),
        mediaInspector = VideoMediaInspector(application),
        storageManager = StudioStorageManager(application)
    )
    private val shortsExporter = ShortsMp4Exporter(application)
    private val uploadAiService = LongVideoAiService()

    val projects: StateFlow<List<VideoProjectEntity>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val clips: StateFlow<List<VideoClipEntity>> = repository.allClips
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val downloads: StateFlow<List<ExportedDownloadEntity>> = repository.allDownloads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val settings: StateFlow<StudioSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StudioSettings()
        )

    private val _uploadState = MutableStateFlow(UploadStudioState())
    val uploadState: StateFlow<UploadStudioState> = _uploadState.asStateFlow()

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    private val _storageBreakdown = MutableStateFlow(StudioStorageBreakdown())
    val storageBreakdown: StateFlow<StudioStorageBreakdown> = _storageBreakdown.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _exportingClipId = MutableStateFlow<Long?>(null)
    val exportingClipId: StateFlow<Long?> = _exportingClipId.asStateFlow()

    private val _exportClipProgress = MutableStateFlow(0)
    val exportClipProgress: StateFlow<Int> = _exportClipProgress.asStateFlow()

    init {
        refreshCacheSize()
    }

    fun observeProject(projectId: Long): Flow<VideoProjectEntity?> =
        repository.observeProject(projectId)

    fun observeClipsForProject(projectId: Long): Flow<List<VideoClipEntity>> =
        repository.observeClipsForProject(projectId)

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun onVideoPicked(uri: Uri, takePersistablePermission: Boolean) {
        viewModelScope.launch {
            val currentSettings = settings.value
            _uploadState.update {
                it.copy(
                    isInspecting = true,
                    errorMessage = null
                )
            }
            try {
                val info = repository.inspectVideo(uri, takePersistablePermission)
                val initialEnd = minOf(
                    info.durationMs,
                    (currentSettings.smartClipSeconds * 1000L).coerceAtLeast(3000L)
                ).coerceAtLeast(1000L)

                _uploadState.value = UploadStudioState(
                    inspectedVideo = info,
                    isInspecting = false,
                    projectTitle = info.suggestedTitle,
                    selectedAspectRatio = currentSettings.defaultAspectRatio,
                    selectedCaptionStyle = currentSettings.defaultCaptionStyle,
                    trimStartMs = 0L,
                    trimEndMs = initialEnd,
                    manualClipTitle = "Highlight #1",
                    stagedClips = emptyList(),
                    errorMessage = null
                )
                refreshCacheSize()
                _snackbarMessage.value = "Loaded ${info.fileName} (${Formatters.formatDuration(info.durationMs)})"
            } catch (e: Exception) {
                _uploadState.update {
                    it.copy(
                        isInspecting = false,
                        errorMessage = "Could not open the selected video file (MP4/MOV/WebM). Please pick another local video."
                    )
                }
            }
        }
    }

    fun updateUploadProjectTitle(title: String) {
        _uploadState.update { it.copy(projectTitle = title) }
    }

    fun updateUploadAspectRatio(option: AspectRatioOption) {
        _uploadState.update { it.copy(selectedAspectRatio = option) }
    }

    fun updateUploadCaptionStyle(option: CaptionStyleOption) {
        _uploadState.update { it.copy(selectedCaptionStyle = option) }
    }

    fun updateTrimRange(startMs: Long, endMs: Long) {
        val duration = _uploadState.value.inspectedVideo?.durationMs ?: return
        val safeStart = startMs.coerceIn(0L, (duration - 500L).coerceAtLeast(0L))
        val safeEnd = endMs.coerceIn((safeStart + 500L).coerceAtMost(duration), duration)
        _uploadState.update {
            it.copy(trimStartMs = safeStart, trimEndMs = safeEnd)
        }
    }

    fun updateManualClipTitle(title: String) {
        _uploadState.update { it.copy(manualClipTitle = title) }
    }

    fun updateManualClipSubtitle(subtitle: String) {
        _uploadState.update { it.copy(manualClipSubtitle = subtitle) }
    }

    fun nudgeTrimStart(deltaMs: Long) {
        val state = _uploadState.value
        val duration = state.inspectedVideo?.durationMs ?: return
        val newStart = (state.trimStartMs + deltaMs).coerceIn(0L, (state.trimEndMs - 1_000L).coerceAtLeast(0L))
        _uploadState.update {
            it.copy(
                trimStartMs = newStart.coerceAtMost(duration),
                activePreviewClipId = null
            )
        }
    }

    fun nudgeTrimEnd(deltaMs: Long) {
        val state = _uploadState.value
        val duration = state.inspectedVideo?.durationMs ?: return
        val newEnd = (state.trimEndMs + deltaMs).coerceIn((state.trimStartMs + 1_000L).coerceAtMost(duration), duration)
        _uploadState.update {
            it.copy(
                trimEndMs = newEnd,
                activePreviewClipId = null
            )
        }
    }

    fun triggerPreviewTrimSeek() {
        val start = _uploadState.value.trimStartMs
        _uploadState.update { it.copy(activePreviewClipId = null, previewSeekTriggerMs = start) }
    }

    fun previewStagedClip(localId: String?) {
        _uploadState.update { state ->
            val target = state.stagedClips.firstOrNull { it.localId == localId }
            state.copy(
                activePreviewClipId = target?.localId,
                previewSeekTriggerMs = target?.startMs
            )
        }
    }

    fun consumePreviewSeekTrigger() {
        _uploadState.update { it.copy(previewSeekTriggerMs = null) }
    }

    fun addManualClipToStage() {
        val state = _uploadState.value
        val video = state.inspectedVideo ?: return
        val clipIndex = state.stagedClips.size + 1
        val title = state.manualClipTitle.trim().ifBlank { "Highlight #$clipIndex" }
        val subtitle = state.manualClipSubtitle.trim()
        val midpoint = (state.trimStartMs + state.trimEndMs) / 2L

        viewModelScope.launch {
            _uploadState.update { it.copy(isGeneratingClips = true) }
            val thumb = repository.extractFrameThumbnail(video.uri, midpoint) ?: video.thumbnailPath
            val draft = StagedClipDraft(
                title = title,
                startMs = state.trimStartMs,
                endMs = state.trimEndMs,
                aspectRatio = state.selectedAspectRatio,
                captionStyle = state.selectedCaptionStyle,
                thumbnailPath = thumb,
                subtitleText = subtitle
            )
            _uploadState.update { current ->
                val nextIndex = current.stagedClips.size + 2
                current.copy(
                    isGeneratingClips = false,
                    stagedClips = current.stagedClips + draft,
                    manualClipTitle = "Highlight #$nextIndex",
                    manualClipSubtitle = ""
                )
            }
            refreshCacheSize()
            _snackbarMessage.value = "Added \"$title\" to clip queue"
        }
    }

    fun updateStagedClip(
        localId: String,
        newTitle: String,
        newSubtitle: String,
        newStartMs: Long,
        newEndMs: Long
    ) {
        val duration = _uploadState.value.inspectedVideo?.durationMs ?: Long.MAX_VALUE
        val safeStart = newStartMs.coerceIn(0L, (duration - 500L).coerceAtLeast(0L))
        val safeEnd = newEndMs.coerceIn((safeStart + 500L).coerceAtMost(duration), duration)
        _uploadState.update { state ->
            state.copy(
                stagedClips = state.stagedClips.map { draft ->
                    if (draft.localId == localId) {
                        draft.copy(
                            title = newTitle.trim().ifBlank { draft.title },
                            subtitleText = newSubtitle.trim(),
                            startMs = safeStart,
                            endMs = safeEnd
                        )
                    } else {
                        draft
                    }
                }
            )
        }
        _snackbarMessage.value = "Updated clip \"$newTitle\""
    }

    fun exportStagedClipMp4(draft: StagedClipDraft) {
        val state = _uploadState.value
        val video = state.inspectedVideo ?: return
        if (state.exportingDraftId != null || state.isBatchExportingDrafts) return

        viewModelScope.launch {
            _uploadState.update {
                it.copy(
                    exportingDraftId = draft.localId,
                    exportDraftProgress = 5,
                    errorMessage = null
                )
            }
            try {
                val currentSettings = settings.value
                val convert9x16 = draft.aspectRatio == AspectRatioOption.PORTRAIT_9_16 ||
                    currentSettings.autoConvert9x16Export
                val result = shortsExporter.exportShortMp4(
                    sourceVideoUri = video.uri,
                    clipTitle = draft.title,
                    startMs = draft.startMs,
                    endMs = draft.endMs,
                    convertTo9x16 = convert9x16,
                    smartCropMode = SmartCropMode.CENTER,
                    exportQuality = currentSettings.exportQuality,
                    onProgress = { pct ->
                        _uploadState.update { it.copy(exportDraftProgress = pct) }
                    }
                )
                repository.recordDownload(
                    ExportedDownloadEntity(
                        fileName = result.fileName,
                        contentUri = result.contentUri.toString(),
                        locationLabel = result.locationLabel,
                        mimeType = "video/mp4",
                        sizeBytes = result.sizeBytes,
                        sourceModule = "Upload Studio (${currentSettings.exportQuality.code})",
                        aspectRatio = if (result.convertedTo9x16) "9:16" else draft.aspectRatio.code,
                        convertedTo9x16 = result.convertedTo9x16
                    )
                )
                refreshCacheSize()
                _snackbarMessage.value = "Exported ${result.fileName} to Downloads/YodhaClipAI"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = "MP4 export failed: ${e.localizedMessage ?: "Unable to trim this video format locally."}"
                _uploadState.update { it.copy(errorMessage = msg) }
                _snackbarMessage.value = msg
            } finally {
                _uploadState.update {
                    it.copy(exportingDraftId = null, exportDraftProgress = 0)
                }
            }
        }
    }

    fun exportAllStagedClipsMp4() {
        val state = _uploadState.value
        val video = state.inspectedVideo ?: return
        val drafts = state.stagedClips
        if (drafts.isEmpty() || state.exportingDraftId != null || state.isBatchExportingDrafts) return

        viewModelScope.launch {
            _uploadState.update {
                it.copy(
                    isBatchExportingDrafts = true,
                    batchExportStatus = "Exporting 1 of ${drafts.size}…",
                    errorMessage = null
                )
            }
            val currentSettings = settings.value
            var succeeded = 0
            var failed = 0
            try {
                drafts.forEachIndexed { idx, draft ->
                    _uploadState.update {
                        it.copy(
                            exportingDraftId = draft.localId,
                            exportDraftProgress = 5,
                            batchExportStatus = "Exporting ${idx + 1} of ${drafts.size}: ${draft.title}"
                        )
                    }
                    try {
                        val convert9x16 = draft.aspectRatio == AspectRatioOption.PORTRAIT_9_16 ||
                            currentSettings.autoConvert9x16Export
                        val result = shortsExporter.exportShortMp4(
                            sourceVideoUri = video.uri,
                            clipTitle = draft.title,
                            startMs = draft.startMs,
                            endMs = draft.endMs,
                            convertTo9x16 = convert9x16,
                            smartCropMode = SmartCropMode.CENTER,
                            exportQuality = currentSettings.exportQuality,
                            onProgress = { pct ->
                                _uploadState.update { it.copy(exportDraftProgress = pct) }
                            }
                        )
                        repository.recordDownload(
                            ExportedDownloadEntity(
                                fileName = result.fileName,
                                contentUri = result.contentUri.toString(),
                                locationLabel = result.locationLabel,
                                mimeType = "video/mp4",
                                sizeBytes = result.sizeBytes,
                                sourceModule = "Upload Studio Batch",
                                aspectRatio = if (result.convertedTo9x16) "9:16" else draft.aspectRatio.code,
                                convertedTo9x16 = result.convertedTo9x16
                            )
                        )
                        succeeded++
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (_: Exception) {
                        failed++
                    }
                }
                refreshCacheSize()
                val summary = if (failed == 0) {
                    "Exported all $succeeded MP4 clips locally to Downloads/YodhaClipAI"
                } else {
                    "Exported $succeeded MP4 clip(s); $failed failed."
                }
                _snackbarMessage.value = summary
            } finally {
                _uploadState.update {
                    it.copy(
                        isBatchExportingDrafts = false,
                        exportingDraftId = null,
                        exportDraftProgress = 0,
                        batchExportStatus = null
                    )
                }
            }
        }
    }

    fun updateUploadAiTranscript(text: String) {
        _uploadState.update { it.copy(aiTranscriptInput = text) }
    }

    fun setUploadAiConsent(granted: Boolean) {
        _uploadState.update { it.copy(aiConsentGranted = granted) }
    }

    fun loadUploadTranscriptFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver
                        .openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?.take(45_000)
                        .orEmpty()
                }
                if (text.isBlank()) {
                    _snackbarMessage.value = "Selected transcript file is empty."
                } else {
                    _uploadState.update { it.copy(aiTranscriptInput = text) }
                    _snackbarMessage.value = "Loaded transcript (${text.length} chars) locally"
                }
            } catch (e: Exception) {
                _snackbarMessage.value = "Could not read transcript file: ${e.localizedMessage ?: "Access denied"}"
            }
        }
    }

    fun analyzeUploadTranscriptWithAi() {
        val state = _uploadState.value
        val video = state.inspectedVideo ?: return
        val transcript = state.aiTranscriptInput.trim()

        if (!state.aiConsentGranted) {
            _uploadState.update {
                it.copy(
                    aiHighlightState = UploadAiState.Error(
                        "Please check the consent box first so only your transcript text is sent to AI. Your private video file is never uploaded."
                    )
                )
            }
            return
        }
        if (transcript.length < 15) {
            _uploadState.update {
                it.copy(
                    aiHighlightState = UploadAiState.Error(
                        "Paste or load a video transcript (at least 15 characters) so AI can detect real highlights. Manual timeline clipping works without AI."
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _uploadState.update { it.copy(aiHighlightState = UploadAiState.Loading) }
            try {
                val lang = SubtitleLanguage.entries.firstOrNull {
                    it.code == settings.value.defaultLanguageCode
                } ?: SubtitleLanguage.ENGLISH

                val analysis = uploadAiService.detectBestMoments(
                    videoTitle = state.projectTitle.ifBlank { video.suggestedTitle },
                    transcript = transcript,
                    durationMs = video.durationMs,
                    requestedCount = 5,
                    targetDurationSec = settings.value.smartClipSeconds,
                    removePausesAndKeepSentences = true,
                    primaryLanguage = lang
                )
                val suggestions = analysis.proposals.map { proposal ->
                    UploadAiHighlightSuggestion(
                        title = proposal.hookTitle,
                        startMs = proposal.startMs,
                        endMs = proposal.endMs,
                        viralScore = proposal.viralScore,
                        reason = proposal.reason,
                        suggestedSubtitle = proposal.subtitlesFor(lang).joinToString(" ")
                    )
                }
                if (suggestions.isEmpty()) {
                    _uploadState.update {
                        it.copy(
                            aiHighlightState = UploadAiState.Error(
                                "AI returned no highlight timestamps for this transcript. You can still trim and export MP4 clips manually above."
                            )
                        )
                    }
                } else {
                    _uploadState.update {
                        it.copy(aiHighlightState = UploadAiState.Success(suggestions))
                    }
                    _snackbarMessage.value = "AI suggested ${suggestions.size} highlights — review & approve below"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uploadState.update {
                    it.copy(
                        aiHighlightState = UploadAiState.Error(
                            "AI highlight service is currently unavailable (${e.localizedMessage ?: "network or quota error"}). " +
                                "No fake results were generated — you can continue trimming, subtitling, and exporting MP4 clips locally for free."
                        )
                    )
                }
            }
        }
    }

    fun approveAiHighlight(suggestion: UploadAiHighlightSuggestion) {
        val state = _uploadState.value
        val video = state.inspectedVideo ?: return
        if (suggestion.approved) return

        viewModelScope.launch {
            val mid = (suggestion.startMs + suggestion.endMs) / 2L
            val thumb = repository.extractFrameThumbnail(video.uri, mid) ?: video.thumbnailPath
            val draft = StagedClipDraft(
                title = suggestion.title,
                startMs = suggestion.startMs,
                endMs = suggestion.endMs,
                aspectRatio = state.selectedAspectRatio,
                captionStyle = state.selectedCaptionStyle,
                thumbnailPath = thumb,
                subtitleText = suggestion.suggestedSubtitle,
                isAiSuggested = true,
                aiReason = suggestion.reason
            )
            _uploadState.update { current ->
                val updatedAi = when (val aiState = current.aiHighlightState) {
                    is UploadAiState.Success -> UploadAiState.Success(
                        aiState.suggestions.map { item ->
                            if (item.id == suggestion.id) item.copy(approved = true) else item
                        }
                    )
                    else -> aiState
                }
                current.copy(
                    stagedClips = current.stagedClips + draft,
                    aiHighlightState = updatedAi
                )
            }
            refreshCacheSize()
            _snackbarMessage.value = "Approved \"${suggestion.title}\" — added to Ready Clips Queue"
        }
    }

    fun dismissAiHighlight(suggestionId: String) {
        _uploadState.update { current ->
            val updatedAi = when (val aiState = current.aiHighlightState) {
                is UploadAiState.Success -> UploadAiState.Success(
                    aiState.suggestions.filterNot { it.id == suggestionId }
                )
                else -> aiState
            }
            current.copy(aiHighlightState = updatedAi)
        }
    }

    fun generateSmartClips() {
        val state = _uploadState.value
        val video = state.inspectedVideo ?: return
        val targetSegmentMs = (settings.value.smartClipSeconds * 1000L).coerceAtLeast(5_000L)

        viewModelScope.launch {
            _uploadState.update { it.copy(isGeneratingClips = true) }
            val totalMs = video.durationMs.coerceAtLeast(1000L)
            val generated = mutableListOf<StagedClipDraft>()
            val labels = listOf(
                "Opening Hook",
                "Key Moment",
                "Spotlight Cut",
                "Action Peak",
                "Story Beat",
                "Closing Punch"
            )

            if (totalMs <= targetSegmentMs) {
                val thumb = repository.extractFrameThumbnail(video.uri, totalMs / 2L) ?: video.thumbnailPath
                generated += StagedClipDraft(
                    title = "Full Spotlight Clip",
                    startMs = 0L,
                    endMs = totalMs,
                    aspectRatio = state.selectedAspectRatio,
                    captionStyle = state.selectedCaptionStyle,
                    thumbnailPath = thumb
                )
            } else {
                val maxSegments = 6
                val count = ((totalMs + targetSegmentMs - 1L) / targetSegmentMs)
                    .toInt()
                    .coerceIn(2, maxSegments)
                val stepMs = totalMs / count
                for (i in 0 until count) {
                    val segStart = i * stepMs
                    val segEnd = if (i == count - 1) totalMs else ((i + 1) * stepMs).coerceAtMost(totalMs)
                    val mid = (segStart + segEnd) / 2L
                    val thumb = repository.extractFrameThumbnail(video.uri, mid) ?: video.thumbnailPath
                    generated += StagedClipDraft(
                        title = "${labels[i % labels.size]} #${i + 1}",
                        startMs = segStart,
                        endMs = segEnd,
                        aspectRatio = state.selectedAspectRatio,
                        captionStyle = state.selectedCaptionStyle,
                        thumbnailPath = thumb
                    )
                }
            }

            _uploadState.update {
                it.copy(
                    isGeneratingClips = false,
                    stagedClips = generated,
                    manualClipTitle = "Highlight #${generated.size + 1}"
                )
            }
            refreshCacheSize()
            _snackbarMessage.value = "Generated ${generated.size} smart clips on-device"
        }
    }

    fun removeStagedClip(localId: String) {
        _uploadState.update { state ->
            state.copy(stagedClips = state.stagedClips.filterNot { it.localId == localId })
        }
    }

    fun clearUploadSelection() {
        _uploadState.value = UploadStudioState(
            selectedAspectRatio = settings.value.defaultAspectRatio,
            selectedCaptionStyle = settings.value.defaultCaptionStyle
        )
    }

    fun saveCurrentProjectAndClips(onSaved: (Long) -> Unit) {
        val state = _uploadState.value
        val video = state.inspectedVideo ?: return
        if (state.isSaving) return

        viewModelScope.launch {
            _uploadState.update { it.copy(isSaving = true) }
            val currentSettings = settings.value
            val cleanTitle = state.projectTitle.trim().ifBlank { video.suggestedTitle }
            val project = VideoProjectEntity(
                title = cleanTitle,
                videoUri = video.uri,
                fileName = video.fileName,
                thumbnailPath = video.thumbnailPath,
                durationMs = video.durationMs,
                width = video.width,
                height = video.height,
                fileSizeBytes = video.fileSizeBytes,
                aspectRatio = state.selectedAspectRatio.code,
                captionStyle = state.selectedCaptionStyle.code,
                exportQuality = currentSettings.exportQuality.code
            )

            val clipsToPersist = if (state.stagedClips.isNotEmpty()) {
                state.stagedClips.map { draft ->
                    VideoClipEntity(
                        projectId = 0L,
                        projectTitle = cleanTitle,
                        title = draft.title,
                        videoUri = video.uri,
                        thumbnailPath = draft.thumbnailPath ?: video.thumbnailPath,
                        startMs = draft.startMs,
                        endMs = draft.endMs,
                        aspectRatio = draft.aspectRatio.code,
                        captionStyle = draft.captionStyle.code
                    )
                }
            } else {
                // Save current trim selection as an initial clip so beginners always see immediate output
                val mid = (state.trimStartMs + state.trimEndMs) / 2L
                val thumb = repository.extractFrameThumbnail(video.uri, mid) ?: video.thumbnailPath
                listOf(
                    VideoClipEntity(
                        projectId = 0L,
                        projectTitle = cleanTitle,
                        title = state.manualClipTitle.trim().ifBlank { "$cleanTitle — Clip 1" },
                        videoUri = video.uri,
                        thumbnailPath = thumb,
                        startMs = state.trimStartMs,
                        endMs = state.trimEndMs,
                        aspectRatio = state.selectedAspectRatio.code,
                        captionStyle = state.selectedCaptionStyle.code
                    )
                )
            }

            val savedProjectId = repository.saveProjectWithClips(project, clipsToPersist)
            _uploadState.value = UploadStudioState(
                selectedAspectRatio = currentSettings.defaultAspectRatio,
                selectedCaptionStyle = currentSettings.defaultCaptionStyle
            )
            refreshCacheSize()
            _snackbarMessage.value = "Saved \"$cleanTitle\" with ${clipsToPersist.size} clip(s)"
            onSaved(savedProjectId)
        }
    }

    fun addClipToProject(
        project: VideoProjectEntity,
        title: String,
        startMs: Long,
        endMs: Long,
        aspectRatio: AspectRatioOption,
        captionStyle: CaptionStyleOption
    ) {
        viewModelScope.launch {
            val mid = (startMs + endMs) / 2L
            val thumb = repository.extractFrameThumbnail(project.videoUri, mid) ?: project.thumbnailPath
            val safeTitle = title.trim().ifBlank { "Clip ${Formatters.formatDuration(startMs)}" }
            repository.addClip(
                VideoClipEntity(
                    projectId = project.id,
                    projectTitle = project.title,
                    title = safeTitle,
                    videoUri = project.videoUri,
                    thumbnailPath = thumb,
                    startMs = startMs,
                    endMs = endMs,
                    aspectRatio = aspectRatio.code,
                    captionStyle = captionStyle.code
                )
            )
            refreshCacheSize()
            _snackbarMessage.value = "Saved clip \"$safeTitle\""
        }
    }

    fun toggleClipFavorite(clip: VideoClipEntity) {
        viewModelScope.launch {
            repository.updateClip(clip.copy(isFavorite = !clip.isFavorite))
        }
    }

    fun editSavedClip(
        clip: VideoClipEntity,
        newTitle: String,
        newStartMs: Long,
        newEndMs: Long,
        newAspectRatio: AspectRatioOption,
        newCaptionStyle: CaptionStyleOption
    ) {
        viewModelScope.launch {
            val cleanTitle = newTitle.trim().ifBlank { clip.title }
            val safeStart = newStartMs.coerceAtLeast(0L)
            val safeEnd = newEndMs.coerceAtLeast(safeStart + 1_000L)
            val mid = (safeStart + safeEnd) / 2L
            val thumb = repository.extractFrameThumbnail(clip.videoUri, mid) ?: clip.thumbnailPath
            repository.updateClip(
                clip.copy(
                    title = cleanTitle,
                    startMs = safeStart,
                    endMs = safeEnd,
                    aspectRatio = newAspectRatio.code,
                    captionStyle = newCaptionStyle.code,
                    thumbnailPath = thumb
                )
            )
            refreshCacheSize()
            _snackbarMessage.value = "Updated clip \"$cleanTitle\""
        }
    }

    fun exportSavedClipToMp4(clip: VideoClipEntity) {
        if (_exportingClipId.value != null) return
        viewModelScope.launch {
            _exportingClipId.value = clip.id
            _exportClipProgress.value = 5
            try {
                val currentSettings = settings.value
                val convert9x16 = clip.aspectRatio == AspectRatioOption.PORTRAIT_9_16.code ||
                    currentSettings.autoConvert9x16Export
                val result = shortsExporter.exportShortMp4(
                    sourceVideoUri = clip.videoUri,
                    clipTitle = clip.title,
                    startMs = clip.startMs,
                    endMs = clip.endMs,
                    convertTo9x16 = convert9x16,
                    smartCropMode = SmartCropMode.CENTER,
                    exportQuality = currentSettings.exportQuality,
                    onProgress = { pct -> _exportClipProgress.value = pct }
                )
                repository.recordDownload(
                    ExportedDownloadEntity(
                        fileName = result.fileName,
                        contentUri = result.contentUri.toString(),
                        locationLabel = result.locationLabel,
                        mimeType = "video/mp4",
                        sizeBytes = result.sizeBytes,
                        sourceModule = "My Clips (${currentSettings.exportQuality.code})",
                        aspectRatio = if (result.convertedTo9x16) "9:16" else clip.aspectRatio,
                        convertedTo9x16 = result.convertedTo9x16
                    )
                )
                refreshCacheSize()
                _snackbarMessage.value = "Downloaded ${result.fileName} to Downloads/YodhaClipAI"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = "Export failed: ${e.localizedMessage ?: "Unable to read local video file"}"
            } finally {
                _exportingClipId.value = null
                _exportClipProgress.value = 0
            }
        }
    }

    fun deleteClip(clipId: Long) {
        viewModelScope.launch {
            repository.deleteClip(clipId)
            _snackbarMessage.value = "Clip removed"
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            _snackbarMessage.value = "Project deleted"
        }
    }

    fun setExportQuality(option: ExportQualityOption) {
        viewModelScope.launch { repository.setExportQuality(option) }
    }

    fun setDefaultAspectRatio(option: AspectRatioOption) {
        viewModelScope.launch { repository.setDefaultAspectRatio(option) }
    }

    fun setSmartClipSeconds(seconds: Int) {
        viewModelScope.launch { repository.setSmartClipSeconds(seconds) }
    }

    fun setDefaultCaptionStyle(option: CaptionStyleOption) {
        viewModelScope.launch { repository.setDefaultCaptionStyle(option) }
    }

    fun setDefaultLanguageCode(code: String) {
        viewModelScope.launch { repository.setDefaultLanguageCode(code) }
    }

    fun setAutoConvert9x16Export(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoConvert9x16Export(enabled) }
    }

    fun setShowBeginnerTips(enabled: Boolean) {
        viewModelScope.launch { repository.setShowBeginnerTips(enabled) }
    }

    fun setLoopClipPreviews(enabled: Boolean) {
        viewModelScope.launch { repository.setLoopClipPreviews(enabled) }
    }

    fun clearThumbnailCache() {
        viewModelScope.launch {
            val cleared = repository.clearThumbnailCache()
            refreshCacheSize()
            _snackbarMessage.value = "Cleared ${Formatters.formatFileSize(cleared)} of local thumbnail cache"
        }
    }

    fun clearLiveBufferCache() {
        viewModelScope.launch {
            val cleared = repository.clearLiveBufferStorage()
            refreshCacheSize()
            _snackbarMessage.value = "Cleared ${Formatters.formatFileSize(cleared)} of live rolling buffer"
        }
    }

    fun clearExportTempCache() {
        viewModelScope.launch {
            val cleared = repository.clearExportTempStorage()
            refreshCacheSize()
            _snackbarMessage.value = "Cleared ${Formatters.formatFileSize(cleared)} of export temp files"
        }
    }

    fun clearAllStudioCaches() {
        viewModelScope.launch {
            val cleared = repository.clearAllStudioCaches()
            refreshCacheSize()
            _snackbarMessage.value = "Cleared ${Formatters.formatFileSize(cleared)} of studio cache storage"
        }
    }

    fun deleteDownloadRecord(downloadId: Long) {
        viewModelScope.launch {
            repository.deleteDownload(downloadId)
            refreshCacheSize()
            _snackbarMessage.value = "Removed download entry"
        }
    }

    fun clearDownloadHistory() {
        viewModelScope.launch {
            repository.clearAllDownloads()
            refreshCacheSize()
            _snackbarMessage.value = "Cleared Download Manager history"
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            repository.resetSettings()
            _snackbarMessage.value = "Studio preferences reset to defaults"
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val dlBytes = downloads.value.sumOf { it.sizeBytes }
            val breakdown = repository.inspectStorageBreakdown(dlBytes)
            _storageBreakdown.value = breakdown
            _cacheSizeBytes.value = breakdown.totalManagedCacheBytes
        }
    }
}
