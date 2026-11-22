package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.LiveHighlightAiService
import com.example.ai.LiveHighlightPreset
import com.example.ai.ShortClipSuggestion
import com.example.ai.SubtitleLanguage
import com.example.ai.YouTubeShortsAiService
import com.example.data.AspectRatioOption
import com.example.data.CaptionStyleOption
import com.example.data.ExportedDownloadEntity
import com.example.data.LiveRollingBufferEngine
import com.example.data.LiveStreamProtocol
import com.example.data.RollingBufferSnapshot
import com.example.data.ShortsMp4Exporter
import com.example.data.SmartCropMode
import com.example.data.VideoClipEntity
import com.example.data.VideoProjectEntity
import com.example.data.YodhaDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

private const val TAG = "LiveStreamShortsVM"

enum class LiveConnectionStatus(val label: String) {
    DISCONNECTED("Disconnected"),
    CONNECTING("Connecting to Stream…"),
    LIVE_BUFFERING("Live • Rolling Buffer Active"),
    ERROR("Stream Error")
}

enum class LiveClipApprovalStatus {
    PENDING_REVIEW,
    APPROVED_SAVED,
    REJECTED
}

data class LiveDetectedClip(
    val id: String = UUID.randomUUID().toString(),
    val hookTitle: String,
    val momentCategory: String,
    val reason: String,
    val viralScore: Int,
    val durationMs: Long,
    val capturedAtMs: Long = System.currentTimeMillis(),
    val localClipPath: String?,
    val clipFileSizeBytes: Long,
    val smartCropMode: SmartCropMode,
    val hashtags: List<String>,
    val englishSubtitles: List<String>,
    val hindiSubtitles: List<String>,
    val teluguSubtitles: List<String>,
    val approvalStatus: LiveClipApprovalStatus = LiveClipApprovalStatus.PENDING_REVIEW,
    val savedProjectId: Long? = null
) {
    fun subtitlesFor(language: SubtitleLanguage): List<String> = when (language) {
        SubtitleLanguage.ENGLISH -> englishSubtitles
        SubtitleLanguage.HINDI -> hindiSubtitles
        SubtitleLanguage.TELUGU -> teluguSubtitles
    }

    fun toShortSuggestion(): ShortClipSuggestion = ShortClipSuggestion(
        id = id,
        rank = 1,
        hookTitle = hookTitle,
        reason = reason,
        viralScore = viralScore,
        startMs = 0L,
        endMs = durationMs.coerceAtLeast(3_000L),
        hashtags = hashtags,
        englishSubtitles = englishSubtitles,
        hindiSubtitles = hindiSubtitles,
        teluguSubtitles = teluguSubtitles
    )
}

data class LiveStreamShortsUiState(
    val streamUrlInput: String = "",
    val streamTitleInput: String = "",
    val detectedProtocol: LiveStreamProtocol = LiveStreamProtocol.UNKNOWN,
    val authorizationConfirmed: Boolean = false,
    val highlightPreset: LiveHighlightPreset = LiveHighlightPreset.GAMING,
    val rollingWindowSeconds: Int = 90,
    val autoDetectEnabled: Boolean = true,
    val subtitleLanguage: SubtitleLanguage = SubtitleLanguage.ENGLISH,
    val convertTo9x16: Boolean = true,
    val liveContextNote: String = "",
    val connectionStatus: LiveConnectionStatus = LiveConnectionStatus.DISCONNECTED,
    val connectedStreamUrl: String? = null,
    val connectionError: String? = null,
    val streamUptimeSeconds: Long = 0L,
    val bufferSnapshot: RollingBufferSnapshot = RollingBufferSnapshot(),
    val isAiScanning: Boolean = false,
    val lastAiScanSummary: String = "Connect a real authorized HLS (.m3u8) or RTMP stream to begin live highlight detection.",
    val aiScansCompleted: Int = 0,
    val detectedClips: List<LiveDetectedClip> = emptyList(),
    val exportingClipId: String? = null,
    val exportProgressPercent: Int = 0,
    val statusMessage: String? = null
) {
    val isLiveConnected: Boolean
        get() = connectionStatus == LiveConnectionStatus.LIVE_BUFFERING

    val pendingReviewCount: Int
        get() = detectedClips.count { it.approvalStatus == LiveClipApprovalStatus.PENDING_REVIEW }

    val approvedCount: Int
        get() = detectedClips.count { it.approvalStatus == LiveClipApprovalStatus.APPROVED_SAVED }
}

