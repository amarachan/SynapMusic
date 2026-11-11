package com.synap.music.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.net.toUri
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
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.synap.music.Database
import com.synap.music.enums.LyricsLanguage
import com.synap.music.enums.Region
import com.synap.music.utils.lyricsLanguageKey
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.query
import com.synap.music.service.PlayerMediaBrowserService
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.platform.LocalLayoutDirection
import com.synap.music.listentogether.ListenTogether
import androidx.core.content.edit
import com.synap.music.ui.components.themed.AppleMusicImportDialog
import com.synap.music.ui.components.themed.ConfirmationDialog
import com.synap.music.ui.components.themed.LiveRadioDialog
import com.synap.music.ui.components.themed.LocalFolderScanDialog
import com.synap.music.ui.screens.smartMixesRoute
import com.synap.music.ui.components.themed.ListenTogetherDialog
import com.synap.music.ui.components.themed.SpotifyImportDialog
import com.synap.compose.persist.persistMap
import com.synap.music.ui.components.themed.TextFieldDialog
import com.synap.music.webdav.WebDavClient
import com.synap.music.webdav.WebDavImportEngine
import com.synap.music.utils.webdavUrlKey
import com.synap.music.utils.webdavUsernameKey
import com.synap.music.utils.webdavPasswordKey
import com.synap.music.utils.isWebdavEnabledKey
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.synap.music.utils.auddApiKeyKey
import com.synap.music.utils.isListenTogetherEnabledKey
import com.synap.music.utils.isAppleMusicImportEnabledKey
import com.synap.music.utils.isLiveRadioEnabledKey
import com.synap.music.utils.isLocalStorageEnabledKey
import com.synap.music.utils.isSmartMixesEnabledKey
import com.synap.music.utils.isSpotifyImportEnabledKey
import com.synap.music.utils.isSynapRecapEnabledKey
import com.synap.music.utils.isMusicRecognitionEnabledKey
import com.synap.music.utils.discordTokenKey
import com.synap.music.utils.isDiscordRpcEnabledKey
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.utils.isAtLeastAndroid12
import com.synap.music.utils.isAtLeastAndroid6
import com.synap.music.utils.isIgnoringBatteryOptimizations
import com.synap.music.utils.isInvincibilityEnabledKey
import com.synap.music.utils.pauseSearchHistoryKey
import com.synap.music.utils.preferences
import com.synap.music.utils.regionKey
import com.synap.music.utils.rememberPreference
import com.synap.music.utils.toast
import kotlinx.coroutines.flow.distinctUntilChanged

