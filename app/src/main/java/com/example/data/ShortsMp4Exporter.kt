package com.example.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "ShortsMp4Exporter"

data class ExportedShortResult(
    val fileName: String,
    val contentUri: Uri,
    val locationLabel: String,
    val sizeBytes: Long,
    val convertedTo9x16: Boolean
)

class ShortsMp4Exporter(private val context: Context) {

    fun detectVideoEngineLabel(): String {
        val ffmpegPaths = listOf("/system/bin/ffmpeg", "/vendor/bin/ffmpeg")
        val hasSystemFfmpeg = ffmpegPaths.any { File(it).canExecute() }
        return if (hasSystemFfmpeg) {
            "Jetpack Media3 Transformer + System FFmpeg"
        } else {
            "Jetpack Media3 Transformer (Open-Source Hardware Pipeline)"
        }
    }

    @OptIn(UnstableApi::class)
    suspend fun exportShortMp4(
        sourceVideoUri: String,
        clipTitle: String,
        startMs: Long,
        endMs: Long,
        convertTo9x16: Boolean,
        smartCropMode: SmartCropMode = SmartCropMode.CENTER,
        exportQuality: ExportQualityOption = ExportQualityOption.FHD_1080,
        backgroundMusicUri: String? = null,
        onProgress: (Int) -> Unit
    ): ExportedShortResult {
        val safeName = sanitizeFileName(clipTitle)
        val suffix = if (convertTo9x16) "_9x16_${exportQuality.code}" else "_${exportQuality.code}"
        val fileName = "${safeName}${suffix}_${System.currentTimeMillis() % 10000}.mp4"
        val tempDir = File(context.cacheDir, "yodha_exports").apply {
            if (!exists()) mkdirs()
        }
        val tempFile = File(tempDir, fileName)
        if (tempFile.exists()) tempFile.delete()

        var converted9x16 = false
        try {
            exportWithMedia3Transformer(
                sourceVideoUri = sourceVideoUri,
                outputPath = tempFile.absolutePath,
                startMs = startMs,
                endMs = endMs,
                convertTo9x16 = convertTo9x16,
                smartCropMode = smartCropMode,
                exportQuality = exportQuality,
                backgroundMusicUri = backgroundMusicUri,
                onProgress = onProgress
            )
            converted9x16 = convertTo9x16
        } catch (e: Exception) {
            Log.w(TAG, "Media3 Transformer pass failed (${e.message}); retrying or falling back", e)
            if (tempFile.exists()) tempFile.delete()
            var secondPassSucceeded = false
            if (!backgroundMusicUri.isNullOrBlank()) {
                try {
                    exportWithMedia3Transformer(
                        sourceVideoUri = sourceVideoUri,
                        outputPath = tempFile.absolutePath,
                        startMs = startMs,
                        endMs = endMs,
                        convertTo9x16 = convertTo9x16,
                        smartCropMode = smartCropMode,
                        exportQuality = exportQuality,
                        backgroundMusicUri = null,
                        onProgress = onProgress
                    )
                    converted9x16 = convertTo9x16
                    secondPassSucceeded = true
                } catch (_: Exception) {
                    if (tempFile.exists()) tempFile.delete()
                }
            }
            if (!secondPassSucceeded) {
                onProgress(35)
                exportWithNativeMuxer(
                    sourceUri = Uri.parse(sourceVideoUri),
                    outputPath = tempFile.absolutePath,
                    startMs = startMs,
                    endMs = endMs
                )
                onProgress(95)
                converted9x16 = false
            }
        }

        if (!tempFile.exists() || tempFile.length() == 0L) {
            throw IllegalStateException("Failed to write exported MP4 file.")
        }

        val saved = saveFileToDownloads(
            sourceFile = tempFile,
            displayName = fileName,
            mimeType = "video/mp4"
        )
        tempFile.delete()
        onProgress(100)
        return saved.copy(convertedTo9x16 = converted9x16)
    }

