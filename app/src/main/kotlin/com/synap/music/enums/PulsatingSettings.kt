package com.synap.music.enums

import androidx.compose.ui.graphics.Color

/**
 * Color intensity levels for pulsating animations.
 */
enum class PulsatingIntensity(val label: String, val alphaFactor: Float) {
    Soft("Soft & Pastel", 0.12f),
    Balanced("Balanced (Default)", 0.22f),
    Vibrant("Vibrant", 0.40f),
    DeepNeon("Deep Neon", 0.70f);

    override fun toString(): String = label
}

/**
 * Speed options for pulsating breathing animations.
 */
enum class PulsatingSpeed(val label: String, val durationMs: Int) {
    Slow("Calm & Relaxed (3.6s)", 3600),
    Natural("Natural (2.4s)", 2400),
    Fast("Fast & Dynamic (1.2s)", 1200);

    override fun toString(): String = label
}

/**
 * Easing curves for pulsating animations.
 */
enum class PulsatingEffectType(val label: String) {
    BreathingWave("Breathing Wave (Smooth)"),
    BlinkingPulse("Blinking Pulse (Rhythmic)"),
    SmoothFade("Smooth Fade (Subtle)");

    override fun toString(): String = label
}

/**
 * Color palette presets for tiles and screen animations.
 */
enum class PulsatingColorPalette(
    val label: String,
    val primary: Color?,
    val secondary: Color?,
    val tertiary: Color? = null,
    val quaternary: Color? = null
) {
    AlbumArtDynamic("Album Art / Dynamic Theme (Harmonized 3x)", null, null, null, null),
    CyberpunkNeon("Cyberpunk Neon (Hot Pink & Cyan)", Color(0xFFE91E63), Color(0xFF00BCD4)),
    SunsetGold("Sunset Gold (Amber & Rose)", Color(0xFFFF9800), Color(0xFFE91E63)),
    NorthernLights("Northern Lights (Emerald & Aqua)", Color(0xFF4CAF50), Color(0xFF22D3EE)),
    DeepSpace("Deep Space (Violet & Indigo)", Color(0xFF3F51B5), Color(0xFF9C27B0)),
    OceanBreeze("Ocean Breeze (Sapphire & Cyan)", Color(0xFF1E88E5), Color(0xFF00E5FF)),
    CrimsonFlame("Crimson Flame (Ruby Red & Gold)", Color(0xFFE53935), Color(0xFFFFC107)),
    ElectricMidnight("Electric Midnight (Purple & Neon Pink)", Color(0xFF673AB7), Color(0xFFFF007F)),
    MintFresh("Mint Fresh (Emerald & Ice Blue)", Color(0xFF00BFA5), Color(0xFF80D8FF)),
    SakuraBlossom("Sakura Blossom (Cherry & Lilac)", Color(0xFFF48FB1), Color(0xFFCE93D8)),
    SolarEclipse("Solar Eclipse (Solar Orange & Gold)", Color(0xFFFF5722), Color(0xFFFFD54F)),
    UltravioletNeon("Ultraviolet Neon (Electric Violet & Cyan)", Color(0xFF8E24AA), Color(0xFF2979FF)),

    // Triple Color Hybrid Mixes (3x)
    TriadCyberpunk("Cyberpunk Triad 3x (Hot Pink & Aqua & Violet)", Color(0xFFE91E63), Color(0xFF00BCD4), Color(0xFF9C27B0)),
    NeonPrism("Neon Prism 3x (Magenta & Lime & Gold)", Color(0xFFFF007F), Color(0xFF76FF03), Color(0xFFFFC107)),
    SunsetHorizon("Sunset Horizon 3x (Crimson & Amber & Orchid)", Color(0xFFE53935), Color(0xFFFF9800), Color(0xFF8E24AA)),
    BorealisTriad("Borealis Triad 3x (Emerald & Blue & Neon Purple)", Color(0xFF00E676), Color(0xFF2979FF), Color(0xFFD500F9)),
    CosmicGalaxy("Cosmic Galaxy 3x (Indigo & Magenta & Ocean Cyan)", Color(0xFF3D5AFE), Color(0xFFF50057), Color(0xFF00E5FF)),
    TropicalFiesta("Tropical Fiesta 3x (Coral & Yellow & Mint Cyan)", Color(0xFFFF5252), Color(0xFFFFEA00), Color(0xFF1DE9B6)),
    InfernoTriad("Inferno Triad 3x (Ruby Red & Orange & Gold)", Color(0xFFD50000), Color(0xFFFF6D00), Color(0xFFFFD600)),
    CyberMatrix("Cyber Matrix 3x (Neon Green & Cyan & Deep Teal)", Color(0xFF00E676), Color(0xFF18FFFF), Color(0xFF006064)),

    // Quad-Color Spectrum Wave Mixes (4x)
    SpectrumPrism4x("Spectrum Prism 4x (Pink & Gold & Mint & Cyan)", Color(0xFFFF007F), Color(0xFFFFC107), Color(0xFF00E676), Color(0xFF00E5FF)),
    AuroraBorealis4x("Aurora Borealis 4x (Emerald & Cyan & Sapphire & Purple)", Color(0xFF00C853), Color(0xFF00E5FF), Color(0xFF2979FF), Color(0xFFAA00FF)),
    CyberOverdrive4x("Cyber Overdrive 4x (Hot Pink & Neon Violet & Blue & Aqua)", Color(0xFFFF007F), Color(0xFF7C4DFF), Color(0xFF2979FF), Color(0xFF18FFFF)),
    SolarFlare4x("Solar Flare 4x (Ruby & Orange & Gold & Amber)", Color(0xFFD50000), Color(0xFFFF6D00), Color(0xFFFFD600), Color(0xFFFF9100));

    override fun toString(): String = label
}

