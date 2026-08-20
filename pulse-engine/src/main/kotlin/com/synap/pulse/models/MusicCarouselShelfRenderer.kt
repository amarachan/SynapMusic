package com.synap.pulse.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicCarouselShelfRenderer(
    val header: Header?,
    val contents: List<Content>?,
) {
    @Serializable
    data class Content(
        val musicTwoRowItemRenderer: MusicTwoRowItemRenderer? = null,
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
        val playlistVideoRenderer: PlaylistVideoRenderer? = null,
    )

    @Serializable
    data class Header(
        val musicTwoRowItemRenderer: MusicTwoRowItemRenderer? = null,
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
        val musicCarouselShelfBasicHeaderRenderer: MusicCarouselShelfBasicHeaderRenderer? = null
    ) {
        @Serializable
        data class MusicCarouselShelfBasicHeaderRenderer(
            val moreContentButton: MoreContentButton?,
            val title: Runs?,
            val strapline: Runs?,
        ) {
            @Serializable
            data class MoreContentButton(
                val buttonRenderer: ButtonRenderer?
            )
        }
    }
}
