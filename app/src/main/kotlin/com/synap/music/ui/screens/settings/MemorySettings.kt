package com.synap.music.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import com.synap.music.Database
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.enums.CoilDiskCacheMaxSize
import com.synap.music.enums.ExoPlayerDiskCacheMaxSize
import com.synap.music.internal
import com.synap.music.path
import com.synap.music.query
import com.synap.music.service.PlayerService
import com.synap.music.ui.components.themed.ConfirmationDialog
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.platform.LocalLayoutDirection
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.MainApplication
import com.synap.music.cubes.AudioCubeManager
import com.synap.music.enums.CubeCompressionLevel
import com.synap.music.enums.CompressionEngine
import com.synap.music.models.Format
import com.synap.pulse.SynapService
import com.synap.pulse.requests.song
import com.synap.music.utils.asMediaItem
import kotlinx.coroutines.flow.first
import com.synap.music.utils.coilDiskCacheMaxSizeKey
import com.synap.music.utils.cubeCompressionLevelKey
import com.synap.music.utils.exoPlayerDiskCacheMaxSizeKey
import com.synap.music.utils.intent
import com.synap.music.utils.rememberPreference
import com.synap.music.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.github.luben.zstd.ZstdInputStream
import com.github.luben.zstd.ZstdOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.BufferedInputStream
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Memory and Cache Settings screen for disk cache management and SynapCube backup/restore options.
 */