@SuppressLint("BatteryLife")
@ExperimentalAnimationApi
@Composable
fun OtherSettings() {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current

    // Toggle Android Auto visibility by enabling/disabling the MediaBrowserService component
    var isAndroidAutoEnabled by remember {
        val component = ComponentName(context, PlayerMediaBrowserService::class.java)
        val disabledFlag = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val enabledFlag = PackageManager.COMPONENT_ENABLED_STATE_ENABLED

        mutableStateOf(
            value = context.packageManager.getComponentEnabledSetting(component) == enabledFlag,
            policy = object : SnapshotMutationPolicy<Boolean> {
                override fun equivalent(a: Boolean, b: Boolean): Boolean {
                    context.packageManager.setComponentEnabledSetting(
                        component,
                        if (b) enabledFlag else disabledFlag,
                        PackageManager.DONT_KILL_APP
                    )
                    return a == b
                }
            }
        )
    }

    var isInvincibilityEnabled by rememberPreference(isInvincibilityEnabledKey, false)

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(context.isIgnoringBatteryOptimizations)
    }

    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            isIgnoringBatteryOptimizations = context.isIgnoringBatteryOptimizations
        }

    var pauseSearchHistory by rememberPreference(pauseSearchHistoryKey, false)
    var region by rememberPreference(regionKey, Region.WORLDWIDE)
    var lyricsLanguage by rememberPreference(lyricsLanguageKey, LyricsLanguage.Auto)

    var isLocalStorageEnabled by rememberPreference(isLocalStorageEnabledKey, true)
    var isSmartMixesEnabled by rememberPreference(isSmartMixesEnabledKey, true)
    var isSynapRecapEnabled by rememberPreference(isSynapRecapEnabledKey, true)
    var isSpotifyImportEnabled by rememberPreference(isSpotifyImportEnabledKey, true)
    var isAppleMusicImportEnabled by rememberPreference(isAppleMusicImportEnabledKey, true)
    var isLiveRadioEnabled by rememberPreference(isLiveRadioEnabledKey, true)

    var isListenTogetherEnabled by rememberPreference(isListenTogetherEnabledKey, false)

    var webdavUrl by rememberPreference(webdavUrlKey, "")
    var webdavUsername by rememberPreference(webdavUsernameKey, "")
    var webdavPassword by rememberPreference(webdavPasswordKey, "")
    var isWebdavEnabled by rememberPreference(isWebdavEnabledKey, false)

    var isEditingWebdavUrl by remember { mutableStateOf(false) }
    var isEditingWebdavUsername by remember { mutableStateOf(false) }
    var isEditingWebdavPassword by remember { mutableStateOf(false) }

    var webdavSyncStatus by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var isDiscordRpcEnabled by rememberPreference(isDiscordRpcEnabledKey, false)
    var discordToken by rememberPreference(discordTokenKey, "")
    var isEditingDiscordToken by remember { mutableStateOf(false) }

    var auddApiKey by rememberPreference(auddApiKeyKey, "test")
    var isMusicRecognitionEnabled by rememberPreference(isMusicRecognitionEnabledKey, true)
    var isEditingAuddApiKey by remember { mutableStateOf(false) }

    var isShowingListenTogetherDialog by remember { mutableStateOf(false) }
    var isEditingCustomListenServerUrl by remember { mutableStateOf(false) }
    val customListenServerUrl by ListenTogether.customServerUrl.collectAsState()

    var isImportingSpotifyPlaylist by remember { mutableStateOf(false) }
    var isImportingAppleMusicPlaylist by remember { mutableStateOf(false) }
    var isShowingLiveRadioDialog by remember { mutableStateOf(false) }
    var isShowingLocalScanDialog by remember { mutableStateOf(false) }
    var isShowingResetConfirmation by remember { mutableStateOf(false) }

    val queriesCount by remember {
        Database.queriesCount().distinctUntilChanged()
    }.collectAsState(initial = 0)

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
        SettingsGroupCard(title = "PLAYLIST TILES & FEATURES") {
            SettingsDescription(text = "Enable or disable feature tiles displayed in your Playlists grid.")

            SwitchSettingEntry(
                title = "Local Storage Scanner",
                text = "Show Local Storage tile in Playlists",
                isChecked = isLocalStorageEnabled,
                onCheckedChange = { isLocalStorageEnabled = it }
            )

            SwitchSettingEntry(
                title = "Smart Mixes Generator",
                text = "Show Smart Mixes tile in Playlists",
                isChecked = isSmartMixesEnabled,
                onCheckedChange = { isSmartMixesEnabled = it }
            )

            SwitchSettingEntry(
                title = "Synap Recap Analytics",
                text = "Show Synap Recap tile in Playlists",
                isChecked = isSynapRecapEnabled,
                onCheckedChange = { isSynapRecapEnabled = it }
            )

            SwitchSettingEntry(
                title = "Spotify Playlist Import",
                text = "Show Import Spotify tile in Playlists",
                isChecked = isSpotifyImportEnabled,
                onCheckedChange = { isSpotifyImportEnabled = it }
            )

            SwitchSettingEntry(
                title = "Apple Music Playlist Import",
                text = "Show Import Apple Music tile in Playlists",
                isChecked = isAppleMusicImportEnabled,
                onCheckedChange = { isAppleMusicImportEnabled = it }
            )

            SwitchSettingEntry(
                title = "Live Internet Radio",
                text = "Show Live Radio tile in Playlists",
                isChecked = isLiveRadioEnabled,
                onCheckedChange = { isLiveRadioEnabled = it }
            )
        }
        SettingsGroupCard(title = "LOCAL DEVICE STORAGE") {
            SettingsDescription(text = "Scan /Music, SD Card, and Downloads folders to index your local MP3, FLAC, and WAV files.")

            SettingsEntry(
                title = "Scan Local Storage Folders",
                text = "Index local phone audio files into Vault and library",
                onClick = { isShowingLocalScanDialog = true }
            )
        }

        SettingsGroupCard(title = "SMART AUTO-PLAYLISTS") {
            SettingsDescription(text = "Generate dynamic mood and filter mixes based on your listening habits and local library.")

            SettingsEntry(
                title = "Smart Mixes Generator",
                text = "Generate Most Played, High Energy, Chill Vibes, or Vault Mixes",
                onClick = { smartMixesRoute.global() }
            )
        }

        SettingsGroupCard(title = "LIVE INTERNET RADIO") {
            SettingsDescription(text = "Browse and stream thousands of free live FM/AM and web radio stations worldwide.")

            SettingsEntry(
                title = "Browse Live Radio Stations",
                text = "Search stations by name, genre, or country",
                onClick = { isShowingLiveRadioDialog = true }
            )
        }

        SettingsGroupCard(title = "LISTEN TOGETHER") {
            SettingsDescription(text = "Sync playback with friends in real-time. Shows a room icon in top bar when enabled.")

            SwitchSettingEntry(
                title = "Enable Listen Together",
                text = "Show party icon in top bar and enable room features",
                isChecked = isListenTogetherEnabled,
                onCheckedChange = { isListenTogetherEnabled = it }
            )

            if (isListenTogetherEnabled) {
                SettingsEntry(
                    title = "Listen Together Room",
                    text = "Create or join a synchronized listening party",
                    onClick = { isShowingListenTogetherDialog = true }
                )

                SettingsEntry(
                    title = "Listen Together Server URL",
                    text = customListenServerUrl.ifBlank { "Default server (tap to set custom URL)" },
                    onClick = { isEditingCustomListenServerUrl = true }
                )
            }
        }

        SettingsGroupCard(title = "SPOTIFY & APPLE MUSIC IMPORT") {
            SettingsDescription(text = "Import public Spotify or Apple Music playlists directly into SynapMusic by pasting a playlist link.")

            SettingsEntry(
                title = "Import Spotify Playlist",
                text = "Convert and save a Spotify playlist to your local library",
                onClick = { isImportingSpotifyPlaylist = true }
            )

            SettingsEntry(
                title = "Import Apple Music Playlist",
                text = "Convert and save an Apple Music playlist to your local library",
                onClick = { isImportingAppleMusicPlaylist = true }
            )
        }

        SettingsGroupCard(title = "MUSIC RECOGNITION") {
            SettingsDescription(text = "Identify ambient songs using AudD. Shows a soundwave icon in the top bar when enabled.")

            SwitchSettingEntry(
                title = "Enable Music Recognition",
                text = "Show soundwave icon in top bar to identify playing music",
                isChecked = isMusicRecognitionEnabled,
                onCheckedChange = { isMusicRecognitionEnabled = it }
            )

            if (isMusicRecognitionEnabled) {
                SettingsEntry(
                    title = "AudD API Token",
                    text = if (auddApiKey.isNotBlank() && auddApiKey != "test") "Custom API key configured" else "Default test token (tap to customize)",
                    onClick = { isEditingAuddApiKey = true }
                )
            }
        }

        SettingsGroupCard(title = "NETWORK STORAGE (WEBDAV)") {
            SettingsDescription(text = "Connect to a personal NAS or WebDAV server to stream high-resolution audio files remotely.")

            SwitchSettingEntry(
                title = "Enable WebDAV",
                text = "Stream audio from your home NAS or server",
                isChecked = isWebdavEnabled,
                onCheckedChange = { isWebdavEnabled = it }
            )

            if (isWebdavEnabled) {
                SettingsEntry(
                    title = "Server URL",
                    text = webdavUrl.ifBlank { "Tap to enter WebDAV URL (e.g. https://nas.local/dav)" },
                    onClick = { isEditingWebdavUrl = true }
                )

                SettingsEntry(
                    title = "Username",
                    text = webdavUsername.ifBlank { "Tap to enter username" },
                    onClick = { isEditingWebdavUsername = true }
                )

                SettingsEntry(
                    title = "Password",
                    text = if (webdavPassword.isNotBlank()) "••••••••" else "Tap to enter password",
                    onClick = { isEditingWebdavPassword = true }
                )

                SettingsEntry(
                    title = "Test Connection",
                    text = "Verify connection to WebDAV server",
                    onClick = {
                        coroutineScope.launch {
                            val result = WebDavClient.testConnection(webdavUrl, webdavUsername, webdavPassword)
                            if (result.isSuccess) {
                                context.toast("WebDAV connection successful!")
                            } else {
                                context.toast("Connection failed: ${result.exceptionOrNull()?.message}")
                            }
                        }
                    }
                )

                SettingsEntry(
                    title = "Sync Network Library",
                    text = webdavSyncStatus ?: "Scan and index remote audio files into SynapMusic",
                    onClick = {
                        coroutineScope.launch {
                            val engine = WebDavImportEngine()
                            engine.syncLibrary(webdavUrl, webdavUsername, webdavPassword).collect { progress ->
                                webdavSyncStatus = if (progress.error != null) {
                                    "Error: ${progress.error}"
                                } else {
                                    "Scanning: ${progress.current}/${progress.total} (${progress.currentFileName})"
                                }
                            }
                        }
                    }
                )
            }
        }

        SettingsGroupCard(title = "DISCORD RICH PRESENCE") {
            SettingsDescription(text = "Broadcast your currently playing song and playback progress to your Discord profile status.")

            SwitchSettingEntry(
                title = "Enable Discord Rich Presence",
                text = "Show what you are listening to on Discord",
                isChecked = isDiscordRpcEnabled,
                onCheckedChange = { isDiscordRpcEnabled = it }
            )

            if (isDiscordRpcEnabled) {
                SettingsEntry(
                    title = "Discord User Token",
                    text = if (discordToken.isNotBlank()) "Token saved (tap to edit)" else "Tap to paste your Discord token",
                    onClick = { isEditingDiscordToken = true }
                )
            }
        }

        SettingsGroupCard(title = "ANDROID AUTO") {
            SettingsDescription(text = "Remember to enable \"Unknown sources\" in the Developer Settings of Android Auto.")

            SwitchSettingEntry(
                title = "Android Auto",
                text = "Enable Android Auto support",
                isChecked = isAndroidAutoEnabled,
                onCheckedChange = { isAndroidAutoEnabled = it }
            )
        }

        SettingsGroupCard(title = "SEARCH HISTORY") {
            SwitchSettingEntry(
                title = "Pause search history",
                text = "Neither save new searched queries nor show history",
                isChecked = pauseSearchHistory,
                onCheckedChange = { pauseSearchHistory = it }
            )

            SettingsEntry(
                title = "Clear search history",
                text = if (queriesCount > 0) {
                    "Delete $queriesCount search queries"
                } else {
                    "History is empty"
                },
                isEnabled = queriesCount > 0,
                onClick = { query(Database::clearQueries) }
            )
        }

        SettingsGroupCard(title = "CONTENT & LYRICS") {
            EnumValueSelectorSettingsEntry(
                title = "Lyrics Language Preference",
                selectedValue = lyricsLanguage,
                onValueSelected = { lyricsLanguage = it },
                valueText = { it.label }
            )

            EnumValueSelectorSettingsEntry(
                title = "Region",
                selectedValue = region,
                onValueSelected = { region = it },
                valueText = { it.name.replace("_", " ").lowercase().replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(java.util.Locale.getDefault()) else char.toString() } }
            )
        }

        SettingsGroupCard(title = "SERVICE LIFETIME") {
            ImportantSettingsDescription(text = "If battery optimizations are applied, the playback notification can suddenly disappear when paused.")

            if (isAtLeastAndroid12) {
                SettingsDescription(text = "Since Android 12, disabling battery optimizations is required for the \"Invincible service\" option to take effect.")
            }

            SettingsEntry(
                title = "Ignore battery optimizations",
                isEnabled = !isIgnoringBatteryOptimizations,
                text = if (isIgnoringBatteryOptimizations) {
                    "Already unrestricted"
                } else {
                    "Disable background restrictions"
                },
                onClick = {
                    if (!isAtLeastAndroid6) return@SettingsEntry

                    try {
                        activityResultLauncher.launch(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = "package:${context.packageName}".toUri()
                            }
                        )
                    } catch (_: ActivityNotFoundException) {
                        try {
                            activityResultLauncher.launch(
                                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            )
                        } catch (_: ActivityNotFoundException) {
                            context.toast("Couldn't find battery optimization settings, please whitelist SynapMusic manually")
                        }
                    }
                }
            )

            SwitchSettingEntry(
                title = "Invincible service",
                text = "When turning off battery optimizations is not enough",
                isChecked = isInvincibilityEnabled,
                onCheckedChange = { isInvincibilityEnabled = it }
            )
        }

        SettingsGroupCard(title = "RESET ALL SETTINGS") {
            SettingsDescription(text = "Restore all application preferences, theme colors, audio configs, and settings back to factory default values.")

            SettingsEntry(
                title = "Reset All Settings to Defaults",
                text = "Clear saved options and restore default settings",
                onClick = { isShowingResetConfirmation = true }
            )
        }
    }

    if (isImportingSpotifyPlaylist) {
        SpotifyImportDialog(
            onDismiss = { isImportingSpotifyPlaylist = false }
        )
    }

    if (isImportingAppleMusicPlaylist) {
        AppleMusicImportDialog(
            onDismiss = { isImportingAppleMusicPlaylist = false }
        )
    }

    if (isShowingLiveRadioDialog) {
        LiveRadioDialog(
            onDismiss = { isShowingLiveRadioDialog = false }
        )
    }

    if (isShowingLocalScanDialog) {
        LocalFolderScanDialog(
            onDismiss = { isShowingLocalScanDialog = false }
        )
    }

    if (isEditingDiscordToken) {
        TextFieldDialog(
            hintText = "Paste your Discord User Token",
            initialTextInput = discordToken,
            onDismiss = { isEditingDiscordToken = false },
            onDone = { text ->
                discordToken = text
                isEditingDiscordToken = false
            }
        )
    }

    if (isShowingListenTogetherDialog) {
        ListenTogetherDialog(
            onDismiss = { isShowingListenTogetherDialog = false }
        )
    }

    if (isEditingCustomListenServerUrl) {
        TextFieldDialog(
            hintText = "Enter Server URL (e.g. https://my-server.com)",
            initialTextInput = customListenServerUrl,
            onDismiss = { isEditingCustomListenServerUrl = false },
            onDone = { text ->
                ListenTogether.setCustomServerUrl(text)
                isEditingCustomListenServerUrl = false
            }
        )
    }

    if (isEditingAuddApiKey) {
        TextFieldDialog(
            hintText = "Enter AudD API Token (or 'test')",
            initialTextInput = auddApiKey,
            onDismiss = { isEditingAuddApiKey = false },
            onDone = { text ->
                auddApiKey = text.ifBlank { "test" }
                isEditingAuddApiKey = false
            }
        )
    }

    if (isEditingWebdavUrl) {
        TextFieldDialog(
            hintText = "Enter WebDAV Server URL",
            initialTextInput = webdavUrl,
            onDismiss = { isEditingWebdavUrl = false },
            onDone = { text ->
                webdavUrl = text
                isEditingWebdavUrl = false
            }
        )
    }

    if (isEditingWebdavUsername) {
        TextFieldDialog(
            hintText = "Enter WebDAV Username",
            initialTextInput = webdavUsername,
            onDismiss = { isEditingWebdavUsername = false },
            onDone = { text ->
                webdavUsername = text
                isEditingWebdavUsername = false
            }
        )
    }

    if (isEditingWebdavPassword) {
        TextFieldDialog(
            hintText = "Enter WebDAV Password",
            initialTextInput = webdavPassword,
            onDismiss = { isEditingWebdavPassword = false },
            onDone = { text ->
                webdavPassword = text
                isEditingWebdavPassword = false
            }
        )
    }

    if (isShowingResetConfirmation) {
        ConfirmationDialog(
            text = "Are you sure you want to reset all app preferences and theme settings to defaults? Your downloaded Vault songs, playlists, and search history will remain untouched.",
            onDismiss = { isShowingResetConfirmation = false },
            onConfirm = {
                val preservedWebdavUrl = context.preferences.getString(webdavUrlKey, "")
                val preservedWebdavUser = context.preferences.getString(webdavUsernameKey, "")
                val preservedWebdavPass = context.preferences.getString(webdavPasswordKey, "")
                val preservedAuddKey = context.preferences.getString(auddApiKeyKey, "test")
                val preservedDiscordToken = context.preferences.getString(discordTokenKey, "")

                context.preferences.edit { clear() }
                context.persistMap?.clear()

                context.preferences.edit {
                    if (!preservedWebdavUrl.isNullOrBlank()) putString(webdavUrlKey, preservedWebdavUrl)
                    if (!preservedWebdavUser.isNullOrBlank()) putString(webdavUsernameKey, preservedWebdavUser)
                    if (!preservedWebdavPass.isNullOrBlank()) putString(webdavPasswordKey, preservedWebdavPass)
                    if (!preservedAuddKey.isNullOrBlank() && preservedAuddKey != "test") putString(auddApiKeyKey, preservedAuddKey)
                    if (!preservedDiscordToken.isNullOrBlank()) putString(discordTokenKey, preservedDiscordToken)
                }

                context.toast("Preferences and theme settings reset to defaults!")
            }
        )
    }
}
