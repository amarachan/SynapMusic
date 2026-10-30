package com.synap.pulse.models

import kotlinx.serialization.Serializable

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import com.synap.pulse.SynapService

@Serializable
data class PlaylistPanelVideoRenderer(
    val title: Runs? = null,
    val longBylineText: Runs? = null,
    val shortBylineText: Runs? = null,
    val lengthText: Runs? = null,
    val navigationEndpoint: NavigationEndpoint? = null,
    val thumbnail: JsonElement? = null,
) {
    val thumbnailItem: Thumbnail?
        get() {
            if (thumbnail == null) return null
            return try {
                // Try Option 1: Standard wrapped renderer
                SynapService.jsonConfig.decodeFromJsonElement<ThumbnailRenderer>(thumbnail)
                    .musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()
            } catch (_: Exception) {
                try {
                    // Try Option 2: Direct thumbnail object with 'thumbnails' array
                    SynapService.jsonConfig.decodeFromJsonElement<ThumbnailRenderer.MusicThumbnailRenderer.Thumbnail>(thumbnail)
                        .thumbnails?.lastOrNull()
                } catch (_: Exception) {
                    /**
                     * Recursive deep-scan (Option 3).
                     * YouTube Radio items often use deeply nested JSON paths for artwork. 
                     * This "Universal Scraper" searches the entire response tree for any 
                     * valid thumbnail array, ensuring Radio tracks never have empty covers.
                     */
                    with(SynapService) {
                        thumbnail.walk("thumbnails") { 
                            try { 
                                SynapService.jsonConfig.decodeFromJsonElement<List<Thumbnail>>(it).lastOrNull()
                            } catch (_: Exception) { null } 
                        }.firstOrNull() ?: 
                        thumbnail.walk("url") {
                             try { 
                                 val url = it.toString().removeSurrounding("\"")
                                 if (url.startsWith("http")) Thumbnail(url = url, width = 800, height = 800) else null
                             } catch (_: Exception) { null }
                        }.firstOrNull()
                    }
                }
            }
        }
}
