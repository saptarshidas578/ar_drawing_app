package com.tracear.app.ar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ImageAdjustments data class and clamping properties.
 */
class ImageAdjustmentsTest {

    @Test
    fun testDefaultAdjustments_IsDefaultTrue() {
        val adj = ImageAdjustments()
        assertTrue(adj.isDefault)
        assertEquals(0f, adj.brightness, 1e-4f)
        assertEquals(1.0f, adj.contrast, 1e-4f)
        assertEquals(0f, adj.invert, 1e-4f)
        assertEquals(0f, adj.bwThreshold, 1e-4f)
    }

    @Test
    fun testModifiedBrightness_IsDefaultFalse() {
        val adj = ImageAdjustments(brightness = 0.15f)
        assertFalse(adj.isDefault)
    }

    @Test
    fun testModifiedContrast_IsDefaultFalse() {
        val adj = ImageAdjustments(contrast = 1.5f)
        assertFalse(adj.isDefault)
    }

    @Test
    fun testModifiedInvert_IsDefaultFalse() {
        val adj = ImageAdjustments(invert = 1.0f)
        assertFalse(adj.isDefault)
    }

    @Test
    fun testModifiedBwThreshold_IsDefaultFalse() {
        val adj = ImageAdjustments(bwThreshold = 0.5f)
        assertFalse(adj.isDefault)
    }

    @Test
    fun testCopyAndReset() {
        val modified = ImageAdjustments(
            brightness = -0.5f,
            contrast = 2.0f,
            invert = 0.8f,
            bwThreshold = 0.4f
        )
        assertFalse(modified.isDefault)

        // Reset to default
        val reset = ImageAdjustments()
        assertTrue(reset.isDefault)
        assertEquals(0f, reset.brightness, 1e-4f)
        assertEquals(1.0f, reset.contrast, 1e-4f)
        assertEquals(0f, reset.invert, 1e-4f)
        assertEquals(0f, reset.bwThreshold, 1e-4f)
    }
}
