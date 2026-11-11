package com.synap.music.engine

import com.synap.music.Database
import com.synap.music.models.Playlist
import com.synap.music.models.SongPlaylistMap
import com.synap.music.network.AppleMusicClient
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
 * Conversion engine that imports Apple Music playlists into SynapMusic.
 * Fetches Apple Music track metadata, queries YouTube Music for matching audio streams,
 * and bulk inserts resolved tracks into Room database local playlists.
 */
class AppleMusicImportEngine(
    private val client: AppleMusicClient = AppleMusicClient()
) {

    /**
     * Fetches Apple Music playlist tracks, sequentially queries YouTube Music for matching songs,
     * and saves the resulting playlist directly into Room DB.
     * Emits [ImportProgress] updates during conversion.
     */
    fun importPlaylist(url: String): Flow<ImportProgress> = flow {
        emit(ImportProgress(0, 0, "Fetching playlist from Apple Music..."))

        try {
            val playlist = client.getPlaylist(url)
            val tracks = playlist.tracks
            val totalTracks = tracks.size

            if (totalTracks == 0) {
                emit(ImportProgress(0, 0, "", "Playlist is empty"))
                return@flow
            }

            emit(ImportProgress(0, totalTracks, "Creating playlist in library..."))

            // 1. Create Local Playlist in DB
            val localPlaylistId = withContext(Dispatchers.IO) {
                Database.insert(
                    Playlist(
                        name = playlist.name,
                        browseId = "applemusic:${playlist.id}"
                    )
                )
            }

            // 2. Match & Insert Each Track on YouTube Music
            tracks.forEachIndexed { index, track ->
                val query = "${track.title} ${track.artist}"
                emit(ImportProgress(index + 1, totalTracks, track.title))

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

            emit(ImportProgress(totalTracks, totalTracks, "Done!"))

        } catch (e: Exception) {
            e.printStackTrace()
            emit(ImportProgress(0, 0, "", e.message ?: "An error occurred during import"))
        }
    }
}
