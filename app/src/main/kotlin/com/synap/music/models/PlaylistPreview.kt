package com.synap.music.models

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class PlaylistPreview(
    @Embedded val playlist: Playlist,
    val songCount: Int
) : Parcelable
