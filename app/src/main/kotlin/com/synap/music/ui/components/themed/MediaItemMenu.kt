package com.synap.music.ui.components.themed

import android.content.Intent
import android.content.ActivityNotFoundException
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synap.music.MainApplication
import androidx.media3.common.MediaItem
import coil.compose.AsyncImage
import com.synap.pulse.SynapService
import com.synap.pulse.models.NavigationEndpoint
import com.synap.music.Database
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.R
import com.synap.music.cubes.AudioCubeManager
import com.synap.music.enums.CubeCompressionLevel
import com.synap.music.models.Album
import com.synap.music.models.Playlist
import com.synap.music.models.Song
import com.synap.music.models.SongPlaylistMap
import com.synap.music.query
import com.synap.music.ui.screens.albumDetailsRoute
import com.synap.music.ui.screens.albumRoute
import com.synap.music.ui.screens.artistRoute
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.ui.styling.px
import com.synap.music.utils.addNext
import com.synap.music.utils.asMediaItem
import com.synap.music.utils.bold
import com.synap.music.utils.forcePlay
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import com.synap.music.utils.thumbnail
import com.synap.music.utils.DownloadState
import com.synap.music.utils.toast
import com.synap.music.utils.MediaFetchUtils
import com.synap.music.utils.cubeCompressionLevelKey
import com.synap.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@Composable
fun InHistoryMediaItemMenu(
    onDismiss: () -> Unit,
    song: Song,
    modifier: Modifier = Modifier,
    onMultipleDelete: (() -> Unit)? = null,
) {
    NonQueuedMediaItemMenu(
        onDismiss = onDismiss,
        mediaItem = song.asMediaItem,
        onDelete = {
            query {
                Database.clearEventsFor(song.id)
            }
        },
        onMultipleDelete = onMultipleDelete,
        modifier = modifier
    )
}

@ExperimentalAnimationApi
@Composable
fun InPlaylistMediaItemMenu(
    onDismiss: () -> Unit,
    playlistId: Long,
    positionInPlaylist: Int,
    song: Song,
    modifier: Modifier = Modifier,
    onMultipleDelete: (() -> Unit)? = null,
) {
    NonQueuedMediaItemMenu(
        onDismiss = onDismiss,
        mediaItem = song.asMediaItem,
        onRemoveFromPlaylist = {
            query {
                Database.delete(
                    SongPlaylistMap(
                        songId = song.id,
                        playlistId = playlistId,
                        position = positionInPlaylist,
                    )
                )
            }
        },
        onMultipleDelete = onMultipleDelete,
        modifier = modifier
    )
}

@ExperimentalAnimationApi
@Composable
fun NonQueuedMediaItemMenu(
    onDismiss: () -> Unit,
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onRemoveFromDiscover: (() -> Unit)? = null,
    showSaveToVault: Boolean = true,
    onMultipleDelete: (() -> Unit)? = null,
) {
    val binder = LocalPlayerServiceBinder.current

    BaseMediaItemMenu(
        mediaItem = mediaItem,
        onDismiss = onDismiss,
        onStartRadio = {
            binder?.stopRadio()
            binder?.player?.forcePlay(mediaItem)
            binder?.setupRadio(
                NavigationEndpoint.Endpoint.Watch(
                    videoId = mediaItem.mediaId,
                    playlistId = mediaItem.mediaMetadata.extras?.getString("playlistId")
                )
            )
        },
        onPlayNext = { binder?.player?.addNext(mediaItem) },
        onRemoveFromPlaylist = onRemoveFromPlaylist,
        onDelete = onDelete,
        onRemoveFromDiscover = onRemoveFromDiscover,
        showSaveToVault = showSaveToVault,
        onMultipleDelete = onMultipleDelete,
        modifier = modifier
    )
}

