package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ShortClipSuggestion
import com.example.ai.ShortsAnalysisResult
import com.example.ai.SubtitleLanguage
import com.example.ai.YouTubeShortsAiService
import com.example.data.AspectRatioOption
import com.example.data.CaptionStyleOption
import com.example.data.ExportedDownloadEntity
import com.example.data.ExportedShortResult
import com.example.data.InspectedVideoInfo
import com.example.data.ShortsMp4Exporter
import com.example.data.VideoClipEntity
import com.example.data.VideoMediaInspector
import com.example.data.VideoProjectEntity
import com.example.data.YodhaDatabase
import com.example.data.YouTubeMetadataService
import com.example.data.YouTubeVideoMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "YouTubeShortsVM"

sealed interface MetadataFetchState {
    data object Idle : MetadataFetchState
    data object Loading : MetadataFetchState
    data class Success(val metadata: YouTubeVideoMetadata) : MetadataFetchState
    data class Error(val message: String) : MetadataFetchState
}

sealed interface ShortsAiState {
    data object Idle : ShortsAiState
    data object Loading : ShortsAiState
    data class Success(val result: ShortsAnalysisResult) : ShortsAiState
    data class Error(val message: String) : ShortsAiState
}

data class YouTubeShortsUiState(
    val urlInput: String = "",
    val metadataState: MetadataFetchState = MetadataFetchState.Idle,
    val ownershipConfirmed: Boolean = false,
    val originalVideo: InspectedVideoInfo? = null,
    val isInspectingOriginal: Boolean = false,
    val transcriptInput: String = "",
    val requestedClipCount: Int = 5,
    val subtitleLanguage: SubtitleLanguage = SubtitleLanguage.ENGLISH,
    val convertTo9x16: Boolean = true,
    val aiState: ShortsAiState = ShortsAiState.Idle,
    val selectedClipIds: Set<String> = emptySet(),
    val exportingClipId: String? = null,
    val exportProgressPercent: Int = 0,
    val isBatchExporting: Boolean = false,
    val batchExportStatus: String? = null,
    val exportedMp4Map: Map<String, ExportedShortResult> = emptyMap(),
    val isSavingToLibrary: Boolean = false,
    val statusMessage: String? = null
)

class YouTubeShortsViewModel(application: Application) : AndroidViewModel(application) {

    private val metadataService = YouTubeMetadataService(application)
    private val aiService = YouTubeShortsAiService()
    private val mediaInspector = VideoMediaInspector(application)
    private val mp4Exporter = ShortsMp4Exporter(application)
    private val dao = YodhaDatabase.getInstance(application).yodhaDao()

    private val _uiState = MutableStateFlow(YouTubeShortsUiState())
    val uiState: StateFlow<YouTubeShortsUiState> = _uiState.asStateFlow()

    fun consumeStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun showStatus(message: String) {
        _uiState.update { it.copy(statusMessage = message) }
    }

    fun onUrlInputChange(value: String) {
        _uiState.update { it.copy(urlInput = value) }
    }

    fun onOwnershipConfirmedChange(confirmed: Boolean) {
        _uiState.update { it.copy(ownershipConfirmed = confirmed) }
    }

    fun onTranscriptChange(value: String) {
        _uiState.update { it.copy(transcriptInput = value) }
    }

    fun onRequestedClipCountChange(count: Int) {
        _uiState.update { it.copy(requestedClipCount = count) }
    }

    fun onSubtitleLanguageChange(language: SubtitleLanguage) {
        _uiState.update { it.copy(subtitleLanguage = language) }
    }

    fun onConvertTo9x16Change(enabled: Boolean) {
        _uiState.update { it.copy(convertTo9x16 = enabled) }
    }

