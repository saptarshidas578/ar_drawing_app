package com.tracear.app.ar

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Standard paper size presets with real-world dimensions in meters.
 */
enum class PaperPreset(
    val displayName: String,
    val widthMeters: Float,
    val heightMeters: Float
) {
    A5("A5", 0.148f, 0.210f),
    A4("A4", 0.210f, 0.297f),
    A3("A3", 0.297f, 0.420f),
    LETTER("Letter", 0.2159f, 0.2794f),
    CUSTOM("Custom", 0.210f, 0.297f);

    fun formatLabel(wMeters: Float = widthMeters, hMeters: Float = heightMeters): String {
        return "%s (%.1f × %.1f cm)".format(displayName, wMeters * 100f, hMeters * 100f)
    }

    companion object {
        fun fromName(name: String?): PaperPreset {
            return entries.find { it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true) }
                ?: A4
        }
    }
}

/**
 * Pure mathematical functions for paper preset snapping, physical unit conversions,
 * and real-world dimension calculations.
 * All math operates without per-frame allocations.
 */
object PaperMath {

    fun metersToCm(meters: Float): Float = meters * 100f

    fun cmToMeters(cm: Float): Float = cm / 100f

    /**
     * Calculates the centroid (geometric center) of 4 corner points.
     */
    fun calculateCentroid(corners: List<Vector3f>): Vector3f {
        if (corners.isEmpty()) return Vector3f(0f, 0f, 0f)
        var sumX = 0f
        var sumY = 0f
        var sumZ = 0f
        for (p in corners) {
            sumX += p.x
            sumY += p.y
            sumZ += p.z
        }
        val count = corners.size.toFloat()
        return Vector3f(sumX / count, sumY / count, sumZ / count)
    }

    /**
     * Calculates the physical paper width and height (in meters) from corners sorted clockwise:
     * 0: TL, 1: TR, 2: BR, 3: BL.
     * Returns Pair(widthMeters, heightMeters).
     */
    fun calculatePaperDimensions(corners: List<Vector3f>): Pair<Float, Float> {
        if (corners.size < 4) return Pair(0.210f, 0.297f)
        val sorted = MathUtils.sortCornersClockwise(corners)
        val topW = (sorted[1] - sorted[0]).length()
        val botW = (sorted[2] - sorted[3]).length()
        val leftH = (sorted[3] - sorted[0]).length()
        val rightH = (sorted[2] - sorted[1]).length()
        val w = (topW + botW) / 2f
        val h = (leftH + rightH) / 2f
        return Pair(max(0.01f, w), max(0.01f, h))
    }

