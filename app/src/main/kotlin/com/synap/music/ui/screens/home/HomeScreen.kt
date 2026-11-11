package com.synap.music.ui.screens.home

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.edit
import com.synap.compose.routing.RouteHandler
import com.synap.music.Database
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.R
import com.synap.music.cubes.AudioCubeManager
import com.synap.music.enums.AlbumSortBy
import com.synap.music.enums.ArtistSortBy
import com.synap.music.enums.CubeCompressionLevel
import com.synap.music.enums.DiscoveryFilter
import com.synap.music.enums.DiscoveryViewMode
import com.synap.music.enums.PlaylistSortBy
import com.synap.music.enums.SongSortBy
import com.synap.music.enums.SortOrder
import com.synap.music.models.Playlist
import com.synap.music.query
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.ui.components.themed.ConfirmationDialog
import com.synap.music.ui.components.themed.FilterButton
import com.synap.music.ui.components.themed.HeaderIconButton
import com.synap.music.ui.components.themed.InListSearchHeader
import com.synap.music.ui.components.themed.ListenTogetherDialog
import com.synap.music.ui.components.themed.Menu
import com.synap.music.ui.components.themed.MenuEntry
import com.synap.music.ui.components.themed.MusicRecognitionDialog
import com.synap.music.ui.components.themed.PlaylistSelectionDialog
import com.synap.music.ui.components.themed.Scaffold
import com.synap.music.ui.components.themed.SecondaryTextButton
import com.synap.music.ui.components.themed.SortOrderMenuEntry
import com.synap.music.ui.components.themed.SpotifyImportDialog
import com.synap.music.ui.components.themed.SynapCubeExportMenu
import com.synap.music.ui.components.themed.SynapCubeTitleButton
import com.synap.music.ui.components.themed.TextFieldDialog
import com.synap.music.ui.screens.albumDetailsRoute
import com.synap.music.ui.screens.artistRoute
import com.synap.music.ui.screens.builtInPlaylistRoute
import com.synap.music.ui.screens.globalRoutes
import com.synap.music.ui.screens.localPlaylistRoute
import com.synap.music.ui.screens.playlistRoute
import com.synap.music.ui.screens.searchResultRoute
import com.synap.music.ui.screens.searchRoute
import com.synap.music.ui.screens.settingsRoute
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.utils.MediaFetchUtils
import com.synap.music.utils.albumSortByKey
import com.synap.music.utils.albumSortOrderKey
import com.synap.music.utils.artistSortByKey
import com.synap.music.utils.artistSortOrderKey
import com.synap.music.utils.asMediaItem
import com.synap.music.utils.bold
import com.synap.music.utils.color
import com.synap.music.utils.cubeCompressionLevelKey
import com.synap.music.utils.discoveryDecadeKey
import com.synap.music.utils.discoveryGenreKey
import com.synap.music.utils.discoveryLanguageKey
import com.synap.music.utils.discoveryLimitKey
import com.synap.music.utils.discoveryStyleKey
import com.synap.music.utils.discoveryViewModeKey
import com.synap.music.utils.filterAiSongsKey
import com.synap.music.utils.isListenTogetherEnabledKey
import com.synap.music.utils.isMusicRecognitionEnabledKey
import com.synap.music.utils.isProSearchModeKey
import com.synap.music.utils.medium
import com.synap.music.utils.playlistSortByKey
import com.synap.music.utils.playlistSortOrderKey
import com.synap.music.utils.rememberPreference
import com.synap.music.utils.searchResultScreenTabIndexKey
import com.synap.music.utils.secondary
import com.synap.music.utils.songSortByKey
import com.synap.music.utils.songSortOrderKey
import com.synap.music.utils.toast
import com.synap.pulse.SynapService
import com.synap.pulse.models.NavigationEndpoint
import com.synap.pulse.models.bodies.SearchSuggestionsBody
import com.synap.pulse.requests.searchSuggestions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * Defines the active floating search filter mode for the current tab.
 * SearchMode.DEN represents the DEN Lair tab (playlists, local storage, smart mixes, recap, radio).
 */
