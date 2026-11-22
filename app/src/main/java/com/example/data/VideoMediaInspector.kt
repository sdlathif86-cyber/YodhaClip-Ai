package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class InspectedVideoInfo(
    val uri: String,
    val fileName: String,
    val suggestedTitle: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
    val thumbnailPath: String?,
    val formatLabel: String = "MP4"
)

class VideoMediaInspector(private val context: Context) {

    companion object {
        val SUPPORTED_VIDEO_MIME_TYPES: Array<String> = arrayOf(
            "video/mp4",
            "video/quicktime",
            "video/webm",
            "video/*"
        )

        fun detectFormatLabel(fileName: String, mimeType: String? = null): String {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return when {
                ext == "mp4" || ext == "m4v" -> "MP4"
                ext == "mov" || ext == "qt" -> "MOV"
                ext == "webm" -> "WEBM"
                ext == "mkv" -> "MKV"
                mimeType?.contains("quicktime", ignoreCase = true) == true -> "MOV"
                mimeType?.contains("webm", ignoreCase = true) == true -> "WEBM"
                mimeType?.contains("mp4", ignoreCase = true) == true -> "MP4"
                ext.isNotBlank() && ext.length <= 5 -> ext.uppercase()
                else -> "MP4"
            }
        }
    }

    private val thumbDir: File
        get() = File(context.filesDir, "yodha_thumbs").apply {
            if (!exists()) mkdirs()
        }

    suspend fun inspectVideo(uri: Uri, takePersistableReadPermission: Boolean): InspectedVideoInfo =
        withContext(Dispatchers.IO) {
            if (takePersistableReadPermission) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // Picker URIs from Photo Picker may not support persistable grants
                } catch (_: IllegalArgumentException) {
                    // Ignore non-document URIs
                }
            }

            var displayName = "video_clip.mp4"
            var fileSize = 0L

            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0) {
                            cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let {
                                displayName = it
                            }
                        }
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                            fileSize = cursor.getLong(sizeIdx)
                        }
                    }
                }
            } catch (_: Exception) {
                // Fallback to default file metadata
            }

            var durationMs = 0L
            var width = 1080
            var height = 1920
            var thumbPath: String? = null

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                durationMs = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0L

                val rawWidth = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                )?.toIntOrNull() ?: 1920
                val rawHeight = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                )?.toIntOrNull() ?: 1080
                val rotation = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
                )?.toIntOrNull() ?: 0

                if (rotation == 90 || rotation == 270) {
                    width = rawHeight
                    height = rawWidth
                } else {
                    width = rawWidth
                    height = rawHeight
                }

                val frameTimeUs = if (durationMs > 2000L) 1_000_000L else (durationMs * 500L)
                val bitmap = retriever.getFrameAtTime(
                    frameTimeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                if (bitmap != null) {
                    thumbPath = saveScaledThumbnail(bitmap)
                    bitmap.recycle()
                }
            } catch (_: Exception) {
                // Keep safe fallback metadata
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }

            val cleanTitle = displayName
                .substringBeforeLast('.')
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()
                .ifBlank { "Untitled Studio Video" }

            val mimeType = try {
                context.contentResolver.getType(uri)
            } catch (_: Exception) {
                null
            }
            val detectedFormat = detectFormatLabel(displayName, mimeType)

            InspectedVideoInfo(
                uri = uri.toString(),
                fileName = displayName,
                suggestedTitle = cleanTitle,
                durationMs = durationMs.coerceAtLeast(1000L),
                width = width,
                height = height,
                fileSizeBytes = fileSize,
                thumbnailPath = thumbPath,
                formatLabel = detectedFormat
            )
        }

    suspend fun extractFrameThumbnail(videoUri: String, timeMs: Long): String? =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(videoUri))
                val bitmap = retriever.getFrameAtTime(
                    timeMs.coerceAtLeast(0L) * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                if (bitmap != null) {
                    val saved = saveScaledThumbnail(bitmap)
                    bitmap.recycle()
                    saved
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        }

    private fun saveScaledThumbnail(source: Bitmap): String? {
        return try {
            val maxEdge = 480
            val scale = minOf(
                maxEdge.toFloat() / source.width.coerceAtLeast(1),
                maxEdge.toFloat() / source.height.coerceAtLeast(1),
                1f
            )
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    source,
                    (source.width * scale).toInt().coerceAtLeast(1),
                    (source.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                source
            }

            val outFile = File(thumbDir, "thumb_${UUID.randomUUID()}.jpg")
            FileOutputStream(outFile).use { stream ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 82, stream)
            }
            if (scaled !== source) {
                scaled.recycle()
            }
            outFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        thumbDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    suspend fun clearThumbnailCache(): Long = withContext(Dispatchers.IO) {
        var cleared = 0L
        thumbDir.listFiles()?.forEach { file ->
            cleared += file.length()
            file.delete()
        }
        cleared
    }
}