    /**
     * Snaps current 4 corner markers into an exact rectangle with real-world dimensions
     * (widthMeters x heightMeters), preserving the center (centroid) and orientation of what was marked.
     *
     * Returns a list of 4 new corners in clockwise order: TL, TR, BR, BL.
     */
    fun snapCornersToPreset(
        currentCorners: List<Vector3f>,
        presetWidthMeters: Float,
        presetHeightMeters: Float
    ): List<Vector3f> {
        if (currentCorners.size < 4) return currentCorners

        // 1. Sort clockwise: TL, TR, BR, BL
        val sorted = MathUtils.sortCornersClockwise(currentCorners)
        val cTL = sorted[0]
        val cTR = sorted[1]
        val cBR = sorted[2]
        val cBL = sorted[3]

        // 2. Centroid of current quadrilateral
        val centroid = calculateCentroid(sorted)

        // 3. Current marked width & height vectors
        val markedWidthVec = cTR - cTL
        val markedHeightVec = cBL - cTL
        val markedWidthLen = markedWidthVec.length()
        val markedHeightLen = markedHeightVec.length()

        // 4. Primary direction unit vector u along width (TL -> TR)
        val u = if (markedWidthLen > 0.001f) {
            markedWidthVec * (1f / markedWidthLen)
        } else {
            Vector3f(1f, 0f, 0f)
        }

        // 5. In-plane orthogonal direction unit vector v along height (TL -> BL)
        // Project markedHeightVec perpendicular to u: v_raw = markedHeightVec - (markedHeightVec . u) * u
        val dotHU = markedHeightVec.dot(u)
        val vProj = markedHeightVec - (u * dotHU)
        val vLen = vProj.length()
        val v = if (vLen > 0.001f) {
            vProj * (1f / vLen)
        } else {
            // Fallback orthogonal in horizontal plane: cross(UP, u)
            Vector3f(-u.z, 0f, u.x)
        }

        // 6. Match preset orientation to marked orientation:
        // If user marked a landscape rectangle (markedWidthLen >= markedHeightLen),
        // assign the larger preset dimension to width. Otherwise, assign larger dimension to height.
        val dimMin = min(presetWidthMeters, presetHeightMeters)
        val dimMax = max(presetWidthMeters, presetHeightMeters)
        val isMarkedLandscape = markedWidthLen >= markedHeightLen

        val targetW = if (isMarkedLandscape) dimMax else dimMin
        val targetH = if (isMarkedLandscape) dimMin else dimMax

        val halfW = targetW / 2f
        val halfH = targetH / 2f

        // 7. Recompute corners centered at centroid:
        // TL = C - u * halfW - v * halfH
        // TR = C + u * halfW - v * halfH
        // BR = C + u * halfW + v * halfH
        // BL = C - u * halfW + v * halfH
        val uHalfW = u * halfW
        val vHalfH = v * halfH

        val newTL = Vector3f(centroid.x - uHalfW.x - vHalfH.x, centroid.y, centroid.z - uHalfW.z - vHalfH.z)
        val newTR = Vector3f(centroid.x + uHalfW.x - vHalfH.x, centroid.y, centroid.z + uHalfW.z - vHalfH.z)
        val newBR = Vector3f(centroid.x + uHalfW.x + vHalfH.x, centroid.y, centroid.z + uHalfW.z + vHalfH.z)
        val newBL = Vector3f(centroid.x - uHalfW.x + vHalfH.x, centroid.y, centroid.z - uHalfW.z + vHalfH.z)

        return listOf(newTL, newTR, newBR, newBL)
    }

    /**
     * Computes the base physical width (in meters) of the drawn image on paper at scale = 1.0f.
     */
    fun calculateBaseDrawingWidthMeters(
        paperWidthMeters: Float,
        paperHeightMeters: Float,
        imageAspect: Float,
        fitMode: FitMode
    ): Float {
        if (paperWidthMeters <= 0.001f || paperHeightMeters <= 0.001f) return 0.210f
        val paperAspect = paperWidthMeters / paperHeightMeters

        return when (fitMode) {
            FitMode.STRETCH -> paperWidthMeters
            FitMode.FIT -> {
                if (imageAspect > paperAspect) {
                    paperWidthMeters
                } else {
                    paperHeightMeters * imageAspect
                }
            }
            FitMode.FILL -> {
                if (imageAspect > paperAspect) {
                    paperHeightMeters * imageAspect
                } else {
                    paperWidthMeters
                }
            }
        }
    }

    /**
     * Calculates the required NormalizedTransform scale to achieve a specific real-world drawing width (in meters).
     */
    fun calculateScaleForDrawingWidth(
        targetWidthMeters: Float,
        paperWidthMeters: Float,
        paperHeightMeters: Float,
        imageAspect: Float,
        fitMode: FitMode
    ): Float {
        val baseWidth = calculateBaseDrawingWidthMeters(paperWidthMeters, paperHeightMeters, imageAspect, fitMode)
        if (baseWidth <= 0.001f) return 1.0f
        return (targetWidthMeters / baseWidth).coerceIn(0.05f, 10.0f)
    }

    /**
     * Calculates the current real-world drawing width (in centimeters) for display.
     */
    fun getCurrentDrawingWidthCm(
        paperWidthMeters: Float,
        paperHeightMeters: Float,
        imageAspect: Float,
        fitMode: FitMode,
        scale: Float
    ): Float {
        val baseWidth = calculateBaseDrawingWidthMeters(paperWidthMeters, paperHeightMeters, imageAspect, fitMode)
        return (baseWidth * scale) * 100f
    }
}
