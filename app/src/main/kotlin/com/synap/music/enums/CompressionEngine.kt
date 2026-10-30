package com.synap.music.enums

enum class CompressionEngine(val label: String, val subtitle: String) {
    Auto("Auto", "Hardware-Adaptive Smart Selection"),
    Opus64("Opus 64k", "Data Saver Ultra-Low (~64 kbps)"),
    Opus92("Opus 92k", "Ultra Efficient Opus (~92 kbps)"),
    Opus("Opus 128k", "Next-Gen Lossy Opus (~128 kbps)"),
    Opus192("Opus 192k", "Audiophile Transparent Opus (~192 kbps)"),
    Opus256("Opus 256k", "High-Definition Opus (~256 kbps)"),
    Opus510("Opus 510k", "Spatial / Max Spec Opus (~510 kbps)"),
    AAC192("AAC 192k", "Lossy Universal AAC (~192 kbps)"),
    AAC("AAC 256k", "Lossy Universal AAC (~256 kbps)"),
    FLAC("FLAC", "Lossless FLAC (~800-1000 kbps, 16-bit/44.1kHz)"),
    SLAC("SLAC", "Fast Lossless FLAC (~850-1050 kbps)"),
    SQZ("SQZ", "Squeeze Level-8 FLAC (~750-900 kbps)"),
    WavPack("WavPack", "Hybrid Lossless (~650-850 kbps)"),
    L3AC("L3AC", "Audiophile Hi-Res WavPack (~1000-1300 kbps, 24-bit/48kHz)"),
    SAC("SAC", "Super Audio Hi-Res WavPack (~1100-1400 kbps, 24-bit/48kHz)");

    val isLossless: Boolean
        get() = when (this) {
            Auto, FLAC, WavPack, L3AC, SQZ, SLAC, SAC -> true
            AAC, AAC192, Opus, Opus64, Opus92, Opus192, Opus256, Opus510 -> false
        }

    val badgeText: String?
        get() = badgeCodecText

    val badgeCodecText: String?
        get() = when (this) {
            SAC, L3AC, WavPack -> "WV"
            FLAC, SLAC, SQZ -> "FLAC"
            AAC, AAC192 -> "AAC"
            Opus, Opus64, Opus92, Opus192, Opus256, Opus510 -> "OPUS"
            else -> null
        }

    val badgeFidelityText: String?
        get() = when (this) {
            SAC, L3AC -> "HI-RES"
            FLAC, WavPack, SLAC, SQZ -> "LOSSLESS"
            Opus256, Opus510, AAC -> "HI-FI"
            Opus, Opus192, AAC192 -> "MID-FI"
            Opus64, Opus92 -> "LO-FI"
            else -> null
        }
}
