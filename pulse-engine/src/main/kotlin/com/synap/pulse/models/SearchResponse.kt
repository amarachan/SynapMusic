package com.synap.pulse.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val contents: Contents? = null,
) {
    @Serializable
    data class Contents(
        val tabbedSearchResultsRenderer: Tabs? = null,
        val twoColumnSearchResultsRenderer: TwoColumnSearchResultsRenderer? = null,
        val sectionListRenderer: SectionListRenderer? = null,
    )

    @Serializable
    data class TwoColumnSearchResultsRenderer(
        val tabs: List<Tabs.Tab>? = null
    )
}
