package com.synap.music.service

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Build
import androidx.media3.common.MediaItem
import com.synap.music.enums.AudioEnhancementCategory
import com.synap.music.enums.LoudnessTarget
import com.synap.music.utils.isAtLeastAndroid9

/**
 * Manages audio DSP effects including Bass Boost, 3D Virtualization,
 * Dynamic Range Compression, and Auto-Genre Equalizer presets.
 */
@Suppress("DEPRECATION")
class AudioEnhancementManager(
    audioSessionId: Int
) {
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var equalizer: Equalizer? = null
    private var presetReverb: PresetReverb? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var dynamicsProcessingManager: DynamicsProcessingManager? = null
    
    private var currentCategory = AudioEnhancementCategory.Auto
    private var currentMediaItem: MediaItem? = null

    init {
        // Initialize the Android Audio Effects system for this specific playback session.
        try {
            bassBoost = BassBoost(0, audioSessionId)
            virtualizer = Virtualizer(0, audioSessionId)
            equalizer = Equalizer(0, audioSessionId)
            presetReverb = PresetReverb(0, audioSessionId)
            loudnessEnhancer = LoudnessEnhancer(audioSessionId)
            if (isAtLeastAndroid9) {
                dynamicsProcessingManager = DynamicsProcessingManager(audioSessionId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Probes Android system Spatializer capabilities (API 32+ / Android 12L+).
     * Safe for minSdk = 24. Returns true if native system spatialization is active on 
     * connected Bluetooth spatial headphones or XR headsets.
     */
    fun isNativeSystemSpatializerActive(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
            val spatializer = audioManager.spatializer
            return spatializer.isAvailable && spatializer.isEnabled
        }
        return false
    }

    /**
     * Mastering Settings.
     * Handles specialized effects like Dynamic Range Compression (DRC) 
     * and Spatial Mastering (Maximum 3D soundstage).
     */
    fun updateMasteringSettings(context: Context, drcEnabled: Boolean, spatialMasteringEnabled: Boolean) {
        if (isAtLeastAndroid9) {
            dynamicsProcessingManager?.enabled = drcEnabled
        }
        
        val isNativeSpatialActive = isNativeSystemSpatializerActive(context)

        if (spatialMasteringEnabled) {
            if (!isNativeSpatialActive) {
                // Engage software 3D virtualizer for standard stereo output
                virtualizer?.let {
                    it.enabled = true
                    it.setStrength(1000) 
                }
            } else {
                // Avoid double-processing when hardware/system Spatializer is already active
                virtualizer?.enabled = false
            }
        } else if (currentCategory != AudioEnhancementCategory.SpatialAudio) {
            // Go back to the standard settings for the current genre.
            applyCategory(currentCategory, currentMediaItem)
        }
    }

    /**
     * Loudness Target (LUFS) Mastering & Volume Normalization.
     * Adjusts LoudnessEnhancer target gain in millibels based on track loudness dB and chosen LUFS mastering profile.
     */
    fun updateLoudnessTarget(
        target: LoudnessTarget,
        loudnessDb: Float? = null,
        mediaItem: MediaItem? = currentMediaItem
    ) {
        val baseGainMb = if (loudnessDb != null && loudnessDb != 0f) {
            -((loudnessDb) * 100).toInt() + 300
        } else {
            0
        }

        val masteringGainMb = if (target == LoudnessTarget.Auto) {
            val title = mediaItem?.mediaMetadata?.title?.toString()?.lowercase() ?: ""
            val artist = mediaItem?.mediaMetadata?.artist?.toString()?.lowercase() ?: ""
            when {
                title.contains("acoustic") || title.contains("live") || title.contains("piano") ||
                title.contains("violin") || title.contains("jazz") || title.contains("classical") ||
                artist.contains("acoustic") -> LoudnessTarget.LUFS18.targetGainMb

                title.contains("phonk") || title.contains("trap") || title.contains("edm") ||
                title.contains("dance") || title.contains("house") || title.contains("boosted") -> LoudnessTarget.LUFS11.targetGainMb

                else -> LoudnessTarget.LUFS14.targetGainMb
            }
        } else {
            target.targetGainMb
        }

        val totalGainMb = (baseGainMb + masteringGainMb).coerceIn(-1500, 1500)

        loudnessEnhancer?.let {
            try {
                it.setTargetGain(totalGainMb)
                it.enabled = true
            } catch (_: Exception) { }
        }
    }

    /**
     * Headphone Crossfeed (BS2B Filter simulation).
     * Blends cross-channel audio into the virtualizer to reduce hard-panned stereo listening fatigue.
     */
    fun updateCrossfeed(enabled: Boolean) {
        if (enabled && virtualizer?.enabled == false) {
            virtualizer?.let {
                it.enabled = true
                it.setStrength(350) // 35% natural crossfeed blending
            }
        }
    }

    /**
     * Switch between different sound profiles (Jazz, Rock, Bass Boost, etc.).
     */
    fun applyCategory(category: AudioEnhancementCategory, mediaItem: MediaItem?) {
        currentCategory = category
        currentMediaItem = mediaItem
        
        disableAll()
        
        when (category) {
            AudioEnhancementCategory.Auto -> applyAuto(mediaItem)
            AudioEnhancementCategory.Equalizer -> applyEqualizer()
            AudioEnhancementCategory.Oboe -> applyOboe() // Pure direct sound.
            AudioEnhancementCategory.SpatialAudio -> applySpatialAudio()
            AudioEnhancementCategory.Surround51 -> applySurround51()
            AudioEnhancementCategory.Surround71 -> applySurround71()
        }
    }

    private fun disableAll() {
        bassBoost?.enabled = false
        virtualizer?.enabled = false
        equalizer?.enabled = false
        loudnessEnhancer?.enabled = false
    }

    /**
     * The Smart "Auto-Genre" Detector.
     * It scans the track title and artist name for keywords and automatically 
     * switches the EQ profile to match the music.
     */
    private fun applyAuto(mediaItem: MediaItem?) {
        val title = mediaItem?.mediaMetadata?.title?.toString()?.lowercase() ?: ""
        val artist = mediaItem?.mediaMetadata?.artist?.toString()?.lowercase() ?: ""

        // Check for special formats first.
        if (title.contains("5.1") || title.contains("surround") || title.contains("dts") || title.contains("ac3")) {
            applySurround51()
            return
        }
        if (title.contains("7.1")) {
            applySurround71()
            return
        }
        if (title.contains("360") || title.contains("8d") || title.contains("ambisonic") || title.contains("spatial")) {
            applySpatialAudio()
            return
        }

        when {
            // Phonk, Trap, Hardstyle -> Engage Extreme Bass Mode.
            title.contains("bass") || title.contains("boosted") || title.contains("low end") ||
            title.contains("phonk") || title.contains("trap") || title.contains("hardstyle") ||
            title.contains("subwoofer") -> applyBoostX()

            // EDM, Dance, House -> Punchy Bass & Crisp Highs.
            title.contains("remix") || title.contains("dance") || title.contains("edm") ||
            title.contains("house") || title.contains("techno") || title.contains("electro") -> applyElectronic()

            // Acoustic, Live, Vocal -> Focus on Mid-range clarity.
            title.contains("acoustic") || title.contains("live") || title.contains("unplugged") ||
            title.contains("vocal") || title.contains("piano") || title.contains("violin") ||
            artist.contains("acoustic") || artist.contains("unplugged") -> applyClearVocal()

            // Rock, Metal, Punk -> Boost guitars and drums.
            title.contains("rock") || title.contains("metal") || title.contains("heavy") ||
            title.contains("punk") || title.contains("alternative") || title.contains("guitar") -> applyRock()

            // Default balanced profile.
            else -> applyProSoundEQ()
        }
    }

    private fun applyProSoundEQ() {
        // High fidelity profile: Balanced EQ + subtle virtualization (Dolby style)
        equalizer?.let {
            it.enabled = true
            if (it.numberOfBands >= 5) {
                it.setBandLevel(0, 200)  // Sub-bass
                it.setBandLevel(1, 100)  // Bass
                it.setBandLevel(2, 0)    // Mids
                it.setBandLevel(3, 150)  // High-mids
                it.setBandLevel(4, 250)  // Highs (Clarity)
            }
        }
        
        virtualizer?.let {
            it.enabled = true
            it.setStrength(400) // 40% spatialization
        }

        bassBoost?.let {
            it.enabled = true
            it.setStrength(200) // Subtle punch
        }
    }

    private fun applyElectronic() {
        // Vibrant & Punchy: Boosted Bass and Treble, V-shaped
        equalizer?.let {
            it.enabled = true
            if (it.numberOfBands >= 5) {
                it.setBandLevel(0, 400)
                it.setBandLevel(1, 200)
                it.setBandLevel(2, -100)
                it.setBandLevel(3, 200)
                it.setBandLevel(4, 500)
            }
        }

        virtualizer?.let {
            it.enabled = true
            it.setStrength(600)
        }

        bassBoost?.let {
            it.enabled = true
            it.setStrength(500)
        }
    }

    private fun applyClearVocal() {
        // Focused on Mid-Highs for vocals and instruments
        equalizer?.let {
            it.enabled = true
            if (it.numberOfBands >= 5) {
                it.setBandLevel(0, -200)
                it.setBandLevel(1, 0)
                it.setBandLevel(2, 300) // Vocal clarity
                it.setBandLevel(3, 400) // Presence
                it.setBandLevel(4, 200)
            }
        }

        virtualizer?.let {
            it.enabled = true
            it.setStrength(800) // Wide airy soundstage
        }
    }

    private fun applyRock() {
        // Warm & Aggressive: Boosted Low-Mids and Bass
        equalizer?.let {
            it.enabled = true
            if (it.numberOfBands >= 5) {
                it.setBandLevel(0, 300)
                it.setBandLevel(1, 400) // Kick & Snare
                it.setBandLevel(2, 200) // Guitars
                it.setBandLevel(3, 100)
                it.setBandLevel(4, 0)
            }
        }
        bassBoost?.let {
            it.enabled = true
            it.setStrength(400)
        }
    }

    private fun applyBassBoost() {
        bassBoost?.let {
            it.enabled = true
            it.setStrength(1000) // Max standard bass boost
        }
    }

    private fun applyBoostX() {
        // Extreme Bass: Max BassBoost + LoudnessEnhancer for punch
        bassBoost?.let {
            it.enabled = true
            it.setStrength(1000)
        }
        loudnessEnhancer?.let {
            it.enabled = true
            it.setTargetGain(500) // 5dB boost
        }
        equalizer?.let {
            it.enabled = true
            if (it.numberOfBands >= 5) {
                it.setBandLevel(0, 500) // Extreme low bass
                it.setBandLevel(1, 300)
            }
        }
    }

    private fun applyOboe() {
        // Oboe profile: No effects for lowest latency and pure sound
    }

    private fun applyEqualizer() {
        equalizer?.enabled = true
    }

    private fun applySpatialAudio() {
        // High-end binaural simulation: Max virtualization with "Airy" EQ
        virtualizer?.let {
            it.enabled = true
            it.setStrength(1000)
        }
        equalizer?.let {
            it.enabled = true
            if (it.numberOfBands >= 5) {
                it.setBandLevel(0, 100)
                it.setBandLevel(4, 400) // High-end sparkle
            }
        }
    }

    private fun applyAmbisonics() {
        // Spherical soundstage: Wide virtualization + Balanced EQ
        virtualizer?.let {
            it.enabled = true
            it.setStrength(800)
        }
        applyProSoundEQ()
    }

    private fun applySurround51() {
        // 5.1 Simulation: Focused virtualization + Punchy Bass
        virtualizer?.let {
            it.enabled = true
            it.setStrength(700)
        }
        bassBoost?.let {
            it.enabled = true
            it.setStrength(400)
        }
    }

    private fun applySurround71() {
        // 7.1 Simulation: Deep virtualization + Sub-bass focus
        virtualizer?.let {
            it.enabled = true
            it.setStrength(900)
        }
        bassBoost?.let {
            it.enabled = true
            it.setStrength(500)
        }
    }

    fun getHardwareEqualizer(): Equalizer? = equalizer

    fun applyCustomEQ(bandLevels: ShortArray) {
        currentCategory = AudioEnhancementCategory.Equalizer
        try {
            val eq = equalizer ?: return
            eq.enabled = true
            val numBands = eq.numberOfBands.toInt()
            for (i in 0 until minOf(numBands, bandLevels.size)) {
                eq.setBandLevel(i.toShort(), bandLevels[i])
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applyBassBoostStrength(strengthShort: Short) {
        try {
            val bb = bassBoost ?: return
            bb.enabled = strengthShort > 0
            if (strengthShort > 0) {
                bb.setStrength(strengthShort)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applyVirtualizerStrength(strengthShort: Short) {
        try {
            val virt = virtualizer ?: return
            virt.enabled = strengthShort > 0
            if (strengthShort > 0) {
                virt.setStrength(strengthShort)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applyReverbPreset(presetShort: Short) {
        try {
            val rev = presetReverb ?: return
            rev.enabled = presetShort > 0
            if (presetShort > 0) {
                rev.preset = presetShort
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        bassBoost?.release()
        virtualizer?.release()
        equalizer?.release()
        presetReverb?.release()
        loudnessEnhancer?.release()
        if (isAtLeastAndroid9) {
            dynamicsProcessingManager?.release()
        }
    }
}
