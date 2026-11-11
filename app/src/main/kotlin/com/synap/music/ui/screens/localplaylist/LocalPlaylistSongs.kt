package com.synap.music.ui.screens.localplaylist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.synap.compose.persist.persist
import com.synap.pulse.SynapService
import com.synap.pulse.models.bodies.NextBody
import com.synap.pulse.models.bodies.BrowseBody
import com.synap.pulse.requests.playlistPage
import com.synap.compose.reordering.ReorderingLazyColumn
import com.synap.compose.reordering.animateItemPlacement
import com.synap.compose.reordering.draggedItem
import com.synap.compose.reordering.rememberReorderingState
import com.synap.compose.reordering.reorder
import com.synap.music.Database
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.MainApplication
import com.synap.music.R
import com.synap.music.models.PlaylistWithSongs
import com.synap.music.models.Song
import com.synap.music.models.SongPlaylistMap
import com.synap.music.query
import com.synap.music.transaction
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.ui.components.themed.ConfirmationDialog
import com.synap.music.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.synap.music.ui.components.themed.HeaderIconButton
import com.synap.music.ui.components.themed.IconButton
import com.synap.music.ui.components.themed.InPlaylistMediaItemMenu
import com.synap.music.ui.components.themed.Menu
import com.synap.music.ui.components.themed.MenuEntry
import com.synap.music.ui.components.themed.TextFieldDialog
import com.synap.music.ui.items.SongItem
import com.synap.music.ui.styling.Dimensions
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.px
import com.synap.music.utils.MediaFetchUtils
import com.synap.music.utils.asMediaItem
import com.synap.music.utils.completed
import com.synap.music.utils.forcePlayAtIndex
import com.synap.music.utils.forcePlayFromBeginning
import com.synap.music.utils.toast
import com.synap.music.enums.CubeCompressionLevel
import com.synap.music.utils.cubeCompressionLevelKey
import com.synap.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.synap.music.cubes.AudioCubeManager

/**
 * The Playlist Details Screen.
 * This screen handles local playlist management, including song reordering, 
 * renaming, and bulk downloading.
 */
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun LocalPlaylistSongs(
    playlistId: Long,
    onDelete: () -> Unit,
    selection: MutableList<String> = remember { mutableStateListOf() },
    onIdsChanged: (List<String>) -> Unit = {}
) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val context = LocalContext.current

    var playlistWithSongs by persist<PlaylistWithSongs?>("localPlaylist/$playlistId/playlistWithSongs")

    // Keep the list in sync with the database in real-time.
    LaunchedEffect(Unit) {
        Database.playlistWithSongs(playlistId).filterNotNull().collect { playlistWithSongs = it }
    }

    LaunchedEffect(playlistWithSongs) {
        onIdsChanged(playlistWithSongs?.songs?.map { it.id } ?: emptyList())
    }

    val lazyListState = rememberLazyListState()

    // exportLauncher removed as it has been moved to Library Long Press Menu

    /**
     * Drag-and-Drop Reordering logic.
     * When you stop dragging a song, we update the "position" field 
     * for all songs in this playlist in the database.
     */
    val reorderingState = rememberReorderingState(
        lazyListState = lazyListState,
        key = playlistWithSongs?.songs ?: emptyList<Any>(),
        onDragEnd = { fromIndex, toIndex ->
            query {
                Database.move(playlistId, fromIndex, toIndex)
            }
        },
        extraItemCount = 1
    )

    var isRenaming by rememberSaveable {
        mutableStateOf(false)
    }

    if (isRenaming) {
        TextFieldDialog(
            hintText = "Enter the playlist name",
            initialTextInput = playlistWithSongs?.playlist?.name ?: "",
            onDismiss = { isRenaming = false },
            onDone = { text ->
                query {
                    playlistWithSongs?.playlist?.copy(name = text)?.let(Database::update)
                }
            }
        )
    }

    var isDeleting by rememberSaveable {
        mutableStateOf(false)
    }

    if (isDeleting) {
        ConfirmationDialog(
            text = "Do you really want to delete this playlist?",
            onDismiss = { isDeleting = false },
            onConfirm = {
                query {
                    playlistWithSongs?.playlist?.let(Database::delete)
                }
                onDelete()
            }
        )
    }

    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSizePx = thumbnailSizeDp.px

    val rippleIndication = rememberRipple(bounded = false)

    val layoutDirection = LocalLayoutDirection.current
    val paddingValues = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    val contentPadding = PaddingValues(
        start = paddingValues.calculateStartPadding(layoutDirection),
        top = paddingValues.calculateTopPadding() + 64.dp,
        end = paddingValues.calculateEndPadding(layoutDirection),
        bottom = paddingValues.calculateBottomPadding()
    )

    Box {
        ReorderingLazyColumn(
            reorderingState = reorderingState,
            contentPadding = contentPadding,
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            itemsIndexed(
                items = playlistWithSongs?.songs ?: emptyList(),
                key = { _, song -> song.id },
                contentType = { _, song -> song },
            ) { index, song ->
                SongItem(
                    song = song,
                    thumbnailSizePx = thumbnailSizePx,
                    thumbnailSizeDp = thumbnailSizeDp,
                    trailingContent = {
                        IconButton(
                            icon = R.drawable.reorder,
                            color = colorPalette.textDisabled,
                            indication = rippleIndication,
                            onClick = {},
                            modifier = Modifier
                                .reorder(reorderingState = reorderingState, index = index)
                                .size(18.dp)
                        )
                    },
                    modifier = Modifier
                        .combinedClickable(
                            onLongClick = {
                                if (selection.isNotEmpty()) {
                                    val isSelected = selection.contains(song.id)
                                    if (isSelected) selection.remove(song.id) else selection.add(song.id)
                                } else {
                                    menuState.display {
                                        InPlaylistMediaItemMenu(
                                            playlistId = playlistId,
                                            positionInPlaylist = index,
                                            song = song,
                                            onDismiss = menuState::hide,
                                            onMultipleDelete = { selection.add(song.id) }
                                        )
                                    }
                                }
                            },
                            onClick = {
                                if (selection.isNotEmpty()) {
                                    val isSelected = selection.contains(song.id)
                                    if (isSelected) selection.remove(song.id) else selection.add(song.id)
                                } else {
                                    playlistWithSongs?.songs
                                        ?.map(Song::asMediaItem)
                                        ?.let { mediaItems ->
                                            binder?.stopRadio()
                                            binder?.player?.forcePlayAtIndex(mediaItems, index)
                                        }
                                }
                            }
                        )
                        .animateItemPlacement(scope = this, reorderingState = reorderingState)
                        .draggedItem(reorderingState = reorderingState, index = index)
                )
            }
        }

        FloatingActionsContainerWithScrollToTop(
            lazyListState = lazyListState,
            iconId = R.drawable.shuffle,
            secondaryIconId = R.drawable.airplane,
            visible = !reorderingState.isDragging,
            onClick = {
                playlistWithSongs?.songs?.let { songs ->
                    if (songs.isNotEmpty()) {
                        binder?.stopRadio()
                        binder?.player?.forcePlayFromBeginning(
                            songs.shuffled().map(Song::asMediaItem)
                        )
                    }
                }
            },
            onSecondaryClick = {
                playlistWithSongs?.songs?.let { songs ->
                    if (songs.isNotEmpty()) {
                        context.toast("Starting download for ${songs.size} songs")
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
