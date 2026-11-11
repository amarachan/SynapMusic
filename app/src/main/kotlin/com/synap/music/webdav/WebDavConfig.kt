package com.synap.music.webdav

import com.synap.music.models.Song
import java.util.Locale

/**
 * Configuration and URL helper functions for WebDAV network storage integration.
 */
object WebDavConfig {

    const val PLAYLIST_BROWSE_ID = "local:webdav"
    const val PLAYLIST_NAME = "Network Drive"

    val audioExtensions = setOf(
        "mp3", "m4a", "flac", "ogg", "opus", "aac", "wav", "wma", "aif", "aiff", "webm"
    )

    /**
     * Normalizes a raw WebDAV server URL string into a proper http/https endpoint.
     */
    fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isEmpty()) return ""
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    /**
     * Checks if a given WebDAV URL string is valid and configured with a host.
     */
    fun isConfigured(url: String): Boolean {
        val normalized = normalizeUrl(url)
        if (!normalized.startsWith("http://", ignoreCase = true) &&
            !normalized.startsWith("https://", ignoreCase = true)
        ) {
            return false
        }
        return hostOf(normalized) != null
    }

    /**
     * Generates HTTP Basic Auth header encoding for WebDAV requests.
     */
    fun basicAuthHeader(username: String, password: String): String? {
        if (username.isBlank() && password.isBlank()) return null
        val credentials = "$username:$password"
        val encoded = android.util.Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        return "Basic $encoded"
    }

    /**
     * Extracts lowercase host name from a WebDAV URL.
     */
    fun hostOf(url: String): String? = runCatching {
        java.net.URI(normalizeUrl(url)).host?.lowercase(Locale.ROOT)
    }.getOrNull()?.takeIf { it.isNotBlank() }

    /**
     * Checks if a file name or path has a supported audio extension.
     */
    fun isAudioFile(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return ext in audioExtensions
    }

    /**
     * Checks if a file name or path has a supported image extension.
     */
    fun isImageFile(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return ext in setOf("jpg", "jpeg", "png", "webp")
    }

    /**
     * Generates a stable webdav: videoId for a remote NAS audio file.
     */
    fun idFor(fileUrl: String): String = "webdav:$fileUrl"

    /**
     * Checks if a videoId belongs to a remote WebDAV NAS file.
     */
    fun isWebDavId(id: String): Boolean = id.startsWith("webdav:")

    /**
     * Extracts the HTTP/HTTPS URL from a webdav: videoId.
     */
    fun fileUrlOf(id: String): String? =
        id.removePrefix("webdav:").takeIf { isWebDavId(id) && it.startsWith("http") }

    /**
     * Constructs a SynapMusic [Song] model from a remote WebDAV file URL.
     */
    fun songFor(fileUrl: String): Song {
        val decoded = runCatching {
            java.net.URLDecoder.decode(fileUrl.substringAfterLast('/'), "UTF-8")
        }.getOrDefault(fileUrl.substringAfterLast('/'))
        val base = decoded.substringBeforeLast('.').takeIf { it.isNotBlank() } ?: decoded
        val (artist, title) = if (" - " in base) {
            val parts = base.split(" - ", limit = 2)
            parts[0].trim().takeIf { it.isNotBlank() } to parts[1].trim().takeIf { it.isNotBlank() }
        } else {
            null to base
        }
        return Song(
            id = idFor(fileUrl),
            title = title ?: base,
            artistsText = artist ?: "Unknown Artist",
            durationText = null,
            thumbnailUrl = null
        )
    }
}
