package com.example.ai

import android.graphics.Bitmap
import android.util.Log
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

private const val TAG = "LiveHighlightAiService"

enum class LiveHighlightPreset(val label: String, val focusPrompt: String) {
    GAMING("Gaming Highlights", "clutch plays, eliminations, victory screens, intense gunfights, scoreboard spikes, and boss fights"),
    REACTIONS("Reactions & Hype", "shocked facial expressions, big laughs, crowd hype, dramatic reveals, and emotional peaks"),
    ESPORTS("Esports & Tournaments", "multi-kills, comeback rounds, caster excitement, and trophy moments"),
    TALK_PODCAST("Live Talk & Q&A", "bold takes, high-energy debates, memorable quotes, and audience Q&A highlights")
}

data class LiveMomentDetectionResult(
    val isHighlightDetected: Boolean,
    val momentCategory: String,
    val hookTitle: String,
    val reason: String,
    val viralScore: Int,
    val recommendedClipSeconds: Int,
    val recommendedCropMode: SmartCropMode,
    val hashtags: List<String>,
    val englishSubtitles: List<String>,
    val hindiSubtitles: List<String>,
    val teluguSubtitles: List<String>
)

class LiveHighlightAiService {

    private val liveDetectionSchema = Schema.obj(
        properties = mapOf(
            "isHighlightDetected" to Schema.boolean(
                description = "True if the live frame/context shows an exciting, clip-worthy highlight moment"
            ),
            "momentCategory" to Schema.string(
                description = "Category e.g. Gaming Highlight, Big Reaction, Clutch Play, Exciting Moment, Quiet Segment"
            ),
            "hookTitle" to Schema.string(
                description = "Short viral Shorts title under 65 characters"
            ),
            "reason" to Schema.string(
                description = "Honest observation explaining what is happening in the captured live frame/context"
            ),
            "viralScore" to Schema.integer(
                description = "Viral potential score 40-99"
            ),
            "recommendedClipSeconds" to Schema.integer(
                description = "Recommended clip length between 15 and 60 seconds"
            ),
            "smartCrop" to Schema.enumeration(
                values = listOf("CENTER", "SPEAKER_TOP", "ACTION_LEFT", "ACTION_RIGHT"),
                description = "Best 9:16 vertical crop framing"
            ),
            "hashtags" to Schema.array(
                items = Schema.string(),
                description = "4 to 5 relevant hashtags starting with #"
            ),
            "subtitlesEnglish" to Schema.array(
                items = Schema.string(),
                description = "3 short English caption lines"
            ),
            "subtitlesHindi" to Schema.array(
                items = Schema.string(),
                description = "3 short Hindi caption lines in Devanagari script"
            ),
            "subtitlesTelugu" to Schema.array(
                items = Schema.string(),
                description = "3 short Telugu caption lines in Telugu script"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = liveDetectionSchema
                temperature = 0.45f
            }
        )
    }

    /**
     * Analyzes real captured frame(s) and live context from an ACTIVE authorized live stream
     * to detect gaming highlights, reactions, and exciting moments in near real time.
     */
    suspend fun evaluateLiveMoment(
        streamTitle: String,
        preset: LiveHighlightPreset,
        liveContextNote: String,
        capturedFrame: Bitmap?,
        streamUptimeSeconds: Long,
        forceManualCapture: Boolean
    ): LiveMomentDetectionResult {
        val prompt = buildString {
            appendLine("You are YodhaClip AI's near-real-time live stream highlight detector.")
            appendLine("Stream Title: $streamTitle")
            appendLine("Live Focus Preset: ${preset.label} (${preset.focusPrompt})")
            appendLine("Stream Elapsed Time: ${streamUptimeSeconds}s")
            if (liveContextNote.isNotBlank()) {
                appendLine("Live Stream Chat / Commentary Note: ${liveContextNote.take(600)}")
            }
            if (forceManualCapture) {
                appendLine("The broadcaster manually triggered a highlight capture right now. Set isHighlightDetected = true and generate accurate metadata, viral score, smart crop, hashtags, and English/Hindi/Telugu captions based on the attached live frame and stream context.")
            } else {
                appendLine("Evaluate the attached live frame honestly. Set isHighlightDetected = true ONLY if there is clear action, reaction, gameplay climax, or exciting visual/context activity. Do not fabricate fake events if the frame is static or loading.")
            }
        }

        val input = content {
            if (capturedFrame != null) {
                image(capturedFrame)
            }
            text(prompt)
        }

        try {
            val response = withTimeout(45_000L) {
                model.generateContent(input)
            }
            val rawJson = response.text
                ?: throw IllegalStateException("Live highlight AI returned an empty response.")
            return parseLiveDetectionJson(rawJson, forceManualCapture)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Live moment AI scan timed out", e)
            throw IllegalStateException("Live highlight scan timed out. Will retry on next cycle.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Live moment AI scan failed", e)
            throw IllegalStateException(
                e.localizedMessage ?: "Live highlight AI scan failed. Check connection.",
                e
            )
        }
    }

    internal fun parseLiveDetectionJson(
        rawJson: String,
        forceManualCapture: Boolean
    ): LiveMomentDetectionResult {
        val json = JSONObject(rawJson)
        val detected = forceManualCapture || json.optBoolean("isHighlightDetected", false)
        val title = json.optString("hookTitle", "Live Highlight Moment").trim()
            .ifBlank { "Live Highlight Moment" }

        val tagsArr = json.optJSONArray("hashtags")
        val hashtags = buildList {
            if (tagsArr != null) {
                for (i in 0 until tagsArr.length()) {
                    val tag = tagsArr.optString(i).trim()
                    if (tag.isNotBlank()) add(if (tag.startsWith("#")) tag else "#$tag")
                }
            }
            if (isEmpty()) addAll(listOf("#LiveShorts", "#Gaming", "#YodhaClipAI"))
        }

        fun readLines(key: String, fallback: String): List<String> {
            val arr = json.optJSONArray(key)
            val lines = buildList {
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val l = arr.optString(i).trim()
                        if (l.isNotBlank()) add(l)
                    }
                }
            }
            return lines.ifEmpty { listOf(fallback) }
        }

        return LiveMomentDetectionResult(
            isHighlightDetected = detected,
            momentCategory = json.optString("momentCategory", "Live Highlight"),
            hookTitle = title,
            reason = json.optString("reason", "Detected live highlight activity."),
            viralScore = json.optInt("viralScore", 82).coerceIn(40, 99),
            recommendedClipSeconds = json.optInt("recommendedClipSeconds", 30).coerceIn(15, 60),
            recommendedCropMode = SmartCropMode.fromCode(json.optString("smartCrop", "CENTER")),
            hashtags = hashtags,
            englishSubtitles = readLines("subtitlesEnglish", title),
            hindiSubtitles = readLines("subtitlesHindi", title),
            teluguSubtitles = readLines("subtitlesTelugu", title)
        )
    }
}
