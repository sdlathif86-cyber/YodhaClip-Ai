package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "yodhaclip_studio_settings"
)

data class StudioSettings(
    val exportQuality: ExportQualityOption = ExportQualityOption.FHD_1080,
    val defaultAspectRatio: AspectRatioOption = AspectRatioOption.PORTRAIT_9_16,
    val smartClipSeconds: Int = 30,
    val defaultCaptionStyle: CaptionStyleOption = CaptionStyleOption.BOLD_VIOLET,
    val defaultLanguageCode: String = "en",
    val autoConvert9x16Export: Boolean = true,
    val showBeginnerTips: Boolean = true,
    val loopClipPreviews: Boolean = true
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val EXPORT_QUALITY = stringPreferencesKey("export_quality")
        val ASPECT_RATIO = stringPreferencesKey("default_aspect_ratio")
        val SMART_CLIP_SECONDS = intPreferencesKey("smart_clip_seconds")
        val CAPTION_STYLE = stringPreferencesKey("default_caption_style")
        val DEFAULT_LANGUAGE_CODE = stringPreferencesKey("default_language_code")
        val AUTO_CONVERT_9_16 = booleanPreferencesKey("auto_convert_9_16")
        val SHOW_BEGINNER_TIPS = booleanPreferencesKey("show_beginner_tips")
        val LOOP_CLIP_PREVIEWS = booleanPreferencesKey("loop_clip_previews")
    }

    val settingsFlow: Flow<StudioSettings> = context.studioDataStore.data.map { prefs ->
        StudioSettings(
            exportQuality = ExportQualityOption.fromCode(
                prefs[Keys.EXPORT_QUALITY] ?: ExportQualityOption.FHD_1080.code
            ),
            defaultAspectRatio = AspectRatioOption.fromCode(
                prefs[Keys.ASPECT_RATIO] ?: AspectRatioOption.PORTRAIT_9_16.code
            ),
            smartClipSeconds = prefs[Keys.SMART_CLIP_SECONDS] ?: 30,
            defaultCaptionStyle = CaptionStyleOption.fromCode(
                prefs[Keys.CAPTION_STYLE] ?: CaptionStyleOption.BOLD_VIOLET.code
            ),
            defaultLanguageCode = prefs[Keys.DEFAULT_LANGUAGE_CODE] ?: "en",
            autoConvert9x16Export = prefs[Keys.AUTO_CONVERT_9_16] ?: true,
            showBeginnerTips = prefs[Keys.SHOW_BEGINNER_TIPS] ?: true,
            loopClipPreviews = prefs[Keys.LOOP_CLIP_PREVIEWS] ?: true
        )
    }

    suspend fun setExportQuality(option: ExportQualityOption) {
        context.studioDataStore.edit { it[Keys.EXPORT_QUALITY] = option.code }
    }

    suspend fun setDefaultAspectRatio(option: AspectRatioOption) {
        context.studioDataStore.edit { it[Keys.ASPECT_RATIO] = option.code }
    }

    suspend fun setSmartClipSeconds(seconds: Int) {
        context.studioDataStore.edit { it[Keys.SMART_CLIP_SECONDS] = seconds }
    }

    suspend fun setDefaultCaptionStyle(option: CaptionStyleOption) {
        context.studioDataStore.edit { it[Keys.CAPTION_STYLE] = option.code }
    }

    suspend fun setDefaultLanguageCode(code: String) {
        context.studioDataStore.edit { it[Keys.DEFAULT_LANGUAGE_CODE] = code }
    }

    suspend fun setAutoConvert9x16Export(enabled: Boolean) {
        context.studioDataStore.edit { it[Keys.AUTO_CONVERT_9_16] = enabled }
    }

    suspend fun setShowBeginnerTips(enabled: Boolean) {
        context.studioDataStore.edit { it[Keys.SHOW_BEGINNER_TIPS] = enabled }
    }

    suspend fun setLoopClipPreviews(enabled: Boolean) {
        context.studioDataStore.edit { it[Keys.LOOP_CLIP_PREVIEWS] = enabled }
    }

    suspend fun resetDefaults() {
        context.studioDataStore.edit { it.clear() }
    }
}