@ExperimentalAnimationApi
@Composable
fun QueuedMediaItemMenu(
    onDismiss: () -> Unit,
    mediaItem: MediaItem,
    indexInQueue: Int?,
    modifier: Modifier = Modifier,
) {
    val binder = LocalPlayerServiceBinder.current

    BaseMediaItemMenu(
        mediaItem = mediaItem,
        onDismiss = onDismiss,
        onRemoveFromQueue = {
            indexInQueue?.let {
                binder?.player?.removeMediaItem(it)
            }
        },
        modifier = modifier
    )
}

@ExperimentalAnimationApi
@Composable
fun BaseMediaItemMenu(
    onDismiss: () -> Unit,
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    onGoToEqualizer: (() -> Unit)? = null,
    onShowSleepTimer: (() -> Unit)? = null,
    onShowAudioInsights: (() -> Unit)? = null,
    onStartRadio: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onRemoveFromQueue: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onRemoveFromDiscover: (() -> Unit)? = null,
    showSaveToVault: Boolean = true,
    onMultipleDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current

    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    var isShowingAudioInsightsInternal by remember { mutableStateOf(false) }

    MediaItemMenu(
        mediaItem = mediaItem,
        onDismiss = onDismiss,
        onGoToEqualizer = onGoToEqualizer ?: {
            try {
                activityResultLauncher.launch(
                    Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, binder?.player?.audioSessionId)
                        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                    }
                )
            } catch (e: ActivityNotFoundException) {
                context.toast("Couldn't find an application to equalize audio")
            }
            Unit
        },
        onShowSleepTimer = onShowSleepTimer ?: { },
        onShowAudioInsights = onShowAudioInsights ?: { isShowingAudioInsightsInternal = true },
        onStartRadio = onStartRadio,
        onPlayNext = onPlayNext,
        onRemoveFromQueue = onRemoveFromQueue,
        onRemoveFromPlaylist = onRemoveFromPlaylist,
        onDelete = onDelete,
        onRemoveFromDiscover = onRemoveFromDiscover,
        showSaveToVault = showSaveToVault,
        onMultipleDelete = onMultipleDelete,
        onGoToAlbum = { album ->
            albumDetailsRoute.global(album.id, album)
        },
        onGoToArtist = { artistId ->
            artistRoute.global(artistId, null)
        },
        onShare = {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${mediaItem.mediaId}")
            }

            context.startActivity(Intent.createChooser(sendIntent, null))
        },
        modifier = modifier
    )

    if (isShowingAudioInsightsInternal) {
        com.synap.music.ui.screens.player.AudioInsights(
            mediaId = mediaItem.mediaId,
            isDisplayed = true,
            onDismiss = { isShowingAudioInsightsInternal = false }
        )
    }
}

/**
 * Interactive long-press context menu for tracks, albums, and playlists.
 * Renders navigation shortcuts, playlist options, vault downloads, share links,
 * and an accent-tinted visual scrollbar for easy vertical navigation.
 */
