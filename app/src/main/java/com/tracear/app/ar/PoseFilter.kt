package com.tracear.app.ar

import com.google.ar.core.Pose
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Smoothing intensity options for the overlay pose.
 */
enum class SmoothingMode(val label: String) {
    OFF("Off"),
    LOW("Low"),
    HIGH("High")
}

/**
 * Low-pass 1€ (One Euro) filter for jitter reduction with adaptive speed response.
 *
 * Designed specifically for AR tracing overlays:
 * - When the phone is still: aggressively eliminates high-frequency VIO jitter.
 * - When the phone moves quickly: increases cutoff frequency to prevent any lag.
 * - When motion stops: filtered pose converges exactly to the raw anchor pose.
 *
 * Pre-allocated memory: ZERO runtime heap allocations inside [filter].
 */
class PoseFilter(var mode: SmoothingMode = SmoothingMode.LOW) {

    // ── Tuning Parameters ────────────────────────────────────────

    // Low: imperceptible lag, steady overlay
    private val lowMinCutoff = 1.2f      // Hz
    private val lowBeta = 1.5f           // Speed coefficient for zero motion lag
    private val dCutoff = 1.5f           // Derivative cutoff frequency

    // High: rock-solid overlay for fine tracing
    private val highMinCutoff = 0.4f     // Hz
    private val highBeta = 0.8f          // Speed coefficient for zero motion lag

    // ── Internal State ───────────────────────────────────────────
    private var isInitialized = false
    private var lastTimestampNs: Long = 0L

    // Position filter states (x, y, z)
    private val prevRawPos = FloatArray(3)
    private val prevFilteredPos = FloatArray(3)
    private val prevDerivPos = FloatArray(3)

    // Rotation filter states (qx, qy, qz, qw)
    private val prevRawRot = FloatArray(4)
    private val prevFilteredRot = FloatArray(4)

    // Pre-allocated output buffer (x, y, z, qx, qy, qz, qw)
    private val outPos = FloatArray(3)
    private val outRot = FloatArray(4)

    /**
     * Resets filter state (e.g. on new anchor or re-calibration).
     */
    fun reset() {
        isInitialized = false
        lastTimestampNs = 0L
    }

    /**
     * Filters the raw ARCore [rawPose].
     *
     * @param rawPose The un-smoothed ARCore anchor pose.
     * @param timestampNs Timestamp in nanoseconds (e.g., from frame.timestamp).
     * @return Smoothed [Pose].
     */
    fun filter(rawPose: Pose, timestampNs: Long): Pose {
        if (mode == SmoothingMode.OFF) {
            reset()
            return rawPose
        }

        if (!isInitialized || lastTimestampNs <= 0L) {
            // First sample: initialize state
            rawPose.getTranslation(prevRawPos, 0)
            System.arraycopy(prevRawPos, 0, prevFilteredPos, 0, 3)
            prevDerivPos[0] = 0f
            prevDerivPos[1] = 0f
            prevDerivPos[2] = 0f

            rawPose.getRotationQuaternion(prevRawRot, 0)
            System.arraycopy(prevRawRot, 0, prevFilteredRot, 0, 4)

            lastTimestampNs = timestampNs
            isInitialized = true
            return rawPose
        }

        val dt = (timestampNs - lastTimestampNs) * 1e-9f
        lastTimestampNs = timestampNs

        // If dt is invalid or frame was paused for too long (> 200ms), snap to raw
        if (dt <= 0.0001f || dt > 0.2f) {
            rawPose.getTranslation(prevFilteredPos, 0)
            rawPose.getRotationQuaternion(prevFilteredRot, 0)
            return rawPose
        }

        val minCutoff = if (mode == SmoothingMode.HIGH) highMinCutoff else lowMinCutoff
        val beta = if (mode == SmoothingMode.HIGH) highBeta else lowBeta

        // ── 1. Filter Position (X, Y, Z) ─────────────────────────
        rawPose.getTranslation(prevRawPos, 0)

        for (i in 0 until 3) {
            val raw = prevRawPos[i]
            val prevF = prevFilteredPos[i]
            val prevD = prevDerivPos[i]

            // Derivative estimation
            val rawD = (raw - prevF) / dt
            val alphaD = computeAlpha(dCutoff, dt)
            val filteredD = alphaD * rawD + (1f - alphaD) * prevD
            prevDerivPos[i] = filteredD

            // Dynamic cutoff based on speed
            val cutoff = minCutoff + beta * abs(filteredD)
            val alpha = computeAlpha(cutoff, dt)

            val filtered = alpha * raw + (1f - alpha) * prevF
            prevFilteredPos[i] = filtered
            outPos[i] = filtered
        }

        // ── 2. Filter Rotation (Quaternion SLERP) ─────────────────
        rawPose.getRotationQuaternion(prevRawRot, 0)
        var qx = prevRawRot[0]
        var qy = prevRawRot[1]
        var qz = prevRawRot[2]
        var qw = prevRawRot[3]

        // Ensure shortest path in quaternion space
        var dot = prevFilteredRot[0] * qx +
                  prevFilteredRot[1] * qy +
                  prevFilteredRot[2] * qz +
                  prevFilteredRot[3] * qw

        if (dot < 0f) {
            qx = -qx; qy = -qy; qz = -qz; qw = -qw
            dot = -dot
        }
        val clampedDot = dot.coerceIn(-1f, 1f)

        // Angular distance
        val angle = 2f * acos(clampedDot)
        val angularSpeed = angle / dt

        val rotCutoff = minCutoff + (beta * 2f) * angularSpeed
        val rotAlpha = computeAlpha(rotCutoff, dt).coerceIn(0.01f, 1f)

        slerp(prevFilteredRot, qx, qy, qz, qw, clampedDot, rotAlpha, outRot)
        System.arraycopy(outRot, 0, prevFilteredRot, 0, 4)

        return Pose(outPos, outRot)
    }

    private fun computeAlpha(cutoffHz: Float, dt: Float): Float {
        val tau = 1f / (2f * PI.toFloat() * cutoffHz)
        return (1f / (1f + tau / dt)).coerceIn(0f, 1f)
    }

    private fun slerp(
        from: FloatArray,
        toX: Float, toY: Float, toZ: Float, toW: Float,
        dot: Float,
        t: Float,
        result: FloatArray
    ) {
        if (dot > 0.9995f) {
            // Linear interpolation for very close angles (avoids div by zero)
            val rx = from[0] + t * (toX - from[0])
            val ry = from[1] + t * (toY - from[1])
            val rz = from[2] + t * (toZ - from[2])
            val rw = from[3] + t * (toW - from[3])
            val len = sqrt(rx * rx + ry * ry + rz * rz + rw * rw).coerceAtLeast(1e-6f)
            result[0] = rx / len
            result[1] = ry / len
            result[2] = rz / len
            result[3] = rw / len
            return
        }

        val theta = acos(dot)
        val sinTheta = sin(theta)
        val w1 = sin((1f - t) * theta) / sinTheta
        val w2 = sin(t * theta) / sinTheta

        result[0] = w1 * from[0] + w2 * toX
        result[1] = w1 * from[1] + w2 * toY
        result[2] = w1 * from[2] + w2 * toZ
        result[3] = w1 * from[3] + w2 * toW
    }
}
