package com.tracear.app.ar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.ar.core.Anchor

/**
 * CalibrationState — manages the single surface anchor and the 4 paper corner positions
 * expressed in that anchor's local coordinate space.
 *
 * All 4 corners reside on the locked table plane, preventing any plane mismatch or floor penetration.
 */
class CalibrationState {

    // Single Anchor on the locked table plane
    var surfaceAnchor by mutableStateOf<Anchor?>(null)

    // Corner positions in local coordinates relative to the surfaceAnchor
    val localCorners = mutableStateListOf<Vector3f>()

    // How many corners have been placed (0..4)
    var cornerCount by mutableIntStateOf(0)
        private set

    // True when all 4 corners are placed and anchored
    var isCalibrated by mutableStateOf(false)
        private set

    /**
     * Set the single surface anchor for calibration.
     */
    fun bindSurfaceAnchor(anchor: Anchor) {
        surfaceAnchor?.detach()
        surfaceAnchor = anchor
        checkCalibrated()
    }

    /**
     * Add a corner position in local coordinates of the surface anchor.
     */
    fun addCorner(localPoint: Vector3f) {
        if (cornerCount >= 4) return
        localCorners.add(localPoint)
        cornerCount = localCorners.size
        checkCalibrated()
    }

    /**
     * Set all 4 corners at once (used by OpenCV automatic paper detection).
     */
    fun setAllCorners(corners: List<Vector3f>) {
        localCorners.clear()
        localCorners.addAll(corners.take(4))
        cornerCount = localCorners.size
        checkCalibrated()
    }

    /**
     * Updates an individual corner's local position (for fine-tuning handles).
     */
    fun updateCorner(index: Int, newLocalPos: Vector3f) {
        if (index in 0 until localCorners.size) {
            localCorners[index] = newLocalPos
        }
    }

    /**
     * Remove the last placed corner (undo).
     */
    fun undoLastCorner() {
        if (localCorners.isEmpty()) return
        localCorners.removeAt(localCorners.lastIndex)
        cornerCount = localCorners.size
        checkCalibrated()
    }

    /**
     * Clear all corners and reset calibration.
     */
    fun reset() {
        surfaceAnchor?.detach()
        surfaceAnchor = null
        localCorners.clear()
        cornerCount = 0
        isCalibrated = false
    }

    private fun checkCalibrated() {
        isCalibrated = (cornerCount >= 4 && surfaceAnchor != null)
    }
}