@ExperimentalAnimationApi
@Composable
fun MediaItemMenu(
    onDismiss: () -> Unit,
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    onGoToEqualizer: (() -> Unit)? = null,
    onShowSleepTimer: (() -> Unit)? = null,
    onShowAudioInsights: (() -> Unit)? = null,
    onStartRadio: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onRemoveFromQueue: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onAddToPlaylist: ((Playlist, Int) -> Unit)? = null,
    onGoToAlbum: ((Album) -> Unit)? = null,
    onGoToArtist: ((String) -> Unit)? = null,
    onRemoveFromDiscover: (() -> Unit)? = null,
    showSaveToVault: Boolean = true,
    onMultipleDelete: (() -> Unit)? = null,
    onShare: () -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val context = LocalContext.current

    var isCreatingPlaylist by remember {
        mutableStateOf(false)
    }

    if (isCreatingPlaylist) {
        TextFieldDialog(
            hintText = "Enter the playlist name",
            onDismiss = { isCreatingPlaylist = false },
            onDone = { text ->
                isCreatingPlaylist = false
                onDismiss()
                query {
                    val playlistId = Database.insert(Playlist(name = text))
                    Database.insert(mediaItem)
                    Database.insert(
                        SongPlaylistMap(
                            songId = mediaItem.mediaId,
                            playlistId = playlistId,
                            position = 0
                        )
                    )
                }
            }
        )
    }

    var isViewingPlaylists by remember {
        mutableStateOf(false)
    }

    var isViewingArtists by remember {
        mutableStateOf(false)
    }

    var isViewingMoreOptions by remember {
        mutableStateOf(false)
    }

    val cubeCompressionLevel by rememberPreference(cubeCompressionLevelKey, CubeCompressionLevel.Auto)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let {
            MainApplication.scope.launch {
                val result = AudioCubeManager.exportSongToCube(context, mediaItem.mediaId, uri, cubeCompressionLevel)
                if (result.isSuccess) {
                    context.toast("Audio Cube exported successfully!")
                } else {
                    context.toast("Failed to export Audio Cube")
                }
            }
        }
    }

    var isShowingSleepTimerDialog by remember {
        mutableStateOf(false)
    }

    var isEditingTags by remember {
        mutableStateOf(false)
    }

    if (isShowingSleepTimerDialog) {
        val binder = LocalPlayerServiceBinder.current
        SleepTimerDialog(
            onDismiss = { isShowingSleepTimerDialog = false },
            onDone = { delay, fadeOut -> binder?.startSleepTimer(delay, fadeOut) }
        )
    }

    if (isEditingTags) {
        TagEditorDialog(
            songId = mediaItem.mediaId,
            initialTitle = mediaItem.mediaMetadata.title?.toString() ?: "",
            initialArtist = mediaItem.mediaMetadata.artist?.toString() ?: "",
            initialArtworkUrl = mediaItem.mediaMetadata.artworkUri?.toString(),
            onDismiss = {
                isEditingTags = false
                onDismiss()
            }
        )
    }

    var albumInfo by remember(mediaItem.mediaId) { mutableStateOf<com.synap.music.models.Info?>(null) }
    var artistInfo by remember(mediaItem.mediaId) { mutableStateOf<List<com.synap.music.models.Info>>(emptyList()) }

    LaunchedEffect(mediaItem.mediaId) {
        query {
            albumInfo = Database.songAlbumInfo(mediaItem.mediaId)
            artistInfo = Database.songArtistInfo(mediaItem.mediaId)
        }
    }

    /**
     * Technical Metadata Recovery.
     * We look in the MediaItem extras first (most detailed), then the local DB. 
     * If both are empty, we fall back to parsing the raw artist string from the metadata.
     */
    val artistIds = mediaItem.mediaMetadata.extras?.getStringArrayList("artistIds")?.filter { it.isNotBlank() } ?: emptyList()
    val artistNamesFromExtras = mediaItem.mediaMetadata.extras?.getStringArrayList("artistNames")?.filter { it.isNotBlank() }
    
    val artistNames = if (!artistNamesFromExtras.isNullOrEmpty()) {
        artistNamesFromExtras
    } else {
        mediaItem.mediaMetadata.artist?.toString()?.split(Regex(",\\s*|\\s*&\\s*|\\s+and\\s+"))?.filter { it.isNotBlank() } ?: emptyList()
    }

    val artists = remember(artistIds, artistNames, artistInfo, mediaItem.mediaId) {
        if (artistIds.isNotEmpty() && artistIds.size == artistNames.size) {
            artistIds.zip(artistNames)
        } else if (artistInfo.isNotEmpty()) {
            artistInfo.map { it.id to (it.name ?: "Unknown") }
        } else if (artistNames.isNotEmpty()) {
            artistNames.map { name ->
                "synthetic_artist_${name.lowercase().filter { it.isLetterOrDigit() }}" to name
            }
        } else emptyList()
    }

    val finalAlbumId = mediaItem.mediaMetadata.extras?.getString("albumId") ?: albumInfo?.id ?: run {
        // Fallback: Create a synthetic album ID if we have a title but no ID.
        val title = mediaItem.mediaMetadata.albumTitle?.toString() ?: albumInfo?.name
        if (title != null && !title.contains("Save this song", ignoreCase = true)) {
            val artistPrefix = (mediaItem.mediaMetadata.artist?.toString() ?: "")
                .lowercase().filter { it.isLetterOrDigit() }
            "synthetic_album_${artistPrefix}_${title.lowercase().filter { it.isLetterOrDigit() }}"
        } else null
    }

    val likedAt by remember(mediaItem.mediaId) {
        Database.likedAt(mediaItem.mediaId).distinctUntilChanged()
    }.collectAsState(initial = null)

    val format by remember(mediaItem.mediaId) {
        Database.format(mediaItem.mediaId)
    }.collectAsState(initial = null)

    val downloadProgress = DownloadState.downloads[mediaItem.mediaId]

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
                .heightIn(max = 600.dp)
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
                    model = mediaItem.mediaMetadata.artworkUri.thumbnail(thumbnailSizePx),
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
                        text = mediaItem.mediaMetadata.title?.toString() ?: "",
                        style = LocalAppearance.current.typography.s.bold.copy(color = colorPalette.text),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    BasicText(
                        text = mediaItem.mediaMetadata.artist?.toString() ?: "",
                        style = LocalAppearance.current.typography.xs.semiBold.secondary.copy(color = colorPalette.textSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(modifier = Modifier.weight(1f, fill = false)) {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .fillMaxWidth()
                ) {
                    AnimatedVisibility(
                        visible = !isViewingPlaylists && !isViewingArtists && !isViewingMoreOptions,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            // NAVIGATION BUTTONS (Priority: Top of the menu)
                            onGoToAlbum?.let { onGoToAlbumFunc ->
                                finalAlbumId?.let { albumId ->
                                    MenuEntry(
                                        icon = R.drawable.disc,
                                        text = "Go to album",
                                        onClick = {
                                            val metadata = mediaItem.mediaMetadata
                                            val albumThumbnail = metadata.extras?.getString("albumThumbnailUrl")
                                            onGoToAlbumFunc(
                                                Album(
                                                    id = albumId,
                                                    title = (metadata.albumTitle ?: albumInfo?.name)?.toString(),
                                                    thumbnailUrl = albumThumbnail,
                                                    authorsText = (metadata.artist ?: albumInfo?.name)?.toString()
                                                )
                                            )
                                            onDismiss()
                                        }
                                    )
                                }
                            }

                            onGoToArtist?.let { onGoToArtistFunc ->
                                if (artists.isNotEmpty()) {
                                    MenuEntry(
                                        icon = R.drawable.person,
                                        text = "Go to artist",
                                        trailingContent = {
                                            if (artists.size > 1) {
                                                Image(
                                                    painter = painterResource(R.drawable.chevron_forward),
                                                    contentDescription = null,
                                                    colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            if (artists.size > 1) {
                                                isViewingArtists = true
                                            } else {
                                                onGoToArtistFunc(artists.first().first)
                                                onDismiss()
                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            MenuEntry(
                                icon = if (likedAt == null) R.drawable.heart_outline else R.drawable.heart,
                                text = if (likedAt == null) "Like" else "Unlike",
                                onClick = {
                                    onDismiss()
                                    query {
                                        if (Database.like(
                                                mediaItem.mediaId,
                                                if (likedAt == null) System.currentTimeMillis() else null
                                            ) == 0
                                        ) {
                                            Database.insert(mediaItem) { it.toggleLike() }
                                        }
                                    }
                                }
                            )

                            if (showSaveToVault) {
                                MenuEntry(
                                    icon = R.drawable.airplane,
                                    text = when {
                                        downloadProgress != null -> {
                                            when {
                                                downloadProgress < 0f -> "Preparing download..."
                                                downloadProgress < 1f -> "Starting download..."
                                                downloadProgress >= 100f -> "Encoding..." // Show encoding state after download finishes
                                                else -> "Downloading... (${downloadProgress.toInt()}%)"
                                            }
                                        }
                                        format == null -> "Save offline to vault"
                                        else -> "Remove from vault"
                                    },
                                    onClick = {
                                        if (downloadProgress != null) return@MenuEntry
                                        onDismiss()
                                        if (format == null) {
                                            MainApplication.scope.launch(Dispatchers.IO) {
                                                MediaFetchUtils.downloadMedia(context, mediaItem, false)
                                            }
                                        } else {
                                            MainApplication.scope.launch(Dispatchers.IO) {
                                                val file = MediaFetchUtils.getAudioFile(context, mediaItem.mediaId)
                                                if (file.exists()) file.delete()
                                                query {
                                                    Database.deleteFormat(mediaItem.mediaId)
                                                }
                                            }
                                        }
                                    }
                                )
                            }

                            onPlayNext?.let { onPlayNextFunc ->
                                MenuEntry(
                                    icon = R.drawable.play_skip_forward,
                                    text = "Play next",
                                    onClick = {
                                        onDismiss()
                                        onPlayNextFunc()
                                    }
                                )
                            }

                            onStartRadio?.let { onStartRadioFunc ->
                                MenuEntry(
                                    icon = R.drawable.radio,
                                    text = "Start radio",
                                    onClick = {
                                        onDismiss()
                                        onStartRadioFunc()
                                    }
                                )
                            }

                            MenuEntry(
                                icon = R.drawable.pencil,
                                text = "Edit Audio Tags & Cover Art",
                                onClick = {
                                    isEditingTags = true
                                }
                            )

                            onShowSleepTimer?.let {
                                MenuEntry(
                                    icon = R.drawable.alarm,
                                    text = "Sleep timer",
                                    onClick = {
                                        isShowingSleepTimerDialog = true
                                    }
                                )
                            }

                            MenuEntry(
                                icon = R.drawable.ellipsis_horizontal,
                                text = "More options",
                                trailingContent = {
                                    Image(
                                        painter = painterResource(R.drawable.chevron_forward),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    isViewingMoreOptions = true
                                }
                            )

                            MenuEntry(
                                icon = R.drawable.add,
                                text = "Add to playlist",
                                trailingContent = {
                                    Image(
                                        painter = painterResource(R.drawable.chevron_forward),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                                        modifier = Modifier
                                            .size(16.dp)
                                    )
                                },
                                onClick = {
                                    isViewingPlaylists = true
                                }
                            )

                            onRemoveFromQueue?.let { onRemoveFromQueueFunc ->
                                MenuEntry(
                                    icon = R.drawable.trash,
                                    text = "Remove from queue",
                                    onClick = {
                                        onDismiss()
                                        onRemoveFromQueueFunc()
                                    }
                                )
                            }

                            onRemoveFromPlaylist?.let { onRemoveFromPlaylistFunc ->
                                MenuEntry(
                                    icon = R.drawable.trash,
                                    text = "Remove from playlist",
                                    onClick = {
                                        onDismiss()
                                        onRemoveFromPlaylistFunc()
                                    }
                                )
                            }

                            onDelete?.let { onDeleteFunc ->
                                MenuEntry(
                                    icon = R.drawable.trash,
                                    text = "Delete",
                                    onClick = {
                                        onDismiss()
                                        onDeleteFunc()
                                    }
                                )
                            }

                            onMultipleDelete?.let { onMultipleDeleteFunc ->
                                MenuEntry(
                                    icon = R.drawable.reorder,
                                    text = "Select",
                                    onClick = {
                                        onDismiss()
                                        onMultipleDeleteFunc()
                                    }
                                )
                            }

                            onRemoveFromDiscover?.let { onRemoveFromDiscoverFunc ->
                                MenuEntry(
                                    icon = R.drawable.close,
                                    text = "Remove from Discover",
                                    onClick = {
                                        onDismiss()
                                        onRemoveFromDiscoverFunc()
                                    }
                                )
                            }

                            MenuEntry(
                                icon = R.drawable.share_social,
                                text = "Share",
                                onClick = {
                                    onDismiss()
                                    onShare()
                                }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isViewingMoreOptions,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.wrapContentHeight()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { isViewingMoreOptions = false }
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
                                    text = "More options",
                                    style = LocalAppearance.current.typography.s.bold.copy(color = colorPalette.text),
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }

                            onMultipleDelete?.let { onMultipleDeleteFunc ->
                                MenuEntry(
                                    icon = R.drawable.reorder,
                                    text = "Select",
                                    onClick = {
                                        onDismiss()
                                        onMultipleDeleteFunc()
                                    }
                                )

                                MenuEntry(
                                    icon = R.drawable.reorder,
                                    text = "Multiple Select",
                                    onClick = {
                                        onDismiss()
                                        onMultipleDeleteFunc()
                                    }
                                )
                            }

                            MenuEntry(
                                icon = R.drawable.share_social,
                                text = "Export as Synapcube",
                                onClick = {
                                    onDismiss()
                                    val title = mediaItem.mediaMetadata.title?.toString() ?: "Song"
                                    exportLauncher.launch("$title.synapcube")
                                }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isViewingArtists,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val lazyListState = rememberLazyListState()

                        Column(
                            modifier = Modifier
                                .heightIn(min = 200.dp, max = 500.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { isViewingArtists = false }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .fillMaxWidth()
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.chevron_back),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(colorPalette.text),
                                    modifier = Modifier
                                        .size(20.dp)
                                )

                                BasicText(
                                    text = "Back",
                                    style = LocalAppearance.current.typography.s.semiBold.copy(color = colorPalette.text),
                                    modifier = Modifier
                                        .padding(start = 16.dp)
                                )
                            }

                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                itemsIndexed(items = artists) { _, artist ->
                                    MenuEntry(
                                        icon = R.drawable.person,
                                        text = artist.second,
                                        onClick = {
                                            onDismiss()
                                            onGoToArtist?.invoke(artist.first)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isViewingPlaylists,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val playlists by Database.playlistPreviews(com.synap.music.enums.PlaylistSortBy.Name, com.synap.music.enums.SortOrder.Ascending).collectAsState(initial = emptyList())
                        val lazyListState = rememberLazyListState()

                        Column(
                            modifier = Modifier
                                .heightIn(min = 200.dp, max = 500.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { isViewingPlaylists = false }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .fillMaxWidth()
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.chevron_back),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(colorPalette.text),
                                    modifier = Modifier
                                        .size(20.dp)
                                )

                                BasicText(
                                    text = "Back",
                                    style = LocalAppearance.current.typography.s.semiBold.copy(color = colorPalette.text),
                                    modifier = Modifier
                                        .padding(start = 16.dp)
                                )
                            }

                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                item {
                                    MenuEntry(
                                        icon = R.drawable.add,
                                        text = "Create new playlist",
                                        onClick = {
                                            isCreatingPlaylist = true
                                        }
                                    )
                                }

                                itemsIndexed(items = playlists) { _, playlist ->
                                    MenuEntry(
                                        icon = R.drawable.playlist,
                                        text = playlist.playlist.name,
                                        onClick = {
                                            onDismiss()
                                            onAddToPlaylist?.invoke(playlist.playlist, playlist.songCount) ?: query {
                                                Database.insert(mediaItem)
                                                Database.insert(
                                                    SongPlaylistMap(
                                                        songId = mediaItem.mediaId,
                                                        playlistId = playlist.playlist.id,
                                                        position = playlist.songCount
                                                    )
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (scrollState.maxValue > 0) {
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .width(5.dp)
                            .fillMaxHeight()
                    ) {
                        val canvasHeight = size.height
                        val scrollMax = scrollState.maxValue.toFloat()
                        val scrollValue = scrollState.value.toFloat()

                        val scrollbarHeight = (canvasHeight * (canvasHeight / (canvasHeight + scrollMax)))
                            .coerceAtLeast(24.dp.toPx())

                        val scrollbarOffset = (scrollValue / scrollMax) * (canvasHeight - scrollbarHeight)

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
        }
    }
}
