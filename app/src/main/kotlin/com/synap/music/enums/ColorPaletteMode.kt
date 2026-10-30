package com.synap.music.enums

enum class ColorPaletteMode(val label: String) {
    Light("Light Mode"),
    Dark("Dark Mode"),
    System("Follow System"),
    OLED("OLED Pitch Black"),
    Dimmed("Dimmed Night");

    override fun toString(): String = label
}
