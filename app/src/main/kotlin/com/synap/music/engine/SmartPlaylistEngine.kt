package com.synap.music.engine

import com.synap.music.Database
import com.synap.music.enums.SongSortBy
import com.synap.music.enums.SortOrder
import com.synap.music.models.Playlist
import com.synap.music.models.Song
import com.synap.music.models.SongPlaylistMap
import com.synap.music.query
import com.synap.music.utils.asMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

/**
 * Smart Auto-Playlist Generator Engine.
 * Analyzes local library stats and track metadata to dynamically generate mood and filter mixes.
 */
object SmartPlaylistEngine {

    enum class MixType(val label: String, val description: String) {
        MostPlayed("Most Played Mix", "Your top listened tracks of all time"),
        RecentlySaved("Recently Saved", "Your latest favorited and saved songs"),
        OfflineVault("Vault Offline Mix", "All downloaded songs ready for offline listening"),
        HighEnergy("High Energy & Workout", "Upbeat, electronic, rock, and party beats"),
        ChillVibes("Chill & Relax", "Acoustic, lofi, ambient, and jazz tracks")
    }

    /**
     * Generates a list of songs matching the specified [MixType].
     */
    suspend fun generateMix(type: MixType, limit: Int = 30): List<Song> = withContext(Dispatchers.IO) {
        val allSongs = Database.songs(SongSortBy.PlayTime, SortOrder.Descending).firstOrNull() ?: emptyList()

        when (type) {
            MixType.MostPlayed -> {
                allSongs.sortedByDescending { it.totalPlayTimeMs }.take(limit)
            }
            MixType.RecentlySaved -> {
                allSongs.filter { it.likedAt != null }.sortedByDescending { it.likedAt }.take(limit)
            }
            MixType.OfflineVault -> {
                Database.songsWithContentLength().firstOrNull()?.map { it.song }?.take(limit) ?: emptyList()
            }
            MixType.HighEnergy -> {
                val keywords = listOf("dance", "electronic", "rock", "pop", "hype", "party", "workout", "fast", "edm", "phonk")
                allSongs.filter { song ->
                    val text = "${song.title} ${song.artistsText}".lowercase()
                    keywords.any { text.contains(it) }
                }.take(limit).ifEmpty { allSongs.shuffled().take(limit) }
            }
            MixType.ChillVibes -> {
                val keywords = listOf("chill", "acoustic", "lofi", "ambient", "jazz", "relax", "piano", "slow", "soul", "mellow")
                allSongs.filter { song ->
                    val text = "${song.title} ${song.artistsText}".lowercase()
                    keywords.any { text.contains(it) }
                }.take(limit).ifEmpty { allSongs.shuffled().take(limit) }
            }
        }
    }

    /**
     * Saves a generated Smart Mix as a permanent local playlist in Room Database.
     */
    suspend fun saveMixAsPlaylist(mixTitle: String, songs: List<Song>): Long = withContext(Dispatchers.IO) {
        if (songs.isEmpty()) return@withContext -1L

        var newPlaylistId = -1L
        query {
            newPlaylistId = Database.insert(Playlist(name = mixTitle))
            songs.forEachIndexed { index, song ->
                Database.insert(song.asMediaItem)
                Database.insert(
                    SongPlaylistMap(
                        songId = song.id,
                        playlistId = newPlaylistId,
                        position = index
                    )
                )
            }
        }
        newPlaylistId
    }
}
