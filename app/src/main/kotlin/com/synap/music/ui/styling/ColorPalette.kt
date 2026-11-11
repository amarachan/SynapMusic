package com.synap.music.ui.styling

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.synap.music.enums.ColorPaletteName

@Immutable
data class ColorPalette(
    val background0: Color,
    val background1: Color,
    val background2: Color,
    val accent: Color,
    val onAccent: Color,
    val red: Color = Color(0xffbf4040),
    val blue: Color = Color(0xff4472cf),
    val text: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val isDark: Boolean
) {
    companion object : Saver<ColorPalette, List<Any>> {
        override fun restore(value: List<Any>): ColorPalette {
            val paletteNameOrdinal = value[0] as Int
            val isDark = value[1] as Boolean
            val accentArgb = value[2] as Int

            return when (paletteNameOrdinal) {
                0 -> DefaultDarkColorPalette
                1 -> DefaultLightColorPalette
                2 -> PureBlackColorPalette
                3 -> NordDarkColorPalette
                4 -> DraculaDarkColorPalette
                5 -> CatppuccinDarkColorPalette
                6 -> TokyoNightDarkColorPalette
                7 -> EmeraldForestDarkColorPalette
                8 -> CyberpunkDarkColorPalette
                9 -> SunsetOLEDColorPalette
                else -> dynamicColorPaletteOf(
                    FloatArray(3).apply { ColorUtils.colorToHSL(accentArgb, this) },
                    isDark
                )
            }
        }

        override fun SaverScope.save(value: ColorPalette) =
            listOf(
                when {
                    value === DefaultDarkColorPalette -> 0
                    value === DefaultLightColorPalette -> 1
                    value === PureBlackColorPalette -> 2
                    value === NordDarkColorPalette -> 3
                    value === DraculaDarkColorPalette -> 4
                    value === CatppuccinDarkColorPalette -> 5
                    value === TokyoNightDarkColorPalette -> 6
                    value === EmeraldForestDarkColorPalette -> 7
                    value === CyberpunkDarkColorPalette -> 8
                    value === SunsetOLEDColorPalette -> 9
                    else -> 100
                },
                value.isDark,
                value.accent.toArgb()
            )
    }
}

val DefaultDarkColorPalette = ColorPalette(
    background0 = Color(0xff0e0f14),
    background1 = Color(0xff17181f),
    background2 = Color(0xff22242e),
    text = Color(0xfff3f4f6),
    textSecondary = Color(0xff9ca3af),
    textDisabled = Color(0xff4b5563),
    accent = Color(0xff6366f1),
    onAccent = Color.White,
    isDark = true
)

val DefaultLightColorPalette = ColorPalette(
    background0 = Color(0xffffffff),
    background1 = Color(0xfff9fafb),
    background2 = Color(0xfff3f4f6),
    text = Color(0xff111827),
    textSecondary = Color(0xff4b5563),
    textDisabled = Color(0xff9ca3af),
    accent = Color(0xff4f46e5),
    onAccent = Color.White,
    isDark = false
)

val PureBlackColorPalette = DefaultDarkColorPalette.copy(
    background0 = Color.Black,
    background1 = Color.Black,
    background2 = Color(0xFF141414)
)

val NordDarkColorPalette = ColorPalette(
    background0 = Color(0xFF2E3440),
    background1 = Color(0xFF3B4252),
    background2 = Color(0xFF434C5E),
    text = Color(0xFFECEFF4),
    textSecondary = Color(0xFFD8DEE9),
    textDisabled = Color(0xFF4C566A),
    accent = Color(0xFF88C0D0),
    onAccent = Color(0xFF2E3440),
    isDark = true
)

val DraculaDarkColorPalette = ColorPalette(
    background0 = Color(0xFF282A36),
    background1 = Color(0xFF343746),
    background2 = Color(0xFF44475A),
    text = Color(0xFFF8F8F2),
    textSecondary = Color(0xFF6272A4),
    textDisabled = Color(0xFF44475A),
    accent = Color(0xFFBD93F9),
    onAccent = Color(0xFF282A36),
    isDark = true
)

