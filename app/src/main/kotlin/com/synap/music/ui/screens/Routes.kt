package com.synap.music.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.synap.compose.routing.Route0
import com.synap.compose.routing.Route1
import com.synap.compose.routing.Route2
import com.synap.compose.routing.RouteHandlerScope
import com.synap.music.Database
import com.synap.music.enums.BuiltInPlaylist
import com.synap.music.models.Album
import com.synap.music.models.SearchQuery
import com.synap.music.query
import com.synap.music.ui.screens.album.AlbumScreen
import com.synap.music.ui.screens.artist.ArtistScreen
import com.synap.music.ui.screens.builtinplaylist.BuiltInPlaylistScreen
import com.synap.music.ui.screens.localplaylist.LocalPlaylistScreen
import com.synap.music.ui.screens.playlist.PlaylistScreen
import com.synap.music.ui.screens.search.SearchScreen
import com.synap.music.ui.screens.searchresult.SearchResultScreen
import com.synap.music.ui.screens.playlists.AllPlaylistsScreen
import com.synap.music.ui.screens.recap.SynapRecapScreen
import com.synap.music.ui.screens.settings.SettingsScreen
import com.synap.music.ui.screens.smartmixes.SmartMixesScreen
import com.synap.music.utils.pauseSearchHistoryKey
import com.synap.music.utils.preferences

import com.synap.music.models.Artist

val albumRoute = Route1<String?>("albumRoute")
val albumDetailsRoute = Route2<String, Album?>("albumDetailsRoute")
val artistRoute = Route2<String, Artist?>("artistRoute")
val builtInPlaylistRoute = Route1<BuiltInPlaylist>("builtInPlaylistRoute")
val localPlaylistRoute = Route1<Long?>("localPlaylistRoute")
val playlistRoute = Route1<String?>("playlistRoute")
val searchResultRoute = Route1<String>("searchResultRoute")
val searchRoute = Route1<String>("searchRoute")
val settingsRoute = Route0("settingsRoute")
val smartMixesRoute = Route0("smartMixesRoute")
val synapRecapRoute = Route0("synapRecapRoute")
val allPlaylistsRoute = Route0("allPlaylistsRoute")

@SuppressLint("ComposableNaming")
@Suppress("NOTHING_TO_INLINE")
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun RouteHandlerScope.globalRoutes() {
    albumRoute(this) { browseId ->
        AlbumScreen(
            browseId = browseId ?: error("browseId cannot be null")
        )
    }

    albumDetailsRoute(this) { browseId, album ->
        AlbumScreen(
            browseId = browseId,
            prefetchedAlbum = album
        )
    }

    artistRoute(this) { browseId, artist ->
        ArtistScreen(
            browseId = browseId,
            prefetchedArtist = artist
        )
    }

    playlistRoute(this) { browseId ->
        PlaylistScreen(
            browseId = browseId ?: error("browseId cannot be null")
        )
    }

    builtInPlaylistRoute(this) { builtInPlaylist ->
        BuiltInPlaylistScreen(
            builtInPlaylist = builtInPlaylist
        )
    }

    localPlaylistRoute(this) { playlistId ->
        LocalPlaylistScreen(
            playlistId = playlistId ?: error("playlistId cannot be null")
        )
    }

    settingsRoute(this) {
        SettingsScreen()
    }

    smartMixesRoute(this) {
        SmartMixesScreen()
    }

    synapRecapRoute(this) {
        SynapRecapScreen()
    }

    allPlaylistsRoute(this) {
        AllPlaylistsScreen()
    }

    searchRoute(this) { initialTextInput ->
        val context = LocalContext.current

        SearchScreen(
            initialTextInput = initialTextInput,
            onSearch = { query ->
                if (query.isNotEmpty() && !context.preferences.getBoolean(pauseSearchHistoryKey, false)) {
                    query {
                        Database.insert(SearchQuery(query = query))
                    }
                }
                searchResultRoute.global(query)
            },
            onViewPlaylist = { url ->
                val playlistId = url.toUri().getQueryParameter("list")
                if (playlistId != null) {
                    playlistRoute.global("VL$playlistId")
                }
            }
        )
    }

    searchResultRoute(this) { query ->
        SearchResultScreen(
            query = query
        )
    }
}
