package com.synap.pulse.models.bodies

import com.synap.pulse.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context = Context.DefaultWeb,
    val browseId: String,
    val params: String? = null,
    val region: String? = null
)
