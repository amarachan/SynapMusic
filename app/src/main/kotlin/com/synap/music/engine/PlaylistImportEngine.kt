package com.synap.music.engine

import com.synap.music.Database
import com.synap.music.models.Playlist
import com.synap.music.models.SongPlaylistMap
import com.synap.music.network.SpotifyClient
import com.synap.music.utils.asMediaItem
import com.synap.pulse.SynapService
import com.synap.pulse.models.bodies.SearchBody
import com.synap.pulse.requests.searchPage
import com.synap.pulse.utils.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Data structure representing real-time import progress during playlist conversions.
 */
data class ImportProgress(
    val current: Int,
    val total: Int,
    val currentTrackName: String,
    val error: String? = null
)

/**
 * Conversion engine that imports Spotify playlists into SynapMusic.
 * Fetches Spotify track metadata, queries YouTube Music for matching audio streams,
 * and bulk inserts resolved tracks into Room database local playlists.
 */
class PlaylistImportEngine(
    private val spotifyClient: SpotifyClient = SpotifyClient()
) {
    /**
     * Extracts the 22-character Playlist ID from a standard Spotify playlist URL string.
     * Example: https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M -> 37i9dQZF1DXcBWIGoYBM5M
     */
    fun extractPlaylistId(url: String): String? {
        val regex = Regex("playlist/([a-zA-Z0-9]+)")
        val match = regex.find(url)
        return match?.groupValues?.get(1)
    }

    /**
     * Fetches tracklists from Spotify, sequentially searches YouTube Music for matching songs,
     * and saves the resulting playlist directly into the local Room database.
     * Emits [ImportProgress] updates for UI progress indicators.
     */
    fun importPlaylist(spotifyUrl: String): Flow<ImportProgress> = flow {
        val playlistId = extractPlaylistId(spotifyUrl)
        if (playlistId == null) {
            emit(ImportProgress(0, 0, "", "Invalid Spotify Playlist URL"))
            return@flow
        }

        try {
            // 1. Fetch Playlist Metadata from Spotify API
            val playlist = spotifyClient.getPlaylist(playlistId)
            var totalTracks = playlist.tracks.items.size
            
            emit(ImportProgress(0, totalTracks, "Fetching from Spotify..."))

            // 2. Paginate to retrieve full tracklist
            val allTracks = playlist.tracks.items.toMutableList()
            var currentOffset = allTracks.size
            var nextUrl = playlist.tracks.next
            while (nextUrl != null) {
                val nextTracks = spotifyClient.getPlaylistTracks(playlistId, currentOffset)
                allTracks.addAll(nextTracks.items)
                currentOffset += nextTracks.items.size
                nextUrl = nextTracks.next
            }
            
            totalTracks = allTracks.size
            
            if (totalTracks == 0) {
                emit(ImportProgress(0, 0, "", "Playlist is empty"))
                return@flow
            }

            // 3. Create Local Playlist Entity in Database
            val localPlaylistId = withContext(Dispatchers.IO) {
                Database.insert(Playlist(name = playlist.name, browseId = "spotify:$playlistId"))
            }

            // 4. Match & Map Each Song on YouTube Music
            allTracks.forEachIndexed { index, item ->
                val track = item.track
                if (track != null) {
                    val artists = track.artists.joinToString(" ") { it.name }
                    val query = "${track.name} $artists"
                    
                    emit(ImportProgress(index + 1, totalTracks, track.name))

                    withContext(Dispatchers.IO) {
                        try {
                            val searchResult = SynapService.searchPage(
                                body = SearchBody(query = query, params = SynapService.SearchFilter.Song.value),
                                fromMusicShelfRendererContent = SynapService.SongItem.Companion::from
                            )?.getOrNull()

                            val bestMatch = searchResult?.list?.firstOrNull()
                            if (bestMatch != null) {
                                val mediaItem = bestMatch.asMediaItem
                                Database.insert(mediaItem)
                                Database.insert(
                                    SongPlaylistMap(
                                        songId = mediaItem.mediaId,
                                        playlistId = localPlaylistId,
                                        position = index
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            emit(ImportProgress(totalTracks, totalTracks, "Done!"))

        } catch (e: Exception) {
            e.printStackTrace()
            emit(ImportProgress(0, 0, "", e.message ?: "An error occurred during import"))
        }
    }
}
