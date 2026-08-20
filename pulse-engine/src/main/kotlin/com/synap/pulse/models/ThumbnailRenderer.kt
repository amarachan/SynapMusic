package com.synap.pulse.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ThumbnailRenderer(
    @JsonNames("croppedSquareThumbnailRenderer")
    val musicThumbnailRenderer: MusicThumbnailRenderer?
) {
    @Serializable
    data class MusicThumbnailRenderer(
        val thumbnail: Thumbnail?
    ) {
        @Serializable
        data class Thumbnail(
            val thumbnails: List<com.synap.pulse.models.Thumbnail>?
        )
    }

    val bestThumbnail: com.synap.pulse.models.Thumbnail?
        get() = musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()
}
