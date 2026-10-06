package com.tracear.app.ar

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * PaperFrame — 2D rigid pose of the paper on the locked table plane.
 *
 * Coordinates are in anchor-local space (horizontal X-Z plane, +Y normal):
 * - dx: translation along X axis in meters
 * - dz: translation along Z axis in meters
 * - rotationDegrees: rotation angle in degrees around the Y axis (+Y up)
 */
data class PaperFrame(
    val dx: Float = 0f,
    val dz: Float = 0f,
    val rotationDegrees: Float = 0f
) {
    /**
     * Transforms a 3D point on the table plane around the paper centroid.
     */
    fun transform(point: Vector3f, centroid: Vector3f): Vector3f {
        if (isIdentity()) return point
        val rad = Math.toRadians(rotationDegrees.toDouble()).toFloat()
        val cosT = cos(rad)
        val sinT = sin(rad)

        val relX = point.x - centroid.x
        val relZ = point.z - centroid.z

        val rotX = cosT * relX - sinT * relZ
        val rotZ = sinT * relX + cosT * relZ

        return Vector3f(
            x = centroid.x + rotX + dx,
            y = point.y,
            z = centroid.z + rotZ + dz
        )
    }

    /**
     * Inverse transforms a point back to baseline coordinates.
     */
    fun inverseTransform(point: Vector3f, centroid: Vector3f): Vector3f {
        if (isIdentity()) return point
        val shiftedX = point.x - centroid.x - dx
        val shiftedZ = point.z - centroid.z - dz

        val rad = -Math.toRadians(rotationDegrees.toDouble()).toFloat()
        val cosT = cos(rad)
        val sinT = sin(rad)

        val origX = cosT * shiftedX - sinT * shiftedZ
        val origZ = sinT * shiftedX + cosT * shiftedZ

        return Vector3f(
            x = centroid.x + origX,
            y = point.y,
            z = centroid.z + origZ
        )
    }

    fun isIdentity(toleranceMeters: Float = 1e-5f, toleranceDeg: Float = 1e-4f): Boolean {
        return abs(dx) < toleranceMeters && abs(dz) < toleranceMeters && abs(rotationDegrees) < toleranceDeg
    }

    companion object {
        val IDENTITY = PaperFrame(0f, 0f, 0f)

        /**
         * Computes the centroid of a list of points on the X-Z plane.
         */
        fun calculateCentroid(points: List<Vector3f>): Vector3f {
            if (points.isEmpty()) return Vector3f.ZERO
            val avgX = points.map { it.x }.average().toFloat()
            val avgY = points.map { it.y }.average().toFloat()
            val avgZ = points.map { it.z }.average().toFloat()
            return Vector3f(avgX, avgY, avgZ)
        }
    }
}
