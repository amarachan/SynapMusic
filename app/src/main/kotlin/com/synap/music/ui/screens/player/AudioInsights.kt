package com.synap.music.ui.screens.player

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheSpan
import com.synap.pulse.SynapService
import com.synap.pulse.requests.lyrics
import com.synap.pulse.models.bodies.NextBody
import com.synap.pulse.models.bodies.PlayerBody
import com.synap.pulse.requests.player
import com.synap.music.Database
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.models.Format
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.onOverlay
import com.synap.music.ui.styling.overlay
import com.synap.music.utils.color
import com.synap.music.utils.medium
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

/**
 * Audio Insights UI.
 * This is a technical overlay for audiophiles. It shows real-time data about 
 * the current playback session, including bitrate, cache status, and loudness levels.
 */
@Composable
fun AudioInsights(
    mediaId: String,
    isDisplayed: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current ?: return

    AnimatedVisibility(
        visible = isDisplayed,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        // Track how many bytes of the current song are actually saved on the phone's disk.
        var cachedBytes by remember(mediaId) {
            mutableStateOf(binder.cache.getCachedBytes(mediaId, 0, -1))
        }

        var format by remember {
            mutableStateOf<Format?>(null)
        }

        // Live bitrate monitor (updated every second).
        var liveBitrate by remember { mutableStateOf<Int?>(null) }

        /**
         * Metadata Probe.
         * If we don't have the technical info for this song, we hit the 
         * YouTube "Player" API to fetch things like the exact bitrate 
         * and the "Itag" (YouTube's internal format ID).
         */
        LaunchedEffect(mediaId) {
            Database.format(mediaId).distinctUntilChanged().collectLatest { currentFormat ->
                if (currentFormat?.itag == null) {
                    binder.player.currentMediaItem?.takeIf { it.mediaId == mediaId }?.let { mediaItem ->
                        withContext(Dispatchers.IO) {
                            delay(1000)
                            SynapService.player(PlayerBody(videoId = mediaId))?.onSuccess { response ->
                                response.streamingData?.highestQualityFormat?.let { itFormat ->
                                    Database.insert(mediaItem)
                                    Database.upsert(
                                        Format(
                                            songId = mediaId,
                                            itag = itFormat.itag,
                                            mimeType = itFormat.mimeType,
                                            bitrate = itFormat.bitrate ?: itFormat.averageBitrate,
                                            loudnessDb = response.playerConfig?.audioConfig?.normalizedLoudnessDb,
                                            contentLength = itFormat.contentLength,
                                            lastModified = itFormat.lastModified
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    format = currentFormat
                }
            }
        }

        // ... Live Bitrate Monitor & Cache Listener ...

        // Live Bitrate Monitor
        LaunchedEffect(mediaId) {
            while (true) {
                val currentFormat = binder.player.audioFormat
                if (currentFormat != null) {
                    liveBitrate = if (currentFormat.bitrate > 0) currentFormat.bitrate else null
                }
                delay(1000)
            }
        }

        DisposableEffect(mediaId) {
            val listener = object : Cache.Listener {
                override fun onSpanAdded(cache: Cache, span: CacheSpan) {
                    cachedBytes += span.length
                }

                override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
                    cachedBytes -= span.length
                }

                override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) =
                    Unit
            }

            binder.cache.addListener(mediaId, listener)

            onDispose {
                binder.cache.removeListener(mediaId, listener)
            }
        }

        Box(
            modifier = modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            onDismiss()
                        }
                    )
                }
                .background(colorPalette.overlay)
                .fillMaxSize()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(all = 16.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    BasicText(
                        text = "Id",
                        style = typography.xs.medium.color(colorPalette.onOverlay)
                    )
                    BasicText(
                        text = "Itag",
                        style = typography.xs.medium.color(colorPalette.onOverlay)
                    )
                    BasicText(
                        text = "Bitrate",
                        style = typography.xs.medium.color(colorPalette.onOverlay)
                    )
                    BasicText(
                        text = "Size",
                        style = typography.xs.medium.color(colorPalette.onOverlay)
                    )
                    BasicText(
                        text = "Cached",
                        style = typography.xs.medium.color(colorPalette.onOverlay)
                    )
                    BasicText(
                        text = "Loudness",
                        style = typography.xs.medium.color(colorPalette.onOverlay)
                    )
                }

                Column {
                    BasicText(
                        text = mediaId,
                        maxLines = 1,
                        style = typography.xs.medium.color(colorPalette.onOverlay)
                    )
                    BasicText(
                        text = format?.itag?.toString() ?: "N/A",
                        maxLines = 1,
                        style = typography.xs.medium.color(colorPalette.onOverlay)
                    )
                    BasicText(
                        text = format?.bitrate?.let { "${it / 1000} kbps" } 
                            ?: liveBitrate?.let { "${it / 1000} kbps" }
                            ?: "N/A",
                        maxLines = 1,
                        style = typography.xs.medium.color(colorPalette.onOverlay)
                    )
                    BasicText(
                        text = format?.contentLength
                            ?.let { Formatter.formatShortFileSize(context, it) } ?: "N/A",
                        maxLines = 1,
                        style = typography.xs.medium.color(colorPalette.onOverlay)
                    )
                    BasicText(
                        text = buildString {
                            append(Formatter.formatShortFileSize(context, cachedBytes))

                            format?.contentLength?.let {
                                if (it > 0) append(" (${(cachedBytes.toFloat() / it * 100).roundToInt()}%)")
                            }
                        },
                        maxLines = 1,
                        style = typography.xs.medium.color(colorPalette.onOverlay)
                    )
                    BasicText(
                        text = format?.loudnessDb?.let { "%.2f dB".format(it) } ?: "N/A",
                        maxLines = 1,
                        style = typography.xs.medium.color(colorPalette.onOverlay)
                    )
                }
            }
        }
    }
}
