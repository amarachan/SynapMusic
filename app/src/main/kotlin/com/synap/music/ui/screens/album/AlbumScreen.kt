package com.synap.music.ui.screens.album

import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.synap.music.ui.styling.LocalAppearance
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.style.TextOverflow
import com.synap.music.utils.bold
import com.synap.music.utils.semiBold
import androidx.compose.ui.unit.dp
import com.synap.compose.persist.persist
import com.synap.pulse.SynapService
import com.synap.pulse.external.TheAudioDB
import com.synap.pulse.models.bodies.BrowseBody
import com.synap.pulse.requests.albumPage
import com.synap.pulse.requests.searchPage
import com.synap.pulse.utils.from
import com.synap.music.Database
import com.synap.music.R
import com.synap.music.models.Album
import com.synap.music.query
import com.synap.music.ui.components.themed.HeaderIconButton
import com.synap.music.ui.components.themed.Scaffold
import com.synap.music.utils.MediaFetchUtils
import com.synap.music.MainApplication
import com.synap.music.ui.components.themed.adaptiveThumbnailContent
import com.synap.music.ui.items.AlbumItem
import com.synap.music.ui.items.AlbumItemPlaceholder
import com.synap.music.ui.screens.albumDetailsRoute
import com.synap.compose.persist.persistList
import com.synap.music.models.Song
import com.synap.music.ui.screens.searchresult.ItemsPage
import com.synap.music.ui.styling.px
import com.synap.music.utils.asMediaItem
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.synap.music.ui.components.themed.ConfirmationDialog
import com.synap.music.ui.components.themed.PlaylistSelectionDialog
import com.synap.music.ui.components.themed.SecondaryTextButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import com.synap.compose.routing.RouteHandlerScope
import com.synap.music.utils.toast

