package com.synap.music.enums

/**
 * Supported Lyrics Language Preferences.
 */
enum class LyricsLanguage(val label: String, val code: String) {
    Auto("Auto-detect", "auto"),
    English("English", "en"),
    Spanish("Spanish", "es"),
    French("French", "fr"),
    German("German", "de"),
    Japanese("Japanese", "ja"),
    Korean("Korean", "ko"),
    Chinese("Chinese", "zh"),
    Hindi("Hindi", "hi"),
    Portuguese("Portuguese", "pt"),
    Italian("Italian", "it"),
    Russian("Russian", "ru"),
    Arabic("Arabic", "ar"),
    Tagalog("Tagalog", "tl"),
    Vietnamese("Vietnamese", "vi");

    override fun toString(): String = label
}
