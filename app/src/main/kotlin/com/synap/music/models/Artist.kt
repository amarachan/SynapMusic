package com.synap.music.models

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
@Entity
data class Artist(
    @PrimaryKey val id: String,
    val name: String? = null,
    val thumbnailUrl: String? = null,
    val timestamp: Long? = null,
    val bookmarkedAt: Long? = null,
) : Parcelable

@Parcelize
data class ArtistWithDownloadStatus(
    @Embedded val artist: Artist,
    val totalSongs: Int,
    val downloadedSongs: Int,
    val anyFormatSongs: Int
) : Parcelable
