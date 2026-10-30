package com.synap.music.webdav

import com.synap.music.Database
import com.synap.music.models.Playlist
import com.synap.music.models.SongPlaylistMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Data structure representing real-time progress during WebDAV directory scanning.
 */
data class WebDavSyncProgress(
    val current: Int,
    val total: Int,
    val currentFileName: String,
    val error: String? = null
)

/**
 * Engine that crawls remote WebDAV directories, converts audio files into [Song] entities,
 * and indexes them under a local "Network Drive" Room database playlist.
 */
class WebDavImportEngine {

    /**
     * Crawls WebDAV server audio files and indexes them in Room DB.
     * Emits [WebDavSyncProgress] updates during traversal.
     */
    fun syncLibrary(
        url: String,
        username: String,
        password: String
    ): Flow<WebDavSyncProgress> = flow {
        emit(WebDavSyncProgress(0, 0, "Connecting to WebDAV server..."))

        val listingResult = WebDavClient.listLibrary(url, username, password)
        val listing = listingResult.getOrElse {
            emit(WebDavSyncProgress(0, 0, "", it.message ?: "Failed to connect to WebDAV server"))
            return@flow
        }

        val audioFiles = listing.audio
        if (audioFiles.isEmpty()) {
            emit(WebDavSyncProgress(0, 0, "", "No audio files found on server"))
            return@flow
        }

        emit(WebDavSyncProgress(0, audioFiles.size, "Preparing database..."))

        // Create or clear the Network Drive playlist
        val playlistId = withContext(Dispatchers.IO) {
            Database.insert(
                Playlist(
                    name = WebDavConfig.PLAYLIST_NAME,
                    browseId = WebDavConfig.PLAYLIST_BROWSE_ID
                )
            )
        }

        audioFiles.forEachIndexed { index, entry ->
            val song = WebDavConfig.songFor(entry.url)
            emit(WebDavSyncProgress(index + 1, audioFiles.size, song.title))

            withContext(Dispatchers.IO) {
                try {
                    Database.upsert(song)
                    Database.insert(
                        SongPlaylistMap(
                            songId = song.id,
                            playlistId = playlistId,
                            position = index
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        emit(WebDavSyncProgress(audioFiles.size, audioFiles.size, "Sync completed successfully!"))
    }
}
