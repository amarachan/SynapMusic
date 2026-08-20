package com.synap.pulse.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicItemThumbnailOverlayRenderer(
    val content: Content?
) {
    @Serializable
    data class Content(
        val musicPlayButtonRenderer: MusicPlayButtonRenderer?
    ) {
        @Serializable
        data class MusicPlayButtonRenderer(
            val navigationEndpoint: NavigationEndpoint?
        )
    }
}
