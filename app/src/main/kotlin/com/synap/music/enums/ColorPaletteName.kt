package com.synap.music.enums

enum class ColorPaletteName(val label: String) {
    System("Follow System"),
    Dynamic("Dynamic Album Art"),
    DefaultDark("Default Dark"),
    DefaultLight("Default Light"),
    PureBlack("Pure Black OLED"),
    Nord("Nord Slate"),
    Dracula("Dracula Cyber"),
    Catppuccin("Catppuccin Mocha"),
    TokyoNight("Tokyo Night"),
    EmeraldForest("Emerald Forest"),
    Cyberpunk("Cyberpunk Obsidian"),
    SunsetOLED("Sunset OLED");

    override fun toString(): String = label
}
