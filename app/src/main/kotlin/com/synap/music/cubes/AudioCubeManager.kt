package com.synap.music.cubes

import android.content.Context
import android.net.Uri
import android.util.Log
import com.github.luben.zstd.Zstd
import com.synap.music.Database
import com.synap.music.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import com.synap.music.enums.CubeCompressionLevel
import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * AudioCubeManager (SynapCube Core Engine)
 * 
 * Handles the creation (export) and extraction (import) of `.synapcube` archives.
 * 
 * A SynapCube file is an encrypted BlackBox container containing:
 * 1. `metadata.json.zst` - A Zstd-compressed JSON string of the `CubeMetadata` object.
 * 2. `audio/` - A directory containing the raw audio files (FLAC, OPUS, M4A, etc.).
 * 
 * The entire stream is wrapped in SynapMusic BlackBox stream obfuscation, preventing
 * third-party zip utilities (WinRAR, 7-Zip, etc.) or media scanners from inspecting 
 * or altering the archive.
 */
object AudioCubeManager {
    private const val TAG = "AudioCubeManager"

    private val BLACKBOX_MAGIC = "SYNAPCUBE_10".toByteArray(Charsets.UTF_8)
    private val BLACKBOX_KEY = "SynapMusic_AudioCube_Secret_Container_Key_2026".toByteArray(Charsets.UTF_8)

    private class BlackBoxOutputStream(private val out: OutputStream) : OutputStream() {
        private var index = 0
        override fun write(b: Int) {
            val keyByte = BLACKBOX_KEY[index % BLACKBOX_KEY.size].toInt()
            out.write(b xor keyByte)
            index++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            val obfuscated = ByteArray(len)
            for (i in 0 until len) {
                val keyByte = BLACKBOX_KEY[(index + i) % BLACKBOX_KEY.size].toInt()
                obfuscated[i] = (b[off + i].toInt() xor keyByte).toByte()
            }
            out.write(obfuscated, 0, len)
            index += len
        }

        override fun flush() { out.flush() }
        override fun close() { out.close() }
    }

    private class BlackBoxInputStream(private val input: InputStream) : InputStream() {
        private var index = 0
        override fun read(): Int {
            val b = input.read()
            if (b == -1) return -1
            val keyByte = BLACKBOX_KEY[index % BLACKBOX_KEY.size].toInt()
            val deobfuscated = b xor keyByte
            index++
            return deobfuscated
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val count = input.read(b, off, len)
            if (count <= 0) return count
            for (i in 0 until count) {
                val keyByte = BLACKBOX_KEY[(index + i) % BLACKBOX_KEY.size].toInt()
                b[off + i] = (b[off + i].toInt() xor keyByte).toByte()
            }
            index += count
            return count
        }

        override fun close() { input.close() }
    }

    private fun getAudioDir(context: Context): File {
        return File(context.filesDir, "vault").apply {
            if (!exists()) mkdirs()
        }
    }

    private fun getCompressionParams(context: Context, level: CubeCompressionLevel): Pair<Int, Int> {
        return when (level) {
            CubeCompressionLevel.Low -> 1 to 0
            CubeCompressionLevel.Medium -> 3 to 0
            CubeCompressionLevel.High -> 9 to 5
            CubeCompressionLevel.Extreme -> 19 to 9
            CubeCompressionLevel.Auto -> {
                val freeSpaceMb = context.filesDir.usableSpace / (1024 * 1024)
                when {
                    freeSpaceMb < 1000 -> 19 to 9  // Under 1GB -> Extreme
                    freeSpaceMb < 5000 -> 9 to 5   // Under 5GB -> High
                    else -> 3 to 0                 // Plenty of space -> Medium (fastest)
                }
            }
        }
    }

