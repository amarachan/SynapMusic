package com.synap.music.enums

/**
 * High-level Audio Enhancement Sound Modes.
 */
enum class AudioEnhancementCategory(val label: String) {
    Auto("Smart Auto-Genre"),
    Equalizer("Graphic Equalizer & Bass Boost"),
    Oboe("Pure Direct (No DSP)"),
    SpatialAudio("3D Spatial Soundstage"),
    Surround51("5.1 Multi-Channel Surround"),
    Surround71("7.1 Spatial Surround");

    override fun toString(): String = label
}
