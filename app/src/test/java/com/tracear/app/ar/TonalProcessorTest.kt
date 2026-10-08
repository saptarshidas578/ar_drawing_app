package com.tracear.app.ar

import com.tracear.app.data.NormalizedTones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TonalProcessorTest {

    @Test
    fun testLuminanceRec601() {
        assertEquals(0, TonalMath.computeLuminance(0, 0, 0))
        assertEquals(255, TonalMath.computeLuminance(255, 255, 255))
        assertEquals(76, TonalMath.computeLuminance(255, 0, 0))   // 0.299 * 255 ~ 76
        assertEquals(150, TonalMath.computeLuminance(0, 255, 0))  // 0.587 * 255 ~ 150
        assertEquals(29, TonalMath.computeLuminance(0, 0, 255))   // 0.114 * 255 ~ 29
    }

    @Test
    fun testQuantizationFourTones() {
        val n = 4
        // Tone 0 = Highlights (inverted luminance 0..63)
        assertEquals(0, TonalMath.quantizeLuminance(255, n))
        assertEquals(0, TonalMath.quantizeLuminance(200, n))

        // Tone 1 = Light Midtone (inverted luminance 64..127)
        assertEquals(1, TonalMath.quantizeLuminance(160, n))
        assertEquals(1, TonalMath.quantizeLuminance(130, n))

        // Tone 2 = Dark Midtone (inverted luminance 128..191)
        assertEquals(2, TonalMath.quantizeLuminance(100, n))
        assertEquals(2, TonalMath.quantizeLuminance(70, n))

        // Tone 3 = Shadows (inverted luminance 192..255)
        assertEquals(3, TonalMath.quantizeLuminance(50, n))
        assertEquals(3, TonalMath.quantizeLuminance(0, n))
    }

    @Test
    fun testQuantizationEdgeCases() {
        // Safe handling of clamping
        assertEquals(0, TonalMath.quantizeLuminance(300, 4))
        assertEquals(3, TonalMath.quantizeLuminance(-50, 4))
        assertEquals(0, TonalMath.quantizeLuminance(128, 1)) // Clamps to 2 tones fallback
    }

    @Test
    fun testHexFormatting() {
        assertEquals("#00E5FF", TonalMath.toHex(0xFF00E5FF.toInt()))
        assertEquals("#FFFFFF", TonalMath.toHex(0xFFFFFFFF.toInt()))
        assertEquals("#000000", TonalMath.toHex(0xFF000000.toInt()))
        assertEquals("#123456", TonalMath.toHex(0xFF123456.toInt()))
    }

    @Test
    fun testDefaultRamps() {
        val configs4 = TonalMath.getDefaultLayerConfigs(4)
        assertEquals(4, configs4.size)
        assertEquals("Highlights", configs4[0].name)
        assertEquals("Light Midtone", configs4[1].name)
        assertEquals("Dark Midtone", configs4[2].name)
        assertEquals("Shadows", configs4[3].name)

        // Opacity should ramp upwards from highlight to shadow
        assertTrue(configs4[0].opacity < configs4[1].opacity)
        assertTrue(configs4[1].opacity < configs4[2].opacity)
        assertTrue(configs4[2].opacity < configs4[3].opacity)

        val configs2 = TonalMath.getDefaultLayerConfigs(2)
        assertEquals(2, configs2.size)
        assertEquals("Lights", configs2[0].name)
        assertEquals("Shadows", configs2[1].name)
    }

    @Test
    fun testTonalStateStagesAndSolo() {
        val state = TonalState().apply {
            isEnabled = true
            updateToneCount(4)
            isStagesMode = true
        }

        assertEquals(0, state.currentStage)
        assertEquals("Stage 1/5: Outline", state.currentStageLabel)

        state.nextStage()
        assertEquals(1, state.currentStage)
        assertEquals("Stage 2/5: Highlights", state.currentStageLabel)

        state.nextStage()
        assertEquals(2, state.currentStage)
        assertEquals("Stage 3/5: Light Midtone", state.currentStageLabel)

        state.prevStage()
        assertEquals(1, state.currentStage)

        // Test solo toggle
        state.toggleSolo(2)
        assertEquals(2, state.soloLayerId)
        state.toggleSolo(2)
        assertEquals(null, state.soloLayerId)
    }

    @Test
    fun testTonalStateSerializationRoundTrip() {
        val state = TonalState().apply {
            isEnabled = true
            updateToneCount(3)
            smoothingLevel = 0.75f
            isStagesMode = true
            currentStage = 2
            isOutlineVisible = false
            outlineColorArgb = 0xFFFF3D00.toInt()
            outlineOpacity = 0.8f
        }

        val data = state.toData()
        assertEquals(true, data.isEnabled)
        assertEquals(3, data.toneCount)
        assertEquals(0.75f, data.smoothingLevel, 1e-4f)
        assertEquals(true, data.isStagesMode)
        assertEquals(2, data.currentStage)
        assertEquals(false, data.isOutlineVisible)
        assertEquals(0xFFFF3D00.toInt(), data.outlineColorArgb)
        assertEquals(0.8f, data.outlineOpacity, 1e-4f)

        // JSON roundtrip of NormalizedTones
        val json = data.toJson()
        assertNotNull(json)
        val restoredData = NormalizedTones.fromJson(json)
        assertEquals(data.isEnabled, restoredData.isEnabled)
        assertEquals(data.toneCount, restoredData.toneCount)
        assertEquals(data.smoothingLevel, restoredData.smoothingLevel, 1e-4f)
        assertEquals(data.isStagesMode, restoredData.isStagesMode)
        assertEquals(data.currentStage, restoredData.currentStage)
        assertEquals(data.isOutlineVisible, restoredData.isOutlineVisible)

        val restoredState = TonalState()
        restoredState.applyData(restoredData)
        assertEquals(true, restoredState.isEnabled)
        assertEquals(3, restoredState.toneCount)
        assertEquals(0.75f, restoredState.smoothingLevel, 1e-4f)
        assertEquals(true, restoredState.isStagesMode)
        assertEquals(2, restoredState.currentStage)
        assertEquals(false, restoredState.isOutlineVisible)

        restoredState.reset()
        assertFalse(restoredState.isEnabled)
        assertEquals(4, restoredState.toneCount)
    }
}
