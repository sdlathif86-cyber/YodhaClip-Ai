package com.example.ai

import android.util.Log
import com.example.data.ExtractedMediaPayload
import com.example.data.SmartCropMode
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

private const val TAG = "LongVideoAiService"

data class TranscriptGenerationResult(
    val detectedLanguage: String,
    val summary: String,
    val timestampedTranscript: String
)

data class LongVideoClipProposal(
    val id: String = UUID.randomUUID().toString(),
    val rank: Int,
    val hookTitle: String,
    val reason: String,
    val category: String,
    val viralScore: Int,
    val startMs: Long,
    val endMs: Long,
    val recommendedCropMode: SmartCropMode,
    val hashtags: List<String>,
    val englishSubtitles: List<String>,
    val hindiSubtitles: List<String>,
    val teluguSubtitles: List<String>,
    val openingHook: String = "",
    val seoDescription: String = "",
    val thumbnailText: String = "",
    val highlightWords: List<String> = emptyList(),
    val pauseTrimNote: String = ""
) {
    val durationMs: Long
        get() = (endMs - startMs).coerceAtLeast(1000L)

    fun subtitlesFor(language: SubtitleLanguage): List<String> = when (language) {
        SubtitleLanguage.ENGLISH -> englishSubtitles
        SubtitleLanguage.HINDI -> hindiSubtitles
        SubtitleLanguage.TELUGU -> teluguSubtitles
    }

    fun toShortClipSuggestion(): ShortClipSuggestion = ShortClipSuggestion(
        id = id,
        rank = rank,
        hookTitle = hookTitle,
        reason = reason,
        viralScore = viralScore,
        startMs = startMs,
        endMs = endMs,
        hashtags = hashtags,
        englishSubtitles = englishSubtitles,
        hindiSubtitles = hindiSubtitles,
        teluguSubtitles = teluguSubtitles
    )
}

data class LongVideoAnalysisResult(
    val detectedTopic: String,
    val overallSummary: String,
    val proposals: List<LongVideoClipProposal>,
    val speakerChanges: List<String> = emptyList(),
    val importantScenes: List<String> = emptyList()
)

data class RegeneratedMetadata(
    val hookTitle: String,
    val hashtags: List<String>,
    val openingHook: String = "",
    val seoDescription: String = "",
    val thumbnailText: String = ""
)

class LongVideoAiService {

    private val transcriptSchema = Schema.obj(
        properties = mapOf(
            "detectedLanguage" to Schema.string(description = "Primary spoken language detected in audio"),
            "summary" to Schema.string(description = "Concise 2-sentence overview of the video"),
            "timestampedTranscript" to Schema.string(
                description = "Accurate timestamped speech transcript with [MM:SS] markers and Speaker labels when speaker changes occur, based strictly on the provided media"
            )
        )
    )

