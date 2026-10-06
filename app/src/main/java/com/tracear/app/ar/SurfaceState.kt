package com.tracear.app.ar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.ar.core.Anchor
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState

enum class ScanQuality {
    LOW,
    MEDIUM,
    GOOD
}

/**
 * SurfaceState — manages candidate surface selection, floor rejection filters,
 * locked table plane status, scan quality, and debug metrics.
 */
class SurfaceState {

    companion object {
        // ── Configurable Floor Rejection Thresholds ──────────
        const val MAX_SURFACE_DISTANCE = 1.20f      // Max distance from camera to surface (meters)
        const val MAX_PLANE_DELTA_Y = 0.15f        // Max vertical deviation between plane center & hit
        const val MAX_NORMAL_ANGLE_DEG = 25.0f     // Surface must be within 25° of horizontal
        const val SCAN_QUALITY_LOW_MAX = 15        // Feature point count thresholds
        const val SCAN_QUALITY_MED_MAX = 40
        const val TIP_TIMEOUT_SECONDS = 8           // Show guidance tip after 8s if no plane
    }

    // ── Candidate Plane State ────────────────────────────────
    var candidatePlane by mutableStateOf<Plane?>(null)
    var candidateHitPose by mutableStateOf<Pose?>(null)
    var candidateDistance by mutableFloatStateOf(0f)
    var isCandidateValid by mutableStateOf(false)

    // ── Locked Plane State ───────────────────────────────────
    var isPlaneLocked by mutableStateOf(false)
    var lockedPlanePose by mutableStateOf<Pose?>(null)
    var lockedPlaneNormal by mutableStateOf(Vector3f.UP)
    var lockedPlanePoint by mutableStateOf(Vector3f.ZERO)
    var lockedSurfaceAnchor by mutableStateOf<Anchor?>(null)

    // ── Scan Quality & Point Cloud ───────────────────────────
    var scanQuality by mutableStateOf(ScanQuality.LOW)
    var featurePointsNearCrosshair by mutableIntStateOf(0)
    var totalPointCloudCount by mutableIntStateOf(0)

    // ── Fallbacks & Guidance ─────────────────────────────────
    var searchDurationSeconds by mutableFloatStateOf(0f)
    var showScanTips by mutableStateOf(false)
    var isInstantPlacementActive by mutableStateOf(false)
    var isDepthModeEnabled by mutableStateOf(false)

    // ── Debug Panel State ────────────────────────────────────
    var isDebugPanelVisible by mutableStateOf(false)
    var trackingState by mutableStateOf(TrackingState.STOPPED)
    var trackingFailureReason by mutableStateOf<TrackingFailureReason?>(null)
    var detectedPlaneCount by mutableIntStateOf(0)
    var lastRayIntersectionResult by mutableStateOf<String>("No tap yet")
    var isLowLight by mutableStateOf(false)

    /**
     * Resets surface selection and unlocks the plane.
     */
    fun reset() {
        lockedSurfaceAnchor?.detach()
        lockedSurfaceAnchor = null
        isPlaneLocked = false
        lockedPlanePose = null
        lockedPlaneNormal = Vector3f.UP
        lockedPlanePoint = Vector3f.ZERO

        candidatePlane = null
        candidateHitPose = null
        candidateDistance = 0f
        isCandidateValid = false

        searchDurationSeconds = 0f
        showScanTips = false
        isInstantPlacementActive = false
        lastRayIntersectionResult = "Reset"
    }

    /**
     * Locks the current candidate plane into the permanent table surface.
     */
    fun lockCandidateSurface(anchor: Anchor) {
        val plane = candidatePlane ?: return
        val pose = candidateHitPose ?: plane.centerPose

        lockedSurfaceAnchor?.detach()
        lockedSurfaceAnchor = anchor

        lockedPlanePose = pose
        lockedPlanePoint = Vector3f(pose.tx(), pose.ty(), pose.tz())

        val q = pose.rotationQuaternion
        lockedPlaneNormal = MathUtils.extractPlaneNormal(q[0], q[1], q[2], q[3])

        isPlaneLocked = true
        isCandidateValid = false
    }

    /**
     * Manually lock onto an approximate plane using Instant Placement.
     */
    fun lockInstantPlacement(anchor: Anchor, approxDistance: Float = 0.5f, cameraPose: Pose) {
        lockedSurfaceAnchor?.detach()
        lockedSurfaceAnchor = anchor

        // Anchor is created along camera forward at approx distance
        val anchorPose = anchor.pose
        lockedPlanePose = anchorPose
        lockedPlanePoint = Vector3f(anchorPose.tx(), anchorPose.ty(), anchorPose.tz())
        lockedPlaneNormal = Vector3f.UP // Local upward facing

        isInstantPlacementActive = true
        isPlaneLocked = true
    }
}
