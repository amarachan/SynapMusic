package com.synap.music.recognition

import com.synap.pulse.SynapService
import com.synap.pulse.models.bodies.SearchBody
import com.synap.pulse.requests.searchPage
import com.synap.pulse.utils.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Top-level response container for AudD music recognition requests.
 */
@Serializable
data class AudDResponse(
    val status: String,
    val result: AudDResult? = null
)

/**
 * Track metadata result from AudD music recognition.
 */
@Serializable
data class AudDResult(
    val artist: String,
    val title: String,
    val album: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val label: String? = null,
    @SerialName("timecode") val timecode: String? = null,
    val song_link: String? = null
)

/**
 * Combined recognition result containing recognized track details and the matched YouTube Music candidate.
 */
data class RecognitionResult(
    val title: String,
    val artist: String,
    val album: String?,
    val matchedSongItem: SynapService.SongItem?
)

/**
 * Client for AudD music recognition API (`https://api.audd.io/`).
 * Posts recorded audio samples and queries YouTube Music to retrieve playable tracks.
 */
class MusicRecognitionClient(
    private val apiKey: String = "test"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Uploads the recorded WAV audio file to AudD recognition API and resolves the matching YouTube Music song.
     */
    suspend fun recognizeSong(audioFile: File): Result<RecognitionResult> = withContext(Dispatchers.IO) {
        runCatching {
            val token = apiKey.ifBlank { "test" }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_token", token)
                .addFormDataPart("return", "timecode,apple_music,spotify")
                .addFormDataPart(
                    "file",
                    audioFile.name,
                    audioFile.asRequestBody("audio/wav".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.audd.io/")
                .post(requestBody)
                .build()

            val responseBody = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Recognition service returned HTTP ${response.code}")
                }
                response.body?.string() ?: throw Exception("Empty response from recognition service")
            }

            val auddResponse = json.decodeFromString<AudDResponse>(responseBody)
            val result = auddResponse.result
                ?: throw Exception("No music match found for audio sample")

            val query = "${result.title} ${result.artist}"

            val searchResult = SynapService.searchPage(
                body = SearchBody(query = query, params = SynapService.SearchFilter.Song.value),
                fromMusicShelfRendererContent = SynapService.SongItem.Companion::from
            )?.getOrNull()

            val bestMatch = searchResult?.list?.firstOrNull()

            RecognitionResult(
                title = result.title,
                artist = result.artist,
                album = result.album,
                matchedSongItem = bestMatch
            )
        }
    }
}
