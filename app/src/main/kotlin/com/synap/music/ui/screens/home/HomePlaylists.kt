package com.synap.music.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import com.synap.compose.persist.persistList
import com.synap.music.Database
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.R
import com.synap.music.enums.BuiltInPlaylist
import com.synap.music.enums.PlaylistSortBy
import com.synap.music.enums.SortOrder
import com.synap.music.models.Playlist
import com.synap.music.models.PlaylistPreview
import com.synap.music.query
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.synap.music.ui.components.themed.InLibraryPlaylistMenu
import com.synap.music.ui.components.themed.BuiltInPlaylistMenu
import com.synap.music.ui.components.themed.AppleMusicImportDialog
import com.synap.music.ui.components.themed.LiveRadioDialog
import com.synap.music.ui.components.themed.LocalFolderScanDialog
import com.synap.music.ui.screens.allPlaylistsRoute
import com.synap.music.ui.screens.smartMixesRoute
import com.synap.music.ui.screens.synapRecapRoute
import com.synap.music.ui.components.themed.SpotifyImportDialog
import com.synap.music.ui.components.themed.TextFieldDialog
import com.synap.music.ui.items.PlaylistItem
import com.synap.music.ui.screens.settingsRoute
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.px
import com.synap.music.utils.isAppleMusicImportEnabledKey
import com.synap.music.utils.playlistSortByKey
import com.synap.music.utils.isLiveRadioEnabledKey
import com.synap.music.utils.isLocalStorageEnabledKey
import com.synap.music.utils.isSmartMixesEnabledKey
import com.synap.music.utils.isSpotifyImportEnabledKey
import com.synap.music.utils.isSynapRecapEnabledKey
import com.synap.music.utils.isWebdavEnabledKey
import com.synap.music.utils.playlistSortOrderKey
import com.synap.music.utils.rememberPreference
import kotlinx.coroutines.flow.map
import com.synap.music.MainApplication
import com.synap.music.cubes.AudioCubeManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.synap.music.enums.CubeCompressionLevel
import com.synap.music.utils.cubeCompressionLevelKey
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.synap.music.utils.toast
import com.synap.music.utils.MediaFetchUtils
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import com.synap.music.transaction
import com.synap.pulse.requests.playlistPage
import com.synap.pulse.models.bodies.BrowseBody
import com.synap.pulse.SynapService
import com.synap.music.models.SongPlaylistMap
import com.synap.music.utils.asMediaItem
import kotlinx.coroutines.flow.firstOrNull
import com.synap.music.utils.completed
import com.synap.music.utils.forcePlayFromBeginning
import com.synap.music.LocalPlayerServiceBinder
import androidx.compose.foundation.combinedClickable

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun HomePlaylists(
    onBuiltInPlaylist: (BuiltInPlaylist) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onSearchClick: (SearchMode) -> Unit,
    searchQuery: String = "",
    selection: MutableList<String>,
    onIdsChanged: (List<String>) -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val menuState = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()
    val binder = LocalPlayerServiceBinder.current

    var isCreatingANewPlaylist by rememberSaveable {
        mutableStateOf(false)
    }

    var isImportingSpotifyPlaylist by rememberSaveable {
        mutableStateOf(false)
    }

    var isImportingAppleMusicPlaylist by rememberSaveable {
        mutableStateOf(false)
    }

    var isShowingLiveRadioDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var isShowingLocalScanDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var isShowingSmartMixesDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var isRenamingPlaylist by rememberSaveable { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }

    if (isRenamingPlaylist && playlistToRename != null) {
        TextFieldDialog(
            hintText = "Enter the new playlist name",
            initialTextInput = playlistToRename!!.name,
            onDismiss = {
                isRenamingPlaylist = false
                playlistToRename = null
            },
            onDone = { text ->
                query {
                    Database.update(playlistToRename!!.copy(name = text))
                }
                isRenamingPlaylist = false
                playlistToRename = null
            }
        )
    }

    var playlistToExport by remember { mutableStateOf<Playlist?>(null) }
    val cubeCompressionLevel by rememberPreference(cubeCompressionLevelKey, CubeCompressionLevel.Auto)
    
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let {
            playlistToExport?.let { playlist ->
                MainApplication.scope.launch {
                    val result = AudioCubeManager.exportPlaylistToCube(context, playlist.id, uri, cubeCompressionLevel)
                    if (result.isSuccess) {
                        context.toast("Audio Cube exported successfully!")
                    } else {
                        context.toast("Failed to export Audio Cube")
                    }
                }
            }
        }
        playlistToExport = null
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

    if (isImportingAppleMusicPlaylist) {
        AppleMusicImportDialog(
            onDismiss = {
                isImportingAppleMusicPlaylist = false
            }
        )
    }

    if (isShowingLiveRadioDialog) {
        LiveRadioDialog(
            onDismiss = {
                isShowingLiveRadioDialog = false
            }
        )
    }

    if (isShowingLocalScanDialog) {
        LocalFolderScanDialog(
            onDismiss = {
                isShowingLocalScanDialog = false
            }
        )
    }

    val sortBy by rememberPreference(playlistSortByKey, PlaylistSortBy.DateAdded)
    val sortOrder by rememberPreference(playlistSortOrderKey, SortOrder.Descending)

    val isLocalStorageEnabled by rememberPreference(isLocalStorageEnabledKey, true)
    val isSmartMixesEnabled by rememberPreference(isSmartMixesEnabledKey, true)
    val isSynapRecapEnabled by rememberPreference(isSynapRecapEnabledKey, true)
    val isSpotifyImportEnabled by rememberPreference(isSpotifyImportEnabledKey, true)
    val isAppleMusicImportEnabled by rememberPreference(isAppleMusicImportEnabledKey, true)
    val isLiveRadioEnabled by rememberPreference(isLiveRadioEnabledKey, true)
    val isWebdavEnabled by rememberPreference(isWebdavEnabledKey, false)

    var playlistItems by persistList<PlaylistPreview>("home/playlists")

    LaunchedEffect(sortBy, sortOrder) {
        Database.playlistPreviews(sortBy, sortOrder).collect { playlistItems = it }
    }

    val filteredItems = remember(playlistItems, searchQuery) {
        if (searchQuery.isEmpty()) playlistItems else playlistItems.filter {
            it.playlist.name.contains(searchQuery, ignoreCase = true)
        }
    }

    val favoritesCount by remember { Database.favorites().map { it.size } }.collectAsState(initial = 0)
    val offlineSongsCount by remember {
        Database.songsWithContentLength().map { songs ->
            songs.size
        }
    }.collectAsState(initial = 0)

    val lazyGridState = rememberLazyGridState()

    val layoutDirection = LocalLayoutDirection.current
    val paddingValues = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    val contentPadding = PaddingValues(
        start = paddingValues.calculateStartPadding(layoutDirection),
        top = paddingValues.calculateTopPadding() + 72.dp + 16.dp,
        end = paddingValues.calculateEndPadding(layoutDirection),
        bottom = paddingValues.calculateBottomPadding()
    )

    val thumbnailSizeDp = 108.dp
    val thumbnailSizePx = thumbnailSizeDp.px

    Box(
        modifier = Modifier
            .background(colorPalette.background0)
            .fillMaxSize()
    ) {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Fixed(2),
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "favorites") {
                PlaylistItem(
                    icon = R.drawable.heart,
                    colorTint = colorPalette.red,
                    name = "Star Collection",
                    songCount = favoritesCount,
                    thumbnailSizeDp = thumbnailSizeDp,
                    alternative = true,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            if (selection.isEmpty()) onBuiltInPlaylist(BuiltInPlaylist.Favorites)
                        },
                        onLongClick = {
                            if (selection.isEmpty()) {
                                menuState.display {
                                    BuiltInPlaylistMenu(
                                        playlistType = BuiltInPlaylist.Favorites,
                                        onDismiss = menuState::hide,
                                        onPlay = {
                                            coroutineScope.launch {
                                                val songs = Database.favorites().firstOrNull() ?: emptyList()
                                                if (songs.isNotEmpty()) {
                                                    binder?.stopRadio()
                                                    binder?.player?.forcePlayFromBeginning(songs.map { it.asMediaItem })
                                                }
                                            }
                                        },
                                        onShuffle = {
                                            coroutineScope.launch {
                                                val songs = Database.favorites().firstOrNull() ?: emptyList()
                                                if (songs.isNotEmpty()) {
                                                    binder?.stopRadio()
                                                    binder?.player?.forcePlayFromBeginning(songs.shuffled().map { it.asMediaItem })
                                                }
                                            }
                                        },
                                        onDownloadAll = {
                                            coroutineScope.launch {
                                                val songs = Database.favorites().firstOrNull() ?: emptyList()
                                                if (songs.isNotEmpty()) {
                                                    withContext(Dispatchers.Main) {
                                                        context.toast("Starting download for ${songs.size} songs")
                                                    }
                                                    songs.forEach { song ->
                                                        MainApplication.scope.launch(Dispatchers.IO) {
                                                            MediaFetchUtils.downloadMedia(context, song.asMediaItem, false)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    )
                )
            }

            item(key = "offline") {
                PlaylistItem(
                    icon = R.drawable.airplane,
                    colorTint = colorPalette.blue,
                    name = "Vault",
                    songCount = offlineSongsCount,
                    thumbnailSizeDp = thumbnailSizeDp,
                    alternative = true,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            if (selection.isEmpty()) onBuiltInPlaylist(BuiltInPlaylist.Offline)
                        },
                        onLongClick = {
                            if (selection.isEmpty()) {
                                menuState.display {
                                    BuiltInPlaylistMenu(
                                        playlistType = BuiltInPlaylist.Offline,
                                        onDismiss = menuState::hide,
                                        onPlay = {
                                            coroutineScope.launch {
                                                val songs = Database.songsWithContentLength().firstOrNull()?.map { it.song } ?: emptyList()
                                                if (songs.isNotEmpty()) {
                                                    binder?.stopRadio()
                                                    binder?.player?.forcePlayFromBeginning(songs.map { it.asMediaItem })
                                                }
                                            }
                                        },
                                        onShuffle = {
                                            coroutineScope.launch {
                                                val songs = Database.songsWithContentLength().firstOrNull()?.map { it.song } ?: emptyList()
                                                if (songs.isNotEmpty()) {
                                                    binder?.stopRadio()
                                                    binder?.player?.forcePlayFromBeginning(songs.shuffled().map { it.asMediaItem })
                                                }
                                            }
                                        },
                                        onExport = {
                                            exportLauncher.launch("Vault.synapcube")
                                        }
                                    )
                                }
                            }
                        }
                    )
                )
            }

            item(key = "user_playlists_hub") {
                PlaylistItem(
                    icon = R.drawable.playlist,
                    colorTint = colorPalette.accent,
                    name = "Playlists",
                    songCount = playlistItems.size,
                    thumbnailSizeDp = thumbnailSizeDp,
                    alternative = true,
                    modifier = Modifier.clickable {
                        if (selection.isEmpty()) {
                            allPlaylistsRoute.global()
                        }
                    }
                )
            }

            if (isLocalStorageEnabled) {
                item(key = "local_storage_scan") {
                    PlaylistItem(
                        icon = R.drawable.library,
                        colorTint = colorPalette.accent,
                        name = "Local Storage",
                        songCount = null,
                        thumbnailSizeDp = thumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier.clickable {
                            if (selection.isEmpty()) {
                                isShowingLocalScanDialog = true
                            }
                        }
                    )
                }
            }

            if (isSmartMixesEnabled) {
                item(key = "smart_mixes") {
                    PlaylistItem(
                        icon = R.drawable.lightning,
                        colorTint = colorPalette.accent,
                        name = "Smart Mixes",
                        songCount = null,
                        thumbnailSizeDp = thumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier.clickable {
                            if (selection.isEmpty()) {
                                smartMixesRoute.global()
                            }
                        }
                    )
                }
            }

            if (isSynapRecapEnabled) {
                item(key = "synap_recap") {
                    PlaylistItem(
                        icon = R.drawable.bar_chart,
                        colorTint = colorPalette.blue,
                        name = "Synap Recap",
                        songCount = null,
                        thumbnailSizeDp = thumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier.clickable {
                            if (selection.isEmpty()) {
                                synapRecapRoute.global()
                            }
                        }
                    )
                }
            }

            if (isSpotifyImportEnabled) {
                item(key = "import_spotify") {
                    PlaylistItem(
                        icon = R.drawable.spotify,
                        colorTint = Color(0xFF1DB954),
                        name = "Import Spotify",
                        songCount = null,
                        thumbnailSizeDp = thumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier.clickable {
                            if (selection.isEmpty()) {
                                isImportingSpotifyPlaylist = true
                            }
                        }
                    )
                }
            }

            if (isAppleMusicImportEnabled) {
                item(key = "import_applemusic") {
                    PlaylistItem(
                        icon = R.drawable.apple_music,
                        colorTint = Color(0xFFFA233B),
                        name = "Import Apple Music",
                        songCount = null,
                        thumbnailSizeDp = thumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier.clickable {
                            if (selection.isEmpty()) {
                                isImportingAppleMusicPlaylist = true
                            }
                        }
                    )
                }
            }

            if (isLiveRadioEnabled) {
                item(key = "live_radio") {
                    PlaylistItem(
                        icon = R.drawable.radio_tower,
                        colorTint = colorPalette.accent,
                        name = "Live Radio",
                        songCount = null,
                        thumbnailSizeDp = thumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier.clickable {
                            if (selection.isEmpty()) {
                                isShowingLiveRadioDialog = true
                            }
                        }
                    )
                }
            }

            if (isWebdavEnabled) {
                item(key = "webdav_nas") {
                    PlaylistItem(
                        icon = R.drawable.server,
                        colorTint = colorPalette.red,
                        name = "Network Drive",
                        songCount = null,
                        thumbnailSizeDp = thumbnailSizeDp,
                        alternative = true,
                        modifier = Modifier.clickable {
                            if (selection.isEmpty()) {
                                settingsRoute.global()
                            }
                        }
                    )
                }
            }

            items(
                items = filteredItems,
                key = { it.playlist.id }
            ) { preview ->
                val idString = preview.playlist.id.toString()
                val isSelected = selection.contains(idString)
                PlaylistItem(
                    playlist = preview,
                    thumbnailSizePx = thumbnailSizePx,
                    thumbnailSizeDp = thumbnailSizeDp,
                    alternative = true,
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                if (selection.isNotEmpty()) {
                                    if (isSelected) selection.remove(idString) else selection.add(idString)
                                } else {
                                    onPlaylistClick(preview.playlist)
                                }
                            },
                            onLongClick = {
                                if (selection.isNotEmpty()) {
                                    if (isSelected) selection.remove(idString) else selection.add(idString)
                                } else {
                                    menuState.display {
                                        InLibraryPlaylistMenu(
                                            playlist = preview.playlist,
                                            onDismiss = menuState::hide,
                                            onRename = {
                                                playlistToRename = preview.playlist
                                                isRenamingPlaylist = true
                                            },
                                            onExport = {
                                                playlistToExport = preview.playlist
                                                exportLauncher.launch("${preview.playlist.name}.synapcube")
                                            },
                                            onDownloadAll = {
                                                coroutineScope.launch {
                                                    val playlistWithSongs = Database.playlistWithSongs(preview.playlist.id).firstOrNull()
                                                    val songs = playlistWithSongs?.songs ?: emptyList()
                                                    if (songs.isNotEmpty()) {
                                                        withContext(Dispatchers.Main) {
                                                            context.toast("Starting download for ${songs.size} songs")
                                                        }
                                                        songs.forEach { song ->
                                                            MainApplication.scope.launch(Dispatchers.IO) {
                                                                MediaFetchUtils.downloadMedia(context, song.asMediaItem, false)
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            onRefreshMetadata = preview.playlist.browseId?.let { browseId ->
                                                {
                                                    transaction {
                                                        runBlocking(Dispatchers.IO) {
                                                            withContext(Dispatchers.IO) {
                                                                SynapService.playlistPage(BrowseBody(browseId = browseId))
                                                                    ?.completed()
                                                            }
                                                        }?.getOrNull()?.let { remotePlaylist ->
                                                            Database.clearPlaylist(preview.playlist.id)

                                                            val mediaItems = remotePlaylist.songsPage?.list?.map {
                                                                it.asMediaItem
                                                            }
                                                            mediaItems?.onEach { Database.insert(it) }
                                                            val maps = mediaItems?.mapIndexed { position, mediaItem ->
                                                                SongPlaylistMap(
                                                                    songId = mediaItem.mediaId,
                                                                    playlistId = preview.playlist.id,
                                                                    position = position
                                                                )
                                                            }
                                                            if (maps != null) Database.insertSongPlaylistMaps(maps)
                                                        }
                                                    }
                                                }
                                            },
                                            onMultipleDelete = { selection.add(idString) }
                                        )
                                    }
                                }
                            }
                        )
                        .then(if (isSelected) Modifier.background(colorPalette.accent.copy(alpha = 0.3f)) else Modifier)
                        .animateItemPlacement()
                )
            }
        }

        // Visual Scrollbar Track & Accent Thumb for Playlists Grid
        val layoutInfo = lazyGridState.layoutInfo
        val totalItemsCount = layoutInfo.totalItemsCount

        if (totalItemsCount > 0 && layoutInfo.visibleItemsInfo.isNotEmpty()) {
            val visibleItemsCount = layoutInfo.visibleItemsInfo.size

            if (totalItemsCount > visibleItemsCount) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp, top = 88.dp, bottom = 88.dp)
                        .width(5.dp)
                        .fillMaxHeight()
                ) {
                    val canvasHeight = size.height

                    val scrollbarHeight = (canvasHeight * (visibleItemsCount.toFloat() / totalItemsCount.toFloat()))
                        .coerceAtLeast(36.dp.toPx())

                    val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull()
                    val scrollProgress = if (firstVisibleItem != null) {
                        val maxScrollIndex = (totalItemsCount - visibleItemsCount).coerceAtLeast(1)
                        val itemHeight = firstVisibleItem.size.height.toFloat()
                        
                        val fractionalOffset = if (itemHeight > 0) {
                            -firstVisibleItem.offset.y.toFloat() / itemHeight
                        } else 0f
                        
                        ((firstVisibleItem.index + fractionalOffset) / maxScrollIndex).coerceIn(0f, 1f)
                    } else 0f

                    val scrollbarOffset = scrollProgress * (canvasHeight - scrollbarHeight)

                    // Track
                    drawRoundRect(
                        color = colorPalette.textDisabled.copy(alpha = 0.2f),
                        size = size,
                        cornerRadius = CornerRadius(2.5f.dp.toPx(), 2.5f.dp.toPx())
                    )

                    // Accent Thumb
                    drawRoundRect(
                        color = colorPalette.accent,
                        topLeft = Offset(0f, scrollbarOffset),
                        size = Size(width = size.width, height = scrollbarHeight),
                        cornerRadius = CornerRadius(2.5f.dp.toPx(), 2.5f.dp.toPx())
                    )
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
        onIdsChanged(filteredItems.map { it.playlist.id.toString() })
    }
}
