package com.synap.music.ui.components.themed

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.synap.music.LocalPlayerServiceBinder
import com.synap.music.R
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.enums.AudioEnhancementCategory
import com.synap.music.utils.audioEnhancementCategoryKey
import com.synap.music.utils.bassBoostStrengthKey
import com.synap.music.utils.center
import com.synap.music.utils.eqPresetIndexKey
import com.synap.music.utils.rememberPreference
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold
import com.synap.music.utils.virtualizerStrengthKey

val eqPresets = listOf(
    "Flat" to shortArrayOf(0, 0, 0, 0, 0),
    "Bass Boost" to shortArrayOf(800, 500, 0, 0, 0),
    "BoostX" to shortArrayOf(1000, 800, 200, 400, 600),
    "ProSoundEQ" to shortArrayOf(600, 300, -200, 300, 600),
    "Rock" to shortArrayOf(500, 300, -100, 200, 500),
    "Pop" to shortArrayOf(-100, 200, 500, 200, -100),
    "Jazz" to shortArrayOf(400, 200, -100, 200, 400),
    "Vocal" to shortArrayOf(-200, 100, 600, 300, -100),
    "Electronic" to shortArrayOf(600, 400, 0, 200, 500),
    "Custom" to shortArrayOf(0, 0, 0, 0, 0)
)

/**
 * Interactive Graphic Equalizer, Bass Boost & 3D Surround DSP Control Panel.
 * Automatically synchronizes frequency band levels in real-time when presets are selected,
 * and provides a top action shortcut to launch Android's native system equalizer.
 */