    private val longVideoHighlightsSchema = Schema.obj(
        properties = mapOf(
            "detectedTopic" to Schema.string(description = "Primary topic, game, or genre of the video"),
            "overallSummary" to Schema.string(description = "2-sentence viral strategy summary"),
            "speakerChanges" to Schema.array(
                items = Schema.string(),
                description = "Detected speaker change timestamps and labels e.g. [00:42] Speaker 2 joins"
            ),
            "importantScenes" to Schema.array(
                items = Schema.string(),
                description = "Detected important scene shifts with timestamps e.g. [01:15] Main reveal"
            ),
            "clips" to Schema.array(
                description = "Ranked list of best vertical Shorts moments with complete viral content kit",
                items = Schema.obj(
                    properties = mapOf(
                        "rank" to Schema.integer(description = "1-based rank order"),
                        "hookTitle" to Schema.string(description = "Catchy YouTube Shorts title under 70 characters"),
                        "openingHook" to Schema.string(description = "Punchy 3-second opening hook line to stop scrolling"),
                        "seoDescription" to Schema.string(description = "SEO-friendly 2-sentence YouTube Shorts description with keywords"),
                        "thumbnailText" to Schema.string(description = "Bold 2-5 word suggested Shorts thumbnail overlay text"),
                        "reason" to Schema.string(description = "Why this moment has strong retention/viral potential"),
                        "category" to Schema.string(description = "Moment category e.g. Opening Hook, Gaming Clutch, Big Reaction, Key Insight, Funny Moment"),
                        "viralScore" to Schema.integer(description = "Viral potential score from 60 to 99"),
                        "startSeconds" to Schema.integer(description = "Natural sentence-boundary start timestamp in seconds"),
                        "endSeconds" to Schema.integer(description = "Natural sentence-boundary end timestamp in seconds"),
                        "pauseTrimNote" to Schema.string(description = "Note on removed dead-air pauses and clean sentence boundaries"),
                        "smartCrop" to Schema.enumeration(
                            values = listOf("CENTER", "SPEAKER_TOP", "ACTION_LEFT", "ACTION_RIGHT"),
                            description = "Best 9:16 vertical crop framing and subject tracking"
                        ),
                        "highlightWords" to Schema.array(
                            items = Schema.string(),
                            description = "3 to 5 impactful keywords from the captions to highlight in animated subtitles"
                        ),
                        "hashtags" to Schema.array(
                            items = Schema.string(),
                            description = "5 relevant viral hashtags starting with #"
                        ),
                        "subtitlesEnglish" to Schema.array(
                            items = Schema.string(),
                            description = "3 crisp English subtitle cues"
                        ),
                        "subtitlesHindi" to Schema.array(
                            items = Schema.string(),
                            description = "3 natural Hindi subtitle cues in Devanagari script"
                        ),
                        "subtitlesTelugu" to Schema.array(
                            items = Schema.string(),
                            description = "3 natural Telugu subtitle cues in Telugu script"
                        )
                    )
                )
            )
        )
    )

    private val titleHashtagSchema = Schema.obj(
        properties = mapOf(
            "hookTitle" to Schema.string(description = "Fresh viral Shorts title under 70 chars"),
            "openingHook" to Schema.string(description = "Short 3-second opening hook"),
            "seoDescription" to Schema.string(description = "SEO-friendly Shorts description"),
            "thumbnailText" to Schema.string(description = "Bold 2-5 word thumbnail text"),
            "hashtags" to Schema.array(
                items = Schema.string(),
                description = "5 viral hashtags starting with #"
            )
        )
    )