val CatppuccinDarkColorPalette = ColorPalette(
    background0 = Color(0xFF1E1E2E),
    background1 = Color(0xFF252538),
    background2 = Color(0xFF313244),
    text = Color(0xFFCDD6F4),
    textSecondary = Color(0xFFA6ADC8),
    textDisabled = Color(0xFF585B70),
    accent = Color(0xFFCBA6F7),
    onAccent = Color(0xFF1E1E2E),
    isDark = true
)

val TokyoNightDarkColorPalette = ColorPalette(
    background0 = Color(0xFF1A1B26),
    background1 = Color(0xFF202330),
    background2 = Color(0xFF2A2E3D),
    text = Color(0xFFA9B1D6),
    textSecondary = Color(0xFF787C99),
    textDisabled = Color(0xFF414868),
    accent = Color(0xFF7AA2F7),
    onAccent = Color(0xFF1A1B26),
    isDark = true
)

val EmeraldForestDarkColorPalette = ColorPalette(
    background0 = Color(0xFF0B1915),
    background1 = Color(0xFF122822),
    background2 = Color(0xFF1B3B32),
    text = Color(0xFFECFDF5),
    textSecondary = Color(0xFF6EE7B7),
    textDisabled = Color(0xFF065F46),
    accent = Color(0xFF10B981),
    onAccent = Color.White,
    isDark = true
)

val CyberpunkDarkColorPalette = ColorPalette(
    background0 = Color(0xFF100814),
    background1 = Color(0xFF1A0F22),
    background2 = Color(0xFF281736),
    text = Color(0xFFFCE7F3),
    textSecondary = Color(0xFFF472B6),
    textDisabled = Color(0xFF831843),
    accent = Color(0xFFEC4899),
    onAccent = Color.White,
    isDark = true
)

val SunsetOLEDColorPalette = ColorPalette(
    background0 = Color.Black,
    background1 = Color(0xFF120C08),
    background2 = Color(0xFF201610),
    text = Color(0xFFFFF7ED),
    textSecondary = Color(0xFFFDBA74),
    textDisabled = Color(0xFF7C2D12),
    accent = Color(0xFFF97316),
    onAccent = Color.White,
    isDark = true
)

fun colorPaletteOf(
    colorPaletteName: ColorPaletteName,
    isSystemInDarkMode: Boolean = true
): ColorPalette {
    return when (colorPaletteName) {
        ColorPaletteName.System -> if (isSystemInDarkMode) DefaultDarkColorPalette else DefaultLightColorPalette
        ColorPaletteName.Dynamic -> if (isSystemInDarkMode) DefaultDarkColorPalette else DefaultLightColorPalette
        ColorPaletteName.DefaultDark -> DefaultDarkColorPalette
        ColorPaletteName.DefaultLight -> DefaultLightColorPalette
        ColorPaletteName.PureBlack -> PureBlackColorPalette
        ColorPaletteName.Nord -> NordDarkColorPalette
        ColorPaletteName.Dracula -> DraculaDarkColorPalette
        ColorPaletteName.Catppuccin -> CatppuccinDarkColorPalette
        ColorPaletteName.TokyoNight -> TokyoNightDarkColorPalette
        ColorPaletteName.EmeraldForest -> EmeraldForestDarkColorPalette
        ColorPaletteName.Cyberpunk -> CyberpunkDarkColorPalette
        ColorPaletteName.SunsetOLED -> SunsetOLEDColorPalette
    }
}

