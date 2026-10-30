package com.synap.music.ui.screens.settings

import com.synap.music.ui.components.themed.EqualizerDialog

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.draw.clip
import com.synap.music.ui.styling.rememberButtonColor
import com.synap.music.ui.styling.rememberPulsatingHybridColor
import com.synap.music.utils.color
import com.synap.music.utils.semiBold
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.synap.music.R
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.synap.music.LocalPlayerAwareWindowInsets
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.enums.AudioEnhancementCategory
import com.synap.music.enums.AudioQuality
import com.synap.music.enums.ColorPaletteName
import com.synap.music.enums.CompressionEngine
import com.synap.music.enums.LoudnessTarget
import com.synap.music.enums.PulsatingButtonColorPreset
import com.synap.music.enums.PulsatingColorPalette
import com.synap.music.enums.PulsatingEffectType
import com.synap.music.enums.PulsatingIntensity
import com.synap.music.enums.PulsatingSpeed
import com.synap.music.enums.ThumbnailRoundness
import com.synap.music.enums.ThumbnailAudioBadgeStyle
import com.synap.music.utils.thumbnailAudioBadgeStyleKey
import com.synap.music.utils.applyPulsatingToButtonsKey
import com.synap.music.utils.applyPulsatingToDenTilesKey
import com.synap.music.utils.applyPulsatingToDiscoverCardsKey
import com.synap.music.utils.buttonColorPresetKey
import com.synap.music.utils.isPulsatingEffectEnabledKey
import com.synap.music.utils.pulsatingColorPaletteKey
import com.synap.music.utils.pulsatingEffectTypeKey
import com.synap.music.utils.pulsatingIntensityKey
import com.synap.music.utils.pulsatingSpeedKey
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.platform.LocalLayoutDirection
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.utils.applyFontPaddingKey
import com.synap.music.utils.audioEnhancementCategoryKey
import com.synap.music.utils.dynamicRangeCompressionKey
import com.synap.music.utils.spatialMasteringKey
import com.synap.music.utils.immersiveDecodingKey
import com.synap.music.utils.loudnessTargetKey
import com.synap.music.utils.crossfeedEnabledKey
import com.synap.music.utils.crossfadeDurationKey
import com.synap.music.utils.compressionEngineKey
import com.synap.music.utils.audioQualityKey
import com.synap.music.utils.colorPaletteNameKey
import com.synap.music.utils.isAtLeastAndroid13
import com.synap.music.utils.isAtLeastAndroid6
import com.synap.music.utils.isShowingThumbnailInLockscreenKey
import com.synap.music.utils.persistentQueueKey
import com.synap.music.utils.rememberPreference
import com.synap.music.utils.resumePlaybackWhenDeviceConnectedKey
import com.synap.music.utils.skipSilenceKey
import com.synap.music.utils.thumbnailRoundnessKey
import com.synap.music.utils.sponsorBlockEnabledKey
import com.synap.music.utils.toast
import com.synap.music.utils.useSystemFontKey
import com.synap.music.utils.volumeNormalizationKey

