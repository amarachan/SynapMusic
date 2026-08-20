package com.synap.pulse.models

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistVideoListRenderer(
    val contents: List<Content>?,
    val playlistId: String?
) {
    @Serializable
    data class Content(
        val playlistVideoRenderer: PlaylistVideoRenderer?
    )
}
