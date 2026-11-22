package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.ai.LiveHighlightAiService
import com.example.ai.LongVideoAiService
import com.example.ai.LongVideoAnalysisResult
import com.example.ai.LongVideoClipProposal
import com.example.ai.SubtitleLanguage
import com.example.data.AspectRatioOption
import com.example.data.CaptionStyleOption
import com.example.data.ExportedDownloadEntity
import com.example.data.InspectedVideoInfo
import com.example.data.LiveRollingBufferEngine
import com.example.data.LiveStreamProtocol
import com.example.data.SmartCropMode
import com.example.data.StudioSettings
import com.example.data.StudioStorageBreakdown
import com.example.data.StudioStorageManager
import com.example.data.VideoClipEntity
import com.example.data.VideoMediaInspector
import com.example.ui.LiveClipApprovalStatus
import com.example.ui.LiveConnectionStatus
import com.example.ui.LiveDetectedClip
import com.example.ui.LiveStreamShortsUiState
import com.example.ui.LongMomentsState
import com.example.ui.LongTranscriptState
import com.example.ui.LongVideoShortsUiState
import com.example.ui.StagedClipDraft
import com.example.ui.UploadAiHighlightSuggestion
import com.example.ui.UploadAiState
import com.example.ui.UploadStudioState
import com.example.ui.VideoInputMode
import com.example.ui.YodhaClipApp
import com.example.ui.screens.ClipsScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.LiveStreamShortsScreen
import com.example.ui.screens.LongVideoShortsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UploadScreen
import com.example.ui.theme.YodhaClipTheme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StudioModulesRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // -------------------------------------------------------------------------
    // MODULE 1 TESTS: LONG VIDEO TO SHORTS
    // -------------------------------------------------------------------------

    @Test
    fun `module1 parses 10-20-30 highlights with 15-60s duration clamping and multilingual captions`() {
        val ai = LongVideoAiService()
        val sampleJson = """
            {
              "detectedTopic": "BGMI Gaming Tournament",
              "overallSummary": "High-octane tournament highlights with clutch plays.",
              "clips": [
                {
                  "rank": 1,
                  "hookTitle": "Insane 1v4 Final Circle Clutch",
                  "reason": "Peak tension and sudden squad wipe",
                  "category": "Gaming Clutch",
                  "viralScore": 96,
                  "startSeconds": 120,
                  "endSeconds": 162,
                  "smartCrop": "SPEAKER_TOP",
                  "hashtags": ["Shorts", "#BGMI", "#Clutch"],
                  "subtitlesEnglish": ["No way he survives this", "Four enemies left", "Clutch victory!"],
                  "subtitlesHindi": ["क्या यह बचेगा?", "चार दुश्मन बाकी", "शानदार क्लच जीत!"],
                  "subtitlesTelugu": ["అతను గెలుస్తాడా?", "నలుగురు శత్రువులు మిగిలారు", "అద్భుతమైన క్లచ్ విజయం!"]
                },
                {
                  "rank": 2,
                  "hookTitle": "Too Long Clip Should Clamp To 60s",
                  "reason": "Extended reaction sequence",
                  "category": "Big Reaction",
                  "viralScore": 105,
                  "startSeconds": 300,
                  "endSeconds": 450,
                  "smartCrop": "ACTION_LEFT",
                  "hashtags": ["#Gaming"],
                  "subtitlesEnglish": ["Watch till the end"],
                  "subtitlesHindi": ["अंत तक देखें"],
                  "subtitlesTelugu": ["చివరి వరకు చూడండి"]
                }
              ]
            }
        """.trimIndent()

        val result = ai.parseHighlightsJson(
            rawJson = sampleJson,
            maxDurationSec = 600L,
            minClipSec = 15L,
            maxClipSec = 60L
        )
        assertEquals("BGMI Gaming Tournament", result.detectedTopic)
        assertEquals(2, result.proposals.size)

        val clip1 = result.proposals[0]
        assertEquals(42_000L, clip1.durationMs)
        assertEquals(96, clip1.viralScore)
        assertEquals(SmartCropMode.SPEAKER_TOP, clip1.recommendedCropMode)
        assertTrue(clip1.hashtags.contains("#Shorts"))
        assertEquals("అతను గెలుస్తాడా?", clip1.subtitlesFor(SubtitleLanguage.TELUGU).first())

        val clip2 = result.proposals[1]
        assertEquals(60_000L, clip2.durationMs)
        assertEquals(99, clip2.viralScore)
        assertEquals(SmartCropMode.ACTION_LEFT, clip2.recommendedCropMode)
    }

    @Test
    fun `module1 LongVideoShortsScreen renders 10 20 30 selectors subtitles and viral clip cards`() {
        var selectedCount = 10
        val sampleProposal = LongVideoClipProposal(
            rank = 1,
            hookTitle = "Crazy Sniper Headshot",
            reason = "Instant hook in first 2 seconds",
            category = "Gaming Highlight",
            viralScore = 95,
            startMs = 15_000L,
            endMs = 45_000L,
            recommendedCropMode = SmartCropMode.CENTER,
            hashtags = listOf("#Shorts", "#Gaming"),
            englishSubtitles = listOf("Watch this shot!"),
            hindiSubtitles = listOf("यह शॉट देखें!"),
            teluguSubtitles = listOf("ఈ షాట్ చూడండి!")
        )

        composeTestRule.setContent {
            YodhaClipTheme {
                LongVideoShortsScreen(
                    state = LongVideoShortsUiState(
                        requestedMomentCount = 20,
                        subtitleLanguage = SubtitleLanguage.TELUGU,
                        transcriptState = LongTranscriptState.Ready("Telugu + English", "Speech transcribed"),
                        momentsState = LongMomentsState.Success(
                            LongVideoAnalysisResult(
                                detectedTopic = "Esports",
                                overallSummary = "Top moments detected",
                                proposals = listOf(sampleProposal)
                            )
                        ),
                        selectedClipIds = setOf(sampleProposal.id),
                        engineLabel = "Jetpack Media3 Transformer"
                    ),
                    onBack = {},
                    onVideoPicked = { _, _ -> },
                    onClearVideo = {},
                    onTranscriptChange = {},
                    onTranscriptFilePicked = {},
                    onGenerateTranscript = {},
                    onMomentCountChange = { selectedCount = it },
                    onSubtitleLanguageChange = {},
                    onConvertTo9x16Change = {},
                    onSmartCropChange = {},
                    onDetectMoments = {},
                    onToggleClipSelection = {},
                    onSelectAllClips = {},
                    onClearSelection = {},
            onUpdateClipProposal = { _, _, _, _, _, _, _, _, _, _ -> },
                    onRegenerateClipMetadata = {},
                    onExportSingleMp4 = {},
                    onExportSelectedBatchMp4 = {},
                    onExportSubtitlesSrt = {},
                    onSaveSelectedToLibrary = {},
                    onOpenDownloads = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("long_step1_upload_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("long_input_mode_file_manager").assertIsDisplayed()
        composeTestRule.onNodeWithTag("long_input_mode_gallery").assertIsDisplayed()
        composeTestRule.onNodeWithTag("long_input_mode_youtube_link").assertIsDisplayed()

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("long_count_30"))
        composeTestRule.onNodeWithTag("long_count_30").performClick()
        assertEquals(30, selectedCount)

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("long_clip_card_1"))
        composeTestRule.onNodeWithTag("long_clip_card_1").assertIsDisplayed()
        composeTestRule.onNodeWithText("95% Viral").assertIsDisplayed()
    }

    @Test
    fun `module1 parses speaker changes important scenes and full viral kit metadata`() {
        val ai = LongVideoAiService()
        val viralJson = """
            {
              "detectedTopic": "Telugu Tech Podcast",
              "overallSummary": "Two hosts debate AI smartphone cameras and battery life.",
              "speakerChanges": ["00:12 Host A -> Guest B", "01:05 Guest B -> Host A"],
              "importantScenes": ["00:18 Camera Zoom Reveal", "01:12 Battery Stress Test"],
              "clips": [
                {
                  "rank": 1,
                  "hookTitle": "Why 200MP Cameras Still Fail At Night",
                  "openingHook": "Stop trusting megapixel numbers right now!",
                  "seoDescription": "Full breakdown of sensor size vs megapixel count for mobile creators.",
                  "thumbnailText": "200MP = MYTH?",
                  "reason": "Sharp debate hook with clear scene reveal",
                  "category": "Tech Insight",
                  "viralScore": 94,
                  "startSeconds": 15,
                  "endSeconds": 45,
                  "smartCrop": "SPEAKER_TOP",
                  "pauseTrimNote": "Trimmed 2s dead air at start, kept full conclusion sentence",
                  "highlightWords": ["200MP", "Sensor", "Night"],
                  "hashtags": ["#Shorts", "#TechTelugu", "#CameraTest"],
                  "subtitlesEnglish": ["Stop trusting 200MP labels", "Sensor size wins at night"],
                  "subtitlesHindi": ["200MP नंबर पर भरोसा न करें", "रात में सेंसर साइज जीतता है"],
                  "subtitlesTelugu": ["200MP నంబర్లను నమ్మకండి", "రాత్రిపూట సెన్సార్ సైజు ముఖ్యం"]
                }
              ]
            }
        """.trimIndent()

        val result = ai.parseHighlightsJson(
            rawJson = viralJson,
            maxDurationSec = 600L,
            minClipSec = 15L,
            maxClipSec = 60L
        )
        assertEquals(2, result.speakerChanges.size)
        assertEquals(2, result.importantScenes.size)
        val clip = result.proposals.first()
        assertEquals("Stop trusting megapixel numbers right now!", clip.openingHook)
        assertEquals("200MP = MYTH?", clip.thumbnailText)
        assertTrue(clip.seoDescription.contains("sensor size", ignoreCase = true))
        assertEquals(listOf("200MP", "Sensor", "Night"), clip.highlightWords)
        assertTrue(clip.pauseTrimNote.contains("sentence", ignoreCase = true))
    }

    @Test
    fun `module1 LongVideoShortsScreen exposes duration presets pause switch music picker and viral kit`() {
        var chosenDuration = 30
        var chosenCustom = false
        var removePauses = true
        val sampleProposal = LongVideoClipProposal(
            rank = 1,
            hookTitle = "Best Gaming Comeback",
            openingHook = "Wait until the last 3 seconds!",
            seoDescription = "Unbelievable squad comeback clip converted to vertical Shorts.",
            thumbnailText = "1HP CLUTCH!",
            reason = "High retention ending",
            category = "Gaming Clutch",
            viralScore = 97,
            startMs = 20_000L,
            endMs = 50_000L,
            recommendedCropMode = SmartCropMode.CENTER,
            highlightWords = listOf("Clutch", "Comeback"),
            hashtags = listOf("#Shorts", "#Gaming"),
            englishSubtitles = listOf("Wait for this comeback!"),
            hindiSubtitles = listOf("इस वापसी का इंतजार करें!"),
            teluguSubtitles = listOf("ఈ కంబ్యాక్ చూడండి!")
        )

        composeTestRule.setContent {
            YodhaClipTheme {
                LongVideoShortsScreen(
                    state = LongVideoShortsUiState(
                        inputMode = VideoInputMode.YOUTUBE_LINK,
                        targetClipDurationSec = 30,
                        removePausesAndKeepSentences = true,
                        momentsState = LongMomentsState.Success(
                            LongVideoAnalysisResult(
                                detectedTopic = "Gaming",
                                overallSummary = "Clutch highlights",
                                speakerChanges = listOf("00:20 Caster -> Player"),
                                importantScenes = listOf("00:35 Final Circle"),
                                proposals = listOf(sampleProposal)
                            )
                        )
                    ),
                    onBack = {},
                    onVideoPicked = { _, _ -> },
                    onClearVideo = {},
                    onTranscriptChange = {},
                    onTranscriptFilePicked = {},
                    onGenerateTranscript = {},
                    onMomentCountChange = {},
                    onSubtitleLanguageChange = {},
                    onConvertTo9x16Change = {},
                    onSmartCropChange = {},
                    onDetectMoments = {},
                    onToggleClipSelection = {},
                    onSelectAllClips = {},
                    onClearSelection = {},
                    onUpdateClipProposal = { _, _, _, _, _, _, _, _, _, _ -> },
                    onRegenerateClipMetadata = {},
                    onExportSingleMp4 = {},
                    onExportSelectedBatchMp4 = {},
                    onExportSubtitlesSrt = {},
                    onSaveSelectedToLibrary = {},
                    onOpenDownloads = {},
                    onTargetDurationChange = { sec, custom ->
                        chosenDuration = sec
                        chosenCustom = custom
                    },
                    onRemovePausesChange = { removePauses = it }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("long_youtube_url_input").assertIsDisplayed()

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("long_duration_45s"))
        composeTestRule.onNodeWithTag("long_duration_45s").performScrollTo().performClick()
        assertEquals(45, chosenDuration)
        assertFalse(chosenCustom)

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("long_remove_pauses_switch"))
        composeTestRule.onNodeWithTag("long_remove_pauses_switch").performScrollTo().performClick()
        assertFalse(removePauses)

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("long_pick_music_button"))
        composeTestRule.onNodeWithTag("long_pick_music_button").performScrollTo().assertIsDisplayed()

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("long_copy_viral_kit_1"))
        composeTestRule.onNodeWithTag("long_copy_viral_kit_1").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `clips screen renders edit download mp4 and delete actions for each saved clip`() {
        var exportedId: Long? = null
        var deletedId: Long? = null
        val clip = VideoClipEntity(
            id = 42L,
            projectId = 7L,
            projectTitle = "Creator Vlog",
            title = "Viral Hook Clip",
            videoUri = "content://media/external/video/42",
            thumbnailPath = null,
            startMs = 5_000L,
            endMs = 35_000L,
            aspectRatio = "9:16",
            captionStyle = "BOLD_VIOLET",
            isFavorite = true
        )

        composeTestRule.setContent {
            YodhaClipTheme {
                ClipsScreen(
                    clips = listOf(clip),
                    settings = StudioSettings(),
                    onNavigateToUpload = {},
                    onToggleFavorite = {},
                    onDeleteClip = { deletedId = it },
                    onShowMessage = {},
                    onExportClipMp4 = { exportedId = it.id }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("clip_edit_42").assertIsDisplayed()
        composeTestRule.onNodeWithTag("clip_download_42").assertIsDisplayed().performClick()
        assertEquals(42L, exportedId)

        composeTestRule.onNodeWithTag("clip_delete_42").assertIsDisplayed().performClick()
        assertEquals(42L, deletedId)
    }

    @Test
    fun `settings screen supports language selection 45s clip duration and auto 9x16 toggle`() {
        var selectedLang = "en"
        var selectedClipSec = 30
        var auto9x16 = true

        composeTestRule.setContent {
            YodhaClipTheme {
                SettingsScreen(
                    settings = StudioSettings(),
                    cacheSizeBytes = 0L,
                    storageBreakdown = StudioStorageBreakdown(),
                    projectCount = 2,
                    clipCount = 5,
                    downloadCount = 3,
                    onExportQualityChange = {},
                    onAspectRatioChange = {},
                    onSmartClipSecondsChange = { selectedClipSec = it },
                    onCaptionStyleChange = {},
                    onShowBeginnerTipsChange = {},
                    onLoopClipPreviewsChange = {},
                    onClearCache = {},
                    onClearLiveBuffer = {},
                    onClearAllCaches = {},
                    onOpenDownloads = {},
                    onResetSettings = {},
                    onDefaultLanguageChange = { selectedLang = it },
                    onAutoConvert9x16Change = { auto9x16 = it }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_quality_720p").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_quality_1080p").assertIsDisplayed()

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("settings_lang_te"))
        composeTestRule.onNodeWithTag("settings_lang_te").performScrollTo().performClick()
        assertEquals("te", selectedLang)

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("settings_clip_len_45"))
        composeTestRule.onNodeWithTag("settings_clip_len_45").performScrollTo().performClick()
        assertEquals(45, selectedClipSec)

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("settings_auto_9x16_switch"))
        composeTestRule.onNodeWithTag("settings_auto_9x16_switch").performScrollTo().performClick()
        assertFalse(auto9x16)
    }

    // -------------------------------------------------------------------------
    // MODULE 2 TESTS: LIVE STREAM TO SHORTS
    // -------------------------------------------------------------------------

    @Test
    fun `module2 detects authorized HLS and RTMP protocols and parses HLS playlists accurately`() {
        assertEquals(
            LiveStreamProtocol.HLS,
            LiveRollingBufferEngine.detectProtocol("https://stream.example.com/live/channel.m3u8")
        )
        assertEquals(
            LiveStreamProtocol.RTMP,
            LiveRollingBufferEngine.detectProtocol("rtmp://ingest.example.com/live/my_key")
        )
        assertEquals(
            LiveStreamProtocol.RTMP,
            LiveRollingBufferEngine.detectProtocol("rtmps://secure.example.com/app/stream")
        )
        assertEquals(
            LiveStreamProtocol.UNKNOWN,
            LiveRollingBufferEngine.detectProtocol("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        )

        val masterPlaylist = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720
            720p/index.m3u8
        """.trimIndent()
        val (variant, direct) = LiveRollingBufferEngine.parseHlsPlaylist(
            "https://cdn.example.com/live/master.m3u8",
            masterPlaylist
        )
        assertEquals("https://cdn.example.com/live/720p/index.m3u8", variant)
        assertTrue(direct.isEmpty())

        val mediaPlaylist = """
            #EXTM3U
            #EXT-X-TARGETDURATION:4
            #EXTINF:4.000,
            seg_001.ts
            #EXTINF:3.500,
            seg_002.ts
        """.trimIndent()
        val (noVariant, segs) = LiveRollingBufferEngine.parseHlsPlaylist(
            "https://cdn.example.com/live/720p/index.m3u8",
            mediaPlaylist
        )
        assertEquals(null, noVariant)
        assertEquals(2, segs.size)
        assertEquals("https://cdn.example.com/live/720p/seg_001.ts", segs[0].uri)
        assertEquals(4.0, segs[0].durationSec, 0.01)
    }

    @Test
    fun `module2 parses live AI highlight detection JSON accurately`() {
        val liveAi = LiveHighlightAiService()
        val json = """
            {
              "isHighlightDetected": true,
              "momentCategory": "Gaming Highlight",
              "hookTitle": "Unbelievable Final Zone Win",
              "reason": "Victory banner and intense reaction detected",
              "viralScore": 92,
              "recommendedClipSeconds": 25,
              "smartCrop": "ACTION_RIGHT",
              "hashtags": ["#LiveShorts", "#Gaming"],
              "subtitlesEnglish": ["Victory royale!"],
              "subtitlesHindi": ["शानदार जीत!"],
              "subtitlesTelugu": ["అద్భుత విజయం!"]
            }
        """.trimIndent()
        val parsed = liveAi.parseLiveDetectionJson(json, forceManualCapture = false)
        assertTrue(parsed.isHighlightDetected)
        assertEquals("Gaming Highlight", parsed.momentCategory)
        assertEquals(92, parsed.viralScore)
        assertEquals(25, parsed.recommendedClipSeconds)
        assertEquals(SmartCropMode.ACTION_RIGHT, parsed.recommendedCropMode)
    }

    @Test
    fun `module2 LiveStreamShortsScreen refuses fake capture when disconnected and supports clip review approval`() {
        var approvedClipId: String? = null
        val pendingClip = LiveDetectedClip(
            id = "live_clip_1",
            hookTitle = "Boss Fight Clutch",
            momentCategory = "Gaming Highlight",
            reason = "Final HP comeback detected",
            viralScore = 91,
            durationMs = 28_000L,
            localClipPath = null,
            clipFileSizeBytes = 0L,
            smartCropMode = SmartCropMode.CENTER,
            hashtags = listOf("#Gaming", "#Shorts"),
            englishSubtitles = listOf("Let's go!!"),
            hindiSubtitles = listOf("चलो!!"),
            teluguSubtitles = listOf("సూపర్!!"),
            approvalStatus = LiveClipApprovalStatus.PENDING_REVIEW
        )

        composeTestRule.setContent {
            YodhaClipTheme {
                LiveStreamShortsScreen(
                    state = LiveStreamShortsUiState(
                        connectionStatus = LiveConnectionStatus.DISCONNECTED,
                        detectedClips = listOf(pendingClip)
                    ),
                    onBack = {},
                    onStreamUrlChange = {},
                    onStreamTitleChange = {},
                    onAuthorizationChange = {},
                    onPresetChange = {},
                    onRollingWindowChange = {},
                    onAutoDetectChange = {},
                    onSubtitleLanguageChange = {},
                    onConvertTo9x16Change = {},
                    onLiveContextNoteChange = {},
                    onConnectStream = {},
                    onDisconnectStream = {},
                    onPlayerLiveReady = {},
                    onPlayerStreamError = {},
                    onScanLiveMoment = { _, _ -> },
                    onApproveClip = { approvedClipId = it.id },
                    onRejectClip = {},
                    onExportClipMp4 = {},
                    onExportClipSrt = {},
                    onClearBuffer = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("live_processing_status_card").assertIsDisplayed()
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("live_capture_moment_button"))
        composeTestRule.onNodeWithTag("live_capture_moment_button").assertIsNotEnabled()

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("live_approve_live_clip_1"))
        composeTestRule.onNodeWithTag("live_approve_live_clip_1").performClick()
        assertEquals("live_clip_1", approvedClipId)
    }

    // -------------------------------------------------------------------------
    // MODULE 4 TESTS: DOWNLOAD MANAGER, STORAGE MANAGEMENT & FULL STUDIO NAVIGATION
    // -------------------------------------------------------------------------

    @Test
    fun `module4 DownloadsScreen filters MP4 and SRT exports cleanly`() {
        val sampleDownloads = listOf(
            ExportedDownloadEntity(
                id = 1L,
                fileName = "clutch_9x16.mp4",
                contentUri = "content://downloads/1",
                locationLabel = "Downloads/YodhaClipAI/clutch_9x16.mp4",
                mimeType = "video/mp4",
                sizeBytes = 4_200_000L,
                sourceModule = "Long Video to Shorts",
                aspectRatio = "9:16",
                convertedTo9x16 = true
            ),
            ExportedDownloadEntity(
                id = 2L,
                fileName = "clutch_te.srt",
                contentUri = "content://downloads/2",
                locationLabel = "Downloads/YodhaClipAI/clutch_te.srt",
                mimeType = "application/x-subrip",
                sizeBytes = 1024L,
                sourceModule = "Long Video to Shorts (Telugu SRT)",
                aspectRatio = "SRT",
                convertedTo9x16 = false
            )
        )

        composeTestRule.setContent {
            YodhaClipTheme {
                DownloadsScreen(
                    downloads = sampleDownloads,
                    onBack = {},
                    onDeleteDownload = {},
                    onClearHistory = {},
                    onOpenLongVideoShorts = {},
                    onShowMessage = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("download_item_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("download_item_2").assertIsDisplayed()

        composeTestRule.onNodeWithTag("downloads_filter_srt").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("download_item_2").assertIsDisplayed()
    }

    @Test
    fun `module4 StudioStorageManager accurately measures and clears real local buffer files`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = StudioStorageManager(context)
        val liveDir = File(context.filesDir, "yodha_live_buffer").apply { mkdirs() }
        val dummySeg = File(liveDir, "test_seg.ts").apply { writeBytes(ByteArray(2048) { 1 }) }
        assertTrue(dummySeg.exists())

        val before = manager.inspectStorage(recordedDownloadsBytes = 1024L)
        assertTrue(before.liveBufferBytes >= 2048L)
        assertTrue(before.liveSegmentCount >= 1)
        assertEquals(1024L, before.recordedDownloadsBytes)

        val cleared = manager.clearLiveBuffer()
        assertTrue(cleared >= 2048L)
        val after = manager.inspectStorage(recordedDownloadsBytes = 1024L)
        assertEquals(0L, after.liveSegmentCount.toLong())
    }

    @Test
    fun `all studio modules are reachable from YodhaClipApp Home without breaking existing features`() {
        composeTestRule.setContent {
            YodhaClipTheme {
                YodhaClipApp()
            }
        }
        composeTestRule.waitForIdle()

        // Navigate to Module 1: Long Video to Shorts
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("home_long_video_shorts_card"))
        composeTestRule.onNodeWithTag("home_long_video_shorts_card").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("long_step1_upload_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("long_shorts_back_button").performClick()
        composeTestRule.waitForIdle()

        // Navigate to Module 2: Live Stream to Shorts
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("home_live_stream_shorts_card"))
        composeTestRule.onNodeWithTag("home_live_stream_shorts_card").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("live_processing_status_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("live_shorts_back_button").performClick()
        composeTestRule.waitForIdle()

        // Navigate to Module 4: Download Manager via Settings
        composeTestRule.onNodeWithTag("nav_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("settings_open_downloads_button"))
        composeTestRule.onNodeWithTag("settings_open_downloads_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("downloads_back_button").assertIsDisplayed()
        assertNotNull(composeTestRule.onNodeWithText("Download Manager"))
    }

    @Test
    fun `video media inspector detects MP4 MOV and WebM formats accurately`() {
        assertEquals("MP4", VideoMediaInspector.detectFormatLabel("podcast_episode.mp4"))
        assertEquals("MOV", VideoMediaInspector.detectFormatLabel("iphone_recording.mov"))
        assertEquals("WEBM", VideoMediaInspector.detectFormatLabel("screen_capture.webm"))
        assertEquals("MOV", VideoMediaInspector.detectFormatLabel("clip_no_ext", "video/quicktime"))
        assertEquals("WEBM", VideoMediaInspector.detectFormatLabel("clip_no_ext", "video/webm"))
        assertTrue(VideoMediaInspector.SUPPORTED_VIDEO_MIME_TYPES.contains("video/mp4"))
        assertTrue(VideoMediaInspector.SUPPORTED_VIDEO_MIME_TYPES.contains("video/quicktime"))
        assertTrue(VideoMediaInspector.SUPPORTED_VIDEO_MIME_TYPES.contains("video/webm"))
    }

    @Test
    fun `upload studio supports editable subtitles AI highlight review approval and local MP4 clip export`() {
        var approvedSuggestionId: String? = null
        var exportedDraftId: String? = null
        var previewedDraftId: String? = null

        val sampleVideo = InspectedVideoInfo(
            uri = "content://media/external/video/99",
            fileName = "gameplay_session.webm",
            suggestedTitle = "Gameplay Session",
            durationMs = 120_000L,
            width = 1920,
            height = 1080,
            fileSizeBytes = 14_500_000L,
            thumbnailPath = null,
            formatLabel = "WEBM"
        )
        val suggestion = UploadAiHighlightSuggestion(
            id = "ai_s1",
            title = "Final Boss Victory",
            startMs = 40_000L,
            endMs = 70_000L,
            viralScore = 93,
            reason = "High energy celebration moment",
            suggestedSubtitle = "We finally beat the final boss!"
        )
        val stagedDraft = StagedClipDraft(
            localId = "draft_1",
            title = "Opening Hook",
            startMs = 5_000L,
            endMs = 25_000L,
            aspectRatio = AspectRatioOption.PORTRAIT_9_16,
            captionStyle = CaptionStyleOption.BOLD_VIOLET,
            thumbnailPath = null,
            subtitleText = "Watch what happens next!"
        )

        composeTestRule.setContent {
            YodhaClipTheme {
                UploadScreen(
                    uploadState = UploadStudioState(
                        inspectedVideo = sampleVideo,
                        projectTitle = "Gameplay Session",
                        trimStartMs = 5_000L,
                        trimEndMs = 35_000L,
                        manualClipTitle = "Highlight #2",
                        manualClipSubtitle = "Editable caption line",
                        stagedClips = listOf(stagedDraft),
                        aiConsentGranted = true,
                        aiHighlightState = UploadAiState.Success(listOf(suggestion))
                    ),
                    settings = StudioSettings(),
                    onMp4Picked = { _, _ -> },
                    onProjectTitleChange = {},
                    onAspectRatioChange = {},
                    onCaptionStyleChange = {},
                    onTrimRangeChange = { _, _ -> },
                    onManualClipTitleChange = {},
                    onPreviewTrim = {},
                    onConsumePreviewSeek = {},
                    onAddManualClip = {},
                    onGenerateSmartClips = {},
                    onRemoveStagedClip = {},
                    onClearSelection = {},
                    onSaveProjectAndClips = {},
                    onOpenYouTubeShorts = {},
                    onPreviewStagedClip = { previewedDraftId = it },
                    onExportStagedClipMp4 = { exportedDraftId = it.localId },
                    onApproveAiHighlight = { approvedSuggestionId = it.id }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify editable subtitle input & precision timeline nudge buttons
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("upload_clip_subtitle_input"))
        composeTestRule.onNodeWithTag("upload_clip_subtitle_input").performScrollTo().assertIsDisplayed()

        // Verify AI Highlight Review & Approve button
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("upload_ai_approve_ai_s1"))
        composeTestRule.onNodeWithTag("upload_ai_approve_ai_s1").performScrollTo().performClick()
        assertEquals("ai_s1", approvedSuggestionId)

        // Verify Staged Clip Preview & Direct MP4 Download actions
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("staged_clip_card_draft_1"))
        composeTestRule.onNodeWithTag("staged_preview_draft_1").performScrollTo().performClick()
        assertEquals("draft_1", previewedDraftId)

        composeTestRule.onNodeWithTag("staged_download_draft_1").performScrollTo().performClick()
        assertEquals("draft_1", exportedDraftId)
    }
}
