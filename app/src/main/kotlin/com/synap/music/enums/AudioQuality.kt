package com.synap.music.enums

enum class AudioQuality(val label: String, val subtitle: String) {
    Auto("Auto", "Smart Network Adaptation"),
    Low("Low (64k)", "Data Saver (64 kbps Opus)"),
    Medium("Medium (92k)", "Saver Plus (92 kbps Opus)"),
    Normal("Normal (128k)", "Standard Quality (128 kbps Opus / AAC)"),
    High("High (160k)", "High Fidelity (160 kbps Opus / AAC)"),
    Ultra("Ultra (192k)", "Audiophile Transparency (192 kbps Opus / AAC)"),
    Surround51("5.1 Surround (256k)", "5.1 Surround Sound & High Definition (256 kbps)"),
    Surround71("7.1 Spatial (510k)", "7.1 Spatial Audio & Max Spec (510 kbps Opus)")
}
