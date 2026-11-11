package com.synap.music.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.asPaddingValues
import com.synap.compose.persist.persistList
import com.synap.music.Database
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.R
import com.synap.music.enums.SongSortBy
import com.synap.music.enums.SortOrder
import com.synap.music.models.Song
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.synap.music.ui.components.themed.InHistoryMediaItemMenu
import com.synap.music.ui.items.SongItem
import com.synap.music.ui.styling.Dimensions
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.onOverlay
import com.synap.music.ui.styling.overlay
import com.synap.music.ui.styling.px
import androidx.compose.ui.platform.LocalContext
import com.synap.music.utils.asMediaItem
import com.synap.music.utils.center
import com.synap.music.utils.color
import com.synap.music.utils.forcePlayAtIndex
import com.synap.music.utils.rememberPreference
import com.synap.music.utils.semiBold
import com.synap.music.utils.songSortByKey
import com.synap.music.utils.songSortOrderKey

/**
 * The Songs Library Screen.
 * Shows every track you've downloaded or liked in a single organized list.
 * Supports sorting by date, play count, and title.
 */
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun HomeSongs(
    onSearchClick: (SearchMode) -> Unit,
    searchQuery: String = "",
    selection: MutableList<String>,
    onIdsChanged: (List<String>) -> Unit
) {
    val (colorPalette, typography, thumbnailShape) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val context = LocalContext.current
    val menuState = LocalMenuState.current

    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSizePx = thumbnailSizeDp.px

    // User preferences for how this list should be sorted (Defaults to Recently Played History).
    val sortBy by rememberPreference(songSortByKey, SongSortBy.RecentlyPlayed)
    val sortOrder by rememberPreference(songSortOrderKey, SortOrder.Descending)

    var songItems by persistList<Song>("home/songs")

    // Automatically refresh the list whenever the database or sorting changes.
    LaunchedEffect(sortBy, sortOrder) {
        Database.songs(sortBy, sortOrder).collect { songItems = it.distinctBy { song -> song.id } }
    }

    // "Search as you type" filter for the local library.
    val filteredItems = remember(songItems, searchQuery) {
        if (searchQuery.isEmpty()) songItems else songItems.filter {
            it.title.contains(searchQuery, ignoreCase = true) || 
            it.artistsText?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    val lazyListState = rememberLazyListState()

    val layoutDirection = LocalLayoutDirection.current
    val paddingValues = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    val contentPadding = PaddingValues(
        start = paddingValues.calculateStartPadding(layoutDirection),
        top = paddingValues.calculateTopPadding() + 72.dp + 8.dp,
        end = paddingValues.calculateEndPadding(layoutDirection),
        bottom = paddingValues.calculateBottomPadding()
    )

    Box(
        modifier = Modifier
            .background(colorPalette.background0)
            .fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = contentPadding,
        ) {
            itemsIndexed(
                items = filteredItems,
                key = { index, song -> "${song.id}_$index" }
            ) { index, song ->
                val isSelected = selection.contains(song.id)
                
                SongItem(
                    song = song,
                    thumbnailSizePx = thumbnailSizePx,
                    thumbnailSizeDp = thumbnailSizeDp,
                    onThumbnailContent = {
                        if (sortBy == SongSortBy.PlayTime) {
                            BasicText(
                                text = song.formattedTotalPlayTime,
                                style = typography.xxs.semiBold.center.color(colorPalette.onOverlay),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, colorPalette.overlay)
                                        ),
                                        shape = thumbnailShape
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .align(Alignment.BottomCenter)
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
                                        InHistoryMediaItemMenu(
                                            song = song,
                                            onDismiss = menuState::hide,
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
                                        filteredItems.map(Song::asMediaItem),
                                        index,
                                        context
                                    )
                                }
                            }
                        )
                        .then(if (isSelected) Modifier.background(colorPalette.accent.copy(alpha = 0.3f)) else Modifier)
                        .animateItemPlacement()
                )
            }
        }

        FloatingActionsContainerWithScrollToTop(
            lazyListState = lazyListState,
            iconId = if (selection.isNotEmpty()) null else R.drawable.search,
            onClick = { onSearchClick(SearchMode.None) }
        )
    }

    LaunchedEffect(filteredItems) {
        onIdsChanged(filteredItems.map { it.id })
    }
}
