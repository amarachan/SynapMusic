package com.synap.pulse.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import com.synap.pulse.SynapService
import com.synap.pulse.models.bodies.NextBody
import com.synap.pulse.models.ContinuationResponse
import com.synap.pulse.models.NextResponse
import com.synap.pulse.models.bodies.ContinuationBody
import com.synap.pulse.utils.runCatchingNonCancellable
import com.synap.pulse.utils.toSongsPage
import com.synap.pulse.SynapService.applySynapMusicHeaders

suspend fun SynapService.nextPage(body: NextBody): Result<SynapService.NextPage>? =
    runCatchingNonCancellable {
        val response = client.post(next) {
            applySynapMusicHeaders(body)
        }.body<NextResponse>()

        val tabs = response
            .contents
            ?.singleColumnMusicWatchNextResultsRenderer
            ?.tabbedRenderer
            ?.watchNextTabbedResultsRenderer
            ?.tabs

        val playlistPanelRenderer = tabs
            ?.getOrNull(0)
            ?.tabRenderer
            ?.content
            ?.musicQueueRenderer
            ?.content
            ?.playlistPanelRenderer

        if (body.playlistId == null) {
            val endpoint = playlistPanelRenderer
                ?.contents
                ?.lastOrNull()
                ?.musicResponsiveListItemRenderer
                ?.menu
                ?.items
                ?.mapNotNull { it.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: it.menuServiceItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint }
                ?.firstOrNull()

            if (endpoint != null) {
                return nextPage(
                    body.copy(
                        playlistId = endpoint.playlistId,
                        params = endpoint.params
                    )
                )
            }
        }

        SynapService.NextPage(
            playlistId = body.playlistId,
            playlistSetVideoId = body.playlistSetVideoId,
            params = body.params,
            itemsPage = playlistPanelRenderer
                ?.toSongsPage()
        )
    }

suspend fun SynapService.nextPage(body: ContinuationBody) = runCatchingNonCancellable {
    val response = client.post(next) {
        applySynapMusicHeaders(body)
    }.body<ContinuationResponse>()

    response
        .continuationContents
        ?.musicShelfContinuation
        ?.toSongsPage()
}
