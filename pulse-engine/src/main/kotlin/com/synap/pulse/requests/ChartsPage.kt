package com.synap.pulse.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.synap.pulse.SynapService
import com.synap.pulse.models.bodies.NextBody
import com.synap.pulse.models.BrowseResponse
import com.synap.pulse.models.Context
import com.synap.pulse.models.bodies.BrowseBody
import com.synap.pulse.utils.from
import com.synap.pulse.utils.runCatchingNonCancellable

suspend fun SynapService.chartsPage(region: String?): Result<List<SynapService.SongItem>?>? = runCatchingNonCancellable {
    val response = client.post(browse) {
        url {
            region?.let { parameters.append("gl", it) }
        }
        applySynapMusicHeaders(
            BrowseBody(
                context = Context.DefaultWeb.copy(
                    client = Context.DefaultWeb.client.copy(
                        gl = region
                    )
                ),
                browseId = "FEmusic_charts"
            )
        )
    }.body<BrowseResponse>()

    val sectionListRendererContents = response
        .contents
        ?.singleColumnBrowseResultsRenderer
        ?.tabs
        ?.firstOrNull()
        ?.tabRenderer
        ?.content
        ?.sectionListRenderer
        ?.contents
        ?: response
            .contents
            ?.sectionListRenderer
            ?.contents

    sectionListRendererContents
        ?.flatMap { content ->
            val shelfSongs = content.musicShelfRenderer?.contents?.mapNotNull { it.musicResponsiveListItemRenderer } ?: emptyList()
            val carouselSongs = content.musicCarouselShelfRenderer?.contents?.mapNotNull { it.musicResponsiveListItemRenderer } ?: emptyList()
            shelfSongs + carouselSongs
        }
        ?.mapNotNull { SynapService.SongItem.from(it) }
}
