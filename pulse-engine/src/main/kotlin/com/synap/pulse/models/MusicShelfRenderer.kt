package com.synap.pulse.models

import com.synap.pulse.SynapService
import com.synap.pulse.SynapService.walk
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MusicShelfRenderer(
    val bottomEndpoint: NavigationEndpoint? = null,
    val contents: List<Content>? = null,
    val continuations: List<Continuation>? = null,
    val title: Runs? = null
) {
    @Serializable
    data class Content(
        @JsonNames(
            "musicResponsiveListItemRenderer", 
            "musicPlaylistItemRenderer", 
            "musicTrackRenderer", 
            "musicAnalyticalListItemRenderer", 
            "musicDetailedListItemRenderer", 
            "musicVideoRenderer",
            "musicVisualListItemRenderer",
            "musicMediaListItemRenderer",
            "playlistVideoRenderer"
        )
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
        val musicTwoRowItemRenderer: MusicTwoRowItemRenderer? = null,
        val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer? = null,
        val unknown: kotlinx.serialization.json.JsonObject? = null,
    ) {
        val allRuns: List<List<Runs.Run>>
            get() = musicResponsiveListItemRenderer?.allRuns
                ?: musicTwoRowItemRenderer?.title?.let { listOf(listOf(Runs.Run(it.text, it.runs.firstOrNull()?.navigationEndpoint))) }
                ?: playlistPanelVideoRenderer?.title?.let { listOf(listOf(Runs.Run(it.text, null))) }
                ?: emptyList()

        val thumbnail: Thumbnail?
            get() = musicResponsiveListItemRenderer?.thumbnailItem
                ?: musicTwoRowItemRenderer?.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()
                ?: playlistPanelVideoRenderer?.thumbnailItem
                ?: (unknown?.get("thumbnail") ?: unknown?.get("thumbnailRenderer") ?: unknown?.get("thumbnail_alt"))?.let { thumb ->
                    try {
                        // Priority 1: Deep scan for highest resolution
                        val thumbnails = thumb.walk<Thumbnail>("thumbnails") { element ->
                            try { SynapService.jsonConfig.decodeFromJsonElement<Thumbnail>(element) } catch(_: Exception) { null }
                        }
                        if (thumbnails.isNotEmpty()) return thumbnails.lastOrNull()

                        // Priority 2: Direct decode attempts
                        SynapService.jsonConfig.decodeFromJsonElement<ThumbnailRenderer>(thumb)
                            .musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()
                    } catch (_: Exception) {
                        try {
                            SynapService.jsonConfig.decodeFromJsonElement<ThumbnailRenderer.MusicThumbnailRenderer.Thumbnail>(thumb)
                                .thumbnails?.lastOrNull()
                        } catch (_: Exception) {
                            try {
                                SynapService.jsonConfig.decodeFromJsonElement<List<Thumbnail>>(thumb)
                                    .lastOrNull()
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }
                }
    }
}
