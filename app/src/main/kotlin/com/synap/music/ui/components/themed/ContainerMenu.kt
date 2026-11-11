package com.synap.music.ui.components.themed

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import com.synap.music.ui.styling.rememberButtonColor
import com.synap.music.utils.bold
import com.synap.music.utils.secondary
import com.synap.music.utils.color
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synap.music.Database
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.MainApplication
import com.synap.music.R
import com.synap.music.enums.BuiltInPlaylist
import com.synap.music.enums.CubeCompressionLevel
import com.synap.music.models.Album
import com.synap.music.models.Artist
import com.synap.music.models.Playlist
import com.synap.music.models.SongPlaylistMap
import com.synap.music.query
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.px
import com.synap.music.utils.asMediaItem
import com.synap.music.utils.bold
import com.synap.music.utils.completed
import com.synap.music.utils.enqueue
import com.synap.music.utils.forcePlayFromBeginning
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import com.synap.music.utils.thumbnail
import com.synap.music.utils.toast
import com.synap.pulse.SynapService
import com.synap.pulse.models.bodies.BrowseBody
import com.synap.pulse.requests.albumPage
import com.synap.pulse.requests.artistPage
import com.synap.pulse.requests.playlistPage
import com.synap.music.utils.MediaFetchUtils
import com.synap.music.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@Composable
fun ContainerMenu(
    onDismiss: () -> Unit,
    title: String?,
    subtitle: String?,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val (colorPalette) = LocalAppearance.current

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .background(colorPalette.background1, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .padding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues())
                .padding(top = 16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                val thumbnailSizeDp = 48.dp
                val thumbnailSizePx = thumbnailSizeDp.px

                AsyncImage(
                    model = thumbnailUrl.thumbnail(thumbnailSizePx),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .clip(LocalAppearance.current.thumbnailShape)
                        .size(thumbnailSizeDp)
                )

                Column(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f)
                ) {
                    BasicText(
                        text = title ?: "",
                        style = LocalAppearance.current.typography.s.bold.copy(color = colorPalette.text),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    BasicText(
                        text = subtitle ?: "",
                        style = LocalAppearance.current.typography.xs.semiBold.secondary.copy(color = colorPalette.textSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(content = content)
        }
    }
}

@ExperimentalAnimationApi
@Composable
fun InLibraryAlbumMenu(
    album: Album,
    onDismiss: () -> Unit,
    onMultipleDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val coroutineScope = rememberCoroutineScope()
    var isViewingPlaylists by remember { mutableStateOf(false) }

    ContainerMenu(
        onDismiss = onDismiss,
        title = album.title,
        subtitle = album.authorsText,
        thumbnailUrl = album.thumbnailUrl
    ) {
        AnimatedVisibility(
            visible = !isViewingPlaylists,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                MenuEntry(
                    icon = R.drawable.play,
                    text = "Play",
                    onClick = {
                        onDismiss()
                        coroutineScope.launch {
                            val songs = Database.albumSongs(album.id).firstOrNull() ?: emptyList()
                            if (songs.isNotEmpty()) {
                                binder?.stopRadio()
                                binder?.player?.forcePlayFromBeginning(songs.map { it.asMediaItem(album.id, album.title, album.thumbnailUrl) })
                            }
                        }
                    }
                )

                MenuEntry(
                    icon = R.drawable.shuffle,
                    text = "Shuffle",
                    onClick = {
                        onDismiss()
                        coroutineScope.launch {
                            val songs = Database.albumSongs(album.id).firstOrNull() ?: emptyList()
                            if (songs.isNotEmpty()) {
                                binder?.stopRadio()
                                binder?.player?.forcePlayFromBeginning(songs.map { it.asMediaItem(album.id, album.title, album.thumbnailUrl) }.shuffled())
                            }
                        }
                    }
                )

                MenuEntry(
                    icon = R.drawable.airplane,
                    text = "Download all",
                    onClick = {
                        onDismiss()
                        context.toast("Starting download for ${album.title}")
                        coroutineScope.launch {
                            val songs = Database.albumSongs(album.id).firstOrNull() ?: emptyList()
                            songs.forEach { song ->
                                MainApplication.scope.launch(Dispatchers.IO) {
                                    MediaFetchUtils.downloadMedia(context, song.asMediaItem(album.id, album.title, album.thumbnailUrl), false)
                                }
                            }
                        }
                    }
                )

                MenuEntry(
                    icon = R.drawable.add,
                    text = "Add all to playlist",
                    onClick = { isViewingPlaylists = true }
                )

                onMultipleDelete?.let {
                    MenuEntry(
                        icon = R.drawable.reorder,
                        text = "Select albums",
                        onClick = {
                            onDismiss()
                            it()
                        }
                    )
                }
                
                MenuEntry(
                    icon = R.drawable.trash,
                    text = "Delete from library",
                    onClick = {
                        onDismiss()
                        coroutineScope.launch {
                            val songs = Database.albumSongs(album.id).firstOrNull() ?: emptyList()
                            query {
                                songs.forEach { song ->
                                    val file = MediaFetchUtils.getAudioFile(context, song.id)
                                    if (file.exists()) file.delete()
                                    Database.deleteFormat(song.id)
                                }
                                Database.delete(album)
                            }
                        }
                        context.toast("Album and downloads removed")
                    }
                )

                MenuEntry(
                    icon = R.drawable.share_social,
                    text = "Share",
                    onClick = {
                        onDismiss()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/browse/${album.id}")
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    }
                )
            }
        }

        PlaylistPicker(
            visible = isViewingPlaylists,
            onBack = { isViewingPlaylists = false },
            onPlaylistSelected = { playlist, count ->
                onDismiss()
                coroutineScope.launch {
                    val songs = Database.albumSongs(album.id).firstOrNull() ?: emptyList()
                    query {
                        songs.forEachIndexed { index, song ->
                            Database.insert(song.asMediaItem)
                            Database.insert(
                                SongPlaylistMap(
                                    songId = song.id,
                                    playlistId = playlist.id,
                                    position = count + index
                                )
                            )
                        }
                    }
                    context.toast("Added ${songs.size} songs to ${playlist.name}")
                }
            }
        )
    }
}

@ExperimentalAnimationApi
/**
 * Long-press context menu for user-created playlists in the Library section.
 * Supports Play, Shuffle, Metadata Refresh, Bulk Downloads, Audio Cube Export, Rename, Multi-Select, and Deletion.
 */
@Composable
fun InLibraryPlaylistMenu(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onRename: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
    onDownloadAll: (() -> Unit)? = null,
    onRefreshMetadata: (() -> Unit)? = null,
    onMultipleDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val coroutineScope = rememberCoroutineScope()

    ContainerMenu(
        onDismiss = onDismiss,
        title = playlist.name,
        subtitle = null,
        thumbnailUrl = null // Local playlists don't have a single URL usually
    ) {
        Column {
            MenuEntry(
                icon = R.drawable.play,
                text = "Play",
                onClick = {
                    onDismiss()
                    coroutineScope.launch {
                        val playlistWithSongs = Database.playlistWithSongs(playlist.id).firstOrNull()
                        val songs = playlistWithSongs?.songs ?: emptyList()
                        if (songs.isNotEmpty()) {
                            binder?.stopRadio()
                            binder?.player?.forcePlayFromBeginning(songs.map { it.asMediaItem })
                        }
                    }
                }
            )

            MenuEntry(
                icon = R.drawable.shuffle,
                text = "Shuffle",
                onClick = {
                    onDismiss()
                    coroutineScope.launch {
                        val playlistWithSongs = Database.playlistWithSongs(playlist.id).firstOrNull()
                        val songs = playlistWithSongs?.songs ?: emptyList()
                        if (songs.isNotEmpty()) {
                            binder?.stopRadio()
                            binder?.player?.forcePlayFromBeginning(songs.map { it.asMediaItem }.shuffled())
                        }
                    }
                }
            )

            onRefreshMetadata?.let {
                MenuEntry(
                    icon = R.drawable.sync,
                    text = "Refresh Metadata",
                    onClick = {
                        onDismiss()
                        it()
                    }
                )
            }

            onDownloadAll?.let {
                MenuEntry(
                    icon = R.drawable.airplane,
                    text = "Download All",
                    onClick = {
                        onDismiss()
                        it()
                    }
                )
            }

            onExport?.let {
                MenuEntry(
                    icon = R.drawable.share_social,
                    text = "Export Audio Cube",
                    onClick = {
                        onDismiss()
                        it()
                    }
                )
            }

            onRename?.let {
                MenuEntry(
                    icon = R.drawable.pencil,
                    text = "Rename",
                    onClick = {
                        onDismiss()
                        it()
                    }
                )
            }

            onMultipleDelete?.let {
                MenuEntry(
                    icon = R.drawable.reorder,
                    text = "Select playlists",
                    onClick = {
                        onDismiss()
                        it()
                    }
                )
            }

            MenuEntry(
                icon = R.drawable.trash,
                text = "Delete playlist",
                onClick = {
                    onDismiss()
                    query {
                        Database.delete(playlist)
                    }
                    context.toast("Playlist deleted")
                }
            )
        }
    }
}

/**
 * Long-press context menu for system built-in collections ("Star Collection" & "Vault").
 */
@ExperimentalAnimationApi
@Composable
fun BuiltInPlaylistMenu(
    playlistType: BuiltInPlaylist,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onDownloadAll: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
) {
    ContainerMenu(
        onDismiss = onDismiss,
        title = if (playlistType == BuiltInPlaylist.Favorites) "Star Collection" else "Vault",
        subtitle = null,
        thumbnailUrl = null
    ) {
        Column {
            MenuEntry(
                icon = R.drawable.play,
                text = "Play",
                onClick = {
                    onDismiss()
                    onPlay()
                }
            )

            MenuEntry(
                icon = R.drawable.shuffle,
                text = "Shuffle",
                onClick = {
                    onDismiss()
                    onShuffle()
                }
            )

            onDownloadAll?.let {
                MenuEntry(
                    icon = R.drawable.airplane,
                    text = "Download All",
                    onClick = {
                        onDismiss()
                        it()
                    }
                )
            }

            onExport?.let {
                MenuEntry(
                    icon = R.drawable.share_social,
                    text = "Export Audio Cube",
                    onClick = {
                        onDismiss()
                        it()
                    }
                )
            }
        }
    }
}

/**
 * Title badge button shown during multi-selection mode.
 */
@Composable
fun SynapCubeTitleButton(
    selectedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current
    val buttonColor = rememberButtonColor()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(buttonColor.copy(alpha = 0.15f))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(CircleShape)
                .background(buttonColor)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            BasicText(
                text = selectedCount.toString(),
                style = typography.xxs.bold.copy(color = colorPalette.onAccent)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Image(
            painter = painterResource(R.drawable.share_social),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorPalette.accent),
            modifier = Modifier.size(13.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        BasicText(
            text = "SynapCube",
            style = typography.xxs.semiBold.copy(color = colorPalette.accent)
        )

        Image(
            painter = painterResource(R.drawable.chevron_forward),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorPalette.accent),
            modifier = Modifier
                .padding(start = 2.dp)
                .size(12.dp)
        )
    }
}

/**
 * A bottom sheet context menu dedicated specifically to `.synapcube` exports.
 * Provides instant export triggers and on-the-fly Zstd/Zip compression level selection.
 */
@ExperimentalAnimationApi
@Composable
fun SynapCubeExportMenu(
    selectedCount: Int,
    compressionLevel: CubeCompressionLevel,
    onCompressionLevelChanged: (CubeCompressionLevel) -> Unit,
    onExport: () -> Unit,
    onDismiss: () -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current

    ContainerMenu(
        onDismiss = onDismiss,
        title = "SynapCube Export Options",
        subtitle = "$selectedCount items ready for packaging",
        thumbnailUrl = null
    ) {
        Column {
            MenuEntry(
                icon = R.drawable.share_social,
                text = "Export .synapcube archive",
                onClick = {
                    onDismiss()
                    onExport()
                }
            )

            BasicText(
                text = "COMPRESSION PROFILE",
                style = typography.xs.semiBold.secondary.copy(color = colorPalette.textSecondary),
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            CubeCompressionLevel.values().forEach { level ->
                val isSelected = level == compressionLevel
                MenuEntry(
                    icon = if (isSelected) R.drawable.checkmark else R.drawable.disc,
                    text = level.name + if (level == CubeCompressionLevel.Auto) " (Auto - Recommended)" else "",
                    onClick = {
                        onCompressionLevelChanged(level)
                    }
                )
            }
        }
    }
}

@ExperimentalAnimationApi
@Composable
fun InLibraryArtistMenu(
    artist: Artist,
    onDismiss: () -> Unit,
    onMultipleDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val coroutineScope = rememberCoroutineScope()

    ContainerMenu(
        onDismiss = onDismiss,
        title = artist.name,
        subtitle = null,
        thumbnailUrl = artist.thumbnailUrl
    ) {
        MenuEntry(
            icon = R.drawable.shuffle,
            text = "Shuffle top songs",
            onClick = {
                onDismiss()
                coroutineScope.launch {
                    val songs = Database.artistSongs(artist.id).firstOrNull() ?: emptyList()
                    if (songs.isNotEmpty()) {
                        binder?.stopRadio()
                        binder?.player?.forcePlayFromBeginning(songs.map { it.asMediaItem }.shuffled())
                    }
                }
            }
        )

        MenuEntry(
            icon = R.drawable.airplane,
            text = "Download top songs",
            onClick = {
                onDismiss()
                context.toast("Starting download for top songs of ${artist.name}")
                coroutineScope.launch {
                    val songs = Database.artistSongs(artist.id).firstOrNull() ?: emptyList()
                    songs.forEach { song ->
                        MainApplication.scope.launch(Dispatchers.IO) {
                            MediaFetchUtils.downloadMedia(context, song.asMediaItem, false)
                        }
                    }
                }
            }
        )

        onMultipleDelete?.let {
            MenuEntry(
                icon = R.drawable.reorder,
                text = "Select artists",
                onClick = {
                    onDismiss()
                    it()
                }
            )
        }
        
        MenuEntry(
            icon = R.drawable.trash,
            text = "Delete from library",
            onClick = {
                onDismiss()
                coroutineScope.launch {
                    val songs = Database.artistSongs(artist.id).firstOrNull() ?: emptyList()
                    query {
                        songs.forEach { song ->
                            val file = MediaFetchUtils.getAudioFile(context, song.id)
                            if (file.exists()) file.delete()
                            Database.deleteFormat(song.id)
                        }
                        Database.delete(artist)
                    }
                }
                context.toast("Artist and downloads removed")
            }
        )

        MenuEntry(
            icon = R.drawable.share_social,
            text = "Share",
            onClick = {
                onDismiss()
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/channel/${artist.id}")
                }
                context.startActivity(Intent.createChooser(sendIntent, null))
            }
        )
    }
}

@ExperimentalAnimationApi
@Composable
fun AlbumMenu(
    album: SynapService.AlbumItem,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val coroutineScope = rememberCoroutineScope()
    var isViewingPlaylists by remember { mutableStateOf(false) }

    ContainerMenu(
        onDismiss = onDismiss,
        title = album.info?.name,
        subtitle = album.authors?.joinToString(", ") { it.name ?: "" },
        thumbnailUrl = album.thumbnail?.url
    ) {
        AnimatedVisibility(
            visible = !isViewingPlaylists,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                MenuEntry(
                    icon = R.drawable.play,
                    text = "Play",
                    onClick = {
                        onDismiss()
                        coroutineScope.launch {
                            val page = SynapService.albumPage(BrowseBody(browseId = album.key))?.completed()?.getOrNull()
                            val songs = page?.songsPage?.list ?: emptyList()
                            if (songs.isNotEmpty()) {
                                binder?.stopRadio()
                                binder?.player?.forcePlayFromBeginning(songs.map { it.asMediaItem })
                            }
                        }
                    }
                )

                MenuEntry(
                    icon = R.drawable.shuffle,
                    text = "Shuffle",
                    onClick = {
                        onDismiss()
                        coroutineScope.launch {
                            val page = SynapService.albumPage(BrowseBody(browseId = album.key))?.completed()?.getOrNull()
                            val songs = page?.songsPage?.list ?: emptyList()
                            if (songs.isNotEmpty()) {
                                binder?.stopRadio()
                                binder?.player?.forcePlayFromBeginning(songs.map { it.asMediaItem }.shuffled())
                            }
                        }
                    }
                )

                MenuEntry(
                    icon = R.drawable.play_skip_forward,
                    text = "Play next",
                    onClick = {
                        onDismiss()
                        coroutineScope.launch {
                            val page = SynapService.albumPage(BrowseBody(browseId = album.key))?.completed()?.getOrNull()
                            val songs = page?.songsPage?.list ?: emptyList()
                            if (songs.isNotEmpty()) {
                                songs.asReversed().forEach { binder?.player?.addMediaItem(binder.player.currentMediaItemIndex + 1, it.asMediaItem) }
                            }
                        }
                    }
                )

                MenuEntry(
                    icon = R.drawable.add,
                    text = "Add to queue",
                    onClick = {
                        onDismiss()
                        coroutineScope.launch {
                            val page = SynapService.albumPage(BrowseBody(browseId = album.key))?.completed()?.getOrNull()
                            val songs = page?.songsPage?.list ?: emptyList()
                            if (songs.isNotEmpty()) {
                                binder?.player?.enqueue(songs.map { it.asMediaItem })
                            }
                        }
                    }
                )

                MenuEntry(
                    icon = R.drawable.airplane,
                    text = "Save all to vault",
                    onClick = {
                        onDismiss()
                        context.toast("Starting download for ${album.info?.name}")
                        coroutineScope.launch {
                            val page = SynapService.albumPage(BrowseBody(browseId = album.key))?.completed()?.getOrNull()
                            val songs = page?.songsPage?.list ?: emptyList()
                            songs.forEach { song ->
                                MainApplication.scope.launch(Dispatchers.IO) {
                                    MediaFetchUtils.downloadMedia(context, song.asMediaItem(album.key, album.info?.name, album.thumbnail?.url), false)
                                }
                            }
                        }
                    }
                )

                MenuEntry(
                    icon = R.drawable.add,
                    text = "Add all to playlist",
                    onClick = { isViewingPlaylists = true }
                )

                MenuEntry(
                    icon = R.drawable.heart_outline,
                    text = "Bookmark album",
                    onClick = {
                        onDismiss()
                        query {
                            Database.upsert(
                                Album(
                                    id = album.key,
                                    title = album.info?.name,
                                    thumbnailUrl = album.thumbnail?.url,
                                    authorsText = album.authors?.joinToString(", ") { it.name ?: "" },
                                    bookmarkedAt = System.currentTimeMillis()
                                ),
                                emptyList()
                            )
                        }
                        context.toast("Album bookmarked")
                    }
                )

                MenuEntry(
                    icon = R.drawable.share_social,
                    text = "Share",
                    onClick = {
                        onDismiss()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/browse/${album.key}")
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    }
                )
            }
        }

        PlaylistPicker(
            visible = isViewingPlaylists,
            onBack = { isViewingPlaylists = false },
            onPlaylistSelected = { playlist, count ->
                onDismiss()
                coroutineScope.launch {
                    val page = SynapService.albumPage(BrowseBody(browseId = album.key))?.completed()?.getOrNull()
                    val songs = page?.songsPage?.list ?: emptyList()
                    query {
                        songs.forEachIndexed { index, song ->
                            Database.insert(song.asMediaItem)
                            Database.insert(
                                SongPlaylistMap(
                                    songId = song.key,
                                    playlistId = playlist.id,
                                    position = count + index
                                )
                            )
                        }
                    }
                    context.toast("Added ${songs.size} songs to ${playlist.name}")
                }
            }
        )
    }
}