enum class SearchMode { None, Songs, DEN, Albums, Artists, Discover }

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalAnimationApi
@Composable
fun HomeScreen() {
    // This helps the app remember where you were (scroll position, etc.) 
    // even if you switch tabs and come back.
    val saveableStateHolder = rememberSaveableStateHolder()

    var tabIndex by rememberSaveable {
        mutableStateOf(0)
    }

    // A way to manually refresh the Discover feed from the top bar.
    var refreshDiscoverLambda by remember {
        mutableStateOf<(() -> Unit)?>(null)
    }

    var isCreatingANewPlaylist by rememberSaveable {
        mutableStateOf(false)
    }

    var isImportingSpotifyPlaylist by rememberSaveable {
        mutableStateOf(false)
    }

    var isRecognizingMusic by rememberSaveable {
        mutableStateOf(false)
    }

    var isShowingListenTogetherDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val isMusicRecognitionEnabled by rememberPreference(isMusicRecognitionEnabledKey, true)
    val isListenTogetherEnabled by rememberPreference(isListenTogetherEnabledKey, false)

    // Search logic: handles the query and whether to show the "type-ahead" suggestions.
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchMode by rememberSaveable { mutableStateOf(SearchMode.None) }

    var searchSuggestionsList by remember { mutableStateOf(emptyList<SynapService.Suggestion>()) }
    var shouldShowSuggestions by remember { mutableStateOf(false) }

    // Multi-Select logic: keeps track of which IDs are currently highlighted.
    val selection = remember { mutableStateListOf<String>() }
    var currentListToSelect by remember { mutableStateOf(emptyList<String>()) }
    var isShowingDeleteDialog by remember { mutableStateOf(false) }
    var isShowingPlaylistSelectionDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isProSearchMode by rememberPreference(isProSearchModeKey, false)

    var selectedPlaylistIdsToExport by remember { mutableStateOf<List<Long>>(emptyList()) }
    var selectedSongIdsToExport by remember { mutableStateOf<List<String>>(emptyList()) }
    var cubeCompressionLevel by rememberPreference(cubeCompressionLevelKey, CubeCompressionLevel.Auto)

    val exportPlaylistsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let {
            val ids = selectedPlaylistIdsToExport.toList()
            if (ids.isNotEmpty()) {
                coroutineScope.launch {
                    val result = AudioCubeManager.exportPlaylistsToCube(context, ids, uri, cubeCompressionLevel)
                    if (result.isSuccess) {
                        context.toast("Exported ${ids.size} playlist(s) to Audio Cube!")
                        selection.clear()
                    } else {
                        context.toast("Failed to export Audio Cube")
                    }
                }
            }
        }
        selectedPlaylistIdsToExport = emptyList()
    }

    val exportSongsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let {
            val ids = selectedSongIdsToExport.toList()
            if (ids.isNotEmpty()) {
                coroutineScope.launch {
                    val result = AudioCubeManager.exportSongsToCube(context, ids, uri, cubeCompressionLevel)
                    if (result.isSuccess) {
                        context.toast("Exported ${ids.size} track(s) to Audio Cube!")
                        selection.clear()
                    } else {
                        context.toast("Failed to export Audio Cube")
                    }
                }
            }
        }
        selectedSongIdsToExport = emptyList()
    }

    // Bulk "Add to Playlist" tool. Triggered from the selection top bar.
    if (isShowingPlaylistSelectionDialog) {
        PlaylistSelectionDialog(
            onDismiss = { isShowingPlaylistSelectionDialog = false },
            onPlaylistSelected = { playlist, count ->
                isShowingPlaylistSelectionDialog = false
                val selectedIds = selection.toList()
                selection.clear()
                coroutineScope.launch {
                    query {
                        when (tabIndex) {
                            1 -> Database.addSongsToPlaylist(selectedIds, playlist.id, count)
                            3 -> Database.addAlbumsToPlaylist(selectedIds, playlist.id, count)
                            4 -> Database.addArtistsToPlaylist(selectedIds, playlist.id, count)
                        }
                    }
                    context.toast("Added items to ${playlist.name}")
                }
            }
        )
    }

    // Safety: clear the selection if the user switches tabs.
    LaunchedEffect(tabIndex) {
        selection.clear()
        currentListToSelect = emptyList()
    }

    // Bulk Delete logic: cleans up both the database entries AND the physical files in the Vault.
    if (isShowingDeleteDialog) {
        ConfirmationDialog(
            text = "Are you sure you want to delete ${selection.size} items from your library and storage?",
            onDismiss = { isShowingDeleteDialog = false },
            onConfirm = {
                val selectedIds = selection.toList()
                selection.clear()
                coroutineScope.launch {
                    selectedIds.forEach { id ->
                        val songs = Database.albumSongs(id).firstOrNull() ?: emptyList()
                        query {
                            songs.forEach { song ->
                                val file = MediaFetchUtils.getAudioFile(context, song.id)
                                if (file.exists()) file.delete()
                                Database.deleteFormat(song.id)
                            }
                            Database.getAlbum(id)?.let { Database.delete(it) }
                            Database.getArtist(id)?.let { Database.delete(it) }
                            Database.delete(Playlist(id = id.toLongOrNull() ?: -1, name = ""))
                        }
                    }
                    context.toast("Deleted ${selectedIds.size} items")
                }
            }
        )
    }

    // Fetch search suggestions as you type (with a small 300ms delay to avoid spamming the server).
    LaunchedEffect(searchQuery) {
        if (shouldShowSuggestions && searchQuery.length > 1) {
            delay(300.milliseconds)
            SynapService.searchSuggestions(SearchSuggestionsBody(input = searchQuery))
                ?.onSuccess { searchSuggestionsList = it ?: emptyList() }
        } else {
            searchSuggestionsList = emptyList()
        }
    }

    if (isCreatingANewPlaylist) {
        TextFieldDialog(
            hintText = "Enter the playlist name",
            onDismiss = {
                isCreatingANewPlaylist = false
            },
            onDone = { text ->
                query {
                    Database.insert(Playlist(name = text))
                }
            }
        )
    }

    if (isImportingSpotifyPlaylist) {
        SpotifyImportDialog(
            onDismiss = {
                isImportingSpotifyPlaylist = false
            }
        )
    }

    if (isRecognizingMusic) {
        MusicRecognitionDialog(
            onDismiss = {
                isRecognizingMusic = false
            }
        )
    }

    if (isShowingListenTogetherDialog) {
        ListenTogetherDialog(
            onDismiss = {
                isShowingListenTogetherDialog = false
            }
        )
    }

    RouteHandler(listenToGlobalEmitter = true) {
        globalRoutes()

        host {
            val (colorPalette, typography) = LocalAppearance.current
            val menuState = LocalMenuState.current

            Scaffold(
                title = {
                    if (selection.isNotEmpty()) {
                        SynapCubeTitleButton(
                            selectedCount = selection.size,
                            onClick = {
                                menuState.display {
                                    SynapCubeExportMenu(
                                        selectedCount = selection.size,
                                        compressionLevel = cubeCompressionLevel,
                                        onCompressionLevelChanged = { cubeCompressionLevel = it },
                                        onExport = {
                                            when (tabIndex) {
                                                1 -> {
                                                    selectedSongIdsToExport = selection.toList()
                                                    exportSongsLauncher.launch("Tracks_Export.synapcube")
                                                }
                                                2 -> {
                                                    val playlistIds = selection.mapNotNull { it.toLongOrNull() }
                                                    if (playlistIds.isNotEmpty()) {
                                                        selectedPlaylistIdsToExport = playlistIds
                                                        exportPlaylistsLauncher.launch("Playlists_Export.synapcube")
                                                    }
                                                }
                                                3 -> {
                                                    val albumIds = selection.toList()
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        val songs = albumIds.flatMap { id -> Database.albumSongs(id).firstOrNull() ?: emptyList() }.distinctBy { it.id }
                                                        if (songs.isNotEmpty()) {
                                                            selectedSongIdsToExport = songs.map { it.id }
                                                            withContext(Dispatchers.Main) {
                                                                exportSongsLauncher.launch("Albums_Export.synapcube")
                                                            }
                                                        }
                                                    }
                                                }
                                                4 -> {
                                                    val artistIds = selection.toList()
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        val songs = artistIds.flatMap { id -> Database.artistSongs(id).firstOrNull() ?: emptyList() }.distinctBy { it.id }
                                                        if (songs.isNotEmpty()) {
                                                            selectedSongIdsToExport = songs.map { it.id }
                                                            withContext(Dispatchers.Main) {
                                                                exportSongsLauncher.launch("Artists_Export.synapcube")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        onDismiss = menuState::hide
                                    )
                                }
                            }
                        )
                    } else if (searchMode != SearchMode.None) {
                        InListSearchHeader(
                            query = searchQuery,
                            onQueryChange = { 
                                searchQuery = it
                                shouldShowSuggestions = true
                            },
                            onClear = {
                                searchQuery = ""
                                searchMode = SearchMode.None
                                shouldShowSuggestions = false
                            },
                            onSearch = {
                                if (searchQuery.isNotEmpty()) {
                                    if (isProSearchMode) {
                                        context.getSharedPreferences("preferences", Context.MODE_PRIVATE).edit {
                                            putInt(searchResultScreenTabIndexKey, 3)
                                        }
                                    }
                                    searchResultRoute.global(searchQuery)
                                    searchMode = SearchMode.None
                                    shouldShowSuggestions = false
                                }
                            },
                            hint = when (searchMode) {
                                SearchMode.Songs -> "Search in Songs"
                                SearchMode.DEN -> "Search in DEN"
                                SearchMode.Albums -> "Search in Albums"
                                SearchMode.Artists -> "Search in Artists"
                                SearchMode.Discover -> "Search in Discover"
                                else -> "Search"
                            }
                        )
                    } else {
                        val titleText = when (tabIndex) {
                            0 -> "Discover"
                            1 -> "Songs"
                            2 -> "DEN"
                            3 -> "Albums"
                            4 -> "Artists"
                            else -> ""
                        }

                        BasicText(
                            text = titleText,
                            style = typography.l.bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                topStartIconButtonId = when {
                    selection.isNotEmpty() -> R.drawable.close
                    searchMode == SearchMode.None -> R.drawable.ellipsis_vertical
                    else -> null
                },
                onTopStartIconButtonClick = when {
                    selection.isNotEmpty() -> ({ selection.clear() })
                    searchMode == SearchMode.None -> ({ settingsRoute.global() })
                    else -> null
                },
                topEndIconButtonId = when {
                    selection.isNotEmpty() -> R.drawable.trash
                    searchMode != SearchMode.None -> R.drawable.close
                    tabIndex == 0 -> R.drawable.sync
                    else -> R.drawable.search
                },
                onTopEndIconButtonClick = when {
                    selection.isNotEmpty() -> ({ isShowingDeleteDialog = true })
                    searchMode != SearchMode.None -> ({
                        searchQuery = ""
                        searchMode = SearchMode.None
                        shouldShowSuggestions = false
                    })
                    tabIndex == 0 -> ({ refreshDiscoverLambda?.invoke() })
                    else -> ({
                        searchMode = when (tabIndex) {
                            1 -> SearchMode.Songs
                            2 -> SearchMode.DEN
                            3 -> SearchMode.Albums
                            4 -> SearchMode.Artists
                            else -> SearchMode.None
                        }
                    })
                },
                topBarActions = {
                    if (selection.isNotEmpty()) {
                        if (tabIndex == 1 || tabIndex == 3 || tabIndex == 4) {
                            HeaderIconButton(
                                icon = R.drawable.playlist,
                                color = colorPalette.text,
                                onClick = { isShowingPlaylistSelectionDialog = true }
                            )
                        }

                        if (tabIndex == 1) {
                            HeaderIconButton(
                                icon = R.drawable.airplane,
                                color = colorPalette.text,
                                onClick = {
                                    val ids = selection.toList()
                                    context.toast("Starting download for ${ids.size} song(s)...")
                                    coroutineScope.launch(Dispatchers.IO) {
                                        ids.forEach { songId ->
                                            val song = Database.getSong(songId)
                                            if (song != null) {
                                                MediaFetchUtils.downloadMedia(context, song.asMediaItem, false)
                                            }
                                        }
                                    }
                                    selection.clear()
                                }
                            )
                        }

                        val isAllSelected = selection.size == currentListToSelect.size && currentListToSelect.isNotEmpty()
                        SecondaryTextButton(
                            text = if (isAllSelected) "Deselect all" else "Select all",
                            onClick = {
                                if (isAllSelected) {
                                    selection.clear()
                                } else {
                                    selection.clear()
                                    selection.addAll(currentListToSelect)
                                }
                            },
                            alternative = false
                        )
                    } else if (searchMode == SearchMode.None) {
                        if (isListenTogetherEnabled) {
                            HeaderIconButton(
                                icon = R.drawable.person,
                                color = colorPalette.text,
                                onClick = { isShowingListenTogetherDialog = true }
                            )
                        }
                        if (isMusicRecognitionEnabled) {
                            HeaderIconButton(
                                icon = R.drawable.soundwave,
                                color = colorPalette.text,
                                onClick = { isRecognizingMusic = true }
                            )
                        }

                        if (tabIndex != 2) {
                            UniversalFilterButton(
                                tabIndex = tabIndex,
                                onRefreshDiscover = { refreshDiscoverLambda?.invoke() },
                                onStartSearch = { searchMode = it }
                            )
                        }
                    }
                },
                topBarModifier = if (selection.isNotEmpty()) Modifier.background(colorPalette.background1) else Modifier,
                tabIndex = tabIndex,
                onTabChanged = {
                    tabIndex = it
                    searchQuery = ""
                    searchMode = SearchMode.None
                    shouldShowSuggestions = false
                },
                tabColumnContent = { item ->
                    item(0, "Discover", R.drawable.radio)
                    item(1, "Songs", R.drawable.musical_notes)
                    item(2, "DEN", R.drawable.den)
                    item(3, "Albums", R.drawable.disc)
                    item(4, "Artists", R.drawable.person)
                }
            ) { currentTabIndex ->
                Box(Modifier.fillMaxSize()) {
                    saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                        when (currentTabIndex) {
                            0 -> Discover(
                                onArtistClick = { artistRoute.global(it.id, it) },
                                onSearchClick = { searchRoute.global("") },
                                onRefresh = { refreshDiscoverLambda = it }
                            )

                            1 -> HomeSongs(
                                onSearchClick = { searchRoute.global("") },
                                searchQuery = if (searchMode == SearchMode.Songs) searchQuery else "",
                                selection = selection,
                                onIdsChanged = { currentListToSelect = it }
                            )

                            2 -> HomePlaylists(
                                onBuiltInPlaylist = { builtInPlaylistRoute.global(it) },
                                onPlaylistClick = { localPlaylistRoute.global(it.id) },
                                onSearchClick = { searchRoute.global("") },
                                searchQuery = if (searchMode == SearchMode.DEN) searchQuery else "",
                                selection = selection,
                                onIdsChanged = { currentListToSelect = it }
                            )

                            3 -> HomeAlbums(
                                onAlbumClick = { albumDetailsRoute.global(it.id, it) },
                                onSearchClick = { searchRoute.global("") },
                                searchQuery = if (searchMode == SearchMode.Albums) searchQuery else "",
                                selection = selection,
                                onIdsChanged = { currentListToSelect = it }
                            )

                            4 -> HomeArtistList(
                                onArtistClick = { artistRoute.global(it.id, it) },
                                onSearchClick = { searchRoute.global("") },
                                searchQuery = if (searchMode == SearchMode.Artists) searchQuery else "",
                                selection = selection,
                                onIdsChanged = { currentListToSelect = it }
                            )
                        }
                    }

                    if (searchMode != SearchMode.None && searchSuggestionsList.isNotEmpty() && shouldShowSuggestions) {
                        val paddingValues = LocalPlayerAwareWindowInsets.current
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top).asPaddingValues()

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = paddingValues.calculateTopPadding() + 72.dp)
                                .background(colorPalette.background0)
                                .zIndex(2f)
                        ) {
                            items(items = searchSuggestionsList) { suggestion ->
                                MenuEntry(
                                    icon = R.drawable.search,
                                    text = suggestion.text,
                                    onClick = {
                                        val endpoint = suggestion.endpoint?.endpoint
                                        if (endpoint is NavigationEndpoint.Endpoint.Browse && endpoint.browseId != null &&
                                            (endpoint.pageType == "MUSIC_PAGE_TYPE_ALBUM" || endpoint.pageType == "MUSIC_PAGE_TYPE_PLAYLIST" || endpoint.pageType == "MUSIC_PAGE_TYPE_ARTIST")) {
                                            when (endpoint.pageType) {
                                                "MUSIC_PAGE_TYPE_ALBUM" -> albumDetailsRoute.global(endpoint.browseId!!, com.synap.music.models.Album(id = endpoint.browseId!!, title = suggestion.text, thumbnailUrl = suggestion.thumbnail?.url))
                                                "MUSIC_PAGE_TYPE_PLAYLIST" -> playlistRoute.global(endpoint.browseId)
                                                "MUSIC_PAGE_TYPE_ARTIST" -> artistRoute.global(endpoint.browseId!!, null)
                                            }
                                            searchMode = SearchMode.None
                                        } else {
                                            searchQuery = suggestion.text
                                        }
                                        searchSuggestionsList = emptyList()
                                        shouldShowSuggestions = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UniversalFilterButton(
    tabIndex: Int,
    onRefreshDiscover: () -> Unit,
    onStartSearch: (SearchMode) -> Unit
) {
    val menuState = LocalMenuState.current

    val language by rememberPreference(discoveryLanguageKey, "None")
    val genre by rememberPreference(discoveryGenreKey, "None")
    val decade by rememberPreference(discoveryDecadeKey, "None")
    val style by rememberPreference(discoveryStyleKey, "Random")
    val limit by rememberPreference(discoveryLimitKey, 40)
    var viewMode by rememberPreference(discoveryViewModeKey, DiscoveryViewMode.Grid)
    var filterAiSongs by rememberPreference(filterAiSongsKey, false)
    var isProSearchMode by rememberPreference(isProSearchModeKey, false)

    FilterButton(
        label = "Filter",
        onShowMenu = {
            menuState.display {
                var currentSubMenu by remember { mutableStateOf<String?>(null) }

                Menu {
                    when (currentSubMenu) {
                        "Language" -> {
                            MenuEntry(icon = R.drawable.chevron_back, text = "Back", onClick = { currentSubMenu = null })
                            DiscoveryFiltersSection(type = "Language", onRefresh = onRefreshDiscover, onSelect = { currentSubMenu = null })
                        }
                        "Genre" -> {
                            MenuEntry(icon = R.drawable.chevron_back, text = "Back", onClick = { currentSubMenu = null })
                            DiscoveryFiltersSection(type = "Genre", onRefresh = onRefreshDiscover, onSelect = { currentSubMenu = null })
                        }
                        "Year" -> {
                            MenuEntry(icon = R.drawable.chevron_back, text = "Back", onClick = { currentSubMenu = null })
                            DiscoveryFiltersSection(type = "Year", onRefresh = onRefreshDiscover, onSelect = { currentSubMenu = null })
                        }
                        "Style" -> {
                            MenuEntry(icon = R.drawable.chevron_back, text = "Back", onClick = { currentSubMenu = null })
                            DiscoveryFiltersSection(type = "Style", onRefresh = onRefreshDiscover, onSelect = { currentSubMenu = null })
                        }
                        "Limit" -> {
                            MenuEntry(icon = R.drawable.chevron_back, text = "Back", onClick = { currentSubMenu = null })
                            DiscoveryLimitSection(onRefresh = onRefreshDiscover, onSelect = { currentSubMenu = null })
                        }
                        "View" -> {
                            MenuEntry(icon = R.drawable.chevron_back, text = "Back", onClick = { currentSubMenu = null })
                            MenuEntry(
                                icon = R.drawable.shapes,
                                text = "Grid View",
                                onClick = { viewMode = DiscoveryViewMode.Grid; currentSubMenu = null },
                                trailingContent = if (viewMode == DiscoveryViewMode.Grid) ({ CheckmarkIcon() }) else null
                            )
                            MenuEntry(
                                icon = R.drawable.reorder,
                                text = "List View",
                                onClick = { viewMode = DiscoveryViewMode.List; currentSubMenu = null },
                                trailingContent = if (viewMode == DiscoveryViewMode.List) ({ CheckmarkIcon() }) else null
                            )
                        }
                        "SearchType" -> {
                            MenuEntry(icon = R.drawable.chevron_back, text = "Back", onClick = { currentSubMenu = null })
                            MenuEntry(
                                icon = if (!isProSearchMode) R.drawable.checkmark else R.drawable.disc,
                                text = "Normal Search",
                                secondaryText = "Standard YouTube Music catalog",
                                onClick = {
                                    isProSearchMode = false
                                    currentSubMenu = null
                                }
                            )
                            MenuEntry(
                                icon = if (isProSearchMode) R.drawable.checkmark else R.drawable.disc,
                                text = "PRO Search",
                                secondaryText = "Deep search for videos & channels (e.g. SynapSonix)",
                                onClick = {
                                    isProSearchMode = true
                                    currentSubMenu = null
                                }
                            )
                        }
                        else -> {
                            MenuEntry(
                                icon = R.drawable.chevron_back,
                                text = "Back",
                                onClick = { menuState.hide() }
                            )
                            MenuEntry(
                                icon = R.drawable.globe,
                                text = "Global Online Search",
                                onClick = {
                                    searchRoute.global("")
                                    menuState.hide()
                                }
                            )

                            MenuEntry(
                                icon = R.drawable.search,
                                text = "Search Mode",
                                secondaryText = if (isProSearchMode) "PRO Search" else "Normal Search",
                                onClick = { currentSubMenu = "SearchType" }
                            )

                            Spacer(Modifier.padding(vertical = 4.dp))

                            // 1. Unified Search Action (always at top)
                            val sectionName = when(tabIndex) {
                                0 -> "Discover"
                                1 -> "Songs"
                                2 -> "DEN"
                                3 -> "Albums"
                                4 -> "Artists"
                                else -> ""
                            }
                            MenuEntry(
                                icon = R.drawable.search,
                                text = "Search here \"$sectionName\"",
                                onClick = {
                                    onStartSearch(when(tabIndex) {
                                        0 -> SearchMode.Discover
                                        1 -> SearchMode.Songs
                                        2 -> SearchMode.DEN
                                        3 -> SearchMode.Albums
                                        4 -> SearchMode.Artists
                                        else -> SearchMode.None
                                    })
                                    menuState.hide()
                                }
                            )

                            Spacer(Modifier.padding(vertical = 4.dp))

                            // 3. Discovery Filters (Always available: language, genre, year, style)
                            MenuEntry(icon = R.drawable.text, text = "Language", secondaryText = if (language == "None") "All Languages" else language, onClick = { currentSubMenu = "Language" })
                            MenuEntry(icon = R.drawable.musical_notes, text = "Genre", secondaryText = if (genre == "None") "All Genres" else genre, onClick = { currentSubMenu = "Genre" })
                            MenuEntry(icon = R.drawable.calendar, text = "Year", secondaryText = if (decade == "None") "All Time" else decade.replace("Year", ""), onClick = { currentSubMenu = "Year" })
                            MenuEntry(icon = R.drawable.star, text = "Style", secondaryText = style, onClick = { currentSubMenu = "Style" })
                            MenuEntry(
                                icon = R.drawable.disc,
                                text = "Exclude AI Songs",
                                secondaryText = if (filterAiSongs) "Enabled (Genuine Songs Only)" else "Disabled (Default)",
                                onClick = {
                                    filterAiSongs = !filterAiSongs
                                    onRefreshDiscover()
                                    menuState.hide()
                                },
                                trailingContent = if (filterAiSongs) ({ CheckmarkIcon() }) else null
                            )

                            // 4. Limit & View (Discover tab ONLY)
                            if (tabIndex == 0) {
                                MenuEntry(icon = R.drawable.reorder, text = "Limit", secondaryText = "$limit songs", onClick = { currentSubMenu = "Limit" })
                                MenuEntry(icon = R.drawable.shapes, text = "View Mode", secondaryText = viewMode.name, onClick = { currentSubMenu = "View" })
                            }

                            Spacer(Modifier.padding(vertical = 4.dp))

                            // 5. Sorting Group (Contextual sorting options)
                            SortingFiltersSection(tabIndex, onRefreshDiscover)
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun DiscoveryFiltersSection(type: String, onRefresh: () -> Unit, onSelect: () -> Unit) {
    var language by rememberPreference(discoveryLanguageKey, "None")
    var genre by rememberPreference(discoveryGenreKey, "None")
    var decade by rememberPreference(discoveryDecadeKey, "None")
    var style by rememberPreference(discoveryStyleKey, "Random")

    var subSearch by remember { mutableStateOf("") }

    SearchEntry(query = subSearch, onValueChange = { subSearch = it }, hint = "Search...", onClear = { subSearch = "" })
    
    when (type) {
        "Language" -> DiscoveryFilter.Language.entries.filter { it.label.contains(subSearch, true) }.forEach {
            val name = it::class.simpleName!!
            MenuEntry(
                icon = R.drawable.text,
                text = it.label,
                onClick = { language = name; onSelect(); onRefresh() },
                trailingContent = if (name == language) ({ CheckmarkIcon() }) else null
            )
        }
        "Genre" -> DiscoveryFilter.Genre.entries.filter { it.label.contains(subSearch, true) }.forEach {
            val name = it::class.simpleName!!
            MenuEntry(
                icon = R.drawable.musical_notes,
                text = it.label,
                onClick = { genre = name; onSelect(); onRefresh() },
                trailingContent = if (name == genre) ({ CheckmarkIcon() }) else null
            )
        }
        "Year" -> DiscoveryFilter.Decade.entries.filter { it.label.contains(subSearch, true) }.forEach {
            val name = it::class.simpleName!!
            MenuEntry(
                icon = R.drawable.calendar,
                text = it.label,
                onClick = { decade = name; onSelect(); onRefresh() },
                trailingContent = if (name == decade) ({ CheckmarkIcon() }) else null
            )
        }
        "Style" -> DiscoveryFilter.Style.entries.forEach {
            val name = it::class.simpleName!!
            MenuEntry(
                icon = R.drawable.star,
                text = it.label,
                onClick = { style = name; onSelect(); onRefresh() },
                trailingContent = if (name == style) ({ CheckmarkIcon() }) else null
            )
        }
    }
}

@Composable
private fun SortingFiltersSection(tabIndex: Int, onRefresh: (() -> Unit)? = null) {
    when (tabIndex) {
        0 -> { // Discover sorting
             var sortBy by rememberPreference(songSortByKey, SongSortBy.DateAdded)
             var sortOrder by rememberPreference(songSortOrderKey, SortOrder.Descending)
             MenuEntry(
                 icon = R.drawable.trending,
                 text = "By Play Time",
                 onClick = { sortBy = SongSortBy.PlayTime; onRefresh?.invoke() },
                 trailingContent = if (sortBy == SongSortBy.PlayTime) ({ CheckmarkIcon() }) else null
             )
             MenuEntry(
                 icon = R.drawable.time,
                 text = "By Date Added",
                 onClick = { sortBy = SongSortBy.DateAdded; onRefresh?.invoke() },
                 trailingContent = if (sortBy == SongSortBy.DateAdded) ({ CheckmarkIcon() }) else null
             )
             MenuEntry(
                 icon = R.drawable.text,
                 text = "By Title",
                 onClick = { sortBy = SongSortBy.Title; onRefresh?.invoke() },
                 trailingContent = if (sortBy == SongSortBy.Title) ({ CheckmarkIcon() }) else null
             )
             SortOrderMenuEntry(sortOrder = sortOrder, onSortOrderChanged = { sortOrder = it; onRefresh?.invoke() })
        }
        1 -> { // Songs
            var sortBy by rememberPreference(songSortByKey, SongSortBy.RecentlyPlayed)
            var sortOrder by rememberPreference(songSortOrderKey, SortOrder.Descending)
            MenuEntry(icon = R.drawable.trending, text = "By Play Time", onClick = { sortBy = SongSortBy.PlayTime }, trailingContent = if (sortBy == SongSortBy.PlayTime) ({ CheckmarkIcon() }) else null)
            MenuEntry(icon = R.drawable.time, text = "By Date Added", onClick = { sortBy = SongSortBy.DateAdded }, trailingContent = if (sortBy == SongSortBy.DateAdded) ({ CheckmarkIcon() }) else null)
            MenuEntry(icon = R.drawable.text, text = "By Title", onClick = { sortBy = SongSortBy.Title }, trailingContent = if (sortBy == SongSortBy.Title) ({ CheckmarkIcon() }) else null)
            SortOrderMenuEntry(sortOrder = sortOrder, onSortOrderChanged = { sortOrder = it })
        }
        2 -> { // Playlists
            var sortBy by rememberPreference(playlistSortByKey, PlaylistSortBy.DateAdded)
            var sortOrder by rememberPreference(playlistSortOrderKey, SortOrder.Descending)
            MenuEntry(icon = R.drawable.text, text = "By Title", onClick = { sortBy = PlaylistSortBy.Name }, trailingContent = if (sortBy == PlaylistSortBy.Name) ({ CheckmarkIcon() }) else null)
            MenuEntry(icon = R.drawable.time, text = "By Date Added", onClick = { sortBy = PlaylistSortBy.DateAdded }, trailingContent = if (sortBy == PlaylistSortBy.DateAdded) ({ CheckmarkIcon() }) else null)
            SortOrderMenuEntry(sortOrder = sortOrder, onSortOrderChanged = { sortOrder = it })
        }
        3 -> { // Albums
            var sortBy by rememberPreference(albumSortByKey, AlbumSortBy.DateAdded)
            var sortOrder by rememberPreference(albumSortOrderKey, SortOrder.Descending)
            MenuEntry(icon = R.drawable.text, text = "By Title", onClick = { sortBy = AlbumSortBy.Title }, trailingContent = if (sortBy == AlbumSortBy.Title) ({ CheckmarkIcon() }) else null)
            MenuEntry(icon = R.drawable.time, text = "By Date Added", onClick = { sortBy = AlbumSortBy.DateAdded }, trailingContent = if (sortBy == AlbumSortBy.DateAdded) ({ CheckmarkIcon() }) else null)
            SortOrderMenuEntry(sortOrder = sortOrder, onSortOrderChanged = { sortOrder = it })
        }
        4 -> { // Artists
            var sortBy by rememberPreference(artistSortByKey, ArtistSortBy.DateAdded)
            var sortOrder by rememberPreference(artistSortOrderKey, SortOrder.Descending)
            MenuEntry(icon = R.drawable.text, text = "By Title", onClick = { sortBy = ArtistSortBy.Name }, trailingContent = if (sortBy == ArtistSortBy.Name) ({ CheckmarkIcon() }) else null)
            MenuEntry(icon = R.drawable.time, text = "By Date Added", onClick = { sortBy = ArtistSortBy.DateAdded }, trailingContent = if (sortBy == ArtistSortBy.DateAdded) ({ CheckmarkIcon() }) else null)
            SortOrderMenuEntry(sortOrder = sortOrder, onSortOrderChanged = { sortOrder = it })
        }
    }
}

@Composable
private fun DiscoveryLimitSection(onRefresh: () -> Unit, onSelect: () -> Unit) {
    var limit by rememberPreference(discoveryLimitKey, 40)
    (10..100 step 10).forEach { count ->
        MenuEntry(
            icon = R.drawable.reorder,
            text = "$count songs",
            onClick = { limit = count; onSelect(); onRefresh() },
            trailingContent = if (limit == count) ({ CheckmarkIcon() }) else null
        )
    }
}

@Composable
private fun CheckmarkIcon() {
    val (colorPalette) = LocalAppearance.current
    Image(painterResource(R.drawable.checkmark), null, colorFilter = ColorFilter.tint(colorPalette.accent), modifier = Modifier.size(16.dp))
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
                cursorBrush = SolidColor(colorPalette.accent),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (query.isNotEmpty()) {
            Image(
                painter = painterResource(R.drawable.close),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                modifier = Modifier
                    .size(14.dp)
                    .clickable(onClick = onClear)
            )
        }
    }
}
