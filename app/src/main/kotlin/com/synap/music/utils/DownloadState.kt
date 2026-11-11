package com.synap.music.utils

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf

object DownloadState {
    private const val TAG = "DownloadState"
    /**
     * Maps mediaId to current download progress (0.0 to 100.0)
     * -1.0f indicates a "Starting/Preparing" state
     */
    val downloads = mutableStateMapOf<String, Float>()

    fun updateProgress(mediaId: String, progress: Float) {
        // We only update the state map if the change is significant (at least 1%)
        // or if it's a critical state transition (Start, 0%, 100%).
        val current = downloads[mediaId] ?: -2f
        
        val isCriticalTransition = (progress < 0f && current >= 0f) || (progress >= 0f && current < 0f)
        val isSignificantChange = progress == 0f || progress == 100f || (progress - current >= 1f)

        if (isCriticalTransition || isSignificantChange) {
            downloads[mediaId] = progress
            if (progress >= 0f) {
                Log.d(TAG, "Download progress for $mediaId: $progress%")
            } else {
                Log.d(TAG, "Download starting for $mediaId")
            }
        }
    }

    fun clearProgress(mediaId: String) {
        Log.d(TAG, "Download cleared for $mediaId")
        downloads.remove(mediaId)
    }
}
