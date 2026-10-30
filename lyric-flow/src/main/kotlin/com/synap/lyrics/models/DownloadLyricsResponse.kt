package com.synap.lyrics.models

import kotlinx.serialization.Serializable

@Serializable
internal class DownloadLyricsResponse(
    val content: String
)
