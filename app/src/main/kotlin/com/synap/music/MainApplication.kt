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

package com.synap.music

import android.app.Application
import android.util.Log
import android.graphics.Bitmap
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.synap.music.utils.preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import com.synap.music.service.OptimizationWorker
import com.synap.music.listentogether.ListenTogether
import com.synap.music.service.MaintenanceHealer

/**
 * MainApplication is the app's "Command Center". 
 * It initializes all the major libraries (Database, YouTube Engine, Image Loader) 
 * as soon as the app process starts, ensuring everything is ready before the UI appears.
 */
class MainApplication : Application(), ImageLoaderFactory {
    companion object {
        lateinit var instance: MainApplication
        // A global coroutine scope that stays alive as long as the app is running.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Connect to the local Room database.
        DatabaseInitializer(this)
        ListenTogether.init(this)

        // Wake up the YouTube engine and FFmpeg (for high-end audio transcoding).
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
        } catch (e: Throwable) {
            Log.e("MainApplication", "failed to initialize youtubedl-android or ffmpeg", e)
        }

        // Try to update the YouTube engine in the background.
        // If the online update fails or times out, youtubedl-android uses its embedded built-in binary.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().updateYoutubeDL(this@MainApplication)
                Log.d("MainApplication", "YoutubeDL updated successfully")
            } catch (e: Exception) {
                Log.i("MainApplication", "yt-dlp online update check skipped, using built-in binary: ${e.message}")
            }
        }

        // Schedule periodic maintenance tasks.
        scheduleOptimizationWorker()
        scheduleMaintenanceHealer()
    }

    /**
     * The Maintenance Healer runs every 12 hours. 
     * It scans your library for orphans (files without DB entries) and fixes them.
     */
    private fun scheduleMaintenanceHealer() {
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<MaintenanceHealer>(
            12, java.util.concurrent.TimeUnit.HOURS
        ).setConstraints(
            androidx.work.Constraints.Builder()
                .setRequiresCharging(true) // Only run when you're charging to save battery.
                .build()
        ).build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MaintenanceHealer",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * The Optimization Worker runs once a day. 
     * It re-transcodes files if a better codec becomes available (e.g. storage freed up).
     */
    private fun scheduleOptimizationWorker() {
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<OptimizationWorker>(
            1, java.util.concurrent.TimeUnit.DAYS
        ).setConstraints(
            androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.UNMETERED) // Only use Wi-Fi.
                .setRequiresCharging(true)
                .build()
        ).build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "OptimizationWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Specialized Image Loader (Coil).
     * We use a dedicated 512MB disk cache for album artwork so that 
     * scrolling through your library is buttery smooth even without internet.
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .respectCacheHeaders(false)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .allowHardware(true)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil"))
                    .maxSizeBytes(512L * 1024 * 1024) // 512MB
                    .build()
            }
            .addLastModifiedToFileCacheKey(true)
            .networkObserverEnabled(true)
            .build()
    }
}
