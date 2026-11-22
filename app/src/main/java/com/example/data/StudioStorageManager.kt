package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class StudioStorageBreakdown(
    val thumbnailCacheBytes: Long = 0L,
    val liveBufferBytes: Long = 0L,
    val exportTempBytes: Long = 0L,
    val recordedDownloadsBytes: Long = 0L,
    val liveSegmentCount: Int = 0
) {
    val totalManagedCacheBytes: Long
        get() = thumbnailCacheBytes + liveBufferBytes + exportTempBytes
}

class StudioStorageManager(private val context: Context) {

    private val thumbDir: File
        get() = File(context.filesDir, "yodha_thumbs")

    private val liveBufferDir: File
        get() = File(context.filesDir, "yodha_live_buffer")

    private val liveApprovedDir: File
        get() = File(context.filesDir, "yodha_live_clips")

    private val exportTempDir: File
        get() = File(context.cacheDir, "yodha_exports")

    suspend fun inspectStorage(recordedDownloadsBytes: Long): StudioStorageBreakdown =
        withContext(Dispatchers.IO) {
            val thumbBytes = folderSize(thumbDir)
            val bufferFiles = liveBufferDir.listFiles()?.filter { it.isFile }.orEmpty()
            val liveBytes = bufferFiles.sumOf { it.length() } + folderSize(liveApprovedDir)
            val tempBytes = folderSize(exportTempDir)
            StudioStorageBreakdown(
                thumbnailCacheBytes = thumbBytes,
                liveBufferBytes = liveBytes,
                exportTempBytes = tempBytes,
                recordedDownloadsBytes = recordedDownloadsBytes,
                liveSegmentCount = bufferFiles.size
            )
        }

    suspend fun clearLiveBuffer(): Long = withContext(Dispatchers.IO) {
        clearFolder(liveBufferDir)
    }

    suspend fun clearExportTemp(): Long = withContext(Dispatchers.IO) {
        clearFolder(exportTempDir)
    }

    suspend fun clearAllStudioCaches(): Long = withContext(Dispatchers.IO) {
        clearFolder(thumbDir) + clearFolder(liveBufferDir) + clearFolder(exportTempDir)
    }

    private fun folderSize(dir: File): Long {
        if (!dir.exists() || !dir.isDirectory) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun clearFolder(dir: File): Long {
        if (!dir.exists() || !dir.isDirectory) return 0L
        var cleared = 0L
        dir.listFiles()?.forEach { child ->
            if (child.isFile) {
                cleared += child.length()
                child.delete()
            } else if (child.isDirectory) {
                cleared += clearFolder(child)
                child.delete()
            }
        }
        return cleared
    }
}
