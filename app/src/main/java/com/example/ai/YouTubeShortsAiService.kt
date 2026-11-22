package com.example.ai

import android.util.Log
import com.example.data.YouTubeVideoMetadata
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

private const val TAG = "YouTubeShortsAiService"

enum class SubtitleLanguage(
    val code: String,
    val label: String,
    val nativeLabel: String
) {
    ENGLISH("en", "English", "English"),
    HINDI("hi", "Hindi", "हिन्दी"),
    TELUGU("te", "Telugu", "తెలుగు")
}

data class ShortClipSuggestion(
    val id: String = UUID.randomUUID().toString(),
    val rank: Int,
    val hookTitle: String,
    val reason: String,
    val viralScore: Int,
    val startMs: Long,
    val endMs: Long,
    val hashtags: List<String>,
    val englishSubtitles: List<String>,
    val hindiSubtitles: List<String>,
    val teluguSubtitles: List<String>
) {
    val durationMs: Long
        get() = (endMs - startMs).coerceAtLeast(1000L)

    fun subtitlesFor(language: SubtitleLanguage): List<String> = when (language) {
        SubtitleLanguage.ENGLISH -> englishSubtitles
        SubtitleLanguage.HINDI -> hindiSubtitles
        SubtitleLanguage.TELUGU -> teluguSubtitles
    }
}

data class ShortsAnalysisResult(
    val transcriptSummary: String,
    val detectedTopic: String,
    val suggestions: List<ShortClipSuggestion>
)

class YouTubeShortsAiService {

    private val analysisSchema = Schema.obj(
        properties = mapOf(
            "transcriptSummary" to Schema.string(
                description = "2-sentence summary of the video's strongest moments and pacing"
            ),
            "detectedTopic" to Schema.string(
                description = "Primary niche or category of the video"
            ),
            "clips" to Schema.array(
                description = "Ranked list of best vertical YouTube Shorts moments",
                items = Schema.obj(
                    properties = mapOf(
                        "rank" to Schema.integer(description = "1-based rank order"),
                        "hookTitle" to Schema.string(
                            description = "Catchy YouTube Shorts title under 70 characters"
                        ),
                        "reason" to Schema.string(
                            description = "Short explanation of why this moment hooks viewers"
                        ),
                        "viralScore" to Schema.integer(
                            description = "Predicted engagement score from 65 to 99"
                        ),
                        "startSeconds" to Schema.integer(
                            description = "Clip start timestamp in whole seconds"
                        ),
                        "endSeconds" to Schema.integer(
                            description = "Clip end timestamp in whole seconds (15 to 58 seconds after startSeconds)"
                        ),
                        "hashtags" to Schema.array(
                            items = Schema.string(),
                            description = "4 to 5 relevant Shorts hashtags starting with #"
                        ),
                        "subtitlesEnglish" to Schema.array(
                            items = Schema.string(),
                            description = "3 short punchy subtitle lines in English for this clip"
                        ),
                        "subtitlesHindi" to Schema.array(
                            items = Schema.string(),
                            description = "3 short punchy subtitle lines in natural Hindi (Devanagari script) for this clip"
                        ),
                        "subtitlesTelugu" to Schema.array(
                            items = Schema.string(),
                            description = "3 short punchy subtitle lines in natural Telugu (Telugu script) for this clip"
                        )
                    )
                )
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = analysisSchema
                temperature = 0.65f
            }
        )
    }

    suspend fun analyzeAndSuggestClips(
        metadata: YouTubeVideoMetadata?,
        transcript: String,
        videoDurationMs: Long,
        requestedCount: Int
    ): ShortsAnalysisResult {
        val safeCount = requestedCount.coerceIn(5, 20)
        val totalSeconds = (videoDurationMs / 1000L).coerceAtLeast(30L)
        val videoTitle = metadata?.title?.ifBlank { "Creator Video" } ?: "Creator Video"
        val channel = metadata?.channelTitle?.ifBlank { "Creator" } ?: "Creator"
        val description = metadata?.description?.take(1200).orEmpty()
        val tagsText = metadata?.tags?.take(12)?.joinToString(", ").orEmpty()
        val trimmedTranscript = transcript.trim().take(12_000)

        val prompt = buildString {
            appendLine("You are an expert YouTube Shorts editor and viral hook strategist for YodhaClip AI.")
            appendLine("Analyze the creator's authorized video metadata and transcript below, and identify the EXACT $safeCount best standalone vertical Shorts moments.")
            appendLine()
            appendLine("Video Title: $videoTitle")
            appendLine("Channel: $channel")
            appendLine("Total Video Duration: $totalSeconds seconds")
            if (tagsText.isNotBlank()) {
                appendLine("Video Tags: $tagsText")
            }
            if (description.isNotBlank()) {
                appendLine("Video Description: $description")
            }
            appendLine()
            if (trimmedTranscript.isNotBlank()) {
                appendLine("Creator Transcript / Script:")
                appendLine(trimmedTranscript)
            } else {
                appendLine("No transcript was pasted yet. Infer strong story beats, hooks, and evenly spaced highlight timestamps across 0..$totalSeconds seconds based on the video title, channel, and description.")
            }
            appendLine()
            appendLine("STRICT REQUIREMENTS:")
            appendLine("1. Return EXACTLY $safeCount items in the 'clips' array, ranked from best (rank 1) to rank $safeCount.")
            appendLine("2. Every clip's startSeconds MUST be >= 0 and < endSeconds <= $totalSeconds.")
            appendLine("3. Ideal Shorts length is 15 to 45 seconds (or shorter if total video duration is short). Spread timestamps thoughtfully across the full video so clips cover distinct moments.")
            appendLine("4. Provide engaging hookTitle, concise reason, viralScore (65-99), and 4-5 hashtags starting with # (always include #Shorts).")
            appendLine("5. For EVERY clip, generate 3 natural, punchy subtitle lines in ALL THREE languages:")
            appendLine("   - subtitlesEnglish: crisp English captions")
            appendLine("   - subtitlesHindi: authentic Hindi captions in Devanagari script")
            appendLine("   - subtitlesTelugu: authentic Telugu captions in Telugu script")
        }

        try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val rawJson = response.text
            if (rawJson.isNullOrBlank()) {
                throw IllegalStateException("Gemini returned an empty analysis response. Please try again.")
            }
            return parseAnalysisJson(rawJson, totalSeconds)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI Shorts analysis timed out", e)
            throw IllegalStateException(
                "AI analysis timed out after 60 seconds. Try a shorter transcript or 5/10 clips and retry.",
                e
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Shorts analysis failed", e)
            throw IllegalStateException(
                e.localizedMessage ?: "AI transcript analysis failed. Please verify connectivity and retry.",
                e
            )
        }
    }