    fun fetchVideoDetails() {
        val rawUrl = _uiState.value.urlInput.trim()
        if (rawUrl.isBlank()) {
            _uiState.update {
                it.copy(
                    metadataState = MetadataFetchState.Error(
                        "Paste a YouTube video link first (for example https://www.youtube.com/watch?v=...)."
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(metadataState = MetadataFetchState.Loading) }
            val result = metadataService.fetchMetadata(rawUrl)
            result.fold(
                onSuccess = { metadata ->
                    _uiState.update {
                        it.copy(
                            metadataState = MetadataFetchState.Success(metadata),
                            statusMessage = "Fetched official metadata via ${metadata.apiSource}"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            metadataState = MetadataFetchState.Error(
                                error.localizedMessage ?: "Unable to fetch YouTube video details."
                            )
                        )
                    }
                }
            )
        }
    }

    fun onOriginalVideoPicked(uri: Uri, takePersistablePermission: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isInspectingOriginal = true) }
            try {
                val info = mediaInspector.inspectVideo(uri, takePersistablePermission)
                _uiState.update {
                    it.copy(
                        originalVideo = info,
                        isInspectingOriginal = false,
                        ownershipConfirmed = true,
                        statusMessage = "Attached original MP4: ${info.fileName} (${Formatters.formatDuration(info.durationMs)})"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to inspect uploaded original video", e)
                _uiState.update {
                    it.copy(
                        isInspectingOriginal = false,
                        statusMessage = "Could not inspect selected MP4 file. Please pick another file."
                    )
                }
            }
        }
    }

    fun clearOriginalVideo() {
        _uiState.update { it.copy(originalVideo = null) }
    }

    fun onTranscriptFilePicked(uri: Uri) {
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver
                        .openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        .orEmpty()
                }
                if (text.isNotBlank()) {
                    _uiState.update {
                        it.copy(
                            transcriptInput = text.take(15_000),
                            statusMessage = "Imported transcript (${text.length} characters)"
                        )
                    }
                } else {
                    _uiState.update { it.copy(statusMessage = "Selected transcript file was empty.") }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read transcript file", e)
                _uiState.update { it.copy(statusMessage = "Could not read transcript file.") }
            }
        }
    }

    fun analyzeWithAi() {
        val state = _uiState.value
        val metadata = (state.metadataState as? MetadataFetchState.Success)?.metadata
        if (metadata == null && state.originalVideo == null && state.transcriptInput.isBlank()) {
            _uiState.update {
                it.copy(
                    aiState = ShortsAiState.Error(
                        "Fetch a YouTube video link, upload your original MP4, or paste a transcript first."
                    )
                )
            }
            return
        }
        if (!state.ownershipConfirmed) {
            _uiState.update {
                it.copy(
                    aiState = ShortsAiState.Error(
                        "Please confirm you own this video or have permission to edit it before analyzing."
                    )
                )
            }
            return
        }

        val effectiveDurationMs = state.originalVideo?.durationMs
            ?: metadata?.durationMs
            ?: 180_000L

        viewModelScope.launch {
            _uiState.update { it.copy(aiState = ShortsAiState.Loading) }
            try {
                val result = aiService.analyzeAndSuggestClips(
                    metadata = metadata,
                    transcript = state.transcriptInput,
                    videoDurationMs = effectiveDurationMs,
                    requestedCount = state.requestedClipCount
                )
                val initialSelection = result.suggestions.take(3).map { it.id }.toSet()
                _uiState.update {
                    it.copy(
                        aiState = ShortsAiState.Success(result),
                        selectedClipIds = initialSelection,
                        exportedMp4Map = emptyMap(),
                        statusMessage = "AI detected ${result.suggestions.size} best Shorts moments!"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Shorts analysis error", e)
                _uiState.update {
                    it.copy(
                        aiState = ShortsAiState.Error(
                            e.localizedMessage ?: "AI analysis failed. Please try again."
                        )
                    )
                }
            }
        }
    }

    fun toggleClipSelection(clipId: String) {
        _uiState.update { current ->
            val updated = current.selectedClipIds.toMutableSet()
            if (!updated.add(clipId)) {
                updated.remove(clipId)
            }
            current.copy(selectedClipIds = updated)
        }
    }

    fun selectAllClips() {
        val suggestions = (_uiState.value.aiState as? ShortsAiState.Success)?.result?.suggestions ?: return
        _uiState.update { it.copy(selectedClipIds = suggestions.map { s -> s.id }.toSet()) }
    }

    fun selectTopFiveClips() {
        val suggestions = (_uiState.value.aiState as? ShortsAiState.Success)?.result?.suggestions ?: return
        _uiState.update { it.copy(selectedClipIds = suggestions.take(5).map { s -> s.id }.toSet()) }
    }

    fun clearClipSelection() {
        _uiState.update { it.copy(selectedClipIds = emptySet()) }
    }

    fun downloadSingleShortMp4(clip: ShortClipSuggestion) {
        val state = _uiState.value
        val original = state.originalVideo
        if (original == null) {
            _uiState.update {
                it.copy(
                    statusMessage = "Original MP4 file is required to convert to 9:16 and download. Please upload your authorized MP4 above."
                )
            }
            return
        }
        if (state.exportingClipId != null || state.isBatchExporting) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    exportingClipId = clip.id,
                    exportProgressPercent = 5
                )
            }
            try {
                val safeEnd = clip.endMs.coerceAtMost(original.durationMs)
                val safeStart = clip.startMs.coerceAtMost((safeEnd - 1000L).coerceAtLeast(0L))
                val exported = mp4Exporter.exportShortMp4(
                    sourceVideoUri = original.uri,
                    clipTitle = clip.hookTitle,
                    startMs = safeStart,
                    endMs = safeEnd,
                    convertTo9x16 = state.convertTo9x16,
                    onProgress = { pct ->
                        _uiState.update { it.copy(exportProgressPercent = pct) }
                    }
                )
                dao.insertDownload(
                    ExportedDownloadEntity(
                        fileName = exported.fileName,
                        contentUri = exported.contentUri.toString(),
                        locationLabel = exported.locationLabel,
                        mimeType = "video/mp4",
                        sizeBytes = exported.sizeBytes,
                        sourceModule = "YouTube to Shorts",
                        aspectRatio = if (exported.convertedTo9x16) "9:16" else "Original",
                        convertedTo9x16 = exported.convertedTo9x16
                    )
                )
                _uiState.update { current ->
                    current.copy(
                        exportingClipId = null,
                        exportProgressPercent = 0,
                        exportedMp4Map = current.exportedMp4Map + (clip.id to exported),
                        statusMessage = "Downloaded MP4 to ${exported.locationLabel}"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "MP4 export failed", e)
                _uiState.update {
                    it.copy(
                        exportingClipId = null,
                        exportProgressPercent = 0,
                        statusMessage = "Export failed: ${e.localizedMessage ?: "Please check the source MP4 file."}"
                    )
                }
            }
        }
    }

    fun downloadSelectedShortsBatch() {
        val state = _uiState.value
        val original = state.originalVideo
        if (original == null) {
            _uiState.update {
                it.copy(
                    statusMessage = "Please upload your original authorized MP4 file first to download 9:16 Shorts."
                )
            }
            return
        }
        val suggestions = (state.aiState as? ShortsAiState.Success)?.result?.suggestions ?: return
        val targets = suggestions.filter { it.id in state.selectedClipIds }
        if (targets.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Select at least one clip to download.") }
            return
        }
        if (state.isBatchExporting || state.exportingClipId != null) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBatchExporting = true,
                    batchExportStatus = "Exporting 1 of ${targets.size}…"
                )
            }
            var successCount = 0
            for ((idx, clip) in targets.withIndex()) {
                _uiState.update {
                    it.copy(
                        exportingClipId = clip.id,
                        exportProgressPercent = 0,
                        batchExportStatus = "Converting ${idx + 1} / ${targets.size}: ${clip.hookTitle}"
                    )
                }
                try {
                    val safeEnd = clip.endMs.coerceAtMost(original.durationMs)
                    val safeStart = clip.startMs.coerceAtMost((safeEnd - 1000L).coerceAtLeast(0L))
                    val exported = mp4Exporter.exportShortMp4(
                        sourceVideoUri = original.uri,
                        clipTitle = clip.hookTitle,
                        startMs = safeStart,
                        endMs = safeEnd,
                        convertTo9x16 = state.convertTo9x16,
                        onProgress = { pct ->
                            _uiState.update { it.copy(exportProgressPercent = pct) }
                        }
                    )
                    successCount++
                    dao.insertDownload(
                        ExportedDownloadEntity(
                            fileName = exported.fileName,
                            contentUri = exported.contentUri.toString(),
                            locationLabel = exported.locationLabel,
                            mimeType = "video/mp4",
                            sizeBytes = exported.sizeBytes,
                            sourceModule = "YouTube to Shorts",
                            aspectRatio = if (exported.convertedTo9x16) "9:16" else "Original",
                            convertedTo9x16 = exported.convertedTo9x16
                        )
                    )
                    _uiState.update { current ->
                        current.copy(exportedMp4Map = current.exportedMp4Map + (clip.id to exported))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Batch export failed for clip ${clip.hookTitle}", e)
                }
            }
            _uiState.update {
                it.copy(
                    isBatchExporting = false,
                    exportingClipId = null,
                    exportProgressPercent = 0,
                    batchExportStatus = null,
                    statusMessage = "Downloaded $successCount of ${targets.size} MP4 clip(s) to Downloads/YodhaClipAI"
                )
            }
        }
    }

    fun downloadSubtitlesSrt(clip: ShortClipSuggestion) {
        val lang = _uiState.value.subtitleLanguage
        val srt = aiService.buildSrtContent(clip, lang)
        if (srt.isBlank()) {
            _uiState.update { it.copy(statusMessage = "No subtitles available for this clip.") }
            return
        }
        viewModelScope.launch {
            try {
                val saved = mp4Exporter.exportSubtitleSrt(
                    clipTitle = clip.hookTitle,
                    languageCode = lang.code,
                    srtContent = srt
                )
                dao.insertDownload(
                    ExportedDownloadEntity(
                        fileName = saved.fileName,
                        contentUri = saved.contentUri.toString(),
                        locationLabel = saved.locationLabel,
                        mimeType = "application/x-subrip",
                        sizeBytes = saved.sizeBytes,
                        sourceModule = "YouTube to Shorts (${lang.label} SRT)",
                        aspectRatio = "SRT",
                        convertedTo9x16 = false
                    )
                )
                _uiState.update {
                    it.copy(statusMessage = "Saved ${lang.label} subtitles to ${saved.locationLabel}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "SRT export failed", e)
                _uiState.update {
                    it.copy(statusMessage = "Could not save subtitle file: ${e.localizedMessage}")
                }
            }
        }
    }

    fun saveSelectedToStudioLibrary(onSaved: (Long) -> Unit) {
        val state = _uiState.value
        val original = state.originalVideo
        if (original == null) {
            _uiState.update {
                it.copy(
                    statusMessage = "Upload your original MP4 file first to save clips into My Projects & My Clips."
                )
            }
            return
        }
        val suggestions = (state.aiState as? ShortsAiState.Success)?.result?.suggestions ?: return
        val selected = suggestions.filter { it.id in state.selectedClipIds }
        if (selected.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Select at least one suggested clip first.") }
            return
        }
        if (state.isSavingToLibrary) return

        val metadata = (state.metadataState as? MetadataFetchState.Success)?.metadata
        val projectTitle = metadata?.title?.ifBlank { original.suggestedTitle } ?: original.suggestedTitle

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingToLibrary = true) }
            try {
                val project = VideoProjectEntity(
                    title = projectTitle,
                    videoUri = original.uri,
                    fileName = original.fileName,
                    thumbnailPath = original.thumbnailPath,
                    durationMs = original.durationMs,
                    width = original.width,
                    height = original.height,
                    fileSizeBytes = original.fileSizeBytes,
                    aspectRatio = AspectRatioOption.PORTRAIT_9_16.code,
                    captionStyle = CaptionStyleOption.BOLD_VIOLET.code,
                    exportQuality = "1080p"
                )
                val projectId = dao.insertProject(project)
                val clipEntities = selected.map { suggestion ->
                    val safeEnd = suggestion.endMs.coerceAtMost(original.durationMs)
                    val safeStart = suggestion.startMs.coerceAtMost((safeEnd - 1000L).coerceAtLeast(0L))
                    val thumb = mediaInspector.extractFrameThumbnail(
                        original.uri,
                        (safeStart + safeEnd) / 2L
                    ) ?: original.thumbnailPath
                    VideoClipEntity(
                        projectId = projectId,
                        projectTitle = projectTitle,
                        title = suggestion.hookTitle,
                        videoUri = original.uri,
                        thumbnailPath = thumb,
                        startMs = safeStart,
                        endMs = safeEnd,
                        aspectRatio = AspectRatioOption.PORTRAIT_9_16.code,
                        captionStyle = CaptionStyleOption.BOLD_VIOLET.code
                    )
                }
                dao.insertClips(clipEntities)
                _uiState.update {
                    it.copy(
                        isSavingToLibrary = false,
                        statusMessage = "Saved \"${projectTitle}\" & ${clipEntities.size} 9:16 Shorts to My Projects & My Clips!"
                    )
                }
                onSaved(projectId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed saving AI shorts to Room library", e)
                _uiState.update {
                    it.copy(
                        isSavingToLibrary = false,
                        statusMessage = "Could not save clips to library: ${e.localizedMessage}"
                    )
                }
            }
        }
    }
}