@ExperimentalAnimationApi
@Composable
fun PlayerSettings() {
    val context = LocalContext.current
    val (colorPalette, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current

    var persistentQueue by rememberPreference(persistentQueueKey, false)
    var resumePlaybackWhenDeviceConnected by rememberPreference(
        resumePlaybackWhenDeviceConnectedKey,
        false
    )
    var skipSilence by rememberPreference(skipSilenceKey, false)
    var volumeNormalization by rememberPreference(volumeNormalizationKey, false)
    var sponsorBlockEnabled by rememberPreference(sponsorBlockEnabledKey, false)
    var audioEnhancementCategory by rememberPreference(audioEnhancementCategoryKey, AudioEnhancementCategory.Auto)
    var compressionEngine by rememberPreference(compressionEngineKey, CompressionEngine.Auto)
    var audioQuality by rememberPreference(audioQualityKey, AudioQuality.Auto)
    var thumbnailAudioBadgeStyle by rememberPreference(thumbnailAudioBadgeStyleKey, ThumbnailAudioBadgeStyle.FidelityRank)

    var dynamicRangeCompression by rememberPreference(dynamicRangeCompressionKey, false)
    var spatialMastering by rememberPreference(spatialMasteringKey, false)
    var immersiveDecoding by rememberPreference(immersiveDecodingKey, false)
    var loudnessTarget by rememberPreference(loudnessTargetKey, LoudnessTarget.Auto)
    var crossfeedEnabled by rememberPreference(crossfeedEnabledKey, false)
    var crossfadeDuration by rememberPreference(crossfadeDurationKey, 0)
    
    // ... UI-specific states ...

    var savedColorPaletteName by rememberPreference(colorPaletteNameKey, ColorPaletteName.Dynamic)
    var thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Light
    )
    var useSystemFont by rememberPreference(useSystemFontKey, false)
    var applyFontPadding by rememberPreference(applyFontPaddingKey, false)
    var isShowingThumbnailInLockscreen by rememberPreference(
        isShowingThumbnailInLockscreenKey,
        false
    )

    var savedPulsatingEffectEnabled by rememberPreference(isPulsatingEffectEnabledKey, true)
    var savedPulsatingIntensity by rememberPreference(pulsatingIntensityKey, PulsatingIntensity.Balanced)
    var savedPulsatingSpeed by rememberPreference(pulsatingSpeedKey, PulsatingSpeed.Natural)
    var savedPulsatingEffectType by rememberPreference(pulsatingEffectTypeKey, PulsatingEffectType.BreathingWave)
    var savedPulsatingColorPalette by rememberPreference(pulsatingColorPaletteKey, PulsatingColorPalette.AlbumArtDynamic)
    var applyPulsatingToButtons by rememberPreference(applyPulsatingToButtonsKey, true)
    var applyPulsatingToDenTiles by rememberPreference(applyPulsatingToDenTilesKey, true)
    var applyPulsatingToDiscoverCards by rememberPreference(applyPulsatingToDiscoverCardsKey, true)
    var savedButtonColorPreset by rememberPreference(buttonColorPresetKey, PulsatingButtonColorPreset.PulsatingHybrid)

    var pendingColorPaletteName by rememberSaveable(savedColorPaletteName) { mutableStateOf(savedColorPaletteName) }
    var pendingPulsatingEffectEnabled by rememberSaveable(savedPulsatingEffectEnabled) { mutableStateOf(savedPulsatingEffectEnabled) }
    var pendingPulsatingIntensity by rememberSaveable(savedPulsatingIntensity) { mutableStateOf(savedPulsatingIntensity) }
    var pendingPulsatingSpeed by rememberSaveable(savedPulsatingSpeed) { mutableStateOf(savedPulsatingSpeed) }
    var pendingPulsatingEffectType by rememberSaveable(savedPulsatingEffectType) { mutableStateOf(savedPulsatingEffectType) }
    var pendingPulsatingColorPalette by rememberSaveable(savedPulsatingColorPalette) { mutableStateOf(savedPulsatingColorPalette) }
    var pendingButtonColorPreset by rememberSaveable(savedButtonColorPreset) { mutableStateOf(savedButtonColorPreset) }

    LaunchedEffect(savedColorPaletteName) {
        pendingColorPaletteName = savedColorPaletteName
    }
    LaunchedEffect(savedPulsatingEffectEnabled) {
        pendingPulsatingEffectEnabled = savedPulsatingEffectEnabled
    }
    LaunchedEffect(savedPulsatingIntensity) {
        pendingPulsatingIntensity = savedPulsatingIntensity
    }
    LaunchedEffect(savedPulsatingSpeed) {
        pendingPulsatingSpeed = savedPulsatingSpeed
    }
    LaunchedEffect(savedPulsatingEffectType) {
        pendingPulsatingEffectType = savedPulsatingEffectType
    }
    LaunchedEffect(savedPulsatingColorPalette) {
        pendingPulsatingColorPalette = savedPulsatingColorPalette
    }
    LaunchedEffect(savedButtonColorPreset) {
        pendingButtonColorPreset = savedButtonColorPreset
    }

    var isAppearanceExpanded by remember {
        mutableStateOf(false)
    }

    var isShowingEqualizerDialog by remember {
        mutableStateOf(false)
    }

    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

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
        SettingsGroupCard(title = "APPEARANCE") {
            SettingsEntry(
                title = "Customize Appearance",
                text = "Colors, shapes, and text settings",
                onClick = { isAppearanceExpanded = !isAppearanceExpanded },
                trailingContent = {
                    val (currentPalette) = LocalAppearance.current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Spacer(
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = currentPalette.accent,
                                    shape = thumbnailRoundness.shape()
                                )
                                .background(
                                    color = currentPalette.background1,
                                    shape = thumbnailRoundness.shape()
                                )
                                .size(28.dp)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.chevron_forward),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(currentPalette.textSecondary),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        }

        if (isAppearanceExpanded) {
            SettingsGroupCard(title = "COLORS") {
                EnumValueSelectorSettingsEntry(
                    title = "Theme",
                    selectedValue = pendingColorPaletteName,
                    onValueSelected = { pendingColorPaletteName = it },
                    valueText = { it.label }
                )

                EnumValueSelectorSettingsEntry(
                    title = "Button & Floating Search Color",
                    selectedValue = pendingButtonColorPreset,
                    onValueSelected = { pendingButtonColorPreset = it },
                    valueText = { it.label }
                )

                SwitchSettingEntry(
                    title = "Pulsating Hybrid Color Effects",
                    text = "Enable smooth breathing hybrid color glow on tiles and buttons",
                    isChecked = pendingPulsatingEffectEnabled,
                    onCheckedChange = { pendingPulsatingEffectEnabled = it }
                )

                if (pendingPulsatingEffectEnabled) {
                    EnumValueSelectorSettingsEntry(
                        title = "Pulsating Palette Preset",
                        selectedValue = pendingPulsatingColorPalette,
                        onValueSelected = { pendingPulsatingColorPalette = it },
                        valueText = { it.label }
                    )

                    EnumValueSelectorSettingsEntry(
                        title = "Pulsating Effect Wave",
                        selectedValue = pendingPulsatingEffectType,
                        onValueSelected = { pendingPulsatingEffectType = it },
                        valueText = { it.label }
                    )

                    EnumValueSelectorSettingsEntry(
                        title = "Pulsating Intensity",
                        selectedValue = pendingPulsatingIntensity,
                        onValueSelected = { pendingPulsatingIntensity = it },
                        valueText = { it.label }
                    )

                    EnumValueSelectorSettingsEntry(
                        title = "Breathing Speed",
                        selectedValue = pendingPulsatingSpeed,
                        onValueSelected = { pendingPulsatingSpeed = it },
                        valueText = { it.label }
                    )

                    SwitchSettingEntry(
                        title = "Apply to Action Buttons",
                        text = "Enable pulsating hybrid glow on dialog & screen buttons",
                        isChecked = applyPulsatingToButtons,
                        onCheckedChange = { applyPulsatingToButtons = it }
                    )

                    SwitchSettingEntry(
                        title = "Apply to DEN Lair Tiles",
                        text = "Enable pulsating hybrid glow on DEN feature tiles",
                        isChecked = applyPulsatingToDenTiles,
                        onCheckedChange = { applyPulsatingToDenTiles = it }
                    )

                    SwitchSettingEntry(
                        title = "Apply to Discover Cards",
                        text = "Enable pulsating hybrid glow on Discover music cards",
                        isChecked = applyPulsatingToDiscoverCards,
                        onCheckedChange = { applyPulsatingToDiscoverCards = it }
                    )
                }

                val buttonColor = rememberButtonColor()

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(buttonColor)
                        .clickable {
                            savedColorPaletteName = pendingColorPaletteName
                            savedPulsatingEffectEnabled = pendingPulsatingEffectEnabled
                            savedPulsatingIntensity = pendingPulsatingIntensity
                            savedPulsatingSpeed = pendingPulsatingSpeed
                            savedPulsatingEffectType = pendingPulsatingEffectType
                            savedPulsatingColorPalette = pendingPulsatingColorPalette
                            savedButtonColorPreset = pendingButtonColorPreset
                            context.toast("Settings applied successfully!")
                        }
                ) {
                    BasicText(
                        text = "Apply Changes",
                        style = typography.xs.semiBold.color(colorPalette.onAccent)
                    )
                }
            }

            SettingsGroupCard(title = "SHAPES") {
                val buttonColor = rememberButtonColor()
                EnumValueSelectorSettingsEntry(
                    title = "Thumbnail roundness",
                    selectedValue = thumbnailRoundness,
                    onValueSelected = { thumbnailRoundness = it },
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = buttonColor,
                                        shape = thumbnailRoundness.shape()
                                    )
                                    .background(
                                        color = colorPalette.background1,
                                        shape = thumbnailRoundness.shape()
                                    )
                                    .size(28.dp)
                            )
                            Image(
                                painter = painterResource(id = R.drawable.chevron_forward),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
            }

            SettingsGroupCard(title = "TEXT") {
                SwitchSettingEntry(
                    title = "Use system font",
                    text = "Use the font applied by the system",
                    isChecked = useSystemFont,
                    onCheckedChange = { useSystemFont = it }
                )

                SwitchSettingEntry(
                    title = "Apply font padding",
                    text = "Add spacing around texts",
                    isChecked = applyFontPadding,
                    onCheckedChange = { applyFontPadding = it }
                )
            }

            if (!isAtLeastAndroid13) {
                SettingsGroupCard(title = "LOCKSCREEN") {
                    SwitchSettingEntry(
                        title = "Show song cover",
                        text = "Use the playing song cover as the lockscreen wallpaper",
                        isChecked = isShowingThumbnailInLockscreen,
                        onCheckedChange = { isShowingThumbnailInLockscreen = it }
                    )
                }
            }
        }

        SettingsGroupCard(title = "PLAYER") {
            SwitchSettingEntry(
                title = "Persistent queue",
                text = "Save and restore playing songs",
                isChecked = persistentQueue,
                onCheckedChange = {
                    persistentQueue = it
                }
            )

            if (isAtLeastAndroid6) {
                SwitchSettingEntry(
                    title = "Resume playback",
                    text = "When a wired or bluetooth device is connected",
                    isChecked = resumePlaybackWhenDeviceConnected,
                    onCheckedChange = {
                        resumePlaybackWhenDeviceConnected = it
                    }
                )
            }
        }

        SettingsGroupCard(title = "AUDIO") {
            SwitchSettingEntry(
                title = "Skip silence",
                text = "Skip silent parts during playback",
                isChecked = skipSilence,
                onCheckedChange = {
                    skipSilence = it
                }
            )

            SwitchSettingEntry(
                title = "Loudness normalization",
                text = "Adjust the volume to a fixed level",
                isChecked = volumeNormalization,
                onCheckedChange = {
                    volumeNormalization = it
                }
            )

            SwitchSettingEntry(
                title = "SponsorBlock",
                text = "Automatically skip sponsorships, intros, and outros",
                isChecked = sponsorBlockEnabled,
                onCheckedChange = {
                    sponsorBlockEnabled = it
                }
            )

            ValueSelectorSettingsEntry(
                title = "Crossfade duration",
                selectedValue = crossfadeDuration,
                values = listOf(0, 2, 4, 6, 8, 10, 12),
                onValueSelected = { crossfadeDuration = it },
                valueText = { if (it == 0) "Off" else "$it seconds" }
            )

            EnumValueSelectorSettingsEntry(
                title = "Audio enhancement",
                selectedValue = audioEnhancementCategory,
                onValueSelected = { audioEnhancementCategory = it },
                valueText = { it.label }
            )

            EnumValueSelectorSettingsEntry(
                title = "Online Streaming Quality",
                selectedValue = audioQuality,
                onValueSelected = { audioQuality = it },
                valueText = { "${it.label} — ${it.subtitle}" }
            )

            EnumValueSelectorSettingsEntry(
                title = "Offline Vault Download Format and Quality",
                selectedValue = compressionEngine,
                onValueSelected = { compressionEngine = it },
                valueText = { "${it.label} — ${it.subtitle}" }
            )

            EnumValueSelectorSettingsEntry(
                title = "Thumbnail Audio Badge Style",
                selectedValue = thumbnailAudioBadgeStyle,
                onValueSelected = { thumbnailAudioBadgeStyle = it },
                valueText = { "${it.label} — ${it.subtitle}" }
            )

            SettingsEntry(
                title = "Graphic Equalizer & Audio DSP",
                text = "In-app 5-band frequency tuning, Bass Boost, and 3D Surround",
                onClick = {
                    isShowingEqualizerDialog = true
                }
            )

            SettingsEntry(
                title = "System Equalizer",
                text = "Interact with the system equalizer",
                onClick = {
                    val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, binder?.player?.audioSessionId)
                        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                    }

                    try {
                        activityResultLauncher.launch(intent)
                    } catch (_: Exception) {
                        try {
                            val soundSettingsIntent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(soundSettingsIntent)
                        } catch (_: Exception) {
                            context.toast("Couldn't find an application to equalize audio")
                        }
                    }
                }
            )
        }

        SettingsGroupCard(title = "MASTERING & IMMERSIVE") {
            EnumValueSelectorSettingsEntry(
                title = "Loudness target",
                selectedValue = loudnessTarget,
                onValueSelected = { loudnessTarget = it },
                valueText = { "${it.label} (${it.subtitle})" }
            )

            SwitchSettingEntry(
                title = "Dynamic Range Compression",
                text = "Studio-grade volume leveling (DRC)",
                isChecked = dynamicRangeCompression,
                onCheckedChange = { dynamicRangeCompression = it }
            )

            SwitchSettingEntry(
                title = "Headphone crossfeed",
                text = "BS2B binaural filter to eliminate hard-panned stereo fatigue",
                isChecked = crossfeedEnabled,
                onCheckedChange = { crossfeedEnabled = it }
            )

            SwitchSettingEntry(
                title = "Spatial Mastering",
                text = "Binaural HRTF & Resonance logic (Steam Audio simulation)",
                isChecked = spatialMastering,
                onCheckedChange = { spatialMastering = it }
            )

            SwitchSettingEntry(
                title = "Immersive Decoding",
                text = "Native support for IAMF (Eclipsa) and multi-channel content",
                isChecked = immersiveDecoding,
                onCheckedChange = { immersiveDecoding = it }
            )
        }
    }

    if (isShowingEqualizerDialog) {
        EqualizerDialog(
            onDismiss = { isShowingEqualizerDialog = false }
        )
    }
}
