package com.synap.music.ui.screens.searchresult

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synap.compose.persist.PersistMapCleanup
import com.synap.pulse.SynapService
import com.synap.pulse.external.TheAudioDB
import com.synap.pulse.models.NavigationEndpoint
import com.synap.pulse.models.bodies.BrowseBody
import com.synap.pulse.models.bodies.ContinuationBody
import com.synap.pulse.models.bodies.SearchBody
import com.synap.pulse.requests.albumPage
import com.synap.pulse.requests.artistPage
import com.synap.pulse.requests.searchPage
import com.synap.pulse.utils.from
import com.synap.music.models.Artist
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.R
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.ui.components.ShimmerHost
import com.synap.music.ui.components.themed.AlbumMenu
import com.synap.music.ui.components.themed.ArtistMenu
import com.synap.music.ui.components.themed.PlaylistMenu
import com.synap.music.ui.components.themed.NonQueuedMediaItemMenu
import com.synap.music.ui.components.themed.Scaffold
import com.synap.music.ui.items.AlbumItem
import com.synap.music.ui.items.AlbumItemPlaceholder
import com.synap.music.ui.items.ArtistItem
import com.synap.music.ui.items.ArtistItemPlaceholder
import com.synap.music.ui.items.PlaylistItem
import com.synap.music.ui.items.PlaylistItemPlaceholder
import com.synap.music.ui.items.SongItem
import com.synap.music.ui.items.SongItemPlaceholder
import com.synap.music.ui.screens.albumRoute
import com.synap.music.ui.screens.albumDetailsRoute
import com.synap.music.ui.screens.artistRoute
import com.synap.music.ui.screens.playlistRoute
import com.synap.music.ui.styling.Dimensions
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.px
import com.synap.music.utils.asMediaItem
import com.synap.music.utils.bold
import com.synap.music.utils.semiBold
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import com.synap.music.utils.forcePlay
import com.synap.music.utils.forcePlayAtIndex
import com.synap.music.Database
import com.synap.music.ui.components.themed.HeaderIconButton
import com.synap.music.utils.searchResultScreenTabIndexKey
import com.synap.music.utils.rememberPreference
import com.synap.compose.routing.RouteHandlerScope
import com.synap.music.models.Album
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.synap.music.models.SongPlaylistMap
import com.synap.music.ui.components.themed.ConfirmationDialog
import com.synap.music.ui.components.themed.PlaylistSelectionDialog
import com.synap.music.ui.components.themed.SecondaryTextButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.synap.music.enums.DiscoveryFilter
import com.synap.music.enums.DiscoveryViewMode
import com.synap.music.enums.SongSortBy
import com.synap.music.enums.SortOrder
import com.synap.music.ui.components.themed.FilterButton
import com.synap.music.ui.components.themed.Menu
import com.synap.music.ui.components.themed.MenuEntry
import com.synap.music.ui.components.themed.SortOrderMenuEntry
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.synap.music.ui.screens.home.filterBySelectedLanguage
import com.synap.music.ui.screens.home.isGenuineSong
import com.synap.music.utils.discoveryLanguageKey
import com.synap.music.utils.filterAiSongsKey
import com.synap.music.utils.searchViewModeKey
import com.synap.music.utils.color
import com.synap.music.utils.medium
import com.synap.music.utils.secondary
import com.synap.music.utils.songSortByKey
import com.synap.music.utils.songSortOrderKey
import com.synap.music.utils.toast
import com.synap.music.utils.MediaFetchUtils
import com.synap.music.query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The Global Search Results screen. 
 * Shows results across 5 tabs: Songs, Albums, Artists, Videos, and Playlists.
 *
 * Features:
 * - Dynamic Search Filters: Language selection (appends query suffixes for YouTube Music API),
 *   Exclude AI Songs toggle, Grid/List View modes, and Title/Relevance sorting.
 * - 3-Tier Ultra-Prioritized Album Resolution Engine in the Albums tab.
 *   Guarantees official studio albums are pinned at the top when searching for tracks.
 * - Reactive multi-selection and bulk action handlers.
 */
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun RouteHandlerScope.SearchResultScreen(query: String) {
    val saveableStateHolder = rememberSaveableStateHolder()

    // Remember the last tab you were on so you don't lose your place.
    var tabIndex by rememberPreference(searchResultScreenTabIndexKey, defaultValue = 0)

    // Cleanup old search states to save memory.
    PersistMapCleanup(tagPrefix = "searchResults/$query/")

    val (colorPalette, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Multi-Select logic for bulk actions like "Add to Playlist".
    val selection = remember { mutableStateListOf<String>() }
    var isShowingPlaylistSelectionDialog by remember { mutableStateOf(false) }

    var filterAiSongs by rememberPreference(filterAiSongsKey, defaultValue = false)
    var searchViewMode by rememberPreference(searchViewModeKey, defaultValue = DiscoveryViewMode.List)
    var language by rememberPreference(discoveryLanguageKey, defaultValue = "None")
    var searchSortBy by rememberPreference(songSortByKey, defaultValue = SongSortBy.DateAdded)
    var searchSortOrder by rememberPreference(songSortOrderKey, defaultValue = SortOrder.Descending)

    var currentQuery by rememberSaveable { mutableStateOf(query) }

    // Logic for adding selected search results to a playlist.
    if (isShowingPlaylistSelectionDialog) {
        PlaylistSelectionDialog(
            onDismiss = { isShowingPlaylistSelectionDialog = false },
            onPlaylistSelected = { playlist, count ->
                isShowingPlaylistSelectionDialog = false
                val selectedIds = selection.toList()
                selection.clear()
                coroutineScope.launch {
                    query {
                        Database.addSongsToPlaylist(selectedIds, playlist.id, count)
                    }
                    context.toast("Added ${selectedIds.size} items to ${playlist.name}")
                }
            }
        )
    }

    Scaffold(
        title = {
            // Show either the current query search bar or the selection count.
            if (selection.isNotEmpty()) {
                BasicText(
                    text = "${selection.size} selected",
                    style = typography.m.semiBold
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp)
                        .background(colorPalette.background2, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                        modifier = Modifier.size(16.dp)
                    )

                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .weight(1f)
                    ) {
                        if (currentQuery.isEmpty()) {
                            BasicText(
                                text = "Search music...",
                                style = typography.s.medium.secondary
                            )
                        }

                        BasicTextField(
                            value = currentQuery,
                            onValueChange = { currentQuery = it },
                            textStyle = typography.s.semiBold.color(colorPalette.text),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (currentQuery.isNotEmpty()) {
                        HeaderIconButton(
                            icon = R.drawable.close,
                            color = colorPalette.textSecondary,
                            onClick = { currentQuery = "" }
                        )
                    }
                }
            }
        },
        topStartIconButtonId = if (selection.isNotEmpty()) R.drawable.close else R.drawable.chevron_back,
        onTopStartIconButtonClick = if (selection.isNotEmpty()) ({ selection.clear() }) else pop,
        topBarActions = {
            // Show the "Add to Playlist" icon when items are selected.
            if (selection.isNotEmpty()) {
                HeaderIconButton(
                    icon = R.drawable.playlist,
                    color = colorPalette.text,
                    onClick = { isShowingPlaylistSelectionDialog = true }
                )
            } else {
                FilterButton(
                    label = "Filter",
                    onShowMenu = {
                        menuState.display {
                            var currentSubMenu by remember { mutableStateOf<String?>(null) }

                            Menu {
                                when (currentSubMenu) {
                                    "Language" -> {
                                        MenuEntry(icon = R.drawable.chevron_back, text = "Back", onClick = { currentSubMenu = null })
                                        SearchResultLanguageSection(onSelect = { currentSubMenu = null })
                                    }
                                    "View" -> {
                                        MenuEntry(icon = R.drawable.chevron_back, text = "Back", onClick = { currentSubMenu = null })
                                        MenuEntry(
                                            icon = R.drawable.shapes,
                                            text = "Grid View",
                                            onClick = { searchViewMode = DiscoveryViewMode.Grid; currentSubMenu = null },
                                            trailingContent = if (searchViewMode == DiscoveryViewMode.Grid) ({ CheckmarkIcon() }) else null
                                        )
                                        MenuEntry(
                                            icon = R.drawable.reorder,
                                            text = "List View",
                                            onClick = { searchViewMode = DiscoveryViewMode.List; currentSubMenu = null },
                                            trailingContent = if (searchViewMode == DiscoveryViewMode.List) ({ CheckmarkIcon() }) else null
                                        )
                                    }
                                    else -> {
                                        MenuEntry(
                                            icon = R.drawable.chevron_back,
                                            text = "Back",
                                            onClick = { menuState.hide() }
                                        )

                                        MenuEntry(
                                            icon = R.drawable.text,
                                            text = "Language Filter",
                                            secondaryText = if (language == "None") "All Languages" else language,
                                            onClick = { currentSubMenu = "Language" }
                                        )

                                        MenuEntry(
                                            icon = R.drawable.disc,
                                            text = "Exclude AI Songs",
                                            secondaryText = if (filterAiSongs) "Enabled (Genuine Songs Only)" else "Disabled (Default)",
                                            onClick = {
                                                filterAiSongs = !filterAiSongs
                                            },
                                            trailingContent = if (filterAiSongs) ({ CheckmarkIcon() }) else null
                                        )

                                        MenuEntry(
                                            icon = R.drawable.shapes,
                                            text = "View Mode",
                                            secondaryText = if (searchViewMode == DiscoveryViewMode.Grid) "Grid View" else "List View",
                                            onClick = { currentSubMenu = "View" }
                                        )

                                        Spacer(Modifier.padding(vertical = 4.dp))

                                        MenuEntry(
                                            icon = R.drawable.text,
                                            text = "Sort by Title",
                                            onClick = {
                                                searchSortBy = SongSortBy.Title
                                            },
                                            trailingContent = if (searchSortBy == SongSortBy.Title) ({ CheckmarkIcon() }) else null
                                        )

                                        MenuEntry(
                                            icon = R.drawable.time,
                                            text = "Sort by Relevance",
                                            onClick = {
                                                searchSortBy = SongSortBy.DateAdded
                                            },
                                            trailingContent = if (searchSortBy == SongSortBy.DateAdded) ({ CheckmarkIcon() }) else null
                                        )

                                        SortOrderMenuEntry(
                                            sortOrder = searchSortOrder,
                                            onSortOrderChanged = { newOrder ->
                                                searchSortOrder = newOrder
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        },
        tabIndex = tabIndex,
        onTabChanged = { tabIndex = it },
        tabColumnContent = { Item ->
            Item(0, "Songs", R.drawable.musical_notes)
            Item(1, "Albums", R.drawable.disc)
            Item(2, "Artists", R.drawable.shapes)
            Item(3, "Videos", R.drawable.film)
            Item(4, "Playlists", R.drawable.playlist)
        }
    ) { currentTabIndex ->
        saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
            val emptyItemsText = "No results found for this search"

            val selectedLanguageObj = DiscoveryFilter.Language.entries.find {
                it::class.simpleName.equals(language, ignoreCase = true) ||
                it.label.equals(language, ignoreCase = true) ||
                it.querySuffix.equals(language, ignoreCase = true)
            }
            val langSuffix = selectedLanguageObj?.querySuffix ?: ""
            val activeTerm = currentQuery.ifBlank { query }
            val effectiveQuery = if (langSuffix.isNotEmpty()) "$activeTerm $langSuffix" else activeTerm
            val filterTag = "$effectiveQuery/$language/$filterAiSongs/$searchSortBy/$searchSortOrder"

            fun <T : SynapService.Item> getFilterTransform(): (List<T>) -> List<T> = { rawList ->
                val genuine = if (filterAiSongs) rawList.filter { isGenuineSong(it) } else rawList
                val langFiltered = filterBySelectedLanguage(genuine, language)

                when (searchSortBy) {
                    SongSortBy.Title -> {
                        fun title(item: SynapService.Item): String = when (item) {
                            is SynapService.SongItem -> item.info?.name
                            is SynapService.VideoItem -> item.info?.name
                            is SynapService.AlbumItem -> item.info?.name
                            is SynapService.ArtistItem -> item.info?.name
                            is SynapService.PlaylistItem -> item.info?.name
                            else -> null
                        } ?: ""

                        if (searchSortOrder == SortOrder.Ascending) langFiltered.sortedBy { title(it) } else langFiltered.sortedByDescending { title(it) }
                    }
                    else -> if (searchSortOrder == SortOrder.Ascending) langFiltered.reversed() else langFiltered
                }
            }

            /**
             * For each tab, we use a generic ItemsPage that handles 
             * infinite scrolling and loading shimmers.
             */
            when (currentTabIndex) {
                0 -> {
                    val thumbnailSizeDp = if (searchViewMode == DiscoveryViewMode.Grid) 180.dp else Dimensions.thumbnails.song
                    val thumbnailSizePx = thumbnailSizeDp.px

                    ItemsPage(
                        tag = "searchResults/$filterTag/songs_v3",
                        filterTransform = getFilterTransform(),
                        itemsPageProvider = { continuation ->
                            // If it's a new search, we use the Search API. 
                            // If we're scrolling, we use the continuation token.
                            if (continuation == null) {
                                SynapService.searchPage(
                                    body = SearchBody(query = effectiveQuery, params = SynapService.SearchFilter.Song.value),
                                    fromMusicShelfRendererContent = SynapService.SongItem.Companion::from
                                )
                            } else {
                                SynapService.searchPage(
                                    body = ContinuationBody(continuation = continuation),
                                    fromMusicShelfRendererContent = SynapService.SongItem.Companion::from
                                )
                            }
                        },
                        emptyItemsText = emptyItemsText,
                        headerContent = { },
                        itemContent = { song, index, list ->
                            val isSelected = selection.contains(song.key)
                            SongItem(
                                song = song,
                                thumbnailSizePx = thumbnailSizePx,
                                thumbnailSizeDp = thumbnailSizeDp,
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {
                                            // Handle multi-select toggle or single item menu.
                                            if (selection.isNotEmpty()) {
                                                if (isSelected) selection.remove(song.key) else selection.add(song.key)
                                            } else {
                                                menuState.display {
                                                    NonQueuedMediaItemMenu(
                                                        onDismiss = menuState::hide,
                                                        mediaItem = song.asMediaItem,
                                                        onMultipleDelete = { selection.add(song.key) }
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            if (selection.isNotEmpty()) {
                                                if (isSelected) selection.remove(song.key) else selection.add(song.key)
                                            } else {
                                                // Standardized Queueing.
                                                val mediaItems = list.filterIsInstance<SynapService.SongItem>().map { it.asMediaItem }
                                                binder?.stopRadio()
                                                binder?.player?.forcePlayAtIndex(mediaItems, index, context)

                                                binder?.setupRadio(song.info?.endpoint)
                                            }
                                        }
                                    )
                                    .then(if (isSelected) Modifier.background(colorPalette.accent.copy(alpha = 0.3f)) else Modifier)
                            )
                        },
                        itemPlaceholderContent = {
                            SongItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                        }
                    )
                }

                1 -> {
                    val thumbnailSizeDp = if (searchViewMode == DiscoveryViewMode.Grid) 180.dp else 108.dp
                    val thumbnailSizePx = thumbnailSizeDp.px

                    ItemsPage(
                        tag = "searchResults/$filterTag/albums_v7", // Force cache invalidation to apply the ultra-prioritized album search
                        filterTransform = getFilterTransform(),
                        itemsPageProvider = { continuation ->
                            if (continuation == null) {
                                coroutineScope {
                                    /**
                                     * 3-TIER ULTRA-PRIORITIZED ALBUM RESOLUTION ENGINE
                                     * We execute three YouTube Music queries simultaneously to assemble the perfect album results.
                                     */

                                    // Tier 1: The Top Song (Used to pinpoint the exact requested album)
                                    val topSongDeferred = async {
                                        val songSearch = SynapService.searchPage(
                                            body = SearchBody(query = effectiveQuery, params = SynapService.SearchFilter.Song.value),
                                            fromMusicShelfRendererContent = SynapService.SongItem.Companion::from
                                        )
                                        songSearch?.getOrNull()?.list?.firstOrNull()
                                    }

                                    // Tier 2: The Top Artist's Official Studio Albums
                                    val artistAlbumsDeferred = async {
                                        val artistSearch = SynapService.searchPage(
                                            body = SearchBody(query = effectiveQuery, params = SynapService.SearchFilter.Artist.value),
                                            fromMusicShelfRendererContent = SynapService.ArtistItem.Companion::from,
                                            fromMusicTwoRowItemRenderer = SynapService.ArtistItem.Companion::from
                                        )
                                        val topArtist = artistSearch?.getOrNull()?.list?.firstOrNull()
                                        
                                        if (topArtist != null) {
                                            // Route through the artist page to guarantee high-quality official studio albums
                                            val artistPage = SynapService.artistPage(BrowseBody(browseId = topArtist.key))
                                            artistPage?.getOrNull()?.albums ?: emptyList()
                                        } else emptyList()
                                    }

                                    // Tier 3: The Standard YouTube Album Search (Fallback for compilations, tributes, etc.)
                                    val standardSearchDeferred = async {
                                        SynapService.searchPage(
                                            body = SearchBody(query = effectiveQuery, params = SynapService.SearchFilter.Album.value),
                                            fromMusicShelfRendererContent = SynapService.AlbumItem.Companion::from,
                                            fromMusicTwoRowItemRenderer = SynapService.AlbumItem.Companion::from
                                        )
                                    }

                                    // Await all parallel network calls
                                    val topSong = topSongDeferred.await()
                                    val artistAlbums = artistAlbumsDeferred.await()
                                    val standardSearch = standardSearchDeferred.await()
                                    
                                    val standardAlbums = standardSearch?.getOrNull()?.list ?: emptyList()
                                    val standardContinuation = standardSearch?.getOrNull()?.continuation

                                    // Identify the absolute best Priority #1 Album for the requested song
                                    var topPriorityAlbum: SynapService.AlbumItem? = null
                                    
                                    if (topSong != null) {
                                        val artistName = topSong.authors?.firstOrNull()?.name ?: ""
                                        val trackName = topSong.info?.name ?: ""
                                        
                                        var officialAlbumName: String? = null
                                        if (artistName.isNotBlank() && trackName.isNotBlank()) {
                                            // Step A: Ask TheAudioDB for the exact official Studio Album name (ignores 1-track Singles)
                                            TheAudioDB.getTrackInfo(artistName, trackName).getOrNull()?.let { track ->
                                                officialAlbumName = track.strAlbum
                                            }
                                        }
                                        
                                        // A. Find the official album in the artist's high-quality albums list
                                        if (officialAlbumName != null) {
                                            // Match TheAudioDB's official name against the Artist's official discography
                                            topPriorityAlbum = artistAlbums.firstOrNull { it.info?.name.equals(officialAlbumName, ignoreCase = true) }
                                        }
                                        
                                        // B. Fallback to searching YouTube directly for the official album name
                                        if (topPriorityAlbum == null && officialAlbumName != null) {
                                            // If not in discography, execute a surgical direct search for the exact official album
                                            val directAlbumSearch = SynapService.searchPage(
                                                body = SearchBody(query = "$artistName $officialAlbumName", params = SynapService.SearchFilter.Album.value),
                                                fromMusicShelfRendererContent = SynapService.AlbumItem.Companion::from,
                                                fromMusicTwoRowItemRenderer = SynapService.AlbumItem.Companion::from
                                            )
                                            topPriorityAlbum = directAlbumSearch?.getOrNull()?.list?.firstOrNull()
                                        }
                                        
                                        // C. Fallback to the single (with otherVersions auto-upgrade)
                                        if (topPriorityAlbum == null) {
                                            // Step B: Fallback to YouTube's native 'otherVersions' single -> studio album auto-upgrade
                                            val songAlbum = topSong.album
                                            var officialAlbumId = songAlbum?.endpoint?.browseId
                                            var albumName = songAlbum?.name
                                            var albumThumbnail = topSong.thumbnail

                                            if (officialAlbumId != null && albumName != null) {
                                                // Upgrade Single to Full Studio Album using otherVersions
                                                val albumPageResult = SynapService.albumPage(BrowseBody(browseId = officialAlbumId))
                                                val albumPage = albumPageResult?.getOrNull()
                                                
                                                if (albumPage != null) {
                                                    val trackCount = albumPage.songsPage?.list?.size ?: 0
                                                    val otherVersions = albumPage.otherVersions
                                                    // If it's a Single (<= 2 tracks), scan 'otherVersions' for the full studio album
                                                    if (trackCount <= 2 && !otherVersions.isNullOrEmpty()) {
                                                        val studioVersion = otherVersions.firstOrNull { it.key != officialAlbumId && !it.info?.name.isNullOrBlank() }
                                                        if (studioVersion != null) {
                                                            val studioResult = SynapService.albumPage(BrowseBody(browseId = studioVersion.key))
                                                            studioResult?.getOrNull()?.let { studioPage ->
                                                                if ((studioPage.songsPage?.list?.size ?: 0) > trackCount) {
                                                                    // Upgrade successful: swap IDs and metadata
                                                                    officialAlbumId = studioVersion.key
                                                                    albumName = studioPage.title ?: studioVersion.info?.name
                                                                    albumThumbnail = studioPage.thumbnail ?: studioVersion.thumbnail
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                topPriorityAlbum = SynapService.AlbumItem(
                                                    info = SynapService.Info(
                                                        name = albumName,
                                                        endpoint = NavigationEndpoint.Endpoint.Browse(browseId = officialAlbumId!!)
                                                    ),
                                                    authors = topSong.authors,
                                                    year = albumPage?.year,
                                                    thumbnail = albumThumbnail
                                                )
                                            }
                                        }
                                    }

                                    // Assemble the ultimate ordered list and remove duplicates
                                    val combinedList = (listOfNotNull(topPriorityAlbum) + artistAlbums + standardAlbums).distinctBy { it.key }
                                    
                                    if (combinedList.isNotEmpty()) {
                                        Result.success(SynapService.ItemsPage(list = combinedList, continuation = standardContinuation))
                                    } else {
                                        Result.success(SynapService.ItemsPage(list = emptyList(), continuation = null))
                                    }
                                }
                            } else {
                                SynapService.searchPage(
                                    body = ContinuationBody(continuation = continuation),
                                    fromMusicShelfRendererContent = SynapService.AlbumItem.Companion::from
                                )
                            }
                        },
                        emptyItemsText = emptyItemsText,
                        headerContent = { },
                        itemContent = { album: SynapService.AlbumItem, _, _ ->
                            val isSelected = selection.contains(album.key)
                            AlbumItem(
                                album = album,
                                thumbnailSizePx = thumbnailSizePx,
                                thumbnailSizeDp = thumbnailSizeDp,
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {
                                            if (selection.isNotEmpty()) {
                                                if (isSelected) selection.remove(album.key) else selection.add(album.key)
                                            } else {
                                                menuState.display {
                                                    AlbumMenu(
                                                        album = album,
                                                        onDismiss = menuState::hide
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            if (selection.isNotEmpty()) {
                                                if (isSelected) selection.remove(album.key) else selection.add(album.key)
                                            } else {
                                                val albumId = album.key
                                                albumDetailsRoute.global(albumId, Album(
                                                    id = albumId,
                                                    title = album.info?.name,
                                                    thumbnailUrl = album.thumbnail?.url,
                                                    authorsText = album.authors?.joinToString(", ") { it.name ?: "" }
                                                ))
                                            }
                                        }
                                    )
                                    .then(if (isSelected) Modifier.background(colorPalette.accent.copy(alpha = 0.3f)) else Modifier)
                            )

                        },
                        itemPlaceholderContent = {
                            AlbumItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                        }
                    )
                }

                2 -> {
                    val thumbnailSizeDp = if (searchViewMode == DiscoveryViewMode.Grid) 180.dp else 92.dp
                    val thumbnailSizePx = thumbnailSizeDp.px

                    ItemsPage(
                        tag = "searchResults/$filterTag/artists_v3",
                        filterTransform = getFilterTransform(),
                        itemsPageProvider = { continuation ->
                            if (continuation == null) {
                                SynapService.searchPage(
                                    body = SearchBody(query = effectiveQuery, params = SynapService.SearchFilter.Artist.value),
                                    fromMusicShelfRendererContent = SynapService.ArtistItem.Companion::from,
                                    fromMusicTwoRowItemRenderer = SynapService.ArtistItem.Companion::from
                                )
                            } else {
                                SynapService.searchPage(
                                    body = ContinuationBody(continuation = continuation),
                                    fromMusicShelfRendererContent = SynapService.ArtistItem.Companion::from
                                )
                            }
                        },
                        emptyItemsText = emptyItemsText,
                        headerContent = { },
                        itemContent = { artist, _, _ ->
                            val isSelected = selection.contains(artist.key)
                            ArtistItem(
                                artist = artist,
                                thumbnailSizePx = thumbnailSizePx,
                                thumbnailSizeDp = thumbnailSizeDp,
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {
                                            if (selection.isNotEmpty()) {
                                                if (isSelected) selection.remove(artist.key) else selection.add(artist.key)
                                            } else {
                                                menuState.display {
                                                    ArtistMenu(
                                                        artist = artist,
                                                        onDismiss = menuState::hide
                                                    )
                                                }
                                            }
                                        },
                                        onClick = { 
                                            if (selection.isNotEmpty()) {
                                                if (isSelected) selection.remove(artist.key) else selection.add(artist.key)
                                            } else {
                                                artistRoute.global(artist.key, Artist(
                                                    id = artist.key,
                                                    name = artist.info?.name,
                                                    thumbnailUrl = artist.thumbnail?.url
                                                ))
                                            }
                                        }
                                    )
                                    .then(if (isSelected) Modifier.background(colorPalette.accent.copy(alpha = 0.3f)) else Modifier)
                            )

                        },
                        itemPlaceholderContent = {
                            ArtistItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                        }
                    )
                }

                3 -> {
                    val thumbnailSizeDp = if (searchViewMode == DiscoveryViewMode.Grid) 180.dp else 108.dp
                    val thumbnailSizePx = thumbnailSizeDp.px

                    ItemsPage(
                        tag = "searchResults/$filterTag/videos_v3",
                        filterTransform = getFilterTransform(),
                        itemsPageProvider = { continuation ->
                            if (continuation == null) {
                                SynapService.searchPage(
                                    body = SearchBody(query = effectiveQuery, params = SynapService.SearchFilter.Video.value),
                                    fromMusicShelfRendererContent = SynapService.VideoItem.Companion::from,
                                    fromMusicTwoRowItemRenderer = SynapService.VideoItem.Companion::from
                                )
                            } else {
                                SynapService.searchPage(
                                    body = ContinuationBody(continuation = continuation),
                                    fromMusicShelfRendererContent = SynapService.VideoItem.Companion::from
                                )
                            }
                        },
                        emptyItemsText = emptyItemsText,
                        headerContent = { },
                        itemContent = { video, index, list ->
                            val isSelected = selection.contains(video.key)
                            SongItem(
                                song = video.asMediaItem,
                                thumbnailSizePx = thumbnailSizePx,
                                thumbnailSizeDp = thumbnailSizeDp,
                                isVertical = searchViewMode == DiscoveryViewMode.Grid,
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {
                                            if (selection.isNotEmpty()) {
                                                if (isSelected) selection.remove(video.key) else selection.add(video.key)
                                            } else {
                                                menuState.display {
                                                    NonQueuedMediaItemMenu(
                                                        onDismiss = menuState::hide,
                                                        mediaItem = video.asMediaItem,
                                                        onMultipleDelete = { selection.add(video.key) }
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            if (selection.isNotEmpty()) {
                                                if (isSelected) selection.remove(video.key) else selection.add(video.key)
                                            } else {
                                                // Standardized Queueing for Videos.
                                                val mediaItems = list.filterIsInstance<SynapService.VideoItem>().map { it.asMediaItem }
                                                binder?.stopRadio()
                                                binder?.player?.forcePlayAtIndex(mediaItems, index, context)
                                                
                                                binder?.setupRadio(video.info?.endpoint)
                                            }
                                        }
                                    )
                                    .then(if (isSelected) Modifier.background(colorPalette.accent.copy(alpha = 0.3f)) else Modifier)
                            )
                        },
                        itemPlaceholderContent = {
                            SongItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                        }
                    )
                }

                4 -> {
                    val thumbnailSizeDp = if (searchViewMode == DiscoveryViewMode.Grid) 180.dp else 108.dp
                    val thumbnailSizePx = thumbnailSizeDp.px

                    ItemsPage(
                        tag = "searchResults/$filterTag/playlists_v3",
                        filterTransform = getFilterTransform(),
                        itemsPageProvider = { continuation ->
                            if (continuation == null) {
                                SynapService.searchPage(
                                    body = SearchBody(query = effectiveQuery, params = SynapService.SearchFilter.CommunityPlaylist.value),
                                    fromMusicShelfRendererContent = SynapService.PlaylistItem.Companion::from,
                                    fromMusicTwoRowItemRenderer = SynapService.PlaylistItem.Companion::from
                                )
                            } else {
                                SynapService.searchPage(
                                    body = ContinuationBody(continuation = continuation),
                                    fromMusicShelfRendererContent = SynapService.PlaylistItem.Companion::from
                                )
                            }
                        },
                        emptyItemsText = emptyItemsText,
                        headerContent = { },
                        itemContent = { playlist, _, _ ->
                            val idString = playlist.key
                            val isSelected = selection.contains(idString)
                            PlaylistItem(
                                playlist = playlist,
                                thumbnailSizePx = thumbnailSizePx,
                                thumbnailSizeDp = thumbnailSizeDp,
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {
                                            if (selection.isNotEmpty()) {
                                                if (isSelected) selection.remove(idString) else selection.add(idString)
                                            } else {
                                                menuState.display {
                                                    PlaylistMenu(
                                                        playlist = playlist,
                                                        onDismiss = menuState::hide
                                                    )
                                                }
                                            }
                                        },
                                        onClick = { 
                                            if (selection.isNotEmpty()) {
                                                if (isSelected) selection.remove(idString) else selection.add(idString)
                                            } else {
                                                playlistRoute.global(playlist.key) 
                                            }
                                        }
                                    )
                                    .then(if (isSelected) Modifier.background(colorPalette.accent.copy(alpha = 0.3f)) else Modifier)
                            )
                        },
                        itemPlaceholderContent = {
                            PlaylistItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultLanguageSection(onSelect: () -> Unit) {
    var language by rememberPreference(discoveryLanguageKey, "None")
    var subSearch by remember { mutableStateOf("") }

    SearchEntry(query = subSearch, onValueChange = { subSearch = it }, hint = "Search...", onClear = { subSearch = "" })

    DiscoveryFilter.Language.entries.filter { it.label.contains(subSearch, true) }.forEach {
        val name = it::class.simpleName!!
        MenuEntry(
            icon = R.drawable.text,
            text = it.label,
            onClick = { language = name; onSelect() },
            trailingContent = if (name == language) ({ CheckmarkIcon() }) else null
        )
    }
}

@Composable
private fun CheckmarkIcon() {
    val (colorPalette) = LocalAppearance.current
    Image(
        painter = painterResource(R.drawable.checkmark),
        contentDescription = null,
        colorFilter = ColorFilter.tint(colorPalette.accent),
        modifier = Modifier.size(18.dp)
    )
}

@Composable
private fun SearchEntry(
    query: String,
    onValueChange: (String) -> Unit,
    hint: String,
    onClear: () -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .background(colorPalette.background2, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.search),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorPalette.textSecondary),
            modifier = Modifier.size(14.dp)
        )

        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        ) {
            if (query.isEmpty()) {
                BasicText(
                    text = hint,
                    style = typography.xs.medium.secondary
                )
            }

            BasicTextField(
                value = query,
                onValueChange = onValueChange,
                textStyle = typography.xs.medium.color(colorPalette.text),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (query.isNotEmpty()) {
            HeaderIconButton(
                icon = R.drawable.close,
                color = colorPalette.textSecondary,
                onClick = onClear
            )
        }
    }
}
