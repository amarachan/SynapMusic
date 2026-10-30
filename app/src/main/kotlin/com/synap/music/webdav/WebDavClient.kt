package com.synap.music.webdav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.w3c.dom.Element
import java.io.InputStream
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

class WebDavException(message: String) : Exception(message)

/**
 * Minimal, pure-OkHttp WebDAV client.
 * Performs PROPFIND directory listings to list and crawl remote NAS audio files and artwork over HTTPS.
 */
object WebDavClient {

    /**
     * Represents a single directory or file entry on a WebDAV server.
     */
    data class Entry(
        val url: String,
        val displayName: String,
        val isCollection: Boolean,
        val contentType: String? = null
    )

    /**
     * Container for remote audio and image entries discovered during directory crawling.
     */
    data class Listing(
        val audio: List<Entry>,
        val images: List<Entry>
    )

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val PROPFIND_BODY = """<?xml version="1.0" encoding="utf-8"?>
<d:propfind xmlns:d="DAV:">
  <d:prop>
    <d:displayname/>
    <d:resourcetype/>
    <d:getcontenttype/>
  </d:prop>
</d:propfind>
""".trimIndent().toRequestBody("application/xml; charset=utf-8".toMediaType())

    /**
     * Tests connection to a remote WebDAV server using Depth: 0 PROPFIND request.
     */
    suspend fun testConnection(url: String, username: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val target = WebDavConfig.normalizeUrl(url)
                require(WebDavConfig.isConfigured(target)) { "Enter a valid HTTP(S) server URL" }
                val request = propfindRequest(target, depth = "0", username, password)
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.code !in 200..299 && response.code != 207) {
                        throw WebDavException("Server returned status ${response.code}")
                    }
                }
            }
        }

    /**
     * Recursively crawls a remote WebDAV directory tree to collect audio files and album artwork.
     */
    suspend fun listLibrary(
        baseUrl: String,
        username: String,
        password: String,
        maxFiles: Int = 5000
    ): Result<Listing> = withContext(Dispatchers.IO) {
        runCatching {
            val root = WebDavConfig.normalizeUrl(baseUrl)
            require(WebDavConfig.isConfigured(root)) { "WebDAV URL is invalid" }
            val foundAudio = LinkedHashMap<String, Entry>()
            val foundImages = LinkedHashMap<String, Entry>()
            val dirs = ArrayDeque<String>()
            dirs.add(root)
            val visited = HashSet<String>()

            while (dirs.isNotEmpty() && foundAudio.size < maxFiles) {
                val dir = dirs.removeFirst()
                if (!visited.add(dir.lowercase())) continue
                val entries = propfind(dir, username, password).getOrThrow()
                for (entry in entries) {
                    if (entry.isCollection) {
                        if (!visited.contains(entry.url.lowercase())) dirs.add(entry.url)
                    } else {
                        val name = entry.displayName.ifBlank { entry.url }
                        when {
                            WebDavConfig.isAudioFile(name) -> foundAudio.putIfAbsent(entry.url, entry)
                            WebDavConfig.isImageFile(name) -> foundImages.putIfAbsent(entry.url, entry)
                        }
                    }
                }
            }

            Listing(foundAudio.values.toList(), foundImages.values.toList())
        }
    }

    private fun propfindRequest(
        url: String,
        depth: String,
        username: String,
        password: String
    ): Request {
        val builder = Request.Builder()
            .url(url)
            .method("PROPFIND", PROPFIND_BODY)
            .header("Depth", depth)

        WebDavConfig.basicAuthHeader(username, password)?.let {
            builder.header("Authorization", it)
        }

        return builder.build()
    }

    private fun propfind(
        dirUrl: String,
        username: String,
        password: String
    ): Result<List<Entry>> = runCatching {
        val request = propfindRequest(dirUrl, depth = "1", username, password)
        okHttpClient.newCall(request).execute().use { response ->
            if (response.code !in 200..299 && response.code != 207) {
                throw WebDavException("HTTP ${response.code} reading $dirUrl")
            }
            val bodyStream = response.body?.byteStream()
                ?: throw WebDavException("Empty response body from $dirUrl")
            parsePropfindXml(dirUrl, bodyStream)
        }
    }

    private fun parsePropfindXml(requestedUrl: String, stream: InputStream): List<Entry> {
        val doc = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(stream)

        val responses = doc.getElementsByTagNameNS("*", "response")
        val result = mutableListOf<Entry>()
        val requestedNormalized = WebDavConfig.normalizeUrl(requestedUrl).lowercase()

        for (i in 0 until responses.length) {
            val respEl = responses.item(i) as? Element ?: continue
            val href = respEl.getElementsByTagNameNS("*", "href").item(0)?.textContent?.trim() ?: continue
            val absoluteUrl = resolveHref(requestedUrl, href)

            if (WebDavConfig.normalizeUrl(absoluteUrl).lowercase() == requestedNormalized) {
                continue
            }

            val propStat = respEl.getElementsByTagNameNS("*", "propstat").item(0) as? Element
            val prop = propStat?.getElementsByTagNameNS("*", "prop")?.item(0) as? Element
                ?: respEl.getElementsByTagNameNS("*", "prop").item(0) as? Element

            val displayName = prop?.getElementsByTagNameNS("*", "displayname")?.item(0)?.textContent?.trim()
                ?: decodeFileName(absoluteUrl)

            val resourceType = prop?.getElementsByTagNameNS("*", "resourcetype")?.item(0) as? Element
            val isCollection = resourceType?.getElementsByTagNameNS("*", "collection")?.length ?: 0 > 0

            val contentType = prop?.getElementsByTagNameNS("*", "getcontenttype")?.item(0)?.textContent?.trim()

            result.add(Entry(absoluteUrl, displayName, isCollection, contentType))
        }

        return result
    }

    private fun resolveHref(baseUrl: String, href: String): String {
        return if (href.startsWith("http://", ignoreCase = true) || href.startsWith("https://", ignoreCase = true)) {
            href
        } else {
            val root = java.net.URI(WebDavConfig.normalizeUrl(baseUrl))
            val path = if (href.startsWith("/")) href else "/$href"
            "${root.scheme}://${root.authority}$path"
        }
    }

    private fun decodeFileName(fileUrl: String): String {
        val raw = fileUrl.trimEnd('/').substringAfterLast('/')
        return runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
    }
}
