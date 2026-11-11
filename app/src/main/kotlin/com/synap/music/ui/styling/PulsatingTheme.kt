package com.synap.music.ui.styling

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.synap.music.enums.PulsatingButtonColorPreset
import com.synap.music.enums.PulsatingColorPalette
import com.synap.music.enums.PulsatingEffectType
import com.synap.music.enums.PulsatingIntensity
import com.synap.music.enums.PulsatingSpeed
import com.synap.music.utils.applyPulsatingToButtonsKey
import com.synap.music.utils.buttonColorPresetKey
import com.synap.music.utils.isPulsatingEffectEnabledKey
import com.synap.music.utils.pulsatingColorPaletteKey
import com.synap.music.utils.pulsatingEffectTypeKey
import com.synap.music.utils.pulsatingIntensityKey
import com.synap.music.utils.pulsatingSpeedKey
import com.synap.music.utils.rememberPreference

// Animates a smooth pulsating color transition across up to 4 color stops.
@Composable
fun rememberPulsatingHybridColor(
    primaryColor: Color = LocalAppearance.current.colorPalette.accent,
    secondaryColor: Color = LocalAppearance.current.colorPalette.red,
    tertiaryColor: Color? = null,
    quaternaryColor: Color? = null,
    overridePalette: Boolean = false,
    durationMs: Int = 2400
): Color {
    val isPulsatingEnabled by rememberPreference(isPulsatingEffectEnabledKey, true)
    val applyToButtons by rememberPreference(applyPulsatingToButtonsKey, true)
    val pulsatingSpeed by rememberPreference(pulsatingSpeedKey, PulsatingSpeed.Natural)
    val effectType by rememberPreference(pulsatingEffectTypeKey, PulsatingEffectType.BreathingWave)
    val customPalette by rememberPreference(pulsatingColorPaletteKey, PulsatingColorPalette.AlbumArtDynamic)
    val pulsatingIntensity by rememberPreference(pulsatingIntensityKey, PulsatingIntensity.Balanced)

    if (!isPulsatingEnabled || !applyToButtons) {
        return primaryColor
    }

    val finalPrimary = if (overridePalette) primaryColor else (customPalette.primary ?: primaryColor)
    val finalSecondary = if (overridePalette) secondaryColor else (customPalette.secondary ?: secondaryColor)
    val finalTertiary = if (overridePalette) tertiaryColor else (customPalette.tertiary ?: tertiaryColor)
    val finalQuaternary = if (overridePalette) quaternaryColor else (customPalette.quaternary ?: quaternaryColor)

    val actualDuration = (durationMs * (pulsatingSpeed.durationMs / 2400f)).toInt()

    val easingCurve = when (effectType) {
        PulsatingEffectType.BreathingWave -> FastOutSlowInEasing
        PulsatingEffectType.BlinkingPulse -> LinearEasing
        PulsatingEffectType.SmoothFade -> LinearEasing
    }

    val infiniteTransition = rememberInfiniteTransition()
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = actualDuration, easing = easingCurve),
            repeatMode = RepeatMode.Reverse
        )
    )

    val animatedColor = when {
        finalQuaternary != null && finalTertiary != null -> {
            when {
                pulseFraction < 0.333f -> lerp(finalPrimary, finalSecondary, pulseFraction * 3f)
                pulseFraction < 0.666f -> lerp(finalSecondary, finalTertiary, (pulseFraction - 0.333f) * 3f)
                else -> lerp(finalTertiary, finalQuaternary, (pulseFraction - 0.666f) * 3f)
            }
        }
        finalTertiary != null -> {
            if (pulseFraction < 0.5f) {
                lerp(finalPrimary, finalSecondary, pulseFraction * 2f)
            } else {
                lerp(finalSecondary, finalTertiary, (pulseFraction - 0.5f) * 2f)
            }
        }
        else -> lerp(finalPrimary, finalSecondary, pulseFraction)
    }

    return lerp(finalPrimary, animatedColor, pulsatingIntensity.alphaFactor.coerceIn(0f, 1f))
}