@ExperimentalAnimationApi
@Composable
fun ArtistMenu(
    artist: SynapService.ArtistItem,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val coroutineScope = rememberCoroutineScope()

    ContainerMenu(
        onDismiss = onDismiss,
        title = artist.info?.name,
        subtitle = artist.subscribersCountText,
        thumbnailUrl = artist.thumbnail?.url
    ) {
        MenuEntry(
            icon = R.drawable.shuffle,
            text = "Shuffle all",
            onClick = {
                onDismiss()
                coroutineScope.launch {
                    val page = SynapService.artistPage(BrowseBody(browseId = artist.key))?.getOrNull()
                    page?.shuffleEndpoint?.let { endpoint ->
                        binder?.stopRadio()
                        binder?.playRadio(endpoint)
                    } ?: run {
                        val songs = page?.songs ?: emptyList()
                        if (songs.isNotEmpty()) {
                            binder?.stopRadio()
                            binder?.player?.forcePlayFromBeginning(songs.map { it.asMediaItem }.shuffled())
                        }
                    }
                }
            }
        )

        MenuEntry(
            icon = R.drawable.radio,
            text = "Start radio",
            onClick = {
                onDismiss()
                coroutineScope.launch {
                    val page = SynapService.artistPage(BrowseBody(browseId = artist.key))?.getOrNull()
                    page?.radioEndpoint?.let { endpoint ->
                        binder?.stopRadio()
                        binder?.playRadio(endpoint)
                    }
                }
            }
        )

        MenuEntry(
            icon = R.drawable.airplane,
            text = "Save top songs to vault",
            onClick = {
                onDismiss()
                context.toast("Starting download for top songs of ${artist.info?.name}")
                coroutineScope.launch {
                    val page = SynapService.artistPage(BrowseBody(browseId = artist.key))?.getOrNull()
                    val songs = page?.songs ?: emptyList()
                    songs.forEach { song ->
                        MainApplication.scope.launch(Dispatchers.IO) {
                            MediaFetchUtils.downloadMedia(context, song.asMediaItem, false)
                        }
                    }
                }
            }
        )

        MenuEntry(
            icon = R.drawable.heart_outline,
            text = "Bookmark artist",
            onClick = {
                onDismiss()
                query {
                    Database.upsert(
                        Artist(
                            id = artist.key,
                            name = artist.info?.name,
                            thumbnailUrl = artist.thumbnail?.url,
                            bookmarkedAt = System.currentTimeMillis()
                        )
                    )
                }
                context.toast("Artist bookmarked")
            }
        )

        MenuEntry(
            icon = R.drawable.share_social,
            text = "Share",
            onClick = {
                onDismiss()
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/channel/${artist.key}")
                }
                context.startActivity(Intent.createChooser(sendIntent, null))
            }
        )
    }
}

