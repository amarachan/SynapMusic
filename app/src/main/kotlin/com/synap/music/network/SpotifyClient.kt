package com.synap.music.network

import android.util.Base64
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.Parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Data response model for Spotify OAuth 2.0 Client Credentials token requests.
 */
@Serializable
data class SpotifyTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int
)

/**
 * Represents a Spotify playlist payload from GET /v1/playlists/{id}.
 */
@Serializable
data class SpotifyPlaylist(
    val id: String,
    val name: String,
    val description: String?,
    val owner: SpotifyOwner?,
    val tracks: SpotifyTracksInfo
)

/**
 * Represents the owner profile of a Spotify playlist.
 */
@Serializable
data class SpotifyOwner(
    @SerialName("display_name") val displayName: String?
)

/**
 * Container for paginated track items inside a Spotify playlist.
 */
@Serializable
data class SpotifyTracksInfo(
    val items: List<SpotifyPlaylistTrackItem>,
    val next: String?
)

/**
 * Wrapper for individual track items returned in playlist responses.
 */
@Serializable
data class SpotifyPlaylistTrackItem(
    val track: SpotifyTrack?
)

/**
 * Represents metadata for an individual Spotify track.
 */
@Serializable
data class SpotifyTrack(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtist>,
    val album: SpotifyAlbum,
    @SerialName("duration_ms") val durationMs: Long
)

/**
 * Represents an artist associated with a Spotify track.
 */
@Serializable
data class SpotifyArtist(
    val name: String
)

/**
 * Represents album details for a Spotify track.
 */
@Serializable
data class SpotifyAlbum(
    val name: String,
    val images: List<SpotifyImage>?
)

/**
 * Artwork image metadata for Spotify albums.
 */
@Serializable
data class SpotifyImage(
    val url: String,
    val height: Int?,
    val width: Int?
)

/**
 * Client for interacting with the official Spotify Web API.
 * Uses OAuth 2.0 Client Credentials flow to retrieve public playlist details and tracklists.
 */
class SpotifyClient(
    private val clientId: String = "e5033c5e8f494191ba5fdf895183db7e",
    private val clientSecret: String = "61d9a0cd89a84aebba49a8de86cb3cb4"
) {
    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private var accessToken: String? = null
    private var tokenExpiryTime: Long = 0
    private val tokenMutex = Mutex()

    /**
     * Obtains or reuses a valid Spotify OAuth 2.0 Client Credentials bearer token.
     */
    private suspend fun getAccessToken(): String {
        tokenMutex.withLock {
            if (accessToken != null && System.currentTimeMillis() < tokenExpiryTime - 60000) {
                return accessToken!!
            }

            val credentials = Base64.encodeToString("$clientId:$clientSecret".toByteArray(), Base64.NO_WRAP)
            val response: SpotifyTokenResponse = httpClient.submitForm(
                url = "https://accounts.spotify.com/api/token",
                formParameters = Parameters.build {
                    append("grant_type", "client_credentials")
                }
            ) {
                header("Authorization", "Basic $credentials")
            }.body()

            accessToken = response.accessToken
            tokenExpiryTime = System.currentTimeMillis() + (response.expiresIn * 1000L)
            return accessToken!!
        }
    }

    /**
     * Retrieves playlist metadata and initial tracklist for a given Spotify playlist ID.
     */
    suspend fun getPlaylist(playlistId: String): SpotifyPlaylist {
        val token = getAccessToken()
        return httpClient.get("https://api.spotify.com/v1/playlists/$playlistId") {
            header("Authorization", "Bearer $token")
        }.body()
    }
    
    /**
     * Retrieves paginated track items from a Spotify playlist.
     */
    suspend fun getPlaylistTracks(playlistId: String, offset: Int = 0): SpotifyTracksInfo {
        val token = getAccessToken()
        return httpClient.get("https://api.spotify.com/v1/playlists/$playlistId/tracks") {
            header("Authorization", "Bearer $token")
            parameter("offset", offset)
            parameter("limit", 100)
        }.body()
    }
}
