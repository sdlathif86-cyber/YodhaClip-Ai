package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.LongVideoAiService
import com.example.ai.LongVideoAnalysisResult
import com.example.ai.LongVideoClipProposal
import com.example.ai.SubtitleLanguage
import com.example.ai.YouTubeShortsAiService
import android.provider.OpenableColumns
import com.example.data.AspectRatioOption
import com.example.data.CaptionStyleOption
import com.example.data.ExportQualityOption
import com.example.data.ExportedDownloadEntity
import com.example.data.ExportedShortResult
import com.example.data.InspectedVideoInfo
import com.example.data.ShortsMp4Exporter
import com.example.data.SmartCropMode
import com.example.data.VideoAudioExtractor
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

private const val TAG = "LongVideoShortsVM"

enum class VideoInputMode(val label: String) {
    FILE_MANAGER("Upload MP4"),
    GALLERY("Gallery"),
    YOUTUBE_LINK("YouTube Link")
}

sealed interface InlineYouTubeState {
    data object Idle : InlineYouTubeState
    data object Loading : InlineYouTubeState
    data class Loaded(val metadata: YouTubeVideoMetadata) : InlineYouTubeState
    data class Error(val message: String) : InlineYouTubeState
}

sealed interface LongTranscriptState {
    data object Idle : LongTranscriptState
    data class Processing(val percent: Int, val stage: String) : LongTranscriptState
    data class Ready(val detectedLanguage: String, val summary: String) : LongTranscriptState
    data class Error(val message: String) : LongTranscriptState
}

sealed interface LongMomentsState {
    data object Idle : LongMomentsState
    data class Processing(val stage: String) : LongMomentsState
    data class Success(val result: LongVideoAnalysisResult) : LongMomentsState
    data class Error(val message: String) : LongMomentsState
}

data class LongVideoShortsUiState(
    val inputMode: VideoInputMode = VideoInputMode.FILE_MANAGER,
    val youtubeUrlInput: String = "",
    val youtubeState: InlineYouTubeState = InlineYouTubeState.Idle,
    val youtubeOwnershipConfirmed: Boolean = false,
    val selectedVideo: InspectedVideoInfo? = null,
    val isUploading: Boolean = false,
    val uploadProgressPercent: Int = 0,
    val uploadStageLabel: String = "",
    val transcriptText: String = "",
    val transcriptState: LongTranscriptState = LongTranscriptState.Idle,
    val requestedMomentCount: Int = 10,
    val targetClipDurationSec: Int = 30,
    val isCustomDuration: Boolean = false,
    val removePausesAndKeepSentences: Boolean = true,
    val subtitleLanguage: SubtitleLanguage = SubtitleLanguage.ENGLISH,
    val convertTo9x16: Boolean = true,
    val defaultSmartCrop: SmartCropMode = SmartCropMode.CENTER,
    val backgroundMusicUri: String? = null,
    val backgroundMusicName: String? = null,
    val momentsState: LongMomentsState = LongMomentsState.Idle,
    val selectedClipIds: Set<String> = emptySet(),
    val regeneratingClipId: String? = null,
    val exportingClipId: String? = null,
    val exportProgressPercent: Int = 0,
    val isBatchExporting: Boolean = false,
    val batchExportStatus: String? = null,
    val exportedMp4Map: Map<String, ExportedShortResult> = emptyMap(),
    val isSavingToLibrary: Boolean = false,
    val engineLabel: String = "",
    val statusMessage: String? = null
)

class LongVideoShortsViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaInspector = VideoMediaInspector(application)
    private val audioExtractor = VideoAudioExtractor(application)
    private val youtubeMetadataService = YouTubeMetadataService(application)
    private val aiService = LongVideoAiService()
    private val srtBuilder = YouTubeShortsAiService()
    private val exporter = ShortsMp4Exporter(application)
    private val dao = YodhaDatabase.getInstance(application).yodhaDao()

    private val _uiState = MutableStateFlow(
        LongVideoShortsUiState(engineLabel = exporter.detectVideoEngineLabel())
    )
    val uiState: StateFlow<LongVideoShortsUiState> = _uiState.asStateFlow()

    fun consumeStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun showStatus(message: String) {
        _uiState.update { it.copy(statusMessage = message) }
    }

    fun onVideoPicked(uri: Uri, takePersistablePermission: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploading = true,
                    uploadProgressPercent = 20,
                    uploadStageLabel = "Reading MP4 metadata & resolution…",
                    transcriptState = LongTranscriptState.Idle,
                    momentsState = LongMomentsState.Idle,
                    selectedClipIds = emptySet(),
                    exportedMp4Map = emptyMap()
                )
            }
            try {
                val info = mediaInspector.inspectVideo(uri, takePersistablePermission)
                _uiState.update {
                    it.copy(
                        selectedVideo = info,
                        isUploading = false,
                        uploadProgressPercent = 100,
                        uploadStageLabel = "Ready (${Formatters.formatDuration(info.durationMs)})",
                        statusMessage = "Loaded ${info.fileName} — ready for AI speech transcription & Shorts detection."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "MP4 inspect failed", e)
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        uploadProgressPercent = 0,
                        uploadStageLabel = "",
                        statusMessage = "Could not load selected MP4 file. Please choose another video."
                    )
                }
            }
        }
    }

    fun clearSelectedVideo() {
        _uiState.update {
            LongVideoShortsUiState(
                requestedMomentCount = it.requestedMomentCount,
                subtitleLanguage = it.subtitleLanguage,
                convertTo9x16 = it.convertTo9x16,
                defaultSmartCrop = it.defaultSmartCrop,
                engineLabel = it.engineLabel
            )
        }
    }

    fun onTranscriptTextChange(value: String) {
        _uiState.update { it.copy(transcriptText = value) }
    }

    fun onTranscriptFilePicked(uri: Uri) {
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        .orEmpty()
                }
                if (text.isBlank()) {
                    _uiState.update { it.copy(statusMessage = "Selected transcript file was empty.") }
                } else {
                    _uiState.update {
                        it.copy(
                            transcriptText = text.take(18_000),
                            transcriptState = LongTranscriptState.Ready(
                                detectedLanguage = "Imported File",
                                summary = "Imported transcript (${text.length} chars) ready for AI highlight detection."
                            ),
                            statusMessage = "Imported transcript (${text.length} chars)."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Failed to read transcript file.") }
            }
        }
    }

    fun onInputModeChange(mode: VideoInputMode) {
        _uiState.update { it.copy(inputMode = mode) }
    }

    fun onYouTubeUrlChange(url: String) {
        _uiState.update { it.copy(youtubeUrlInput = url) }
    }

    fun onYouTubeOwnershipConfirmed(confirmed: Boolean) {
        _uiState.update { it.copy(youtubeOwnershipConfirmed = confirmed) }
    }

    fun analyzeYouTubeLink() {
        val rawUrl = _uiState.value.youtubeUrlInput.trim()
        if (youtubeMetadataService.extractVideoId(rawUrl) == null) {
            _uiState.update {
                it.copy(
                    youtubeState = InlineYouTubeState.Error(
                        "Invalid YouTube URL. Paste a valid youtube.com/watch?v=... or youtu.be/... link."
                    )
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(youtubeState = InlineYouTubeState.Loading) }
            val result = youtubeMetadataService.fetchMetadata(rawUrl)
            result.fold(
                onSuccess = { meta ->
                    _uiState.update {
                        it.copy(
                            youtubeState = InlineYouTubeState.Loaded(meta),
                            statusMessage = "Loaded official YouTube metadata: ${meta.title}"
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            youtubeState = InlineYouTubeState.Error(
                                err.localizedMessage ?: "Could not fetch YouTube details."
                            )
                        )
                    }
                }
            )
        }
    }

    fun onRequestedMomentCountChange(count: Int) {
        _uiState.update { it.copy(requestedMomentCount = count) }
    }

    fun onTargetDurationChange(seconds: Int, isCustom: Boolean) {
        _uiState.update {
            it.copy(
                targetClipDurationSec = seconds.coerceIn(10, 90),
                isCustomDuration = isCustom
            )
        }
    }

    fun onRemovePausesChange(enabled: Boolean) {
        _uiState.update { it.copy(removePausesAndKeepSentences = enabled) }
    }

    fun onSubtitleLanguageChange(language: SubtitleLanguage) {
        _uiState.update { it.copy(subtitleLanguage = language) }
    }

    fun onConvertTo9x16Change(enabled: Boolean) {
        _uiState.update { it.copy(convertTo9x16 = enabled) }
    }

    fun onDefaultSmartCropChange(mode: SmartCropMode) {
        _uiState.update { it.copy(defaultSmartCrop = mode) }
    }

    fun onBackgroundMusicPicked(uri: Uri) {
        viewModelScope.launch {
            val resolver = getApplication<Application>().contentResolver
            try {
                resolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            var displayName = "Authorized Audio Track"
            try {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            displayName = c.getString(idx).orEmpty().ifBlank { displayName }
                        }
                    }
                }
            } catch (_: Exception) {
            }
            _uiState.update {
                it.copy(
                    backgroundMusicUri = uri.toString(),
                    backgroundMusicName = displayName,
                    statusMessage = "Attached background music: $displayName"
                )
            }
        }
    }

    fun clearBackgroundMusic() {
        _uiState.update {
            it.copy(
                backgroundMusicUri = null,
                backgroundMusicName = null,
                statusMessage = "Removed background music track"
            )
        }
    }

    /**
     * Step 2: Extracts actual audio/keyframes from the uploaded MP4 and runs real
     * Firebase AI Logic multimodal speech transcription.
     */
    fun generateAiTranscript() {
        val video = _uiState.value.selectedVideo
        if (video == null) {
            _uiState.update { it.copy(statusMessage = "Upload an MP4 video from your gallery first.") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    transcriptState = LongTranscriptState.Processing(
                        percent = 10,
                        stage = "Extracting audio & video frames…"
                    )
                )
            }
            var payloadFrames = emptyList<android.graphics.Bitmap>()
            try {
                val payload = audioExtractor.extractForAiAnalysis(
                    videoUri = Uri.parse(video.uri),
                    videoDurationMs = video.durationMs
                ) { percent, label ->
                    _uiState.update {
                        it.copy(
                            transcriptState = LongTranscriptState.Processing(
                                percent = (percent * 0.55f).toInt().coerceIn(10, 55),
                                stage = label
                            )
                        )
                    }
                }
                payloadFrames = payload.keyframes
                _uiState.update {
                    it.copy(
                        transcriptState = LongTranscriptState.Processing(
                            percent = 72,
                            stage = "Gemini AI transcribing speech & timestamps…"
                        )
                    )
                }
                val result = aiService.transcribeVideoMedia(
                    videoTitle = video.suggestedTitle,
                    durationMs = video.durationMs,
                    payload = payload
                )
                _uiState.update {
                    it.copy(
                        transcriptText = result.timestampedTranscript,
                        transcriptState = LongTranscriptState.Ready(
                            detectedLanguage = result.detectedLanguage,
                            summary = result.summary
                        ),
                        statusMessage = "AI transcript generated (${result.detectedLanguage})."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI transcription error", e)
                _uiState.update {
                    it.copy(
                        transcriptState = LongTranscriptState.Error(
                            e.localizedMessage ?: "AI transcription failed. Please retry."
                        )
                    )
                }
            } finally {
                payloadFrames.forEach { bmp ->
                    if (!bmp.isRecycled) bmp.recycle()
                }
            }
        }
    }

    /**
     * Step 3: Runs real Firebase AI Logic highlight detection to find the best 10, 20, or 30
     * viral moments (15-60s each) with viral scoring and Telugu/Hindi/English subtitles.
     */
    fun detectBestMomentsWithAi() {
        val state = _uiState.value
        val video = state.selectedVideo
        if (video == null) {
            _uiState.update { it.copy(statusMessage = "Upload an MP4 video first.") }
            return
        }
        val transcript = state.transcriptText.trim()
        if (transcript.isBlank()) {
            _uiState.update {
                it.copy(
                    momentsState = LongMomentsState.Error(
                        "Generate an AI speech transcript above (or paste/import a transcript) so AI analyzes real moments without guessing."
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    momentsState = LongMomentsState.Processing(
                        "Gemini AI detecting top ${state.requestedMomentCount} viral moments (~${state.targetClipDurationSec}s), speaker shifts, scenes & ${state.subtitleLanguage.label} Viral Kit…"
                    )
                )
            }
            try {
                val ytTitle = (state.youtubeState as? InlineYouTubeState.Loaded)?.metadata?.title
                val effectiveTitle = ytTitle ?: video.suggestedTitle
                val result = aiService.detectBestMoments(
                    videoTitle = effectiveTitle,
                    transcript = transcript,
                    durationMs = video.durationMs,
                    requestedCount = state.requestedMomentCount,
                    targetDurationSec = state.targetClipDurationSec,
                    removePausesAndKeepSentences = state.removePausesAndKeepSentences,
                    primaryLanguage = state.subtitleLanguage
                )
                val initialSelected = result.proposals.take(5).map { it.id }.toSet()
                _uiState.update {
                    it.copy(
                        momentsState = LongMomentsState.Success(result),
                        selectedClipIds = initialSelected,
                        statusMessage = "Detected ${result.proposals.size} viral Shorts moments!"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI highlight detection error", e)
                _uiState.update {
                    it.copy(
                        momentsState = LongMomentsState.Error(
                            e.localizedMessage ?: "AI moment detection failed. Please retry."
                        )
                    )
                }
            }
        }
    }

    fun toggleClipSelection(clipId: String) {
        _uiState.update { state ->
            val updated = state.selectedClipIds.toMutableSet()
            if (!updated.add(clipId)) updated.remove(clipId)
            state.copy(selectedClipIds = updated)
        }
    }

    fun selectAllClips() {
        val success = _uiState.value.momentsState as? LongMomentsState.Success ?: return
        _uiState.update { it.copy(selectedClipIds = success.result.proposals.map { p -> p.id }.toSet()) }
    }

    fun clearClipSelection() {
        _uiState.update { it.copy(selectedClipIds = emptySet()) }
    }

    /**
     * Editable Clip Timeline & Captions: Updates start/end timestamps, title, hashtags,
     * smart vertical crop, viral kit (hook, SEO description, thumbnail text), or editable captions.
     */
    fun updateClipProposal(
        clipId: String,
        newTitle: String? = null,
        newStartMs: Long? = null,
        newEndMs: Long? = null,
        newCropMode: SmartCropMode? = null,
        newHashtags: List<String>? = null,
        newOpeningHook: String? = null,
        newSeoDescription: String? = null,
        newThumbnailText: String? = null,
        newSubtitleLines: List<String>? = null,
        subtitleLanguage: SubtitleLanguage = _uiState.value.subtitleLanguage
    ) {
        val success = _uiState.value.momentsState as? LongMomentsState.Success ?: return
        val maxDuration = _uiState.value.selectedVideo?.durationMs ?: Long.MAX_VALUE
        val updatedProposals = success.result.proposals.map { proposal ->
            if (proposal.id != clipId) {
                proposal
            } else {
                val safeStart = (newStartMs ?: proposal.startMs).coerceIn(0L, (maxDuration - 3_000L).coerceAtLeast(0L))
                val safeEnd = (newEndMs ?: proposal.endMs).coerceIn(
                    (safeStart + 3_000L).coerceAtMost(maxDuration),
                    maxDuration
                )
                val cleanSubtitles = newSubtitleLines?.map { it.trim() }?.filter { it.isNotBlank() }
                proposal.copy(
                    hookTitle = newTitle?.trim()?.ifBlank { proposal.hookTitle } ?: proposal.hookTitle,
                    startMs = safeStart,
                    endMs = safeEnd,
                    recommendedCropMode = newCropMode ?: proposal.recommendedCropMode,
                    hashtags = newHashtags ?: proposal.hashtags,
                    openingHook = newOpeningHook?.trim()?.ifBlank { proposal.openingHook } ?: proposal.openingHook,
                    seoDescription = newSeoDescription?.trim()?.ifBlank { proposal.seoDescription } ?: proposal.seoDescription,
                    thumbnailText = newThumbnailText?.trim()?.ifBlank { proposal.thumbnailText } ?: proposal.thumbnailText,
                    englishSubtitles = if (cleanSubtitles != null && cleanSubtitles.isNotEmpty() && subtitleLanguage == SubtitleLanguage.ENGLISH) cleanSubtitles else proposal.englishSubtitles,
                    hindiSubtitles = if (cleanSubtitles != null && cleanSubtitles.isNotEmpty() && subtitleLanguage == SubtitleLanguage.HINDI) cleanSubtitles else proposal.hindiSubtitles,
                    teluguSubtitles = if (cleanSubtitles != null && cleanSubtitles.isNotEmpty() && subtitleLanguage == SubtitleLanguage.TELUGU) cleanSubtitles else proposal.teluguSubtitles
                )
            }
        }
        _uiState.update {
            it.copy(
                momentsState = LongMomentsState.Success(
                    success.result.copy(proposals = updatedProposals)
                )
            )
        }
    }

    fun regenerateClipMetadata(proposal: LongVideoClipProposal) {
        val state = _uiState.value
        val transcript = state.transcriptText
        viewModelScope.launch {
            _uiState.update { it.copy(regeneratingClipId = proposal.id) }
            try {
                val fresh = aiService.regenerateTitleAndHashtags(
                    currentTitle = proposal.hookTitle,
                    transcriptSnippet = "${proposal.reason}\n$transcript",
                    category = proposal.category,
                    language = state.subtitleLanguage
                )
                updateClipProposal(
                    clipId = proposal.id,
                    newTitle = fresh.hookTitle,
                    newHashtags = fresh.hashtags,
                    newOpeningHook = fresh.openingHook,
                    newSeoDescription = fresh.seoDescription,
                    newThumbnailText = fresh.thumbnailText
                )
                _uiState.update {
                    it.copy(
                        regeneratingClipId = null,
                        statusMessage = "AI refreshed Viral Kit (${state.subtitleLanguage.label}) for Clip #${proposal.rank}"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        regeneratingClipId = null,
                        statusMessage = e.localizedMessage ?: "Failed to regenerate viral kit."
                    )
                }
            }
        }
    }

    fun exportSingleClipMp4(
        proposal: LongVideoClipProposal,
        exportQuality: ExportQualityOption = ExportQualityOption.FHD_1080,
        onDownloadRecorded: () -> Unit = {}
    ) {
        val state = _uiState.value
        val video = state.selectedVideo ?: return
        if (state.exportingClipId != null || state.isBatchExporting) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(exportingClipId = proposal.id, exportProgressPercent = 5)
            }
            try {
                val result = exporter.exportShortMp4(
                    sourceVideoUri = video.uri,
                    clipTitle = proposal.hookTitle,
                    startMs = proposal.startMs,
                    endMs = proposal.endMs,
                    convertTo9x16 = state.convertTo9x16,
                    smartCropMode = proposal.recommendedCropMode,
                    exportQuality = exportQuality,
                    backgroundMusicUri = state.backgroundMusicUri,
                    onProgress = { pct ->
                        _uiState.update { it.copy(exportProgressPercent = pct) }
                    }
                )
                dao.insertDownload(
                    ExportedDownloadEntity(
                        fileName = result.fileName,
                        contentUri = result.contentUri.toString(),
                        locationLabel = result.locationLabel,
                        mimeType = "video/mp4",
                        sizeBytes = result.sizeBytes,
                        sourceModule = "Long Video to Shorts (${exportQuality.code})",
                        aspectRatio = if (result.convertedTo9x16) "9:16" else "Original",
                        convertedTo9x16 = result.convertedTo9x16
                    )
                )
                onDownloadRecorded()
                _uiState.update { current ->
                    current.copy(
                        exportingClipId = null,
                        exportProgressPercent = 0,
                        exportedMp4Map = current.exportedMp4Map + (proposal.id to result),
                        statusMessage = "Exported ${result.fileName} (${exportQuality.code}) to Downloads/YodhaClipAI"
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
                        statusMessage = "Export failed: ${e.localizedMessage ?: "Unsupported codec"}"
                    )
                }
            }
        }
    }

    fun exportSelectedBatchMp4(
        exportQuality: ExportQualityOption = ExportQualityOption.FHD_1080,
        onDownloadRecorded: () -> Unit = {}
    ) {
        val state = _uiState.value
        val video = state.selectedVideo ?: return
        val success = state.momentsState as? LongMomentsState.Success ?: return
        val chosen = success.result.proposals.filter { it.id in state.selectedClipIds }
        if (chosen.isEmpty() || state.isBatchExporting || state.exportingClipId != null) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBatchExporting = true,
                    batchExportStatus = "Preparing batch export of ${chosen.size} MP4 clips (${exportQuality.code})…"
                )
            }
            var exportedCount = 0
            val updatedMap = state.exportedMp4Map.toMutableMap()

            for ((index, clip) in chosen.withIndex()) {
                _uiState.update {
                    it.copy(
                        exportingClipId = clip.id,
                        exportProgressPercent = 5,
                        batchExportStatus = "Rendering ${index + 1}/${chosen.size}: ${clip.hookTitle}"
                    )
                }
                try {
                    val res = exporter.exportShortMp4(
                        sourceVideoUri = video.uri,
                        clipTitle = clip.hookTitle,
                        startMs = clip.startMs,
                        endMs = clip.endMs,
                        convertTo9x16 = state.convertTo9x16,
                        smartCropMode = clip.recommendedCropMode,
                        exportQuality = exportQuality,
                        backgroundMusicUri = state.backgroundMusicUri,
                        onProgress = { pct ->
                            _uiState.update { it.copy(exportProgressPercent = pct) }
                        }
                    )
                    updatedMap[clip.id] = res
                    exportedCount++
                    dao.insertDownload(
                        ExportedDownloadEntity(
                            fileName = res.fileName,
                            contentUri = res.contentUri.toString(),
                            locationLabel = res.locationLabel,
                            mimeType = "video/mp4",
                            sizeBytes = res.sizeBytes,
                            sourceModule = "Long Video to Shorts (${exportQuality.code})",
                            aspectRatio = if (res.convertedTo9x16) "9:16" else "Original",
                            convertedTo9x16 = res.convertedTo9x16
                        )
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Batch item failed: ${clip.hookTitle}", e)
                }
            }

            onDownloadRecorded()
            _uiState.update {
                it.copy(
                    isBatchExporting = false,
                    exportingClipId = null,
                    exportProgressPercent = 0,
                    batchExportStatus = null,
                    exportedMp4Map = updatedMap,
                    statusMessage = "Batch complete: exported $exportedCount/${chosen.size} MP4 clips to Downloads/YodhaClipAI"
                )
            }
        }
    }

    fun exportSubtitlesSrt(proposal: LongVideoClipProposal, onDownloadRecorded: () -> Unit = {}) {
        val lang = _uiState.value.subtitleLanguage
        viewModelScope.launch {
            try {
                val srt = srtBuilder.buildSrtContent(proposal.toShortClipSuggestion(), lang)
                val saved = exporter.exportSubtitleSrt(
                    clipTitle = proposal.hookTitle,
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
                        sourceModule = "Long Video to Shorts (${lang.label} SRT)",
                        aspectRatio = "SRT",
                        convertedTo9x16 = false
                    )
                )
                onDownloadRecorded()
                _uiState.update {
                    it.copy(statusMessage = "Saved ${lang.label} subtitles: ${saved.locationLabel}")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Could not export SRT subtitles.") }
            }
        }
    }

    fun saveSelectedToStudioLibrary(onSavedProject: (Long) -> Unit) {
        val state = _uiState.value
        val video = state.selectedVideo ?: return
        val success = state.momentsState as? LongMomentsState.Success ?: return
        val chosen = success.result.proposals.filter { it.id in state.selectedClipIds }
            .ifEmpty { success.result.proposals.take(5) }
        if (state.isSavingToLibrary) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingToLibrary = true) }
            try {
                val aspect = if (state.convertTo9x16) AspectRatioOption.PORTRAIT_9_16.code else AspectRatioOption.LANDSCAPE_16_9.code
                val project = VideoProjectEntity(
                    title = "${video.suggestedTitle} — AI Shorts",
                    videoUri = video.uri,
                    fileName = video.fileName,
                    thumbnailPath = video.thumbnailPath,
                    durationMs = video.durationMs,
                    width = video.width,
                    height = video.height,
                    fileSizeBytes = video.fileSizeBytes,
                    aspectRatio = aspect,
                    captionStyle = CaptionStyleOption.BOLD_VIOLET.code,
                    exportQuality = "1080p"
                )
                val projectId = dao.insertProject(project)
                val entities = chosen.map { clip ->
                    val mid = (clip.startMs + clip.endMs) / 2L
                    val thumb = mediaInspector.extractFrameThumbnail(video.uri, mid) ?: video.thumbnailPath
                    VideoClipEntity(
                        projectId = projectId,
                        projectTitle = project.title,
                        title = "${clip.hookTitle} (${clip.viralScore}% Viral)",
                        videoUri = video.uri,
                        thumbnailPath = thumb,
                        startMs = clip.startMs,
                        endMs = clip.endMs,
                        aspectRatio = aspect,
                        captionStyle = CaptionStyleOption.BOLD_VIOLET.code
                    )
                }
                dao.insertClips(entities)
                _uiState.update {
                    it.copy(
                        isSavingToLibrary = false,
                        statusMessage = "Saved ${entities.size} AI Shorts clips into My Projects & My Clips"
                    )
                }
                onSavedProject(projectId)
            } catch (e: Exception) {
                Log.e(TAG, "Save to library failed", e)
                _uiState.update {
                    it.copy(
                        isSavingToLibrary = false,
                        statusMessage = "Failed to save clips to library."
                    )
                }
            }
        }
    }
}