class LiveStreamShortsViewModel(application: Application) : AndroidViewModel(application) {

    private val bufferEngine = LiveRollingBufferEngine(application)
    private val aiService = LiveHighlightAiService()
    private val srtBuilder = YouTubeShortsAiService()
    private val exporter = ShortsMp4Exporter(application)
    private val dao = YodhaDatabase.getInstance(application).yodhaDao()

    private val _uiState = MutableStateFlow(LiveStreamShortsUiState())
    val uiState: StateFlow<LiveStreamShortsUiState> = _uiState.asStateFlow()

    private var hlsPollingJob: Job? = null
    private var uptimeTickerJob: Job? = null
    private var activeLiveProjectId: Long? = null

    fun consumeStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun showStatus(message: String) {
        _uiState.update { it.copy(statusMessage = message) }
    }

    fun onStreamUrlChange(url: String) {
        _uiState.update {
            it.copy(
                streamUrlInput = url,
                detectedProtocol = LiveRollingBufferEngine.detectProtocol(url),
                connectionError = null
            )
        }
    }

    fun onStreamTitleChange(title: String) {
        _uiState.update { it.copy(streamTitleInput = title) }
    }

    fun onAuthorizationConfirmedChange(confirmed: Boolean) {
        _uiState.update { it.copy(authorizationConfirmed = confirmed) }
    }

    fun onPresetChange(preset: LiveHighlightPreset) {
        _uiState.update { it.copy(highlightPreset = preset) }
    }

    fun onRollingWindowChange(seconds: Int) {
        _uiState.update {
            it.copy(
                rollingWindowSeconds = seconds,
                bufferSnapshot = it.bufferSnapshot.copy(maxWindowSeconds = seconds)
            )
        }
    }

    fun onAutoDetectChange(enabled: Boolean) {
        _uiState.update { it.copy(autoDetectEnabled = enabled) }
    }

    fun onSubtitleLanguageChange(language: SubtitleLanguage) {
        _uiState.update { it.copy(subtitleLanguage = language) }
    }

    fun onConvertTo9x16Change(enabled: Boolean) {
        _uiState.update { it.copy(convertTo9x16 = enabled) }
    }

    fun onLiveContextNoteChange(note: String) {
        _uiState.update { it.copy(liveContextNote = note) }
    }

