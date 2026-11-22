package com.example.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

object Formatters {

    fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs.coerceAtLeast(0L) / 1000L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            else -> "${kb.roundToLong().coerceAtLeast(1L)} KB"
        }
    }

    fun formatDate(timestampMs: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }

    fun formatResolution(width: Int, height: Int): String {
        if (width <= 0 || height <= 0) return "HD"
        val shortEdge = minOf(width, height)
        val label = when {
            shortEdge >= 2160 -> "4K"
            shortEdge >= 1080 -> "1080p"
            shortEdge >= 720 -> "720p"
            else -> "${shortEdge}p"
        }
        return "${width}×${height} ($label)"
    }
}
