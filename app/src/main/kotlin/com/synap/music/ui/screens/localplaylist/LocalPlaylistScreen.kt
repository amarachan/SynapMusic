package com.synap.music.ui.screens.localplaylist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.synap.compose.persist.PersistMapCleanup
import com.synap.music.R
import com.synap.music.ui.components.themed.Scaffold
import com.synap.music.ui.styling.LocalAppearance
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.style.TextOverflow
import com.synap.music.utils.bold
import com.synap.music.utils.semiBold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import com.synap.music.Database
import com.synap.compose.routing.RouteHandlerScope
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.ui.components.themed.HeaderIconButton
import com.synap.music.ui.components.themed.SecondaryTextButton
import com.synap.music.ui.components.themed.ConfirmationDialog
import com.synap.music.ui.components.themed.PlaylistSelectionDialog
import com.synap.music.ui.components.themed.SynapCubeTitleButton
import com.synap.music.ui.components.themed.SynapCubeExportMenu
import com.synap.music.utils.rememberPreference
import com.synap.music.utils.cubeCompressionLevelKey
import com.synap.music.enums.CubeCompressionLevel
import com.synap.music.utils.toast
import com.synap.music.utils.MediaFetchUtils
import com.synap.music.utils.asMediaItem
import com.synap.music.cubes.AudioCubeManager
import com.synap.music.models.SongPlaylistMap
import com.synap.music.query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun RouteHandlerScope.LocalPlaylistScreen(playlistId: Long) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val playlistWithSongs by Database.playlistWithSongs(playlistId).collectAsState(initial = null)

    PersistMapCleanup(tagPrefix = "localPlaylist/$playlistId/")

    val (colorPalette, typography) = LocalAppearance.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val menuState = LocalMenuState.current

    val selection = remember { mutableStateListOf<String>() }
    var currentListToSelect by remember { mutableStateOf(emptyList<String>()) }
    var isShowingDeleteDialog by remember { mutableStateOf(false) }
    var isShowingPlaylistSelectionDialog by remember { mutableStateOf(false) }

    var selectedSongIdsToExport by remember { mutableStateOf<List<String>>(emptyList()) }
    var cubeCompressionLevel by rememberPreference(cubeCompressionLevelKey, CubeCompressionLevel.Auto)

    val exportSongsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
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
                    context.toast("Added ${selectedIds.size} songs to ${playlist.name}")
                }
            }
        )
    }

    if (isShowingDeleteDialog) {
        ConfirmationDialog(
            text = "Are you sure you want to remove ${selection.size} songs from this playlist?",
            onDismiss = { isShowingDeleteDialog = false },
            onConfirm = {
                val toDelete = selection.toList()
                selection.clear()
                coroutineScope.launch {
                    query {
                        toDelete.forEach { songId ->
                            Database.delete(SongPlaylistMap(songId = songId, playlistId = playlistId, position = 0))
                        }
                    }
                    context.toast("Removed ${toDelete.size} songs")
                }
            }
        )
    }

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
                                    val ids = selection.toList()
                                    if (ids.isNotEmpty()) {
                                        selectedSongIdsToExport = ids
                                        exportSongsLauncher.launch("Tracks_Export.synapcube")
                                    }
                                },
                                onDismiss = menuState::hide
                            )
                        }
                    }
                )
            } else {
                BasicText(
                    text = playlistWithSongs?.playlist?.name ?: "Playlist",
                    style = typography.l.bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        topStartIconButtonId = if (selection.isNotEmpty()) R.drawable.close else R.drawable.chevron_back,
        onTopStartIconButtonClick = if (selection.isNotEmpty()) ({ selection.clear() }) else pop,
        topEndIconButtonId = if (selection.isNotEmpty()) R.drawable.trash else null,
        onTopEndIconButtonClick = if (selection.isNotEmpty()) ({ isShowingDeleteDialog = true }) else null,
        topBarActions = {
            if (selection.isNotEmpty()) {
                HeaderIconButton(
                    icon = R.drawable.playlist,
                    color = colorPalette.text,
                    onClick = { isShowingPlaylistSelectionDialog = true }
                )

                HeaderIconButton(
                    icon = R.drawable.airplane,
                    color = colorPalette.text,
                    onClick = {
                        val ids = selection.toList()
                        context.toast("Starting download for ${ids.size} songs...")
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
            }
        },
        topBarModifier = if (selection.isNotEmpty()) Modifier.background(colorPalette.background1) else Modifier,
        tabIndex = 0,
        onTabChanged = { },
        tabColumnContent = { Item ->
            Item(0, "Songs", R.drawable.musical_notes)
        }
    ) { currentTabIndex ->
        saveableStateHolder.SaveableStateProvider(currentTabIndex) {
            when (currentTabIndex) {
                0 -> LocalPlaylistSongs(
                    playlistId = playlistId,
                    onDelete = pop,
                    selection = selection,
                    onIdsChanged = { currentListToSelect = it }
                )
            }
        }
    }
}
