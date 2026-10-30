package com.synap.pulse.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.synap.pulse.SynapService
import com.synap.pulse.models.bodies.NextBody
import com.synap.pulse.models.BrowseResponse
import com.synap.pulse.models.NextResponse
import com.synap.pulse.models.bodies.BrowseBody
import com.synap.pulse.utils.runCatchingNonCancellable

import com.synap.pulse.SynapService.applySynapMusicHeaders
import kotlinx.serialization.json.*

private val JsonElement.jsonObjectOrNull get() = this as? JsonObject
private val JsonElement.jsonArrayOrNull get() = this as? JsonArray
private val JsonElement.jsonPrimitiveOrNull get() = this as? JsonPrimitive

suspend fun SynapService.lyrics(body: NextBody): Result<String?>? = runCatchingNonCancellable {
    val nextResponse = client.post(next) {
        applySynapMusicHeaders(body)
    }.body<JsonObject>()

    // UNIVERSAL LYRICS DISCOVERY: Scan tree for lyrics browseId
    var browseId: String? = null

    fun scan(element: JsonElement) {
        if (browseId != null) return
        when (element) {
            is JsonObject -> {
                // Check 1: Known YouTube Music lyrics browseId prefixes (FMrh, MPLY, MPLA, MP, etc.)
                element["browseId"]?.jsonPrimitiveOrNull?.content?.let { id ->
                    if (id.startsWith("FMrh") || id.startsWith("MPLY") || id.startsWith("MPLA") || id.startsWith("MP")) {
                        browseId = id
                        return
                    }
                }

                // Check 2: Tab Renderer labeled "Lyrics"
                val title = element["title"]?.jsonPrimitiveOrNull?.content 
                    ?: element["title"]?.jsonObjectOrNull?.get("runs")?.jsonArrayOrNull?.firstOrNull()?.jsonObjectOrNull?.get("text")?.jsonPrimitiveOrNull?.content
                
                if (title?.contains("Lyrics", ignoreCase = true) == true) {
                    val endpointBrowseId = element["endpoint"]?.jsonObjectOrNull?.get("browseEndpoint")?.jsonObjectOrNull?.get("browseId")?.jsonPrimitiveOrNull?.content
                        ?: element["browseEndpoint"]?.jsonObjectOrNull?.get("browseId")?.jsonPrimitiveOrNull?.content
                    if (endpointBrowseId != null) {
                        browseId = endpointBrowseId
                        return
                    }
                }

                element.values.forEach { scan(it) }
            }
            is JsonArray -> element.forEach { scan(it) }
            else -> {}
        }
    }

    scan(nextResponse)

    if (browseId == null) {
        println("Lyrics: No lyrics browseId found in tree for ${body.videoId}")
        return@runCatchingNonCancellable null
    }

    val response = client.post(browse) {
        applySynapMusicHeaders(BrowseBody(browseId = browseId))
    }.body<JsonObject>()

    // Scan response for description (the lyrics text)
    var lyricsText: String? = null

    fun scanLyrics(element: JsonElement) {
        if (lyricsText != null) return
        when (element) {
            is JsonObject -> {
                val shelfDesc = element["musicDescriptionShelfRenderer"]?.jsonObjectOrNull?.get("description")?.jsonObjectOrNull
                val mainDesc = element["description"]?.jsonObjectOrNull
                val targetDesc = shelfDesc ?: mainDesc

                targetDesc?.get("runs")?.jsonArrayOrNull?.joinToString("") { 
                    it.jsonObjectOrNull?.get("text")?.jsonPrimitiveOrNull?.content ?: "" 
                }?.let {
                    if (it.isNotBlank()) {
                        lyricsText = it
                        return
                    }
                }

                val runs = element["runs"]?.jsonArrayOrNull
                if (runs != null && (element.containsKey("musicDescriptionShelfRenderer") || element.containsKey("musicLyricsRenderer") || element.containsKey("description"))) {
                    val text = runs.joinToString("") { it.jsonObjectOrNull?.get("text")?.jsonPrimitiveOrNull?.content ?: "" }
                    if (text.isNotBlank()) {
                        lyricsText = text
                        return
                    }
                }

                element.values.forEach { scanLyrics(it) }
            }
            is JsonArray -> element.forEach { scanLyrics(it) }
            else -> {}
        }
    }

    scanLyrics(response)
    lyricsText
}
