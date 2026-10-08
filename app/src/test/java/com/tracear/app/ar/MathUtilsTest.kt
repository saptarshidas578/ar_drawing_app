package com.tracear.app.ar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MathUtilsTest {

    @Test
    fun testRayPlaneIntersection_ValidHit() {
        // Camera at (0, 1, 0), looking straight down (0, -1, 0)
        val ray = Ray(origin = Vector3f(0f, 1f, 0f), direction = Vector3f(0f, -1f, 0f))
        // Table plane at (0, 0, 0) with normal pointing UP (0, 1, 0)
        val planePoint = Vector3f(0f, 0f, 0f)
        val planeNormal = Vector3f(0f, 1f, 0f)

        val result = MathUtils.rayPlaneIntersection(ray, planePoint, planeNormal)
        assertTrue(result is RayPlaneResult.Hit)

        val hit = result as RayPlaneResult.Hit
        assertEquals(0f, hit.point.x, 1e-4f)
        assertEquals(0f, hit.point.y, 1e-4f)
        assertEquals(0f, hit.point.z, 1e-4f)
        assertEquals(1f, hit.distance, 1e-4f)
    }

    @Test
    fun testRayPlaneIntersection_AngledHit() {
        // Camera at (0, 1, -1), pointing forward-down (0, -1, 1)
        val ray = Ray(origin = Vector3f(0f, 1f, -1f), direction = Vector3f(0f, -1f, 1f).normalized())
        val planePoint = Vector3f(0f, 0f, 0f)
        val planeNormal = Vector3f(0f, 1f, 0f)

        val result = MathUtils.rayPlaneIntersection(ray, planePoint, planeNormal)
        assertTrue(result is RayPlaneResult.Hit)

        val hit = result as RayPlaneResult.Hit
        assertEquals(0f, hit.point.x, 1e-4f)
        assertEquals(0f, hit.point.y, 1e-4f)
        assertEquals(0f, hit.point.z, 1e-4f)
    }

    @Test
    fun testRayPlaneIntersection_ParallelRay() {
        // Ray pointing horizontally along +X
        val ray = Ray(origin = Vector3f(0f, 1f, 0f), direction = Vector3f(1f, 0f, 0f))
        val planePoint = Vector3f(0f, 0f, 0f)
        val planeNormal = Vector3f(0f, 1f, 0f)

        val result = MathUtils.rayPlaneIntersection(ray, planePoint, planeNormal)
        assertTrue(result is RayPlaneResult.Parallel)
    }

    @Test
    fun testRayPlaneIntersection_BehindCamera() {
        // Camera at (0, 1, 0), looking UP (0, 1, 0) away from plane at y = 0
        val ray = Ray(origin = Vector3f(0f, 1f, 0f), direction = Vector3f(0f, 1f, 0f))
        val planePoint = Vector3f(0f, 0f, 0f)
        val planeNormal = Vector3f(0f, 1f, 0f)

        val result = MathUtils.rayPlaneIntersection(ray, planePoint, planeNormal)
        assertTrue(result is RayPlaneResult.BehindCamera)
    }

    @Test
    fun testRayPlaneIntersection_TooFar() {
        // Camera at (0, 5, 0), looking down (0, -1, 0) towards plane at y = 0
        val ray = Ray(origin = Vector3f(0f, 5f, 0f), direction = Vector3f(0f, -1f, 0f))
        val planePoint = Vector3f(0f, 0f, 0f)
        val planeNormal = Vector3f(0f, 1f, 0f)

        // max distance 2.0m -> distance is 5.0m -> TooFar
        val result = MathUtils.rayPlaneIntersection(ray, planePoint, planeNormal, maxDistance = 2.0f)
        assertTrue(result is RayPlaneResult.TooFar)
        val tooFar = result as RayPlaneResult.TooFar
        assertEquals(5f, tooFar.distance, 1e-4f)
    }

    @Test
    fun testExtractPlaneNormal_IdentityQuaternion() {
        // Identity quaternion [0, 0, 0, 1] means no rotation: plane normal should be UP (0, 1, 0)
        val normal = MathUtils.extractPlaneNormal(0f, 0f, 0f, 1f)
        assertEquals(0f, normal.x, 1e-4f)
        assertEquals(1f, normal.y, 1e-4f)
        assertEquals(0f, normal.z, 1e-4f)
    }

    @Test
    fun testIsRoughlyVertical() {
        // Perfectly vertical normal (pointing up)
        assertTrue(MathUtils.isRoughlyVertical(Vector3f(0f, 1f, 0f)))

        // Slightly tilted normal (10 degrees)
        val rad10 = Math.toRadians(10.0).toFloat()
        val slightlyTilted = Vector3f(kotlin.math.sin(rad10), kotlin.math.cos(rad10), 0f)
        assertTrue(MathUtils.isRoughlyVertical(slightlyTilted, maxAngleDegrees = 25f))

        // Horizontal normal (wall, 90 degrees)
        assertFalse(MathUtils.isRoughlyVertical(Vector3f(1f, 0f, 0f)))
    }

    @Test
    fun testSortCornersClockwise() {
        // Points on the table surface (Y=0):
        // TL = (-1, 0, -1), TR = (1, 0, -1), BR = (1, 0, 1), BL = (-1, 0, 1)
        val pTL = Vector3f(-1f, 0f, -1f)
        val pTR = Vector3f(1f, 0f, -1f)
        val pBR = Vector3f(1f, 0f, 1f)
        val pBL = Vector3f(-1f, 0f, 1f)

        // Pass in jumbled order: BL, TR, TL, BR
        val jumbled = listOf(pBL, pTR, pTL, pBR)
        val sorted = MathUtils.sortCornersClockwise(jumbled)

        assertEquals(4, sorted.size)
        // Clockwise sorted should match TL, TR, BR, BL
        assertEquals(pTL, sorted[0])
        assertEquals(pTR, sorted[1])
        assertEquals(pBR, sorted[2])
        assertEquals(pBL, sorted[3])
    }

    @Test
    fun testMatrixInversion() {
        val identity = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )
        val inv = FloatArray(16)
        assertTrue(MathUtils.invertMatrix4(identity, inv))
        for (i in 0..15) {
            assertEquals(identity[i], inv[i], 1e-4f)
        }
    }

    @Test
    fun testCameraUnprojectionAndProjection() {
        // Simulated camera looking down at origin from (0, 0.5, 0)
        // Camera optical coordinates: +Z is forward (down towards -Y in world),
        // +X is right (+X in world), +Y is down (+Z in world)
        // Rotation quaternion for this: 90 deg around +X axis
        // qx = sin(45 deg) = 0.7071f, qw = cos(45 deg) = 0.7071f
        val qx = 0.70710677f
        val qy = 0f
        val qz = 0f
        val qw = 0.70710677f

        val fx = 1000f
        val fy = 1000f
        val cx = 500f
        val cy = 500f

        // Center pixel (500, 500) unprojected should shoot ray straight down towards -Y
        val ray = MathUtils.unprojectCameraPixelToRay(
            u = 500f, v = 500f,
            fx = fx, fy = fy, cx = cx, cy = cy,
            camTx = 0f, camTy = 0.5f, camTz = 0f,
            camQx = qx, camQy = qy, camQz = qz, camQw = qw
        )

        assertEquals(0f, ray.origin.x, 1e-4f)
        assertEquals(0.5f, ray.origin.y, 1e-4f)
        assertEquals(0f, ray.origin.z, 1e-4f)

        // Direction should be (0, -1, 0)
        assertEquals(0f, ray.direction.x, 1e-4f)
        assertEquals(-1f, ray.direction.y, 1e-4f)
        assertEquals(0f, ray.direction.z, 1e-4f)

        // Hit point on table plane y=0 should be (0, 0, 0)
        val planePoint = Vector3f(0f, 0f, 0f)
        val planeNormal = Vector3f(0f, 1f, 0f)
        val hitResult = MathUtils.rayPlaneIntersection(ray, planePoint, planeNormal)
        assertTrue(hitResult is RayPlaneResult.Hit)
        val hit = (hitResult as RayPlaneResult.Hit).point
        assertEquals(0f, hit.x, 1e-4f)
        assertEquals(0f, hit.y, 1e-4f)
        assertEquals(0f, hit.z, 1e-4f)

        // Projecting (0, 0, 0) back should yield center pixel (500, 500)
        val proj = MathUtils.projectWorldPointToCameraPixel(
            worldPoint = Vector3f(0f, 0f, 0f),
            fx = fx, fy = fy, cx = cx, cy = cy,
            camTx = 0f, camTy = 0.5f, camTz = 0f,
            camQx = qx, camQy = qy, camQz = qz, camQw = qw
        )
        org.junit.Assert.assertNotNull(proj)
        assertEquals(500f, proj!!.first, 1e-2f)
        assertEquals(500f, proj.second, 1e-2f)
    }

    @Test
    fun testSortCornersClockwise_IncompleteAndDegenerate() {
        // Empty list should return empty list without crash
        val empty = MathUtils.sortCornersClockwise(emptyList())
        assertTrue(empty.isEmpty())

        // Fewer than 4 points should return original list without crash
        val twoPoints = listOf(Vector3f(0f, 0f, 0f), Vector3f(1f, 0f, 0f))
        val sortedTwo = MathUtils.sortCornersClockwise(twoPoints)
        assertEquals(2, sortedTwo.size)

        // 4 collinear points
        val collinear = listOf(
            Vector3f(0f, 0f, 0f),
            Vector3f(1f, 0f, 0f),
            Vector3f(2f, 0f, 0f),
            Vector3f(3f, 0f, 0f)
        )
        val sortedCollinear = MathUtils.sortCornersClockwise(collinear)
        assertEquals(4, sortedCollinear.size)
    }

    @Test
    fun testMatrixInversion_SingularMatrixReturnsFalse() {
        val zeroMatrix = FloatArray(16) { 0f }
        val inv = FloatArray(16)
        val result = MathUtils.invertMatrix4(zeroMatrix, inv)
        assertFalse(result)
    }

    @Test
    fun testProjectWorldPointToCameraPixel_PointBehindCameraReturnsNull() {
        val fx = 1000f; val fy = 1000f; val cx = 500f; val cy = 500f
        // Camera at (0, 0, 0), looking along optical +Z (identity rotation)
        val result = MathUtils.projectWorldPointToCameraPixel(
            worldPoint = Vector3f(0f, 0f, -5f), // Behind camera in local coordinates
            fx = fx, fy = fy, cx = cx, cy = cy,
            camTx = 0f, camTy = 0f, camTz = 0f,
            camQx = 0f, camQy = 0f, camQz = 0f, camQw = 1f
        )
        org.junit.Assert.assertNull(result)
    }
}
