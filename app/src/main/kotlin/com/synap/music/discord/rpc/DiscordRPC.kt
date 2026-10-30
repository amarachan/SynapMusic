package com.synap.music.discord.rpc

import android.content.Context
import com.synap.music.models.Song
import java.util.Locale

class DiscordRPC(
    val context: Context,
    token: String,
) : KizzyRPC(
    token = token,
    os = "Android",
    browser = "Discord Android",
    device = android.os.Build.DEVICE,
    userAgent = SuperProperties.userAgent,
    superPropertiesBase64 = SuperProperties.superPropertiesBase64,
) {
    suspend fun updateSong(
        song: Song,
        currentPlaybackTimeMillis: Long,
        durationMillis: Long,
        playbackSpeed: Float = 1.0f,
        useDetails: Boolean = false,
        status: String = "online",
        button1Text: String = "",
        button1Visible: Boolean = true,
        button2Text: String = "",
        button2Visible: Boolean = true,
        activityType: String = "listening",
        activityName: String = "",
    ) = runCatching {
        val currentTime = System.currentTimeMillis()

        val adjustedPlaybackTime = (currentPlaybackTimeMillis / playbackSpeed).toLong()
        val calculatedStartTime = currentTime - adjustedPlaybackTime

        val songTitleWithRate = if (playbackSpeed != 1.0f) {
            "${song.title} [${String.format(Locale.ROOT, "%.2fx", playbackSpeed)}]"
        } else {
            song.title
        }

        val remainingDuration = durationMillis - currentPlaybackTimeMillis
        val adjustedRemainingDuration = (remainingDuration / playbackSpeed).toLong()

        val buttonsList = mutableListOf<Pair<String, String>>()
        if (button1Visible) {
            val resolvedText = resolveVariables(
                button1Text.ifEmpty { DEFAULT_BUTTON_1 },
                song,
            )
            buttonsList.add(resolvedText to watchUrl(song))
        }
        if (button2Visible) {
            val resolvedText = resolveVariables(
                button2Text.ifEmpty { DEFAULT_BUTTON_2 },
                song,
            )
            buttonsList.add(resolvedText to PROJECT_URL)
        }

        val type = when (activityType) {
            "playing" -> Type.PLAYING
            "streaming" -> Type.STREAMING
            "listening" -> Type.LISTENING
            "watching" -> Type.WATCHING
            "competing" -> Type.COMPETING
            else -> Type.LISTENING
        }

        val name = activityName.ifEmpty { appName() }

        val artworkUrl = song.thumbnailUrl ?: "https://i.ytimg.com/vi/${song.id}/hqdefault.jpg"

        setActivity(
            name = name,
            state = if (useDetails) songTitleWithRate else (song.artistsText ?: "Unknown Artist"),
            details = if (useDetails) (song.artistsText ?: "Unknown Artist") else songTitleWithRate,
            largeImage = RpcImage.ExternalImage(
                artworkUrl.takeIf { it.startsWith("http") } ?: FALLBACK_ART_URL,
            ),
            smallImage = null,
            largeText = song.title,
            smallText = null,
            buttons = if (buttonsList.isNotEmpty()) buttonsList else null,
            type = type,
            statusDisplayType = if (useDetails) StatusDisplayType.DETAILS else StatusDisplayType.STATE,
            since = currentTime,
            startTime = calculatedStartTime,
            endTime = currentTime + adjustedRemainingDuration,
            applicationId = APPLICATION_ID,
            status = status,
        )
    }

    private fun appName(): String = "SynapMusic"

    companion object {
        private const val APPLICATION_ID = "1411019391843172514"

        const val PROJECT_URL = "https://github.com/synapmusic/SynapMusic"

        const val DEFAULT_BUTTON_1 = "Listen on YouTube Music"
        const val DEFAULT_BUTTON_2 = "SynapMusic"

        private const val FALLBACK_ART_URL =
            "https://raw.githubusercontent.com/synapmusic/SynapMusic/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png"

        fun watchUrl(song: Song): String =
            "https://music.youtube.com/watch?v=${song.id}"

        fun resolveVariables(text: String, song: Song): String {
            return text
                .replace("{song_name}", song.title)
                .replace("{artist_name}", song.artistsText ?: "")
        }
    }
}
