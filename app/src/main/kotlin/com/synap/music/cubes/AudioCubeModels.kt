package com.synap.music.cubes

import com.synap.music.enums.CompressionEngine
import com.synap.music.models.*
import kotlinx.serialization.Serializable

/**
 * SynapCube (.synapcube) Data Models
 * 
 * SynapCube is a portable backup and sharing format for SynapMusic. It allows users 
 * to export a playlist (or their entire library) into a single archive file. 
 * This file contains both the physical audio files and the exact relational database 
 * structures (albums, artists, lyrics).
 * 
 * We use `kotlinx.serialization` to convert these DTOs to JSON, which is then
 * compressed using the Zstandard (Zstd) algorithm for maximum efficiency before being 
 * packaged alongside the audio files in a standard ZIP container.
 */
@Serializable
data class CubeMetadata(
    val cubeName: String,
    val orderedSongIds: List<String>,
    val songs: List<CubeSong>,
    val albums: List<CubeAlbum>,
    val artists: List<CubeArtist>,
    val lyrics: List<CubeLyrics>,
    val formats: List<CubeFormat>,
    val songAlbumMaps: List<CubeSongAlbumMap>,
    val songArtistMaps: List<CubeSongArtistMap>
)

@Serializable
data class CubeSong(
    val id: String,
    val title: String,
    val artistsText: String? = null,
    val durationText: String?,
    val thumbnailUrl: String?
)

@Serializable
data class CubeAlbum(
    val id: String,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val year: String? = null,
    val authorsText: String? = null,
    val shareUrl: String? = null
)

@Serializable
data class CubeArtist(
    val id: String,
    val name: String? = null,
    val thumbnailUrl: String? = null
)

@Serializable
data class CubeLyrics(
    val songId: String,
    val fixed: String?,
    val synced: String?
)

@Serializable
data class CubeFormat(
    val songId: String,
    val itag: Int? = null,
    val mimeType: String? = null,
    val bitrate: Long? = null,
    val contentLength: Long? = null,
    val loudnessDb: Float? = null,
    val compressionEngine: CompressionEngine? = null
)

@Serializable
data class CubeSongAlbumMap(
    val songId: String,
    val albumId: String,
    val position: Int?
)

@Serializable
data class CubeSongArtistMap(
    val songId: String,
    val artistId: String
)

// Mapping extensions

fun Song.toCubeSong() = CubeSong(
    id = id,
    title = title,
    artistsText = artistsText,
    durationText = durationText,
    thumbnailUrl = thumbnailUrl
)

fun CubeSong.toSong() = Song(
    id = id,
    title = title,
    artistsText = artistsText,
    durationText = durationText,
    thumbnailUrl = thumbnailUrl,
    likedAt = null,
    totalPlayTimeMs = 0L
)

fun Album.toCubeAlbum() = CubeAlbum(
    id = id,
    title = title,
    thumbnailUrl = thumbnailUrl,
    year = year,
    authorsText = authorsText,
    shareUrl = shareUrl
)

fun CubeAlbum.toAlbum() = Album(
    id = id,
    title = title,
    thumbnailUrl = thumbnailUrl,
    year = year,
    authorsText = authorsText,
    shareUrl = shareUrl,
    timestamp = System.currentTimeMillis(),
    bookmarkedAt = null
)

fun Artist.toCubeArtist() = CubeArtist(
    id = id,
    name = name,
    thumbnailUrl = thumbnailUrl
)

fun CubeArtist.toArtist() = Artist(
    id = id,
    name = name,
    thumbnailUrl = thumbnailUrl,
    timestamp = System.currentTimeMillis(),
    bookmarkedAt = null
)

fun Lyrics.toCubeLyrics() = CubeLyrics(
    songId = songId,
    fixed = fixed,
    synced = synced
)

fun CubeLyrics.toLyrics() = Lyrics(
    songId = songId,
    fixed = fixed,
    synced = synced
)

fun Format.toCubeFormat() = CubeFormat(
    songId = songId,
    itag = itag,
    mimeType = mimeType,
    bitrate = bitrate,
    contentLength = contentLength,
    loudnessDb = loudnessDb,
    compressionEngine = compressionEngine
)

fun CubeFormat.toFormat() = Format(
    songId = songId,
    itag = itag,
    mimeType = mimeType,
    bitrate = bitrate,
    contentLength = contentLength,
    lastModified = System.currentTimeMillis(),
    loudnessDb = loudnessDb,
    compressionEngine = compressionEngine
)

fun SongAlbumMap.toCubeSongAlbumMap() = CubeSongAlbumMap(
    songId = songId,
    albumId = albumId,
    position = position
)

fun CubeSongAlbumMap.toSongAlbumMap() = SongAlbumMap(
    songId = songId,
    albumId = albumId,
    position = position
)

fun SongArtistMap.toCubeSongArtistMap() = CubeSongArtistMap(
    songId = songId,
    artistId = artistId
)

fun CubeSongArtistMap.toSongArtistMap() = SongArtistMap(
    songId = songId,
    artistId = artistId
)
