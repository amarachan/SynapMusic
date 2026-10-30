package com.synap.pulse.external

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

object MusicBrainz {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }
        defaultRequest {
            url("https://musicbrainz.org/ws/2/")
            header(HttpHeaders.UserAgent, "SynapMusic/1.0 (https://github.com/synapmusic/SynapMusic)")
            url.parameters.append("fmt", "json")
        }
    }

    @Serializable
    data class ArtistSearchResponse(val artists: List<Artist>? = null)

    @Serializable
    data class Artist(
        val id: String,
        val name: String? = null,
        val country: String? = null,
        val disambiguation: String? = null
    )

    /**
     * Searches for an artist by name to get their unique MusicBrainz ID.
     */
    suspend fun searchArtist(name: String): Result<Artist?> = runCatching {
        val response = client.get("artist") {
            parameter("query", name)
        }.body<ArtistSearchResponse>()
        
        response.artists?.firstOrNull()
    }
}
