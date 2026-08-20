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

package com.synap.pulse

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.compression.brotli
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import com.synap.pulse.models.NavigationEndpoint
import com.synap.pulse.models.Runs
import com.synap.pulse.models.Thumbnail
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * SynapService is the core engine for talking to YouTube Music.
 * It handles raw HTTP requests, signs them with the necessary headers,
 * and parses the complex JSON responses back into clean Kotlin models.
 */
object SynapService {
    @OptIn(ExperimentalSerializationApi::class)
    internal val jsonConfig = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    /**
     * The Universal Tree Walker.
     * YouTube's JSON is a massive, unpredictable nest. Instead of mapping every 
     * single field (which changes every week), this function recursively crawls 
     * the whole tree to find specific pieces of data (like track IDs or titles) 
     * wherever they might be hidden.
     */
    fun <T> JsonElement.walk(key: String, transform: (JsonElement) -> T?): List<T> {
        val results = mutableListOf<T>()

        fun recurse(element: JsonElement) {
            when (element) {
                is JsonObject -> {
                    element[key]?.let { transform(it)?.let { results.add(it) } }
                    element.values.forEach { recurse(it) }
                }
                is JsonArray -> {
                    element.forEach { recurse(it) }
                }
                else -> {}
            }
        }

        recurse(this)
        return results
    }

    private val INNER_TUBE_KEY = String(
        charArrayOf('A','I','z','a','S','y','A','O','_','F','J','2','S','l','q','U','8','Q','4','S','T','E','H','L','G','C','i','l','w','_','Y','9','_','1','1','q','c','W','8')
    )

