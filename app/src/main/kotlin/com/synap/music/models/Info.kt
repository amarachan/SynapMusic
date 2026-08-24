package com.synap.music.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Info(
    val id: String,
    val name: String?
) : Parcelable
