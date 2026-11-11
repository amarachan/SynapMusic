package com.synap.music.utils

import android.net.Uri
import android.os.Build
import android.text.format.DateUtils
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.synap.pulse.SynapService
import com.synap.pulse.models.bodies.ContinuationBody
import com.synap.pulse.requests.playlistPage
import com.synap.pulse.utils.plus
import com.synap.music.Database
import com.synap.music.models.Song
import kotlinx.coroutines.runBlocking

/**
 * This is our main conversion tool. It takes raw track data from the engine
 * and turns it into a Media3 "MediaItem" that Android's audio player understands.
 * We also bake in extra metadata like album IDs and track duration.
 */
fun SynapService.SongItem.asMediaItem(
    albumIdOverride: String? = null,
    albumTitleOverride: String? = null,
    albumThumbnailUrlOverride: String? = null
): MediaItem {
    val artistNames = authors?.mapNotNull { it.name } ?: emptyList<String>()
    val artistIds = authors?.mapNotNull { it.endpoint?.browseId } ?: emptyList<String>()
    val albumTitle = albumTitleOverride ?: album?.name
    val albumId = albumIdOverride ?: album?.endpoint?.browseId
    val artworkUrl = albumThumbnailUrlOverride ?: thumbnail?.url

    return MediaItem.Builder()
        .setMediaId(key)
        .setUri(key)
        .setCustomCacheKey(key)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(info?.name)
                .setArtist(artistNames.joinToString(", ").takeIf { it.isNotBlank() })
                .setAlbumTitle(albumTitle)
                .setArtworkUri(artworkUrl?.thumbnail(512)?.toUri())
                .setExtras(
                    bundleOf(
                        "albumId" to albumId,
                        "albumThumbnailUrl" to artworkUrl,
                        "durationText" to durationText,
                        "artistNames" to ArrayList(artistNames),
                        "artistIds" to ArrayList(artistIds),
                        "artistThumbnailUrl" to artworkUrl,
                    )
                )
                .build()
        )
        .build()
}

val SynapService.SongItem.asMediaItem: MediaItem
    get() = asMediaItem()

/**
 * Same as above, but specialized for video results.
 */
val SynapService.VideoItem.asMediaItem: MediaItem
    get() = MediaItem.Builder()
        .setMediaId(key)
        .setUri(key)
        .setCustomCacheKey(key)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(info?.name)
                .setArtist(authors?.joinToString(", ") { it.name ?: "" })
                .setArtworkUri(thumbnail?.url?.thumbnail(512)?.toUri())
                .setExtras(
                    bundleOf(
                        "isVideo" to true,
                        "durationText" to durationText,
                        "artistNames" to ArrayList(authors?.mapNotNull { it.name } ?: emptyList<String>()),
                        "artistIds" to ArrayList(authors?.mapNotNull { it.endpoint?.browseId } ?: emptyList<String>()),
                    )
                )
                .build()
        )
        .build()

/**
 * Converts a database "Song" model into a playable MediaItem.
 * This is used when loading songs from your local Vault or Favorites.
 * 
 * Artwork Fallback Chain:
 * 1. Song-specific thumbnail (highest priority).
 * 2. Album-specific thumbnail.
 * 3. YouTube CDN thumbnail (fallback using the song's YouTube ID).
 */
fun Song.asMediaItem(
    albumId: String? = null,
    albumTitle: String? = null,
    albumThumbnailUrl: String? = null,
    artistIds: List<String>? = null
): MediaItem {
    val artworkUrl = thumbnailUrl 
        ?: albumThumbnailUrl 
        ?: if (id.isNotBlank() && !id.startsWith("http")) "https://i.ytimg.com/vi/$id/hqdefault.jpg" else null

    return MediaItem.Builder()
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artistsText)
                .setAlbumTitle(albumTitle)
                // Apply the resolved artwork from our fallback chain.
                .setArtworkUri(artworkUrl?.thumbnail(512)?.toUri())
                .setExtras(
                    bundleOf(
                        "durationText" to durationText,
                        "albumId" to albumId,
                        "albumThumbnailUrl" to (albumThumbnailUrl ?: artworkUrl),
                        "artistIds" to (artistIds?.let { ArrayList(it) })
                    )
                )
                .build()
        )
        .setMediaId(id)
        .setUri(id)
        .setCustomCacheKey(id)
        .build()
}

