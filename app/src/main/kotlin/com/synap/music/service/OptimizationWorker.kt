package com.synap.music.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.synap.music.Database
import com.synap.music.enums.CompressionEngine
import com.synap.music.enums.SongSortBy
import com.synap.music.enums.SortOrder
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * The Optimization Worker is the app's "Storage Manager". 
 * It runs in the background (usually at night when charging) to ensure 
 * your library is stored in the most efficient way possible based on 
 * your current phone state.
 */
class OptimizationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "OptimizationWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting optimization pass...")
        
        // 1. Get all the songs in your local library.
        val songs = try {
            Database.songs(SongSortBy.Title, SortOrder.Ascending).first()
        } catch (_: Exception) {
            emptyList()
        }
        
        // 2. Figure out the "Ideal" format for right now (e.g. storage might be low).
        val targetEngine = CompressionManager.resolveEngine(context, CompressionEngine.Auto)
        
        for (song in songs) {
            val currentFormat = Database.format(song.id).first() ?: continue
            val currentEngine = currentFormat.compressionEngine ?: CompressionEngine.FLAC
            
            // 3. If a song is stored in a "worse" format than our ideal, re-transcode it.
            if (shouldRecompress(currentEngine, targetEngine)) {
                try {
                    recompress(song.id, currentEngine, targetEngine)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to recompress ${song.id}", e)
                }
            }
        }
        
        return Result.success()
    }

    /**
     * Logic to decide if we should upgrade/downgrade a file.
     * For example, if you have lots of space now, we might upgrade 
     * an old AAC file to a lossless WavPack file if we can re-fetch it.
     */
    private fun shouldRecompress(current: CompressionEngine, target: CompressionEngine): Boolean {
        val priorityMap = mapOf(
            CompressionEngine.FLAC to 0,
            CompressionEngine.WavPack to 1,
            CompressionEngine.AAC to 2
        )

        val currentPriority = priorityMap[current] ?: 0
        val targetPriority = priorityMap[target] ?: 0

        return targetPriority > currentPriority
    }

    /**
     * The actual transcoding logic. 
     * Uses FFmpeg to convert the physical file from one format to another 
     * without losing your playback history or "Liked" status.
     */
    private suspend fun recompress(songId: String, from: CompressionEngine, to: CompressionEngine) {
        val vaultDir = File(context.filesDir, "vault")
        val inputFile = File(vaultDir, "$songId.${CompressionManager.getExtension(from)}")
        val outputFile = File(vaultDir, "$songId.${CompressionManager.getExtension(to)}.tmp")

        if (!inputFile.exists()) return

        val args = mutableListOf<String>()
        args.add("-i")
        args.add(inputFile.absolutePath)
        args.addAll(CompressionManager.getFFmpegArgs(to))
        args.add("-y")
        args.add(outputFile.absolutePath)

        val result = CompressionManager.executeFFmpeg(context, args.toTypedArray())
        
        if (result == 0 && outputFile.exists()) {
            val finalOutputFile = File(vaultDir, "$songId.${CompressionManager.getExtension(to)}")
            outputFile.renameTo(finalOutputFile)
            inputFile.delete() // Cleanup the old, less efficient file.
            
            // Update the DB so the app knows the file has a new format.
            val format = Database.format(songId).first()
            if (format != null) {
                Database.upsert(format.copy(compressionEngine = to))
            }
            Log.d(TAG, "Successfully recompressed $songId from $from to $to")
        }
    }
}
