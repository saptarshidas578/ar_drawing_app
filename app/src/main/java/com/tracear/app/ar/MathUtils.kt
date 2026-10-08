package com.tracear.app.ar

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sqrt

// ── 3D Vector & Ray ──────────────────────────────────────────

data class Vector3f(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vector3f) = Vector3f(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vector3f) = Vector3f(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vector3f(x * s, y * s, z * s)

    fun dot(o: Vector3f): Float = x * o.x + y * o.y + z * o.z

    fun cross(o: Vector3f) = Vector3f(
        y * o.z - z * o.y,
        z * o.x - x * o.z,
        x * o.y - y * o.x
    )

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalized(): Vector3f {
        val l = length()
        return if (l > 1e-6f) Vector3f(x / l, y / l, z / l) else Vector3f(0f, 0f, 0f)
    }

    companion object {
        val ZERO = Vector3f(0f, 0f, 0f)
        val UP = Vector3f(0f, 1f, 0f)
    }
}

data class Ray(val origin: Vector3f, val direction: Vector3f)

// ── Ray-Plane Intersection Result ───────────────────────────

sealed class RayPlaneResult {
    data class Hit(val point: Vector3f, val distance: Float) : RayPlaneResult()
    object Parallel : RayPlaneResult()
    object BehindCamera : RayPlaneResult()
    data class TooFar(val distance: Float) : RayPlaneResult()
}

// ── Pure Mathematical Functions ─────────────────────────────

object MathUtils {

    /**
     * Computes the analytical intersection of a 3D ray with an infinite mathematical plane.
     *
     * @param ray The optical camera ray in [World Space (meters)] or [Anchor-Local Space (meters)].
     * @param planePoint A known reference point lying on the target plane in the same coordinate space.
     * @param planeNormal The unit normal vector perpendicular to the plane surface.
     * @param maxDistance Maximum plausible ray length in meters (default 2.0m) to reject extreme background intersections.
     * @return [RayPlaneResult.Hit] with exact 3D coordinates and distance `t`, or failure edge cases:
     *         [RayPlaneResult.Parallel] if ray is parallel to plane (|denom| < 1e-4),
     *         [RayPlaneResult.BehindCamera] if intersection is behind camera (t < 0),
     *         or [RayPlaneResult.TooFar] if distance exceeds [maxDistance].
     * @gotcha Ensure [planeNormal] and [ray.direction] are normalized vectors.
     */
    fun rayPlaneIntersection(
        ray: Ray,
        planePoint: Vector3f,
        planeNormal: Vector3f,
        maxDistance: Float = 2.0f
    ): RayPlaneResult {
        val normal = planeNormal.normalized()
        val dir = ray.direction.normalized()

        // Denominator = ray.direction • normal
        val denom = dir.dot(normal)

        // If denominator is close to zero, ray is nearly parallel to the plane
        if (kotlin.math.abs(denom) < 1e-4f) {
            return RayPlaneResult.Parallel
        }

        // t = ((planePoint - rayOrigin) • normal) / denom
        val diff = planePoint - ray.origin
        val t = diff.dot(normal) / denom

        if (t < 0f) {
            return RayPlaneResult.BehindCamera
        }

        if (t > maxDistance) {
            return RayPlaneResult.TooFar(t)
        }

        val hitPoint = ray.origin + dir * t
        return RayPlaneResult.Hit(hitPoint, t)
    }

    /**
     * Extracts the world-space upward normal (+Y in ARCore local plane coordinates)
     * from a plane's rotation quaternion [qx, qy, qz, qw].
     *
     * @return Normalized 3D normal vector in [World Space (meters)].
     */
    fun extractPlaneNormal(qx: Float, qy: Float, qz: Float, qw: Float): Vector3f {
        // Rotating local (0, 1, 0) by quaternion [qx, qy, qz, qw]:
        val nx = 2f * (qx * qy - qw * qz)
        val ny = 1f - 2f * (qx * qx + qz * qz)
        val nz = 2f * (qy * qz + qw * qx)
        return Vector3f(nx, ny, nz).normalized()
    }

    /**
     * Checks if a detected plane normal vector is roughly vertical (pointing upward towards +Y).
     * Used for floor and wall rejection during surface calibration.
     *
     * @param normal Candidate surface normal vector in [World Space].
     * @param maxAngleDegrees Maximum permitted tilt angle in degrees away from pure vertical (default 25°).
     * @return True if the surface is a horizontal tabletop; false if slanted or vertical wall.
     */
    fun isRoughlyVertical(normal: Vector3f, maxAngleDegrees: Float = 25f): Boolean {
        val n = normal.normalized()
        val dot = n.dot(Vector3f.UP) // Cosine of angle with world UP (0, 1, 0)
        val threshold = cos(Math.toRadians(maxAngleDegrees.toDouble())).toFloat()
        return dot >= threshold
    }