/**
 * The Album Details screen.
 * This screen is designed to be "Bulletproof" in terms of metadata.
 * It uses a 4-step synchronization engine to ensure that you always see the
 * highest quality artwork and tracklist, even if the initial network result is poor.
 */
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun RouteHandlerScope.AlbumScreen(browseId: String, prefetchedAlbum: Album? = null) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var tabIndex by rememberSaveable(browseId) {
        mutableStateOf(0)
    }

    /**
     * State Persistence.
     * We cache the metadata and song list in memory so the screen feels
     * instantaneous when you hit "Back" from a song.
     */
    var album by persist("album/$browseId/current_metadata_v5", prefetchedAlbum)
    var albumPage by persist<SynapService.PlaylistOrAlbumPage?>("album/$browseId/last_fetch_result_v5")
    var songs by persistList<Song>("album/$browseId/cached_songs_v5")

    val selection = remember { mutableStateListOf<String>() }
    var isShowingDeleteDialog by remember { mutableStateOf(false) }
    var isShowingPlaylistSelectionDialog by remember { mutableStateOf(false) }

    // Multi-select bulk actions (Add to Playlist / Delete).
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
            text = "Are you sure you want to delete ${selection.size} songs from your storage?",
            onDismiss = { isShowingDeleteDialog = false },
            onConfirm = {
                val toDelete = selection.toList()
                selection.clear()
                coroutineScope.launch(Dispatchers.IO) {
                    toDelete.forEach { songId ->
                        val file = MediaFetchUtils.getAudioFile(context, songId)
                        if (file.exists()) file.delete()
                        query {
                            Database.deleteFormat(songId)
                            Database.clearEventsFor(songId)
                        }
                    }
                }
                context.toast("Deleted ${toDelete.size} songs")
            }
        )
    }

    /**
     * THE 4-STEP SYNCHRONIZATION ENGINE.
     * 1. Prefetch: Lock in data from the previous screen immediately.
     * 2. Local Sync: Observe the DB to see if we already have these songs offline.
     * 3. Discovery: Trigger a background fetch if the data is old or missing.
     * 4. Atomic Commit: Save the official metadata back to the DB to "heal" the record.
     */
    LaunchedEffect(browseId, prefetchedAlbum) {
        // Step 1: Lock-in prefetched data
        // We only overwrite the current state if the prefetched data is "better" (has a title/thumbnail).
        if (prefetchedAlbum != null) {
            album = album?.copy(
                title = prefetchedAlbum.title ?: album?.title,
                thumbnailUrl = prefetchedAlbum.thumbnailUrl ?: album?.thumbnailUrl,
                authorsText = prefetchedAlbum.authorsText ?: album?.authorsText
            ) ?: prefetchedAlbum
        }

        Database
            .album(browseId)
            .combine(Database.albumSongs(browseId)) { localAlbum, localSongs ->
                localAlbum to localSongs
            }
            .distinctUntilChanged()
            .collect { (currentAlbum, localSongs) ->
                // Step 2: Merge Local DB Metadata.
                // We prefer the prefetched title/thumbnail if the DB record is just a skeleton
                // or if we've already loaded a high-res cover in the current session.
                if (currentAlbum != null) {
                    album = currentAlbum.copy(
                        title = currentAlbum.title ?: album?.title,
                        thumbnailUrl = if (currentAlbum.thumbnailUrl?.contains("http") == true) currentAlbum.thumbnailUrl else album?.thumbnailUrl,
                        authorsText = currentAlbum.authorsText ?: album?.authorsText
                    )
                } else if (album == null && prefetchedAlbum != null) {
                    album = prefetchedAlbum
                }

                if (localSongs.isNotEmpty()) {
                    songs = localSongs
                }

                // Step 3: Optimistic Discovery (Non-blocking Async Fetch)
                // We trigger background fetch if data is missing or older than 24 hours.
                val needsFetch = albumPage == null || (currentAlbum?.timestamp ?: 0L) < System.currentTimeMillis() - 86400000

                if (needsFetch) {
                    coroutineScope.launch(Dispatchers.IO) {
                        var targetId = browseId

                        // If it's a synthetic ID (from a local file or name-only search result),
                        // we first need to find the official album ID on YouTube.
                        if (browseId.startsWith("synthetic_album_")) {
                            val query = "${album?.authorsText} ${album?.title}"
                            println("AlbumScreen: Synthetic ID $browseId detected, searching for '$query'...")

                            val searchResult = SynapService.searchPage(
                                body = com.synap.pulse.models.bodies.SearchBody(
                                    query = query,
                                    params = SynapService.SearchFilter.Album.value
                                ),
                                fromMusicTwoRowItemRenderer = { SynapService.AlbumItem.from(it) }
                            )

                            searchResult?.getOrNull()?.list?.firstOrNull()?.let { found ->
                                targetId = found.key
                                println("AlbumScreen: Matched synthetic album to official ID: $targetId")
                            }
                        }

                        val result = SynapService.albumPage(BrowseBody(browseId = targetId))

                        if (result != null && result.isSuccess) {
                            val currentAlbumPage = result.getOrNull()!!
                            albumPage = currentAlbumPage

                            val albumArtistName = currentAlbumPage.authors?.firstOrNull()?.name ?: currentAlbum?.authorsText ?: album?.authorsText ?: ""
                            val albumTitle = currentAlbumPage.title ?: currentAlbum?.title ?: album?.title ?: ""
                            var primaryThumbnailUrl = currentAlbumPage.thumbnail?.url ?: currentAlbum?.thumbnailUrl ?: album?.thumbnailUrl

                            // Primary: Query TheAudioDB for high-res album cover, falling back to YouTube Music
                            TheAudioDB.getAlbumInfo(albumArtistName, albumTitle).onSuccess { tdbAlbum ->
                                if (tdbAlbum?.strAlbumThumb != null) {
                                    primaryThumbnailUrl = tdbAlbum.strAlbumThumb
                                }
                            }

                            val fetchedSongs = currentAlbumPage.songsPage?.list?.map { it.asMediaItem(
                                albumIdOverride = targetId,
                                albumTitleOverride = currentAlbumPage.title,
                                albumThumbnailUrlOverride = primaryThumbnailUrl
                            ) } ?: emptyList()

                            // Step 4: Atomic Commit (Heal the DB)
                            val updatedAlbum = Album(
                                id = targetId,
                                title = currentAlbumPage.title ?: currentAlbum?.title ?: album?.title,
                                thumbnailUrl = primaryThumbnailUrl,
                                year = currentAlbumPage.year ?: currentAlbum?.year,
                                authorsText = currentAlbumPage.authors?.joinToString(", ") { it.name ?: "" } ?: currentAlbum?.authorsText ?: album?.authorsText,
                                shareUrl = currentAlbumPage.url ?: currentAlbum?.shareUrl,
                                timestamp = System.currentTimeMillis(),
                                bookmarkedAt = currentAlbum?.bookmarkedAt
                            )
                            
                            query {
                                if (targetId != browseId) {
                                    // If we matched a synthetic ID to a real one, merge them.
                                    Database.remapAlbumSongs(browseId, targetId)
                                    Database.getAlbum(browseId)?.let { Database.delete(it) }
                                    
                                    // Update the route locally so returning back doesn't point to a deleted synthetic album
                                    MainApplication.scope.launch(Dispatchers.Main) {
                                        replace(albumDetailsRoute, targetId, updatedAlbum)
                                    }
                                }
                                Database.atomicAlbumUpdate(targetId, updatedAlbum, fetchedSongs)
                            }
                        } else {
                            // If the fetch fails, we set a "sentinel" object so the UI stops shimmering 
                            // and shows what we have in the DB (or "No songs found").
                            if (albumPage == null) {
                                albumPage = SynapService.PlaylistOrAlbumPage(
                                    title = album?.title,
                                    thumbnail = null,
                                    authors = null,
                                    year = null,
                                    url = null,
                                    songsPage = null,
                                    otherVersions = null
                                )
                            }
                        }
                    }
                }
            }
    }

    val thumbnailContent =
        adaptiveThumbnailContent(album?.thumbnailUrl == null && albumPage == null, album?.thumbnailUrl)

    val (colorPalette, typography) = LocalAppearance.current

    Scaffold(
        title = {
            if (selection.isNotEmpty()) {
                BasicText(
                    text = "${selection.size} selected",
                    style = typography.m.semiBold
                )
            } else {
                val title = album?.title ?: albumPage?.title
                BasicText(
                    text = title ?: "Loading...",
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

                val isAllSelected = selection.size == songs.size && songs.isNotEmpty()
                SecondaryTextButton(
                    text = if (isAllSelected) "Deselect all" else "Select all",
                    onClick = {
                        if (isAllSelected) {
                            selection.clear()
                        } else {
                            selection.clear()
                            selection.addAll(songs.map { it.id })
                        }
                    },
                    alternative = false
                )
            } else if (album?.timestamp != null) {
                HeaderIconButton(
                    icon = R.drawable.server,
                    color = colorPalette.accent,
                    onClick = {
                        val albumSongs = songs
                        context.toast("Adding ${albumSongs.size} songs to Vault...")
                        albumSongs.forEach { song ->
                            MainApplication.scope.launch {
                                MediaFetchUtils.downloadMedia(context, song.asMediaItem(browseId, album?.title, album?.thumbnailUrl), isVideo = false)
                            }
                        }
                    }
                )

                HeaderIconButton(
                    icon = if (album?.bookmarkedAt == null) R.drawable.bookmark_outline else R.drawable.bookmark,
                    color = colorPalette.accent,
                    onClick = {
                        val bookmarkedAt = if (album?.bookmarkedAt == null) System.currentTimeMillis() else null
                        query {
                            album?.copy(bookmarkedAt = bookmarkedAt)?.let(Database::update)
                        }
                    }
                )

                HeaderIconButton(
                    icon = R.drawable.share_social,
                    color = colorPalette.text,
                    onClick = {
                        album?.shareUrl?.let { url ->
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, url)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, null))
                        }
                    }
                )
            }
        },
        tabIndex = tabIndex,
        onTabChanged = { tabIndex = it },
        tabColumnContent = { item ->
            item(0, "Songs", R.drawable.musical_notes)
            item(1, "Other versions", R.drawable.disc)
        }
    ) { currentTabIndex ->
        saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
            when (currentTabIndex) {
                0 -> AlbumSongs(
                    albumId = browseId,
                    albumTitle = album?.title,
                    albumThumbnailUrl = album?.thumbnailUrl,
                    songs = songs,
                    isLoading = albumPage == null,
                    selection = selection,
                    thumbnailContent = thumbnailContent,
                )

                1 -> {
                    val thumbnailSizeDp = 108.dp
                    val thumbnailSizePx = thumbnailSizeDp.px

                    ItemsPage(
                        tag = "album/$browseId/alternatives",
                        headerContent = { },
                        initialPlaceholderCount = 1,
                        continuationPlaceholderCount = 1,
                        emptyItemsText = "No alternative versions found",
                        itemsPageProvider = albumPage?.let {
                            ({
                                Result.success(
                                    SynapService.ItemsPage(
                                        list = albumPage?.otherVersions,
                                        continuation = null
                                    )
                                )
                            })
                        },
                        itemContent = { albumItem: SynapService.AlbumItem, _, _ ->
                            AlbumItem(
                                album = albumItem,
                                thumbnailSizePx = thumbnailSizePx,
                                thumbnailSizeDp = thumbnailSizeDp,
                                modifier = Modifier
                                    .clickable {
                                        albumDetailsRoute.global(albumItem.key, Album(
                                            id = albumItem.key,
                                            title = albumItem.info?.name,
                                            thumbnailUrl = albumItem.thumbnail?.url,
                                            authorsText = albumItem.authors?.joinToString(", ") { it.name ?: "" }
                                        ))
                                    }
                            )
                        },
                        itemPlaceholderContent = {
                            AlbumItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                        }
                    )
                }
            }
        }
    }
}
