package com.synap.pulse.models

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistVideoRenderer(
    val title: Runs? = null,
    val thumbnail: ThumbnailRenderer.MusicThumbnailRenderer.Thumbnail? = null,
    val shortBylineText: Runs? = null,
    val lengthText: Runs? = null,
    val navigationEndpoint: NavigationEndpoint? = null,
    val videoId: String? = null,
)
