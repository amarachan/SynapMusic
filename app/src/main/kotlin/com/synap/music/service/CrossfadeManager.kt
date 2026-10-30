package com.synap.music.service

import androidx.media3.common.C
import androidx.media3.common.Player
import com.synap.music.utils.crossfadeDurationKey
import com.synap.music.utils.preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages volume automation for crossfade transitions between songs in [PlayerService].
 * Automatically ramps down volume near track ends and ramps up volume when new tracks start.
 */
class CrossfadeManager(
    private val playerService: PlayerService,
    private val player: Player,
    private val scope: CoroutineScope
) {
    private var tickerJob: Job? = null
    private var isFadingIn = false
    private var fadeInStartTime = 0L

    /**
     * Starts monitoring ExoPlayer playback position for crossfade volume automation.
     */
    fun startMonitoring() {
        tickerJob?.cancel()
        tickerJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                updateVolume()
                delay(100)
            }
        }
    }

    /**
     * Stops monitoring and restores full player volume.
     */
    fun stopMonitoring() {
        tickerJob?.cancel()
        tickerJob = null
        player.volume = 1.0f
    }

    /**
     * Triggers track transition fade-in handling when a new media item starts playing.
     */
    fun onMediaItemTransition() {
        val crossfadeSec = playerService.preferences.getInt(crossfadeDurationKey, 0)
        if (crossfadeSec > 0) {
            isFadingIn = true
            fadeInStartTime = System.currentTimeMillis()
            player.volume = 0.0f
        } else {
            isFadingIn = false
            player.volume = 1.0f
        }
    }

    /**
     * Computes real-time player volume based on current track position and remaining duration.
     */
    private fun updateVolume() {
        val crossfadeSec = playerService.preferences.getInt(crossfadeDurationKey, 0)
        if (crossfadeSec <= 0) {
            if (player.volume != 1.0f) {
                player.volume = 1.0f
            }
            isFadingIn = false
            return
        }

        val crossfadeMs = crossfadeSec * 1000L
        val duration = player.duration
        val position = player.currentPosition

        // 1. Handle Fade In at the start of a track
        if (isFadingIn) {
            val elapsed = System.currentTimeMillis() - fadeInStartTime
            if (elapsed < crossfadeMs) {
                val fadeInVolume = (elapsed.toFloat() / crossfadeMs).coerceIn(0.0f, 1.0f)
                player.volume = fadeInVolume
                return
            } else {
                isFadingIn = false
                player.volume = 1.0f
            }
        }

        // 2. Handle Fade Out near the end of a track
        if (duration != C.TIME_UNSET && duration > crossfadeMs) {
            val remaining = duration - position
            if (remaining in 0..crossfadeMs) {
                val fadeOutVolume = (remaining.toFloat() / crossfadeMs).coerceIn(0.0f, 1.0f)
                player.volume = fadeOutVolume
            } else if (!isFadingIn) {
                player.volume = 1.0f
            }
        } else if (!isFadingIn) {
            player.volume = 1.0f
        }
    }
}