    private fun parseAnalysisJson(rawJson: String, maxDurationSec: Long): ShortsAnalysisResult {
        val root = JSONObject(rawJson)
        val summary = root.optString("transcriptSummary", "AI identified high-retention Shorts hooks across your video.")
        val topic = root.optString("detectedTopic", "Video Highlights")
        val clipsArray = root.optJSONArray("clips")
            ?: throw IllegalStateException("AI response did not include clip suggestions.")

        val parsedClips = buildList {
            for (i in 0 until clipsArray.length()) {
                val obj = clipsArray.getJSONObject(i)
                val rawStart = obj.optLong("startSeconds", (i * 15L).coerceAtMost((maxDurationSec - 5L).coerceAtLeast(0L)))
                val rawEnd = obj.optLong("endSeconds", rawStart + 25L)

                val clampedStartSec = rawStart.coerceIn(0L, (maxDurationSec - 2L).coerceAtLeast(0L))
                val minEndSec = (clampedStartSec + 3L).coerceAtMost(maxDurationSec)
                val clampedEndSec = rawEnd.coerceIn(minEndSec, maxDurationSec)

                val hashtagsArray = obj.optJSONArray("hashtags")
                val hashtags = buildList {
                    if (hashtagsArray != null) {
                        for (j in 0 until hashtagsArray.length()) {
                            val tag = hashtagsArray.optString(j).trim()
                            if (tag.isNotBlank()) {
                                add(if (tag.startsWith("#")) tag else "#$tag")
                            }
                        }
                    }
                    if (isEmpty()) {
                        add("#Shorts")
                        add("#YodhaClipAI")
                    }
                }

                fun readLines(key: String, fallback: String): List<String> {
                    val arr = obj.optJSONArray(key)
                    val lines = buildList {
                        if (arr != null) {
                            for (j in 0 until arr.length()) {
                                val line = arr.optString(j).trim()
                                if (line.isNotBlank()) add(line)
                            }
                        }
                    }
                    return lines.ifEmpty { listOf(fallback) }
                }

                val hookTitle = obj.optString("hookTitle", "Viral Moment #${i + 1}").trim()
                add(
                    ShortClipSuggestion(
                        rank = obj.optInt("rank", i + 1),
                        hookTitle = hookTitle.ifBlank { "Viral Moment #${i + 1}" },
                        reason = obj.optString("reason", "Strong hook and high audience retention moment."),
                        viralScore = obj.optInt("viralScore", 85).coerceIn(50, 99),
                        startMs = clampedStartSec * 1000L,
                        endMs = clampedEndSec * 1000L,
                        hashtags = hashtags,
                        englishSubtitles = readLines("subtitlesEnglish", hookTitle),
                        hindiSubtitles = readLines("subtitlesHindi", hookTitle),
                        teluguSubtitles = readLines("subtitlesTelugu", hookTitle)
                    )
                )
            }
        }

        if (parsedClips.isEmpty()) {
            throw IllegalStateException("AI analysis returned zero clip suggestions. Please try again.")
        }

        return ShortsAnalysisResult(
            transcriptSummary = summary,
            detectedTopic = topic,
            suggestions = parsedClips
        )
    }

    fun buildSrtContent(clip: ShortClipSuggestion, language: SubtitleLanguage): String {
        val lines = clip.subtitlesFor(language)
        if (lines.isEmpty()) return ""
        val totalMs = clip.durationMs.coerceAtLeast(3000L)
        val stepMs = totalMs / lines.size
        return buildString {
            lines.forEachIndexed { index, text ->
                val start = index * stepMs
                val end = if (index == lines.lastIndex) totalMs else (index + 1) * stepMs
                appendLine(index + 1)
                appendLine("${formatSrtTime(start)} --> ${formatSrtTime(end)}")
                appendLine(text)
                appendLine()
            }
        }
    }

    private fun formatSrtTime(ms: Long): String {
        val safe = ms.coerceAtLeast(0L)
        val hours = safe / 3_600_000L
        val minutes = (safe % 3_600_000L) / 60_000L
        val seconds = (safe % 60_000L) / 1000L
        val millis = safe % 1000L
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }
}
