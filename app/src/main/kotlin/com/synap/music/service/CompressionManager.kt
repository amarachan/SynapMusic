package com.synap.music.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.util.Log
import com.synap.music.enums.CompressionEngine
import com.synap.music.enums.AudioQuality
import com.synap.music.utils.audioQualityKey
import com.synap.music.utils.compressionEngineKey
import com.synap.music.utils.getEnum
import com.synap.music.utils.preferences
import com.synap.music.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The CompressionManager is the app's "Hardware Expert".
 * It looks at your phone's current state (Battery, RAM, Storage) and 
 * decides which audio codec will give you the best experience without 
 * killing your battery or filling up your phone.
 */
object CompressionManager {
    private const val TAG = "CompressionManager"

    data class DeviceProfile(
        val batteryPct: Float,
        val ramGb: Float,
        val cpuCores: Int,
        val gigaBytesAvailable: Long,
    )

    /**
     * Probes the Android system to see what kind of hardware we're dealing with.
     */
    private fun getDeviceProfile(context: Context): DeviceProfile {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = (level / scale.toFloat()) * 100

        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val ramGb = memInfo.totalMem / (1024f * 1024f * 1024f)

        val cpuCores = Runtime.getRuntime().availableProcessors()

        val stat = StatFs(Environment.getDataDirectory().path)
        val gigaBytesAvailable = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024 * 1024)