    /**
     * Sorts 4 planar points clockwise around their geometric centroid:
     * Order: Top-Left, Top-Right, Bottom-Right, Bottom-Left.
     *
     * @param pts List of 4 points in [Anchor-Local Space (meters)] or [World Space (meters)].
     * @return Clockwise sorted list of 4 points, or original list if size is not 4.
     * @gotcha Points must lie roughly in a single plane (e.g. tabletop X-Z plane).
     */
    fun sortCornersClockwise(pts: List<Vector3f>): List<Vector3f> {
        if (pts.size != 4) return pts
        val cx = pts.map { it.x }.average().toFloat()
        val cz = pts.map { it.z }.average().toFloat()

        // Sort ascending by angle around centroid in X-Z plane
        // TL (-135°), TR (-45°), BR (+45°), BL (+135°) -> directly CW
        return pts.sortedBy { p ->
            atan2((p.z - cz).toDouble(), (p.x - cx).toDouble())
        }
    }

    /**
     * Unprojects a 2D screen touch coordinate into a 3D ray in [World Space (meters)]
     * using the camera's inverted View-Projection matrix.
     *
     * @param touchX Screen touch coordinate X in pixels [0..viewportWidth].
     * @param touchY Screen touch coordinate Y in pixels [0..viewportHeight].
     * @param viewportWidth Surface viewport width in pixels.
     * @param viewportHeight Surface viewport height in pixels.
     * @param invViewProjMatrix 16-element column-major inverted (View * Projection) matrix.
     * @return [Ray] with near-plane origin and unit direction vector in [World Space (meters)].
     */
    fun unprojectScreenPointToRay(
        touchX: Float,
        touchY: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        invViewProjMatrix: FloatArray
    ): Ray {
        // Normalized Device Coordinates (-1 to 1)
        val ndcX = (2f * touchX / viewportWidth) - 1f
        val ndcY = 1f - (2f * touchY / viewportHeight) // Inverted Y for OpenGL / ARCore

        val nearVec = FloatArray(4)
        val farVec = FloatArray(4)

        multiplyMatrixAndVector(invViewProjMatrix, floatArrayOf(ndcX, ndcY, -1f, 1f), nearVec)
        multiplyMatrixAndVector(invViewProjMatrix, floatArrayOf(ndcX, ndcY, 1f, 1f), farVec)

        val nearW = if (nearVec[3] != 0f) nearVec[3] else 1f
        val farW = if (farVec[3] != 0f) farVec[3] else 1f

        val nearPoint = Vector3f(nearVec[0] / nearW, nearVec[1] / nearW, nearVec[2] / nearW)
        val farPoint = Vector3f(farVec[0] / farW, farVec[1] / farW, farVec[2] / farW)

        val dir = (farPoint - nearPoint).normalized()
        return Ray(nearPoint, dir)
    }

    /**
     * Multiplies a 4x4 matrix (in column-major order) by a 4D vector.
     */
    fun multiplyMatrixAndVector(matrix: FloatArray, vec: FloatArray, result: FloatArray) {
        for (row in 0..3) {
            result[row] = matrix[row + 0] * vec[0] +
                    matrix[row + 4] * vec[1] +
                    matrix[row + 8] * vec[2] +
                    matrix[row + 12] * vec[3]
        }
    }

