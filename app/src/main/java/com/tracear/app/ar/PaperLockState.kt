package com.tracear.app.ar

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class PaperBannerType {
    NONE,
    PAPER_MOVED, // "Paper moved? Tap to re-align."
    PAPER_FOUND  // "Paper found. Re-align here?"
}

/**
 * PaperLockState — Reactive Compose state holder for Paper Lock settings,
 * live tracking status, paper frame pose, and fallbacks.
 */
class PaperLockState(context: Context? = null) {

    private val prefs = context?.getSharedPreferences("tracear_prefs", Context.MODE_PRIVATE)

    // Master Paper Lock toggle (persisted, default ON)
    private var _isEnabled by mutableStateOf(prefs?.getBoolean("paper_lock_enabled", true) ?: true)
    var isEnabled: Boolean
        get() = _isEnabled
        set(value) {
            _isEnabled = value
            prefs?.edit()?.putBoolean("paper_lock_enabled", value)?.apply()
            if (!value) {
                trackingStatus = PaperTrackingStatus.OFF
                currentPose = PaperFrame.IDENTITY
                bannerType = PaperBannerType.NONE
            } else {
                trackingStatus = PaperTrackingStatus.SEARCHING
            }
        }

    // Freeze paper toggle (temporary, does not persist across restarts)
    var isFrozen by mutableStateOf(false)

    // Sensitivity setting (LOW, NORMAL, HIGH, persisted)
    var sensitivity by mutableStateOf(
        PaperSensitivity.entries.find { it.name == prefs?.getString("paper_lock_sensitivity", "NORMAL") }
            ?: PaperSensitivity.NORMAL
    )

    // Tracking status chip: FOLLOWING, SEARCHING, PAUSED, OFF
    var trackingStatus by mutableStateOf(if (_isEnabled) PaperTrackingStatus.SEARCHING else PaperTrackingStatus.OFF)

    // Active smoothed paper frame pose (dx, dz, rotationDegrees)
    var currentPose by mutableStateOf(PaperFrame.IDENTITY)

    // Tracking metrics for UI & Debug panel
    var confidence by mutableFloatStateOf(0f)
    var residualMm by mutableFloatStateOf(0f)
    var cycleDurationMs by mutableLongStateOf(0L)
    var updateRateHz by mutableFloatStateOf(0f)

    // Debug quads in anchor-local plane coordinates
    var detectedQuadOnPlane by mutableStateOf<List<Vector3f>?>(null)
    var predictedQuadOnPlane by mutableStateOf<List<Vector3f>?>(null)

    // Non-blocking banner alerts
    var bannerType by mutableStateOf(PaperBannerType.NONE)

    // Timestamps for banner logic
    var lostSinceTimeMs: Long = 0L

    fun toggleEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun updateSensitivity(newSens: PaperSensitivity) {
        sensitivity = newSens
        prefs?.edit()?.putString("paper_lock_sensitivity", newSens.name)?.apply()
    }

    fun updateFromResult(result: TrackerResult) {
        if (!isEnabled) {
            trackingStatus = PaperTrackingStatus.OFF
            return
        }
        trackingStatus = result.status
        currentPose = result.paperFrame
        confidence = result.confidence
        residualMm = result.residualMm
        cycleDurationMs = result.cycleDurationMs
        updateRateHz = if (result.cycleDurationMs > 0) 1000f / result.cycleDurationMs else 0f
        detectedQuadOnPlane = result.detectedQuad
        predictedQuadOnPlane = result.predictedQuad

        val now = System.currentTimeMillis()
        if (result.status == PaperTrackingStatus.SEARCHING) {
            if (lostSinceTimeMs == 0L) {
                lostSinceTimeMs = now
            } else if (now - lostSinceTimeMs > 3000L) {
                if (result.detectedQuad != null) {
                    bannerType = PaperBannerType.PAPER_FOUND
                } else {
                    bannerType = PaperBannerType.PAPER_MOVED
                }
            }
        } else if (result.status == PaperTrackingStatus.FOLLOWING) {
            lostSinceTimeMs = 0L
            bannerType = PaperBannerType.NONE
        }
    }

    /**
     * Computes the effective corners on the plane to use for quad rendering,
     * section grid, ruler, and corner handles.
     */
    fun computeEffectiveCorners(baselineCorners: List<Vector3f>): List<Vector3f> {
        if (!isEnabled || currentPose.isIdentity() || baselineCorners.size < 4) {
            return baselineCorners
        }
        val centroid = PaperFrame.calculateCentroid(baselineCorners)
        return baselineCorners.map { currentPose.transform(it, centroid) }
    }

    /**
     * Resets the paper frame to identity (e.g. on full re-calibration or manual re-align).
     */
    fun resetToIdentity() {
        currentPose = PaperFrame.IDENTITY
        detectedQuadOnPlane = null
        predictedQuadOnPlane = null
        bannerType = PaperBannerType.NONE
        lostSinceTimeMs = 0L
    }
}
