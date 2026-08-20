package com.synap.pulse.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.synap.pulse.SynapService
import com.synap.pulse.models.ContinuationResponse
import com.synap.pulse.models.MusicShelfRenderer
import com.synap.pulse.models.SearchResponse
import com.synap.pulse.models.GridRenderer
import com.synap.pulse.models.MusicTwoRowItemRenderer
import com.synap.pulse.models.bodies.ContinuationBody
import com.synap.pulse.models.bodies.SearchBody
import com.synap.pulse.utils.runCatchingNonCancellable
import com.synap.pulse.models.Tabs
import com.synap.pulse.models.SectionListRenderer
import com.synap.pulse.SynapService.applySynapMusicHeaders

/**
 * Executes a search query against YouTube Music's InnerTube API.
 * Scans all section lists, tabs, and shelf renderers to parse candidate items.
 */
suspend fun <T : SynapService.Item> SynapService.searchPage(
    body: SearchBody,
    fromMusicShelfRendererContent: ((MusicShelfRenderer.Content) -> T?)? = null,
    fromMusicTwoRowItemRenderer: ((MusicTwoRowItemRenderer) -> T?)? = null,
) = runCatchingNonCancellable {
    val response = client.post(search) {
        applySynapMusicHeaders(body)
    }.body<SearchResponse>()

    // DISCOVERY ENGINE: Scan ALL tabs and sections for valid results
    val allShelves = mutableListOf<MusicShelfRenderer>()
    val allGrids = mutableListOf<GridRenderer>()

    fun processSectionList(sectionList: SectionListRenderer) {
        sectionList.contents?.forEach { content ->
            content.musicShelfRenderer?.let { allShelves.add(it) }
            content.gridRenderer?.let { allGrids.add(it) }
        }
    }

    fun processTabs(tabs: List<Tabs.Tab>) {
        tabs.forEach { tab ->
            val content = tab.tabRenderer?.content ?: return@forEach
            content.sectionListRenderer?.let { processSectionList(it) }
            content.musicShelfRenderer?.let { allShelves.add(it) }
            content.musicPlaylistShelfRenderer?.let { allShelves.add(it) }
        }
    }

    response.contents?.tabbedSearchResultsRenderer?.let { processTabs(it.tabs ?: emptyList()) }
    response.contents?.twoColumnSearchResultsRenderer?.let { processTabs(it.tabs ?: emptyList()) }
    response.contents?.sectionListRenderer?.let { processSectionList(it) }

    println("searchPage: allShelves=${allShelves.size}, allGrids=${allGrids.size}")

    // Priority 1: High-fidelity shelves
    if (fromMusicShelfRendererContent != null) {
        val shelfResults = allShelves.map { shelf ->
            shelf to (shelf.contents?.mapNotNull(fromMusicShelfRendererContent) ?: emptyList())
        }
        // CRITICAL: We use 'firstOrNull' instead of 'maxByOrNull'. 
        // YouTube places highly-relevant official albums in a small shelf at the top.
        // If we used 'maxByOrNull', we would skip the official albums and grab the massive
        // "Tribute/Instrumental/Karaoke Covers" shelf at the bottom of the page simply because it has more items.
        val bestShelfEntry = shelfResults.firstOrNull { it.second.isNotEmpty() }
        if (bestShelfEntry != null) {
            println("searchPage: Found ${bestShelfEntry.second.size} items in best shelf")
            return@runCatchingNonCancellable bestShelfEntry.first.toItemsPage(fromMusicShelfRendererContent)
        }
    }

    // Priority 2: Grids
    if (fromMusicTwoRowItemRenderer != null) {
        val gridResults = allGrids.map { grid ->
            grid to (grid.items?.mapNotNull { it.musicTwoRowItemRenderer }?.mapNotNull(fromMusicTwoRowItemRenderer) ?: emptyList())
        }
        val bestGridEntry = gridResults.firstOrNull { it.second.isNotEmpty() }
        if (bestGridEntry != null) {
            println("searchPage: Found ${bestGridEntry.second.size} items in best grid")
            return@runCatchingNonCancellable bestGridEntry.first.toItemsPage(fromMusicTwoRowItemRenderer)
        }
    }

    println("searchPage: No items found")
    null
}

suspend fun <T : SynapService.Item> SynapService.searchPage(
    body: ContinuationBody,
    fromMusicShelfRendererContent: (MusicShelfRenderer.Content) -> T?
) = runCatchingNonCancellable {
    val response = client.post(search) {
        applySynapMusicHeaders(body)
    }.body<ContinuationResponse>()

    response
        .continuationContents
        ?.musicShelfContinuation
        ?.toItemsPage(fromMusicShelfRendererContent)
}

private fun <T : SynapService.Item> MusicShelfRenderer?.toItemsPage(mapper: (MusicShelfRenderer.Content) -> T?) =
    SynapService.ItemsPage(
        list = this
            ?.contents
            ?.mapNotNull(mapper),
        continuation = this
            ?.continuations
            ?.firstOrNull()
            ?.nextContinuationData
            ?.continuation
    )

private fun <T : SynapService.Item> GridRenderer?.toItemsPage(mapper: (MusicTwoRowItemRenderer) -> T?) =
    SynapService.ItemsPage(
        list = this
            ?.items
            ?.mapNotNull { it.musicTwoRowItemRenderer }
            ?.mapNotNull(mapper),
        continuation = null
    )
