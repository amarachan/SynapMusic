package com.synap.music.ffmpeg

import android.content.Context
import android.system.Os
import android.util.Log
import androidx.core.content.edit
import com.yausername.youtubedl_android.YoutubeDLException
import java.io.File
import java.util.zip.ZipFile

class FFmpeg {
    companion object {
        private const val TAG = "FFmpeg"
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

        if ((!ffmpegDir.exists()) || (currentVersion != ffmpegSize)) {
            deleteRecursive(ffmpegDir)
            ffmpegDir.mkdirs()
            try {
                unzip(ffmpegLib, ffmpegDir)
                restoreSymlinks(ffmpegDir)
            } catch (e: Exception) {
                deleteRecursive(ffmpegDir)
                throw YoutubeDLException("failed to initialize ffmpeg", e)
            }
            prefs.edit { putString("ffmpegLibVersion", ffmpegSize) }
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

    /**
     * ZipFile extracts symlink entries as small plain text files containing the target filename.
     * On Linux/Android, the dynamic linker requires real symlinks (or ELF file copies).
     */
    private fun restoreSymlinks(directory: File) {
        val libDir = File(directory, "usr/lib")
        if (!libDir.exists() || !libDir.isDirectory) return

        libDir.listFiles()?.forEach { file ->
            if (file.isFile && (file.length() in (1..100))) {
                try {
                    val targetName = file.readText().trim()
                    val targetFile = File(libDir, targetName)
                    if (targetFile.exists() && (targetFile.length() > 100)) {
                        file.delete()
                        try {
                            Os.symlink(targetName, file.absolutePath)
                        } catch (e: Exception) {
                            Log.w(TAG, "Os.symlink failed, falling back to copy", e)
                            targetFile.copyTo(file, overwrite = true)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to resolve symlink for ${file.name}", e)
                }
            }
        }
    }
}