@OptIn(ExperimentalCoilApi::class)
@ExperimentalAnimationApi
@Composable
fun MemorySettings() {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current

    // Cache preferences
    var coilDiskCacheMaxSize by rememberPreference(
        coilDiskCacheMaxSizeKey,
        CoilDiskCacheMaxSize.`128MB`
    )
    var exoPlayerDiskCacheMaxSize by rememberPreference(
        exoPlayerDiskCacheMaxSizeKey,
        ExoPlayerDiskCacheMaxSize.`2GB`
    )

    val coroutineScope = rememberCoroutineScope()
    var isClearingImageCache by remember { mutableStateOf(false) }
    var isClearingSongCache by remember { mutableStateOf(false) }
    var isClearingVault by remember { mutableStateOf(false) }

    var cacheRefreshTrigger by remember { mutableStateOf(0) }

    // Auto-refresh cache size after clearing
    LaunchedEffect(cacheRefreshTrigger) {
        if (cacheRefreshTrigger > 0) {
            repeat(5) {
                delay(200.milliseconds)
            }
        }
    }

    if (isClearingImageCache) {
        ConfirmationDialog(
            text = "Are you sure you want to clear the image cache? This will delete all downloaded thumbnails and artwork.",
            onDismiss = { isClearingImageCache = false },
            onConfirm = {
                Coil.imageLoader(context).diskCache?.clear()
                cacheRefreshTrigger++
                context.toast("Image cache cleared")
            }
        )
    }

    if (isClearingSongCache) {
        ConfirmationDialog(
            text = "Are you sure you want to clear the song cache? This will delete all temporarily cached songs. Offline downloads in the Vault will NOT be affected.",
            onDismiss = { isClearingSongCache = false },
            onConfirm = {
                binder?.cache?.let { cache ->
                    cache.keys.forEach { key ->
                        try {
                            cache.removeResource(key)
                        } catch (_: Exception) {}
                    }
                }
                cacheRefreshTrigger++
                context.toast("Song cache cleared")
            }
        )
    }

    if (isClearingVault) {
        ConfirmationDialog(
            text = "Are you sure you want to clear all offline downloads from the Vault? This will permanently delete all downloaded songs from your device.",
            onDismiss = { isClearingVault = false },
            onConfirm = {
                val vaultDir = File(context.filesDir, "vault")
                if (vaultDir.exists()) {
                    vaultDir.listFiles()?.forEach { file ->
                        file.delete()
                    }
                }
                query {
                    Database.allFormats().forEach { format ->
                        Database.deleteFormat(format.songId)
                    }
                }
                cacheRefreshTrigger++
                context.toast("Offline Vault downloads cleared")
            }
        )
    }

    // Database state
    val eventsCount by remember {
        Database.eventsCount().distinctUntilChanged()
    }.collectAsState(initial = 0)

    /**
     * Backup Engine.
     * We export the database using Zstandard (Zstd) compression. 
     * This creates a tiny, portable '.synap' file that contains your 
     * entire library metadata and preferences.
     */
    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            query {
                // Ensure the database is "clean" before copying the file.
                Database.checkpoint()

                context.applicationContext.contentResolver.openOutputStream(uri)
                    ?.use { outputStream ->
                        ZstdOutputStream(outputStream).use { zstdStream ->
                            FileInputStream(Database.internal.path).use { inputStream ->
                                inputStream.copyTo(zstdStream)
                            }
                        }
                    }
            }
        }

    /**
     * Restore Engine.
     * It intelligently detects if a file is compressed (Zstd) or a 
     * raw SQLite file and restores it. The app is force-closed after 
     * restore to ensure a clean reload of the database.
     */
    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            query {
                Database.checkpoint()
                Database.internal.close()

                context.applicationContext.contentResolver.openInputStream(uri)
                    ?.use { inputStream ->
                        val bufferedInput = BufferedInputStream(inputStream)
                        bufferedInput.mark(4)
                        val magic = ByteArray(4)
                        bufferedInput.read(magic)
                        bufferedInput.reset()

                        // Check for Zstd magic header: 28 B5 2F FD
                        val isZstd = magic[0] == 0x28.toByte() && magic[1] == 0xB5.toByte() && 
                                     magic[2] == 0x2F.toByte() && magic[3] == 0xFD.toByte()

                        FileOutputStream(Database.internal.path).use { outputStream ->
                            if (isZstd) {
                                ZstdInputStream(bufferedInput).use { zstdStream ->
                                    zstdStream.copyTo(outputStream)
                                }
                            } else {
                                bufferedInput.copyTo(outputStream)
                            }
                        }
                    }

                // Close the background service and exit to force a full refresh.
                context.stopService(context.intent<PlayerService>())
                exitProcess(0)
            }
        }

    var cubeCompressionLevel by rememberPreference(cubeCompressionLevelKey, CubeCompressionLevel.Auto)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let {
            MainApplication.scope.launch {
                val result = AudioCubeManager.exportLibraryToCube(context, uri, cubeCompressionLevel)
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        context.toast("Full Library Audio Cube exported successfully!")
                    } else {
                        context.toast("Failed to export Audio Cube")
                    }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.toast("Importing Audio Cube...")
            MainApplication.scope.launch {
                val result = AudioCubeManager.importCube(context, uri)
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        context.toast("Audio Cube imported successfully!")
                    } else {
                        context.toast("Failed to import Audio Cube")
                    }
                }
            }
        }
    }

    val paddingValues = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current

    Column(
        modifier = Modifier
            .background(colorPalette.background0)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                PaddingValues(
                    start = paddingValues.calculateStartPadding(layoutDirection),
                    top = paddingValues.calculateTopPadding() + 72.dp,
                    end = paddingValues.calculateEndPadding(layoutDirection),
                    bottom = paddingValues.calculateBottomPadding()
                )
            )
    ) {
        // CACHE SECTION
        Coil.imageLoader(context).diskCache?.let { diskCache ->
            val diskCacheSize = remember(diskCache, cacheRefreshTrigger) { diskCache.size }

            SettingsGroupCard(title = "IMAGE CACHE") {
                SettingsDescription(
                    text = "${Formatter.formatShortFileSize(context, diskCacheSize)} used (${diskCacheSize * 100 / coilDiskCacheMaxSize.bytes.coerceAtLeast(1)}%)"
                )
                EnumValueSelectorSettingsEntry(
                    title = "Max image size",
                    selectedValue = coilDiskCacheMaxSize,
                    onValueSelected = { coilDiskCacheMaxSize = it }
                )
                SettingsEntry(
                    title = "Clear image cache",
                    text = "Delete all temporary thumbnails",
                    onClick = { isClearingImageCache = true }
                )
            }
        }

        binder?.cache?.let { cache ->
            val diskCacheSize by remember(cacheRefreshTrigger) { derivedStateOf { cache.cacheSpace } }

            SettingsGroupCard(title = "SONG CACHE") {
                SettingsDescription(
                    text = buildString {
                        append(Formatter.formatShortFileSize(context, diskCacheSize))
                        append(" used")
                        when (val size = exoPlayerDiskCacheMaxSize) {
                            ExoPlayerDiskCacheMaxSize.Unlimited -> {}
                            else -> append(" (${diskCacheSize * 100 / size.bytes}%)")
                        }
                    }
                )
                EnumValueSelectorSettingsEntry(
                    title = "Max song size",
                    selectedValue = exoPlayerDiskCacheMaxSize,
                    onValueSelected = { exoPlayerDiskCacheMaxSize = it }
                )
                SettingsEntry(
                    title = "Clear song cache",
                    text = "Delete all temporary audio data",
                    onClick = { isClearingSongCache = true }
                )
            }
        }

        // OFFLINE VAULT SECTION
        val vaultDir = remember { File(context.filesDir, "vault") }
        val vaultFiles = remember(cacheRefreshTrigger) { 
            if (vaultDir.exists()) vaultDir.listFiles()?.filter { it.isFile } ?: emptyList() else emptyList() 
        }
        val vaultSize = remember(vaultFiles) { vaultFiles.sumOf { it.length() } }
        val vaultCount = vaultFiles.size

        SettingsGroupCard(title = "OFFLINE VAULT") {
            SettingsDescription(
                text = if (vaultCount > 0) {
                    "${Formatter.formatShortFileSize(context, vaultSize)} used ($vaultCount downloaded ${if (vaultCount == 1) "song" else "songs"})"
                } else {
                    "No offline songs downloaded"
                }
            )

            SettingsEntry(
                title = "Heal & Scan Vault Library",
                text = "Scan storage for missing or orphaned songs and restore metadata",
                onClick = {
                    context.toast("Scanning Vault storage...")
                    coroutineScope.launch(Dispatchers.IO) {
                        val dir = File(context.filesDir, "vault")
                        if (dir.exists()) {
                            val files = dir.listFiles()?.filter { it.isFile } ?: emptyList()
                            var recoveredCount = 0
                            for (file in files) {
                                val videoId = file.nameWithoutExtension
                                val format = Database.format(videoId).first()
                                if (format == null || format.contentLength == null || format.contentLength == 0L) {
                                    SynapService.song(videoId)?.getOrNull()?.let { recovered ->
                                        val mediaItem = recovered.asMediaItem
                                        Database.insert(mediaItem)
                                        val engine = when (file.extension.lowercase()) {
                                            "flac" -> CompressionEngine.FLAC
                                            "mkv" -> CompressionEngine.WavPack
                                            "m4a" -> CompressionEngine.AAC
                                            "opus" -> CompressionEngine.Opus
                                            else -> null
                                        }
                                        val newFormat = Format(
                                            songId = videoId,
                                            contentLength = file.length(),
                                            mimeType = "audio/${file.extension}",
                                            compressionEngine = engine
                                        )
                                        Database.upsert(newFormat)
                                        recoveredCount++
                                    }
                                }
                            }
                            withContext(Dispatchers.Main) {
                                cacheRefreshTrigger++
                                if (recoveredCount > 0) {
                                    context.toast("Successfully recovered $recoveredCount missing Vault songs!")
                                } else {
                                    context.toast("Vault library is healthy! No missing tracks found.")
                                }
                            }
                        }
                    }
                }
            )

            if (vaultCount > 0) {
                SettingsEntry(
                    title = "Clear offline downloads",
                    text = "Delete all downloaded songs from Vault",
                    onClick = { isClearingVault = true }
                )
            }
        }

        // DATABASE SECTION
        SettingsGroupCard(title = "DATABASE & DISCOVERY") {
            SettingsEntry(
                title = "Reset Discover & Recap Stats",
                text = if (eventsCount > 0) "Delete $eventsCount playback events & stats" else "Playback events and stats cleared",
                isEnabled = eventsCount > 0,
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        Database.clearEvents()
                        Database.clearTotalPlayTimeMs()
                    }
                }
            )
        }

        SettingsGroupCard(title = "BACKUP & RESTORE") {
            SettingsDescription(text = "Personal preferences (i.e. theme mode) and cache are excluded.")

            SettingsEntry(
                title = "Backup Library",
                text = "Export a compressed (.synap) backup",
                onClick = {
                    @SuppressLint("SimpleDateFormat")
                    val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")
                    try {
                        backupLauncher.launch("synapmusic_${dateFormat.format(Date())}.synap")
                    } catch (e: ActivityNotFoundException) {
                        context.toast("Couldn't find an application to create documents")
                    }
                }
            )

            ImportantSettingsDescription(text = "Existing data will be overwritten upon restore. The app will restart.")

            SettingsEntry(
                title = "Restore Library",
                text = "Import a .synap library file",
                onClick = {
                    try {
                        restoreLauncher.launch(arrayOf("application/octet-stream", "application/vnd.sqlite3", "application/x-sqlite3"))
                    } catch (e: ActivityNotFoundException) {
                        context.toast("Couldn't find an application to open documents")
                    }
                }
            )
        }

        // AUDIO CUBES SECTION
        SettingsGroupCard(title = "AUDIO CUBES") {
            EnumValueSelectorSettingsEntry(
                title = "Compression Level",
                selectedValue = cubeCompressionLevel,
                onValueSelected = { cubeCompressionLevel = it },
                valueText = { it.name }
            )

            SettingsDescription(text = "Export your entire offline library into a compressed .synapcube archive.")

            SettingsEntry(
                title = "Export Offline Library",
                text = "Create a .synapcube file",
                onClick = {
                    exportLauncher.launch("MyLibrary.synapcube")
                }
            )

            SettingsEntry(
                title = "Import Audio Cube",
                text = "Restore or add a .synapcube file",
                onClick = {
                    importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                }
            )
        }
    }
}
