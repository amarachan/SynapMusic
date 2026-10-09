package com.synap.music.utils

import androidx.media3.common.MediaItem
import com.synap.pulse.SynapService
import com.synap.pulse.requests.lyrics
import com.synap.pulse.models.bodies.NextBody
import com.synap.pulse.models.bodies.ContinuationBody
import com.synap.pulse.requests.nextPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The YouTube Radio engine.
 * This class handles the logic for "Endless Playback". It fetches a batch of songs
 * based on a seed (like a single track or an artist), and continues to fetch more
 * in the background as you listen.
 */
data class YouTubeRadio(
    private val videoId: String? = null,
    private var playlistId: String? = null,
    private var playlistSetVideoId: String? = null,
    private var parameters: String? = null,
    // If set, we'll filter out any songs that don't belong to this artist.
    // Useful for "Artist Radios" to prevent unrelated trending hits from popping in.
    private val strictArtistName: String? = null 
) {
    private var nextContinuation: String? = null

    /**
     * Fetches the next batch of songs from the server.
     */
    suspend fun process(): List<MediaItem> {
        var mediaItems: List<MediaItem>? = null

        nextContinuation = withContext(Dispatchers.IO) {
            val continuation = nextContinuation

            if (continuation == null) {
               // First fetch: use the seed Video/Playlist ID.
               SynapService.nextPage(
                    NextBody(
                        videoId = videoId,
                        playlistId = playlistId,
                        params = parameters,
                        playlistSetVideoId = playlistSetVideoId
                    )
                )?.map { nextResult ->
                    playlistId = nextResult.playlistId
                    parameters = nextResult.params
                    playlistSetVideoId = nextResult.playlistSetVideoId

                    nextResult.itemsPage
                }
            } else {
                // Subsequent fetches: use the continuation token from the last response.
                SynapService.nextPage(ContinuationBody(continuation = continuation))
            }?.getOrNull()?.let { songsPage ->
                val items = songsPage.list ?: emptyList()
                
                /**
                 * Strict Artist Filtering.
                 * YouTube sometimes adds "Trending" or "Recommended" songs to radios 
                 * that have nothing to do with the original artist. We check the 
                 * names here and throw away the intruders if strict mode is on.
                 */
                val filteredItems = if (strictArtistName != null) {
                    items.filter { song ->
                        val artistMatch = song.authors?.any { 
                            it.name?.contains(strictArtistName, ignoreCase = true) == true 
                        } ?: false
                        artistMatch
                    }
                } else {
                    items
                }
                
                mediaItems = (if (filteredItems.isEmpty() && strictArtistName != null) {
                    // Safety: if the filter was too aggressive and removed everything, 
                    // keep one song so the player doesn't just stop.
                    items.take(1)
                } else {
                    filteredItems
                }).map(SynapService.SongItem::asMediaItem)

                // Update the token for the next batch.
                songsPage.continuation?.takeUnless { nextContinuation == it }
            }

        }

        return mediaItems ?: emptyList()
    }
}
