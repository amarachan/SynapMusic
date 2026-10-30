package com.synap.pulse.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicTwoRowItemRenderer(
    val navigationEndpoint: NavigationEndpoint? = null,
    val thumbnailRenderer: ThumbnailRenderer? = null,
    val title: Runs? = null,
    val subtitle: Runs? = null,
)