@ExperimentalAnimationApi
@Composable
fun PlaylistMenu(
    playlist: SynapService.PlaylistItem,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val coroutineScope = rememberCoroutineScope()
    var isViewingPlaylists by remember { mutableStateOf(false) }

    ContainerMenu(
        onDismiss = onDismiss,
        title = playlist.info?.name,
        subtitle = playlist.channel?.name,
        thumbnailUrl = playlist.thumbnail?.url
    ) {
        AnimatedVisibility(
            visible = !isViewingPlaylists,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                MenuEntry(
                    icon = R.drawable.play,
                    text = "Play",
                    onClick = {
                        onDismiss()
                        coroutineScope.launch {
                            val page = SynapService.playlistPage(BrowseBody(browseId = playlist.key))?.completed()?.getOrNull()
                            val songs = page?.songsPage?.list ?: emptyList()
                            if (songs.isNotEmpty()) {
                                binder?.stopRadio()
                                binder?.player?.forcePlayFromBeginning(songs.map { it.asMediaItem })
                            }
                        }
                    }
                )

                MenuEntry(
                    icon = R.drawable.shuffle,
                    text = "Shuffle",
                    onClick = {
                        onDismiss()
                        coroutineScope.launch {
                            val page = SynapService.playlistPage(BrowseBody(browseId = playlist.key))?.completed()?.getOrNull()
                            val songs = page?.songsPage?.list ?: emptyList()
                            if (songs.isNotEmpty()) {
                                binder?.stopRadio()
                                binder?.player?.forcePlayFromBeginning(songs.map { it.asMediaItem }.shuffled())
                            }
                        }
                    }
                )

                MenuEntry(
                    icon = R.drawable.play_skip_forward,
                    text = "Play next",
                    onClick = {
                        onDismiss()
                        coroutineScope.launch {
                            val page = SynapService.playlistPage(BrowseBody(browseId = playlist.key))?.completed()?.getOrNull()
                            val songs = page?.songsPage?.list ?: emptyList()
                            if (songs.isNotEmpty()) {
                                songs.asReversed().forEach { binder?.player?.addMediaItem(binder.player.currentMediaItemIndex + 1, it.asMediaItem) }
                            }
                        }
                    }
                )

                MenuEntry(
                    icon = R.drawable.add,
                    text = "Add to queue",
                    onClick = {
                        onDismiss()
                        coroutineScope.launch {
                            val page = SynapService.playlistPage(BrowseBody(browseId = playlist.key))?.completed()?.getOrNull()
                            val songs = page?.songsPage?.list ?: emptyList()
                            if (songs.isNotEmpty()) {
                                binder?.player?.enqueue(songs.map { it.asMediaItem })
                            }
                        }
                    }
                )

                MenuEntry(
                    icon = R.drawable.airplane,
                    text = "Save all to vault",
                    onClick = {
                        onDismiss()
                        context.toast("Starting download for ${playlist.info?.name}")
                        coroutineScope.launch {
                            val page = SynapService.playlistPage(BrowseBody(browseId = playlist.key))?.completed()?.getOrNull()
                            val songs = page?.songsPage?.list ?: emptyList()
                            songs.forEach { song ->
                                MainApplication.scope.launch(Dispatchers.IO) {
                                    MediaFetchUtils.downloadMedia(context, song.asMediaItem, false)
                                }
                            }
                        }
                    }
                )

                MenuEntry(
                    icon = R.drawable.add,
                    text = "Copy to playlist",
                    onClick = { isViewingPlaylists = true }
                )

                MenuEntry(
                    icon = R.drawable.share_social,
                    text = "Share",
                    onClick = {
                        onDismiss()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/playlist?list=${playlist.key.removePrefix("VL")}")
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    }
                )
            }
        }

        PlaylistPicker(
            visible = isViewingPlaylists,
            onBack = { isViewingPlaylists = false },
            onPlaylistSelected = { targetPlaylist, count ->
                onDismiss()
                coroutineScope.launch {
                    val page = SynapService.playlistPage(BrowseBody(browseId = playlist.key))?.completed()?.getOrNull()
                    val songs = page?.songsPage?.list ?: emptyList()
                    query {
                        songs.forEachIndexed { index, song ->
                            Database.insert(song.asMediaItem)
                            Database.insert(
                                SongPlaylistMap(
                                    songId = song.key,
                                    playlistId = targetPlaylist.id,
                                    position = count + index
                                )
                            )
                        }
                    }
                    context.toast("Copied ${songs.size} songs to ${targetPlaylist.name}")
                }
            }
        )
    }
}

@Composable
private fun PlaylistPicker(
    visible: Boolean,
    onBack: () -> Unit,
    onPlaylistSelected: (Playlist, Int) -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val playlists by Database.playlistPreviews(com.synap.music.enums.PlaylistSortBy.Name, com.synap.music.enums.SortOrder.Ascending).collectAsState(initial = emptyList())
    val lazyListState = rememberLazyListState()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .heightIn(min = 200.dp, max = 500.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(R.drawable.chevron_back),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette.text),
                    modifier = Modifier.size(20.dp)
                )

                BasicText(
                    text = "Back",
                    style = LocalAppearance.current.typography.s.semiBold.copy(color = colorPalette.text),
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                itemsIndexed(items = playlists) { _, playlist ->
                    MenuEntry(
                        icon = R.drawable.playlist,
                        text = playlist.playlist.name,
                        onClick = {
                            onPlaylistSelected(playlist.playlist, playlist.songCount)
                        }
                    )
                }
            }
        }
    }
}
