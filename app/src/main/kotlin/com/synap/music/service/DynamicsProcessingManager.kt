package com.synap.music.service

import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.DynamicsProcessing.*
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.P)
class DynamicsProcessingManager(audioSessionId: Int) {
    private var dynamicsProcessing: DynamicsProcessing? = null

    init {
        try {
            val builder = Config.Builder(
                VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                2, // channel count
                true, // preEq
                0, // preEq bands
                true, // mbc
                3, // mbc bands
                true, // postEq
                0, // postEq bands
                true // limiter
            )

            val config = builder.build()
            
            // Configure Multi-band Compressor (MBC) for DRC
            for (ch in 0 until 2) {
                val mbc = config.getChannelByChannelIndex(ch).mbc
                for (i in 0 until mbc.bandCount) {
                    val band = mbc.getBand(i)
                    band.isEnabled = true
                    band.attackTime = 50f
                    band.releaseTime = 200f
                    band.ratio = 2f
                    band.threshold = -20f
                    band.kneeWidth = 0f
                    band.noiseGateThreshold = -80f
                    band.expanderRatio = 1f
                    band.preGain = 0f
                    band.postGain = 0f
                }

                // Configure Limiter
                val limiter = config.getChannelByChannelIndex(ch).limiter
                limiter.isEnabled = true
                limiter.linkGroup = 0
                limiter.attackTime = 1f
                limiter.releaseTime = 60f
                limiter.ratio = 10f
                limiter.threshold = -1f
                limiter.postGain = 0f
            }

            dynamicsProcessing = DynamicsProcessing(0, audioSessionId, config)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var enabled: Boolean
        get() = dynamicsProcessing?.enabled ?: false
        set(value) {
            dynamicsProcessing?.enabled = value
        }

    fun release() {
        dynamicsProcessing?.release()
        dynamicsProcessing = null
    }
}
