package com.synap.pulse.external

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object TheAudioDB {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }
        defaultRequest {
            url("https://www.theaudiodb.com/api/v1/json/2/") // Using free test key '2'
        }
    }

    @Serializable
    data class ArtistResponse(val artists: List<Artist>? = null)

    @Serializable
    data class Artist(
        val strArtist: String? = null,
        val strArtistThumb: String? = null,
        val strArtistLogo: String? = null,
        val strArtistBanner: String? = null,
        val strBiographyEN: String? = null,
        val strGenre: String? = null
    )

    @Serializable
    data class AlbumResponse(val album: List<Album>? = null)

    @Serializable
    data class Album(
        val strAlbum: String? = null,
        val strArtist: String? = null,
        val strAlbumThumb: String? = null,
        val strAlbumThumbHQ: String? = null,
        val strDescriptionEN: String? = null,
        val intYearReleased: String? = null
    )

    @Serializable
    data class TrackResponse(val track: List<Track>? = null)

    @Serializable
    data class Track(
        val strTrack: String? = null,
        val strArtist: String? = null,
        val strAlbum: String? = null,
        val strMusicVid: String? = null,
        val strGenre: String? = null,
        val strDescriptionEN: String? = null
    )

    /**
     * Searches for a track by artist name and track title to retrieve official album/movie soundtrack name and metadata.
     */
    suspend fun getTrackInfo(artistName: String, trackName: String): Result<Track?> = runCatching {
        if (artistName.isBlank() || trackName.isBlank()) return Result.success(null)
        val response = client.get("searchtrack.php") {
            parameter("s", artistName)
            parameter("t", trackName)
        }.body<TrackResponse>()

        response.track?.firstOrNull { !it.strAlbum.isNullOrBlank() && !it.strAlbum.equals(trackName, ignoreCase = true) && !it.strAlbum.equals("Single", ignoreCase = true) } ?: response.track?.firstOrNull()
    }

    /**
     * Searches for an artist and returns high-resolution imagery and metadata.
     */
    suspend fun getArtistInfo(name: String): Result<Artist?> = runCatching {
        val response = client.get("search.php") {
            parameter("s", name)
        }.body<ArtistResponse>()
        
        response.artists?.firstOrNull()
    }

    /**
     * Searches for an album by artist name and album title to retrieve high-resolution album cover artwork.
     */
    suspend fun getAlbumInfo(artistName: String, albumTitle: String): Result<Album?> = runCatching {
        if (artistName.isBlank() || albumTitle.isBlank()) return Result.success(null)
        val response = client.get("searchalbum.php") {
            parameter("s", artistName)
            parameter("a", albumTitle)
        }.body<AlbumResponse>()

        response.album?.firstOrNull()
    }
}
