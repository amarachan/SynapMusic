package com.synap.music.lyrics

import android.content.Context
import com.synap.music.enums.LyricsLanguage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Lightweight, on-demand lyric translation engine powered by Google Translate API.
 * Translates LRC synchronized or plain-text lyrics into any target language
 * while preserving LRC timestamps and karaoke alignment.
 */
object LyricsTranslation {

    private val httpClient = HttpClient(OkHttp)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Translates LRC synchronized lyrics (e.g. "[00:12.34] Hello world") or plain-text
     * into the target language code (e.g. "es", "fr", "ja", "de").
     */
    suspend fun translateLrc(
        rawLrcText: String,
        targetLanguage: LyricsLanguage
    ): String = withContext(Dispatchers.IO) {
        if (targetLanguage == LyricsLanguage.Auto || rawLrcText.isBlank()) {
            return@withContext rawLrcText
        }

        runCatching {
            val lines = rawLrcText.lines()
            val translatedLines = mutableListOf<String>()

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isBlank()) {
                    translatedLines.add("")
                    continue
                }

                // Check for LRC timestamp prefix (e.g. "[00:12.34]")
                val timestampEndIndex = trimmed.indexOf(']')
                if (trimmed.startsWith("[") && timestampEndIndex > 0) {
                    val timestampPrefix = trimmed.substring(0, timestampEndIndex + 1)
                    val lyricText = trimmed.substring(timestampEndIndex + 1).trim()

                    if (lyricText.isBlank()) {
                        translatedLines.add(timestampPrefix)
                    } else {
                        val translatedText = translateText(lyricText, targetLanguage.code)
                        translatedLines.add("$timestampPrefix $translatedText")
                    }
                } else {
                    // Plain text line
                    val translatedText = translateText(trimmed, targetLanguage.code)
                    translatedLines.add(translatedText)
                }
            }

            translatedLines.joinToString("\n")
        }.getOrDefault(rawLrcText)
    }

    /**
     * Connects to Google Translate's free web endpoint for quick, keyless translation.
     */
    private suspend fun translateText(
        text: String,
        targetLangCode: String
    ): String = withContext(Dispatchers.IO) {
        runCatching {
            val responseText = httpClient.get("https://translate.googleapis.com/translate_a/single") {
                parameter("client", "gtx")
                parameter("sl", "auto")
                parameter("tl", targetLangCode)
                parameter("dt", "t")
                parameter("q", text)
            }.body<String>()

            val rootArray = json.parseToJsonElement(responseText) as? JsonArray
            val sentencesArray = rootArray?.getOrNull(0) as? JsonArray
            val translatedBuilder = StringBuilder()

            sentencesArray?.forEach { element ->
                val sentencePair = element as? JsonArray
                val translatedSentence = sentencePair?.getOrNull(0)?.jsonPrimitive?.content
                if (translatedSentence != null) {
                    translatedBuilder.append(translatedSentence)
                }
            }

            translatedBuilder.toString().ifBlank { text }
        }.getOrDefault(text)
    }
}
