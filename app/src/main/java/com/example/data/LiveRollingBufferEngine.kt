package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.concurrent.TimeUnit

enum class LiveStreamProtocol(val label: String) {
    HLS("HLS (.m3u8)"),
    RTMP("RTMP / RTMPS"),
    UNKNOWN("Unsupported URL")
}

data class HlsPlaylistSegment(
    val uri: String,
    val durationSec: Double
)

data class BufferedSegmentRecord(
    val segmentUrl: String,
    val localFile: File,
    val durationMs: Long,
    val capturedAtMs: Long,
    val sizeBytes: Long
)

data class RollingBufferSnapshot(
    val segmentCount: Int = 0,
    val totalDurationMs: Long = 0L,
    val totalSizeBytes: Long = 0L,
    val maxWindowSeconds: Int = 90
)

class LiveRollingBufferEngine(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val bufferDir: File
        get() = File(context.filesDir, "yodha_live_buffer").apply {
            if (!exists()) mkdirs()
        }

    private val approvedClipsDir: File
        get() = File(context.filesDir, "yodha_live_clips").apply {
            if (!exists()) mkdirs()
        }

    private val segments = mutableListOf<BufferedSegmentRecord>()
    private val seenSegmentUrls = LinkedHashSet<String>()

    companion object {
        fun detectProtocol(rawUrl: String): LiveStreamProtocol {
            val trimmed = rawUrl.trim().lowercase()
            return when {
                trimmed.startsWith("rtmp://") || trimmed.startsWith("rtmps://") -> LiveStreamProtocol.RTMP
                (trimmed.startsWith("https://") || trimmed.startsWith("http://")) &&
                    (trimmed.contains(".m3u8") || trimmed.contains("hls") || trimmed.contains("playlist")) ->
                    LiveStreamProtocol.HLS
                else -> LiveStreamProtocol.UNKNOWN
            }
        }

        fun parseHlsPlaylist(playlistUrl: String, body: String): Pair<String?, List<HlsPlaylistSegment>> {
            val lines = body.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val segments = mutableListOf<HlsPlaylistSegment>()
            var pendingDurationSec: Double? = null
            var variantPlaylistUri: String? = null
            var expectingVariant = false

            for (line in lines) {
                when {
                    line.startsWith("#EXT-X-STREAM-INF") -> {
                        expectingVariant = true
                    }
                    expectingVariant && !line.startsWith("#") -> {
                        if (variantPlaylistUri == null) {
                            variantPlaylistUri = resolveRelativeUrl(playlistUrl, line)
                        }
                        expectingVariant = false
                    }
                    line.startsWith("#EXTINF:") -> {
                        val rawDur = line.removePrefix("#EXTINF:")
                            .substringBefore(',')
                            .trim()
                        pendingDurationSec = rawDur.toDoubleOrNull() ?: 4.0
                    }
                    !line.startsWith("#") && pendingDurationSec != null -> {
                        segments.add(
                            HlsPlaylistSegment(
                                uri = resolveRelativeUrl(playlistUrl, line),
                                durationSec = pendingDurationSec!!
                            )
                        )
                        pendingDurationSec = null
                    }
                }
            }
            return variantPlaylistUri to segments
        }

        fun resolveRelativeUrl(baseUrl: String, relative: String): String {
            return try {
                if (relative.startsWith("http://") || relative.startsWith("https://")) {
                    relative
                } else {
                    URI(baseUrl).resolve(relative).toString()
                }
            } catch (_: Exception) {
                relative
            }
        }
    }

    @Synchronized
    fun currentSnapshot(maxWindowSeconds: Int): RollingBufferSnapshot {
        val totalDur = segments.sumOf { it.durationMs }
        val totalBytes = segments.sumOf { it.sizeBytes }
        return RollingBufferSnapshot(
            segmentCount = segments.size,
            totalDurationMs = totalDur,
            totalSizeBytes = totalBytes,
            maxWindowSeconds = maxWindowSeconds
        )
    }

    /**
     * Fetches the latest live HLS playlist and downloads new real segments into the
     * rolling disk buffer while pruning expired segments beyond [maxWindowSeconds].
     */
    suspend fun pollHlsSegmentsIntoRollingBuffer(
        hlsUrl: String,
        maxWindowSeconds: Int
    ): Result<RollingBufferSnapshot> = withContext(Dispatchers.IO) {
        try {
            val initialBody = fetchText(hlsUrl)
                ?: return@withContext Result.failure(
                    IllegalStateException("Unable to read HLS playlist from stream URL.")
                )
            val (variantUrl, directSegments) = parseHlsPlaylist(hlsUrl, initialBody)
            val mediaSegments = if (directSegments.isNotEmpty()) {
                directSegments
            } else if (variantUrl != null) {
                val variantBody = fetchText(variantUrl)
                    ?: return@withContext Result.failure(
                        IllegalStateException("Unable to read HLS media variant playlist.")
                    )
                parseHlsPlaylist(variantUrl, variantBody).second
            } else {
                emptyList()
            }

            if (mediaSegments.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("HLS playlist returned no live media segments yet.")
                )
            }

            // Take the latest 4 segments per poll to stay live and avoid flooding storage
            val tailSegments = mediaSegments.takeLast(4)
            for (seg in tailSegments) {
                if (seenSegmentUrls.contains(seg.uri)) continue
                val bytes = fetchBytes(seg.uri) ?: continue
                if (bytes.isEmpty()) continue

                val ext = when {
                    seg.uri.lowercase().contains(".m4s") -> "m4s"
                    seg.uri.lowercase().contains(".mp4") -> "mp4"
                    else -> "ts"
                }
                val file = File(bufferDir, "seg_${System.currentTimeMillis()}_${segments.size}.$ext")
                file.writeBytes(bytes)

                synchronized(this@LiveRollingBufferEngine) {
                    seenSegmentUrls.add(seg.uri)
                    segments.add(
                        BufferedSegmentRecord(
                            segmentUrl = seg.uri,
                            localFile = file,
                            durationMs = (seg.durationSec * 1000.0).toLong().coerceAtLeast(1000L),
                            capturedAtMs = System.currentTimeMillis(),
                            sizeBytes = file.length()
                        )
                    )
                    pruneRollingWindowLocked(maxWindowSeconds)
                }
            }
            Result.success(currentSnapshot(maxWindowSeconds))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Packages the most recent [targetClipSeconds] of real buffered HLS segments into a
     * standalone local clip file for preview, review, approval, and MP4 export.
     */
    suspend fun materializeRollingClip(
        clipTitle: String,
        targetClipSeconds: Int
    ): Pair<File, Long>? = withContext(Dispatchers.IO) {
        val selected = synchronized(this@LiveRollingBufferEngine) {
            if (segments.isEmpty()) return@withContext null
            val targetMs = (targetClipSeconds * 1000L).coerceIn(10_000L, 60_000L)
            val chosen = mutableListOf<BufferedSegmentRecord>()
            var acc = 0L
            for (seg in segments.asReversed()) {
                if (!seg.localFile.exists()) continue
                chosen.add(0, seg)
                acc += seg.durationMs
                if (acc >= targetMs) break
            }
            chosen to acc
        }
        val (chosenSegments, totalDurationMs) = selected
        if (chosenSegments.isEmpty()) return@withContext null

        val safeBase = clipTitle.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(32)
            .ifBlank { "live_highlight" }
        val ext = chosenSegments.last().localFile.extension.ifBlank { "ts" }
        val outFile = File(approvedClipsDir, "${safeBase}_${System.currentTimeMillis()}.$ext")

        FileOutputStream(outFile).use { output ->
            chosenSegments.forEach { seg ->
                if (seg.localFile.exists()) {
                    seg.localFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
            }
        }
        if (!outFile.exists() || outFile.length() == 0L) return@withContext null
        outFile to totalDurationMs.coerceAtLeast(3_000L)
    }

    @Synchronized
    fun clearRollingBuffer(): Long {
        var cleared = 0L
        segments.forEach { record ->
            if (record.localFile.exists()) {
                cleared += record.localFile.length()
                record.localFile.delete()
            }
        }
        segments.clear()
        seenSegmentUrls.clear()
        bufferDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                cleared += file.length()
                file.delete()
            }
        }
        return cleared
    }

    private fun pruneRollingWindowLocked(maxWindowSeconds: Int) {
        val maxWindowMs = maxWindowSeconds.coerceIn(30, 300) * 1000L
        var totalMs = segments.sumOf { it.durationMs }
        while (segments.size > 1 && totalMs > maxWindowMs) {
            val oldest = segments.removeAt(0)
            totalMs -= oldest.durationMs
            if (oldest.localFile.exists()) {
                oldest.localFile.delete()
            }
        }
        while (seenSegmentUrls.size > 200) {
            val first = seenSegmentUrls.firstOrNull() ?: break
            seenSegmentUrls.remove(first)
        }
    }

    private fun fetchText(url: String): String? {
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return null
            return res.body?.string()
        }
    }

    private fun fetchBytes(url: String): ByteArray? {
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return null
            return res.body?.bytes()
        }
    }
}
