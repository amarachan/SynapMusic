package com.synap.pulse.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchSuggestionsResponse(
    val contents: List<Content>?
) {
    @Serializable
    data class Content(
        val searchSuggestionsSectionRenderer: SearchSuggestionsSectionRenderer?
    ) {
        @Serializable
        data class SearchSuggestionsSectionRenderer(
            val contents: List<Content>?
        ) {
            @Serializable
            data class Content(
                val searchSuggestionRenderer: SearchSuggestionRenderer? = null,
                val historySuggestionRenderer: SearchSuggestionRenderer? = null
            ) {
                @Serializable
                data class SearchSuggestionRenderer(
                    val navigationEndpoint: NavigationEndpoint? = null,
                    val suggestion: Runs? = null,
                    val thumbnail: ThumbnailRenderer? = null
                )
            }
        }
    }
}
