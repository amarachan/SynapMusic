package com.synap.music.listentogether

import android.os.SystemClock
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.synap.music.models.Song
import com.synap.music.utils.asMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class PartySync(
    private val exo: Player,
    private val scope: CoroutineScope
) {
    enum class Intent { PLAY, PAUSE, SEEK, TRACK_CHANGED, QUEUE_REORDERED }

    private var syncJob: Job? = null
    private var isApplyingRemoteState = false

    fun start() {
        syncJob?.cancel()
        syncJob = scope.launch(Dispatchers.Main) {
            ListenTogether.state
                .map { it.playback }
                .distinctUntilChanged()
                .collect { playback ->
                    if (ListenTogether.state.value.inParty) {
                        applyRemotePlayback(playback)
                    }
                }
        }
    }

    fun stop() {
        syncJob?.cancel()
        syncJob = null
    }

    fun onLocalIntent(intent: Intent, positionMs: Long = exo.currentPosition) {
        if (!ListenTogether.state.value.inParty || isApplyingRemoteState) return

        when (intent) {
            Intent.PLAY -> {
                val song = exo.currentMediaItem?.toSong() ?: return
                ListenTogether.setTrack(song.toPartyTrack(exo.duration), positionMs, isPlaying = true)
            }
            Intent.PAUSE -> {
                ListenTogether.pause(positionMs)
            }
            Intent.SEEK -> {
                ListenTogether.seek(positionMs)
            }
            Intent.TRACK_CHANGED -> {
                val song = exo.currentMediaItem?.toSong() ?: return
                ListenTogether.setTrack(song.toPartyTrack(exo.duration), positionMs, isPlaying = exo.isPlaying)
            }
            Intent.QUEUE_REORDERED -> {
                // Handled via queue sync
            }
        }
    }

    private suspend fun applyRemotePlayback(playback: PartyPlayback) {
        if (playback.track == null) return

        isApplyingRemoteState = true
        try {
            val targetSong = playback.track.toSong()
            val currentMediaId = exo.currentMediaItem?.mediaId

            if (currentMediaId != targetSong.id) {
                exo.setMediaItem(targetSong.asMediaItem)
                exo.prepare()
            }

            val targetPositionMs = ListenTogether.partyPositionMs() ?: 0L
            val currentPositionMs = exo.currentPosition

            if (abs(currentPositionMs - targetPositionMs) > 1500) {
                exo.seekTo(targetPositionMs.coerceAtLeast(0L))
            }

            if (playback.isPlaying && !exo.isPlaying) {
                exo.play()
            } else if (!playback.isPlaying && exo.isPlaying) {
                exo.pause()
            }
        } catch (e: Exception) {
            Log.e("PartySync", "Error applying remote playback state", e)
        } finally {
            delay(300)
            isApplyingRemoteState = false
        }
    }

    private fun MediaItem.toSong(): Song = Song(
        id = mediaId,
        title = mediaMetadata.title?.toString() ?: "Unknown Track",
        artistsText = mediaMetadata.artist?.toString() ?: "Unknown Artist",
        thumbnailUrl = mediaMetadata.artworkUri?.toString(),
        durationText = null
    )

    private fun Song.toPartyTrack(playerDurationMs: Long): PartyTrack = PartyTrack(
        videoId = id,
        title = title,
        artist = artistsText ?: "Unknown Artist",
        thumbnailUrl = thumbnailUrl,
        durationMs = playerDurationMs.takeIf { it > 0 }
    )

    private fun PartyTrack.toSong(): Song = Song(
        id = videoId,
        title = title,
        artistsText = artist,
        thumbnailUrl = thumbnailUrl ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
        durationText = null
    )
}
