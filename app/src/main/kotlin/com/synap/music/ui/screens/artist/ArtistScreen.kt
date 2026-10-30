package com.synap.music.ui.screens.artist

import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.synap.music.ui.styling.LocalAppearance
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.style.TextOverflow
import com.synap.music.utils.bold
import androidx.compose.ui.unit.dp
import com.synap.compose.persist.PersistMapCleanup
import com.synap.compose.persist.persist
import com.synap.pulse.SynapService
import com.synap.pulse.models.bodies.BrowseBody
import com.synap.pulse.models.bodies.ContinuationBody
import com.synap.pulse.requests.artistPage
import com.synap.pulse.requests.searchPage
import com.synap.pulse.requests.itemsPage
import com.synap.pulse.utils.from
import com.synap.pulse.external.TheAudioDB
import com.synap.music.Database
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.ui.components.LocalMenuState
import com.synap.music.R
import com.synap.music.models.Artist
import com.synap.music.query
import com.synap.music.ui.components.themed.HeaderIconButton
import com.synap.music.ui.components.themed.NonQueuedMediaItemMenu
import com.synap.music.ui.components.themed.Scaffold
import com.synap.music.ui.components.themed.adaptiveThumbnailContent
import com.synap.music.ui.items.AlbumItem
import com.synap.music.ui.items.AlbumItemPlaceholder
import com.synap.music.ui.items.SongItem
import com.synap.music.ui.items.SongItemPlaceholder
import com.synap.music.ui.screens.albumDetailsRoute
import com.synap.music.ui.screens.searchresult.ItemsPage
import com.synap.music.ui.styling.Dimensions
import com.synap.music.ui.styling.px
import com.synap.music.utils.artistScreenTabIndexKey
import com.synap.music.utils.asMediaItem
import com.synap.music.utils.forcePlayAtIndex
import com.synap.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.synap.compose.routing.RouteHandlerScope
import androidx.compose.foundation.combinedClickable
import com.synap.pulse.models.Thumbnail

