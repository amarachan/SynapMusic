package com.synap.music.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import com.synap.compose.persist.persist
import com.synap.compose.persist.persistMap
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect

const val colorPaletteNameKey = "colorPaletteName"
const val colorPaletteModeKey = "colorPaletteMode"
const val thumbnailRoundnessKey = "thumbnailRoundness"
const val coilDiskCacheMaxSizeKey = "coilDiskCacheMaxSize"
const val exoPlayerDiskCacheMaxSizeKey = "exoPlayerDiskCacheMaxSize"
const val isInvincibilityEnabledKey = "isInvincibilityEnabled"
const val useSystemFontKey = "useSystemFont"
const val applyFontPaddingKey = "applyFontPadding"
const val songSortOrderKey = "songSortOrder"
const val songSortByKey = "songSortBy"
const val playlistSortOrderKey = "playlistSortOrder"
const val playlistSortByKey = "playlistSortBy"
const val albumSortOrderKey = "albumSortOrder"
const val albumSortByKey = "albumSortBy"
const val artistSortOrderKey = "artistSortOrder"
const val artistSortByKey = "artistSortBy"
const val trackLoopEnabledKey = "trackLoopEnabled"
const val queueLoopEnabledKey = "queueLoopEnabled"
const val skipSilenceKey = "skipSilence"
const val volumeNormalizationKey = "volumeNormalization"
const val audioEnhancementCategoryKey = "audioEnhancementCategory"
const val cubeCompressionLevelKey = "cubeCompressionLevel"
const val compressionEngineKey = "compressionEngine"
const val audioQualityKey = "audioQuality"
const val resumePlaybackWhenDeviceConnectedKey = "resumePlaybackWhenDeviceConnected"
const val persistentQueueKey = "persistentQueue"
const val isShowingSynchronizedLyricsKey = "isShowingSynchronizedLyrics"
const val isShowingThumbnailInLockscreenKey = "isShowingThumbnailInLockscreen"
const val homeScreenTabIndexKey = "homeScreenTabIndex"
const val searchResultScreenTabIndexKey = "searchResultScreenTabIndex"
const val artistScreenTabIndexKey = "artistScreenTabIndex"
const val pauseSearchHistoryKey = "pauseSearchHistory"
const val regionKey = "region"
const val discoveryLanguageKey = "discoveryLanguage"
const val discoveryGenreKey = "discoveryGenre"
const val discoveryDecadeKey = "discoveryDecade"
const val discoveryLimitKey = "discoveryLimit"
const val discoveryStyleKey = "discoveryStyle"
const val discoveryViewModeKey = "discoveryViewMode"
const val vaultViewModeKey = "vaultViewMode"
const val searchViewModeKey = "searchViewMode"
const val filterAiSongsKey = "filterAiSongs"
const val isProSearchModeKey = "isProSearchMode"
const val dynamicRangeCompressionKey = "dynamicRangeCompression"
const val spatialMasteringKey = "spatialMastering"
const val immersiveDecodingKey = "immersiveDecoding"
const val loudnessTargetKey = "loudnessTarget"
const val crossfeedEnabledKey = "crossfeedEnabled"
const val crossfadeDurationKey = "crossfadeDuration"
const val discordTokenKey = "discordToken"
const val isDiscordRpcEnabledKey = "isDiscordRpcEnabled"
const val sponsorBlockEnabledKey = "sponsorBlockEnabled"
const val webdavUrlKey = "webdavUrl"
const val webdavUsernameKey = "webdavUsername"
const val webdavPasswordKey = "webdavPassword"
const val isWebdavEnabledKey = "isWebdavEnabled"
const val auddApiKeyKey = "auddApiKey"
const val isMusicRecognitionEnabledKey = "isMusicRecognitionEnabled"
const val eqCustomBandLevelsKey = "eqCustomBandLevels"
const val eqPresetIndexKey = "eqPresetIndex"
const val bassBoostStrengthKey = "bassBoostStrength"
const val virtualizerStrengthKey = "virtualizerStrength"
const val reverbPresetKey = "reverbPreset"
const val sleepTimerFadeOutEnabledKey = "sleepTimerFadeOutEnabled"
const val isListenTogetherEnabledKey = "isListenTogetherEnabled"
const val lyricsLanguageKey = "lyricsLanguage"
const val isLocalStorageEnabledKey = "isLocalStorageEnabled"
const val isSmartMixesEnabledKey = "isSmartMixesEnabled"
const val isSynapRecapEnabledKey = "isSynapRecapEnabled"
const val isSpotifyImportEnabledKey = "isSpotifyImportEnabled"
const val isAppleMusicImportEnabledKey = "isAppleMusicImportEnabled"
const val isLiveRadioEnabledKey = "isLiveRadioEnabled"
const val isPulsatingEffectEnabledKey = "isPulsatingEffectEnabled"
const val pulsatingIntensityKey = "pulsatingIntensity"
const val pulsatingSpeedKey = "pulsatingSpeed"
const val pulsatingEffectTypeKey = "pulsatingEffectType"
const val pulsatingColorPaletteKey = "pulsatingColorPalette"
const val applyPulsatingToButtonsKey = "applyPulsatingToButtons"
const val applyPulsatingToDenTilesKey = "applyPulsatingToDenTiles"
const val applyPulsatingToDiscoverCardsKey = "applyPulsatingToDiscoverCards"
const val buttonColorPresetKey = "buttonColorPreset"
const val thumbnailAudioBadgeStyleKey = "thumbnailAudioBadgeStyle"

