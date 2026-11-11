package com.synap.music.ui.screens.builtinplaylist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.text.style.TextOverflow
import com.synap.compose.persist.PersistMapCleanup
import com.synap.music.R
import com.synap.music.enums.BuiltInPlaylist
import com.synap.music.ui.components.themed.Scaffold
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.utils.bold
import com.synap.compose.routing.RouteHandlerScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import com.synap.music.Database
import com.synap.music.cubes.AudioCubeManager
import com.synap.music.enums.CubeCompressionLevel
import com.synap.music.enums.DiscoveryViewMode
import com.synap.music.enums.SongSortBy
import com.synap.music.enums.SortOrder
import com.synap.music.utils.vaultViewModeKey
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.synap.music.ui.components.themed.FilterButton
import com.synap.music.ui.components.themed.InListSearchHeader
import com.synap.music.ui.components.themed.Menu
import com.synap.music.ui.components.themed.MenuEntry
import com.synap.music.ui.components.themed.SortOrderMenuEntry
import com.synap.music.utils.songSortByKey
import com.synap.music.utils.songSortOrderKey
import com.synap.music.ui.components.themed.ConfirmationDialog
import com.synap.music.ui.components.themed.SecondaryTextButton
import com.synap.music.ui.components.themed.HeaderIconButton
import com.synap.music.ui.components.themed.PlaylistSelectionDialog
import com.synap.music.models.SongPlaylistMap
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.ui.components.themed.SynapCubeExportMenu
import com.synap.music.ui.components.themed.SynapCubeTitleButton
import com.synap.music.utils.MediaFetchUtils
import com.synap.music.utils.asMediaItem
import com.synap.music.utils.cubeCompressionLevelKey
import com.synap.music.utils.rememberPreference
import com.synap.music.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.synap.music.utils.semiBold

