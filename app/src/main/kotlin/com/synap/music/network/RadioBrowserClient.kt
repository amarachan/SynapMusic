package com.synap.music.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Data response model for RadioBrowser API station entries.
 */
@Serializable
data class RadioStation(
    @SerialName("stationuuid") val stationUuid: String,
    val name: String,
    val url: String,
    @SerialName("url_resolved") val urlResolved: String? = null,
    val homepage: String? = null,
    val favicon: String? = null,
    val tags: String? = null,
    val country: String? = null,
    val bitrate: Int? = null,
    val codec: String? = null
)

/**
 * Client for querying the open-source RadioBrowser API (`https://de1.api.radio-browser.info`).
 * Provides access to tens of thousands of free live FM/AM and online web radio stations.
 */
class RadioBrowserClient {

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private val baseUrl = "https://de1.api.radio-browser.info/json"

    /**
     * Fetches top-voted / most popular radio stations worldwide.
     */
    suspend fun getTopStations(limit: Int = 30): List<RadioStation> = withContext(Dispatchers.IO) {
        runCatching {
            httpClient.get("$baseUrl/stations/topclick/$limit") {
                header("User-Agent", "SynapMusic/1.0.0")
            }.body<List<RadioStation>>()
        }.getOrDefault(emptyList())
    }

    /**
     * Searches radio stations by station name or genre tag.
     */
    suspend fun searchStations(query: String, limit: Int = 30): List<RadioStation> = withContext(Dispatchers.IO) {
        runCatching {
            httpClient.get("$baseUrl/stations/search") {
                header("User-Agent", "SynapMusic/1.0.0")
                parameter("name", query)
                parameter("limit", limit)
            }.body<List<RadioStation>>()
        }.getOrDefault(emptyList())
    }

    /**
     * Fetches radio stations by genre tag (e.g., "pop", "rock", "jazz", "news").
     */
    suspend fun getStationsByTag(tag: String, limit: Int = 30): List<RadioStation> = withContext(Dispatchers.IO) {
        runCatching {
            httpClient.get("$baseUrl/stations/bytag/${tag.lowercase()}") {
                header("User-Agent", "SynapMusic/1.0.0")
                parameter("limit", limit)
            }.body<List<RadioStation>>()
        }.getOrDefault(emptyList())
    }
}
