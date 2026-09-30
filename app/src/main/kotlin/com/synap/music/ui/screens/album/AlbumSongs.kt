package com.synap.music.ui.screens.album

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.R
import com.synap.music.models.Song
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.ui.components.ShimmerHost
import com.synap.music.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.synap.music.ui.components.themed.LayoutWithAdaptiveThumbnail
import com.synap.music.ui.components.themed.NonQueuedMediaItemMenu
import com.synap.music.ui.items.SongItem
import com.synap.music.ui.items.SongItemPlaceholder
import com.synap.music.ui.styling.Dimensions
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.px
import com.synap.music.utils.asMediaItem
import com.synap.music.utils.center
import com.synap.music.utils.color
import com.synap.music.utils.forcePlayAtIndex
import com.synap.music.utils.isLandscape
import com.synap.music.utils.MediaFetchUtils
import com.synap.music.utils.semiBold
import com.synap.music.MainApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.synap.music.utils.toast
import androidx.compose.ui.platform.LocalContext
import com.synap.music.utils.secondary

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun AlbumSongs(
    albumId: String,
    albumTitle: String?,
    albumThumbnailUrl: String?,
    songs: List<Song>,
    isLoading: Boolean,
    selection: MutableList<String>,
    thumbnailContent: @Composable () -> Unit,
) {
    val (colorPalette, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val context = LocalContext.current

    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSizePx = thumbnailSizeDp.px

    val lazyListState = rememberLazyListState()

    val layoutDirection = LocalLayoutDirection.current
    val paddingValues = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    val contentPadding = PaddingValues(
        start = paddingValues.calculateStartPadding(layoutDirection),
        top = paddingValues.calculateTopPadding() + 80.dp,
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
                    .fillMaxSize()
            ) {
                item(
                    key = "header",
                    contentType = 0
                ) {
                    if (!isLandscape) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            thumbnailContent()
                        }
                    }
                }

                itemsIndexed(
                    items = songs,
                    key = { _, song -> song.id }
                ) { index, song ->
                    val isSelected = selection.contains(song.id)
                    SongItem(
                        song = song,
                        thumbnailSizePx = thumbnailSizePx,
                        thumbnailSizeDp = thumbnailSizeDp,
                        thumbnailUrlOverride = albumThumbnailUrl,
                        onThumbnailContent = {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(colorPalette.accent.copy(alpha = 0.4f))
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.checkmark),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(colorPalette.onAccent),
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(24.dp)
                                    )
                                }
                            } else {
                                BasicText(
                                    text = "${index + 1}",
                                    style = typography.s.semiBold.center.color(colorPalette.textDisabled),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .width(thumbnailSizeDp)
                                        .align(Alignment.Center)
                                )
                            }
                        },
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = {
                                    if (selection.isNotEmpty()) {
                                        if (isSelected) selection.remove(song.id) else selection.add(song.id)
                                    } else {
                                        menuState.display {
                                            NonQueuedMediaItemMenu(
                                                onDismiss = menuState::hide,
                                                mediaItem = song.asMediaItem(albumId, albumTitle, albumThumbnailUrl),
                                                onMultipleDelete = { selection.add(song.id) }
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    if (selection.isNotEmpty()) {
                                        if (isSelected) selection.remove(song.id) else selection.add(song.id)
                                    } else {
                                        binder?.stopRadio()
                                        binder?.player?.forcePlayAtIndex(
                                            songs.map { it.asMediaItem(albumId, albumTitle, albumThumbnailUrl) },
                                            index,
                                            context
                                        )
                                    }
                                }
                            )
                            .then(if (isSelected) Modifier.background(colorPalette.accent.copy(alpha = 0.3f)) else Modifier)
                    )
                }

                if (songs.isEmpty()) {
                    item(key = "empty") {
                        if (isLoading) {
                            ShimmerHost(
                                modifier = Modifier
                                    .fillParentMaxSize()
                            ) {
                                repeat(8) {
                                    SongItemPlaceholder(thumbnailSizeDp = Dimensions.thumbnails.song)
                                }
                            }
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(vertical = 64.dp)
                            ) {
                                BasicText(
                                    text = "No songs found for this album",
                                    style = typography.s.secondary.center,
                                )
                            }
                        }
                    }
                }
            }

            FloatingActionsContainerWithScrollToTop(
                lazyListState = lazyListState,
                iconId = if (selection.isEmpty()) R.drawable.shuffle else null,
                secondaryIconId = if (selection.isEmpty()) R.drawable.download else null,
                onClick = {
                    if (songs.isNotEmpty()) {
                        binder?.stopRadio()
                        binder?.player?.forcePlayAtIndex(
                            songs.map { it.asMediaItem(albumId, albumTitle, albumThumbnailUrl) },
                            (0 until songs.size).random(),
                            context
                        )
                    }
                },
                onSecondaryClick = {
                    if (songs.isNotEmpty()) {
                        context.toast("Starting download for ${songs.size} songs")
                        songs.forEach { song ->
                            MainApplication.scope.launch(Dispatchers.IO) {
                                MediaFetchUtils.downloadMedia(context, song.asMediaItem(albumId, albumTitle, albumThumbnailUrl), false)
                            }
                        }
                    }
                }
            )
        }
    }
}
