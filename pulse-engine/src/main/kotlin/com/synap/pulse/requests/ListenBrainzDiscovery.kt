package com.synap.pulse.requests

import com.synap.pulse.SynapService
import com.synap.pulse.models.bodies.SearchBody
import com.synap.pulse.utils.from
import com.synap.pulse.utils.runCatchingNonCancellable

suspend fun SynapService.listenBrainzDiscovery(querySuffix: String = ""): Result<List<SynapService.SongItem>> = runCatchingNonCancellable {
    // We prioritize searched content if filters are present to avoid generic David Guetta/The Weeknd charts
    if (querySuffix.isNotEmpty()) {
        searchPage(
            body = SearchBody(
                query = querySuffix,
                params = SynapService.SearchFilter.Song.value
            ),
            fromMusicShelfRendererContent = SynapService.SongItem::from
        )?.getOrNull()?.list ?: emptyList()
    } else {
        // Only show general charts if NO filters are selected
        chartsPage(null)?.getOrNull() ?: emptyList()
    }
} ?: Result.success(emptyList())
