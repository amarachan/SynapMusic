package com.synap.music.engine

import com.synap.music.Database
import com.synap.music.enums.SongSortBy
import com.synap.music.enums.SortOrder
import com.synap.music.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Data model for Synap Recap listening analytics.
 */
data class RecapData(
    val totalListeningTimeMs: Long,
    val totalSongsCount: Int,
    val topSongs: List<Song>,
    val topArtists: List<Pair<String, Long>>,
    val peakListeningHabit: String
)

/**
 * Analytics Engine for computing Synap Recap listening statistics on-device from Room Database.
 */
object AnalyticsEngine {

    /**
     * Calculates listening analytics from Room Database.
     */
    suspend fun getRecapData(): RecapData = withContext(Dispatchers.IO) {
        val allSongs = Database.songs(SongSortBy.PlayTime, SortOrder.Descending).firstOrNull() ?: emptyList()
        val totalPlayTime = allSongs.sumOf { it.totalPlayTimeMs }
        val topSongsList = allSongs.filter { it.totalPlayTimeMs > 0 }.take(5)

        // Aggregate top artists by play time
        val artistPlayTimeMap = mutableMapOf<String, Long>()
        allSongs.forEach { song ->
            val artistName = song.artistsText?.ifBlank { "Unknown Artist" } ?: "Unknown Artist"
            if (song.totalPlayTimeMs > 0) {
                artistPlayTimeMap[artistName] = (artistPlayTimeMap[artistName] ?: 0L) + song.totalPlayTimeMs
            }
        }

        val topArtistsList = artistPlayTimeMap.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }

        // Determine peak listening habit based on current hour
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val habit = when (currentHour) {
            in 5..11 -> "Morning Energy ☀️"
            in 12..17 -> "Afternoon Groove 🌤️"
            in 18..22 -> "Evening Chill 🌆"
            else -> "Late Night Listener 🌙"
        }

        RecapData(
            totalListeningTimeMs = totalPlayTime,
            totalSongsCount = allSongs.count { it.totalPlayTimeMs > 0 },
            topSongs = topSongsList,
            topArtists = topArtistsList,
            peakListeningHabit = habit
        )
    }
}
