package com.synap.music.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.asPaddingValues
import com.synap.compose.persist.persistList
import com.synap.music.Database
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.R
import com.synap.music.enums.ArtistSortBy
import com.synap.music.enums.SortOrder
import com.synap.music.models.Artist
import com.synap.music.models.ArtistWithDownloadStatus
import com.synap.music.query
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.ui.components.ShimmerHost
import com.synap.music.ui.components.themed.ConfirmationDialog
import com.synap.music.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.synap.music.ui.components.themed.InLibraryArtistMenu
import com.synap.music.ui.components.themed.NonQueuedMediaItemMenu
import com.synap.music.ui.items.ArtistItem
import com.synap.music.ui.items.ArtistItemPlaceholder
import com.synap.music.ui.items.SongItem
import com.synap.music.ui.styling.Dimensions
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.px
import com.synap.music.ui.styling.card
import com.synap.music.utils.artistSortByKey
import com.synap.music.utils.artistSortOrderKey
import com.synap.music.utils.asMediaItem
import com.synap.music.utils.forcePlay
import com.synap.music.utils.rememberPreference
import com.synap.music.utils.toast
import com.synap.music.utils.MediaFetchUtils
import com.synap.pulse.SynapService
import com.synap.pulse.models.NavigationEndpoint
import com.synap.pulse.models.bodies.SearchBody
import com.synap.pulse.models.bodies.BrowseBody
import com.synap.pulse.requests.searchPage
import com.synap.pulse.requests.artistPage
import com.synap.pulse.external.TheAudioDB
import com.synap.pulse.utils.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * The Artists Library Screen.
 * Shows all the artists you've saved.
 * It includes an "Automatic Thumbnail Repair" engine that tries to find 
 * high-res portraits from TheAudioDB if the YouTube ones are blurry.
 */
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun HomeArtistList(
    onArtistClick: (Artist) -> Unit,
    onSearchClick: (SearchMode) -> Unit,
    searchQuery: String = "",
    selection: MutableList<String>,
    onIdsChanged: (List<String>) -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val menuState = LocalMenuState.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var sortBy by rememberPreference(artistSortByKey, ArtistSortBy.DateAdded)
    var sortOrder by rememberPreference(artistSortOrderKey, SortOrder.Descending)

    var artistWithStatusItems by persistList<ArtistWithDownloadStatus>("home/artists/v3")

    LaunchedEffect(sortBy, sortOrder) {
        Database.artistsWithStatus(sortBy, sortOrder).collect { artistWithStatusItems = it }
    }

    var isShowingDeleteDialog by remember { mutableStateOf(false) }

    if (isShowingDeleteDialog) {
        ConfirmationDialog(
            text = "Are you sure you want to delete ${selection.size} artists from your library and storage?",
            onDismiss = { isShowingDeleteDialog = false },
            onConfirm = {
                val selectedIds = selection.toList()
                selection.clear()
                coroutineScope.launch {
                    selectedIds.forEach { id ->
                        val songs = Database.artistSongs(id).firstOrNull() ?: emptyList()
                        query {
                            songs.forEach { song ->
                                val file = MediaFetchUtils.getAudioFile(context, song.id)
                                if (file.exists()) file.delete()
                                Database.deleteFormat(song.id)
                            }
                            Database.getArtist(id)?.let { Database.delete(it) }
                        }
                    }
                    context.toast("Deleted ${selectedIds.size} artists")
                }
            }
        )
    }

    var onlineSearchItems by remember { mutableStateOf<List<SynapService.ArtistItem>>(emptyList()) }
    var isSearchingOnline by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            isSearchingOnline = true
            delay(500.milliseconds)
            val result = SynapService.searchPage(
                body = SearchBody(
                    query = searchQuery,
                    params = SynapService.SearchFilter.Artist.value
                ),
                fromMusicTwoRowItemRenderer = { SynapService.ArtistItem.from(it) },
                fromMusicShelfRendererContent = { SynapService.ArtistItem.from(it) }
            )
            onlineSearchItems = result?.getOrNull()?.list ?: emptyList()
            isSearchingOnline = false
        } else {
            onlineSearchItems = emptyList()
        }
    }

    val filteredItems = remember(artistWithStatusItems, searchQuery) {
        if (searchQuery.isEmpty()) artistWithStatusItems else artistWithStatusItems.filter {
            it.artist.name?.contains(searchQuery, ignoreCase = true) == true
        }
    }
    
    /**
     * Primary Artist Thumbnail Parallel Auto-Resolver.
     * Uses Kotlin Coroutines parallel batch execution (chunked in groups of 4)
     * to resolve missing high-res artist portraits in parallel without blocking 
     * the main UI thread.
     */
    LaunchedEffect(filteredItems) {
        val missingItems = filteredItems.filter { it.artist.thumbnailUrl.isNullOrBlank() }
        if (missingItems.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                missingItems.chunked(4).forEach { batch ->
                    supervisorScope {
                        batch.map { item ->
                            async {
                                val artistName = item.artist.name ?: return@async
                                var resolvedThumb: String? = null

                                // 1. Try TheAudioDB for professional HD portrait
                                TheAudioDB.getArtistInfo(artistName).onSuccess { tdbArtist ->
                                    resolvedThumb = tdbArtist?.strArtistThumb
                                }

                                // 2. If TheAudioDB is empty, query YouTube Music for official headshot
                                if (resolvedThumb.isNullOrBlank()) {
                                    if (item.artist.id.startsWith("synthetic_artist_")) {
                                        val searchResult = SynapService.searchPage(
                                            body = SearchBody(
                                                query = artistName,
                                                params = SynapService.SearchFilter.Artist.value
                                            ),
                                            fromMusicTwoRowItemRenderer = { SynapService.ArtistItem.from(it) },
                                            fromMusicShelfRendererContent = { SynapService.ArtistItem.from(it) }
                                        )
                                        resolvedThumb = searchResult?.getOrNull()?.list?.firstOrNull()?.thumbnail?.url
                                    } else {
                                        val result = SynapService.artistPage(BrowseBody(browseId = item.artist.id))
                                        resolvedThumb = result?.getOrNull()?.thumbnail?.url
                                    }
                                }

                                // 3. Update Room DB so the picture shows up immediately
                                if (!resolvedThumb.isNullOrBlank()) {
                                    query {
                                        Database.getArtist(item.artist.id)?.let { current ->
                                            Database.upsert(
                                                current.copy(
                                                    thumbnailUrl = resolvedThumb,
                                                    timestamp = System.currentTimeMillis()
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }.awaitAll()
                    }
                }
            }
        }
    }

    val thumbnailSizeDp = Dimensions.thumbnails.song * 2
    val thumbnailSizePx = thumbnailSizeDp.px

    val lazyGridState = rememberLazyGridState()

    val layoutDirection = LocalLayoutDirection.current
    val paddingValues = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    val contentPadding = PaddingValues(
        start = paddingValues.calculateStartPadding(layoutDirection),
        top = paddingValues.calculateTopPadding() + 72.dp + 8.dp,
        end = paddingValues.calculateEndPadding(layoutDirection),
        bottom = paddingValues.calculateBottomPadding()
    )

    Box {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Adaptive(Dimensions.thumbnails.song * 2 + Dimensions.itemsVerticalPadding * 2),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(Dimensions.itemsVerticalPadding * 2),
            horizontalArrangement = Arrangement.spacedBy(
                space = Dimensions.itemsVerticalPadding * 2,
                alignment = Alignment.CenterHorizontally
            ),
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            if (searchQuery.isEmpty()) {
                items(
                    items = filteredItems,
                    key = { it.artist.id }
                ) { artistWithStatus ->
                    val artist = artistWithStatus.artist
                    val isSelected = selection.contains(artist.id)
                    ArtistItem(
                        artistWithStatus = artistWithStatus,
                        thumbnailSizePx = thumbnailSizePx,
                        thumbnailSizeDp = thumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { 
                                    if (selection.isNotEmpty()) {
                                        if (isSelected) selection.remove(artist.id) else selection.add(artist.id)
                                    } else {
                                        onArtistClick(artist) 
                                    }
                                },
                                onLongClick = {
                                    if (selection.isNotEmpty()) {
                                        if (isSelected) selection.remove(artist.id) else selection.add(artist.id)
                                    } else {
                                        menuState.display {
                                            InLibraryArtistMenu(
                                                artist = artist,
                                                onDismiss = menuState::hide,
                                                onMultipleDelete = { selection.add(artist.id) }
                                            )
                                        }
                                    }
                                }
                            )
                            .then(if (isSelected) Modifier.background(colorPalette.accent.copy(alpha = 0.3f)) else Modifier)
                            .animateItemPlacement()
                    )
                }
            } else {
                if (isSearchingOnline) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ShimmerHost {
                            repeat(8) {
                                ArtistItemPlaceholder(
                                    thumbnailSizeDp = thumbnailSizeDp,
                                    alternative = true
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = onlineSearchItems,
                        key = { it.key }
                    ) { artist ->
                        ArtistItem(
                            artist = artist,
                            thumbnailSizePx = thumbnailSizePx,
                            thumbnailSizeDp = thumbnailSizeDp,
                            alternative = true,
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = { 
                                        onArtistClick(Artist(
                                            id = artist.key,
                                            name = artist.info?.name,
                                            thumbnailUrl = artist.thumbnail?.url
                                        )) 
                                    },
                                    onLongClick = {
                                        // Artist menu if needed
                                    }
                                )
                        )
                    }
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(
            lazyGridState = lazyGridState,
            iconId = if (selection.isNotEmpty()) null else R.drawable.search,
            onClick = { onSearchClick(SearchMode.None) }
        )
    }

    LaunchedEffect(filteredItems) {
        onIdsChanged(filteredItems.map { it.artist.id })
    }
}
