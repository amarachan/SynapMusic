package com.synap.music.enums

enum class LoudnessTarget(val label: String, val subtitle: String, val targetGainMb: Int) {
    Auto("Auto", "Smart Auto-Genre Adaptation", 300),
    LUFS14("-14 LUFS", "Streaming Standard (-14 LUFS)", 300),
    LUFS11("-11 LUFS", "Punchy Club Loudness (-11 LUFS)", 600),
    LUFS18("-18 LUFS", "Audiophile Dynamic (-18 LUFS)", 0)
}