    /**
     * Pure Kotlin 4x4 matrix inversion (column-major order).
     * Works identically on Android and JVM unit tests without dependencies.
     */
    fun invertMatrix4(m: FloatArray, inv: FloatArray): Boolean {
        val a00 = m[0]; val a01 = m[1]; val a02 = m[2]; val a03 = m[3]
        val a10 = m[4]; val a11 = m[5]; val a12 = m[6]; val a13 = m[7]
        val a20 = m[8]; val a21 = m[9]; val a22 = m[10]; val a23 = m[11]
        val a30 = m[12]; val a31 = m[13]; val a32 = m[14]; val a33 = m[15]

        val b00 = a00 * a11 - a01 * a10
        val b01 = a00 * a12 - a02 * a10
        val b02 = a00 * a13 - a03 * a10
        val b03 = a01 * a12 - a02 * a11
        val b04 = a01 * a13 - a03 * a11
        val b05 = a02 * a13 - a03 * a12
        val b06 = a20 * a31 - a21 * a30
        val b07 = a20 * a32 - a22 * a30
        val b08 = a20 * a33 - a23 * a30
        val b09 = a21 * a32 - a22 * a31
        val b10 = a21 * a33 - a23 * a31
        val b11 = a22 * a33 - a23 * a32

        val det = b00 * b11 - b01 * b10 + b02 * b09 + b03 * b08 - b04 * b07 + b05 * b06
        if (kotlin.math.abs(det) < 1e-8f) return false
        val invDet = 1.0f / det

        inv[0] = (a11 * b11 - a12 * b10 + a13 * b09) * invDet
        inv[1] = (-a01 * b11 + a02 * b10 - a03 * b09) * invDet
        inv[2] = (a31 * b05 - a32 * b04 + a33 * b03) * invDet
        inv[3] = (-a21 * b05 + a22 * b04 - a23 * b03) * invDet
        inv[4] = (-a10 * b11 + a12 * b08 - a13 * b07) * invDet
        inv[5] = (a00 * b11 - a02 * b08 + a03 * b07) * invDet
        inv[6] = (-a30 * b05 + a32 * b02 - a33 * b01) * invDet
        inv[7] = (a20 * b05 - a22 * b02 + a23 * b01) * invDet
        inv[8] = (a10 * b10 - a11 * b08 + a13 * b06) * invDet
        inv[9] = (-a00 * b10 + a01 * b08 - a03 * b06) * invDet
        inv[10] = (a30 * b04 - a31 * b02 + a33 * b00) * invDet
        inv[11] = (-a20 * b04 + a21 * b02 - a23 * b00) * invDet
        inv[12] = (-a10 * b09 + a11 * b07 - a12 * b06) * invDet
        inv[13] = (a00 * b09 - a01 * b07 + a02 * b06) * invDet
        inv[14] = (-a30 * b03 + a31 * b01 - a32 * b00) * invDet
        inv[15] = (a20 * b03 - a21 * b01 + a22 * b00) * invDet
        return true
    }

    /**
     * Rotates a 3D vector by a quaternion [qx, qy, qz, qw].
     */
    fun rotateVectorByQuaternion(v: Vector3f, qx: Float, qy: Float, qz: Float, qw: Float): Vector3f {
        val tx = 2f * (qy * v.z - qz * v.y)
        val ty = 2f * (qz * v.x - qx * v.z)
        val tz = 2f * (qx * v.y - qy * v.x)
        return Vector3f(
            x = v.x + qw * tx + (qy * tz - qz * ty),
            y = v.y + qw * ty + (qz * tx - qx * tz),
            z = v.z + qw * tz + (qx * ty - qy * tx)
        )
    }

    /**
     * Converts a 2D CPU camera image pixel (u, v) into a 3D ray in world space
     * using camera intrinsics and the camera world pose.
     */
    fun unprojectCameraPixelToRay(
        u: Float,
        v: Float,
        fx: Float,
        fy: Float,
        cx: Float,
        cy: Float,
        camTx: Float,
        camTy: Float,
        camTz: Float,
        camQx: Float,
        camQy: Float,
        camQz: Float,
        camQw: Float
    ): Ray {
        val camDir = Vector3f(
            x = (u - cx) / fx,
            y = (v - cy) / fy,
            z = 1.0f
        ).normalized()

        val worldDir = rotateVectorByQuaternion(camDir, camQx, camQy, camQz, camQw).normalized()
        val worldOrigin = Vector3f(camTx, camTy, camTz)
        return Ray(worldOrigin, worldDir)
    }

    /**
     * Projects a 3D world coordinate into a 2D CPU camera image pixel (u, v).
     * Returns null if the point is behind or on the camera plane.
     */
    fun projectWorldPointToCameraPixel(
        worldPoint: Vector3f,
        fx: Float,
        fy: Float,
        cx: Float,
        cy: Float,
        camTx: Float,
        camTy: Float,
        camTz: Float,
        camQx: Float,
        camQy: Float,
        camQz: Float,
        camQw: Float
    ): Pair<Float, Float>? {
        val diff = Vector3f(worldPoint.x - camTx, worldPoint.y - camTy, worldPoint.z - camTz)
        // Conjugate quaternion rotates from world space into camera space
        val pCam = rotateVectorByQuaternion(diff, -camQx, -camQy, -camQz, camQw)
        if (pCam.z <= 0.001f) return null

        val u = cx + fx * (pCam.x / pCam.z)
        val v = cy + fy * (pCam.y / pCam.z)
        return Pair(u, v)
    }
}
