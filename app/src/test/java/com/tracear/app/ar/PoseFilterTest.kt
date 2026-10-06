package com.tracear.app.ar

import com.google.ar.core.Pose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Unit tests for PoseFilter verifying:
 * 1. SmoothingMode.OFF passes raw pose through with 0 lag.
 * 2. Static position hold converges to raw anchor position.
 * 3. Fast movements adapt cutoff frequency to prevent dragging lag.
 * 4. Micro-vibrations are effectively smoothed.
 * 5. Rotations remain normalized unit quaternions.
 * 6. Reset correctly clears history.
 */
class PoseFilterTest {

    @Test
    fun testModeOff_PassesThroughInstantly() {
        val filter = PoseFilter(SmoothingMode.OFF)
        val rawPose = Pose(floatArrayOf(1.23f, 4.56f, 7.89f), floatArrayOf(0f, 0f, 0f, 1f))

        val filtered = filter.filter(rawPose, 1_000_000_000L)

        assertEquals(rawPose.tx(), filtered.tx(), 1e-6f)
        assertEquals(rawPose.ty(), filtered.ty(), 1e-6f)
        assertEquals(rawPose.tz(), filtered.tz(), 1e-6f)
        assertEquals(rawPose.qx(), filtered.qx(), 1e-6f)
        assertEquals(rawPose.qy(), filtered.qy(), 1e-6f)
        assertEquals(rawPose.qz(), filtered.qz(), 1e-6f)
        assertEquals(rawPose.qw(), filtered.qw(), 1e-6f)
    }

    @Test
    fun testStaticHold_ConvergesToExactAnchorPosition() {
        val filter = PoseFilter(SmoothingMode.LOW)
        val targetPose = Pose(floatArrayOf(0.5f, -0.2f, 1.0f), floatArrayOf(0f, 0f, 0f, 1f))

        var timestamp = 1_000_000_000L // 1.0s
        val dtNanos = 16_666_667L       // ~60 FPS (16.6ms)

        // First frame initializes
        filter.filter(targetPose, timestamp)

        // 30 subsequent frames with phone held static over paper
        var result = targetPose
        for (i in 1..30) {
            timestamp += dtNanos
            result = filter.filter(targetPose, timestamp)
        }

        // Once motion stops, pose sits exactly where paper is
        assertEquals(targetPose.tx(), result.tx(), 1e-4f)
        assertEquals(targetPose.ty(), result.ty(), 1e-4f)
        assertEquals(targetPose.tz(), result.tz(), 1e-4f)
    }

    @Test
    fun testJitterAttenuation_ReducesVariance() {
        val filter = PoseFilter(SmoothingMode.LOW)
        val basePos = floatArrayOf(0f, 0f, 1f)

        var timestamp = 1_000_000_000L
        val dtNanos = 16_666_667L

        // Initialize at base
        filter.filter(Pose(basePos, floatArrayOf(0f, 0f, 0f, 1f)), timestamp)

        // Alternate small jitter of +/- 2mm
        var maxObservedDelta = 0f
        for (i in 1..10) {
            timestamp += dtNanos
            val jitter = if (i % 2 == 0) 0.002f else -0.002f
            val noisyPose = Pose(floatArrayOf(jitter, 0f, 1f), floatArrayOf(0f, 0f, 0f, 1f))
            val filtered = filter.filter(noisyPose, timestamp)
            val delta = abs(filtered.tx() - basePos[0])
            if (delta > maxObservedDelta) {
                maxObservedDelta = delta
            }
        }

        // Filtered jitter must be significantly smaller than raw 2mm spike
        assertTrue(maxObservedDelta < 0.0015f)
    }

    @Test
    fun testFastMotion_AdaptsAlphaQuickly() {
        val filter = PoseFilter(SmoothingMode.LOW)
        var timestamp = 1_000_000_000L
        val dtNanos = 16_666_667L

        filter.filter(Pose(floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f, 1f)), timestamp)

        // Fast movement: phone shifts 0.3m in one frame (speed = 18 m/s!)
        timestamp += dtNanos
        val fastPose = Pose(floatArrayOf(0.3f, 0f, 0f), floatArrayOf(0f, 0f, 0f, 1f))
        val step1 = filter.filter(fastPose, timestamp)

        // Because beta responds to high derivative dx/dt, alpha increases and step1 moves rapidly
        assertTrue(step1.tx() > 0.05f)

        // Next frame
        timestamp += dtNanos
        val step2 = filter.filter(fastPose, timestamp)
        assertTrue(step2.tx() > 0.15f)
    }

    @Test
    fun testQuaternionNormalization_AlwaysUnitLength() {
        val filter = PoseFilter(SmoothingMode.HIGH)
        var timestamp = 1_000_000_000L
        val dtNanos = 16_666_667L

        // Start with identity rotation
        filter.filter(Pose(floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f, 1f)), timestamp)

        // Rotate 45 degrees around Y: q = [0, sin(pi/8), 0, cos(pi/8)]
        val angle = Math.PI / 4.0
        val qy = kotlin.math.sin(angle / 2.0).toFloat()
        val qw = kotlin.math.cos(angle / 2.0).toFloat()
        val rotPose = Pose(floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, qy, 0f, qw))

        timestamp += dtNanos
        val filtered = filter.filter(rotPose, timestamp)

        // Check quaternion magnitude: x^2 + y^2 + z^2 + w^2 must be 1.0
        val magSq = filtered.qx() * filtered.qx() +
                filtered.qy() * filtered.qy() +
                filtered.qz() * filtered.qz() +
                filtered.qw() * filtered.qw()
        val mag = sqrt(magSq)
        assertEquals(1.0f, mag, 1e-4f)
    }

    @Test
    fun testReset_ClearsHistory() {
        val filter = PoseFilter(SmoothingMode.LOW)
        filter.filter(Pose(floatArrayOf(10f, 10f, 10f), floatArrayOf(0f, 0f, 0f, 1f)), 1_000_000_000L)

        filter.reset()

        // After reset, first pose immediately initializes filter output
        val newPose = Pose(floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f, 1f))
        val result = filter.filter(newPose, 2_000_000_000L)

        assertEquals(0f, result.tx(), 1e-4f)
        assertEquals(0f, result.ty(), 1e-4f)
        assertEquals(0f, result.tz(), 1e-4f)
    }
}
