package com.tracear.app.ar

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class RigidResult(
    val paperFrame: PaperFrame,
    val residualMeters: Float,
    val inlierRatio: Float = 1.0f
)

/**
 * RigidTransform2D — Closed-form 2D rigid isometry estimation (translation + rotation)
 * on the horizontal X-Z plane with zero perspective distortion and zero scaling.
 */
object RigidTransform2D {

    const val MAX_EDGE_SIDE_ERROR = 0.05f // 5% maximum deviation in paper side lengths

    /**
     * Checks if the 4 observed corners match the baseline corners in side lengths within tolerance.
     */
    fun validateCornerLengths(
        baselineCorners: List<Vector3f>,
        observedCorners: List<Vector3f>,
        maxSideErrorRatio: Float = MAX_EDGE_SIDE_ERROR
    ): Boolean {
        if (baselineCorners.size != 4 || observedCorners.size != 4) return false

        for (i in 0 until 4) {
            val nextIdx = (i + 1) % 4
            val baseLen = (baselineCorners[nextIdx] - baselineCorners[i]).length()
            val obsLen = (observedCorners[nextIdx] - observedCorners[i]).length()

            if (baseLen < 0.01f || obsLen < 0.01f) return false
            val error = abs(obsLen - baseLen) / baseLen
            if (error > maxSideErrorRatio) {
                return false
            }
        }
        return true
    }

    /**
     * Estimates the optimal rigid transform (PaperFrame) from baseline corners to observed corners.
     * Uses closed-form 2D Procrustes / Kabsch formulation.
     */
    fun estimateFromCorrespondingPoints(
        baselinePoints: List<Vector3f>,
        observedPoints: List<Vector3f>
    ): RigidResult? {
        if (baselinePoints.size < 2 || baselinePoints.size != observedPoints.size) return null

        val n = baselinePoints.size
        val baseCentroid = PaperFrame.calculateCentroid(baselinePoints)
        val obsCentroid = PaperFrame.calculateCentroid(observedPoints)

        var sxx = 0.0
        var sxz = 0.0
        var szx = 0.0
        var szz = 0.0

        for (i in 0 until n) {
            val bp = baselinePoints[i]
            val op = observedPoints[i]

            val bx = (bp.x - baseCentroid.x).toDouble()
            val bz = (bp.z - baseCentroid.z).toDouble()

            val ox = (op.x - obsCentroid.x).toDouble()
            val oz = (op.z - obsCentroid.z).toDouble()

            sxx += bx * ox
            sxz += bx * oz
            szx += bz * ox
            szz += bz * oz
        }

        // Optimal rotation angle in X-Z plane:
        // theta = atan2(sxz - szx, sxx + szz)
        val thetaRad = atan2(sxz - szx, sxx + szz)
        val thetaDeg = Math.toDegrees(thetaRad).toFloat()

        // Translation of the centroid:
        // dx = obsCentroid.x - baseCentroid.x
        // dz = obsCentroid.z - baseCentroid.z
        val frame = PaperFrame(
            dx = obsCentroid.x - baseCentroid.x,
            dz = obsCentroid.z - baseCentroid.z,
            rotationDegrees = thetaDeg
        )

        // Compute residual RMS error
        var totalSqError = 0.0
        for (i in 0 until n) {
            val pred = frame.transform(baselinePoints[i], baseCentroid)
            val obs = observedPoints[i]
            val errX = (pred.x - obs.x).toDouble()
            val errZ = (pred.z - obs.z).toDouble()
            totalSqError += (errX * errX + errZ * errZ)
        }
        val rms = sqrt(totalSqError / n).toFloat()

        return RigidResult(paperFrame = frame, residualMeters = rms, inlierRatio = 1.0f)
    }

    /**
     * Robust RANSAC-based 2D rigid estimation from 2D point correspondences (for template feature matches).
     */
    fun estimateWithRansac(
        baselinePoints: List<Vector3f>,
        observedPoints: List<Vector3f>,
        maxInlierDistMeters: Float = 0.005f, // 5 mm inlier tolerance
        minInlierRatio: Float = 0.35f,
        maxIterations: Int = 50
    ): RigidResult? {
        val count = baselinePoints.size
        if (count < 4 || count != observedPoints.size) return null

        val baseCentroid = PaperFrame.calculateCentroid(baselinePoints)
        var bestInliers = emptyList<Int>()
        var bestFrame: PaperFrame? = null

        val random = java.util.Random(42)

        for (iter in 0 until maxIterations) {
            // Pick 2 distinct random points
            val idx1 = random.nextInt(count)
            var idx2 = random.nextInt(count)
            while (idx2 == idx1) {
                idx2 = random.nextInt(count)
            }

            val p1 = baselinePoints[idx1]
            val p2 = baselinePoints[idx2]
            val q1 = observedPoints[idx1]
            val q2 = observedPoints[idx2]

            val baseDist = (p2 - p1).length()
            val obsDist = (q2 - q1).length()
            if (baseDist < 0.01f || abs(obsDist - baseDist) / baseDist > 0.1f) {
                continue // Inconsistent distance pair
            }

            // Estimate rigid transform from the 2-point pair
            val candidateResult = estimateFromCorrespondingPoints(listOf(p1, p2), listOf(q1, q2)) ?: continue
            val candFrame = candidateResult.paperFrame

            // Count inliers
            val inliers = mutableListOf<Int>()
            for (i in 0 until count) {
                val pred = candFrame.transform(baselinePoints[i], baseCentroid)
                val obs = observedPoints[i]
                val dist = (pred - obs).length()
                if (dist <= maxInlierDistMeters) {
                    inliers.add(i)
                }
            }

            if (inliers.size > bestInliers.size) {
                bestInliers = inliers
                bestFrame = candFrame
                if (inliers.size.toFloat() / count > 0.85f) break // Early exit if high consensus
            }
        }

        val inlierRatio = bestInliers.size.toFloat() / count
        if (inlierRatio < minInlierRatio || bestInliers.size < 4) {
            return null // Not enough confident inliers
        }

        // Refine with all inliers
        val inlierBase = bestInliers.map { baselinePoints[it] }
        val inlierObs = bestInliers.map { observedPoints[it] }
        val refined = estimateFromCorrespondingPoints(inlierBase, inlierObs) ?: return null

        return refined.copy(inlierRatio = inlierRatio)
    }
}
