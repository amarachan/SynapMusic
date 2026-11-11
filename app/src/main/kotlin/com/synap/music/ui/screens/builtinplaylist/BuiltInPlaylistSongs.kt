package com.synap.music.ui.screens.builtinplaylist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import com.synap.music.utils.MediaFetchUtils
import com.synap.music.Database
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.R
import com.synap.music.enums.BuiltInPlaylist
import com.synap.music.enums.DiscoveryViewMode
import com.synap.music.enums.SongSortBy
import com.synap.music.enums.SortOrder
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as itemsIndexedGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.synap.music.models.Song
import com.synap.music.models.SongWithContentLength
import com.synap.music.query
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.synap.music.ui.components.themed.NonQueuedMediaItemMenu
import com.synap.music.ui.items.SongItem
import com.synap.music.ui.styling.Dimensions
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.px
import com.synap.music.utils.asMediaItem
import com.synap.music.utils.center
import com.synap.music.utils.forcePlayAtIndex
import com.synap.music.utils.forcePlayFromBeginning
import com.synap.music.utils.secondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import com.synap.music.ui.styling.card
import com.synap.music.utils.color
import com.synap.music.utils.semiBold
import com.synap.music.utils.medium

/**
 * The Song List for Built-in Playlists.
 * It observes the local database for changes in your "Liked" tracks 
 * or the "Vault" files, and supports both List and 2-column Grid view modes.
 */
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun BuiltInPlaylistSongs(
    builtInPlaylist: BuiltInPlaylist,
    selection: MutableList<String>,
    searchQuery: String = "",
    sortBy: SongSortBy = SongSortBy.DateAdded,
    sortOrder: SortOrder = SortOrder.Descending,
    viewMode: DiscoveryViewMode = DiscoveryViewMode.List,
    onIdsChanged: (List<String>) -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()

    var songs by remember { mutableStateOf<List<Song>?>(null) }
    val context = LocalContext.current

    /**
     * Database Observation.
     * We pick a different flow depending on the playlist type:
     * - Favorites: Every track with a non-null 'likedAt' timestamp.
     * - Offline: Every track that has a physical file associated with it.
     */
    LaunchedEffect(builtInPlaylist) {
        when (builtInPlaylist) {
            BuiltInPlaylist.Favorites -> Database
                .favorites()
                .map { favorites -> favorites.distinctBy { it.id } }

            BuiltInPlaylist.Offline -> Database
                .songsWithContentLength()
                .map { songs ->
                    Log.d("BuiltInPlaylistSongs", "Vault flow emitted ${songs.size} songs")
                    songs.map(SongWithContentLength::song).distinctBy { it.id }
                }
        }.flowOn(Dispatchers.IO).collect { songs = it }
    }

    val filteredSongs = remember(songs, searchQuery, sortBy, sortOrder) {
        val list = songs ?: return@remember null
        val searched = if (searchQuery.isBlank()) list else list.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    (it.artistsText?.contains(searchQuery, ignoreCase = true) == true)
        }

        when (sortBy) {
            SongSortBy.Title -> if (sortOrder == SortOrder.Ascending) searched.sortedBy { it.title } else searched.sortedByDescending { it.title }
            SongSortBy.PlayTime -> if (sortOrder == SortOrder.Ascending) searched.sortedBy { it.totalPlayTimeMs } else searched.sortedByDescending { it.totalPlayTimeMs }
            else -> if (sortOrder == SortOrder.Ascending) searched.reversed() else searched
        }
    }

    val thumbnailSizeDp = if (viewMode == DiscoveryViewMode.Grid) 180.dp else Dimensions.thumbnails.song
    val thumbnailSizePx = thumbnailSizeDp.px

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

    val layoutDirection = LocalLayoutDirection.current
    val paddingValues = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    val contentPadding = PaddingValues(
        start = paddingValues.calculateStartPadding(layoutDirection),
        top = paddingValues.calculateTopPadding() + 64.dp,
        end = paddingValues.calculateEndPadding(layoutDirection),
        bottom = paddingValues.calculateBottomPadding()
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (viewMode == DiscoveryViewMode.Grid) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = lazyGridState,
                contentPadding = contentPadding,
                modifier = Modifier
                    .background(colorPalette.background0)
                    .fillMaxSize()
            ) {
                val currentSongs = filteredSongs
                
                if (currentSongs == null) {
                    item(span = { GridItemSpan(2) }) {
                        BasicText(
                            text = "Loading songs...",
                            style = typography.s.secondary.center,
                            modifier = Modifier
                                .padding(all = 32.dp)
                                .fillMaxWidth()
                        )
                    }
                } else if (currentSongs.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        BasicText(
                            text = "No songs found in this collection",
                            style = typography.s.secondary.center,
                            modifier = Modifier
                                .padding(all = 32.dp)
                                .fillMaxWidth()
                        )
                    }
                } else {
                    itemsIndexedGrid(
                        items = currentSongs,
                        key = { index, song -> "${song.id}_$index" },
                        contentType = { _, song -> song },
                    ) { index, song ->
                        val isSelected = selection.contains(song.id)
                        val selectionBackgroundColor by animateColorAsState(
                            if (isSelected) colorPalette.accent.copy(alpha = 0.3f) else Color.Transparent
                        )

                        SongItem(
                            song = song,
                            thumbnailSizeDp = thumbnailSizeDp,
                            thumbnailSizePx = thumbnailSizePx,
                            isVertical = true,
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
                                }
                            },
                            modifier = Modifier
                                .padding(8.dp)
                                .background(selectionBackgroundColor)
                                .combinedClickable(
                                    onLongClick = {
                                        if (selection.isNotEmpty()) {
                                            if (isSelected) selection.remove(song.id) else selection.add(song.id)
                                        } else {
                                            menuState.display {
                                                when (builtInPlaylist) {
                                                    BuiltInPlaylist.Favorites -> NonQueuedMediaItemMenu(
                                                        mediaItem = song.asMediaItem,
                                                        onDismiss = menuState::hide,
                                                        onMultipleDelete = { selection.add(song.id) }
                                                    )

                                                    BuiltInPlaylist.Offline -> NonQueuedMediaItemMenu(
                                                        mediaItem = song.asMediaItem,
                                                        onDismiss = menuState::hide,
                                                        showSaveToVault = false,
                                                        onMultipleDelete = {
                                                            selection.add(song.id)
                                                        },
                                                        onDelete = {
                                                            coroutineScope.launch(Dispatchers.IO) {
                                                                val file = MediaFetchUtils.getAudioFile(context, song.id)
                                                                if (file.exists()) file.delete()
                                                                query {
                                                                    Database.deleteFormat(song.id)
                                                                    Database.clearEventsFor(song.id)
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        if (selection.isNotEmpty()) {
                                            if (isSelected) selection.remove(song.id) else selection.add(song.id)
                                        } else {
                                            binder?.stopRadio()
                                            binder?.player?.forcePlayAtIndex(
                                                currentSongs.map(Song::asMediaItem),
                                                index
                                            )
                                        }
                                    }
                                )
                                .animateItemPlacement()
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = lazyListState,
                contentPadding = contentPadding,
                modifier = Modifier
                    .background(colorPalette.background0)
                    .fillMaxSize()
            ) {
                val currentSongs = filteredSongs
                
                if (currentSongs == null) {
                    item {
                        BasicText(
                            text = "Loading songs...",
                            style = typography.s.secondary.center,
                            modifier = Modifier
                                .padding(all = 32.dp)
                                .fillMaxWidth()
                        )
                    }
                } else if (currentSongs.isEmpty()) {
                    item {
                        BasicText(
                            text = "No songs found in this collection",
                            style = typography.s.secondary.center,
                            modifier = Modifier
                                .padding(all = 32.dp)
                                .fillMaxWidth()
                        )
                    }
                } else {
                    itemsIndexed(
                        items = currentSongs,
                        key = { index, song -> "${song.id}_$index" },
                        contentType = { _, song -> song },
                    ) { index, song ->
                        val isSelected = selection.contains(song.id)
                        val selectionBackgroundColor by animateColorAsState(
                            if (isSelected) colorPalette.accent.copy(alpha = 0.3f) else Color.Transparent
                        )

                        SongItem(
                            song = song,
                            thumbnailSizeDp = thumbnailSizeDp,
                            thumbnailSizePx = thumbnailSizePx,
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
                                }
                            },
                            modifier = Modifier
                                .background(selectionBackgroundColor)
                                .combinedClickable(
                                    onLongClick = {
                                        if (selection.isNotEmpty()) {
                                            if (isSelected) selection.remove(song.id) else selection.add(song.id)
                                        } else {
                                            menuState.display {
                                                when (builtInPlaylist) {
                                                    BuiltInPlaylist.Favorites -> NonQueuedMediaItemMenu(
                                                        mediaItem = song.asMediaItem,
                                                        onDismiss = menuState::hide,
                                                        onMultipleDelete = { selection.add(song.id) }
                                                    )

                                                    BuiltInPlaylist.Offline -> NonQueuedMediaItemMenu(
                                                        mediaItem = song.asMediaItem,
                                                        onDismiss = menuState::hide,
                                                        showSaveToVault = false,
                                                        onMultipleDelete = {
                                                            selection.add(song.id)
                                                        },
                                                        onDelete = {
                                                            coroutineScope.launch(Dispatchers.IO) {
                                                                val file = MediaFetchUtils.getAudioFile(context, song.id)
                                                                if (file.exists()) file.delete()
                                                                query {
                                                                    Database.deleteFormat(song.id)
                                                                    Database.clearEventsFor(song.id)
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        if (selection.isNotEmpty()) {
                                            if (isSelected) selection.remove(song.id) else selection.add(song.id)
                                        } else {
                                            binder?.stopRadio()
                                            binder?.player?.forcePlayAtIndex(
                                                currentSongs.map(Song::asMediaItem),
                                                index
                                            )
                                        }
                                    }
                                )
                                .animateItemPlacement()
                        )
                    }
                }
            }
        }

        if (viewMode == DiscoveryViewMode.Grid) {
            FloatingActionsContainerWithScrollToTop(
                lazyGridState = lazyGridState,
                iconId = if (selection.isNotEmpty()) null else R.drawable.shuffle,
                onClick = {
                    songs?.let { songsList ->
                        if (songsList.isNotEmpty()) {
                            binder?.stopRadio()
                            binder?.player?.forcePlayFromBeginning(
                                songsList.shuffled().map(Song::asMediaItem)
                            )
                        }
                    }
                }
            )
        } else {
            FloatingActionsContainerWithScrollToTop(
                lazyListState = lazyListState,
                iconId = if (selection.isNotEmpty()) null else R.drawable.shuffle,
                onClick = {
                    songs?.let { songsList ->
                        if (songsList.isNotEmpty()) {
                            binder?.stopRadio()
                            binder?.player?.forcePlayFromBeginning(
                                songsList.shuffled().map(Song::asMediaItem)
                            )
                        }
                    }
                }
            )
        }
    }

    LaunchedEffect(songs) {
        onIdsChanged(songs?.map { it.id } ?: emptyList())
    }
}
