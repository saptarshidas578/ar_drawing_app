package com.tracear.app.ar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PaperLockFilterTest {

    private lateinit var filter: PaperLockFilter

    @Before
    fun setUp() {
        filter = PaperLockFilter()
        filter.reset(PaperFrame.IDENTITY, nowMs = 1000L)
    }

    @Test
    fun testDeadband_IgnoresMicroNoise() {
        // Change is 0.3mm (below 0.5mm threshold) and 0.05 deg (below 0.1 deg threshold)
        val noisyPose = PaperFrame(dx = 0.0003f, dz = 0.0002f, rotationDegrees = 0.05f)

        val output = filter.filter(noisyPose, nowMs = 1100L)
        // Should remain exact identity
        assertEquals(0f, output.dx, 1e-6f)
        assertEquals(0f, output.dz, 1e-6f)
        assertEquals(0f, output.rotationDegrees, 1e-6f)
    }

    @Test
    fun testSmallNudge_SmoothsImmediately() {
        // Nudge of 3mm (above deadband 0.5mm, below jump 10mm)
        val nudge = PaperFrame(dx = 0.003f, dz = 0.002f, rotationDegrees = 0.5f)

        val output = filter.filter(nudge, nowMs = 1100L)
        // With normal alpha = 0.35:
        val expectedDx = 0f + 0.35f * 0.003f
        val expectedDz = 0f + 0.35f * 0.002f

        assertEquals(expectedDx, output.dx, 1e-4f)
        assertEquals(expectedDz, output.dz, 1e-4f)
    }

    @Test
    fun testLargeJump_RequiresThreeConsistentCycles() {
        // Jump of 2 cm (20mm, above jump threshold 10mm)
        val jump = PaperFrame(dx = 0.020f, dz = 0.015f, rotationDegrees = 5.0f)

        // Cycle 1: should hold old pose
        val out1 = filter.filter(jump, nowMs = 1100L)
        assertEquals(0f, out1.dx, 1e-6f)

        // Cycle 2: should still hold old pose
        val out2 = filter.filter(jump, nowMs = 1200L)
        assertEquals(0f, out2.dx, 1e-6f)

        // Cycle 3: confirmed! Fast catchup applies (alpha = 0.85)
        val out3 = filter.filter(jump, nowMs = 1300L)
        assertTrue(out3.dx > 0.015f) // Has jumped towards 0.020m
    }

    @Test
    fun testTemplateRefreshPolicy_StillTimeAndConfidence() {
        // Case 1: Confidence too low (<0.80) -> false
        assertFalse(filter.shouldRefreshTemplate(confidence = 0.70f, nowMs = 5000L))

        // Case 2: Stationary for only 1.5 seconds (<3.0s) -> false
        filter.filter(PaperFrame(dx = 0.003f, dz = 0f, rotationDegrees = 0f), nowMs = 2000L)
        assertFalse(filter.shouldRefreshTemplate(confidence = 0.90f, nowMs = 3500L))

        // Case 3: Stationary for 3.5 seconds with high confidence -> true
        assertTrue(filter.shouldRefreshTemplate(confidence = 0.90f, nowMs = 5600L))
    }
}
