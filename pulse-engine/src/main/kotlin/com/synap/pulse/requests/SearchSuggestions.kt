package com.synap.pulse.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.synap.pulse.SynapService
import com.synap.pulse.models.SearchSuggestionsResponse
import com.synap.pulse.models.bodies.SearchSuggestionsBody
import com.synap.pulse.utils.runCatchingNonCancellable

suspend fun SynapService.searchSuggestions(body: SearchSuggestionsBody) = runCatchingNonCancellable {
    val response = client.post(searchSuggestions) {
        applySynapMusicHeaders(body)
    }.body<SearchSuggestionsResponse>()

    response
        .contents
        ?.flatMap { content ->
            content.searchSuggestionsSectionRenderer?.contents?.mapNotNull { sectionContent ->
                val renderer = sectionContent.searchSuggestionRenderer ?: sectionContent.historySuggestionRenderer ?: return@mapNotNull null
                SynapService.Suggestion(
                    text = renderer.suggestion?.text ?: renderer.navigationEndpoint?.searchEndpoint?.query ?: return@mapNotNull null,
                    endpoint = renderer.navigationEndpoint,
                    thumbnail = renderer.thumbnail?.bestThumbnail
                )
            } ?: emptyList()
        }
}
