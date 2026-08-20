package com.synap.pulse.utils

import com.synap.pulse.SynapService
import com.synap.pulse.models.MusicResponsiveListItemRenderer
import com.synap.pulse.models.MusicShelfRenderer

fun SynapService.SongItem.Companion.from(renderer: MusicResponsiveListItemRenderer): SynapService.SongItem? {
    return from(MusicShelfRenderer.Content(musicResponsiveListItemRenderer = renderer))
}
