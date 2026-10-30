package com.synap.music.ui.screens.playlist

import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.synap.compose.persist.PersistMapCleanup
import com.synap.compose.persist.persist
import com.synap.music.Database
import com.synap.music.R
import com.synap.music.models.Playlist
import com.synap.music.models.SongPlaylistMap
import com.synap.music.query
import com.synap.music.transaction
import com.synap.music.ui.components.themed.HeaderIconButton
import com.synap.music.ui.components.themed.Scaffold
import com.synap.music.ui.components.themed.TextFieldDialog
import com.synap.music.utils.MediaFetchUtils
import com.synap.music.MainApplication
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.utils.asMediaItem
import com.synap.music.utils.bold
import com.synap.music.utils.toast
import com.synap.pulse.SynapService
import com.synap.compose.routing.RouteHandlerScope
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun RouteHandlerScope.PlaylistScreen(browseId: String) {
    val saveableStateHolder = rememberSaveableStateHolder()
    PersistMapCleanup(tagPrefix = "playlist/$browseId")

    var playlistPage by persist<SynapService.PlaylistOrAlbumPage?>("playlist/$browseId/playlistPage")

    var isImportingPlaylist by rememberSaveable {
        mutableStateOf(value = false)
    }

    if (isImportingPlaylist) {
        TextFieldDialog(
            hintText = "Enter the playlist name",
            initialTextInput = playlistPage?.title ?: "",
            onDismiss = {
                if (isImportingPlaylist) {
                    isImportingPlaylist = false
                }
            },
            onDone = { text ->
                if (isImportingPlaylist) {
                    isImportingPlaylist = false
                    query {
                        transaction {
                            val playlistId = Database.insert(Playlist(name = text, browseId = browseId))

                            playlistPage?.songsPage?.list
                                ?.asSequence()
                                ?.map(SynapService.SongItem::asMediaItem)
                                ?.onEach(Database::insert)
                                ?.mapIndexed { index, mediaItem ->
                                    SongPlaylistMap(
                                        songId = mediaItem.mediaId,
                                        playlistId = playlistId,
                                        position = index,
                                    )
                                }
                                ?.toList()
                                ?.let(Database::insertSongPlaylistMaps)
                        }
                    }
                }
            },
        )
    }

    val (colorPalette, typography) = LocalAppearance.current
    val context = LocalContext.current

    Scaffold(
        title = {
            BasicText(
                text = playlistPage?.title ?: "Playlist",
                style = typography.l.bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        topStartIconButtonId = R.drawable.chevron_back,
        onTopStartIconButtonClick = pop,
        topBarActions = {
            if (playlistPage != null) {
                HeaderIconButton(
                    icon = R.drawable.server,
                    color = colorPalette.accent,
                    onClick = {
                        context.toast("Adding ${playlistPage?.songsPage?.list?.size} songs to Vault...")
                        playlistPage?.songsPage?.list?.forEach { song ->
                            MainApplication.scope.launch {
                                MediaFetchUtils.downloadMedia(context, song.asMediaItem, isVideo = false)
                            }
                        }
                    },
                )

                HeaderIconButton(
                    icon = R.drawable.download,
                    color = colorPalette.text,
                    onClick = {
                        isImportingPlaylist = true
                    },
                )

                HeaderIconButton(
                    icon = R.drawable.share_social,
                    color = colorPalette.text,
                    onClick = {
                        val url = playlistPage?.url ?: "https://music.youtube.com/playlist?list=${browseId.removePrefix("VL")}"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, url)
                        }

                        context.startActivity(Intent.createChooser(sendIntent, null))
                    },
                )
            }
        },
        tabIndex = 0,
        onTabChanged = { },
        tabColumnContent = { Item ->
            Item(0, "Songs", R.drawable.musical_notes)
        }
    ) { currentTabIndex ->
        saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
            when (currentTabIndex) {
                0 -> PlaylistSongList(
                    browseId = browseId,
                    onPlaylistPageLoaded = { playlistPage = it }
                )
            }
        }
    }
}
