/*
 * Copyright (C) 2026 SynapMusic Maintainers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.synap.pulse.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import com.synap.pulse.SynapService
import com.synap.pulse.models.ContinuationResponse
import com.synap.pulse.models.MusicShelfRenderer
import com.synap.pulse.models.MusicResponsiveListItemRenderer
import com.synap.pulse.models.bodies.BrowseBody
import com.synap.pulse.models.bodies.ContinuationBody
import com.synap.pulse.utils.from
import com.synap.pulse.utils.runCatchingNonCancellable
import com.synap.pulse.utils.toSongsPage
import com.synap.pulse.models.NavigationEndpoint
import com.synap.pulse.models.Context
import com.synap.pulse.SynapService.applySynapMusicHeaders
import kotlinx.serialization.json.*

private val JsonElement.jsonObjectOrNull get() = this as? JsonObject
private val JsonElement.jsonArrayOrNull get() = this as? JsonArray
private val JsonElement.jsonPrimitiveOrNull get() = this as? JsonPrimitive

/**
 * This is the workhorse of the discovery engine. It fetches playlists and albums.
 * Since YouTube's API is inconsistent, we use a "Dual-Identity" strategy:
 * we first try the Web version for high-res metadata, and if that fails to find songs,
 * we fall back to the Android version which is more reliable for tracklists.
 */
suspend fun SynapService.playlistPage(body: BrowseBody) = runCatchingNonCancellable {
    println("PlaylistPage: Starting exhaustive discovery for ${body.browseId}")
    
    // Pass 1: Try the Web identity.
    val webResponse = client.post(browse) {
        applySynapMusicHeaders(body.copy(context = Context.DefaultWeb))
    }.body<JsonObject>()
    
    val webPage = walkResponseTree(webResponse)
    
    if (webPage.songsPage?.list.isNullOrEmpty()) {
        // Pass 2: If Web came up empty, try the Android identity.
        println("PlaylistPage: Web pass yielded no songs. Trying Android identity.")
        val androidResponse = client.post(browse) {
            applySynapMusicHeaders(body.copy(context = Context.DefaultAndroid))
        }.body<JsonObject>()
        
        val androidPage = walkResponseTree(androidResponse)
        
        if (!androidPage.songsPage?.list.isNullOrEmpty()) {
            androidPage.copy(
                title = androidPage.title ?: webPage.title,
                authors = androidPage.authors ?: webPage.authors,
                playlistId = androidPage.playlistId ?: webPage.playlistId
            )
        } else {
            webPage.copy(playlistId = webPage.playlistId ?: androidPage.playlistId)
        }
    } else {
        webPage
    }
}

/**
 * The Tree Walker logic for Playlists. 
 * It breaks down the massive JSON into three distinct steps.
 */
