package com.synap.music.service

import com.synap.music.enums.CompressionEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class CompressionAutoTest {

    @Test
    fun testExplicitEngines() {
        // We verify that if a user manually picks an engine, it is respected.
        // We can't test "Auto" here easily because it requires a real Android Context for battery/RAM info.
        // But we can verify the logic that doesn't depend on Context.
        
        // This is a minimal test to ensure the file compiles and basic enum matching works.
        assertEquals("flac", CompressionManager.getExtension(CompressionEngine.FLAC))
        assertEquals("wv", CompressionManager.getExtension(CompressionEngine.WavPack))
    }
}
