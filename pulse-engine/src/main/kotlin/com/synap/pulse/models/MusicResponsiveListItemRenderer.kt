package com.synap.pulse.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

import com.synap.pulse.SynapService
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MusicResponsiveListItemRenderer(
    val fixedColumns: List<FlexColumn>? = null,
    val flexColumns: List<FlexColumn>? = null,
    val thumbnail: JsonElement? = null,
    val navigationEndpoint: NavigationEndpoint? = null,
    val overlay: Overlay? = null,
    val menu: Menu? = null,
    @JsonNames("title") val title_alt: Runs? = null,
    @JsonNames("videoId") val videoId_alt: String? = null,
) {
    val allRuns: List<List<Runs.Run>>
        get() {
            val runs = mutableListOf<List<Runs.Run>>()
            flexColumns?.forEach { it.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.let { runs.add(it) } }
            fixedColumns?.forEach { it.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.let { runs.add(it) } }
            if (runs.isEmpty()) title_alt?.let { runs.add(it.runs) }
            return runs
        }

    val thumbnailItem: Thumbnail?
        get() {
            if (thumbnail == null) return null
            return try {
                // Case 1: Wrapped in ThumbnailRenderer
                SynapService.jsonConfig.decodeFromJsonElement<ThumbnailRenderer>(thumbnail)
                    .musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()
            } catch (_: Exception) {
                try {
                    // Case 2: Direct Thumbnail object
                    SynapService.jsonConfig.decodeFromJsonElement<ThumbnailRenderer.MusicThumbnailRenderer.Thumbnail>(thumbnail)
                        .thumbnails?.lastOrNull()
                } catch (_: Exception) {
                    try {
                        // Case 3: List of Thumbnails (sometimes happens in some renderers)
                        SynapService.jsonConfig.decodeFromJsonElement<List<Thumbnail>>(thumbnail)
                            .lastOrNull()
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }

    @Serializable
    data class Menu(
        val items: List<Item>?
    ) {
        @Serializable
        data class Item(
            val menuNavigationItemRenderer: MenuNavigationItemRenderer? = null,
            val menuServiceItemRenderer: MenuNavigationItemRenderer? = null,
        ) {
            @Serializable
            data class MenuNavigationItemRenderer(
                val navigationEndpoint: NavigationEndpoint?,
                val serviceEndpoint: NavigationEndpoint? = null,
            )
        }
    }

    @Serializable
    data class FlexColumn(
        @JsonNames("musicResponsiveListItemFixedColumnRenderer")
        val musicResponsiveListItemFlexColumnRenderer: MusicResponsiveListItemFlexColumnRenderer?
    ) {
        @Serializable
        data class MusicResponsiveListItemFlexColumnRenderer(
            val text: Runs?
        )
    }

    @Serializable
    data class Overlay(
        val musicItemThumbnailOverlayRenderer: MusicItemThumbnailOverlayRenderer?
    )
}