// Gets the active color for primary buttons based on Settings preferences.
@Composable
fun rememberButtonColor(): Color {
    val colorPalette = LocalAppearance.current.colorPalette
    val buttonPreset by rememberPreference(buttonColorPresetKey, PulsatingButtonColorPreset.PulsatingHybrid)
    val hybridGlow = rememberPulsatingHybridColor(
        primaryColor = colorPalette.accent,
        secondaryColor = colorPalette.red,
        tertiaryColor = colorPalette.blue
    )

    return when {
        buttonPreset == PulsatingButtonColorPreset.DynamicTheme -> {
            rememberPulsatingHybridColor(
                primaryColor = colorPalette.accent,
                secondaryColor = colorPalette.red,
                tertiaryColor = colorPalette.blue
            )
        }
        buttonPreset == PulsatingButtonColorPreset.PulsatingHybrid -> hybridGlow
        buttonPreset.color != null && buttonPreset.secondaryColor != null -> {
            rememberPulsatingHybridColor(
                primaryColor = buttonPreset.color!!,
                secondaryColor = buttonPreset.secondaryColor!!,
                tertiaryColor = buttonPreset.tertiaryColor,
                quaternaryColor = buttonPreset.quaternaryColor,
                overridePalette = true
            )
        }
        else -> buttonPreset.color ?: colorPalette.accent
    }
}

// Gets staggered animated colors for grid tiles based on index.
@Composable
fun rememberTileHybridColor(index: Int): Color {
    val colorPalette = LocalAppearance.current.colorPalette
    val isPulsatingEnabled by rememberPreference(isPulsatingEffectEnabledKey, true)
    val pulsatingSpeed by rememberPreference(pulsatingSpeedKey, PulsatingSpeed.Natural)
    val effectType by rememberPreference(pulsatingEffectTypeKey, PulsatingEffectType.BreathingWave)
    val customPalette by rememberPreference(pulsatingColorPaletteKey, PulsatingColorPalette.AlbumArtDynamic)
    val pulsatingIntensity by rememberPreference(pulsatingIntensityKey, PulsatingIntensity.Balanced)

    val primary: Color
    val secondary: Color
    val tertiary: Color?
    val quaternary: Color?

    if (customPalette.primary != null && customPalette.secondary != null) {
        primary = customPalette.primary!!
        secondary = customPalette.secondary!!
        tertiary = customPalette.tertiary
        quaternary = customPalette.quaternary
    } else {
        tertiary = colorPalette.blue
        quaternary = null
        when (index % 10) {
            0 -> {
                primary = colorPalette.accent
                secondary = colorPalette.red
            }
            1 -> {
                primary = colorPalette.accent
                secondary = Color(0xFFE91E63) // Rose / Pink
            }
            2 -> {
                primary = colorPalette.red
                secondary = Color(0xFFFF9800) // Gold / Amber
            }
            3 -> {
                primary = Color(0xFFE91E63) // Rose / Pink
                secondary = colorPalette.accent
            }
            4 -> {
                primary = Color(0xFF9C27B0) // Violet / Purple
                secondary = colorPalette.accent
            }
            5 -> {
                primary = Color(0xFF00BCD4) // Cyan / Aqua
                secondary = colorPalette.accent
            }
            6 -> {
                primary = Color(0xFF4CAF50) // Emerald Green
                secondary = colorPalette.accent
            }
            7 -> {
                primary = colorPalette.red
                secondary = Color(0xFF9C27B0) // Crimson / Violet
            }
            8 -> {
                primary = colorPalette.accent
                secondary = Color(0xFFFF9800) // Accent / Gold
            }
            else -> {
                primary = colorPalette.accent
                secondary = Color(0xFFE91E63) // Accent / Rose
            }
        }
    }

    if (!isPulsatingEnabled) {
        return primary
    }

    val durationMs = ((1800 + (index % 4) * 500) * (pulsatingSpeed.durationMs / 2400f)).toInt()

    val easingCurve = when (effectType) {
        PulsatingEffectType.BreathingWave -> FastOutSlowInEasing
        PulsatingEffectType.BlinkingPulse -> LinearEasing
        PulsatingEffectType.SmoothFade -> LinearEasing
    }

    val infiniteTransition = rememberInfiniteTransition()
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = easingCurve),
            repeatMode = RepeatMode.Reverse
        )
    )

    val animatedColor = when {
        quaternary != null && tertiary != null -> {
            when {
                pulseFraction < 0.333f -> lerp(primary, secondary, pulseFraction * 3f)
                pulseFraction < 0.666f -> lerp(secondary, tertiary, (pulseFraction - 0.333f) * 3f)
                else -> lerp(tertiary, quaternary, (pulseFraction - 0.666f) * 3f)
            }
        }
        tertiary != null -> {
            if (pulseFraction < 0.5f) {
                lerp(primary, secondary, pulseFraction * 2f)
            } else {
                lerp(secondary, tertiary, (pulseFraction - 0.5f) * 2f)
            }
        }
        else -> lerp(primary, secondary, pulseFraction)
    }

    return lerp(primary, animatedColor, pulsatingIntensity.alphaFactor.coerceIn(0f, 1f))
}