/**
 * Presets for action buttons and floating search bars.
 */
enum class PulsatingButtonColorPreset(
    val label: String,
    val color: Color?,
    val secondaryColor: Color? = null,
    val tertiaryColor: Color? = null,
    val quaternaryColor: Color? = null
) {
    DynamicTheme("Dynamic Theme Accent", null),
    PulsatingHybrid("Pulsating Hybrid Glow (Matches Palette)", null),

    // Static Single-Tint Presets
    CyberpunkPink("Cyberpunk Pink", Color(0xFFE91E63)),
    OceanSapphire("Ocean Sapphire", Color(0xFF2196F3)),
    EmeraldMint("Emerald Mint", Color(0xFF1DB954)),
    SunsetGold("Sunset Gold", Color(0xFFFF9800)),
    DeepPurple("Deep Purple", Color(0xFF9C27B0)),
    ElectricCyan("Electric Cyan", Color(0xFF00E5FF)),
    RubyCrimson("Ruby Crimson", Color(0xFFE53935)),
    SolarOrange("Solar Orange", Color(0xFFFF5722)),
    SakuraPink("Sakura Pink", Color(0xFFF48FB1)),

    // Dedicated Button & Floating Search Multi-Color Hybrid Glows
    TriadCyberpunkGlow("3x Cyberpunk Triad Glow (Pink & Cyan & Violet)", Color(0xFFE91E63), Color(0xFF00BCD4), Color(0xFF9C27B0)),
    NeonPrismGlow("3x Neon Prism Glow (Magenta & Lime & Gold)", Color(0xFFFF007F), Color(0xFF76FF03), Color(0xFFFFC107)),
    SunsetHorizonGlow("3x Sunset Horizon Glow (Crimson & Amber & Orchid)", Color(0xFFE53935), Color(0xFFFF9800), Color(0xFF8E24AA)),
    BorealisGlow("3x Borealis Glow (Emerald & Blue & Neon Purple)", Color(0xFF00E676), Color(0xFF2979FF), Color(0xFFD500F9)),
    CosmicGalaxyGlow("3x Cosmic Galaxy Glow (Indigo & Magenta & Cyan)", Color(0xFF3D5AFE), Color(0xFFF50057), Color(0xFF00E5FF)),
    InfernoTriadGlow("3x Inferno Triad Glow (Ruby & Orange & Gold)", Color(0xFFD50000), Color(0xFFFF6D00), Color(0xFFFFD600)),
    SpectrumPrism4xGlow("4x Spectrum Prism Wave (Pink & Gold & Mint & Cyan)", Color(0xFFFF007F), Color(0xFFFFC107), Color(0xFF00E676), Color(0xFF00E5FF)),
    AuroraBorealis4xGlow("4x Aurora Borealis Wave (Emerald & Cyan & Sapphire & Purple)", Color(0xFF00C853), Color(0xFF00E5FF), Color(0xFF2979FF), Color(0xFFAA00FF));

    override fun toString(): String = label
}
