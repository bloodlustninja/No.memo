package com.example.nomemo.preview

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class LinkPreview(
    val title: String? = null,
    val thumbnailUrl: String? = null,
)

object LinkPreviewResolver {

    private val YOUTUBE_REGEX = "(?:https?://)?(?:www\\.|m\\.)?(?:youtube\\.com/(?:watch\\?v=|shorts/|embed/)|youtu\\.be/)([a-zA-Z0-9_-]{11})".toRegex()
    private val URL_REGEX = "(https?://[^\\s<>\"]+)".toRegex()

    private val OG_TITLE_REGEX_1 = "<meta\\s+[^>]*property=[\"']og:title[\"'][^>]*content=[\"']([^\"']+)[\"']".toRegex(RegexOption.IGNORE_CASE)
    private val OG_TITLE_REGEX_2 = "<meta\\s+[^>]*content=[\"']([^\"']+)[\"'][^>]*property=[\"']og:title[\"']".toRegex(RegexOption.IGNORE_CASE)
    private val TITLE_TAG_REGEX = "<title[^>]*>([^<]+)</title>".toRegex(RegexOption.IGNORE_CASE)

    private val OG_IMAGE_REGEX_1 = "<meta\\s+[^>]*property=[\"']og:image[\"'][^>]*content=[\"']([^\"']+)[\"']".toRegex(RegexOption.IGNORE_CASE)
    private val OG_IMAGE_REGEX_2 = "<meta\\s+[^>]*content=[\"']([^\"']+)[\"'][^>]*property=[\"']og:image[\"']".toRegex(RegexOption.IGNORE_CASE)

    /**
     * Extracts the first URL found in the text, if any.
     */
    fun extractFirstUrl(text: String): String? {
        return URL_REGEX.find(text)?.value
    }

    /**
     * Resolves a lightweight low-res link preview on Dispatchers.IO.
     * YouTube links use direct thumbnail URLs (mqdefault.jpg ~12KB) + oEmbed title without API keys.
     * Web URLs fetch only the first 16KB of HTML with a strict 3-second timeout.
     */
    suspend fun resolve(rawUrl: String): LinkPreview? = withContext(Dispatchers.IO) {
        val targetUrl = URL_REGEX.find(rawUrl)?.value ?: rawUrl.trim()
        if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
            return@withContext null
        }

        // 1. YouTube URLs (Video ID -> mqdefault.jpg & oEmbed Title)
        val ytMatch = YOUTUBE_REGEX.find(targetUrl)
        if (ytMatch != null) {
            val videoId = ytMatch.groupValues[1]
            val thumbnailUrl = "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
            var videoTitle: String? = null

            try {
                val oEmbedUrl = "https://www.youtube.com/oembed?url=${URLEncoder.encode("https://www.youtube.com/watch?v=$videoId", "UTF-8")}&format=json"
                val conn = (URL(oEmbedUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3000
                    readTimeout = 3000
                    setRequestProperty("User-Agent", "NoMemo-Android/1.0")
                }
                if (conn.responseCode in 200..299) {
                    val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(jsonStr)
                    videoTitle = json.optString("title").ifBlank { null }
                }
                conn.disconnect()
            } catch (_: Exception) {}

            return@withContext LinkPreview(
                title = videoTitle,
                thumbnailUrl = thumbnailUrl,
            )
        }

        // 2. Generic HTTP/HTTPS URLs (Stream first ~16KB of HTML with 3s timeout)
        try {
            val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36")
                setRequestProperty("Accept", "text/html,application/xhtml+xml")
            }

            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return@withContext null
            }

            val buffer = ByteArray(16384)
            val bytesRead = conn.inputStream.use { stream ->
                stream.read(buffer)
            }
            conn.disconnect()

            if (bytesRead <= 0) return@withContext null
            val html = String(buffer, 0, bytesRead, Charsets.UTF_8)

            // Extract Title
            val title = OG_TITLE_REGEX_1.find(html)?.groupValues?.get(1)
                ?: OG_TITLE_REGEX_2.find(html)?.groupValues?.get(1)
                ?: TITLE_TAG_REGEX.find(html)?.groupValues?.get(1)?.trim()

            // Extract Image
            var imageUrl = OG_IMAGE_REGEX_1.find(html)?.groupValues?.get(1)
                ?: OG_IMAGE_REGEX_2.find(html)?.groupValues?.get(1)

            if (!imageUrl.isNullOrBlank()) {
                if (imageUrl.startsWith("//")) {
                    imageUrl = "https:$imageUrl"
                } else if (imageUrl.startsWith("/") && !imageUrl.startsWith("http")) {
                    val base = URL(targetUrl)
                    imageUrl = "${base.protocol}://${base.host}$imageUrl"
                }
            }

            if (title != null || imageUrl != null) {
                return@withContext LinkPreview(
                    title = title?.take(120),
                    thumbnailUrl = imageUrl,
                )
            }
        } catch (_: Exception) {}

        null
    }
}
