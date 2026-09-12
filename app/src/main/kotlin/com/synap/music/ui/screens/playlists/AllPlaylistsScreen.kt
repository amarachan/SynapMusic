package com.synap.music.ui.screens.playlists

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synap.compose.persist.persistList
import com.synap.compose.routing.RouteHandlerScope
import com.synap.music.Database
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.MainApplication
import com.synap.music.R
import com.synap.music.cubes.AudioCubeManager
import com.synap.music.enums.PlaylistSortBy
import com.synap.music.enums.SortOrder
import com.synap.music.models.Playlist
import com.synap.music.models.PlaylistPreview
import com.synap.music.models.SongPlaylistMap
import com.synap.music.query
import com.synap.music.service.PlayerService
import com.synap.music.transaction
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.ui.components.themed.HeaderIconButton
import com.synap.music.ui.screens.home.UniversalFilterButton
import com.synap.music.ui.screens.searchRoute
import com.synap.music.ui.components.themed.InLibraryPlaylistMenu
import com.synap.music.ui.components.themed.Menu
import com.synap.music.ui.components.themed.MenuEntry
import com.synap.music.ui.components.themed.Scaffold
import com.synap.music.ui.components.themed.SynapCubeExportMenu
import com.synap.music.ui.components.themed.SynapCubeTitleButton
import com.synap.music.ui.components.themed.TextFieldDialog
import com.synap.music.ui.items.PlaylistItem
import com.synap.music.ui.screens.localPlaylistRoute
import com.synap.music.ui.screens.playlistRoute
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.px
import com.synap.music.utils.MediaFetchUtils
import com.synap.music.utils.asMediaItem
import com.synap.music.utils.bold
import com.synap.music.utils.forcePlayFromBeginning
import com.synap.music.utils.playlistSortByKey
import com.synap.music.utils.playlistSortOrderKey
import com.synap.music.utils.rememberPreference
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import com.synap.music.utils.toast
import com.synap.music.utils.completed
import com.synap.pulse.SynapService
import com.synap.pulse.models.bodies.BrowseBody
import com.synap.pulse.requests.playlistPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Dedicated Full-Screen All Playlists Screen.
 * Contains user-created and imported playlists with Create Playlist (+), Filter/Sort, and batch controls.
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun RouteHandlerScope.AllPlaylistsScreen() {
    val context = LocalContext.current
    val (colorPalette, typography) = LocalAppearance.current
    val menuState = LocalMenuState.current
    val binder = LocalPlayerServiceBinder.current
    val coroutineScope = rememberCoroutineScope()

    var sortBy by rememberPreference(playlistSortByKey, PlaylistSortBy.DateAdded)
    var sortOrder by rememberPreference(playlistSortOrderKey, SortOrder.Descending)

    var playlistItems by persistList<PlaylistPreview>("all_playlists/list")
    var isCreatingPlaylist by rememberSaveable { mutableStateOf(false) }
    var isRenamingPlaylist by rememberSaveable { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var playlistToExport by remember { mutableStateOf<Playlist?>(null) }

    val selection = remember { mutableStateListOf<String>() }
    val lazyGridState = rememberLazyGridState()
    val paddingValues = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current

    val thumbnailSizeDp = 108.dp
    val thumbnailSizePx = thumbnailSizeDp.px

    LaunchedEffect(sortBy, sortOrder) {
        Database.playlistPreviews(sortBy, sortOrder).collect { playlistItems = it }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            playlistToExport?.let { playlist ->
                coroutineScope.launch {
                    val result = AudioCubeManager.exportPlaylistsToCube(
                        context,
                        listOf(playlist.id),
                        uri
                    )
                    if (result.isSuccess) {
                        context.toast("Exported '${playlist.name}' to SynapCube!")
                    } else {
                        context.toast("Failed to export SynapCube")
                    }
                }
            }
        }
        playlistToExport = null
    }

    if (isCreatingPlaylist) {
        TextFieldDialog(
            hintText = "Enter playlist name",
            onDismiss = { isCreatingPlaylist = false },
            onDone = { text ->
                query {
                    Database.insert(Playlist(name = text))
                }
            }
        )
    }

    if (isRenamingPlaylist && playlistToRename != null) {
        playlistToRename?.let { playlist ->
            TextFieldDialog(
                initialTextInput = playlist.name,
                hintText = "Enter new playlist name",
                onDismiss = {
                    isRenamingPlaylist = false
                    playlistToRename = null
                },
                onDone = { text ->
                    query {
                        Database.update(playlist.copy(name = text))
                    }
                }
            )
        }
    }

    fun onPlaylistClick(playlist: Playlist) {
        if (playlist.browseId != null) {
            playlistRoute.global(playlist.browseId)
        } else {
            localPlaylistRoute.global(playlist.id)
        }
    }

    Scaffold(
        title = {
            BasicText(
                text = "Playlists",
                style = typography.l.bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        topStartIconButtonId = R.drawable.chevron_back,
        onTopStartIconButtonClick = pop,
        topBarActions = {
            // Create Playlist Button (+)
            HeaderIconButton(
                icon = R.drawable.add,
                color = colorPalette.text,
                onClick = { isCreatingPlaylist = true }
            )

            // Exact Universal Filter Button from DEN
            UniversalFilterButton(
                tabIndex = 2,
                onRefreshDiscover = {},
                onStartSearch = {
                    searchRoute.global("")
                }
            )
        },
        tabIndex = 0,
        onTabChanged = { },
        tabColumnContent = { } // No bottom navigation rail
    ) {
        val gridPaddingValues = PaddingValues(
            start = paddingValues.calculateStartPadding(layoutDirection) + 16.dp,
            top = 72.dp + 12.dp,
            end = paddingValues.calculateEndPadding(layoutDirection) + 16.dp,
            bottom = paddingValues.calculateBottomPadding() + 16.dp
        )

        Box(
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            if (playlistItems.isEmpty()) {
                BasicText(
                    text = "No playlists found. Tap '+' above to create one!",
                    style = typography.s.secondary,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
            } else {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Fixed(2),
                    contentPadding = gridPaddingValues,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = playlistItems,
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
                        )
                    }
                }
            }
        }
    }
}
