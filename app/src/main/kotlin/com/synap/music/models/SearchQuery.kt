package com.synap.music.models

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
@Entity(
    indices = [
        Index(
            value = ["query"],
            unique = true
        )
    ]
)
data class SearchQuery(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String
) : Parcelable