    suspend fun exportLocalFileToDownloads(
        sourceFile: File,
        clipTitle: String,
        convertTo9x16: Boolean = false
    ): ExportedShortResult {
        val safeName = sanitizeFileName(clipTitle)
        val ext = sourceFile.extension.ifBlank { "mp4" }
        val fileName = "${safeName}_live_${System.currentTimeMillis() % 10000}.$ext"
        val mime = if (ext.equals("ts", ignoreCase = true)) "video/mp2t" else "video/mp4"
        val saved = saveFileToDownloads(
            sourceFile = sourceFile,
            displayName = fileName,
            mimeType = mime
        )
        return saved.copy(convertedTo9x16 = convertTo9x16)
    }

    suspend fun exportSubtitleSrt(
        clipTitle: String,
        languageCode: String,
        srtContent: String
    ): ExportedShortResult = withContext(Dispatchers.IO) {
        val safeName = sanitizeFileName(clipTitle)
        val fileName = "${safeName}_${languageCode}.srt"
        val tempFile = File(context.cacheDir, fileName)
        tempFile.writeText(srtContent, Charsets.UTF_8)
        val saved = saveFileToDownloads(
            sourceFile = tempFile,
            displayName = fileName,
            mimeType = "application/x-subrip"
        )
        tempFile.delete()
        saved
    }

