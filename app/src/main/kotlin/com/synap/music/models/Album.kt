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
data class Album(
    @PrimaryKey val id: String,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val year: String? = null,
    val authorsText: String? = null,
    val shareUrl: String? = null,
    val timestamp: Long? = null,
    val bookmarkedAt: Long? = null
) : Parcelable

@Parcelize
data class AlbumWithDownloadStatus(
    @Embedded val album: Album,
    val totalSongs: Int,
    val downloadedSongs: Int,
    val anyFormatSongs: Int
) : Parcelable
