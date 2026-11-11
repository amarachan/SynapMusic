package com.synap.music.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.synap.music.Database
import com.synap.music.enums.CompressionEngine
import com.synap.music.models.Format
import com.synap.pulse.SynapService
import com.synap.pulse.requests.song
import com.synap.music.utils.asMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The Maintenance Healer is a background janitor. 
 * It ensures that the files on your phone and the entries in the database 
 * are always in sync. If a file exists but the DB forgot about it, this 
 * task restores it so it shows up in your library again.
 */
class MaintenanceHealer(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "MaintenanceHealer"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting Maintenance healing pass...")

        val vaultDir = File(context.filesDir, "vault").apply { if (!exists()) mkdirs() }
        val oldCacheDir = File(context.cacheDir, "audio_cache")

        /**
         * STEP 0: Migration.
         * If you just updated the app, we move files from the old "temporary" cache 
         * into the new permanent "Vault" so you don't have to re-download them.
         */
        if (oldCacheDir.exists()) {
            oldCacheDir.listFiles()?.filter { it.isFile }?.forEach { file ->
                val targetFile = File(vaultDir, file.name)
                if (!targetFile.exists()) {
                    file.copyTo(targetFile)
                    Log.d(TAG, "Migrated file to vault: ${file.name}")
                }
                file.delete()
            }
            oldCacheDir.deleteRecursively()
        }

        /**
         * STEP 1: Orphan Recovery.
         * We look for files in the Vault that aren't mentioned in the Database.
         * This can happen if the app crashed during a download or if the DB was reset.
         */
        val vaultFiles = vaultDir.listFiles()?.filter { it.isFile } ?: emptyList()
        for (file in vaultFiles) {
            val videoId = file.nameWithoutExtension
            val format = Database.format(videoId).first()
            if (format == null || format.contentLength == null || format.contentLength == 0L) {
                Log.d(TAG, "Found orphan or incomplete record for file: ${file.name}. Recovering metadata...")
                recoverMetadata(videoId, file)
            }
        }

        /**
         * STEP 2: Metadata Repair.
         * We look for songs that have "Unknown" in their title. 
         * We try to hit the server one last time to get the proper song name 
         * and professional album art.
         */
        val songs = Database.search("Unknown").first()
        for (song in songs) {
            if (song.title.contains("Unknown", ignoreCase = true) || song.artistsText == null) {
                Log.d(TAG, "Song ${song.id} has low-quality metadata. Repairing...")
                SynapService.song(song.id)?.getOrNull()?.let { recovered ->
                    Database.insert(recovered.asMediaItem)
                }
            }
        }

        Result.success()
    }

    /**
     * Recover Metadata logic.
     * Hits the YouTube API to get the official title/artist/artwork 
     * for an orphaned physical file.
     */
    private suspend fun recoverMetadata(videoId: String, file: File) {
        SynapService.song(videoId)?.getOrNull()?.let { recovered ->
            val mediaItem = recovered.asMediaItem

            // Use the transaction insert to restore all links (Album/Artist)
            Database.insert(mediaItem)

            val engine = when (file.extension.lowercase()) {
                "flac" -> CompressionEngine.FLAC
                "mkv" -> CompressionEngine.WavPack
                "m4a" -> CompressionEngine.AAC
                "opus" -> CompressionEngine.Opus
                else -> null
            }

            val format = Format(
                songId = videoId,
                contentLength = file.length(),
                mimeType = "audio/${file.extension}",
                compressionEngine = engine
            )
            Database.upsert(format)
            Log.d(TAG, "Recovered metadata for: ${mediaItem.mediaMetadata.title}")
        }
    }
}
