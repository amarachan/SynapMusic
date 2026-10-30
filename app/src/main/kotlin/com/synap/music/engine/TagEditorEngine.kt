package com.synap.music.engine

import android.content.Context
import android.net.Uri
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.synap.music.Database
import com.synap.music.query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ID3 Audio Tag & Artwork Editor Engine.
 * Updates song metadata, persists custom cover art from phone gallery, and clears image cache.
 */
object TagEditorEngine {

    /**
     * Updates song title, artist, album, and custom cover artwork in Room Database and local storage.
     */
    @OptIn(ExperimentalCoilApi::class)
    suspend fun updateSongTags(
        context: Context,
        songId: String,
        newTitle: String,
        newArtist: String,
        customArtworkUri: Uri? = null
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val existingSong = Database.song(songId).firstOrNull() ?: return@withContext false

            var finalArtworkPath: String? = existingSong.thumbnailUrl

            if (customArtworkUri != null) {
                val artworkDir = File(context.filesDir, "artwork").apply {
                    if (!exists()) mkdirs()
                }
                val destFile = File(artworkDir, "art_${songId.hashCode()}.jpg")

                context.contentResolver.openInputStream(customArtworkUri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                if (destFile.exists() && destFile.length() > 0) {
                    finalArtworkPath = destFile.absolutePath
                }
            }

            val updatedSong = existingSong.copy(
                title = newTitle.ifBlank { existingSong.title },
                artistsText = newArtist.ifBlank { existingSong.artistsText },
                thumbnailUrl = finalArtworkPath
            )

            query {
                Database.upsert(updatedSong)
            }

            // Evict Coil image memory & disk cache for instant UI refresh
            runCatching {
                context.imageLoader.diskCache?.clear()
                context.imageLoader.memoryCache?.clear()
            }

            true
        }.getOrDefault(false)
    }
}
