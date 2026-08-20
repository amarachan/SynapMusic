package com.synap.pulse.models

import kotlinx.serialization.Serializable

@Serializable
data class PlayerResponse(
    val playabilityStatus: PlayabilityStatus?,
    val playerConfig: PlayerConfig?,
    val streamingData: StreamingData?,
    val videoDetails: VideoDetails?,
) {
    @Serializable
    data class PlayabilityStatus(
        val status: String?
    )

    @Serializable
    data class PlayerConfig(
        val audioConfig: AudioConfig?
    ) {
        @Serializable
        data class AudioConfig(
            private val loudnessDb: Double?
        ) {
            // For music clients only
            val normalizedLoudnessDb: Float?
                get() = loudnessDb?.plus(7)?.toFloat()
        }
    }

    @Serializable
    data class StreamingData(
        val adaptiveFormats: List<AdaptiveFormat>?
    ) {
        val highestQualityFormat: AdaptiveFormat?
            get() {
                val formats = adaptiveFormats ?: return null
                return formats.filter { it.url != null }.let { plainFormats ->
                    plainFormats.findLast { it.itag == 251 || it.itag == 140 || it.itag == 171 }
                        ?: plainFormats.findLast { it.itag == 250 || it.itag == 249 || it.itag == 139 }
                        ?: plainFormats.lastOrNull()
                } ?: formats.findLast { it.itag == 251 || it.itag == 140 || it.itag == 171 }
                ?: formats.findLast { it.itag == 250 || it.itag == 249 || it.itag == 139 }
                ?: formats.lastOrNull()
            }

        @Serializable
        data class AdaptiveFormat(
            val itag: Int,
            val mimeType: String,
            val bitrate: Long?,
            val averageBitrate: Long?,
            val contentLength: Long?,
            val audioQuality: String?,
            val approxDurationMs: Long?,
            val lastModified: Long?,
            val loudnessDb: Double?,
            val audioSampleRate: Int?,
            val url: String?,
            val signatureCipher: String? = null,
        )
    }

    @Serializable
    data class VideoDetails(
        val videoId: String?
    )
}
