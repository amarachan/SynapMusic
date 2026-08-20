package com.synap.pulse.models

import kotlinx.serialization.Serializable

@Serializable
data class Runs(
    val runs: List<Run> = listOf()
) {
    val text: String
        get() = runs.joinToString("") { it.text ?: "" }

    fun splitBySeparator(): List<List<Run>> {
        val result = mutableListOf<List<Run>>()
        var current = mutableListOf<Run>()
        
        // YT often leaks tokens and separators into the artist field. 
        // We filter them out here to keep the library clean.
        val forbidden = setOf("token", "trackingparams", "clicktracking", "continuation", "and", "&", ",", "•")

        runs.forEach { run ->
            val text = run.text?.trim()?.lowercase()
            if (run.text == " • " || text == "and" || text == "&" || text == ",") {
                if (current.isNotEmpty()) {
                    result.add(current)
                    current = mutableListOf()
                }
            } else if (text != null && text !in forbidden) {
                current.add(run)
            }
        }
        if (current.isNotEmpty()) result.add(current)
        
        // Fallback if everything was junk, otherwise return the split list
        return result.ifEmpty { if (runs.isNotEmpty()) listOf(runs) else emptyList() }
    }

    @Serializable
    data class Run(
        val text: String?,
        val navigationEndpoint: NavigationEndpoint?,
    )
}