    // Our main HTTP client. We use OkHttp because it's reliable on Android.
    val client = HttpClient(OkHttp) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(jsonConfig)
        }

        install(ContentEncoding) {
            brotli() // Use Brotli to save data. It's way faster than Gzip.
        }

        defaultRequest {
            url(scheme = "https", host ="music.youtube.com")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header("Origin", "https://music.youtube.com")
            header("Referer", "https://music.youtube.com/")
            // Public InnerTube client key.
            header("X-Goog-Api-Key", INNER_TUBE_KEY)
            url.parameters.append("key", INNER_TUBE_KEY)
            url.parameters.append("prettyPrint", "false")
        }
    }

    // YouTube's internal endpoints.
    internal const val browse = "/youtubei/v1/browse"
    internal const val next = "/youtubei/v1/next"
    internal const val player = "/youtubei/v1/player"
    internal const val queue = "/youtubei/v1/music/get_queue"
    internal const val search = "/youtubei/v1/search"
    internal const val searchSuggestions = "/youtubei/v1/music/get_search_suggestions"

    // Field Masks: Used to tell the server exactly which pieces of data we want back.
    // This makes the JSON responses much smaller and faster to parse.
    internal const val playlistPanelVideoRendererMask = "playlistPanelVideoRenderer(title,longBylineText,shortBylineText,lengthText,navigationEndpoint,thumbnail)"
    internal const val musicResponsiveListItemRendererMask = "musicResponsiveListItemRenderer(thumbnail,title,flexColumns,menu,navigationEndpoint,itemHeight)"
    internal const val musicTwoRowItemRendererMask = "musicTwoRowItemRenderer(thumbnail,title,subtitle,navigationEndpoint)"

    internal fun HttpRequestBuilder.mask(value: String = "*") =
        header("X-Goog-FieldMask", value)

    /**
     * Robust Header Injector.
     * YT Music is picky. If you don't send the exact headers they expect, they'll 
     * block the request or send low-quality audio. This function injects the 
     * correct Client Name, Version, and User-Agent to make our requests look official.
     */
    internal inline fun <reified T : Any> HttpRequestBuilder.applySynapMusicHeaders(body: T) {
        val jsonBody = jsonConfig.encodeToJsonElement(body).jsonObject
        val contextJson = jsonBody["context"]?.jsonObject
        val clientJson = contextJson?.get("client")?.jsonObject
        
        val clientName = clientJson?.get("clientName")?.toString()?.removeSurrounding("\"")
        val clientVersion = "7.07.51" 
        var userAgent = clientJson?.get("userAgent")?.toString()?.removeSurrounding("\"")

        if (clientName != null) {
            val clientCode = when (clientName) {
                "WEB_REMIX" -> "67"
                "ANDROID_MUSIC" -> "26"
                "IOS_MUSIC" -> "57"
                "TVHTML5_SIMPLY_EMBEDDED_PLAYER" -> "85"
                else -> "26" 
            }
            header("X-YouTube-Client-Name", clientCode)
            
            if (userAgent == null || userAgent == "null") {
                userAgent = when (clientName) {
                    "WEB_REMIX" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    else -> "com.google.android.apps.youtube.music/$clientVersion (Linux; U; Android 14) gzip"
                }
            }
        }
        
        header("X-YouTube-Client-Version", clientVersion)
        header("X-YouTube-Device", "phone")
        header("X-YouTube-Page-CL", "600000000")
        header("X-YouTube-Page-Label", "latest")
        
        if (userAgent != null) {
            header(HttpHeaders.UserAgent, userAgent)
        }
        
        header("X-Goog-Api-Format-Version", "2")
        header("X-Origin", "https://music.youtube.com")
        header("Referer", "https://music.youtube.com/")
        
        setBody(body)
    }

    data class Info<T : NavigationEndpoint.Endpoint>(
        val name: String?,
        val endpoint: T?
    ) {
        @Suppress("UNCHECKED_CAST")
        constructor(run: Runs.Run) : this(
            name = run.text,
            endpoint = run.navigationEndpoint?.endpoint as T?
        )
    }

    /**
     * Search Filters for YouTube Music API.
     * YouTube expects these as raw Base64 strings (padded with == or =).
     * DO NOT URL-encode these (e.g. %3D%3D). The JSON payload does not decode URLs,
     * so sending %3D%3D causes the backend to ignore the filter entirely, resulting in mixed/junk results.
     */
    @JvmInline
    value class SearchFilter(val value: String) {
        companion object {
            val Song = SearchFilter("EgWKAQIIAWoKEAkQBRAKEAMQBA==")
            val Video = SearchFilter("EgWKAQIQAWoKEAkQChAFEAMQBA==")
            val Album = SearchFilter("EgWKAQIYAWoKEAkQChAFEAMQBA==")
            val Artist = SearchFilter("EgWKAQIgAWoKEAkQChAFEAMQBA==")
            val CommunityPlaylist = SearchFilter("EgeKAQQoAEABagoQAxAEEAoQCRAF")
            val FeaturedPlaylist = SearchFilter("EgeKAQQoADgBagwQDhAKEAMQBRAJEAQ=")
        }
    }

    sealed class Item {
        abstract val thumbnail: Thumbnail?
        abstract val key: String
    }

    data class SongItem(
        val info: Info<NavigationEndpoint.Endpoint.Watch>?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val album: Info<NavigationEndpoint.Endpoint.Browse>?,
        val durationText: String?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info!!.endpoint!!.videoId!!

        companion object
    }

    data class VideoItem(
        val info: Info<NavigationEndpoint.Endpoint.Watch>?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val viewsText: String?,
        val durationText: String?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info!!.endpoint!!.videoId!!

        val isOfficialMusicVideo: Boolean
            get() = info?.endpoint?.type == "MUSIC_VIDEO_TYPE_OMV" || info?.endpoint?.type == "MUSIC_VIDEO_TYPE_ATV"

        companion object
    }

    data class AlbumItem(
        val info: Info<NavigationEndpoint.Endpoint.Browse>?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val year: String?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info!!.endpoint!!.browseId!!

        companion object
    }

    data class ArtistItem(
        val info: Info<NavigationEndpoint.Endpoint.Browse>?,
        val subscribersCountText: String?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info!!.endpoint!!.browseId!!

        companion object
    }

    data class PlaylistItem(
        val info: Info<NavigationEndpoint.Endpoint.Browse>?,
        val channel: Info<NavigationEndpoint.Endpoint.Browse>?,
        val songCount: Int?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info!!.endpoint!!.browseId!!

        companion object
    }

    data class NextPage(
        val playlistId: String? = null,
        val playlistSetVideoId: String? = null,
        val params: String? = null,
        val itemsPage: ItemsPage<SongItem>? = null
    )

    data class RelatedPage(
        val songs: List<SongItem>? = null,
        val playlists: List<PlaylistItem>? = null,
        val albums: List<AlbumItem>? = null,
        val artists: List<ArtistItem>? = null
    )

    data class ArtistPage(
        val name: String?,
        val description: String?,
        val thumbnail: Thumbnail?,
        val shuffleEndpoint: NavigationEndpoint.Endpoint.Watch?,
        val radioEndpoint: NavigationEndpoint.Endpoint.Watch?,
        val songs: List<SongItem>?,
        val songsEndpoint: NavigationEndpoint.Endpoint.Browse?,
        val albums: List<AlbumItem>?,
        val albumsEndpoint: NavigationEndpoint.Endpoint.Browse?,
        val singles: List<AlbumItem>?,
        val singlesEndpoint: NavigationEndpoint.Endpoint.Browse?,
    )

    data class PlaylistOrAlbumPage(
        val title: String?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val year: String?,
        val thumbnail: Thumbnail?,
        val url: String?,
        val songsPage: ItemsPage<SongItem>?,
        val otherVersions: List<AlbumItem>?,
        val pageType: String? = null,
        val playlistId: String? = null
    )

    data class ItemsPage<T : Item>(
        val list: List<T>?,
        val continuation: String?
    )

    data class Suggestion(
        val text: String,
        val endpoint: NavigationEndpoint? = null,
        val thumbnail: Thumbnail? = null,
        val subtitle: String? = null
    )
}
