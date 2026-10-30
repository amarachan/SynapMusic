/*
 * Copyright (C) 2026 SynapMusic Maintainers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.synap.music.ui.screens.player

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.synap.compose.routing.OnGlobalRoute
import com.synap.pulse.models.NavigationEndpoint
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.R
import com.synap.music.service.PlayerService
import com.synap.music.ui.components.BottomSheet
import com.synap.music.ui.components.BottomSheetState
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.ui.components.rememberBottomSheetState
import com.synap.music.ui.components.themed.BaseMediaItemMenu
import com.synap.music.ui.components.themed.IconButton
import com.synap.music.ui.styling.Dimensions
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.onGlass
import com.synap.music.ui.styling.px
import com.synap.music.utils.DisposableListener
import com.synap.music.utils.forceSeekToNext
import com.synap.music.utils.forceSeekToPrevious
import com.synap.music.utils.isLandscape
import com.synap.music.utils.positionAndDurationState
import com.synap.music.utils.seamlessPlay
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import com.synap.music.utils.shouldBePlaying
import com.synap.music.utils.thumbnail
import com.synap.music.utils.thumbnailLowRes
import com.synap.music.utils.toast
import com.synap.music.utils.rememberPreference
import com.synap.music.utils.trackLoopEnabledKey
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlin.math.absoluteValue

/**
 * The main Player screen. 
 * This is a gesture-based UI that lives in a "Bottom Sheet". 
 * It can be a tiny bar at the bottom (Mini Player) or expand to fill 
 * the whole screen with artwork, lyrics, and controls.
 */
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun Player(
    layoutState: BottomSheetState,
    modifier: Modifier = Modifier,
) {
    val menuState = LocalMenuState.current
    val (colorPalette, typography, thumbnailShape) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current

    // If there's no player service running, we don't show anything.
    binder?.player ?: return

    // Track the current song and whether it's actually playing.
    var nullableMediaItem by remember {
        mutableStateOf(binder.player.currentMediaItem, neverEqualPolicy())
    }

    var shouldBePlaying by remember {
        mutableStateOf(binder.player.shouldBePlaying)
    }

    var isShowingLyrics by rememberSaveable {
        mutableStateOf(false)
    }

    var isShowingAudioInsights by rememberSaveable {
        mutableStateOf(false)
    }

    var trackLoopEnabled by rememberPreference(trackLoopEnabledKey, defaultValue = false)

    LaunchedEffect(nullableMediaItem?.mediaId) {
        isShowingLyrics = false
        isShowingAudioInsights = false
    }

    // Listen to the player and update the UI when the song changes.
    binder.player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                nullableMediaItem = mediaItem
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }
        }
    }

    val mediaItem = nullableMediaItem ?: return
    val positionAndDuration by binder.player.positionAndDurationState()

    val windowInsets = WindowInsets.systemBars
    val horizontalBottomPaddingValues = windowInsets
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom).asPaddingValues()

    // Collapse the player automatically if the user navigates to a new page.
    OnGlobalRoute {
        layoutState.collapseSoft()
    }

    BottomSheet(
        state = layoutState,
        modifier = modifier,
        onDismiss = {
            binder.stopRadio()
        },
        /**
         * The Mini Player (Collapsed State).
         * A compact bar showing the current track details, quick playback controls, and progress line.
         * Features a horizontal swipe-to-dismiss gesture (swipe left or right past 35% threshold)
         * to stop playback, clear the queue, and dismiss the Mini Player bar.
         */
        collapsedContent = {
            val offsetX = remember { Animatable(0f) }
            val coroutineScope = rememberCoroutineScope()
            val density = LocalDensity.current
            val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
            val dismissThreshold = screenWidthPx * 0.35f

            Box(
                modifier = Modifier
                    .padding(horizontal = Dimensions.miniPlayerPadding)
                    .padding(bottom = horizontalBottomPaddingValues.calculateBottomPadding() + 8.dp)
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .graphicsLayer {
                        alpha = (1f - (abs(offsetX.value) / screenWidthPx)).coerceIn(0f, 1f)
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    if (abs(offsetX.value) > dismissThreshold) {
                                        val targetX = if (offsetX.value > 0) screenWidthPx else -screenWidthPx
                                        offsetX.animateTo(targetX, spring())
                                        binder.stopRadio()
                                        binder.player.stop()
                                        binder.player.clearMediaItems()
                                        offsetX.snapTo(0f)
                                    } else {
                                        offsetX.animateTo(0f, spring())
                                    }
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    offsetX.animateTo(0f, spring())
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                coroutineScope.launch {
                                    offsetX.snapTo(offsetX.value + dragAmount)
                                }
                            }
                        )
                    }
                    .clip(RoundedCornerShape(32.dp))
                    .background(colorPalette.background2)
                    .height(Dimensions.collapsedPlayer)
                    .fillMaxWidth()
                    .clickable(onClick = layoutState::expandSoft)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    // Artwork with smooth WebP loading & offline disk cache fallback.
                    val artworkUri = mediaItem.mediaMetadata.artworkUri
                    val artworkUrl = artworkUri?.toString()?.thumbnail(Dimensions.thumbnails.song.px)
                    val lowResUrl = artworkUri?.toString()?.thumbnailLowRes()
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artworkUrl)
                            .placeholderMemoryCacheKey(lowResUrl)
                            .crossfade(true)
                            .build(),
                        placeholder = rememberAsyncImagePainter(artworkUri),
                        error = rememberAsyncImagePainter(artworkUri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .clip(thumbnailShape)
                            .size(44.dp)
                    )

                    // Song Details.
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        BasicText(
                            text = mediaItem.mediaMetadata.title?.toString() ?: "",
                            style = typography.xs.semiBold.copy(color = colorPalette.text),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        BasicText(
                            text = mediaItem.mediaMetadata.artist?.toString() ?: "",
                            style = typography.xs.semiBold.secondary.copy(color = colorPalette.textSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Quick Actions (Previous, Play/Pause, Next).
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            icon = R.drawable.play_skip_back,
                            color = colorPalette.text,
                            onClick = binder.player::forceSeekToPrevious,
                            modifier = Modifier
                                .size(24.dp)
                        )

                        IconButton(
                            icon = if (shouldBePlaying) R.drawable.pause else R.drawable.play,
                            color = colorPalette.text,
                            onClick = {
                                if (shouldBePlaying) {
                                    binder.player.pause()
                                } else {
                                    if (binder.player.playbackState == Player.STATE_IDLE) {
                                        binder.player.prepare()
                                    }
                                    binder.player.play()
                                }
                            },
                            modifier = Modifier
                                .size(24.dp)
                        )

                        IconButton(
                            icon = R.drawable.play_skip_forward,
                            color = colorPalette.text,
                            onClick = binder.player::forceSeekToNext,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                }

                // Subtle progress indicator at the bottom of the mini player.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(if (positionAndDuration.second.absoluteValue > 0) positionAndDuration.first.toFloat() / positionAndDuration.second.absoluteValue else 0f)
                        .height(2.dp)
                        .background(colorPalette.accent)
                )
            }
        }
    ) {
        /**
         * The Full Player (Expanded State).
         * A beautiful layout that adapts to the current album art's colors.
         * Includes the big cover, playback controls, lyrics, and the up-next queue.
         */
        // Logic for the sliding "Queue" panel that pulls up from the bottom.
        val playerBottomSheetState = rememberBottomSheetState(
            64.dp + horizontalBottomPaddingValues.calculateBottomPadding(),
            layoutState.expandedBound
        )

        Box(
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            val containerModifier = Modifier
                .padding(
                    windowInsets
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                        .asPaddingValues()
                )
                .padding(bottom = playerBottomSheetState.collapsedBound)

            val thumbnailContent: @Composable (modifier: Modifier) -> Unit = { modifier ->
                Thumbnail(
                    isShowingLyrics = isShowingLyrics,
                    onShowLyrics = { isShowingLyrics = it },
                    isShowingAudioInsights = isShowingAudioInsights,
                    onShowAudioInsights = { isShowingAudioInsights = it },
                    modifier = modifier
                )
            }

            val controlsContent: @Composable (modifier: Modifier) -> Unit = { modifier ->
                Controls(
                    mediaId = mediaItem.mediaId,
                    title = mediaItem.mediaMetadata.title?.toString(),
                    artist = mediaItem.mediaMetadata.artist?.toString(),
                    shouldBePlaying = shouldBePlaying,
                    position = positionAndDuration.first,
                    duration = positionAndDuration.second,
                    isShowingLyrics = isShowingLyrics,
                    onShowLyrics = { isShowingLyrics = it },
                    modifier = modifier
                )
            }

            if (isLandscape) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = containerModifier
                        .padding(top = 32.dp)
                        .fillMaxSize()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(0.66f)
                            .padding(bottom = 16.dp)
                    ) {
                        thumbnailContent(
                            Modifier
                                .padding(horizontal = 16.dp)
                        )
                    }

                    controlsContent(
                        Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxHeight()
                            .weight(1f)
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = containerModifier
                        .padding(top = 54.dp)
                        .fillMaxSize()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1.25f)
                    ) {
                        thumbnailContent(
                            Modifier
                                .padding(horizontal = 32.dp, vertical = 8.dp)
                        )
                    }

                    controlsContent(
                        Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }


            Queue(
                layoutState = playerBottomSheetState,
                content = {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
                        IconButton(
                            icon = R.drawable.infinite,
                            color = if (trackLoopEnabled) colorPalette.text else colorPalette.textDisabled,
                            onClick = { trackLoopEnabled = !trackLoopEnabled },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 14.dp)
                                .size(24.dp)
                        )

                        IconButton(
                            icon = R.drawable.ellipsis_horizontal,
                            color = colorPalette.text,
                            onClick = {
                                menuState.display {
                                    PlayerMenu(
                                        onDismiss = menuState::hide,
                                        mediaItem = mediaItem,
                                        binder = binder,
                                        onShowAudioInsights = { isShowingAudioInsights = true }
                                    )
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 14.dp)
                                .size(24.dp)
                        )
                    }
                },
                backgroundColorProvider = { colorPalette.background1 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@ExperimentalAnimationApi
@Composable
private fun PlayerMenu(
    binder: PlayerService.Binder,
    mediaItem: MediaItem,
    onDismiss: () -> Unit,
    onShowAudioInsights: () -> Unit,
) {
    val context = LocalContext.current

    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    BaseMediaItemMenu(
        mediaItem = mediaItem,
        onStartRadio = {
            binder.stopRadio()
            binder.player.seamlessPlay(mediaItem)
            binder.setupRadio(NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId))
        },
        onGoToEqualizer = {
            val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, binder.player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }

            try {
                activityResultLauncher.launch(intent)
            } catch (_: Exception) {
                try {
                    val soundSettingsIntent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(soundSettingsIntent)
                } catch (_: Exception) {
                    context.toast("Couldn't find an application to equalize audio")
                }
            }
        },
        onShowSleepTimer = { },
        onShowAudioInsights = onShowAudioInsights,
        onDismiss = onDismiss
    )
}
