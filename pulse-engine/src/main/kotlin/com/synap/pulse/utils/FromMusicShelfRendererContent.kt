package com.synap.pulse.utils

import com.synap.pulse.SynapService
import com.synap.pulse.models.MusicShelfRenderer
import com.synap.pulse.models.NavigationEndpoint
import com.synap.pulse.models.Runs

private val durationRegex = Regex("(\\d+:)?\\d+:\\d+")

fun SynapService.SongItem.Companion.from(content: MusicShelfRenderer.Content): SynapService.SongItem? {
    content.playlistPanelVideoRenderer?.let { return from(it) }
    val renderer = content.musicResponsiveListItemRenderer ?: return null
    val runs = content.allRuns
    val flattenedRuns = runs.flatten()
    
    // Playback ID Recovery
    val watchEndpoint = renderer.navigationEndpoint?.watchEndpoint
        ?: renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.navigationEndpoint?.watchEndpoint
        ?: renderer.menu?.items?.mapNotNull { it.menuNavigationItemRenderer?.navigationEndpoint?.watchEndpoint ?: it.menuServiceItemRenderer?.navigationEndpoint?.watchEndpoint }?.firstOrNull()
        ?: flattenedRuns.mapNotNull { it.navigationEndpoint?.watchEndpoint }.firstOrNull()
        ?: renderer.videoId_alt?.let { NavigationEndpoint.Endpoint.Watch(videoId = it) }

    if (watchEndpoint?.videoId == null) return null

    // Title Discovery
    val title = renderer.title_alt?.text
        ?: runs.getOrNull(0)?.firstOrNull { it.text != null }?.text
        ?: flattenedRuns.firstOrNull { run ->
            run.text != null && 
            run.navigationEndpoint?.browseEndpoint?.pageType != "MUSIC_PAGE_TYPE_ARTIST" && 
            run.navigationEndpoint?.browseEndpoint?.pageType != "MUSIC_PAGE_TYPE_ALBUM" &&
            !run.text!!.matches(durationRegex) && run.text != " • " && run.text!!.length > 1
        }?.text

    // Artist Discovery (PageType-aware)
    // We strictly filter out technical identifiers like "token" that YT leaks into the artist fields.
    val forbidden = setOf("token", "trackingparams", "clicktracking", "continuation", "and", "&", ",", "•")
    val authors = flattenedRuns.filter { run ->
        val pt = run.navigationEndpoint?.browseEndpoint?.pageType
        val text = run.text?.trim()?.lowercase() ?: ""
        (pt == "MUSIC_PAGE_TYPE_ARTIST" || pt == "MUSIC_PAGE_TYPE_USER_CHANNEL") && 
        text !in forbidden && !text.startsWith("and ") && !text.startsWith("& ")
    }.distinctBy { it.navigationEndpoint?.browseEndpoint?.browseId }
     .map { SynapService.Info<NavigationEndpoint.Endpoint.Browse>(it) }
     .takeIf { it.isNotEmpty() }
     ?: runs.getOrNull(1)?.filter { 
         val t = it.text?.trim()?.lowercase() ?: ""
         it.text != null && it.text != " • " && t !in forbidden && !t.startsWith("and ") && !t.startsWith("& ")
     }?.map { SynapService.Info<NavigationEndpoint.Endpoint.Browse>(it) }

    // Album Discovery
    val album = flattenedRuns.find { run ->
        run.navigationEndpoint?.browseEndpoint?.pageType == "MUSIC_PAGE_TYPE_ALBUM"
    }?.let { SynapService.Info<NavigationEndpoint.Endpoint.Browse>(it) }
     ?: runs.getOrNull(2)?.find { 
         val t = it.text?.lowercase() ?: ""
         it.text != null && 
         !it.text!!.matches(durationRegex) && 
         !t.contains("view") && 
         !t.contains("play") && 
         !t.contains("subscriber")
     }?.let { 
         // Fallback: If we find a 3rd text column that isn't a duration or view count, it's likely the album title
         SynapService.Info<NavigationEndpoint.Endpoint.Browse>(name = it.text, endpoint = it.navigationEndpoint?.browseEndpoint)
     }

    val durationText = flattenedRuns.find { it.text?.matches(durationRegex) == true }?.text

    return SynapService.SongItem(
        info = SynapService.Info(name = title, endpoint = watchEndpoint),
        authors = authors,
        album = album,
        durationText = durationText,
        thumbnail = content.thumbnail
    )
}