    @OptIn(UnstableApi::class)
    private suspend fun exportWithMedia3Transformer(
        sourceVideoUri: String,
        outputPath: String,
        startMs: Long,
        endMs: Long,
        convertTo9x16: Boolean,
        smartCropMode: SmartCropMode,
        exportQuality: ExportQualityOption,
        backgroundMusicUri: String?,
        onProgress: (Int) -> Unit
    ) = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val safeStart = startMs.coerceAtLeast(0L)
            val safeEnd = endMs.coerceAtLeast(safeStart + 1000L)
            val clipDurationMs = (safeEnd - safeStart).coerceAtLeast(1000L)

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(sourceVideoUri))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(safeStart)
                        .setEndPositionMs(safeEnd)
                        .build()
                )
                .build()

            val videoEffects = mutableListOf<Effect>()
            if (convertTo9x16) {
                when (smartCropMode) {
                    SmartCropMode.CENTER -> Unit
                    SmartCropMode.SPEAKER_TOP -> videoEffects.add(Crop(-0.85f, 0.85f, -0.55f, 1.0f))
                    SmartCropMode.ACTION_LEFT -> videoEffects.add(Crop(-1.0f, 0.45f, -1.0f, 1.0f))
                    SmartCropMode.ACTION_RIGHT -> videoEffects.add(Crop(-0.45f, 1.0f, -1.0f, 1.0f))
                }
                val (targetW, targetH) = when (exportQuality) {
                    ExportQualityOption.HD_720 -> 720 to 1280
                    ExportQualityOption.FHD_1080 -> 1080 to 1920
                    ExportQualityOption.UHD_4K -> 1080 to 1920
                }
                videoEffects.add(
                    Presentation.createForWidthAndHeight(
                        targetW,
                        targetH,
                        Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
                    )
                )
            } else if (exportQuality == ExportQualityOption.HD_720) {
                videoEffects.add(Presentation.createForHeight(720))
            }

            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setEffects(Effects(emptyList(), videoEffects))
                .build()

            val handler = Handler(Looper.getMainLooper())
            var progressRunnable: Runnable? = null

            val transformer = Transformer.Builder(context)
                .addListener(
                    object : Transformer.Listener {
                        override fun onCompleted(
                            composition: Composition,
                            exportResult: ExportResult
                        ) {
                            progressRunnable?.let { handler.removeCallbacks(it) }
                            if (continuation.isActive) {
                                continuation.resume(Unit)
                            }
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            progressRunnable?.let { handler.removeCallbacks(it) }
                            if (continuation.isActive) {
                                continuation.resumeWithException(exportException)
                            }
                        }
                    }
                )
                .build()

            val holder = ProgressHolder()
            progressRunnable = object : Runnable {
                override fun run() {
                    val state = transformer.getProgress(holder)
                    if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                        onProgress(holder.progress.coerceIn(0, 95))
                    }
                    if (state != Transformer.PROGRESS_STATE_NOT_STARTED && continuation.isActive) {
                        handler.postDelayed(this, 250L)
                    }
                }
            }

            continuation.invokeOnCancellation {
                progressRunnable?.let { handler.removeCallbacks(it) }
                handler.post {
                    try {
                        transformer.cancel()
                    } catch (_: Exception) {
                    }
                }
            }

            if (!backgroundMusicUri.isNullOrBlank()) {
                val bgAudioItem = MediaItem.Builder()
                    .setUri(Uri.parse(backgroundMusicUri))
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(0L)
                            .setEndPositionMs(clipDurationMs)
                            .build()
                    )
                    .build()
                val editedBgAudio = EditedMediaItem.Builder(bgAudioItem)
                    .setRemoveVideo(true)
                    .build()
                val composition = Composition.Builder(
                    listOf(
                        EditedMediaItemSequence.Builder(listOf(editedMediaItem)).build(),
                        EditedMediaItemSequence.Builder(listOf(editedBgAudio)).build()
                    )
                ).build()
                transformer.start(composition, outputPath)
            } else {
                transformer.start(editedMediaItem, outputPath)
            }
            handler.post(progressRunnable)
        }
    }

    private suspend fun exportWithNativeMuxer(
        sourceUri: Uri,
        outputPath: String,
        startMs: Long,
        endMs: Long
    ) = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(context, sourceUri, null)
            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackIndexMap = HashMap<Int, Int>()
            var maxBufferSize = 1024 * 1024

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val dstTrack = muxer.addTrack(format)
                    trackIndexMap[i] = dstTrack
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        val size = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                        if (size > maxBufferSize) maxBufferSize = size
                    }
                }
            }

            if (trackIndexMap.isEmpty()) {
                throw IllegalStateException("Selected file does not contain playable MP4 tracks.")
            }

            muxer.start()

            val startUs = startMs.coerceAtLeast(0L) * 1000L
            val endUs = endMs.coerceAtLeast(startMs + 1000L) * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()
            var baseTimeUs = -1L

            while (true) {
                val sampleTrack = extractor.sampleTrackIndex
                if (sampleTrack < 0) break

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endUs) break

                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val dstTrack = trackIndexMap[sampleTrack]
                if (dstTrack != null && sampleTimeUs >= startUs - 500_000L) {
                    if (baseTimeUs < 0L) {
                        baseTimeUs = sampleTimeUs
                    }
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = (sampleTimeUs - baseTimeUs).coerceAtLeast(0L)
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(dstTrack, buffer, bufferInfo)
                }
                extractor.advance()
            }
        } finally {
            try {
                muxer?.stop()
            } catch (_: Exception) {
            }
            try {
                muxer?.release()
            } catch (_: Exception) {
            }
            try {
                extractor.release()
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun saveFileToDownloads(
        sourceFile: File,
        displayName: String,
        mimeType: String
    ): ExportedShortResult = withContext(Dispatchers.IO) {
        val fileSize = sourceFile.length()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/YodhaClipAI"
                )
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Unable to create file in Downloads/YodhaClipAI.")

            try {
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                } ?: throw IllegalStateException("Could not open Downloads output stream.")

                val doneValues = ContentValues().apply {
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }
                resolver.update(uri, doneValues, null, null)

                ExportedShortResult(
                    fileName = displayName,
                    contentUri = uri,
                    locationLabel = "Downloads/YodhaClipAI/$displayName",
                    sizeBytes = fileSize,
                    convertedTo9x16 = true
                )
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            val outDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "YodhaClipAI").apply {
                if (!exists()) mkdirs()
            }
            val dest = File(outDir, displayName)
            sourceFile.copyTo(dest, overwrite = true)
            ExportedShortResult(
                fileName = displayName,
                contentUri = Uri.fromFile(dest),
                locationLabel = "App Movies/YodhaClipAI/$displayName",
                sizeBytes = dest.length(),
                convertedTo9x16 = true
            )
        }
    }

    private fun sanitizeFileName(raw: String): String {
        return raw.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(36)
            .ifBlank { "yodhaclip_short" }
    }
}
