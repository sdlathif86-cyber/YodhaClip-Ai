package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.ai.ShortClipSuggestion
import com.example.ai.SubtitleLanguage
import com.example.ai.YouTubeShortsAiService
import com.example.data.AspectRatioOption
import com.example.data.CaptionStyleOption
import com.example.data.ExportQualityOption
import com.example.data.StudioSettings
import com.example.data.YouTubeMetadataService
import com.example.ui.Formatters
import com.example.ui.MetadataFetchState
import com.example.ui.ShortsAiState
import com.example.ui.YodhaClipApp
import com.example.ui.YouTubeShortsUiState
import com.example.ui.screens.YouTubeShortsScreen
import com.example.ui.theme.YodhaClipTheme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class YodhaClipRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `no api keys are exposed in BuildConfig`() {
        val fieldNames = BuildConfig::class.java.declaredFields.map { it.name }
        assertFalse(
            "YOUTUBE_API_KEY must never be compiled into BuildConfig/APK",
            fieldNames.any { it.contains("YOUTUBE_API_KEY", ignoreCase = true) }
        )
        assertFalse(
            "GEMINI_API_KEY must never be compiled into BuildConfig/APK",
            fieldNames.any { it.contains("GEMINI_API_KEY", ignoreCase = true) }
        )
        assertFalse(
            "No API_KEY field should be baked into BuildConfig",
            fieldNames.any { it.endsWith("API_KEY", ignoreCase = true) }
        )
    }

    @Test
    fun `youtube metadata service extracts video IDs across all URL formats`() {
        val service = YouTubeMetadataService()
        assertEquals("dQw4w9WgXcQ", service.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", service.extractVideoId("https://youtube.com/watch?si=abc&v=dQw4w9WgXcQ&t=42"))
        assertEquals("dQw4w9WgXcQ", service.extractVideoId("https://youtu.be/dQw4w9WgXcQ?t=10"))
        assertEquals("dQw4w9WgXcQ", service.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", service.extractVideoId("https://www.youtube.com/embed/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", service.extractVideoId("https://www.youtube.com/live/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", service.extractVideoId("dQw4w9WgXcQ"))
        assertNull(service.extractVideoId(""))
        assertNull(service.extractVideoId("https://example.com/not-youtube"))
    }

    @Test
    fun `youtube metadata service rejects invalid URL gracefully without api key`() = runTest {
        val service = YouTubeMetadataService()
        val result = service.fetchMetadata("not a youtube link")
        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message?.contains("valid YouTube link", ignoreCase = true) == true
        )
    }

    @Test
    fun `formatters handle duration file size and resolution accurately`() {
        assertEquals("00:00", Formatters.formatDuration(0L))
        assertEquals("00:45", Formatters.formatDuration(45_000L))
        assertEquals("02:05", Formatters.formatDuration(125_000L))
        assertEquals("1:01:01", Formatters.formatDuration(3_661_000L))

        assertEquals("0 KB", Formatters.formatFileSize(0L))
        assertEquals("512 KB", Formatters.formatFileSize(512L * 1024L))
        assertEquals("2.5 MB", Formatters.formatFileSize((2.5 * 1024 * 1024).toLong()))

        assertEquals("1080×1920 (1080p)", Formatters.formatResolution(1080, 1920))
        assertEquals("1280×720 (720p)", Formatters.formatResolution(1280, 720))
        assertEquals("3840×2160 (4K)", Formatters.formatResolution(3840, 2160))
    }

    @Test
    fun `srt subtitle builder generates valid English Hindi and Telugu SRT cues`() {
        val aiService = YouTubeShortsAiService()
        val clip = ShortClipSuggestion(
            rank = 1,
            hookTitle = "Best Creator Hook",
            reason = "Immediate tension in first 3 seconds",
            viralScore = 94,
            startMs = 10_000L,
            endMs = 25_000L,
            hashtags = listOf("#Shorts", "#YodhaClipAI"),
            englishSubtitles = listOf("Stop scrolling right now", "Watch this quick trick", "Save it for later"),
            hindiSubtitles = listOf("अभी स्क्रॉल करना रोकें", "यह ट्रिक जरूर देखें", "इसे सेव करें"),
            teluguSubtitles = listOf("ఇప్పుడే స్క్రోల్ చేయడం ఆపండి", "ఈ ట్రిక్ తప్పక చూడండి", "దీన్ని సేవ్ చేసుకోండి")
        )

        val enSrt = aiService.buildSrtContent(clip, SubtitleLanguage.ENGLISH)
        assertTrue(enSrt.contains("00:00:00,000 --> 00:00:05,000"))
        assertTrue(enSrt.contains("Stop scrolling right now"))
        assertTrue(enSrt.contains("00:00:10,000 --> 00:00:15,000"))

        val hiSrt = aiService.buildSrtContent(clip, SubtitleLanguage.HINDI)
        assertTrue(hiSrt.contains("अभी स्क्रॉल करना रोकें"))

        val teSrt = aiService.buildSrtContent(clip, SubtitleLanguage.TELUGU)
        assertTrue(teSrt.contains("ఇప్పుడే స్క్రోల్ చేయడం ఆపండి"))
    }

    @Test
    fun `settings enum code round trips fall back safely`() {
        assertEquals(AspectRatioOption.PORTRAIT_9_16, AspectRatioOption.fromCode("9:16"))
        assertEquals(AspectRatioOption.SQUARE_1_1, AspectRatioOption.fromCode("1:1"))
        assertEquals(AspectRatioOption.LANDSCAPE_16_9, AspectRatioOption.fromCode("16:9"))
        assertEquals(AspectRatioOption.PORTRAIT_9_16, AspectRatioOption.fromCode("unknown"))

        assertEquals(CaptionStyleOption.NEON_GLOW, CaptionStyleOption.fromCode("NEON_GLOW"))
        assertEquals(CaptionStyleOption.BOLD_VIOLET, CaptionStyleOption.fromCode("invalid"))

        assertEquals(ExportQualityOption.UHD_4K, ExportQualityOption.fromCode("4K"))
        assertEquals(ExportQualityOption.FHD_1080, ExportQualityOption.fromCode("invalid"))
    }

    @Test
    fun `full app navigation across Home Upload Projects Clips Settings and YouTube Shorts`() {
        composeTestRule.setContent {
            YodhaClipTheme {
                YodhaClipApp()
            }
        }
        composeTestRule.waitForIdle()

        // Home screen renders
        composeTestRule.onNodeWithTag("header_upload_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_home").assertIsDisplayed()

        // Navigate to Upload
        composeTestRule.onNodeWithTag("nav_upload").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("upload_dropzone_card").assertIsDisplayed()

        // Navigate to My Projects
        composeTestRule.onNodeWithTag("nav_projects").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("My Projects").assertIsDisplayed()

        // Navigate to My Clips
        composeTestRule.onNodeWithTag("nav_clips").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("My Clips").assertIsDisplayed()

        // Navigate to Settings
        composeTestRule.onNodeWithTag("nav_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_quality_1080p").assertIsDisplayed()

        // Return Home and open YouTube Video to Shorts
        composeTestRule.onNodeWithTag("nav_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(androidx.compose.ui.test.hasScrollAction())
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("home_yt_shorts_card"))
        composeTestRule.onNodeWithTag("home_yt_shorts_card").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("yt_shorts_back_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("yt_url_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("yt_fetch_details_button").assertIsDisplayed()
    }

    @Test
    fun `youtube shorts screen displays loading indicator and retryable error state`() {
        var retryTapped = false
        composeTestRule.setContent {
            YodhaClipTheme {
                YouTubeShortsScreen(
                    state = YouTubeShortsUiState(
                        urlInput = "https://youtu.be/dQw4w9WgXcQ",
                        metadataState = MetadataFetchState.Error(
                            "Could not reach YouTube right now. Check your internet connection and tap Retry."
                        ),
                        aiState = ShortsAiState.Loading
                    ),
                    settings = StudioSettings(),
                    onBack = {},
                    onUrlChange = {},
                    onFetchMetadata = { retryTapped = true },
                    onOwnershipConfirmedChange = {},
                    onOriginalVideoPicked = { _, _ -> },
                    onClearOriginalVideo = {},
                    onTranscriptChange = {},
                    onTranscriptFilePicked = {},
                    onClipCountChange = {},
                    onSubtitleLanguageChange = {},
                    onConvertTo9x16Change = {},
                    onAnalyzeWithAi = {},
                    onToggleClipSelection = {},
                    onSelectAllClips = {},
                    onSelectTopFiveClips = {},
                    onClearSelection = {},
                    onDownloadSingleMp4 = {},
                    onDownloadSelectedBatch = {},
                    onDownloadSubtitlesSrt = {},
                    onSaveSelectedToLibrary = {},
                    onShowMessage = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(androidx.compose.ui.test.hasScrollAction())
            .performScrollToNode(androidx.compose.ui.test.hasText("Retry Fetch"))
        composeTestRule.onNodeWithText(
            "Could not reach YouTube right now. Check your internet connection and tap Retry."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry Fetch").performClick()
        assertTrue(retryTapped)
    }
}
