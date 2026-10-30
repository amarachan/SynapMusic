package com.synap.music.scanner

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.synap.music.Database
import com.synap.music.models.Format
import com.synap.music.models.Song
import com.synap.music.query
import com.synap.music.utils.asMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import kotlin.math.abs

/**
 * MediaStore Local Audio File Scanner.
 * Queries Android's MediaStore for local audio files (.mp3, .flac, .wav, .m4a, .opus, .ogg)
 * and indexes them into SynapMusic's Room Database for offline playback.
 */
object LocalFolderScanner {

    /**
     * Scans local phone storage for music files and yields progress (scanned count).
     */
    fun scanLocalAudio(context: Context): Flow<Int> = flow {
        val resolver: ContentResolver = context.contentResolver
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        var scannedCount = 0

        runCatching {
            resolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                val albumArtUri = Uri.parse("content://media/external/audio/albumart")

                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataColumn) ?: continue
                    val file = File(path)
                    if (!file.exists() || file.length() <= 0) continue

                    val rawTitle = cursor.getString(titleColumn) ?: file.nameWithoutExtension
                    val rawArtist = cursor.getString(artistColumn) ?: "Local Artist"
                    val durationMs = cursor.getLong(durationColumn)
                    val albumId = cursor.getLong(albumIdColumn)

                    val artworkUri = ContentUris.withAppendedId(albumArtUri, albumId).toString()
                    val mediaId = "local:${abs(path.hashCode())}"

                    val minutes = durationMs / 1000 / 60
                    val seconds = (durationMs / 1000) % 60
                    val durationText = String.format("%d:%02d", minutes, seconds)

                    val song = Song(
                        id = mediaId,
                        title = rawTitle,
                        artistsText = if (rawArtist.contains("<unknown>", ignoreCase = true)) "Local Artist" else rawArtist,
                        durationText = durationText,
                        thumbnailUrl = artworkUri
                    )

                    val format = Format(
                        songId = mediaId,
                        contentLength = file.length()
                    )

                    query {
                        Database.insert(song.asMediaItem)
                        Database.upsert(format)
                    }

                    scannedCount++
                    emit(scannedCount)
                }
            }
        }

        emit(scannedCount)
    }.flowOn(Dispatchers.IO)
}
