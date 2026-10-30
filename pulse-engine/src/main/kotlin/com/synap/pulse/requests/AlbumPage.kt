package com.synap.pulse.requests

import com.synap.pulse.SynapService
import com.synap.pulse.models.NavigationEndpoint
import com.synap.pulse.models.bodies.BrowseBody

suspend fun SynapService.albumPage(body: BrowseBody): Result<SynapService.PlaylistOrAlbumPage>? {
    return playlistPage(body)?.map { album ->
        // If the exhaustive discovery already found songs, we're done
        if (!album.songsPage?.list.isNullOrEmpty()) return@map album

        // Deep Fallback: Fetch by technical playlistId if tracks are missing from the browse page
        album.playlistId?.let { id ->
            val browseId = if (id.startsWith("VL")) id else "VL$id"
            println("AlbumPage: Missing tracks. Attempting deep fallback with playlistId: $browseId")
            playlistPage(BrowseBody(browseId = browseId))?.getOrNull()?.let { playlist ->
                if (!playlist.songsPage?.list.isNullOrEmpty()) {
                    return@map album.copy(songsPage = playlist.songsPage)
                }
            }
        }
        album
    }?.map { album ->
        // Decorate songs with album metadata if missing
        val albumInfo = SynapService.Info(
            name = album.title,
            endpoint = NavigationEndpoint.Endpoint.Browse(
                browseId = body.browseId,
                params = body.params
            )
        )

        album.copy(
            songsPage = album.songsPage?.copy(
                list = album.songsPage.list?.map { song ->
                    song.copy(
                        authors = song.authors ?: album.authors,
                        album = albumInfo,
                        thumbnail = song.thumbnail ?: album.thumbnail
                    )
                }
            )
        )
    }
}
