package com.synap.music.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.asPaddingValues
import com.synap.compose.persist.persist
import com.synap.music.Database
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.R
import com.synap.music.enums.AlbumSortBy
import com.synap.music.enums.SortOrder
import com.synap.music.models.Album
import com.synap.music.models.AlbumWithDownloadStatus
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.ui.components.ShimmerHost
import com.synap.music.ui.components.themed.AlbumMenu
import com.synap.music.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.synap.music.ui.components.themed.InLibraryAlbumMenu
import com.synap.music.ui.items.AlbumItem
import com.synap.music.ui.items.AlbumItemPlaceholder
import com.synap.music.ui.styling.Dimensions
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.px
import com.synap.music.utils.albumSortByKey
import com.synap.music.utils.albumSortOrderKey
import com.synap.music.utils.rememberPreference
import com.synap.pulse.SynapService
import com.synap.pulse.models.bodies.SearchBody
import com.synap.pulse.requests.searchPage
import com.synap.pulse.utils.from
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * The Albums Library Screen.
 * Shows all the albums you've saved or downloaded.
 * If you search, it seamlessly switches to an online search to find 
 * new albums from the YouTube catalog.
 */
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun HomeAlbums(
    onAlbumClick: (Album) -> Unit,
    onSearchClick: (SearchMode) -> Unit,
    searchQuery: String = "",
    selection: MutableList<String>,
    onIdsChanged: (List<String>) -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val menuState = LocalMenuState.current

    var sortBy by rememberPreference(albumSortByKey, AlbumSortBy.DateAdded)
    var sortOrder by rememberPreference(albumSortOrderKey, SortOrder.Descending)

    var albumWithStatusItems by persist<List<AlbumWithDownloadStatus>>(tag = "home/albums/v4", emptyList())

    // Observe the local database for saved albums.
    LaunchedEffect(sortBy, sortOrder) {
        Database.albumsWithStatus(sortBy, sortOrder).collect { 
            albumWithStatusItems = it 
        }
    }

    var onlineSearchItems by remember { mutableStateOf<List<SynapService.AlbumItem>>(emptyList()) }
    var isSearchingOnline by remember { mutableStateOf(false) }

    /**
     * Online Search logic. 
     * If the user types in the search box, we wait 500ms (debounce) 
     * and then hit the YouTube API to find matching albums.
     */
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            isSearchingOnline = true
            delay(500.milliseconds)
            val result = SynapService.searchPage(
                body = SearchBody(
                    query = searchQuery,
                    params = SynapService.SearchFilter.Album.value
                ),
                fromMusicShelfRendererContent = { SynapService.AlbumItem.from(it) },
                fromMusicTwoRowItemRenderer = { SynapService.AlbumItem.from(it) }
            )
            onlineSearchItems = result?.getOrNull()?.list ?: emptyList()
            isSearchingOnline = false
        } else {
            onlineSearchItems = emptyList()
        }
    }

    val filteredItems = remember(albumWithStatusItems, searchQuery) {
        val uniqueAlbums = albumWithStatusItems.distinctBy { it.album.id }
        if (searchQuery.isEmpty()) uniqueAlbums else uniqueAlbums.filter {
            it.album.title?.contains(searchQuery, ignoreCase = true) == true || 
            it.album.authorsText?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    val thumbnailSizeDp = Dimensions.thumbnails.song * 2
    val thumbnailSizePx = thumbnailSizeDp.px

    val lazyListState = rememberLazyListState()

    val layoutDirection = LocalLayoutDirection.current
    val paddingValues = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    val contentPadding = PaddingValues(
        start = paddingValues.calculateStartPadding(layoutDirection),
        top = paddingValues.calculateTopPadding() + 72.dp + 8.dp,
        end = paddingValues.calculateEndPadding(layoutDirection),
        bottom = paddingValues.calculateBottomPadding()
    )

    Box {
        LazyColumn(
            state = lazyListState,
            contentPadding = contentPadding,
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            if (searchQuery.isEmpty()) {
                items(
                    items = filteredItems,
                    key = { it.album.id }
                ) { albumWithStatus ->
                    val album = albumWithStatus.album
                    val isSelected = selection.contains(album.id)
                    
                    AlbumItem(
                        albumWithStatus = albumWithStatus,
                        thumbnailSizePx = thumbnailSizePx,
                        thumbnailSizeDp = thumbnailSizeDp,
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { 
                                    if (selection.isNotEmpty()) {
                                        if (isSelected) selection.remove(album.id) else selection.add(album.id)
                                    } else {
                                        onAlbumClick(album) 
                                    }
                                },
                                onLongClick = {
                                    if (selection.isNotEmpty()) {
                                        if (isSelected) selection.remove(album.id) else selection.add(album.id)
                                    } else {
                                        menuState.display {
                                            InLibraryAlbumMenu(
                                                album = album,
                                                onDismiss = menuState::hide,
                                                onMultipleDelete = { selection.add(album.id) }
                                            )
                                        }
                                    }
                                }
                            )
                            .then(if (isSelected) Modifier.background(colorPalette.accent.copy(alpha = 0.3f)) else Modifier)
                            .animateItemPlacement(),
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
                                            .size(32.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            } else {
                if (isSearchingOnline) {
                    item {
                        ShimmerHost {
                            repeat(8) {
                                AlbumItemPlaceholder(
                                    thumbnailSizeDp = thumbnailSizeDp,
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = onlineSearchItems,
                        key = { it.key }
                    ) { album ->
                        AlbumItem(
                            album = album,
                            thumbnailSizePx = thumbnailSizePx,
                            thumbnailSizeDp = thumbnailSizeDp,
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = {
                                        onAlbumClick(Album(
                                            id = album.key,
                                            title = album.info?.name,
                                            thumbnailUrl = album.thumbnail?.url,
                                            authorsText = album.authors?.joinToString(", ") { it.name ?: "" }
                                        ))
                                    },
                                    onLongClick = {
                                        menuState.display {
                                            AlbumMenu(
                                                album = album,
                                                onDismiss = menuState::hide
                                            )
                                        }
                                    }
                                )
                        )
                    }
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(
            lazyListState = lazyListState,
            iconId = if (selection.isNotEmpty()) null else R.drawable.search,
            onClick = { onSearchClick(SearchMode.None) }
        )
    }

    LaunchedEffect(filteredItems) {
        onIdsChanged(filteredItems.map { it.album.id })
    }
}