private fun walkResponseTree(json: JsonObject): SynapService.PlaylistOrAlbumPage {
    val allSongs = mutableListOf<SynapService.SongItem>()
    var pageTitle: String? = null
    var pageThumbnail: com.synap.pulse.models.Thumbnail? = null
    var pageYear: String? = null
    var continuation: String? = null
    val authors = mutableListOf<SynapService.Info<NavigationEndpoint.Endpoint.Browse>>()

    val jsonString = json.toString()
    val playlistIdRegex = Regex("\"playlistId\":\"([^\"]+)\"")
    val technicalPlaylistId = playlistIdRegex.find(jsonString)?.groupValues?.getOrNull(1)

    // STEP 1: Find the Header.
    // We look for the top-level "identity" of the page (The Album Name and big Cover Art).
    // We do this first so we don't accidentally grab a tiny song thumbnail as the main cover.
    fun findHeader(element: JsonElement) {
        if (element !is JsonObject) return
        
        val headerKey = element.keys.firstOrNull { 
            it == "musicDetailHeaderRenderer" || it == "musicImmersiveHeaderRenderer" ||
            it == "musicVisualHeaderRenderer" || it == "musicResponsiveHeaderRenderer" ||
            it == "musicEditablePlaylistDetailHeaderRenderer" || it == "musicAlbumReleaseDetailHeaderRenderer"
        }

        if (headerKey != null) {
            val headerObj = element[headerKey]?.jsonObjectOrNull
            
            // Extract Page Title (Handling both rich runs and simple text)
            headerObj?.get("title")?.jsonObjectOrNull?.let { titleObj ->
                titleObj["runs"]?.jsonArrayOrNull?.getOrNull(0)?.jsonObjectOrNull?.get("text")?.jsonPrimitiveOrNull?.content?.let { text ->
                    if (!text.contains("Save this song", ignoreCase = true)) pageTitle = text
                }
                if (pageTitle == null) {
                    titleObj["simpleText"]?.jsonPrimitiveOrNull?.content?.let { text ->
                        if (!text.contains("Save this song", ignoreCase = true)) pageTitle = text
                    }
                }
            }
            
            // Extract Page Artwork. We look in multiple possible fields to ensure we get the high-res one.
            val thumbObj = headerObj?.get("thumbnail") 
                           ?: headerObj?.get("foregroundThumbnail")
                           ?: headerObj?.get("musicThumbnailRenderer")?.jsonObjectOrNull?.get("thumbnail")
            
            thumbObj?.jsonObjectOrNull?.get("thumbnails")?.jsonArrayOrNull?.lastOrNull()?.let { thumb ->
                pageThumbnail = try { SynapService.jsonConfig.decodeFromJsonElement<com.synap.pulse.models.Thumbnail>(thumb) } catch (_: Exception) { null }
            }
        } else {
            // Keep digging deeper if we haven't found the header yet.
            if (pageTitle == null || pageThumbnail == null) {
                element.values.forEach { findHeader(it) }
            }
        }
    }

    findHeader(json)

    // STEP 2: Scrape the Tracks.
    // Now we walk the tree again, but this time we're looking for lists of songs (shelves).
    val otherVersions = mutableListOf<SynapService.AlbumItem>()

    fun scan(element: JsonElement) {
        when (element) {
            is JsonObject -> {
                // Universal Renderer Capture: Grab songs from any type of list YT uses.
                val shelfJson = element["musicShelfRenderer"] ?: element["musicPlaylistShelfRenderer"] ?: element["playlistVideoListRenderer"] ?: element["itemSectionRenderer"]
                if (shelfJson != null) {
                    try {
                        val contents = shelfJson.jsonObjectOrNull?.get("contents")?.jsonArrayOrNull
                        contents?.forEach { item ->
                            if (item is JsonObject) {
                                try {
                                    val content = SynapService.jsonConfig.decodeFromJsonElement<MusicShelfRenderer.Content>(item)
                                    SynapService.SongItem.from(content)?.let { allSongs.add(it) }
                                } catch (e: Exception) {
                                    println("PlaylistPage: Failed to decode shelf content: ${e.message}")
                                }
                            }
                        }
                        
                        // Grab the "continuation" token if this list is long and has more pages.
                        if (continuation == null) {
                             shelfJson.jsonObjectOrNull?.get("continuations")?.jsonArrayOrNull?.getOrNull(0)?.jsonObjectOrNull?.get("nextContinuationData")?.jsonObjectOrNull?.get("continuation")?.jsonPrimitiveOrNull?.content?.let { continuation = it }
                        }
                    } catch (e: Exception) {
                        println("PlaylistPage: Failed to decode shelf: ${e.message}")
                    }
                }

                // While we're here, grab any artist links so we can show who made the album.
                element["navigationEndpoint"]?.jsonObjectOrNull?.get("browseEndpoint")?.jsonObjectOrNull?.let { browse ->
                    val type = browse["browseEndpointContextSupportedConfigs"]?.jsonObjectOrNull?.get("browseEndpointContextMusicConfig")?.jsonObjectOrNull?.get("pageType")?.jsonPrimitiveOrNull?.content
                    if (type == "MUSIC_PAGE_TYPE_ARTIST" || type == "MUSIC_PAGE_TYPE_USER_CHANNEL") {
                        val id = browse["browseId"]?.jsonPrimitiveOrNull?.content
                        val name = element["text"]?.jsonPrimitiveOrNull?.content 
                            ?: element["title"]?.jsonObjectOrNull?.get("runs")?.jsonArrayOrNull?.getOrNull(0)?.jsonObjectOrNull?.get("text")?.jsonPrimitiveOrNull?.content
                        if (id != null && name != null) {
                            authors.add(SynapService.Info(name = name, endpoint = NavigationEndpoint.Endpoint.Browse(browseId = id)))
                        }
                    }
                }

                // Native 'otherVersions' auto-upgrade parser
                // YouTube Music often hides full Studio Albums inside the 'Other versions' carousel
                // of 1-track Singles. By extracting them here, the UI can seamlessly auto-upgrade singles.
                val carouselShelf = element["musicCarouselShelfRenderer"]?.jsonObjectOrNull
                if (carouselShelf != null) {
                    val headerTitle = carouselShelf["header"]?.jsonObjectOrNull
                        ?.get("musicCarouselShelfBasicHeaderRenderer")?.jsonObjectOrNull
                        ?.get("title")?.jsonObjectOrNull
                        ?.get("runs")?.jsonArrayOrNull?.getOrNull(0)?.jsonObjectOrNull
                        ?.get("text")?.jsonPrimitiveOrNull?.content

                    if (headerTitle?.contains("Other versions", ignoreCase = true) == true) {
                        try {
                            val contents = carouselShelf["contents"]?.jsonArrayOrNull
                            contents?.forEach { item ->
                                val twoRowItem = item.jsonObjectOrNull?.get("musicTwoRowItemRenderer")
                                if (twoRowItem != null) {
                                    val content = SynapService.jsonConfig.decodeFromJsonElement<com.synap.pulse.models.MusicTwoRowItemRenderer>(twoRowItem)
                                    SynapService.AlbumItem.from(content)?.let { otherVersions.add(it) }
                                }
                            }
                        } catch (e: Exception) {
                            println("PlaylistPage: Failed to decode otherVersions carousel: ${e.message}")
                        }
                    }
                }

                element.values.forEach { scan(it) }
            }
            is JsonArray -> element.forEach { scan(it) }
            else -> {}
        }
    }

    scan(json)

    // STEP 3: Final Cleaning.
    // YT often omits thumbnails for songs inside an album to save bandwidth.
    // We fix this by applying the album's cover art to any song that's missing one.
    val finalizedSongs = allSongs.map { song ->
        if (song.thumbnail == null) song.copy(thumbnail = pageThumbnail) else song
    }.distinctBy { it.key }

    return SynapService.PlaylistOrAlbumPage(
        title = pageTitle,
        thumbnail = pageThumbnail,
        authors = authors.distinctBy { it.endpoint?.browseId }.takeIf { it.isNotEmpty() },
        year = pageYear,
        url = null,
        songsPage = if (finalizedSongs.isEmpty()) null else SynapService.ItemsPage(finalizedSongs, continuation),
        otherVersions = otherVersions.takeIf { it.isNotEmpty() },
        playlistId = technicalPlaylistId
    )
}

suspend fun SynapService.playlistPage(body: ContinuationBody) = runCatchingNonCancellable {
    val response = client.post(next) {
        applySynapMusicHeaders(body)
    }.body<ContinuationResponse>()

    response
        .continuationContents
        ?.musicShelfContinuation
        ?.toSongsPage()
}

private fun MusicShelfRenderer?.toSongsPage(): SynapService.ItemsPage<SynapService.SongItem>? {
    val items = this?.contents?.mapNotNull(SynapService.SongItem::from) ?: emptyList()
    if (items.isEmpty()) return null
    return SynapService.ItemsPage(list = items, continuation = this?.continuations?.firstOrNull()?.nextContinuationData?.continuation)
}