fun SynapService.AlbumItem.Companion.from(content: MusicShelfRenderer.Content): SynapService.AlbumItem? {
    content.musicTwoRowItemRenderer?.let { return from(it) }
    val renderer = content.musicResponsiveListItemRenderer ?: return null
    
    val browseEndpoint = renderer.navigationEndpoint?.browseEndpoint 
    
    // Strict Mode: If it's a responsive item, only count it if its main endpoint is an album
    if (browseEndpoint?.pageType != "MUSIC_PAGE_TYPE_ALBUM") {
        return null
    }

    val authors = content.allRuns.flatten()
        .filter { it.navigationEndpoint?.browseEndpoint?.pageType == "MUSIC_PAGE_TYPE_ARTIST" }
        .map { SynapService.Info<NavigationEndpoint.Endpoint.Browse>(it) }
        .takeIf { it.isNotEmpty() }

    val title = renderer.title_alt?.text ?: 
                content.allRuns.getOrNull(0)?.firstOrNull { it.text != null }?.text ?: 
                content.allRuns.flatten().firstOrNull { it.text != null }?.text

    return SynapService.AlbumItem(
        info = SynapService.Info(
            name = title?.takeIf { !it.contains("Save this song", ignoreCase = true) },
            endpoint = browseEndpoint
        ),
        authors = authors,
        year = content.allRuns.flatten().lastOrNull { it.text?.all { c -> c.isDigit() } == true && it.text.length == 4 }?.text,
        thumbnail = content.thumbnail
    )
}

fun SynapService.ArtistItem.Companion.from(content: MusicShelfRenderer.Content): SynapService.ArtistItem? {
    content.musicTwoRowItemRenderer?.let { return from(it) }
    val renderer = content.musicResponsiveListItemRenderer ?: return null
    
    val browseEndpoint = renderer.navigationEndpoint?.browseEndpoint 
    
    if (browseEndpoint?.pageType != "MUSIC_PAGE_TYPE_ARTIST" && browseEndpoint?.pageType != "MUSIC_PAGE_TYPE_USER_CHANNEL") {
        return null
    }

    return SynapService.ArtistItem(
        info = SynapService.Info(
            name = renderer.title_alt?.text ?: content.allRuns.getOrNull(0)?.firstOrNull()?.text ?: content.allRuns.flatten().firstOrNull { it.text != null }?.text,
            endpoint = browseEndpoint
        ),
        subscribersCountText = content.allRuns.flatten().find { it.text?.contains("subscriber") == true }?.text,
        thumbnail = content.thumbnail
    )
}

fun SynapService.PlaylistItem.Companion.from(content: MusicShelfRenderer.Content): SynapService.PlaylistItem? {
    content.musicTwoRowItemRenderer?.let { return from(it) }
    val renderer = content.musicResponsiveListItemRenderer ?: return null
    
    val browseEndpoint = renderer.navigationEndpoint?.browseEndpoint 
    
    if (browseEndpoint?.pageType != "MUSIC_PAGE_TYPE_PLAYLIST") {
        return null
    }

    return SynapService.PlaylistItem(
        info = SynapService.Info(
            name = renderer.title_alt?.text ?: content.allRuns.getOrNull(0)?.firstOrNull()?.text ?: content.allRuns.flatten().firstOrNull { it.text != null }?.text,
            endpoint = browseEndpoint
        ),
        channel = content.allRuns.flatten().firstOrNull { it.navigationEndpoint?.browseEndpoint?.pageType == "MUSIC_PAGE_TYPE_USER_CHANNEL" }?.let(SynapService::Info),
        songCount = content.allRuns.flatten().lastOrNull { it.text?.contains("song") == true }?.text?.split(' ')?.firstOrNull()?.toIntOrNull(),
        thumbnail = content.thumbnail
    )
}

fun SynapService.VideoItem.Companion.from(content: MusicShelfRenderer.Content): SynapService.VideoItem? {
    content.musicTwoRowItemRenderer?.let { return from(it) }
    val renderer = content.musicResponsiveListItemRenderer ?: return null
    
    val runs = content.allRuns
    val flattenedRuns = runs.flatten()

    val watchEndpoint = renderer.navigationEndpoint?.watchEndpoint 
        ?: renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.navigationEndpoint?.watchEndpoint
        ?: flattenedRuns.mapNotNull { it.navigationEndpoint?.watchEndpoint }.firstOrNull()
        ?: return null

    val title = renderer.title_alt?.text 
        ?: runs.getOrNull(0)?.firstOrNull { it.text != null }?.text
        ?: flattenedRuns.firstOrNull { run ->
            val t = run.text ?: return@firstOrNull false
            t != " • " && !t.contains("view") && !t.matches(durationRegex) && t.length > 1
        }?.text

    val authors = flattenedRuns.filter { it.navigationEndpoint?.browseEndpoint != null }
        .map { SynapService.Info<NavigationEndpoint.Endpoint.Browse>(it) }
        .takeIf { it.isNotEmpty() }
        ?: runs.getOrNull(1)?.filter { it.text != null && it.text != " • " }?.map { SynapService.Info<NavigationEndpoint.Endpoint.Browse>(it) }

    val viewsText = flattenedRuns.find { it.text?.contains("view") == true }?.text
    val durationText = flattenedRuns.find { it.text?.matches(durationRegex) == true }?.text

    return SynapService.VideoItem(
        info = SynapService.Info(
            name = title,
            endpoint = watchEndpoint
        ),
        authors = authors,
        viewsText = viewsText,
        durationText = durationText,
        thumbnail = content.thumbnail
    )
}
