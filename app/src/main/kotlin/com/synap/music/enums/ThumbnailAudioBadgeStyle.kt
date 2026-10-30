package com.synap.music.enums

enum class ThumbnailAudioBadgeStyle(val label: String, val subtitle: String) {
    CodecName("Codec Name", "Show exact audio codec (e.g. OPUS, AAC, FLAC, WV)"),
    FidelityRank("Fidelity Rank", "Show audiophile rank (e.g. LO-FI, MID-FI, HI-FI, LOSSLESS, HI-RES)"),
    Off("Off", "Hide audio quality badges on thumbnails")
}