fun dynamicColorPaletteOf(bitmap: Bitmap, isDark: Boolean): ColorPalette? {
    val palette = Palette
        .from(bitmap)
        .maximumColorCount(16)
        .addFilter(if (isDark) ({ _, hsl -> hsl[0] !in 36f..100f }) else null)
        .generate()

    val primarySwatch = (if (isDark) palette.vibrantSwatch ?: palette.dominantSwatch else palette.vibrantSwatch ?: palette.dominantSwatch) ?: return null
    val primaryHsl = primarySwatch.hsl

    val secondarySwatch = palette.darkVibrantSwatch
        ?: palette.lightVibrantSwatch
        ?: palette.mutedSwatch
    val secondaryColor = secondarySwatch?.rgb?.let { Color(it) }

    val tertiarySwatch = palette.lightVibrantSwatch
        ?: palette.vibrantSwatch
        ?: palette.darkMutedSwatch
    val tertiaryColor = tertiarySwatch?.rgb?.let { Color(it) }

    return dynamicColorPaletteOf(
        hsl = primaryHsl,
        isDark = isDark,
        extractedSecondary = secondaryColor,
        extractedTertiary = tertiaryColor
    )
}

fun dynamicColorPaletteOf(
    hsl: FloatArray,
    isDark: Boolean,
    extractedSecondary: Color? = null,
    extractedTertiary: Color? = null
): ColorPalette {
    val fallbackSecondary = Color.hsl((hsl[0] + 35f) % 360f, hsl[1].coerceAtLeast(0.5f).coerceAtMost(0.85f), 0.55f)
    val fallbackTertiary = Color.hsl((hsl[0] + 180f) % 360f, hsl[1].coerceAtLeast(0.5f).coerceAtMost(0.85f), 0.55f)

    return colorPaletteOf(ColorPaletteName.Dynamic, isDark).copy(
        background0 = Color.hsl(hsl[0], hsl[1].coerceAtMost(0.1f), if (isDark) 0.10f else 0.925f),
        background1 = Color.hsl(hsl[0], hsl[1].coerceAtMost(0.3f), if (isDark) 0.15f else 0.90f),
        background2 = Color.hsl(hsl[0], hsl[1].coerceAtMost(0.4f), if (isDark) 0.2f else 0.85f),
        accent = Color.hsl(hsl[0], hsl[1].coerceAtMost(0.5f), 0.5f),
        red = extractedSecondary ?: fallbackSecondary,
        blue = extractedTertiary ?: fallbackTertiary,
        text = Color.hsl(hsl[0], hsl[1].coerceAtMost(0.02f), if (isDark) 0.88f else 0.12f),
        textSecondary = Color.hsl(hsl[0], hsl[1].coerceAtMost(0.1f), if (isDark) 0.65f else 0.40f),
        textDisabled = Color.hsl(hsl[0], hsl[1].coerceAtMost(0.2f), if (isDark) 0.40f else 0.65f),
    )
}

inline val ColorPalette.favoritesIcon: Color
    get() = if (this === DefaultDarkColorPalette || this === DefaultLightColorPalette || this === PureBlackColorPalette) {
        red
    } else {
        accent
    }

inline val ColorPalette.shimmer: Color
    get() = if (this === DefaultDarkColorPalette || this === DefaultLightColorPalette || this === PureBlackColorPalette) {
        Color(0xff838383)
    } else {
        accent
    }

inline val ColorPalette.glass: Color
    get() = if (isDark) background1 else background2

inline val ColorPalette.onGlass: Color
    get() = text

inline val ColorPalette.card: Color
    get() = background1

inline val ColorPalette.onCard: Color
    get() = text

inline val ColorPalette.primaryButton: Color
    get() = if (this === PureBlackColorPalette) {
        Color(0xFF272727)
    } else {
        background2
    }

inline val ColorPalette.overlay: Color
    get() = PureBlackColorPalette.background0.copy(alpha = 0.75f)

inline val ColorPalette.onOverlay: Color
    get() = PureBlackColorPalette.text

inline val ColorPalette.onOverlayShimmer: Color
    get() = PureBlackColorPalette.shimmer