    private fun isNetworkConnected(): Boolean {
        return try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    /**
     * Initiates a real connection to an authorized HLS (.m3u8) or RTMP (rtmp:// / rtmps://) live stream.
     * Never simulates a connection or creates synthetic clips.
     */
    fun connectAuthorizedStream() {
        val state = _uiState.value
        val url = state.streamUrlInput.trim()
        val protocol = LiveRollingBufferEngine.detectProtocol(url)

        if (url.isBlank() || protocol == LiveStreamProtocol.UNKNOWN) {
            _uiState.update {
                it.copy(
                    connectionStatus = LiveConnectionStatus.ERROR,
                    connectionError = "Enter a valid authorized HLS (.m3u8) or RTMP (rtmp:// / rtmps://) live stream URL."
                )
            }
            return
        }
        if (!state.authorizationConfirmed) {
            _uiState.update {
                it.copy(
                    connectionError = "Confirm that you own or have explicit broadcaster authorization for this live stream."
                )
            }
            return
        }
        if (!isNetworkConnected()) {
            _uiState.update {
                it.copy(
                    connectionStatus = LiveConnectionStatus.ERROR,
                    connectionError = "Device appears offline. Connect to Wi-Fi or mobile data before connecting to a live stream."
                )
            }
            return
        }

        disconnectStream(silent = true)
        activeLiveProjectId = null

        _uiState.update {
            it.copy(
                detectedProtocol = protocol,
                connectionStatus = LiveConnectionStatus.CONNECTING,
                connectedStreamUrl = url,
                connectionError = null,
                streamUptimeSeconds = 0L,
                lastAiScanSummary = "Handshaking with authorized ${protocol.label} live stream…"
            )
        }

        if (protocol == LiveStreamProtocol.HLS) {
            startHlsRollingBufferLoop(url)
        }
    }

    fun disconnectStream(silent: Boolean = false) {
        hlsPollingJob?.cancel()
        hlsPollingJob = null
        uptimeTickerJob?.cancel()
        uptimeTickerJob = null

        _uiState.update {
            it.copy(
                connectionStatus = LiveConnectionStatus.DISCONNECTED,
                connectedStreamUrl = null,
                isAiScanning = false,
                lastAiScanSummary = "Stream disconnected. Rolling buffer paused.",
                statusMessage = if (silent) it.statusMessage else "Disconnected from live stream."
            )
        }
    }

    fun onPlayerPlaybackLiveReady() {
        if (_uiState.value.connectedStreamUrl == null) return
        val wasLive = _uiState.value.connectionStatus == LiveConnectionStatus.LIVE_BUFFERING
        _uiState.update {
            it.copy(
                connectionStatus = LiveConnectionStatus.LIVE_BUFFERING,
                connectionError = null,
                lastAiScanSummary = if (!wasLive) {
                    "Connected to live ${it.detectedProtocol.label} stream! Rolling buffer & AI highlight monitoring active."
                } else {
                    it.lastAiScanSummary
                }
            )
        }
        if (uptimeTickerJob == null || uptimeTickerJob?.isActive != true) {
            startUptimeTicker()
        }
    }

    fun onPlayerPlaybackError(errorMessage: String) {
        if (_uiState.value.connectedStreamUrl == null) return
        hlsPollingJob?.cancel()
        uptimeTickerJob?.cancel()
        _uiState.update {
            it.copy(
                connectionStatus = LiveConnectionStatus.ERROR,
                connectionError = errorMessage,
                isAiScanning = false,
                lastAiScanSummary = "Live stream connection interrupted: $errorMessage"
            )
        }
    }

    private fun startUptimeTicker() {
        uptimeTickerJob?.cancel()
        uptimeTickerJob = viewModelScope.launch {
            while (isActive && _uiState.value.connectionStatus == LiveConnectionStatus.LIVE_BUFFERING) {
                delay(1_000L)
                _uiState.update { it.copy(streamUptimeSeconds = it.streamUptimeSeconds + 1L) }
            }
        }
    }

    private fun startHlsRollingBufferLoop(hlsUrl: String) {
        hlsPollingJob?.cancel()
        hlsPollingJob = viewModelScope.launch {
            while (isActive && _uiState.value.connectedStreamUrl == hlsUrl) {
                val windowSec = _uiState.value.rollingWindowSeconds
                val pollResult = bufferEngine.pollHlsSegmentsIntoRollingBuffer(hlsUrl, windowSec)
                pollResult.onSuccess { snapshot ->
                    _uiState.update {
                        it.copy(
                            bufferSnapshot = snapshot,
                            connectionStatus = LiveConnectionStatus.LIVE_BUFFERING,
                            connectionError = null
                        )
                    }
                    if (uptimeTickerJob == null || uptimeTickerJob?.isActive != true) {
                        startUptimeTicker()
                    }
                }.onFailure { err ->
                    Log.w(TAG, "HLS rolling buffer poll warning: ${err.message}")
                    if (_uiState.value.bufferSnapshot.segmentCount == 0 &&
                        _uiState.value.connectionStatus == LiveConnectionStatus.CONNECTING
                    ) {
                        _uiState.update {
                            it.copy(
                                lastAiScanSummary = "Waiting for live HLS media segments (${err.localizedMessage ?: "retrying"})…"
                            )
                        }
                    }
                }
                delay(4_000L)
            }
        }
    }

    /**
     * Evaluates a real frame captured from the currently active, connected live stream via
     * Firebase AI Logic (`gemini-3.8-flash`) to detect gaming highlights, reactions, or exciting moments.
     * Refuses to run when no real live stream is connected.
     */
    fun scanLiveMomentWithAi(
        capturedFrame: Bitmap?,
        forceManualCapture: Boolean
    ) {
        val state = _uiState.value
        if (!state.isLiveConnected) {
            capturedFrame?.recycle()
            _uiState.update {
                it.copy(
                    statusMessage = "Connect an active authorized HLS or RTMP live stream first before capturing highlights."
                )
            }
            return
        }
        if (state.isAiScanning) {
            capturedFrame?.recycle()
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAiScanning = true,
                    lastAiScanSummary = if (forceManualCapture) {
                        "Capturing live moment from rolling buffer & scoring with Gemini AI…"
                    } else {
                        "Gemini AI scanning live stream for ${state.highlightPreset.label}…"
                    }
                )
            }
            try {
                val title = state.streamTitleInput.trim().ifBlank { "Authorized Live Stream" }
                val detection = aiService.evaluateLiveMoment(
                    streamTitle = title,
                    preset = state.highlightPreset,
                    liveContextNote = state.liveContextNote,
                    capturedFrame = capturedFrame,
                    streamUptimeSeconds = state.streamUptimeSeconds,
                    forceManualCapture = forceManualCapture
                )

                if (detection.isHighlightDetected) {
                    val materialized = bufferEngine.materializeRollingClip(
                        clipTitle = detection.hookTitle,
                        targetClipSeconds = detection.recommendedClipSeconds
                    )
                    val clip = LiveDetectedClip(
                        hookTitle = detection.hookTitle,
                        momentCategory = detection.momentCategory,
                        reason = detection.reason,
                        viralScore = detection.viralScore,
                        durationMs = materialized?.second ?: (detection.recommendedClipSeconds * 1000L),
                        localClipPath = materialized?.first?.absolutePath,
                        clipFileSizeBytes = materialized?.first?.length() ?: 0L,
                        smartCropMode = detection.recommendedCropMode,
                        hashtags = detection.hashtags,
                        englishSubtitles = detection.englishSubtitles,
                        hindiSubtitles = detection.hindiSubtitles,
                        teluguSubtitles = detection.teluguSubtitles,
                        approvalStatus = LiveClipApprovalStatus.PENDING_REVIEW
                    )
                    _uiState.update { current ->
                        current.copy(
                            isAiScanning = false,
                            aiScansCompleted = current.aiScansCompleted + 1,
                            detectedClips = listOf(clip) + current.detectedClips,
                            lastAiScanSummary = "Highlight detected (${clip.viralScore}% Viral): ${clip.hookTitle}",
                            statusMessage = "New live highlight ready for your review: ${clip.hookTitle}"
                        )
                    }
                } else {
                    _uiState.update { current ->
                        current.copy(
                            isAiScanning = false,
                            aiScansCompleted = current.aiScansCompleted + 1,
                            lastAiScanSummary = "Scanned live feed — no climax yet (${detection.reason.take(90)})"
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Live highlight AI scan error", e)
                _uiState.update {
                    it.copy(
                        isAiScanning = false,
                        lastAiScanSummary = "AI scan error: ${e.localizedMessage ?: "retry shortly"}"
                    )
                }
            } finally {
                if (capturedFrame != null && !capturedFrame.isRecycled) {
                    capturedFrame.recycle()
                }
            }
        }
    }

    /**
     * Approves a reviewed live clip and saves it directly into Room My Projects and My Clips.
     */
    fun approveAndSaveClip(clip: LiveDetectedClip, onSavedToLibrary: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val streamTitle = state.streamTitleInput.trim().ifBlank { "Live Stream Session" }
                val videoUri = clip.localClipPath?.let { Uri.fromFile(File(it)).toString() }
                    ?: state.connectedStreamUrl.orEmpty()
                val aspect = if (state.convertTo9x16) AspectRatioOption.PORTRAIT_9_16.code else AspectRatioOption.LANDSCAPE_16_9.code

                val projectId = activeLiveProjectId ?: dao.insertProject(
                    VideoProjectEntity(
                        title = "$streamTitle (Live Clips)",
                        videoUri = videoUri,
                        fileName = clip.localClipPath?.let { File(it).name } ?: "live_stream.m3u8",
                        thumbnailPath = null,
                        durationMs = clip.durationMs,
                        width = 1080,
                        height = 1920,
                        fileSizeBytes = clip.clipFileSizeBytes,
                        aspectRatio = aspect,
                        captionStyle = CaptionStyleOption.NEON_GLOW.code,
                        exportQuality = "1080p"
                    )
                ).also { activeLiveProjectId = it }

                dao.insertClip(
                    VideoClipEntity(
                        projectId = projectId,
                        projectTitle = "$streamTitle (Live Clips)",
                        title = "${clip.hookTitle} [${clip.momentCategory}]",
                        videoUri = videoUri,
                        thumbnailPath = null,
                        startMs = 0L,
                        endMs = clip.durationMs,
                        aspectRatio = aspect,
                        captionStyle = CaptionStyleOption.NEON_GLOW.code
                    )
                )

                _uiState.update { current ->
                    current.copy(
                        detectedClips = current.detectedClips.map { item ->
                            if (item.id == clip.id) {
                                item.copy(
                                    approvalStatus = LiveClipApprovalStatus.APPROVED_SAVED,
                                    savedProjectId = projectId
                                )
                            } else {
                                item
                            }
                        },
                        statusMessage = "Approved & saved \"${clip.hookTitle}\" to My Projects & My Clips!"
                    )
                }
                onSavedToLibrary()
            } catch (e: Exception) {
                Log.e(TAG, "Approve clip failed", e)
                _uiState.update { it.copy(statusMessage = "Could not save live clip to library.") }
            }
        }
    }

