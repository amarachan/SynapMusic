package com.synap.pulse.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import com.synap.pulse.SynapService
import com.synap.pulse.models.GetQueueResponse
import com.synap.pulse.models.bodies.QueueBody
import com.synap.pulse.utils.from
import com.synap.pulse.utils.runCatchingNonCancellable
import com.synap.pulse.SynapService.applySynapMusicHeaders
import com.synap.pulse.SynapService.mask
import com.synap.pulse.SynapService.playlistPanelVideoRendererMask

suspend fun SynapService.queue(body: QueueBody): Result<List<SynapService.SongItem>?>? = runCatchingNonCancellable {
    val response = client.post(queue) {
        applySynapMusicHeaders(body)
        mask("queueDatas.content.$playlistPanelVideoRendererMask")
    }.body<GetQueueResponse>()

    response
        .queueDatas
        ?.mapNotNull { queueData ->
            queueData
                .content
                ?.let(SynapService.SongItem::from)
        }
}

suspend fun SynapService.song(videoId: String): Result<SynapService.SongItem?>? =
    queue(QueueBody(videoIds = listOf(videoId)))?.map { it?.firstOrNull() }
