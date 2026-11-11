/*
 * Copyright (C) 2026 SynapMusic Maintainers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.synap.music.utils

import android.content.Context
import android.util.Log
import com.synap.music.enums.CompressionEngine
import com.synap.music.service.CompressionManager
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import androidx.media3.common.MediaItem
import com.synap.music.Database
import com.synap.music.internal
import com.synap.music.query
import com.synap.music.models.Format
import com.synap.pulse.SynapService
import com.synap.pulse.requests.song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The MediaFetchUtils is the app's "Logistics Manager". 
 * It handles the actual work of getting audio data, whether that's 
 * streaming it from YouTube or downloading it into the high-fidelity Vault.
 */
object MediaFetchUtils {
    private const val TAG = "MediaFetchUtils"
    private val downloadSemaphore = Semaphore(permits = 2)

    private fun getAudioDir(context: Context): File {
        return File(context.filesDir, "vault").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Finds the physical file for a track on your phone's storage.
     * It checks all possible extensions because you might have 
     * different tracks saved in different lossless formats.
     */
    fun getAudioFile(context: Context, videoId: String, isVideo: Boolean = false, engine: CompressionEngine? = null): File {
        if (isVideo) return File(getAudioDir(context), "$videoId.mp4")
        
        // If an explicit engine is requested (e.g. during download), target that engine's exact extension
        if (engine != null) {
            val extension = CompressionManager.getExtension(engine)
            val targetFile = File(getAudioDir(context), "$videoId.$extension")
            if (targetFile.exists() && targetFile.length() > 0) return targetFile
            return targetFile
        }

        // 1. Check if the file is already there in any of our supported formats.
        val possibleExtensions = listOf("mkv", "flac", "m4a", "opus", "mp3", "ogg", "wv")
        for (ext in possibleExtensions) {
            val file = File(getAudioDir(context), "$videoId.$ext")
            if (file.exists() && file.length() > 0) return file
        }
        
        // 2. If it's a new download, figure out what the file extension SHOULD be.
        val preferred = context.preferences.getEnum(compressionEngineKey, CompressionEngine.Auto)
        val resolved = CompressionManager.resolveEngine(context, preferred)
        val extension = CompressionManager.getExtension(resolved)
        
        return File(getAudioDir(context), "$videoId.$extension")
    }

    /**
     * Fetches a direct, high-quality streaming URL for immediate playback.
     */
    suspend fun getStreamingUrl(context: Context, videoId: String, isVideo: Boolean): String? = withContext(Dispatchers.IO) {
        val decision = CompressionManager.evaluateSmartDecision(context, CompressionManager.TaskType.Streaming)
        val request = YoutubeDLRequest("https://www.youtube.com/watch?v=$videoId")

        if (isVideo) {
            request.addOption("-f", "bestvideo[height<=720]+bestaudio/best[height<=720]")
        } else {
            request.addOption("-f", decision.networkFormatOption)
        }
        request.addOption("-g") // "-g" tells yt-dlp to just output the URL.
        request.addCommonOptions()

        try {
            val response = YoutubeDL.getInstance().execute(request, null)
            val url = response.out.lines().firstOrNull { it.startsWith("http") }
            if (url != null) return@withContext url
        } catch (e: Exception) {
            Log.e(TAG, "YoutubeDL failed to get streaming URL for $videoId", e)
        }

        null
    }

    private fun YoutubeDLRequest.addCommonOptions() {
        addOption("--no-playlist")
        addOption("--no-check-certificate")
        addOption("--no-warnings")
        addOption("--extractor-args", "youtube:player-client=android,web,tv")
    }

    /**
     * The Download Engine.
     * This is a complex multi-stage process:
     * 1. Recovery: Fetch rich metadata if the current item is missing details.
     * 2. Download: Pull the raw stream from the server.
     * 3. Transcode: Convert it to your chosen audiophile format (FLAC, WavPack, etc.).
     * 4. Vault: Atomic database registration so the song shows up in your library.
     */
    suspend fun downloadMedia(context: Context, mediaItem: MediaItem, isVideo: Boolean): Boolean = withContext(Dispatchers.IO) {
        val videoId = mediaItem.mediaId
        
        // Show "..." right away so the user knows we're working on it. 
        // It takes a second for the actual download to start.
        DownloadState.updateProgress(videoId, -1f)

        // Metadata Recovery Pass. 
        // If the item we're downloading has weak metadata (like "Unknown Album"),
        // we hit the server one last time to try and find the official rich metadata.
        var finalMediaItem = mediaItem
        val lacksMetadata = mediaItem.mediaMetadata.albumTitle == null ||
                          mediaItem.mediaMetadata.artist == null ||
                          mediaItem.mediaMetadata.albumTitle.toString().contains("Unknown", ignoreCase = true)
        
        if (lacksMetadata && !isVideo) {
            Log.d(TAG, "Lacks metadata for $videoId. Performing SynapPulse Recovery Pass...")
            SynapService.song(videoId)?.getOrNull()?.let { recovered ->
                finalMediaItem = recovered.asMediaItem
                Log.d(TAG, "Metadata recovered for $videoId: ${finalMediaItem.mediaMetadata.title}")
            }
        }

        val preferredEngine = context.preferences.getEnum(compressionEngineKey, CompressionEngine.Auto)
        var targetEngine = if (isVideo) null else CompressionManager.resolveEngine(context, preferredEngine)
        var outputFile = getAudioFile(context, videoId, isVideo, targetEngine)
        
        // Instant Feedback: Add it to the DB immediately as a "pending" item.
        registerInVault(videoId, finalMediaItem, outputFile, targetEngine, isVideo, isPlaceholder = true)

        if (outputFile.exists() && outputFile.length() > 0) {
             Log.d(TAG, "Vault file already exists for $videoId. Re-registering...")
             registerInVault(videoId, finalMediaItem, outputFile, targetEngine, isVideo)
             return@withContext true
        }

        Log.d(TAG, "Queued vault save for $videoId. Target: ${outputFile.name}")
        
        downloadSemaphore.withPermit {
            // Signal that a download slot was acquired and we're starting the network request.
            DownloadState.updateProgress(videoId, 0f)

            val request = YoutubeDLRequest("https://www.youtube.com/watch?v=$videoId")
            
            val tempDir = File(context.cacheDir, "temp_dl_$videoId").apply { 
                if (exists()) deleteRecursively()
                mkdirs() 
            }

            if (isVideo) {
                request.addOption("-f", "bestvideo[height<=720]+bestaudio/best[height<=720]")
                request.addOption("-o", outputFile.absolutePath)
            } else {
                // High-fidelity download logic.
                CompressionManager.applyToRequest(context, request, targetEngine!!)
                request.addOption("-o", "${tempDir.absolutePath}/input.%(ext)s")
            }
            
            request.addCommonOptions()

            try {
                Log.d(TAG, "Executing yt-dlp for $videoId...")
                val response = YoutubeDL.getInstance().execute(request) { progress, _, _ ->
                    if (progress >= 0) {
                        DownloadState.updateProgress(videoId, progress)
                    }
                }
                
                var success = false
                if (response.exitCode == 0) {
                    if (isVideo) {
                        success = outputFile.exists() && outputFile.length() > 0
                    } else {
                        val downloadedFile = tempDir.listFiles()?.firstOrNull { it.isFile }
                        if (downloadedFile != null && downloadedFile.exists()) {
                            Log.d(TAG, "Download Success. Encoding $videoId to $targetEngine...")
                            
                            val ffmpegArgs = mutableListOf<String>()
                            ffmpegArgs.addAll(listOf("-y", "-i", downloadedFile.absolutePath))
                            ffmpegArgs.addAll(CompressionManager.getFFmpegArgs(targetEngine!!))
                            ffmpegArgs.add(outputFile.absolutePath)
                            
                            var ffmpegResponse = CompressionManager.executeFFmpeg(context, ffmpegArgs.toTypedArray())
                            
                            // Encoder fallback: If native 'opus' failed, try 'libopus' (and vice versa)
                            if (ffmpegResponse != 0 && targetEngine.name.startsWith("Opus")) {
                                 Log.w(TAG, "Primary FFmpeg opus encoder failed (Code $ffmpegResponse). Attempting fallback opus encoder.")
                                 val opusBitrate = when (targetEngine) {
                                     CompressionEngine.Opus64 -> "64k"
                                     CompressionEngine.Opus92 -> "92k"
                                     CompressionEngine.Opus -> "128k"
                                     CompressionEngine.Opus192 -> "192k"
                                     CompressionEngine.Opus256 -> "256k"
                                     CompressionEngine.Opus510 -> "510k"
                                     else -> "128k"
                                 }
                                 val fallbackArgs = arrayOf("-y", "-i", downloadedFile.absolutePath, "-c:a", "libopus", "-b:a", opusBitrate, outputFile.absolutePath)
                                 ffmpegResponse = CompressionManager.executeFFmpeg(context, fallbackArgs)
                            }

                            /**
                             * Resilience Fallback.
                             * If the audiophile transcoding fails (e.g., an FFmpeg crash), 
                             * we try a raw stream copy. If THAT fails, we do a pure Kotlin 
                             * file copy. We never want to lose your download.
                             */
                            if (ffmpegResponse == 139 || ffmpegResponse != 0) {
                                 Log.w(TAG, "FFmpeg failed (Code $ffmpegResponse). Attempting copy fallback.")
                                 val fallbackArgs = arrayOf("-y", "-i", downloadedFile.absolutePath, "-c:a", "copy", outputFile.absolutePath)
                                 val fallbackResponse = CompressionManager.executeFFmpeg(context, fallbackArgs)
                                 
                                 if (fallbackResponse == 0 && outputFile.exists() && outputFile.length() > 0) {
                                     success = true
                                     Log.d(TAG, "FFmpeg copy fallback success")
                                 } else {
                                     Log.w(TAG, "FFmpeg fallback failed. Attempting pure Kotlin copy.")
                                     try {
                                         val trueContainerFile = File(outputFile.parentFile, "$videoId.${downloadedFile.extension}")
                                         downloadedFile.copyTo(trueContainerFile, overwrite = true)
                                         outputFile = trueContainerFile
                                         success = trueContainerFile.exists() && trueContainerFile.length() > 0
                                         Log.d(TAG, "Pure Kotlin copy success as ${trueContainerFile.name}")
                                     } catch (e: Exception) {
                                         Log.e(TAG, "Pure Kotlin copy failed", e)
                                         success = false
                                     }
                                 }
                            } else {
                                 success = outputFile.exists() && outputFile.length() > 0
                            }
                        }
                    }
                }
                
                tempDir.deleteRecursively()
                
                if (success) {
                    val possibleExtensions = listOf("mkv", "flac", "m4a", "opus", "mp3", "ogg", "wv", "webm")
                    for (ext in possibleExtensions) {
                        if (ext != outputFile.extension) {
                            val staleFile = File(getAudioDir(context), "$videoId.$ext")
                            if (staleFile.exists()) staleFile.delete()
                        }
                    }
                    registerInVault(videoId, finalMediaItem, outputFile, targetEngine, isVideo)
                } else {
                    Log.e(TAG, "Download/Processing failed for $videoId")
                    if (outputFile.exists()) outputFile.delete()
                    query { Database.deleteFormat(videoId) }
                }
                DownloadState.clearProgress(videoId)
                success
            } catch (e: Exception) {
                Log.e(TAG, "Critical Failure for $videoId", e)
                if (outputFile.exists()) outputFile.delete()
                query { Database.deleteFormat(videoId) }
                DownloadState.clearProgress(videoId)
                false
            }
        }
    }

    /**
     * Atomic Vault Registration.
     * This links the physical audio file to the song record in our DB.
     * We trigger a manual "refresh" to make sure the library UI updates 
     * instantly without a flicker.
     */
    private fun registerInVault(videoId: String, mediaItem: MediaItem, outputFile: File, targetEngine: CompressionEngine?, isVideo: Boolean, isPlaceholder: Boolean = false) {
        val size = if (isPlaceholder) 0L else outputFile.length()
        
        if (size <= 0 && !isPlaceholder) {
            Log.e(TAG, "Vault Registration Failed: File size is 0 for $videoId")
            return
        }

        try {
            Database.insert(mediaItem)
            
            val format = Format(
                songId = videoId,
                contentLength = size,
                mimeType = if (isVideo) "video/mp4" else "audio/${outputFile.extension}",
                compressionEngine = targetEngine
            )
            Database.upsert(format)
            Database.checkpoint()
            
            // Force the Room database to tell the UI that something changed.
            try {
                Database.internal.invalidationTracker.refreshVersionsAsync()
            } catch (_: Exception) { }
        } catch (e: Exception) {
            Log.e(TAG, "Vault Registration Error for $videoId", e)
        }
    }
}
