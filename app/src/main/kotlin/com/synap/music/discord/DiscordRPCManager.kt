package com.synap.music.discord

import android.content.Context
import com.synap.music.discord.rpc.DiscordRPC
import com.synap.music.models.Song
import com.synap.music.utils.discordTokenKey
import com.synap.music.utils.isDiscordRpcEnabledKey
import com.synap.music.utils.preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Controller class that manages Discord Rich Presence connection state and updates.
 * Observes active song metadata and sends status updates over Discord WebSocket Gateway.
 */
class DiscordRPCManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var rpc: DiscordRPC? = null
    private var currentToken: String = ""
    private var updateJob: Job? = null

    /**
     * Updates active Discord presence with song details, playback position, and remaining time.
     */
    fun updatePresence(
        song: Song?,
        currentPositionMs: Long,
        durationMs: Long,
        isPlaying: Boolean
    ) {
        val prefs = context.preferences
        val isEnabled = prefs.getBoolean(isDiscordRpcEnabledKey, false)
        val token = prefs.getString(discordTokenKey, "") ?: ""

        if (!isEnabled || token.isBlank() || song == null || !isPlaying) {
            clearPresence()
            return
        }

        if (rpc == null || currentToken != token) {
            rpc?.closeRPC()
            currentToken = token
            rpc = DiscordRPC(context, token)
        }

        updateJob?.cancel()
        updateJob = scope.launch(Dispatchers.IO) {
            try {
                rpc?.updateSong(
                    song = song,
                    currentPlaybackTimeMillis = currentPositionMs,
                    durationMillis = durationMs
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Clears current Discord Rich Presence activity.
     */
    fun clearPresence() {
        updateJob?.cancel()
        updateJob = scope.launch(Dispatchers.IO) {
            try {
                rpc?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Closes Discord WebSocket connection and releases RPC resources.
     */
    fun close() {
        updateJob?.cancel()
        rpc?.closeRPC()
        rpc = null
    }
}
