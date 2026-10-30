package com.synap.pulse.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import com.synap.pulse.SynapService
import com.synap.pulse.models.BrowseResponse
import com.synap.pulse.models.MusicCarouselShelfRenderer
import com.synap.pulse.models.MusicShelfRenderer
import com.synap.pulse.models.SectionListRenderer
import com.synap.pulse.models.bodies.BrowseBody
import com.synap.pulse.models.Thumbnail
import com.synap.pulse.utils.findSectionByTitle
import com.synap.pulse.utils.from
import com.synap.pulse.utils.runCatchingNonCancellable
import kotlinx.serialization.json.*

suspend fun SynapService.artistPage(body: BrowseBody): Result<SynapService.ArtistPage>? =
    runCatchingNonCancellable {
        val response = client.post(browse) {
            applySynapMusicHeaders(body)
        }.body<BrowseResponse>()
        
        val jsonResponse = jsonConfig.encodeToJsonElement(response)

        fun findSectionByTitle(text: String): SectionListRenderer.Content? {
            return response
                .contents
                ?.singleColumnBrowseResultsRenderer
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.findSectionByTitle(text)
        }

        val songsSection = findSectionByTitle("Songs")?.musicShelfRenderer
            ?: findSectionByTitle("Top songs")?.musicShelfRenderer
            ?: findSectionByTitle("Top tracks")?.musicShelfRenderer
        val albumsSection = findSectionByTitle("Albums")?.musicCarouselShelfRenderer
            ?: findSectionByTitle("Featured albums")?.musicCarouselShelfRenderer
            ?: findSectionByTitle("Discography")?.musicCarouselShelfRenderer
        val singlesSection = findSectionByTitle("Singles")?.musicCarouselShelfRenderer
            ?: findSectionByTitle("Singles & EPs")?.musicCarouselShelfRenderer
            
        val header = response.header?.musicImmersiveHeaderRenderer

        SynapService.ArtistPage(
            name = header?.title?.text ?: jsonResponse.walk("title") { element ->
                element.jsonObject["runs"]?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.content 
            }.firstOrNull(),
            description = header?.description?.text,
            thumbnail = (header?.foregroundThumbnail ?: header?.thumbnail)
                ?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull() 
                ?: jsonResponse.walk<Thumbnail>("thumbnail") { element ->
                    element.jsonObject["thumbnails"]?.jsonArray?.lastOrNull()?.let { t -> 
                        try {
                            jsonConfig.decodeFromJsonElement<Thumbnail>(t) 
                        } catch(_: Exception) { null }
                    }
                }.firstOrNull(),
            shuffleEndpoint = header?.playButton?.buttonRenderer?.navigationEndpoint?.watchEndpoint,
            radioEndpoint = header?.startRadioButton?.buttonRenderer?.navigationEndpoint?.watchEndpoint,
            songs = songsSection
                ?.contents
                ?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                ?.mapNotNull(SynapService.SongItem::from),
            songsEndpoint = songsSection
                ?.bottomEndpoint
                ?.browseEndpoint,
            albums = albumsSection
                ?.contents
                ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                ?.mapNotNull(SynapService.AlbumItem::from),
            albumsEndpoint = albumsSection
                ?.header
                ?.musicCarouselShelfBasicHeaderRenderer
                ?.moreContentButton
                ?.buttonRenderer
                ?.navigationEndpoint
                ?.browseEndpoint,
            singles = singlesSection
                ?.contents
                ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                ?.mapNotNull(SynapService.AlbumItem::from),
            singlesEndpoint = singlesSection
                ?.header
                ?.musicCarouselShelfBasicHeaderRenderer
                ?.moreContentButton
                ?.buttonRenderer
                ?.navigationEndpoint
                ?.browseEndpoint,
        )
    }
