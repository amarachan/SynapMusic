package com.synap.pulse.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.synap.pulse.SynapService
import com.synap.pulse.models.bodies.NextBody
import com.synap.pulse.models.Context
import com.synap.pulse.models.BrowseResponse
import com.synap.pulse.models.MusicCarouselShelfRenderer
import com.synap.pulse.models.NextResponse
import com.synap.pulse.models.bodies.BrowseBody
import com.synap.pulse.utils.findSectionByStrapline
import com.synap.pulse.utils.findSectionByTitle
import com.synap.pulse.utils.from
import com.synap.pulse.utils.runCatchingNonCancellable

suspend fun SynapService.relatedPage(body: NextBody) = runCatchingNonCancellable {
    val nextResponse = client.post(next) {
        url {
            body.region?.let { parameters.append("gl", it) }
        }
        applySynapMusicHeaders(
            body.copy(
                context = Context.DefaultWeb.copy(
                    client = Context.DefaultWeb.client.copy(
                        gl = body.region
                    )
                )
            )
        )
    }.body<NextResponse>()

    val browseId = nextResponse
        .contents
        ?.singleColumnMusicWatchNextResultsRenderer
        ?.tabbedRenderer
        ?.watchNextTabbedResultsRenderer
        ?.tabs
        ?.find { it.tabRenderer?.title == "Related" }
        ?.tabRenderer
        ?.endpoint
        ?.browseEndpoint
        ?.browseId
        ?: nextResponse
            .contents
            ?.singleColumnMusicWatchNextResultsRenderer
            ?.tabbedRenderer
            ?.watchNextTabbedResultsRenderer
            ?.tabs
            ?.getOrNull(2)
            ?.tabRenderer
            ?.endpoint
            ?.browseEndpoint
            ?.browseId
        ?: return@runCatchingNonCancellable null

    val response = client.post(browse) {
        url {
            body.region?.let { parameters.append("gl", it) }
        }
        applySynapMusicHeaders(
            BrowseBody(
                context = Context.DefaultWeb.copy(
                    client = Context.DefaultWeb.client.copy(
                        gl = body.region
                    )
                ),
                browseId = browseId
            )
        )
    }.body<BrowseResponse>()

    val sectionListRenderer = response
        .contents
        ?.sectionListRenderer

    SynapService.RelatedPage(
        songs = sectionListRenderer
            ?.findSectionByTitle("You might also like")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicResponsiveListItemRenderer)
            ?.mapNotNull(SynapService.SongItem::from),
        playlists = sectionListRenderer
            ?.findSectionByTitle("Recommended playlists")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(SynapService.PlaylistItem::from)
            ?.sortedByDescending { it.channel?.name == "YouTube Music" },
        albums = sectionListRenderer
            ?.findSectionByStrapline("MORE FROM")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(SynapService.AlbumItem::from),
        artists = sectionListRenderer
            ?.findSectionByTitle("Similar artists")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(SynapService.ArtistItem::from),
    )
}