val Song.asMediaItem: MediaItem
    get() = asMediaItem()

/**
 * High-Performance Thumbnail Engine.
 * YouTube's default images are often small or low quality. This function 
 * intelligently rewrites the URL to request a high-fidelity WebP version 
 * at exactly the size we need for the UI.
 */
fun String?.thumbnail(size: Int): String? {
    if (this.isNullOrBlank()) return null
    // Clamp requested dimension to 800px max to save mobile data while maintaining sharp quality
    val s = (size * 1.5).toInt().coerceIn(120, 800)
    return when {
        // Google User Content (lh3/yt3) allows dynamic resizing via URL parameters.
        contains("lh3.googleusercontent.com") || contains("yt3.ggpht.com") || contains("yt3.googleusercontent.com") || contains("googleusercontent.com") -> {
            val baseUrl = when {
                contains("=") -> substringBeforeLast("=")
                contains("-w") -> substringBeforeLast("-w")
                contains("-s") -> substringBeforeLast("-s")
                else -> this
            }
            // "-rw" forces WebP, which is much smaller but higher quality than JPEG.
            "$baseUrl=w$s-h$s-l90-rj-rw"
        }
        // Traditional YouTube CDN: We upgrade the link to the "hqdefault" WebP version.
        contains("i.ytimg.com") -> {
            val videoId = if (contains("/vi/")) {
                substringAfter("/vi/").substringBefore("/")
            } else if (contains("/vi_webp/")) {
                substringAfter("/vi_webp/").substringBefore("/")
            } else {
                return this
            }
            "https://i.ytimg.com/vi_webp/$videoId/hqdefault.webp"
        }
        else -> this
    }
}

/**
 * Quick loader for low-res placeholders. 
 * We show these instantly while the high-res WebP images are still downloading.
 */
fun String?.thumbnailLowRes(): String? {
    if (this.isNullOrBlank()) return null
    return when {
        contains("lh3.googleusercontent.com") || contains("yt3.ggpht.com") || contains("yt3.googleusercontent.com") || contains("googleusercontent.com") -> {
            val baseUrl = when {
                contains("=") -> substringBeforeLast("=")
                contains("-w") -> substringBeforeLast("-w")
                contains("-s") -> substringBeforeLast("-s")
                else -> this
            }
            "$baseUrl=s120-c-rj"
        }
        contains("i.ytimg.com") -> {
            val videoId = if (contains("/vi/")) {
                substringAfter("/vi/").substringBefore("/")
            } else if (contains("/vi_webp/")) {
                substringAfter("/vi_webp/").substringBefore("/")
            } else {
                return this
            }
            "https://i.ytimg.com/vi/$videoId/default.jpg"
        }
        else -> this
    }
}

fun Uri?.thumbnail(size: Int): Uri? {
    return this?.toString()?.thumbnail(size)?.toUri()
}

/**
 * Turns milliseconds into a "human" time format like 3:45.
 */
fun formatAsDuration(millis: Long) = DateUtils.formatElapsedTime(millis / 1000).removePrefix("0")

/**
 * This helper ensures that we fetch ALL tracks from a long playlist or album.
 * If the first request only gives us 50 tracks, this will follow the "continuation" 
 * links until the whole list is recovered.
 */
suspend fun Result<SynapService.PlaylistOrAlbumPage>.completed(): Result<SynapService.PlaylistOrAlbumPage>? {
    var playlistPage = getOrNull() ?: return null

    while (playlistPage.songsPage?.continuation != null) {
        val continuation = playlistPage.songsPage?.continuation!!
        val otherPlaylistPageResult = SynapService.playlistPage(ContinuationBody(continuation = continuation)) ?: break

        if (otherPlaylistPageResult.isFailure) break

        otherPlaylistPageResult.getOrNull()?.let { otherSongsPage ->
            playlistPage = playlistPage.copy(songsPage = playlistPage.songsPage + otherSongsPage)
        }
    }

    return Result.success(playlistPage)
}

// Quick checks for Android version features.
inline val isAtLeastAndroid6
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

inline val isAtLeastAndroid8
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

inline val isAtLeastAndroid9
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

inline val isAtLeastAndroid12
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

inline val isAtLeastAndroid13
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
