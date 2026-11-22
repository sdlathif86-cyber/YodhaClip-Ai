package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

data class ExtractedMediaPayload(
    val audioBytes: ByteArray?,
    val audioMimeType: String?,
    val audioExtractedDurationMs: Long,
    val keyframes: List<Bitmap>,
    val extractionSummary: String
)

class VideoAudioExtractor(private val context: Context) {

    companion object {
        // Keep well below Gemini's 20MB inlineData payload limit
        private const val MAX_AUDIO_BYTES = 14 * 1024 * 1024
        private const val MAX_KEYFRAMES = 6
    }

    suspend fun extractForAiAnalysis(
        videoUri: Uri,
        videoDurationMs: Long,
        onProgress: (percent: Int, stepLabel: String) -> Unit
    ): ExtractedMediaPayload = withContext(Dispatchers.IO) {
        onProgress(15, "Extracting audio track from MP4…")
        val audioResult = extractAudioTrackBytes(videoUri)

        onProgress(55, "Sampling video keyframes for visual context…")
        val frames = extractKeyframes(videoUri, videoDurationMs)

        onProgress(90, "Preparing multimodal AI payload…")
        val summary = buildString {
            if (audioResult != null) {
                val kb = (audioResult.first.size / 1024).coerceAtLeast(1)
                append("Extracted audio track (${kb} KB, ${audioResult.third / 1000L}s)")
            } else {
                append("No decodable AAC/MP4 audio track found")
            }
            if (frames.isNotEmpty()) {
                append(" + ${frames.size} video keyframes")
            }
        }
        onProgress(100, summary)

        ExtractedMediaPayload(
            audioBytes = audioResult?.first,
            audioMimeType = audioResult?.second,
            audioExtractedDurationMs = audioResult?.third ?: 0L,
            keyframes = frames,
            extractionSummary = summary
        )
    }

    private fun extractAudioTrackBytes(videoUri: Uri): Triple<ByteArray, String, Long>? {
        val tempFile = File(context.cacheDir, "yodha_audio_${System.currentTimeMillis()}.m4a")
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(context, videoUri, null)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }
            if (audioTrackIndex < 0 || audioFormat == null) return null

            extractor.selectTrack(audioTrackIndex)
            muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val dstTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val maxInput = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(65_536)
            } else {
                262_144
            }
            val buffer = ByteBuffer.allocate(maxInput)
            val info = MediaCodec.BufferInfo()
            var totalBytesWritten = 0L
            var lastTimeUs = 0L

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                if (totalBytesWritten + sampleSize > MAX_AUDIO_BYTES) break

                val sampleTimeUs = extractor.sampleTime.coerceAtLeast(0L)
                info.offset = 0
                info.size = sampleSize
                info.presentationTimeUs = sampleTimeUs
                info.flags = extractor.sampleFlags
                muxer.writeSampleData(dstTrack, buffer, info)

                totalBytesWritten += sampleSize
                lastTimeUs = sampleTimeUs
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null

            if (!tempFile.exists() || tempFile.length() < 256L || tempFile.length() > MAX_AUDIO_BYTES + 512_000L) {
                return null
            }
            val bytes = tempFile.readBytes()
            return Triple(bytes, "audio/mp4", (lastTimeUs / 1000L).coerceAtLeast(1000L))
        } catch (_: Exception) {
            return null
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
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private fun extractKeyframes(videoUri: Uri, durationMs: Long): List<Bitmap> {
        val frames = mutableListOf<Bitmap>()
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val safeDuration = durationMs.coerceAtLeast(3_000L)
            val count = MAX_KEYFRAMES
            val stepMs = safeDuration / (count + 1)
            for (i in 1..count) {
                val timeUs = (i * stepMs) * 1000L
                val raw = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (raw != null) {
                    val scaled = scaleDown(raw, 448)
                    if (scaled !== raw) raw.recycle()
                    frames.add(scaled)
                }
            }
        } catch (_: Exception) {
            // Return whatever frames were extracted
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
        return frames
    }

    private fun scaleDown(source: Bitmap, maxEdge: Int): Bitmap {
        val scale = minOf(
            maxEdge.toFloat() / source.width.coerceAtLeast(1),
            maxEdge.toFloat() / source.height.coerceAtLeast(1),
            1f
        )
        return if (scale < 1f) {
            Bitmap.createScaledBitmap(
                source,
                (source.width * scale).toInt().coerceAtLeast(1),
                (source.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            source
        }
    }
}
