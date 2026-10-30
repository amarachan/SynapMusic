package com.synap.pulse.utils

import com.synap.pulse.SynapService
import com.synap.pulse.models.PlaylistPanelVideoRenderer
import com.synap.pulse.models.NavigationEndpoint

fun SynapService.SongItem.Companion.from(renderer: PlaylistPanelVideoRenderer): SynapService.SongItem? {
    val runs = renderer.longBylineText?.runs ?: emptyList()
    
    // Strict pageType parsing ensures we don't accidentally parse "2.5M views" or generic text as an artist.
    val authors = runs.filter { run ->
        val pageType = run.navigationEndpoint?.browseEndpoint?.pageType
        pageType == "MUSIC_PAGE_TYPE_ARTIST" || pageType == "MUSIC_PAGE_TYPE_USER_CHANNEL"
    }.map { SynapService.Info<NavigationEndpoint.Endpoint.Browse>(it) }.takeIf { it.isNotEmpty() }

    // Strict pageType parsing ensures we extract the genuine Album name and browseId (MPREb_...), avoiding junk metadata.
    val album = runs.find { run ->
        run.navigationEndpoint?.browseEndpoint?.pageType == "MUSIC_PAGE_TYPE_ALBUM"
    }?.let { SynapService.Info<NavigationEndpoint.Endpoint.Browse>(it) }

    return SynapService.SongItem(
        info = SynapService.Info(
            name = renderer.title?.text,
            endpoint = renderer.navigationEndpoint?.watchEndpoint
        ),
        authors = authors,
        album = album,
        thumbnail = renderer.thumbnailItem,
        durationText = renderer.lengthText?.text
    ).takeIf { it.info?.endpoint?.videoId != null }
}
