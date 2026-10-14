package com.synap.lyrics

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.math.abs

object LrcLib {
    @OptIn(ExperimentalSerializationApi::class)
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }
        defaultRequest {
            url("https://lrclib.net/api/")
            header(HttpHeaders.UserAgent, "SynapMusic (https://github.com/synapmusic/SynapMusic)")
        }
    }

    @Serializable
    data class LyricsResponse(
        val id: Int? = null,
        val trackName: String? = null,
        val artistName: String? = null,
        val albumName: String? = null,
        val duration: Float? = null,
        val instrumental: Boolean? = false,
        val plainLyrics: String? = null,
        val syncedLyrics: String? = null
    )

    /**
     * Fetches lyrics from LRCLIB using track metadata.
     * Tier 1: Exact Match (/api/get)
     * Tier 2: Search Fallback (/api/search) with duration tolerance for non-English / alternate titles
     */
    suspend fun getLyrics(
        artist: String,
        title: String,
        album: String? = null,
        duration: Long? = null
    ): Result<SkyLyrics.Lyrics?> = runCatching {
        // Tier 1: Exact match query (artist & title)
        val response = client.get("get") {
            parameter("artist_name", artist)
            parameter("track_name", title)
        }

        if (response.status == HttpStatusCode.OK) {
            val body = response.body<LyricsResponse>()
            val content = body.syncedLyrics ?: body.plainLyrics
            if (content != null) return@runCatching SkyLyrics.Lyrics(content)
        }

        // Tier 2: Search query fallback for non-English / alternate title formats
        val searchQueries = listOf(
            "$title $artist".trim(),
            title.trim()
        ).filter { it.isNotBlank() }.distinct()

        val queryIsLatin = title.all { it.code < 0x250 }

        for (q in searchQueries) {
            val searchResults = searchLyrics(q).getOrNull() ?: emptyList()
            
            // Candidate verification: Fuzzy match on title/artist
            val candidate = searchResults.firstOrNull { item ->
                val content = item.syncedLyrics ?: item.plainLyrics ?: ""
                val isArabicContent = content.any { it in '\u0600'..'\u06FF' }
                if (queryIsLatin && isArabicContent) return@firstOrNull false

                val durationMatches = duration != null && duration > 0 &&
                        item.duration != null && abs(item.duration - duration) <= 10

                val cleanItemTitle = item.trackName?.lowercase()?.replace(Regex("[^a-z0-9]"), "") ?: ""
                val cleanQueryTitle = title.lowercase().replace(Regex("[^a-z0-9]"), "")

                val titleMatches = cleanItemTitle.isNotBlank() && cleanQueryTitle.isNotBlank() &&
                        (cleanItemTitle.contains(cleanQueryTitle) || cleanQueryTitle.contains(cleanItemTitle))

                val artistMatches = artist.isNotBlank() && item.artistName?.contains(artist, ignoreCase = true) == true

                (titleMatches || artistMatches || durationMatches) && content.isNotBlank()
            }

            val foundContent = candidate?.syncedLyrics ?: candidate?.plainLyrics
            if (foundContent != null) {
                return@runCatching SkyLyrics.Lyrics(foundContent)
            }
        }

        null
    }

    /**
     * Searches for lyrics on LRCLIB.
     */
    suspend fun searchLyrics(query: String): Result<List<LyricsResponse>> = runCatching {
        client.get("search") {
            parameter("q", query)
        }.body<List<LyricsResponse>>()
    }
}
