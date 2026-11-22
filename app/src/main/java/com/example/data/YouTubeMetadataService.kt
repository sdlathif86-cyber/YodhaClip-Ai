package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

data class YouTubeVideoMetadata(
    val videoId: String,
    val canonicalUrl: String,
    val title: String,
    val channelTitle: String,
    val description: String,
    val thumbnailUrl: String,
    val durationMs: Long?,
    val tags: List<String>,
    val publishedAt: String?,
    val apiSource: String
)

class YouTubeMetadataService(private val context: Context? = null) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun extractVideoId(rawInput: String): String? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null

        val rawIdRegex = Regex("^[a-zA-Z0-9_-]{11}$")
        if (rawIdRegex.matches(trimmed)) {
            return trimmed
        }

        val patterns = listOf(
            Regex("(?:youtube\\.com/watch\\?(?:.*&)?v=)([a-zA-Z0-9_-]{11})"),
            Regex("(?:youtu\\.be/)([a-zA-Z0-9_-]{11})"),
            Regex("(?:youtube\\.com/shorts/)([a-zA-Z0-9_-]{11})"),
            Regex("(?:youtube\\.com/embed/)([a-zA-Z0-9_-]{11})"),
            Regex("(?:youtube\\.com/live/)([a-zA-Z0-9_-]{11})")
        )
        for (pattern in patterns) {
            val match = pattern.find(trimmed)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun isNetworkAvailable(): Boolean {
        val appContext = context ?: return true
        return try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    /**
     * Fetches official permitted YouTube metadata using YouTube's public oEmbed endpoint.
     * Requires NO YouTube API key and NEVER exposes any API key in the app or APK.
     * Strictly metadata-only: never downloads streams or bypasses YouTube restrictions.
     */
    suspend fun fetchMetadata(rawUrl: String): Result<YouTubeVideoMetadata> =
        withContext(Dispatchers.IO) {
            val videoId = extractVideoId(rawUrl)
                ?: return@withContext Result.failure(
                    IllegalArgumentException(
                        "Please enter a valid YouTube link (e.g., youtube.com/watch?v=... or youtu.be/...)."
                    )
                )

            if (!isNetworkAvailable()) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "You appear to be offline. Connect to the internet to fetch YouTube details, or upload your original MP4 file directly below."
                    )
                )
            }

            val canonicalUrl = "https://www.youtube.com/watch?v=$videoId"
            fetchViaOfficialOEmbed(videoId, canonicalUrl)
        }

    private fun fetchViaOfficialOEmbed(
        videoId: String,
        canonicalUrl: String
    ): Result<YouTubeVideoMetadata> {
        return try {
            val encodedUrl = URLEncoder.encode(canonicalUrl, "UTF-8")
            val oembedUrl = "https://www.youtube.com/oembed?url=$encodedUrl&format=json"
            val request = Request.Builder()
                .url(oembedUrl)
                .header("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val hint = when (response.code) {
                        401, 403 -> "This video restricts public metadata embedding. Confirm you own or have permission for it and upload your original MP4 below."
                        404 -> "YouTube video not found. Double-check the URL and make sure the video is public or unlisted."
                        else -> "YouTube metadata service temporarily unavailable (HTTP ${response.code}). Please retry or upload your MP4 directly."
                    }
                    return Result.failure(IllegalStateException(hint))
                }
                val json = JSONObject(body)
                val title = json.optString("title", "YouTube Video").trim().ifBlank { "YouTube Video" }
                val author = json.optString("author_name", "YouTube Creator").trim().ifBlank { "YouTube Creator" }
                val thumb = json.optString(
                    "thumbnail_url",
                    "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                ).ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" }

                Result.success(
                    YouTubeVideoMetadata(
                        videoId = videoId,
                        canonicalUrl = canonicalUrl,
                        title = title,
                        channelTitle = author,
                        description = "",
                        thumbnailUrl = thumb,
                        durationMs = null,
                        tags = emptyList(),
                        publishedAt = null,
                        apiSource = "Official YouTube oEmbed (No API Key)"
                    )
                )
            }
        } catch (_: UnknownHostException) {
            Result.failure(
                IllegalStateException(
                    "Could not reach YouTube right now. Check your internet connection and tap Retry, or upload your original MP4 file directly below."
                )
            )
        } catch (_: SocketTimeoutException) {
            Result.failure(
                IllegalStateException(
                    "Fetching YouTube details timed out. Please check your connection and try again."
                )
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    "Unable to fetch YouTube details (${e.localizedMessage ?: "network error"}). You can still upload your original MP4 below."
                )
            )
        }
    }
}