inline fun <reified T : Enum<T>> SharedPreferences.getEnum(
    key: String,
    defaultValue: T
): T =
    getString(key, null)?.let {
        try {
            enumValueOf<T>(it)
        } catch (_: IllegalArgumentException) {
            null
        }
    } ?: defaultValue

inline fun <reified T : Enum<T>> SharedPreferences.Editor.putEnum(
    key: String,
    value: T
): SharedPreferences.Editor =
    putString(key, value.name)

val Context.preferences: SharedPreferences
    get() = getSharedPreferences("preferences", Context.MODE_PRIVATE)

@Composable
fun rememberPreference(key: String, defaultValue: Boolean): MutableState<Boolean> {
    val context = LocalContext.current
    val state = persist(tag = "pref/$key", initialValue = context.preferences.getBoolean(key, defaultValue))

    DisposableEffect(key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, updatedKey ->
            if (updatedKey == key || updatedKey == null) {
                state.value = prefs.getBoolean(key, defaultValue)
            }
        }
        context.preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            context.preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    LaunchedEffect(state.value) {
        if (context.preferences.getBoolean(key, defaultValue) != state.value) {
            context.preferences.edit { putBoolean(key, state.value) }
        }
    }
    return state
}

@Composable
fun rememberPreference(key: String, defaultValue: Int): MutableState<Int> {
    val context = LocalContext.current
    val state = persist(tag = "pref/$key", initialValue = context.preferences.getInt(key, defaultValue))

    DisposableEffect(key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, updatedKey ->
            if (updatedKey == key || updatedKey == null) {
                state.value = prefs.getInt(key, defaultValue)
            }
        }
        context.preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            context.preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    LaunchedEffect(state.value) {
        if (context.preferences.getInt(key, defaultValue) != state.value) {
            context.preferences.edit { putInt(key, state.value) }
        }
    }
    return state
}

@Composable
fun rememberPreference(key: String, defaultValue: Float): MutableState<Float> {
    val context = LocalContext.current
    val state = persist(tag = "pref/$key", initialValue = context.preferences.getFloat(key, defaultValue))

    DisposableEffect(key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, updatedKey ->
            if (updatedKey == key || updatedKey == null) {
                state.value = prefs.getFloat(key, defaultValue)
            }
        }
        context.preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            context.preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    LaunchedEffect(state.value) {
        if (context.preferences.getFloat(key, defaultValue) != state.value) {
            context.preferences.edit { putFloat(key, state.value) }
        }
    }
    return state
}

@Composable
fun rememberPreference(key: String, defaultValue: String): MutableState<String> {
    val context = LocalContext.current
    val state = persist(tag = "pref/$key", initialValue = context.preferences.getString(key, null) ?: defaultValue)

    DisposableEffect(key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, updatedKey ->
            if (updatedKey == key || updatedKey == null) {
                state.value = prefs.getString(key, null) ?: defaultValue
            }
        }
        context.preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            context.preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    LaunchedEffect(state.value) {
        if ((context.preferences.getString(key, null) ?: defaultValue) != state.value) {
            context.preferences.edit { putString(key, state.value) }
        }
    }
    return state
}

@Composable
inline fun <reified T : Enum<T>> rememberPreference(key: String, defaultValue: T): MutableState<T> {
    val context = LocalContext.current
    val state = persist(tag = "pref/$key", initialValue = context.preferences.getEnum(key, defaultValue))

    DisposableEffect(key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, updatedKey ->
            if (updatedKey == key || updatedKey == null) {
                state.value = prefs.getEnum(key, defaultValue)
            }
        }
        context.preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            context.preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    LaunchedEffect(state.value) {
        if (context.preferences.getEnum(key, defaultValue) != state.value) {
            context.preferences.edit { putEnum(key, state.value) }
        }
    }
    return state
}
