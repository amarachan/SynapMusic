package com.synap.pulse.external

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object ListenBrainz {
    private val client = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }
        defaultRequest {
            url("https://api.listenbrainz.org/1/")
        }
    }

    @Serializable
    data class Recording(
        val artist_name: String,
        val track_name: String,
        val release_name: String? = null
    )

    @Serializable
    data class TopRecordingsResponse(
        val payload: Payload
    ) {
        @Serializable
        data class Payload(
            val recordings: List<Recording>
        )
    }

    suspend fun getTopRecordings(): Result<List<Recording>> = runCatching {
        client.get("stats/sitewide/recordings") {
            parameter("range", "this_week")
            parameter("count", 40)
        }.body<TopRecordingsResponse>().payload.recordings
    }

    suspend fun getFreshReleases(): Result<List<Recording>> = runCatching {
        // Fallback to top recordings if fresh releases endpoint is restrictive
        // In a real scenario, we'd use their recommendation endpoints
        getTopRecordings().getOrThrow()
    }
}
