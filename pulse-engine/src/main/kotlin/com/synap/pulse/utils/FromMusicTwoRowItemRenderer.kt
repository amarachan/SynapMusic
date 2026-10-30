package com.synap.pulse.utils

import com.synap.pulse.SynapService
import com.synap.pulse.models.bodies.NextBody
import com.synap.pulse.models.MusicTwoRowItemRenderer
import com.synap.pulse.models.NavigationEndpoint

fun SynapService.AlbumItem.Companion.from(renderer: MusicTwoRowItemRenderer): SynapService.AlbumItem? {
    val browseEndpoint = renderer.navigationEndpoint?.browseEndpoint
        ?: renderer.title?.runs?.firstOrNull()?.navigationEndpoint?.browseEndpoint
        ?: return null

    if (browseEndpoint.pageType != "MUSIC_PAGE_TYPE_ALBUM") return null

    val subtitleRuns = renderer.subtitle?.runs ?: emptyList()

    val title = renderer.title?.runs?.firstOrNull()?.text

    return SynapService.AlbumItem(
        info = SynapService.Info(
            name = title?.takeIf { !it.contains("Save this song", ignoreCase = true) },
            endpoint = browseEndpoint
        ),
        authors = subtitleRuns.filter { it.navigationEndpoint?.browseEndpoint?.pageType == "MUSIC_PAGE_TYPE_ARTIST" }
            .map { SynapService.Info<NavigationEndpoint.Endpoint.Browse>(it) }
            .takeIf { it.isNotEmpty() },
        year = subtitleRuns.lastOrNull { it.text?.all { c -> c.isDigit() } == true && it.text?.length == 4 }?.text,
        thumbnail = renderer
            .thumbnailRenderer
            ?.musicThumbnailRenderer
            ?.thumbnail
            ?.thumbnails
            ?.lastOrNull()
    )
}

fun SynapService.ArtistItem.Companion.from(renderer: MusicTwoRowItemRenderer): SynapService.ArtistItem? {
    val browseEndpoint = renderer.navigationEndpoint?.browseEndpoint
        ?: renderer.title?.runs?.mapNotNull { it.navigationEndpoint?.browseEndpoint }?.firstOrNull { it.pageType == "MUSIC_PAGE_TYPE_ARTIST" || it.pageType == "MUSIC_PAGE_TYPE_USER_CHANNEL" }
        ?: return null

    if (browseEndpoint.pageType != "MUSIC_PAGE_TYPE_ARTIST" && browseEndpoint.pageType != "MUSIC_PAGE_TYPE_USER_CHANNEL") return null

    return SynapService.ArtistItem(
        info = SynapService.Info(
            name = renderer.title?.runs?.firstOrNull()?.text,
            endpoint = browseEndpoint
        ),
        subscribersCountText = renderer.subtitle?.runs?.find { it.text?.contains("subscriber") == true }?.text,
        thumbnail = renderer
            .thumbnailRenderer
            ?.musicThumbnailRenderer
            ?.thumbnail
            ?.thumbnails
            ?.lastOrNull()
    )
}

fun SynapService.PlaylistItem.Companion.from(renderer: MusicTwoRowItemRenderer): SynapService.PlaylistItem? {
    val browseEndpoint = renderer.navigationEndpoint?.browseEndpoint
        ?: renderer.title?.runs?.mapNotNull { it.navigationEndpoint?.browseEndpoint }?.firstOrNull { it.pageType == "MUSIC_PAGE_TYPE_PLAYLIST" }
        ?: return null

    if (browseEndpoint.pageType != "MUSIC_PAGE_TYPE_PLAYLIST") return null

    val subtitleRuns = renderer.subtitle?.runs ?: emptyList()

    return SynapService.PlaylistItem(
        info = SynapService.Info(
            name = renderer.title?.runs?.firstOrNull()?.text,
            endpoint = browseEndpoint
        ),
        channel = subtitleRuns.firstOrNull { it.navigationEndpoint?.browseEndpoint?.pageType == "MUSIC_PAGE_TYPE_USER_CHANNEL" }?.let(SynapService::Info),
        songCount = subtitleRuns.lastOrNull { it.text?.contains("song") == true }?.text?.split(' ')?.firstOrNull()?.toIntOrNull(),
        thumbnail = renderer
            .thumbnailRenderer
            ?.musicThumbnailRenderer
            ?.thumbnail
            ?.thumbnails
            ?.lastOrNull()
    )
}

fun SynapService.VideoItem.Companion.from(renderer: MusicTwoRowItemRenderer): SynapService.VideoItem? {
    val watchEndpoint = renderer.navigationEndpoint?.watchEndpoint
        ?: renderer.title?.runs?.mapNotNull { it.navigationEndpoint?.watchEndpoint }?.firstOrNull()
        ?: return null

    val subtitleRuns = renderer.subtitle?.runs ?: emptyList()

    return SynapService.VideoItem(
        info = SynapService.Info(
            name = renderer.title?.runs?.firstOrNull()?.text,
            endpoint = watchEndpoint
        ),
        authors = subtitleRuns.filter { it.navigationEndpoint?.browseEndpoint != null }
            .map { SynapService.Info<NavigationEndpoint.Endpoint.Browse>(it) }
            .takeIf { it.isNotEmpty() },
        viewsText = subtitleRuns.find { it.text?.contains("view") == true }?.text,
        durationText = subtitleRuns.find { it.text?.contains(":") == true }?.text,
        thumbnail = renderer
            .thumbnailRenderer
            ?.musicThumbnailRenderer
            ?.thumbnail
            ?.thumbnails
            ?.lastOrNull()
    )
}
