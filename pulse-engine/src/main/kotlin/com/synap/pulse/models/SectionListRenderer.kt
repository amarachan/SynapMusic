package com.synap.pulse.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SectionListRenderer(
    val contents: List<Content>? = null,
    val continuations: List<Continuation>? = null
) {
    @Serializable
    data class Content(
        @JsonNames("musicImmersiveCarouselShelfRenderer")
        val musicCarouselShelfRenderer: MusicCarouselShelfRenderer? = null,
        @JsonNames("musicPlaylistShelfRenderer", "musicShelfRenderer")
        val musicShelfRenderer: MusicShelfRenderer? = null,
        val gridRenderer: GridRenderer? = null,
        val musicDescriptionShelfRenderer: MusicDescriptionShelfRenderer? = null,
        val playlistVideoListRenderer: PlaylistVideoListRenderer? = null,
        val musicAlbumReleaseContentsRenderer: MusicAlbumReleaseContentsRenderer? = null,
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
        val musicMultiRowListItemRenderer: MusicResponsiveListItemRenderer? = null,
        val itemSectionRenderer: ItemSectionRenderer? = null,
    ) {

        @Serializable
        data class ItemSectionRenderer(
            val contents: List<Content>? = null
        )

        @Serializable
        data class MusicDescriptionShelfRenderer(
            val description: Runs? = null,
        )

        @Serializable
        data class MusicAlbumReleaseContentsRenderer(
            val contents: List<MusicShelfRenderer.Content>? = null
        )
    }

}