        return DeviceProfile(batteryPct, ramGb, cpuCores, gigaBytesAvailable)
    }

    /**
     * Resolve Engine.
     * If the user chooses "Auto", we pick the best codec based on their hardware:
     * - Low battery? Use FLAC (decoded by the hardware chip, saves power).
     * - Low storage? Use AAC (tiny files, great quality).
     * - High-end phone? Use WavPack/SAC (maximum audiophile fidelity).
     */
    enum class TaskType {
        Streaming,
        VaultDownload,
    }

    class ContextualScenario(
        val isWifi: Boolean,
        val isMetered: Boolean,
        val isRoaming: Boolean,
        val isCharging: Boolean,
        val batteryPct: Float,
        val isPowerSaveMode: Boolean,
        val freeStorageGb: Long,
        val ramGb: Float,
        val cpuCores: Int,
    )

    data class SmartAudioDecision(
        val networkFormatOption: String,
        val compressionEngine: CompressionEngine,
        val scenarioName: String,
    )

    /**
     * Contextual Probe: Evaluates real-time Network, Power, and Hardware specs.
     */
    fun probeContextualScenario(context: Context): ContextualScenario {
        // Network
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connManager?.activeNetwork
        val caps = activeNetwork?.let { connManager.getNetworkCapabilities(it) }

        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isMetered = connManager?.isActiveNetworkMetered == true
        val isRoaming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING) == false
        } else {
            @Suppress("DEPRECATION")
            connManager?.activeNetworkInfo?.isRoaming == true
        }

        // Power & Battery
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING) || (status == BatteryManager.BATTERY_STATUS_FULL)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = (level / scale.toFloat()) * 100

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSaveMode = powerManager?.isPowerSaveMode == true

        // Hardware Specs
        val (_, ramGb, cpuCores, gigaBytesAvailable) = getDeviceProfile(context)

        return ContextualScenario(
            isWifi = isWifi,
            isMetered = isMetered,
            isRoaming = isRoaming,
            isCharging = isCharging,
            batteryPct = batteryPct,
            isPowerSaveMode = isPowerSaveMode,
            freeStorageGb = gigaBytesAvailable,
            ramGb = ramGb,
            cpuCores = cpuCores,
        )
    }

    /**
     * Smart Evaluation Engine:
     * Evaluates Scenario (Network + Battery + Power Mode) + Hardware Specs (RAM + CPU + Free Storage).
     */
    fun evaluateSmartDecision(
        context: Context,
        @Suppress("UNUSED_PARAMETER") taskType: TaskType = TaskType.VaultDownload,
    ): SmartAudioDecision {
        val preferredQuality = context.preferences.getEnum(audioQualityKey, AudioQuality.Auto)
        val preferredEngine = context.preferences.getEnum(compressionEngineKey, CompressionEngine.Auto)

        val scenario = probeContextualScenario(context)

        // 1. Resolve Network Stream Format Option (-f)
        val networkFormatOption = if (preferredQuality != AudioQuality.Auto) {
            when (preferredQuality) {
                AudioQuality.Low -> "bestaudio[abr<=64]/ba[abr<=64]/bestaudio/best"
                AudioQuality.Medium -> "bestaudio[abr<=92]/ba[abr<=92]/bestaudio/best"
                AudioQuality.Normal -> "bestaudio[abr<=128]/ba[abr<=128]/bestaudio/best"
                AudioQuality.High -> "bestaudio[abr<=160]/ba[abr<=160]/bestaudio/best"
                AudioQuality.Ultra -> "bestaudio[abr<=192]/ba[abr<=192]/bestaudio/best"
                AudioQuality.Surround51 -> "bestaudio[abr<=256]/ba[abr<=256]/bestaudio/best"
                AudioQuality.Surround71 -> "bestaudio/best"
                else -> "bestaudio/best"
            }
        } else {
            when {
                // Emergency or Roaming: Low Quality Data Saver (64k)
                (scenario.isRoaming || scenario.isPowerSaveMode || (scenario.batteryPct < 15f)) -> {
                    "bestaudio[abr<=64]/ba[abr<=64]/bestaudio/best"
                }
                // Cellular / Metered Data: Normal Data Saver (128k)
                (scenario.isMetered || (!scenario.isWifi)) -> {
                    "bestaudio[abr<=128]/ba[abr<=128]/bestaudio/best"
                }
                // Wi-Fi: Unrestricted High Quality
                else -> "bestaudio/best"
            }
        }

        // 2. Resolve Local Compression Engine
        val targetEngine = if (preferredEngine != CompressionEngine.Auto) {
            preferredEngine
        } else {
            when {
                // Low Battery / Power Save Mode: Use Opus for fast, zero-transcode power saving & 85% storage reduction
                (scenario.batteryPct < 15f) || scenario.isPowerSaveMode -> CompressionEngine.Opus

                // Storage Critical (< 3 GB): Use AAC for minimal file size footprint
                scenario.freeStorageGb < 3 -> CompressionEngine.AAC

                // Storage Constrained (< 8 GB): Use Opus for high-efficiency compression
                scenario.freeStorageGb < 8 -> CompressionEngine.Opus

                // High-End Device & Charging / Unlimited: Use SAC/WavPack
                scenario.isCharging && (scenario.ramGb >= 6f) && (scenario.cpuCores >= 8) -> CompressionEngine.SAC

                // High Performance Hardware: Use SQZ (FLAC Level 8 max compression)
                (scenario.ramGb >= 6f) && (scenario.cpuCores >= 6) -> CompressionEngine.SQZ

                // Standard Mid-Range Device: Use WavPack
                else -> CompressionEngine.WavPack
            }
        }

        val scenarioName = when {
            scenario.isPowerSaveMode || (scenario.batteryPct < 15f) -> "Power Saver"
            scenario.isRoaming -> "Roaming Data Saver"
            scenario.isMetered || !scenario.isWifi -> "Cellular Data Saver"
            scenario.isCharging -> "High Performance (Charging)"
            else -> "Standard Auto"
        }

        Log.d(TAG, "Smart Strategy [$scenarioName]: NetworkFormat=$networkFormatOption, Engine=$targetEngine, FreeStorage=${scenario.freeStorageGb}GB, Battery=${scenario.batteryPct.toInt()}%")

        return SmartAudioDecision(networkFormatOption, targetEngine, scenarioName)
    }

    fun resolveEngine(context: Context, preferredEngine: CompressionEngine): CompressionEngine {
        if (preferredEngine != CompressionEngine.Auto) return preferredEngine
        return evaluateSmartDecision(context, TaskType.VaultDownload).compressionEngine
    }

    fun applyToRequest(context: Context, request: YoutubeDLRequest, engine: CompressionEngine) {
        val (networkFormatOption) = evaluateSmartDecision(context, TaskType.VaultDownload)
        if (engine.name.startsWith("Opus")) {
            request.addOption("-f", "bestaudio[ext=webm]/bestaudio[acodec=opus]/ba[acodec=opus]/$networkFormatOption")
        } else if (engine.name.startsWith("AAC")) {
            request.addOption("-f", "bestaudio[ext=m4a]/bestaudio[acodec=mp4a]/ba[acodec=mp4a]/$networkFormatOption")
        } else {
            request.addOption("-f", networkFormatOption)
        }
    }

    /**
     * Technical mapping of our custom engine names to actual FFmpeg CLI commands.
     * Always prepends "-vn" so that embedded/associated video streams are discarded
     * for pure audio transcoding.
     */
    fun getFFmpegArgs(engine: CompressionEngine): List<String> {
        val codecArgs = when (engine) {
            CompressionEngine.FLAC -> listOf("-c:a", "flac")
            CompressionEngine.WavPack -> listOf("-c:a", "wavpack")
            CompressionEngine.AAC -> listOf("-c:a", "aac", "-b:a", "256k")
            CompressionEngine.AAC192 -> listOf("-c:a", "aac", "-b:a", "192k")
            CompressionEngine.Opus64 -> listOf("-c:a", "opus", "-b:a", "64k", "-strict", "-2")
            CompressionEngine.Opus92 -> listOf("-c:a", "opus", "-b:a", "92k", "-strict", "-2")
            CompressionEngine.Opus -> listOf("-c:a", "opus", "-b:a", "128k", "-strict", "-2")
            CompressionEngine.Opus192 -> listOf("-c:a", "opus", "-b:a", "192k", "-strict", "-2")
            CompressionEngine.Opus256 -> listOf("-c:a", "opus", "-b:a", "256k", "-strict", "-2")
            CompressionEngine.Opus510 -> listOf("-c:a", "opus", "-b:a", "510k", "-strict", "-2")
            CompressionEngine.L3AC -> listOf("-c:a", "wavpack") 
            CompressionEngine.SLAC -> listOf("-c:a", "flac") 
            CompressionEngine.SQZ -> listOf("-c:a", "flac", "-compression_level", "8")
            CompressionEngine.SAC -> listOf("-c:a", "wavpack")
            else -> listOf("-c:a", "flac")
        }
        return listOf("-vn") + codecArgs
    }

    /**
     * FFmpeg Execution.
     * This is where we run the actual transcoding commands.
     * We have a priority system for finding the FFmpeg binary to ensure 
     * it works on as many Android versions and CPUs as possible.
     */
    suspend fun executeFFmpeg(context: Context, args: Array<String>): Int = withContext(Dispatchers.IO) {
        try {
            FFmpeg.getInstance().init(context)
        } catch (e: Exception) {
            Log.e(TAG, "FFmpeg init error", e)
        }

        val baseDir = File(context.noBackupFilesDir, "youtubedl-android")
        val packagesDir = File(baseDir, "packages")
        val ffmpegDir = File(packagesDir, "ffmpeg")
        val pythonDir = File(packagesDir, "python")
        val binDir = File(context.applicationInfo.nativeLibraryDir)

        // FFmpeg support libraries that must NOT be treated as the ffmpeg executable
        val ffmpegSupportLibs = setOf(
            "libavformat", "libavcodec", "libavutil", "libavfilter",
            "libavdevice", "libswresample", "libswscale", "libpostproc",
        )

        // Priority 1: Check native Library Dir for libffmpeg.so (Standard Android location for executable binary)
        // Priority 2: Check well-known paths for the extracted ffmpeg executable in packages
        // Priority 3: Recursively search packages for an ffmpeg executable (excluding support libs & zip archives)
        val ffmpegBin = File(binDir, "libffmpeg.so").takeIf { it.exists() }
            ?: File(ffmpegDir, "usr/bin/ffmpeg").takeIf { it.exists() }
            ?: File(ffmpegDir, "bin/ffmpeg").takeIf { it.exists() }
            ?: File(ffmpegDir, "ffmpeg").takeIf { it.exists() }
            ?: ffmpegDir.walkTopDown().firstOrNull { file ->
                file.isFile && file.name.contains("ffmpeg", ignoreCase = true)
                    && !file.name.endsWith(".zip.so")
                    && ffmpegSupportLibs.none { lib -> file.name.startsWith(lib, ignoreCase = true) }
            }

        if ((ffmpegBin == null) || (!ffmpegBin.exists())) {
            Log.e(TAG, "FFmpeg binary not found in $ffmpegDir or $binDir")
            return@withContext -1
        }

        Log.d(TAG, "Resolved FFmpeg binary: ${ffmpegBin.absolutePath}")

        // Only chmod files outside nativeLibraryDir to prevent Android SELinux denials
        if (ffmpegBin.parentFile?.absolutePath != binDir.absolutePath) {
            try {
                ffmpegBin.setExecutable(true, false)
            } catch (_: Exception) {}
        }

        val command = mutableListOf<String>()
        command.add(ffmpegBin.absolutePath)
        command.addAll(args)
        
        Log.d(TAG, "Executing FFmpeg: ${command.joinToString(" ")}")

        val pb = ProcessBuilder(command)
        val env = pb.environment()
        
        // Link all native directories including python/usr/lib for shared support libraries
        val ldLibraryPath = listOfNotNull(
            binDir.absolutePath,
            File(ffmpegDir, "usr/lib").takeIf { it.exists() }?.absolutePath,
            File(pythonDir, "usr/lib").takeIf { it.exists() }?.absolutePath,
            ffmpegBin.parentFile?.absolutePath,
            ffmpegBin.parentFile?.parentFile?.let { File(it, "lib").absolutePath },
            File(ffmpegDir, "usr/lib64").takeIf { it.exists() }?.absolutePath,
            File(ffmpegDir, "lib").takeIf { it.exists() }?.absolutePath,
            "/system/lib64",
            "/system/lib",
        ).asSequence().distinct().joinToString(":")

        env["LD_LIBRARY_PATH"] = ldLibraryPath
        env["PATH"] = (System.getenv("PATH") ?: "") + ":" + (ffmpegBin.parentFile?.absolutePath ?: "") + ":" + binDir.absolutePath

        try {
            val process = pb.start()
            
            // FFmpeg prints progress and metadata to stderr; log at debug level to avoid logcat spam.
            val outThread = Thread { process.inputStream.bufferedReader().forEachLine { Log.d(TAG, "FFmpeg: $it") } }
            val errThread = Thread { process.errorStream.bufferedReader().forEachLine { Log.d(TAG, "FFmpeg: $it") } }
            
            outThread.start()
            errThread.start()
            
            val exitCode = process.waitFor()
            outThread.join(500)
            errThread.join(500)
            
            Log.d(TAG, "FFmpeg finished with exit code: $exitCode")

            exitCode
        } catch (e: Exception) {
            Log.e(TAG, "FFmpeg execution failed", e)
            -1
        }
    }
    
    fun getExtension(engine: CompressionEngine): String {
        return when (engine) {
            CompressionEngine.FLAC, CompressionEngine.SLAC, CompressionEngine.SQZ -> "flac"
            CompressionEngine.WavPack, CompressionEngine.L3AC, CompressionEngine.SAC -> "mkv"
            CompressionEngine.AAC, CompressionEngine.AAC192 -> "m4a"
            CompressionEngine.Opus64, CompressionEngine.Opus92, CompressionEngine.Opus, CompressionEngine.Opus192, CompressionEngine.Opus256, CompressionEngine.Opus510 -> "opus"
            else -> "flac"
        }
    }
}
