package com.yausername.ffmpeg

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDLException
import java.io.File
import java.util.zip.ZipFile

class FFmpeg {
    companion object {
        private val instance = FFmpeg()
        @JvmStatic
        fun getInstance() = instance
    }

    private var initialized = false

    @Synchronized
    @Throws(YoutubeDLException::class)
    fun init(appContext: Context) {
        if (initialized) return

        val baseDir = File(appContext.noBackupFilesDir, "youtubedl-android")
        val packagesDir = File(baseDir, "packages")
        val ffmpegDir = File(packagesDir, "ffmpeg")
        val binDir = File(appContext.applicationInfo.nativeLibraryDir)
        val ffmpegLib = File(binDir, "libffmpeg.zip.so")

        if (!ffmpegLib.exists()) {
            // If the zip doesn't exist, we might be using a static build or it's missing
            initialized = true
            return
        }

        // Using size as version
        val ffmpegSize = ffmpegLib.length().toString()
        val prefs = appContext.getSharedPreferences("youtubedl-android", Context.MODE_PRIVATE)
        val currentVersion = prefs.getString("ffmpegLibVersion", null)

        if (!ffmpegDir.exists() || currentVersion != ffmpegSize) {
            deleteRecursive(ffmpegDir)
            ffmpegDir.mkdirs()
            try {
                unzip(ffmpegLib, ffmpegDir)
            } catch (e: Exception) {
                deleteRecursive(ffmpegDir)
                throw YoutubeDLException("failed to initialize ffmpeg", e)
            }
            prefs.edit().putString("ffmpegLibVersion", ffmpegSize).apply()
        }
        initialized = true
    }

    private fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.forEach { deleteRecursive(it) }
        }
        fileOrDirectory.delete()
    }

    private fun unzip(zipFile: File, targetDirectory: File) {
        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val destFile = File(targetDirectory, entry.name)
                if (entry.isDirectory) {
                    destFile.mkdirs()
                } else {
                    destFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }
}