    /**
     * Exports the user's entire offline library (Vault) into a single SynapCube file.
     * Iterates over all downloaded formats, gathers relational data, compresses
     * the JSON metadata via Zstd, and zips it up with the audio files.
     */
    suspend fun exportLibraryToCube(
        context: Context,
        uri: Uri,
        compressionLevel: CubeCompressionLevel = CubeCompressionLevel.Auto
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Get all songs that have been downloaded (have a format)
            val formatIds = Database.allFormats().map { it.songId }.distinct()
            val songs = formatIds.mapNotNull { Database.getSong(it) }

            val cubeSongs = mutableListOf<CubeSong>()
            val cubeAlbums = mutableListOf<CubeAlbum>()
            val cubeArtists = mutableListOf<CubeArtist>()
            val cubeLyrics = mutableListOf<CubeLyrics>()
            val cubeFormats = mutableListOf<CubeFormat>()
            val cubeSongAlbumMaps = mutableListOf<CubeSongAlbumMap>()
            val cubeSongArtistMaps = mutableListOf<CubeSongArtistMap>()

            val audioDir = getAudioDir(context)
            val filesToPack = mutableListOf<File>()

            for (song in songs) {
                cubeSongs.add(song.toCubeSong())

                // Format
                Database.getFormat(song.id)?.let { format ->
                    cubeFormats.add(format.toCubeFormat())
                }

                // Lyrics
                Database.getLyrics(song.id)?.let { lyrics ->
                    cubeLyrics.add(lyrics.toCubeLyrics())
                }

                // Albums
                Database.getSongAlbumsSync(song.id).forEach { album ->
                    if (cubeAlbums.none { it.id == album.id }) {
                        cubeAlbums.add(album.toCubeAlbum())
                    }
                    val pos = Database.getAlbumPosition(song.id, album.id)
                    cubeSongAlbumMaps.add(CubeSongAlbumMap(song.id, album.id, pos))
                }

                // Artists
                Database.getSongArtistsSync(song.id).forEach { artist ->
                    if (cubeArtists.none { it.id == artist.id }) {
                        cubeArtists.add(artist.toCubeArtist())
                    }
                    cubeSongArtistMaps.add(CubeSongArtistMap(song.id, artist.id))
                }

                // Find audio file
                val possibleExtensions = listOf("mkv", "flac", "m4a", "opus", "mp3", "ogg", "wv", "mp4")
                for (ext in possibleExtensions) {
                    val file = File(audioDir, "${song.id}.$ext")
                    if (file.exists() && file.length() > 0) {
                        filesToPack.add(file)
                        break
                    }
                }
            }

            val metadata = CubeMetadata(
                cubeName = "Full Library Backup",
                orderedSongIds = songs.map { it.id },
                songs = cubeSongs,
                albums = cubeAlbums,
                artists = cubeArtists,
                lyrics = cubeLyrics,
                formats = cubeFormats,
                songAlbumMaps = cubeSongAlbumMaps,
                songArtistMaps = cubeSongArtistMaps
            )

            val jsonString = Json.encodeToString(metadata)
            val (zstdLevel, zipLevel) = getCompressionParams(context, compressionLevel)
            val compressedJson = Zstd.compress(jsonString.toByteArray(Charsets.UTF_8), zstdLevel)

            context.contentResolver.openOutputStream(uri)?.use { rawOs ->
                rawOs.write(BLACKBOX_MAGIC)
                val blackBoxOs = BlackBoxOutputStream(rawOs)
                ZipOutputStream(BufferedOutputStream(blackBoxOs)).use { zos ->
                    // 1. Write Metadata
                    val metaEntry = ZipEntry("metadata.json.zst")
                    zos.putNextEntry(metaEntry)
                    zos.write(compressedJson)
                    zos.closeEntry()

                    // 2. Write Audio Files
                    for (file in filesToPack) {
                        val entry = ZipEntry("audio/${file.name}")
                        zos.setLevel(zipLevel) 
                        zos.putNextEntry(entry)
                        
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export cube", e)
            Result.failure(e)
        }
    }

    /**
     * Exports a specific Playlist into a single SynapCube file.
     * Identical mechanism to full library export, but isolated to tracks mapped
     * to a given `playlistId`.
     */
    suspend fun exportPlaylistToCube(
        context: Context,
        playlistId: Long,
        uri: Uri,
        compressionLevel: CubeCompressionLevel = CubeCompressionLevel.Auto
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val playlist = Database.getPlaylistSync(playlistId)
                ?: return@withContext Result.failure(Exception("Playlist not found"))
            
            val songs = Database.getPlaylistSongsSync(playlistId)
            
            val cubeSongs = mutableListOf<CubeSong>()
            val cubeAlbums = mutableListOf<CubeAlbum>()
            val cubeArtists = mutableListOf<CubeArtist>()
            val cubeLyrics = mutableListOf<CubeLyrics>()
            val cubeFormats = mutableListOf<CubeFormat>()
            val cubeSongAlbumMaps = mutableListOf<CubeSongAlbumMap>()
            val cubeSongArtistMaps = mutableListOf<CubeSongArtistMap>()

            val audioDir = getAudioDir(context)
            val filesToPack = mutableListOf<File>()

            for (song in songs) {
                cubeSongs.add(song.toCubeSong())

                // Format
                Database.getFormat(song.id)?.let { format ->
                    cubeFormats.add(format.toCubeFormat())
                }

                // Lyrics
                Database.getLyrics(song.id)?.let { lyrics ->
                    cubeLyrics.add(lyrics.toCubeLyrics())
                }

                // Albums
                Database.getSongAlbumsSync(song.id).forEach { album ->
                    if (cubeAlbums.none { it.id == album.id }) {
                        cubeAlbums.add(album.toCubeAlbum())
                    }
                    val pos = Database.getAlbumPosition(song.id, album.id)
                    cubeSongAlbumMaps.add(CubeSongAlbumMap(song.id, album.id, pos))
                }

                // Artists
                Database.getSongArtistsSync(song.id).forEach { artist ->
                    if (cubeArtists.none { it.id == artist.id }) {
                        cubeArtists.add(artist.toCubeArtist())
                    }
                    cubeSongArtistMaps.add(CubeSongArtistMap(song.id, artist.id))
                }

                // Find audio file
                val possibleExtensions = listOf("mkv", "flac", "m4a", "opus", "mp3", "ogg", "wv", "mp4")
                for (ext in possibleExtensions) {
                    val file = File(audioDir, "${song.id}.$ext")
                    if (file.exists() && file.length() > 0) {
                        filesToPack.add(file)
                        break
                    }
                }
            }

            val metadata = CubeMetadata(
                cubeName = playlist.name,
                orderedSongIds = songs.map { it.id },
                songs = cubeSongs,
                albums = cubeAlbums,
                artists = cubeArtists,
                lyrics = cubeLyrics,
                formats = cubeFormats,
                songAlbumMaps = cubeSongAlbumMaps,
                songArtistMaps = cubeSongArtistMaps
            )

            val jsonString = Json.encodeToString(metadata)
            val (zstdLevel, zipLevel) = getCompressionParams(context, compressionLevel)
            val compressedJson = Zstd.compress(jsonString.toByteArray(Charsets.UTF_8), zstdLevel)

            context.contentResolver.openOutputStream(uri)?.use { rawOs ->
                rawOs.write(BLACKBOX_MAGIC)
                val blackBoxOs = BlackBoxOutputStream(rawOs)
                ZipOutputStream(BufferedOutputStream(blackBoxOs)).use { zos ->
                    // 1. Write Metadata
                    val metaEntry = ZipEntry("metadata.json.zst")
                    zos.putNextEntry(metaEntry)
                    zos.write(compressedJson)
                    zos.closeEntry()

                    // 2. Write Audio Files
                    for (file in filesToPack) {
                        val entry = ZipEntry("audio/${file.name}")
                        // STORED requires setting size, crc, and compressedSize manually if not using deflater
                        // But to keep it simple, we can just use default compression (DEFLATED) with our calculated level
                        zos.setLevel(zipLevel) 
                        zos.putNextEntry(entry)
                        
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export cube", e)
            Result.failure(e)
        }
    }

    /**
     * Exports a list of selected Songs into a single `.synapcube` file.
     */
    suspend fun exportSongsToCube(
        context: Context,
        songIds: List<String>,
        uri: Uri,
        compressionLevel: CubeCompressionLevel = CubeCompressionLevel.Auto
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val songs = songIds.mapNotNull { Database.getSong(it) }
            if (songs.isEmpty()) throw Exception("No songs found to export")

            val cubeSongs = mutableListOf<CubeSong>()
            val cubeAlbums = mutableListOf<CubeAlbum>()
            val cubeArtists = mutableListOf<CubeArtist>()
            val cubeLyrics = mutableListOf<CubeLyrics>()
            val cubeFormats = mutableListOf<CubeFormat>()
            val cubeSongAlbumMaps = mutableListOf<CubeSongAlbumMap>()
            val cubeSongArtistMaps = mutableListOf<CubeSongArtistMap>()

            val audioDir = getAudioDir(context)
            val filesToPack = mutableListOf<File>()

            for (song in songs) {
                cubeSongs.add(song.toCubeSong())

                // Format
                Database.getFormat(song.id)?.let { format ->
                    cubeFormats.add(format.toCubeFormat())
                }

                // Lyrics
                Database.getLyrics(song.id)?.let { lyrics ->
                    cubeLyrics.add(lyrics.toCubeLyrics())
                }

                // Albums
                Database.getSongAlbumsSync(song.id).forEach { album ->
                    if (cubeAlbums.none { it.id == album.id }) {
                        cubeAlbums.add(album.toCubeAlbum())
                    }
                    val pos = Database.getAlbumPosition(song.id, album.id)
                    cubeSongAlbumMaps.add(CubeSongAlbumMap(song.id, album.id, pos))
                }

                // Artists
                Database.getSongArtistsSync(song.id).forEach { artist ->
                    if (cubeArtists.none { it.id == artist.id }) {
                        cubeArtists.add(artist.toCubeArtist())
                    }
                    cubeSongArtistMaps.add(CubeSongArtistMap(song.id, artist.id))
                }

                // Find audio file
                val possibleExtensions = listOf("mkv", "flac", "m4a", "opus", "mp3", "ogg", "wv", "mp4")
                for (ext in possibleExtensions) {
                    val file = File(audioDir, "${song.id}.$ext")
                    if (file.exists() && file.length() > 0) {
                        if (filesToPack.none { it.name == file.name }) {
                            filesToPack.add(file)
                        }
                        break
                    }
                }
            }

            val cubeName = if (songs.size == 1) songs.first().title else "Selected Tracks Export (${songs.size})"

            val metadata = CubeMetadata(
                cubeName = cubeName,
                orderedSongIds = songs.map { it.id },
                songs = cubeSongs,
                albums = cubeAlbums,
                artists = cubeArtists,
                lyrics = cubeLyrics,
                formats = cubeFormats,
                songAlbumMaps = cubeSongAlbumMaps,
                songArtistMaps = cubeSongArtistMaps
            )

            val jsonString = Json.encodeToString(metadata)
            val (zstdLevel, zipLevel) = getCompressionParams(context, compressionLevel)
            val compressedJson = Zstd.compress(jsonString.toByteArray(Charsets.UTF_8), zstdLevel)

            context.contentResolver.openOutputStream(uri)?.use { rawOs ->
                rawOs.write(BLACKBOX_MAGIC)
                val blackBoxOs = BlackBoxOutputStream(rawOs)
                ZipOutputStream(BufferedOutputStream(blackBoxOs)).use { zos ->
                    val metaEntry = ZipEntry("metadata.json.zst")
                    zos.putNextEntry(metaEntry)
                    zos.write(compressedJson)
                    zos.closeEntry()

                    for (file in filesToPack) {
                        val entry = ZipEntry("audio/${file.name}")
                        zos.setLevel(zipLevel) 
                        zos.putNextEntry(entry)
                        
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export cube", e)
            Result.failure(e)
        }
    }

    /**
     * Exports a single Song into a `.synapcube` file.
     */
    suspend fun exportSongToCube(
        context: Context,
        songId: String,
        uri: Uri,
        compressionLevel: CubeCompressionLevel = CubeCompressionLevel.Auto
    ): Result<Unit> = exportSongsToCube(context, listOf(songId), uri, compressionLevel)

    /**
     * Exports multiple selected Playlists into a single `.synapcube` file.
     */
    suspend fun exportPlaylistsToCube(
        context: Context,
        playlistIds: List<Long>,
        uri: Uri,
        compressionLevel: CubeCompressionLevel = CubeCompressionLevel.Auto
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val allSongs = mutableListOf<Song>()
            val cubeSongs = mutableListOf<CubeSong>()
            val cubeAlbums = mutableListOf<CubeAlbum>()
            val cubeArtists = mutableListOf<CubeArtist>()
            val cubeLyrics = mutableListOf<CubeLyrics>()
            val cubeFormats = mutableListOf<CubeFormat>()
            val cubeSongAlbumMaps = mutableListOf<CubeSongAlbumMap>()
            val cubeSongArtistMaps = mutableListOf<CubeSongArtistMap>()

            val audioDir = getAudioDir(context)
            val filesToPack = mutableListOf<File>()
            val playlistNames = mutableListOf<String>()

            for (playlistId in playlistIds) {
                val playlist = Database.getPlaylistSync(playlistId)
                if (playlist != null) {
                    playlistNames.add(playlist.name)
                    val songs = Database.getPlaylistSongsSync(playlistId)
                    for (song in songs) {
                        if (allSongs.none { it.id == song.id }) {
                            allSongs.add(song)
                        }
                    }
                }
            }

            for (song in allSongs) {
                cubeSongs.add(song.toCubeSong())

                // Format
                Database.getFormat(song.id)?.let { format ->
                    cubeFormats.add(format.toCubeFormat())
                }

                // Lyrics
                Database.getLyrics(song.id)?.let { lyrics ->
                    cubeLyrics.add(lyrics.toCubeLyrics())
                }

                // Albums
                Database.getSongAlbumsSync(song.id).forEach { album ->
                    if (cubeAlbums.none { it.id == album.id }) {
                        cubeAlbums.add(album.toCubeAlbum())
                    }
                    val pos = Database.getAlbumPosition(song.id, album.id)
                    cubeSongAlbumMaps.add(CubeSongAlbumMap(song.id, album.id, pos))
                }

                // Artists
                Database.getSongArtistsSync(song.id).forEach { artist ->
                    if (cubeArtists.none { it.id == artist.id }) {
                        cubeArtists.add(artist.toCubeArtist())
                    }
                    cubeSongArtistMaps.add(CubeSongArtistMap(song.id, artist.id))
                }

                // Find audio file
                val possibleExtensions = listOf("mkv", "flac", "m4a", "opus", "mp3", "ogg", "wv", "mp4")
                for (ext in possibleExtensions) {
                    val file = File(audioDir, "${song.id}.$ext")
                    if (file.exists() && file.length() > 0) {
                        if (filesToPack.none { it.name == file.name }) {
                            filesToPack.add(file)
                        }
                        break
                    }
                }
            }

            val cubeName = if (playlistNames.size == 1) playlistNames.first() else "Playlists Export (${playlistNames.size})"

            val metadata = CubeMetadata(
                cubeName = cubeName,
                orderedSongIds = allSongs.map { it.id },
                songs = cubeSongs,
                albums = cubeAlbums,
                artists = cubeArtists,
                lyrics = cubeLyrics,
                formats = cubeFormats,
                songAlbumMaps = cubeSongAlbumMaps,
                songArtistMaps = cubeSongArtistMaps
            )

            val jsonString = Json.encodeToString(metadata)
            val (zstdLevel, zipLevel) = getCompressionParams(context, compressionLevel)
            val compressedJson = Zstd.compress(jsonString.toByteArray(Charsets.UTF_8), zstdLevel)

            context.contentResolver.openOutputStream(uri)?.use { rawOs ->
                rawOs.write(BLACKBOX_MAGIC)
                val blackBoxOs = BlackBoxOutputStream(rawOs)
                ZipOutputStream(BufferedOutputStream(blackBoxOs)).use { zos ->
                    val metaEntry = ZipEntry("metadata.json.zst")
                    zos.putNextEntry(metaEntry)
                    zos.write(compressedJson)
                    zos.closeEntry()

                    for (file in filesToPack) {
                        val entry = ZipEntry("audio/${file.name}")
                        zos.setLevel(zipLevel) 
                        zos.putNextEntry(entry)
                        
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export cube", e)
            Result.failure(e)
        }
    }

    /**
     * Imports a `.synapcube` file and unpacks it into the library.
     * 1. Extracts audio files into the local Vault directory.
     * 2. Decompresses Zstd metadata and parses the JSON.
     * 3. Upserts all relational models (Artists, Albums, Songs, Formats, Lyrics).
     * 4. Synthesizes a new Playlist in the DB if the cube wasn't a "Full Library Backup".
     */
    suspend fun importCube(
        context: Context,
        uri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val audioDir = getAudioDir(context)
            var metadata: CubeMetadata? = null

            // 1. De-obfuscate BlackBox Stream & Unzip
            context.contentResolver.openInputStream(uri)?.use { rawIs ->
                val buffered = BufferedInputStream(rawIs)
                buffered.mark(BLACKBOX_MAGIC.size + 2)
                
                val header = ByteArray(BLACKBOX_MAGIC.size)
                var bytesRead = 0
                while (bytesRead < BLACKBOX_MAGIC.size) {
                    val count = buffered.read(header, bytesRead, BLACKBOX_MAGIC.size - bytesRead)
                    if (count <= 0) break
                    bytesRead += count
                }

                val isBlackBox = bytesRead == BLACKBOX_MAGIC.size && header.contentEquals(BLACKBOX_MAGIC)
                
                val targetStream: InputStream = if (isBlackBox) {
                    BlackBoxInputStream(buffered)
                } else {
                    buffered.reset()
                    buffered
                }

                ZipInputStream(targetStream).use { zis ->
                    var entry: ZipEntry?
                    while (zis.nextEntry.also { entry = it } != null) {
                        val currentEntry = entry!!
                        if (currentEntry.name == "metadata.json.zst") {
                            val compressedData = zis.readBytes()
                            val decompressedSize = Zstd.decompressedSize(compressedData)
                            
                            // SECURITY: Prevent Zip Bomb / Memory Exhaustion attacks
                            // Don't allow metadata files larger than 100MB (arbitrary safe limit for JSON).
                            if (decompressedSize > 100 * 1024 * 1024) {
                                throw SecurityException("Malicious payload detected: Metadata exceeds safe memory limits (Zip Bomb)")
                            }

                            val jsonString = Zstd.decompress(compressedData, decompressedSize.toInt()).toString(Charsets.UTF_8)
                            metadata = Json.decodeFromString<CubeMetadata>(jsonString)
                        } else if (currentEntry.name.startsWith("audio/")) {
                            val fileName = currentEntry.name.removePrefix("audio/")
                            val targetFile = File(audioDir, fileName)
                            
                            // SECURITY: Prevent Zip Slip (Path Traversal) attacks
                            val canonicalDestPath = targetFile.canonicalPath
                            val canonicalDirPath = audioDir.canonicalPath
                            if (!canonicalDestPath.startsWith(canonicalDirPath + File.separator)) {
                                throw SecurityException("Malicious ZIP path traversal detected: ${currentEntry.name}")
                            }

                            if (!targetFile.exists()) {
                                FileOutputStream(targetFile).use { fos ->
                                    zis.copyTo(fos)
                                }
                            }
                        }
                        zis.closeEntry()
                    }
                }
            }

            if (metadata == null) return@withContext Result.failure(Exception("Metadata not found in cube"))

            // 2. Insert Metadata into Database
            metadata.apply {
                // Artists
                artists.forEach { Database.upsert(it.toArtist()) }
                // Albums
                albums.forEach { Database.upsert(it.toAlbum()) }
                // Songs
                songs.forEach { Database.upsert(it.toSong()) }
                // Formats
                formats.forEach { Database.upsert(it.toFormat()) }
                // Lyrics
                lyrics.forEach { Database.upsert(it.toLyrics()) }
                // Maps
                songAlbumMaps.forEach { Database.upsert(it.toSongAlbumMap()) }
                songArtistMaps.forEach { Database.upsert(it.toSongArtistMap()) }

                // 3. Create Playlist (Only if it's not a Full Library Backup)
                if (cubeName != "Full Library Backup") {
                    val newPlaylistId = Database.insert(Playlist(name = "Cube: $cubeName"))
                    orderedSongIds.forEachIndexed { index, songId ->
                        Database.insert(SongPlaylistMap(songId, newPlaylistId, index))
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import cube", e)
            Result.failure(e)
        }
    }
}