    fun rejectClip(clipId: String) {
        _uiState.update { current ->
            current.copy(
                detectedClips = current.detectedClips.map { item ->
                    if (item.id == clipId) {
                        item.copy(approvalStatus = LiveClipApprovalStatus.REJECTED)
                    } else {
                        item
                    }
                },
                statusMessage = "Clip dismissed from review queue."
            )
        }
    }

    fun exportLiveClipToDownloads(clip: LiveDetectedClip, onDownloadRecorded: () -> Unit = {}) {
        val path = clip.localClipPath
        if (path == null || !File(path).exists()) {
            _uiState.update {
                it.copy(
                    statusMessage = "No buffered HLS media file attached to this clip. Connect an HLS (.m3u8) stream to materialize downloadable clip files."
                )
            }
            return
        }
        val state = _uiState.value
        if (state.exportingClipId != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(exportingClipId = clip.id, exportProgressPercent = 10) }
            try {
                val sourceFile = File(path)
                val exported = try {
                    exporter.exportShortMp4(
                        sourceVideoUri = Uri.fromFile(sourceFile).toString(),
                        clipTitle = clip.hookTitle,
                        startMs = 0L,
                        endMs = clip.durationMs,
                        convertTo9x16 = state.convertTo9x16,
                        smartCropMode = clip.smartCropMode,
                        onProgress = { pct ->
                            _uiState.update { it.copy(exportProgressPercent = pct) }
                        }
                    )
                } catch (_: Exception) {
                    exporter.exportLocalFileToDownloads(
                        sourceFile = sourceFile,
                        clipTitle = clip.hookTitle,
                        convertTo9x16 = false
                    )
                }

                dao.insertDownload(
                    ExportedDownloadEntity(
                        fileName = exported.fileName,
                        contentUri = exported.contentUri.toString(),
                        locationLabel = exported.locationLabel,
                        mimeType = "video/mp4",
                        sizeBytes = exported.sizeBytes,
                        sourceModule = "Live Stream to Shorts",
                        aspectRatio = if (exported.convertedTo9x16) "9:16" else "Live Buffer",
                        convertedTo9x16 = exported.convertedTo9x16
                    )
                )
                onDownloadRecorded()
                _uiState.update {
                    it.copy(
                        exportingClipId = null,
                        exportProgressPercent = 0,
                        statusMessage = "Exported live clip: ${exported.locationLabel}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        exportingClipId = null,
                        exportProgressPercent = 0,
                        statusMessage = "Live clip export failed: ${e.localizedMessage ?: "error"}"
                    )
                }
            }
        }
    }

    fun exportLiveSubtitlesSrt(clip: LiveDetectedClip, onDownloadRecorded: () -> Unit = {}) {
        val lang = _uiState.value.subtitleLanguage
        viewModelScope.launch {
            try {
                val srt = srtBuilder.buildSrtContent(clip.toShortSuggestion(), lang)
                val saved = exporter.exportSubtitleSrt(
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
                        sourceModule = "Live Stream (${lang.label} SRT)",
                        aspectRatio = "SRT",
                        convertedTo9x16 = false
                    )
                )
                onDownloadRecorded()
                _uiState.update { it.copy(statusMessage = "Saved ${lang.label} SRT: ${saved.locationLabel}") }
            } catch (_: Exception) {
                _uiState.update { it.copy(statusMessage = "Failed to export SRT subtitles.") }
            }
        }
    }

    fun clearBufferNow() {
        viewModelScope.launch {
            val cleared = bufferEngine.clearRollingBuffer()
            _uiState.update {
                it.copy(
                    bufferSnapshot = bufferEngine.currentSnapshot(it.rollingWindowSeconds),
                    statusMessage = "Cleared ${Formatters.formatFileSize(cleared)} from live rolling buffer."
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        hlsPollingJob?.cancel()
        uptimeTickerJob?.cancel()
    }
}
