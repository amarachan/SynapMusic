package com.synap.music.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.synap.music.Database
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.service.LoginRequiredException
import com.synap.music.service.PlayableFormatNotFoundException
import com.synap.music.service.UnplayableException
import com.synap.music.service.VideoIdMismatchException
import com.synap.music.ui.styling.Dimensions
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.px
import com.synap.music.utils.currentWindow
import com.synap.music.utils.DisposableListener
import com.synap.music.utils.thumbnail
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

/**
 * The Large Artwork / Video Player UI.
 * This component is "Hybrid":
 * - If the track is an Official Music Video, it embeds a real video player (ExoPlayer).
 * - If it's a song, it renders a high-res cover art with smooth transitions 
 *   as you skip tracks.
 * It also acts as the container for the Lyrics and Audio Insights overlays.
 */
@ExperimentalAnimationApi
@Composable
fun Thumbnail(
    isShowingLyrics: Boolean,
    onShowLyrics: (Boolean) -> Unit,
    isShowingAudioInsights: Boolean,
    onShowAudioInsights: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return

    val thumbnailSizeDp = Dimensions.thumbnails.player.song
    val thumbnailSizePx = 800

    // Track the player's current "Window" (the current song in the timeline).
    var nullableWindow by remember {
        mutableStateOf(player.currentWindow)
    }

    var error by remember {
        mutableStateOf<PlaybackException?>(player.playerError)
    }

    // Sync the local state with the player's real-time events.
    player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                nullableWindow = player.currentWindow
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                error = player.playerError
            }

            override fun onPlayerError(playbackException: PlaybackException) {
                error = playbackException
            }
        }
    }

    val window = nullableWindow ?: return

    val (colorPalette, _, thumbnailShape) = LocalAppearance.current
    val context = LocalContext.current

    /**
     * Page Transition Animation.
     * When you skip to the next song, the old artwork slides out and 
     * the new one scales and fades in for a fluid, physical feel.
     */
    AnimatedContent(
        targetState = window,
        transitionSpec = {
            val duration = 300
            val slideDirection =
                if (targetState.firstPeriodIndex > initialState.firstPeriodIndex) AnimatedContentScope.SlideDirection.Left else AnimatedContentScope.SlideDirection.Right

            ContentTransform(
                targetContentEnter = slideIntoContainer(
                    towards = slideDirection,
                    animationSpec = tween(duration)
                ) + fadeIn(
                    animationSpec = tween(duration)
                ) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(duration)
                ),
                initialContentExit = slideOutOfContainer(
                    towards = slideDirection,
                    animationSpec = tween(duration)
                ) + fadeOut(
                    animationSpec = tween(duration)
                ) + scaleOut(
                    targetScale = 0.95f,
                    animationSpec = tween(duration)
                ),
                sizeTransform = SizeTransform(clip = false)
            )
        },
        contentAlignment = Alignment.Center
    ) {currentWindow ->
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .clip(thumbnailShape)
                .size(thumbnailSizeDp)
        ) {
            val isVideo = currentWindow.mediaItem.mediaMetadata.extras?.getBoolean("isVideo") ?: false

            if (isVideo) {
                /**
                 * Video Mode.
                 * We wrap the native Android PlayerView so we can render 
                 * the high-definition video stream directly inside our UI.
                 */
                AndroidView<PlayerView>(
                    factory = { context ->
                        PlayerView(context).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            this.player = player
                        }
                    },
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures()
                        }
                        .fillMaxSize()
                )
            } else {
                /**
                 * Artwork Mode.
                 * Uses Coil to load high-res covers with a low-res placeholder 
                 * for instant visual feedback.
                 * (Artwork URIs are resolved via the fallback chain in Song.asMediaItem)
                 */
                val artworkUri = currentWindow.mediaItem.mediaMetadata.artworkUri
                val artworkUrl = artworkUri.thumbnail(thumbnailSizePx)
                val lowResUrl = artworkUri.thumbnail(120)
                
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(artworkUrl)
                        .placeholderMemoryCacheKey(lowResUrl?.toString())
                        .crossfade(true)
                        .build(),
                    placeholder = rememberAsyncImagePainter(artworkUri ?: lowResUrl),
                    error = rememberAsyncImagePainter(artworkUri ?: lowResUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .background(colorPalette.background2)
                        .pointerInput(Unit) {
                            detectTapGestures()
                        }
                        .fillMaxSize()
                )
            }

            Lyrics(
                mediaId = currentWindow.mediaItem.mediaId,
                isDisplayed = isShowingLyrics && error == null,
                onDismiss = { onShowLyrics(false) },
                ensureSongInserted = { Database.insert(currentWindow.mediaItem) },
                size = thumbnailSizeDp,
                mediaMetadataProvider = currentWindow.mediaItem::mediaMetadata,
                durationProvider = player::getDuration,
            )

            AudioInsights(
                mediaId = currentWindow.mediaItem.mediaId,
                isDisplayed = isShowingAudioInsights && error == null,
                onDismiss = { onShowAudioInsights(false) }
            )

            PlaybackError(
                isDisplayed = error != null,
                messageProvider = {
                    when (val cause = error?.cause?.cause ?: error?.cause ?: error) {
                        is UnresolvedAddressException, is UnknownHostException -> "A network error has occurred"
                        is PlayableFormatNotFoundException -> "Couldn't find a playable audio format"
                        is UnplayableException -> "The original video source of this song has been deleted"
                        is LoginRequiredException -> "This song cannot be played due to server restrictions"
                        is VideoIdMismatchException -> "The returned video id doesn't match the requested one"
                        else -> cause?.message ?: "An unknown playback error has occurred"
                    }
                },
                onDismiss = player::prepare
            )
        }
    }
}
