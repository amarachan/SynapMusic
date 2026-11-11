package com.synap.music.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Data structure representing a parsed Apple Music track.
 */
data class AppleMusicTrack(
    val title: String,
    val artist: String,
    val album: String? = null
)

/**
 * Data structure representing a parsed Apple Music playlist.
 */
data class AppleMusicPlaylist(
    val id: String,
    val name: String,
    val tracks: List<AppleMusicTrack>
)

/**
 * Client for fetching and parsing public Apple Music playlist web pages.
 * Extracts playlist details and tracklists directly from embedded JSON-LD metadata.
 */
class AppleMusicClient {

    private val httpClient = HttpClient(OkHttp)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Extracts the Playlist ID from a standard Apple Music playlist URL.
     * Example: https://music.apple.com/us/playlist/todays-hits/pl.f4d106fed2bd41149bb18d382d08a505 -> pl.f4d106fed2bd41149bb18d382d08a505
     */
    fun extractPlaylistId(url: String): String? {
        val regex = Regex("playlist/[^/]+/(pl\\.[a-zA-Z0-9]+|p\\.[a-zA-Z0-9]+)")
        val match = regex.find(url)
        return match?.groupValues?.get(1) ?: Regex("(pl\\.[a-zA-Z0-9]+|p\\.[a-zA-Z0-9]+)").find(url)?.value
    }

    /**
     * Fetches the Apple Music playlist HTML page and parses JSON-LD metadata.
     */
    suspend fun getPlaylist(url: String): AppleMusicPlaylist {
        val playlistId = extractPlaylistId(url) ?: throw IllegalArgumentException("Invalid Apple Music Playlist URL")
        val normalizedUrl = if (url.startsWith("http")) url else "https://$url"

        val html = httpClient.get(normalizedUrl) {
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        }.bodyAsText()

        // Extract JSON-LD script block <script type="application/ld+json">...</script>
        val jsonLdRegex = Regex("<script[^>]*type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>", RegexOption.DOT_MATCHES_ALL)
        val jsonLdMatch = jsonLdRegex.find(html)?.groupValues?.get(1)

        val tracks = mutableListOf<AppleMusicTrack>()
        var playlistName = "Apple Music Playlist"

        if (jsonLdMatch != null) {
            try {
                val rootJson = json.parseToJsonElement(jsonLdMatch).jsonObject
                playlistName = rootJson["name"]?.jsonPrimitive?.content ?: playlistName

                val trackArray = rootJson["track"]?.jsonArray
                trackArray?.forEach { itemElement ->
                    val trackObj = itemElement.jsonObject
                    val title = trackObj["name"]?.jsonPrimitive?.content
                    val artist = trackObj["byArtist"]?.jsonObject?.get("name")?.jsonPrimitive?.content
                        ?: trackObj["author"]?.jsonObject?.get("name")?.jsonPrimitive?.content
                        ?: "Unknown Artist"
                    val album = trackObj["inAlbum"]?.jsonObject?.get("name")?.jsonPrimitive?.content

                    if (!title.isNullOrBlank()) {
                        tracks.add(AppleMusicTrack(title = title, artist = artist, album = album))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback meta parsing if JSON-LD parsing returned 0 tracks
        if (tracks.isEmpty()) {
            val titleRegex = Regex("<meta[^>]*property=[\"']og:title[\"'][^>]*content=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            titleRegex.find(html)?.groupValues?.get(1)?.let { playlistName = it }

            val songMetaRegex = Regex("<meta[^>]*property=[\"']music:song[\"'][^>]*content=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            songMetaRegex.findAll(html).forEach { match ->
                val trackUrl = match.groupValues[1]
                val trackName = trackUrl.substringAfterLast('/').replace('-', ' ')
                if (trackName.isNotBlank()) {
                    tracks.add(AppleMusicTrack(title = trackName, artist = "Unknown Artist"))
                }
            }
        }

        if (tracks.isEmpty()) {
            throw Exception("Could not find any tracks in the specified Apple Music playlist.")
        }

        return AppleMusicPlaylist(
            id = playlistId,
            name = playlistName,
            tracks = tracks
        )
    }
}