@Composable
fun EqualizerDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current

    var selectedPresetIndex by rememberPreference(eqPresetIndexKey, 0)
    var bassBoostValue by rememberPreference(bassBoostStrengthKey, 0)
    var virtualizerValue by rememberPreference(virtualizerStrengthKey, 0)
    var audioEnhancementCategory by rememberPreference(audioEnhancementCategoryKey, AudioEnhancementCategory.Auto)
    var isShowingPresetMenu by remember { mutableStateOf(false) }

    val bandFrequencies = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
    val bandLevels = remember { mutableStateListOf(0, 0, 0, 0, 0) }

    // Synchronize UI frequency band levels whenever a preset is chosen or on launch
    LaunchedEffect(selectedPresetIndex) {
        if (selectedPresetIndex != 9) { // Non-custom presets
            val presetLevels = eqPresets.getOrNull(selectedPresetIndex)?.second
            presetLevels?.forEachIndexed { i, valShort ->
                if (i < bandLevels.size) {
                    bandLevels[i] = valShort.toInt()
                }
            }
        }
    }



    DefaultDialog(
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                BasicText(
                    text = "Graphic Equalizer & Audio DSP",
                    style = typography.s.semiBold
                )
            }

            // Scrollable Content Area
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                // EQ Preset Dropdown Selector
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorPalette.background0)
                        .clickable { isShowingPresetMenu = !isShowingPresetMenu }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BasicText(
                            text = "PRESET: ${eqPresets.getOrNull(selectedPresetIndex)?.first ?: "Custom"}",
                            style = typography.xs.semiBold
                        )
                        Image(
                            painter = painterResource(id = if (isShowingPresetMenu) R.drawable.chevron_up else R.drawable.chevron_down),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (isShowingPresetMenu) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorPalette.background0)
                    ) {
                        eqPresets.forEachIndexed { idx, (label, levels) ->
                            val isSelected = selectedPresetIndex == idx
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPresetIndex = idx
                                        isShowingPresetMenu = false
                                        audioEnhancementCategory = AudioEnhancementCategory.Equalizer
                                        if (idx != 9) {
                                            levels.forEachIndexed { i, valShort ->
                                                if (i < bandLevels.size) {
                                                    bandLevels[i] = valShort.toInt()
                                                }
                                            }
                                        }
                                        binder?.audioEnhancementManager?.applyCustomEQ(levels)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                BasicText(
                                    text = label,
                                    style = if (isSelected) typography.xs.semiBold else typography.xs.secondary
                                )
                                if (isSelected) {
                                    Image(
                                        painter = painterResource(id = R.drawable.checkmark),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(colorPalette.accent),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Frequency Bands
                BasicText(
                    text = "FREQUENCY BANDS (-15dB to +15dB)",
                    style = typography.xxs.secondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colorPalette.background0)
                        .padding(8.dp)
                ) {
                    bandFrequencies.forEachIndexed { index, freq ->
                        val currentLevel = bandLevels.getOrElse(index) { 0 }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            BasicText(
                                text = freq,
                                style = typography.xs.semiBold,
                                modifier = Modifier.width(56.dp)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(colorPalette.background1)
                                        .clickable {
                                            val newLvl = (currentLevel - 200).coerceAtLeast(-1500)
                                            bandLevels[index] = newLvl
                                            selectedPresetIndex = 9 // Custom
                                            audioEnhancementCategory = AudioEnhancementCategory.Equalizer
                                            binder?.audioEnhancementManager?.applyCustomEQ(
                                                bandLevels
                                                    .map { it.toShort() }
                                                    .toShortArray()
                                            )
                                        }
                                ) {
                                    BasicText("-", style = typography.xs.semiBold)
                                }

                                BasicText(
                                    text = "${currentLevel / 100} dB",
                                    style = typography.xs.semiBold.center,
                                    modifier = Modifier.width(52.dp)
                                )

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(colorPalette.background1)
                                        .clickable {
                                            val newLvl = (currentLevel + 200).coerceAtMost(1500)
                                            bandLevels[index] = newLvl
                                            selectedPresetIndex = 9 // Custom
                                            audioEnhancementCategory = AudioEnhancementCategory.Equalizer
                                            binder?.audioEnhancementManager?.applyCustomEQ(
                                                bandLevels
                                                    .map { it.toShort() }
                                                    .toShortArray()
                                            )
                                        }
                                ) {
                                    BasicText("+", style = typography.xs.semiBold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Compact Bass Boost & Virtualizer Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Bass Boost Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colorPalette.background0)
                            .padding(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            BasicText("BASS BOOST", style = typography.xxs.secondary)
                            BasicText(
                                text = "${bassBoostValue / 10}%",
                                style = typography.s.semiBold,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colorPalette.background1)
                                        .clickable {
                                            bassBoostValue = (bassBoostValue - 100).coerceAtLeast(0)
                                            binder?.audioEnhancementManager?.applyBassBoostStrength(bassBoostValue.toShort())
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    BasicText("-10%", style = typography.xxs.semiBold)
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colorPalette.background1)
                                        .clickable {
                                            bassBoostValue = (bassBoostValue + 100).coerceAtMost(1000)
                                            binder?.audioEnhancementManager?.applyBassBoostStrength(bassBoostValue.toShort())
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    BasicText("+10%", style = typography.xxs.semiBold)
                                }
                            }
                        }
                    }

                    // 3D Surround Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colorPalette.background0)
                            .padding(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            BasicText("3D SURROUND", style = typography.xxs.secondary)
                            BasicText(
                                text = "${virtualizerValue / 10}%",
                                style = typography.s.semiBold,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colorPalette.background1)
                                        .clickable {
                                            virtualizerValue = (virtualizerValue - 100).coerceAtLeast(0)
                                            binder?.audioEnhancementManager?.applyVirtualizerStrength(virtualizerValue.toShort())
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    BasicText("-10%", style = typography.xxs.semiBold)
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colorPalette.background1)
                                        .clickable {
                                            virtualizerValue = (virtualizerValue + 100).coerceAtMost(1000)
                                            binder?.audioEnhancementManager?.applyVirtualizerStrength(virtualizerValue.toShort())
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    BasicText("+10%", style = typography.xxs.semiBold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pinned Close Button
            DialogTextButton(
                text = "Close",
                primary = true,
                onClick = onDismiss
            )
        }
    }
}
