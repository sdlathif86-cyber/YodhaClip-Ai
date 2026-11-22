package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AspectRatioOption(
    val code: String,
    val label: String,
    val subtitle: String,
    val ratio: Float
) {
    PORTRAIT_9_16("9:16", "9:16", "Reels / Shorts", 9f / 16f),
    SQUARE_1_1("1:1", "1:1", "Square Feed", 1f),
    LANDSCAPE_16_9("16:9", "16:9", "YouTube HD", 16f / 9f);

    companion object {
        fun fromCode(code: String): AspectRatioOption =
            entries.firstOrNull { it.code == code } ?: PORTRAIT_9_16
    }
}

enum class CaptionStyleOption(
    val code: String,
    val label: String,
    val sampleText: String
) {
    BOLD_VIOLET("BOLD_VIOLET", "Bold Violet", "MAKE EVERY SECOND COUNT"),
    NEON_GLOW("NEON_GLOW", "Neon Glow", "VIRAL HOOK DETECTED"),
    MINIMAL_CLEAN("MINIMAL_CLEAN", "Minimal Clean", "Clean studio subtitles"),
    NONE("NONE", "Off", "");

    companion object {
        fun fromCode(code: String): CaptionStyleOption =
            entries.firstOrNull { it.code == code } ?: BOLD_VIOLET
    }
}

enum class ExportQualityOption(
    val code: String,
    val label: String
) {
    HD_720("720p", "720p HD"),
    FHD_1080("1080p", "1080p Full HD"),
    UHD_4K("4K", "4K Ultra HD");

    companion object {
        fun fromCode(code: String): ExportQualityOption =
            entries.firstOrNull { it.code == code } ?: FHD_1080
    }
}

@Entity(tableName = "video_projects")
data class VideoProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val videoUri: String,
    val fileName: String,
    val thumbnailPath: String?,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
    val aspectRatio: String,
    val captionStyle: String,
    val exportQuality: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "video_clips",
    foreignKeys = [
        ForeignKey(
            entity = VideoProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class VideoClipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val projectTitle: String,
    val title: String,
    val videoUri: String,
    val thumbnailPath: String?,
    val startMs: Long,
    val endMs: Long,
    val aspectRatio: String,
    val captionStyle: String,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val durationMs: Long
        get() = (endMs - startMs).coerceAtLeast(0L)
}

enum class SmartCropMode(
    val code: String,
    val label: String,
    val description: String
) {
    CENTER("CENTER", "Center Focus", "Balanced 9:16 center crop"),
    SPEAKER_TOP("SPEAKER_TOP", "Speaker / Face", "Prioritizes upper face & webcam area"),
    ACTION_LEFT("ACTION_LEFT", "Left Action", "Focuses left-side gameplay HUD"),
    ACTION_RIGHT("ACTION_RIGHT", "Right Action", "Focuses right-side gameplay HUD");

    companion object {
        fun fromCode(code: String): SmartCropMode =
            entries.firstOrNull { it.code == code } ?: CENTER
    }
}

@Entity(tableName = "exported_downloads")
data class ExportedDownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val contentUri: String,
    val locationLabel: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sourceModule: String,
    val aspectRatio: String,
    val convertedTo9x16: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

