package com.synap.music.utils

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import android.util.Log
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString

@Serializable
data class SponsorBlockSegment(
    val category: String,
    val segment: List<Float>,
    val UUID: String
) {
    val start: Long get() = (segment[0] * 1000).toLong()
    val end: Long get() = (segment[1] * 1000).toLong()
}

/**
 * SponsorBlock API client for retrieving non-music segment timestamps (intros, promos, sponsor sections).
 */
object SponsorBlock {
    private const val TAG = "SponsorBlock"

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    // These are the types of segments we care about for a music app.
    private val categories = listOf(
        "sponsor",
        "intro",
        "outro",
        "interaction",
        "selfpromo",
        "music_offtopic" // Very useful for skipping talking in music videos.
    )

    /**
     * Queries the API for skip segments associated with a YouTube Video ID.
     */
    suspend fun getSegments(videoId: String): List<SponsorBlockSegment> {
        return try {
            val url = "https://sponsor.ajay.app/api/skipSegments"
            val response: List<SponsorBlockSegment> = client.get(url) {
                parameter("videoID", videoId)
                parameter("categories", json.encodeToString(categories))
            }.body()
            response
        } catch (e: Exception) {
            // 404 just means nobody has tagged this video yet. It's normal.
            if (e.message?.contains("404") != true) {
                Log.d(TAG, "No segments or error for $videoId: ${e.message}")
            }
            emptyList()
        }
    }
}
