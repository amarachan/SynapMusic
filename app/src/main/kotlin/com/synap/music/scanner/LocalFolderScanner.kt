package com.synap.music.scanner

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.edit
import androidx.core.net.toUri
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
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * MediaStore Local Audio File Scanner.
 * Queries Android's MediaStore for local audio files (.mp3, .flac, .wav, .m4a, .opus, .ogg)
 * and indexes them into SynapMusic's Room Database for offline playback.
 */
object LocalFolderScanner {

    private const val PREFS_NAME = "synap_local_tracks"
    private val uriCache = ConcurrentHashMap<String, Uri>()
    private val pathCache = ConcurrentHashMap<String, String>()

    /**
     * Checks if the given videoId/mediaId belongs to a scanned local file.
     */
    fun isLocalId(id: String): Boolean = id.startsWith("local:")

    /**
     * Resolves the playable MediaStore [Uri] for a given local track ID.
     * Uses in-memory caching, SharedPreferences persistence, and on-demand
     * MediaStore resolution to handle tracks across app restarts and re-scans.
     */
    fun resolveContentUri(context: Context, localId: String): Uri? {
        if (!isLocalId(localId)) return null

        // 1. Fast in-memory cache hit
        uriCache[localId]?.let { return it }

        // 2. Persistent SharedPreferences lookup
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUri = prefs.getString("uri_$localId", null)
        if (!savedUri.isNullOrBlank()) {
            val uri = savedUri.toUri()
            uriCache[localId] = uri
            return uri
        }

        // 3. Fallback: Query MediaStore to locate the track by path hash
        val resolver: ContentResolver = context.contentResolver
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
        )

        runCatching {
            resolver.query(uri, projection, null, null, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                prefs.edit {
                    while (cursor.moveToNext()) {
                        val path = cursor.getString(dataColumn) ?: continue
                        val mediaStoreId = cursor.getLong(idColumn)
                        val currentId = "local:${abs(path.hashCode())}"
                        val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaStoreId)
                        uriCache[currentId] = contentUri
                        pathCache[currentId] = path
                        putString("uri_$currentId", contentUri.toString())
                        putString("path_$currentId", path)
                    }
                }
            }
        }

        return uriCache[localId]
    }

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
            MediaStore.Audio.Media.ALBUM_ID,
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var scannedCount = 0

        runCatching {
            resolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                val albumArtUri = "content://media/external/audio/albumart".toUri()

                prefs.edit {
                    while (cursor.moveToNext()) {
                        val path = cursor.getString(dataColumn) ?: continue
                        val file = File(path)
                        if ((!file.exists()) || (file.length() <= 0)) continue

                        val rawTitle = cursor.getString(titleColumn) ?: file.nameWithoutExtension
                        val rawArtist = cursor.getString(artistColumn) ?: "Local Artist"
                        val durationMs = cursor.getLong(durationColumn)
                        val albumId = cursor.getLong(albumIdColumn)
                        val mediaStoreId = cursor.getLong(idColumn)

                        val artworkUri = ContentUris.withAppendedId(albumArtUri, albumId).toString()
                        val mediaId = "local:${abs(path.hashCode())}"
                        val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaStoreId)

                        // Cache for instant playback resolution
                        uriCache[mediaId] = contentUri
                        pathCache[mediaId] = path
                        putString("uri_$mediaId", contentUri.toString())
                        putString("path_$mediaId", path)

                        val minutes = durationMs / 1000 / 60
                        val seconds = (durationMs / 1000) % 60
                        val durationText = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)

                        val song = Song(
                            id = mediaId,
                            title = rawTitle,
                            artistsText = if (rawArtist.contains("<unknown>", ignoreCase = true)) "Local Artist" else rawArtist,
                            durationText = durationText,
                            thumbnailUrl = artworkUri,
                        )

                        val format = Format(
                            songId = mediaId,
                            contentLength = file.length(),
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
        }

        emit(scannedCount)
    }.flowOn(Dispatchers.IO)
}
