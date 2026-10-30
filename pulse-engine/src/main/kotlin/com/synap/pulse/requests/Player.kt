package com.synap.pulse.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.synap.pulse.SynapService
import com.synap.pulse.models.Context
import com.synap.pulse.models.PlayerResponse
import com.synap.pulse.models.bodies.PlayerBody
import com.synap.pulse.utils.runCatchingNonCancellable

suspend fun SynapService.player(body: PlayerBody) = runCatchingNonCancellable {
    val response = client.post(player) {
        applySynapMusicHeaders(body)
    }.body<PlayerResponse>()

    if (response.playabilityStatus?.status == "OK" && response.streamingData?.highestQualityFormat?.let { it.url != null || it.signatureCipher != null } == true) {
        response
    } else {
        val safePlayerResponse = client.post(player) {
            applySynapMusicHeaders(
                body.copy(
                    context = Context.DefaultAgeRestrictionBypass.copy(
                        thirdParty = Context.ThirdParty(
                            embedUrl = "https://www.youtube.com/watch?v=${body.videoId}"
                        )
                    ),
                )
            )
        }.body<PlayerResponse>()

        if (safePlayerResponse.playabilityStatus?.status == "OK" && safePlayerResponse.streamingData?.highestQualityFormat?.let { it.url != null || it.signatureCipher != null } == true) {
            return@runCatchingNonCancellable safePlayerResponse
        }

        safePlayerResponse
    }
}