/**
 * The Artist Screen.
 * This is a comprehensive profile page for any artist. It combines local data 
 * (songs you've downloaded) with a deep search on YouTube and TheAudioDB 
 * to show high-res portraits, bios, and their whole catalog.
 */
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun RouteHandlerScope.ArtistScreen(browseId: String, prefetchedArtist: Artist? = null) {
    val saveableStateHolder = rememberSaveableStateHolder()

    var tabIndex by rememberPreference(artistScreenTabIndexKey, defaultValue = 0)

    PersistMapCleanup(tagPrefix = "artist/$browseId/")

    // STATE: We persist the artist info so it stays in memory during navigation.
    var artist by persist("artist/$browseId/artist", prefetchedArtist)

    var artistPage by persist<SynapService.ArtistPage?>("artist/$browseId/artistPage")

    /**
     * The Multi-API Healing Engine.
     * It performs a 3-step synchronization:
     * 1. Prefetch: Show what we already know from the previous screen immediately.
     * 2. Profile Match: If it's a "synthetic" artist (from a local file), try to 
     *    find the real official YouTube profile to get a better photo and bio.
     * 3. Sync: Pull the latest discography and trending tracks from the server.
     */
    LaunchedEffect(browseId, prefetchedArtist) {
        // Step 1: Lock-in prefetched data
        if (prefetchedArtist != null && (artist == null || artist?.thumbnailUrl == null)) {
            artist = prefetchedArtist
        }

        Database
            .artist(browseId)
            .combine(snapshotFlow { tabIndex }.map { it != 4 }) { artist, mustFetch -> artist to mustFetch }
            .distinctUntilChanged()
            .collect { (currentArtist, mustFetch) ->
                artist = currentArtist

                if (artistPage == null && (currentArtist?.timestamp == null || mustFetch)) {
                    if (browseId.startsWith("synthetic_artist_")) {
                        val artistName = currentArtist?.name ?: browseId.removePrefix("synthetic_artist_")
                        println("ArtistScreen: Synthetic ID $browseId detected for $artistName, attempting profile match...")
                        
                        withContext(Dispatchers.IO) {
                            // Stage 1: Official YouTube Match
                            val searchResult = SynapService.searchPage(
                                body = com.synap.pulse.models.bodies.SearchBody(
                                    query = artistName,
                                    params = SynapService.SearchFilter.Artist.value
                                ),
                                fromMusicTwoRowItemRenderer = { SynapService.ArtistItem.from(it) }
                            )

                            searchResult?.getOrNull()?.list?.firstOrNull()?.let { found ->
                                SynapService.artistPage(BrowseBody(browseId = found.key))?.onSuccess { matchedPage ->
                                    artistPage = matchedPage
                                    Database.upsert(
                                        Artist(
                                            id = browseId, 
                                            name = matchedPage.name,
                                            thumbnailUrl = matchedPage.thumbnail?.url,
                                            timestamp = System.currentTimeMillis(),
                                            bookmarkedAt = currentArtist?.bookmarkedAt
                                        )
                                    )
                                }
                            }

                            // Stage 2: Primary External Imagery & Bio (TheAudioDB)
                            TheAudioDB.getArtistInfo(artistName).onSuccess { externalArtist ->
                                if (externalArtist != null) {
                                    val finalName = externalArtist.strArtist ?: artistName
                                    val finalBio = externalArtist.strBiographyEN
                                    val finalThumb = externalArtist.strArtistThumb
                                    
                                    artistPage = (artistPage ?: SynapService.ArtistPage(
                                        name = finalName,
                                        description = null,
                                        thumbnail = null,
                                        shuffleEndpoint = null,
                                        radioEndpoint = null,
                                        songs = null,
                                        songsEndpoint = null,
                                        albums = null,
                                        albumsEndpoint = null,
                                        singles = null,
                                        singlesEndpoint = null
                                    )).copy(
                                        description = finalBio ?: artistPage?.description,
                                        thumbnail = finalThumb?.let { Thumbnail(url = it, width = 512, height = 512) } ?: artistPage?.thumbnail
                                    )

                                    Database.upsert(
                                        Artist(
                                            id = browseId,
                                            name = finalName,
                                            thumbnailUrl = finalThumb ?: artistPage?.thumbnail?.url ?: currentArtist?.thumbnailUrl,
                                            timestamp = System.currentTimeMillis(),
                                            bookmarkedAt = currentArtist?.bookmarkedAt
                                        )
                                    )
                                }
                            }

                            // Final Fallback: If EVERYTHING failed, we stop the loading shimmer and show what we have.
                            if (artistPage == null) {
                                artistPage = SynapService.ArtistPage(
                                    name = currentArtist?.name ?: artistName,
                                    description = "This artist's online profile couldn't be matched automatically.",
                                    thumbnail = currentArtist?.thumbnailUrl?.let { Thumbnail(url = it, width = 512, height = 512) },
                                    shuffleEndpoint = null,
                                    radioEndpoint = null,
                                    songs = null,
                                    songsEndpoint = null,
                                    albums = null,
                                    albumsEndpoint = null,
                                    singles = null,
                                    singlesEndpoint = null
                                )
                            }
                        }
                        return@collect
                    }

                    withContext(Dispatchers.IO) {
                        val result = SynapService.artistPage(BrowseBody(browseId = browseId))
                        
                        if (result != null && result.isSuccess) {
                            val currentArtistPage = result.getOrNull()!!
                            var finalArtistPage = currentArtistPage
                            
                            // Prefer TheAudioDB artist portrait and bio as primary, falling back to YouTube Music.
                            TheAudioDB.getArtistInfo(currentArtistPage.name ?: currentArtist?.name ?: "").onSuccess { external ->
                                if (external != null) {
                                    finalArtistPage = finalArtistPage.copy(
                                        description = external.strBiographyEN ?: finalArtistPage.description,
                                        thumbnail = external.strArtistThumb?.let { Thumbnail(url = it, width = 512, height = 512) }
                                            ?: finalArtistPage.thumbnail
                                    )
                                }
                            }
                            
                            artistPage = finalArtistPage

                            Database.upsert(
                                Artist(
                                    id = browseId,
                                    name = finalArtistPage.name,
                                    thumbnailUrl = finalArtistPage.thumbnail?.url,
                                    timestamp = System.currentTimeMillis(),
                                    bookmarkedAt = currentArtist?.bookmarkedAt
                                )
                            )
                        } else {
                            // NETWORK FAILURE FALLBACK
                            // If the main fetch fails, try a quick lookup in TheAudioDB 
                            // to at least show a bio and photo instead of a blank loading screen.
                            val artistName = currentArtist?.name ?: "Unknown Artist"
                            TheAudioDB.getArtistInfo(artistName).onSuccess { external ->
                                if (external != null) {
                                    artistPage = SynapService.ArtistPage(
                                        name = external.strArtist ?: artistName,
                                        description = external.strBiographyEN,
                                        thumbnail = external.strArtistThumb?.let { Thumbnail(url = it, width = 512, height = 512) },
                                        shuffleEndpoint = null,
                                        radioEndpoint = null,
                                        songs = null,
                                        songsEndpoint = null,
                                        albums = null,
                                        albumsEndpoint = null,
                                        singles = null,
                                        singlesEndpoint = null
                                    )
                                } else {
                                    // STOP SHIMMER: If everything fails, show a basic profile.
                                    artistPage = SynapService.ArtistPage(
                                        name = artistName,
                                        description = "No profile information available.",
                                        thumbnail = currentArtist?.thumbnailUrl?.let { Thumbnail(url = it, width = 512, height = 512) },
                                        shuffleEndpoint = null,
                                        radioEndpoint = null,
                                        songs = null,
                                        songsEndpoint = null,
                                        albums = null,
                                        albumsEndpoint = null,
                                        singles = null,
                                        singlesEndpoint = null
                                    )
                                }
                            }
                        }
                    }
                }
            }
    }

    val thumbnailContent =
        adaptiveThumbnailContent(
            artist?.timestamp == null,
            artist?.thumbnailUrl,
            CircleShape
        )

    val (colorPalette, typography) = LocalAppearance.current
    val context = LocalContext.current

    Scaffold(
        title = {
            BasicText(
                text = artist?.name ?: "Artist",
                style = typography.l.bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        topStartIconButtonId = R.drawable.chevron_back,
        onTopStartIconButtonClick = pop,
        topBarActions = {
            if (artist?.timestamp != null) {
                HeaderIconButton(
                    icon = if (artist?.bookmarkedAt == null) {
                        R.drawable.bookmark_outline
                    } else {
                        R.drawable.bookmark
                    },
                    color = colorPalette.accent,
                    onClick = {
                        val bookmarkedAt =
                            if (artist?.bookmarkedAt == null) System.currentTimeMillis() else null

                        query {
                            artist
                                ?.copy(bookmarkedAt = bookmarkedAt)
                                ?.let(Database::update)
                        }
                    }
                )

                HeaderIconButton(
                    icon = R.drawable.share_social,
                    color = colorPalette.text,
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "https://music.youtube.com/channel/$browseId"
                            )
                        }

                        context.startActivity(Intent.createChooser(sendIntent, null))
                    }
                )
            }
        },
        tabIndex = tabIndex,
        onTabChanged = { tabIndex = it },
        tabColumnContent = { item ->
            item(0, "Overview", R.drawable.sparkles)
            item(1, "Songs", R.drawable.musical_notes)
            item(2, "Albums", R.drawable.disc)
            item(3, "Singles", R.drawable.disc)
            item(4, "Local", R.drawable.library)
        },
    ) { currentTabIndex ->
        saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
            when (currentTabIndex) {
                0 -> ArtistOverview(
                    youtubeArtistPage = artistPage,
                    thumbnailContent = thumbnailContent,
                    onAlbumClick = { album ->
                        albumDetailsRoute.global(album.key, com.synap.music.models.Album(
                            id = album.key,
                            title = album.info?.name,
                            thumbnailUrl = album.thumbnail?.url
                        ))
                    },
                    onViewAllSongsClick = { tabIndex = 1 },
                    onViewAllAlbumsClick = { tabIndex = 2 },
                    onViewAllSinglesClick = { tabIndex = 3 },
                )

                1 -> {
                    val binder = LocalPlayerServiceBinder.current
                    val menuState = LocalMenuState.current
                    val thumbnailSizeDp = Dimensions.thumbnails.song
                    val thumbnailSizePx = thumbnailSizeDp.px

                    ItemsPage(
                        tag = "artist/$browseId/songs",
                        headerContent = { },
                        itemsPageProvider = artistPage?.let {
                            ({ continuation ->
                                continuation?.let {
                                    SynapService.itemsPage(
                                        body = ContinuationBody(continuation = continuation),
                                        fromMusicResponsiveListItemRenderer = SynapService.SongItem.Companion::from,
                                    )
                                } ?: artistPage
                                    ?.songsEndpoint
                                    ?.takeIf { it.browseId != null }
                                    ?.let { endpoint ->
                                        SynapService.itemsPage(
                                            body = BrowseBody(
                                                browseId = endpoint.browseId!!,
                                                params = endpoint.params,
                                            ),
                                            fromMusicResponsiveListItemRenderer = SynapService.SongItem.Companion::from,
                                        )
                                    }
                                ?: Result.success(
                                    SynapService.ItemsPage(
                                        list = artistPage?.songs,
                                        continuation = null
                                    )
                                )
                            })
                        },
                        itemContent = { song, index, list ->
                            SongItem(
                                song = song,
                                thumbnailSizeDp = thumbnailSizeDp,
                                thumbnailSizePx = thumbnailSizePx,
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {
                                            menuState.display {
                                                NonQueuedMediaItemMenu(
                                                    onDismiss = menuState::hide,
                                                    mediaItem = song.asMediaItem,
                                                )
                                            }
                                        },
                                        onClick = {
                                            val mediaItems = list.map { it.asMediaItem }
                                            binder?.stopRadio()
                                            binder?.player?.forcePlayAtIndex(mediaItems, index, context)
                                            binder?.setupRadio(song.info?.endpoint)
                                        }
                                    )
                            )
                        },
                        itemPlaceholderContent = {
                            SongItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                        }
                    )
                }

                2 -> {
                    val thumbnailSizeDp = 108.dp
                    val thumbnailSizePx = thumbnailSizeDp.px

                    ItemsPage(
                        tag = "artist/$browseId/albums",
                        headerContent = { },
                        emptyItemsText = "This artist didn't release any album",
                        itemsPageProvider = artistPage?.let {
                            ({ continuation ->
                                continuation?.let {
                                    SynapService.itemsPage(
                                        body = ContinuationBody(continuation = continuation),
                                        fromMusicTwoRowItemRenderer = SynapService.AlbumItem.Companion::from,
                                    )
                                } ?: artistPage
                                    ?.albumsEndpoint
                                    ?.takeIf { it.browseId != null }
                                    ?.let { endpoint ->
                                        SynapService.itemsPage(
                                            body = BrowseBody(
                                                browseId = endpoint.browseId!!,
                                                params = endpoint.params,
                                            ),
                                            fromMusicTwoRowItemRenderer = SynapService.AlbumItem.Companion::from,
                                        )
                                    }
                                ?: Result.success(
                                    SynapService.ItemsPage(
                                        list = artistPage?.albums,
                                        continuation = null
                                    )
                                )
                            })
                        },
                        itemContent = { album, _, _ ->
                            AlbumItem(
                                album = album,
                                thumbnailSizePx = thumbnailSizePx,
                                thumbnailSizeDp = thumbnailSizeDp,
                                modifier = Modifier
                                    .clickable(onClick = { 
                                        albumDetailsRoute.global(album.key, com.synap.music.models.Album(
                                            id = album.key,
                                            title = album.info?.name,
                                            thumbnailUrl = album.thumbnail?.url
                                        ))
                                    })
                            )
                        },
                        itemPlaceholderContent = {
                            AlbumItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                        }
                    )
                }

                3 -> {
                    val thumbnailSizeDp = 108.dp
                    val thumbnailSizePx = thumbnailSizeDp.px

                    ItemsPage(
                        tag = "artist/$browseId/singles",
                        headerContent = { },
                        emptyItemsText = "This artist didn't release any single",
                        itemsPageProvider = artistPage?.let {
                            ({ continuation ->
                                continuation?.let {
                                    SynapService.itemsPage(
                                        body = ContinuationBody(continuation = continuation),
                                        fromMusicTwoRowItemRenderer = SynapService.AlbumItem.Companion::from,
                                    )
                                } ?: artistPage
                                    ?.singlesEndpoint
                                    ?.takeIf { it.browseId != null }
                                    ?.let { endpoint ->
                                        SynapService.itemsPage(
                                            body = BrowseBody(
                                                browseId = endpoint.browseId!!,
                                                params = endpoint.params,
                                            ),
                                            fromMusicTwoRowItemRenderer = SynapService.AlbumItem.Companion::from,
                                        )
                                    }
                                ?: Result.success(
                                    SynapService.ItemsPage(
                                        list = artistPage?.singles,
                                        continuation = null
                                    )
                                )
                            })
                        },
                        itemContent = { album, _, _ ->
                            AlbumItem(
                                album = album,
                                thumbnailSizePx = thumbnailSizePx,
                                thumbnailSizeDp = thumbnailSizeDp,
                                modifier = Modifier
                                    .clickable(onClick = { 
                                        albumDetailsRoute.global(album.key, com.synap.music.models.Album(
                                            id = album.key,
                                            title = album.info?.name,
                                            thumbnailUrl = album.thumbnail?.url
                                        ))
                                    })
                            )
                        },
                        itemPlaceholderContent = {
                            AlbumItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                        }
                    )
                }

                4 -> ArtistLocalSongs(
                    browseId = browseId,
                    thumbnailContent = thumbnailContent,
                )
            }
        }
    }
}
