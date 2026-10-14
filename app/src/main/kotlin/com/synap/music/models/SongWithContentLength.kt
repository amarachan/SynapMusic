package com.synap.music.models

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class SongWithContentLength(
    @Embedded val song: Song,
    val contentLength: Long?,
    val compressionEngine: com.synap.music.enums.CompressionEngine?
) : Parcelable
