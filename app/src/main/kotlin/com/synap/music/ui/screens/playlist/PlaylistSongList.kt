package com.synap.music.ui.screens.playlist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.synap.compose.persist.persist
import com.synap.pulse.SynapService
import com.synap.pulse.requests.lyrics
import com.synap.pulse.models.bodies.NextBody
import com.synap.pulse.models.bodies.BrowseBody
import com.synap.pulse.requests.playlistPage
import com.synap.music.Database
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.R
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.ui.components.ShimmerHost
import com.synap.music.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.synap.music.ui.components.themed.LayoutWithAdaptiveThumbnail
import com.synap.music.ui.components.themed.NonQueuedMediaItemMenu
import com.synap.music.ui.components.themed.adaptiveThumbnailContent
import com.synap.music.ui.items.SongItem
import com.synap.music.ui.items.SongItemPlaceholder
import com.synap.music.ui.styling.Dimensions
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.px
import com.synap.music.utils.asMediaItem
import com.synap.music.utils.completed
import com.synap.music.utils.forcePlayAtIndex
import com.synap.music.utils.forcePlayFromBeginning
import com.synap.music.utils.isLandscape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun PlaylistSongList(
    browseId: String,
    onPlaylistPageLoaded: (SynapService.PlaylistOrAlbumPage?) -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current

    var playlistPage by persist<SynapService.PlaylistOrAlbumPage?>("playlist/$browseId/playlistPage")

    LaunchedEffect(Unit) {
        if ((playlistPage != null) && (playlistPage?.songsPage?.continuation == null)) {
            onPlaylistPageLoaded(playlistPage)
            return@LaunchedEffect
        }

        playlistPage = withContext(Dispatchers.IO) {
            SynapService.playlistPage(BrowseBody(browseId = browseId))?.completed()?.getOrNull()
        }.also {
            onPlaylistPageLoaded(it)
        }
    }

    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px

    val thumbnailContent = adaptiveThumbnailContent(playlistPage == null, playlistPage?.thumbnail?.url)

    val lazyListState = rememberLazyListState()

    val layoutDirection = LocalLayoutDirection.current
    val paddingValues = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    val contentPadding = PaddingValues(
        start = paddingValues.calculateStartPadding(layoutDirection),
        top = paddingValues.calculateTopPadding() + 64.dp,
        end = paddingValues.calculateEndPadding(layoutDirection),
        bottom = paddingValues.calculateBottomPadding()
    )

    LayoutWithAdaptiveThumbnail(thumbnailContent = thumbnailContent) {
        Box {
            LazyColumn(
                state = lazyListState,
                contentPadding = contentPadding,
                modifier = Modifier
                    .background(colorPalette.background0)
                    .fillMaxSize(),
            ) {
                item(
                    key = "header",
                    contentType = 0,
                ) {
                    if (!isLandscape) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                        ) {
                            thumbnailContent()
                        }
                    }
                }

                itemsIndexed(items = playlistPage?.songsPage?.list ?: emptyList()) { index, song ->
                    SongItem(
                        song = song,
                        thumbnailSizePx = songThumbnailSizePx,
                        thumbnailSizeDp = songThumbnailSizeDp,
                        modifier = Modifier
                            .combinedClickable(
                            onLongClick = {
                                menuState.display {
                                    NonQueuedMediaItemMenu(
                                        onDismiss = menuState::hide,
                                        mediaItem = song.asMediaItem,
                                    )
                                }
                            },
                        ) {
                            val mediaItems = playlistPage?.songsPage?.list
                                ?.map(SynapService.SongItem::asMediaItem)
                                ?: return@combinedClickable

                            binder?.stopRadio()
                            binder?.player?.forcePlayAtIndex(mediaItems, index)
                        },
                    )
                }

                if (playlistPage == null) {
                    item(key = "loading") {
                        ShimmerHost(
                            modifier = Modifier
                                .fillParentMaxSize(),
                        ) {
                            repeat(4) {
                                SongItemPlaceholder(thumbnailSizeDp = songThumbnailSizeDp)
                            }
                        }
                    }
                }
            }

            FloatingActionsContainerWithScrollToTop(
                lazyListState = lazyListState,
                iconId = R.drawable.shuffle,
                onClick = {
                    playlistPage?.songsPage?.list?.let { songs ->
                        if (songs.isNotEmpty()) {
                            val mediaItems = songs.shuffled().map(SynapService.SongItem::asMediaItem)
                            binder?.stopRadio()
                            binder?.player?.forcePlayFromBeginning(
                                mediaItems = mediaItems,
                            )
                        }
                    }
                },
            )
        }
    }
}