/**
 * Renders built-in system collections (e.g., Favorites and Offline Vault downloads).
 * Supports Title/PlayTime/DateAdded sorting and Grid/List View Mode layout toggling.
 */
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun RouteHandlerScope.BuiltInPlaylistScreen(builtInPlaylist: BuiltInPlaylist) {
    val saveableStateHolder = rememberSaveableStateHolder()

    // Determine which tab to show first based on where the user came from.
    var tabIndex by rememberSaveable {
        mutableStateOf(when (builtInPlaylist) {
            BuiltInPlaylist.Favorites -> 0
            BuiltInPlaylist.Offline -> 1
        })
    }

    PersistMapCleanup(tagPrefix = "${builtInPlaylist.name}/")

    val (colorPalette, typography) = LocalAppearance.current
    val coroutineScope = rememberCoroutineScope()

    val selection = remember { mutableStateListOf<String>() }
    var currentListToSelect by remember { mutableStateOf(emptyList<String>()) }
    var isShowingDeleteDialog by remember { mutableStateOf(false) }
    var isShowingPlaylistSelectionDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedSongIdsToExport by remember { mutableStateOf<List<String>>(emptyList()) }
    var cubeCompressionLevel by rememberPreference(cubeCompressionLevelKey, CubeCompressionLevel.Auto)

    var isSearchingVault by remember { mutableStateOf(false) }
    var inVaultSearchQuery by remember { mutableStateOf("") }
    var vaultSortBy by rememberPreference(songSortByKey, SongSortBy.DateAdded)
    var vaultSortOrder by rememberPreference(songSortOrderKey, SortOrder.Descending)
    var vaultViewMode by rememberPreference(vaultViewModeKey, DiscoveryViewMode.List)

    val exportSongsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
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
                    com.synap.music.query {
                        Database.addSongsToPlaylist(selectedIds, playlist.id, count)
                    }
                    context.toast("Added ${selectedIds.size} songs to ${playlist.name}")
                }
            }
        )
    }

    if (isShowingDeleteDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        ConfirmationDialog(
            text = "Are you sure you want to delete ${selection.size} songs from your storage?",
            onDismiss = { isShowingDeleteDialog = false },
            onConfirm = {
                val toDelete = selection.toList()
                selection.clear()
                coroutineScope.launch(Dispatchers.IO) {
                    toDelete.forEach { songId ->
                        val file = MediaFetchUtils.getAudioFile(context, songId)
                        if (file.exists()) file.delete()
                        com.synap.music.query {
                            Database.deleteFormat(songId)
                            Database.clearEventsFor(songId)
                        }
                    }
                }
                context.toast("Deleted ${toDelete.size} songs")
            }
        )
    }

    val title = when (builtInPlaylist) {
        BuiltInPlaylist.Favorites -> "Star Collection"
        BuiltInPlaylist.Offline -> "Vault"
    }

    Scaffold(
        title = {
            if (selection.isNotEmpty()) {
                val menuState = LocalMenuState.current
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
            } else if (isSearchingVault) {
                InListSearchHeader(
                    query = inVaultSearchQuery,
                    onQueryChange = { inVaultSearchQuery = it },
                    onClear = { inVaultSearchQuery = "" },
                    hint = "Search in $title..."
                )
            } else {
                BasicText(
                    text = title,
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
            val menuState = LocalMenuState.current
            if (selection.isNotEmpty()) {
                HeaderIconButton(
                    icon = R.drawable.playlist,
                    color = colorPalette.text,
                    onClick = { isShowingPlaylistSelectionDialog = true }
                )

                if (tabIndex == 0) {
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
            } else {
                // In-Vault Search Button
                HeaderIconButton(
                    icon = if (isSearchingVault) R.drawable.close else R.drawable.search,
                    color = colorPalette.text,
                    onClick = {
                        isSearchingVault = !isSearchingVault
                        if (!isSearchingVault) inVaultSearchQuery = ""
                    }
                )

                // Vault Filter & Sort Button Pill
                FilterButton(
                    label = "Filter",
                    onShowMenu = {
                        menuState.display {
                            var currentSubMenu by remember { mutableStateOf<String?>(null) }

                            Menu {
                                when (currentSubMenu) {
                                    "View" -> {
                                        MenuEntry(icon = R.drawable.chevron_back, text = "Back", onClick = { currentSubMenu = null })
                                        MenuEntry(
                                            icon = R.drawable.shapes,
                                            text = "Grid View",
                                            onClick = { vaultViewMode = DiscoveryViewMode.Grid; currentSubMenu = null },
                                            trailingContent = if (vaultViewMode == DiscoveryViewMode.Grid) ({ CheckmarkIcon() }) else null
                                        )
                                        MenuEntry(
                                            icon = R.drawable.reorder,
                                            text = "List View",
                                            onClick = { vaultViewMode = DiscoveryViewMode.List; currentSubMenu = null },
                                            trailingContent = if (vaultViewMode == DiscoveryViewMode.List) ({ CheckmarkIcon() }) else null
                                        )
                                    }
                                    else -> {
                                        MenuEntry(
                                            icon = R.drawable.text,
                                            text = "Sort by Title",
                                            onClick = {
                                                vaultSortBy = SongSortBy.Title
                                            },
                                            trailingContent = if (vaultSortBy == SongSortBy.Title) ({ CheckmarkIcon() }) else null
                                        )

                                        MenuEntry(
                                            icon = R.drawable.trending,
                                            text = "Sort by Play Time",
                                            onClick = {
                                                vaultSortBy = SongSortBy.PlayTime
                                            },
                                            trailingContent = if (vaultSortBy == SongSortBy.PlayTime) ({ CheckmarkIcon() }) else null
                                        )

                                        MenuEntry(
                                            icon = R.drawable.time,
                                            text = "Sort by Date Added",
                                            onClick = {
                                                vaultSortBy = SongSortBy.DateAdded
                                            },
                                            trailingContent = if (vaultSortBy == SongSortBy.DateAdded) ({ CheckmarkIcon() }) else null
                                        )

                                        SortOrderMenuEntry(
                                            sortOrder = vaultSortOrder,
                                            onSortOrderChanged = { newOrder ->
                                                vaultSortOrder = newOrder
                                            }
                                        )

                                        Spacer(Modifier.padding(vertical = 4.dp))

                                        MenuEntry(
                                            icon = R.drawable.shapes,
                                            text = "View Mode",
                                            secondaryText = if (vaultViewMode == DiscoveryViewMode.Grid) "Grid View" else "List View",
                                            onClick = { currentSubMenu = "View" }
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        },
        topBarModifier = if (selection.isNotEmpty()) Modifier.background(colorPalette.background1) else Modifier,
        tabIndex = tabIndex,
        onTabChanged = { tabIndex = it; selection.clear() },
        tabColumnContent = { Item ->
            Item(0, "Star Collection", R.drawable.heart)
            Item(1, "Vault", R.drawable.airplane)
        }
    ) { currentTabIndex ->
        saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
            when (currentTabIndex) {
                0 -> BuiltInPlaylistSongs(
                    builtInPlaylist = BuiltInPlaylist.Favorites,
                    selection = selection,
                    searchQuery = inVaultSearchQuery,
                    sortBy = vaultSortBy,
                    sortOrder = vaultSortOrder,
                    viewMode = vaultViewMode,
                    onIdsChanged = { if (tabIndex == 0) currentListToSelect = it }
                )
                1 -> BuiltInPlaylistSongs(
                    builtInPlaylist = BuiltInPlaylist.Offline,
                    selection = selection,
                    searchQuery = inVaultSearchQuery,
                    sortBy = vaultSortBy,
                    sortOrder = vaultSortOrder,
                    viewMode = vaultViewMode,
                    onIdsChanged = { if (tabIndex == 1) currentListToSelect = it }
                )
            }
        }
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