    private val transcriptModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = transcriptSchema
                temperature = 0.2f
            }
        )
    }

    private val highlightsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = longVideoHighlightsSchema
                temperature = 0.65f
            }
        )
    }

    private val metadataModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = titleHashtagSchema
                temperature = 0.8f
            }
        )
    }

    /**
     * Generates a real timestamped transcript from the uploaded video's extracted audio track
     * and sampled visual keyframes using Firebase AI Logic (Gemini 3.8 Flash).
     */
    suspend fun transcribeVideoMedia(
        videoTitle: String,
        durationMs: Long,
        payload: ExtractedMediaPayload
    ): TranscriptGenerationResult {
        if (payload.audioBytes == null && payload.keyframes.isEmpty()) {
            throw IllegalStateException(
                "Could not extract audio or video frames from this MP4. Try another MP4 file or paste/import a transcript."
            )
        }
        val totalSeconds = (durationMs / 1000L).coerceAtLeast(1L)
        val promptText = buildString {
            appendLine("You are YodhaClip AI's multimodal speech transcription engine.")
            appendLine("Video Title: $videoTitle")
            appendLine("Total Duration: $totalSeconds seconds")
            if (payload.audioBytes != null) {
                appendLine("Transcribe the spoken speech from the attached audio track accurately with [MM:SS] timestamps and mark speaker changes (e.g. Speaker 1, Speaker 2) when multiple speakers are heard.")
                appendLine("Preserve original spoken words (English, Hindi, Telugu, or mixed). Do NOT invent dialogue that is not in the audio.")
                appendLine("If the audio contains mostly gameplay/music without clear speech, describe the audible action cues and visual moments honestly with [MM:SS] timestamps.")
            } else {
                appendLine("This MP4 had no extractable audio track. Analyze the attached sequential video keyframes and produce honest visual scene timestamps [MM:SS] without fabricating spoken quotes.")
            }
        }

        val inputContent = content {
            payload.audioBytes?.let { bytes ->
                inlineData(bytes, payload.audioMimeType ?: "audio/mp4")
            }
            payload.keyframes.forEach { bitmap ->
                image(bitmap)
            }
            text(promptText)
        }

        try {
            val response = withTimeout(60_000L) {
                transcriptModel.generateContent(inputContent)
            }
            val rawJson = response.text
                ?: throw IllegalStateException("AI transcription returned an empty response. Please retry.")
            val json = JSONObject(rawJson)
            return TranscriptGenerationResult(
                detectedLanguage = json.optString("detectedLanguage", "Auto-Detected"),
                summary = json.optString("summary", "Video media analyzed."),
                timestampedTranscript = json.optString("timestampedTranscript", "").trim()
                    .ifBlank { throw IllegalStateException("No speech or scene transcript could be extracted.") }
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Transcription timed out", e)
            throw IllegalStateException(
                "AI speech transcription timed out after 60 seconds. Check your connection and retry, or paste a transcript.",
                e
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            throw IllegalStateException(
                e.localizedMessage ?: "AI speech transcription failed. Please verify connectivity and retry.",
                e
            )
        }
    }

    /**
     * Detects the best viral moments with AI viral scoring, speaker change & scene detection,
     * target duration guidance (15/30/45/60s or custom), pause trimming & complete sentence boundaries,
     * and full Viral Content Kit (title, SEO description, hashtags, thumbnail text, short hook,
     * highlight words, and Telugu/Hindi/English subtitles).
     */
    suspend fun detectBestMoments(
        videoTitle: String,
        transcript: String,
        durationMs: Long,
        requestedCount: Int,
        targetDurationSec: Int = 30,
        removePausesAndKeepSentences: Boolean = true,
        primaryLanguage: SubtitleLanguage = SubtitleLanguage.ENGLISH
    ): LongVideoAnalysisResult {
        val safeCount = when (requestedCount) {
            5, 10, 20, 30 -> requestedCount
            else -> requestedCount.coerceIn(5, 30)
        }
        val totalSeconds = (durationMs / 1000L).coerceAtLeast(15L)
        val clampedTarget = targetDurationSec.toLong().coerceIn(10L, minOf(90L, totalSeconds).coerceAtLeast(10L))
        val minClipSec = (clampedTarget - 10L).coerceAtLeast(if (totalSeconds >= 20L) 10L else 5L)
            .coerceAtMost(totalSeconds)
        val maxClipSec = (clampedTarget + 15L).coerceAtMost(minOf(90L, totalSeconds)).coerceAtLeast(minClipSec)

        val prompt = buildString {
            appendLine("You are an elite YouTube Shorts / Reels AI editor for YodhaClip AI.")
            appendLine("Analyze the real video transcript below and identify the EXACT $safeCount highest-potential viral Shorts moments.")
            appendLine()
            appendLine("Video Title: $videoTitle")
            appendLine("Total Video Duration: $totalSeconds seconds")
            appendLine("Required Clip Count: $safeCount")
            appendLine("Target Clip Duration: ~$clampedTarget seconds (allowed window: $minClipSec to $maxClipSec seconds)")
            appendLine("Primary Content Language for Viral Kit (Title, Hook, SEO Description, Thumbnail Text): ${primaryLanguage.label} (${primaryLanguage.nativeLabel})")
            appendLine()
            appendLine("Transcript & Scene Notes:")
            appendLine(transcript.trim().take(16_000))
            appendLine()
            appendLine("STRICT RULES:")
            appendLine("1. Identify speakerChanges (timestamps & speaker shifts) and importantScenes (key visual/topic moments).")
            appendLine("2. Return EXACTLY $safeCount clips ranked 1..$safeCount from highest to lowest viralScore (60-99).")
            appendLine("3. Every clip MUST satisfy 0 <= startSeconds < endSeconds <= $totalSeconds and stay near $clampedTarget seconds ($minClipSec–$maxClipSec s).")
            if (removePausesAndKeepSentences) {
                appendLine("4. NATURAL CONVERSATION FLOW: Start and end every clip on COMPLETE sentence boundaries. Never cut a sentence in the middle, and skip leading/trailing silent pauses or filler dead air. Summarize the clean cut in pauseTrimNote.")
            }
            appendLine("5. Choose smartCrop from CENTER, SPEAKER_TOP, ACTION_LEFT, ACTION_RIGHT for smart subject tracking in 9:16 vertical format.")
            appendLine("6. For EVERY clip, generate the full Viral Content Kit in ${primaryLanguage.label}:")
            appendLine("   - hookTitle: Catchy YouTube Shorts title (< 70 chars)")
            appendLine("   - openingHook: Punchy 3-second opening hook")
            appendLine("   - seoDescription: SEO-friendly YouTube Shorts description (2 sentences)")
            appendLine("   - thumbnailText: Bold 2–5 word thumbnail cover text")
            appendLine("   - hashtags: 5 relevant viral hashtags starting with # (including #Shorts)")
            appendLine("   - highlightWords: 3–5 impactful words to emphasize in animated captions")
            appendLine("7. Provide 3 natural subtitle cues per clip in ALL THREE languages: English (subtitlesEnglish), Hindi Devanagari (subtitlesHindi), and Telugu script (subtitlesTelugu).")
        }

        try {
            val response = withTimeout(60_000L) {
                highlightsModel.generateContent(prompt)
            }
            val rawJson = response.text
                ?: throw IllegalStateException("AI highlight detection returned an empty response.")
            return parseHighlightsJson(
                rawJson = rawJson,
                maxDurationSec = totalSeconds,
                minClipSec = minClipSec,
                maxClipSec = maxClipSec
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Highlight detection timed out", e)
            throw IllegalStateException(
                "AI highlight detection timed out after 60 seconds. Try selecting 10 clips and retry.",
                e
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Highlight detection failed", e)
            throw IllegalStateException(
                e.localizedMessage ?: "AI highlight detection failed. Please check internet connectivity and retry.",
                e
            )
        }
    }

    suspend fun regenerateTitleAndHashtags(
        currentTitle: String,
        transcriptSnippet: String,
        category: String,
        language: SubtitleLanguage = SubtitleLanguage.ENGLISH
    ): RegeneratedMetadata {
        val prompt = buildString {
            appendLine("Generate a fresher, higher-CTR viral YouTube Shorts kit in ${language.label} (${language.nativeLabel}):")
            appendLine("- hookTitle (< 65 chars)")
            appendLine("- openingHook (3-second scroll-stopping hook)")
            appendLine("- seoDescription (2-sentence SEO-friendly description)")
            appendLine("- thumbnailText (2–5 bold cover words)")
            appendLine("- 5 trending #hashtags")
            appendLine("Current Title: $currentTitle")
            appendLine("Category: $category")
            appendLine("Context: ${transcriptSnippet.take(1200)}")
        }
        try {
            val response = withTimeout(30_000L) {
                metadataModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty AI response.")
            val json = JSONObject(rawJson)
            val title = json.optString("hookTitle", currentTitle).trim().ifBlank { currentTitle }
            val arr = json.optJSONArray("hashtags")
            val tags = buildList {
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val t = arr.optString(i).trim()
                        if (t.isNotBlank()) add(if (t.startsWith("#")) t else "#$t")
                    }
                }
                if (isEmpty()) addAll(listOf("#Shorts", "#YodhaClipAI"))
            }
            return RegeneratedMetadata(
                hookTitle = title,
                hashtags = tags,
                openingHook = json.optString("openingHook", "").trim(),
                seoDescription = json.optString("seoDescription", "").trim(),
                thumbnailText = json.optString("thumbnailText", "").trim()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Regenerate metadata failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Could not regenerate viral kit.", e)
        }
    }

    internal fun parseHighlightsJson(
        rawJson: String,
        maxDurationSec: Long,
        minClipSec: Long,
        maxClipSec: Long
    ): LongVideoAnalysisResult {
        val root = JSONObject(rawJson)
        val topic = root.optString("detectedTopic", "Video Studio")
        val summary = root.optString("overallSummary", "AI detected top viral Shorts moments.")

        fun readRootList(key: String): List<String> {
            val arr = root.optJSONArray(key) ?: return emptyList()
            return buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.optString(i).trim()
                    if (item.isNotBlank()) add(item)
                }
            }
        }

        val speakerChanges = readRootList("speakerChanges")
        val importantScenes = readRootList("importantScenes")
        val clipsArray = root.optJSONArray("clips")
            ?: throw IllegalStateException("AI response did not contain any clip proposals.")

        val proposals = buildList {
            for (i in 0 until clipsArray.length()) {
                val obj = clipsArray.getJSONObject(i)
                val rawStart = obj.optLong("startSeconds", 0L)
                val rawEnd = obj.optLong("endSeconds", rawStart + minClipSec)

                val clampedStart = rawStart.coerceIn(0L, (maxDurationSec - minClipSec).coerceAtLeast(0L))
                val targetDuration = (rawEnd - rawStart).coerceIn(minClipSec, maxClipSec)
                val clampedEnd = (clampedStart + targetDuration).coerceIn(
                    (clampedStart + 3L).coerceAtMost(maxDurationSec),
                    maxDurationSec
                )

                val hashtagsArr = obj.optJSONArray("hashtags")
                val hashtags = buildList {
                    if (hashtagsArr != null) {
                        for (j in 0 until hashtagsArr.length()) {
                            val tag = hashtagsArr.optString(j).trim()
                            if (tag.isNotBlank()) add(if (tag.startsWith("#")) tag else "#$tag")
                        }
                    }
                    if (isEmpty()) addAll(listOf("#Shorts", "#YodhaClipAI"))
                }

                fun readLines(key: String, fallback: String): List<String> {
                    val arr = obj.optJSONArray(key)
                    val out = buildList {
                        if (arr != null) {
                            for (j in 0 until arr.length()) {
                                val line = arr.optString(j).trim()
                                if (line.isNotBlank()) add(line)
                            }
                        }
                    }
                    return out.ifEmpty { listOf(fallback) }
                }

                val highlightArr = obj.optJSONArray("highlightWords")
                val highlightWords = buildList {
                    if (highlightArr != null) {
                        for (j in 0 until highlightArr.length()) {
                            val w = highlightArr.optString(j).trim()
                            if (w.isNotBlank()) add(w)
                        }
                    }
                }

                val hookTitle = obj.optString("hookTitle", "Viral Moment #${i + 1}").trim()
                    .ifBlank { "Viral Moment #${i + 1}" }
                val reason = obj.optString("reason", "Strong hook & retention peak.")

                add(
                    LongVideoClipProposal(
                        rank = obj.optInt("rank", i + 1),
                        hookTitle = hookTitle,
                        reason = reason,
                        category = obj.optString("category", "Highlight"),
                        viralScore = obj.optInt("viralScore", 84).coerceIn(50, 99),
                        startMs = clampedStart * 1000L,
                        endMs = clampedEnd * 1000L,
                        recommendedCropMode = SmartCropMode.fromCode(obj.optString("smartCrop", "CENTER")),
                        hashtags = hashtags,
                        englishSubtitles = readLines("subtitlesEnglish", hookTitle),
                        hindiSubtitles = readLines("subtitlesHindi", hookTitle),
                        teluguSubtitles = readLines("subtitlesTelugu", hookTitle),
                        openingHook = obj.optString("openingHook", "").trim().ifBlank { hookTitle },
                        seoDescription = obj.optString("seoDescription", "").trim()
                            .ifBlank { "$hookTitle — $reason ${hashtags.take(3).joinToString(" ")}" },
                        thumbnailText = obj.optString("thumbnailText", "").trim()
                            .ifBlank { hookTitle.split(" ").take(3).joinToString(" ").uppercase() },
                        highlightWords = highlightWords,
                        pauseTrimNote = obj.optString("pauseTrimNote", "").trim()
                    )
                )
            }
        }

        if (proposals.isEmpty()) {
            throw IllegalStateException("AI returned zero highlight moments. Please try again.")
        }

        return LongVideoAnalysisResult(
            detectedTopic = topic,
            overallSummary = summary,
            proposals = proposals,
            speakerChanges = speakerChanges,
            importantScenes = importantScenes
        )
    }
}
