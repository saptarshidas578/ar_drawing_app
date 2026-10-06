package com.tracear.app.ar

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * PaperLockFilter — temporal smoothing, deadband, jump gating, and template refresh logic.
 */
class PaperLockFilter {

    companion object {
        const val DEADBAND_TRANSLATION_METERS = 0.0005f // 0.5 mm
        const val DEADBAND_ROTATION_DEG = 0.10f        // 0.1 degree

        const val JUMP_THRESHOLD_METERS = 0.010f      // 1.0 cm
        const val JUMP_ROTATION_DEG = 3.0f            // 3.0 degrees
        const val JUMP_CONSISTENCY_CYCLES = 3         // 3 consistent cycles required for large jumps

        const val NORMAL_SMOOTHING_ALPHA = 0.35f      // Silky smooth for small drift/nudges
        const val FAST_CATCHUP_ALPHA = 0.85f          // Fast response for deliberate slides

        const val TEMPLATE_STILL_TIME_MS = 3000L      // 3.0 seconds stationary required
        const val TEMPLATE_REFRESH_MIN_CONFIDENCE = 0.80f
    }

    var currentPose: PaperFrame = PaperFrame.IDENTITY
        private set

    private var pendingJumpPose: PaperFrame? = null
    private var pendingJumpCount: Int = 0

    private var lastMovementTimeMs: Long = 0L
    private var isStationarySinceMs: Long = 0L

    /**
     * Resets filter to identity or given baseline.
     */
    fun reset(pose: PaperFrame = PaperFrame.IDENTITY, nowMs: Long = System.currentTimeMillis()) {
        currentPose = pose
        pendingJumpPose = null
        pendingJumpCount = 0
        lastMovementTimeMs = nowMs
        isStationarySinceMs = nowMs
    }

    /**
     * Filters a newly observed candidate pose.
     * @param observed The raw estimated PaperFrame from tracking.
     * @param nowMs Current system timestamp in milliseconds.
     * @return The smoothed, gated PaperFrame.
     */
    fun filter(observed: PaperFrame, nowMs: Long = System.currentTimeMillis()): PaperFrame {
        val deltaX = observed.dx - currentPose.dx
        val deltaZ = observed.dz - currentPose.dz
        val deltaDist = sqrt(deltaX * deltaX + deltaZ * deltaZ)

        var deltaRot = observed.rotationDegrees - currentPose.rotationDegrees
        while (deltaRot > 180f) deltaRot -= 360f
        while (deltaRot < -180f) deltaRot += 360f
        val absDeltaRot = abs(deltaRot)

        // 1. Deadband check: if change is smaller than 0.5mm and 0.1 deg, reject micro-noise
        if (deltaDist < DEADBAND_TRANSLATION_METERS && absDeltaRot < DEADBAND_ROTATION_DEG) {
            // Paper is still
            if (isStationarySinceMs == 0L) {
                isStationarySinceMs = nowMs
            }
            pendingJumpPose = null
            pendingJumpCount = 0
            return currentPose
        }

        // Paper has moved
        isStationarySinceMs = nowMs
        lastMovementTimeMs = nowMs

        // 2. Large jump gating: changes > 1cm or > 3deg need 3 consistent cycles
        val isLargeJump = deltaDist > JUMP_THRESHOLD_METERS || absDeltaRot > JUMP_ROTATION_DEG
        if (isLargeJump) {
            val pending = pendingJumpPose
            if (pending != null) {
                val distToPending = sqrt(
                    (observed.dx - pending.dx) * (observed.dx - pending.dx) +
                            (observed.dz - pending.dz) * (observed.dz - pending.dz)
                )
                if (distToPending < 0.005f) { // Within 5mm of pending jump target
                    pendingJumpCount++
                } else {
                    pendingJumpPose = observed
                    pendingJumpCount = 1
                }
            } else {
                pendingJumpPose = observed
                pendingJumpCount = 1
            }

            if (pendingJumpCount < JUMP_CONSISTENCY_CYCLES) {
                // Hold current pose until jump is confirmed
                return currentPose
            }
            // Jump confirmed! Apply with fast catchup
            pendingJumpPose = null
            pendingJumpCount = 0
        } else {
            pendingJumpPose = null
            pendingJumpCount = 0
        }

        // 3. Dynamic low-pass filter
        val alpha = if (deltaDist > JUMP_THRESHOLD_METERS) FAST_CATCHUP_ALPHA else NORMAL_SMOOTHING_ALPHA

        val smoothedDx = currentPose.dx + alpha * deltaX
        val smoothedDz = currentPose.dz + alpha * deltaZ
        val smoothedRot = currentPose.rotationDegrees + alpha * deltaRot

        currentPose = PaperFrame(
            dx = smoothedDx,
            dz = smoothedDz,
            rotationDegrees = smoothedRot
        )

        return currentPose
    }

    /**
     * Checks if the paper has been still long enough and confidence is high enough to refresh the template.
     */
    fun shouldRefreshTemplate(confidence: Float, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (confidence < TEMPLATE_REFRESH_MIN_CONFIDENCE) return false
        if (isStationarySinceMs == 0L) return false
        val stillDuration = nowMs - isStationarySinceMs
        return stillDuration >= TEMPLATE_STILL_TIME_MS
    }
}
