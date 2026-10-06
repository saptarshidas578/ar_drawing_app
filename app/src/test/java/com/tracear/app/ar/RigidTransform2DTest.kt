package com.tracear.app.ar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class RigidTransform2DTest {

    private val eps = 1e-3f

    // Standard A4 paper corners in meters around origin: 210mm x 297mm
    private val baselineA4 = listOf(
        Vector3f(-0.105f, 0f, -0.1485f), // TL
        Vector3f(0.105f, 0f, -0.1485f),  // TR
        Vector3f(0.105f, 0f, 0.1485f),   // BR
        Vector3f(-0.105f, 0f, 0.1485f)   // BL
    )

    @Test
    fun testValidateCornerLengths_ExactAndSlightVariation() {
        // Exact match
        assertTrue(RigidTransform2D.validateCornerLengths(baselineA4, baselineA4))

        // 3% larger sides (within 5% tolerance)
        val validObserved = baselineA4.map { Vector3f(it.x * 1.03f, 0f, it.z * 1.03f) }
        assertTrue(RigidTransform2D.validateCornerLengths(baselineA4, validObserved))

        // 7% larger sides (exceeds 5% tolerance)
        val invalidObserved = baselineA4.map { Vector3f(it.x * 1.07f, 0f, it.z * 1.07f) }
        assertFalse(RigidTransform2D.validateCornerLengths(baselineA4, invalidObserved))
    }

    @Test
    fun testEstimateFromCorrespondingPoints_PureTranslation() {
        val targetDx = 0.025f // 2.5 cm
        val targetDz = -0.018f // -1.8 cm
        val shifted = baselineA4.map { Vector3f(it.x + targetDx, 0f, it.z + targetDz) }

        val result = RigidTransform2D.estimateFromCorrespondingPoints(baselineA4, shifted)
        assertNotNull(result)
        assertEquals(targetDx, result!!.paperFrame.dx, eps)
        assertEquals(targetDz, result.paperFrame.dz, eps)
        assertEquals(0f, result.paperFrame.rotationDegrees, eps)
        assertEquals(0f, result.residualMeters, eps)
    }

    @Test
    fun testEstimateFromCorrespondingPoints_RotationAndTranslation() {
        val targetDx = 0.020f // 2.0 cm
        val targetDz = 0.015f // 1.5 cm
        val angleDeg = 12.0f // 12 degrees
        val rad = Math.toRadians(angleDeg.toDouble()).toFloat()

        val transformed = baselineA4.map { p ->
            val rx = cos(rad) * p.x - sin(rad) * p.z + targetDx
            val rz = sin(rad) * p.x + cos(rad) * p.z + targetDz
            Vector3f(rx, 0f, rz)
        }

        val result = RigidTransform2D.estimateFromCorrespondingPoints(baselineA4, transformed)
        assertNotNull(result)
        assertEquals(targetDx, result!!.paperFrame.dx, eps)
        assertEquals(targetDz, result.paperFrame.dz, eps)
        assertEquals(angleDeg, result.paperFrame.rotationDegrees, 0.1f)
        assertEquals(0f, result.residualMeters, eps)
    }

    @Test
    fun testEstimateWithRansac_OutlierRejection() {
        val targetDx = 0.020f
        val targetDz = 0.010f

        // Create 20 points
        val basePoints = mutableListOf<Vector3f>()
        val obsPoints = mutableListOf<Vector3f>()

        for (i in 0 until 20) {
            val bx = (i % 5) * 0.04f - 0.08f
            val bz = (i / 5) * 0.06f - 0.09f
            basePoints.add(Vector3f(bx, 0f, bz))
            // Apply rigid translation
            obsPoints.add(Vector3f(bx + targetDx, 0f, bz + targetDz))
        }

        // Corrupt 5 points as outliers (e.g. from moving hand)
        for (i in 15 until 20) {
            obsPoints[i] = Vector3f(obsPoints[i].x + 0.10f, 0f, obsPoints[i].z - 0.15f)
        }

        val result = RigidTransform2D.estimateWithRansac(
            baselinePoints = basePoints,
            observedPoints = obsPoints,
            maxInlierDistMeters = 0.005f,
            minInlierRatio = 0.50f
        )

        assertNotNull(result)
        assertEquals(targetDx, result!!.paperFrame.dx, eps)
        assertEquals(targetDz, result.paperFrame.dz, eps)
        assertEquals(0f, result.paperFrame.rotationDegrees, 0.2f)
        assertTrue(result.inlierRatio >= 0.70f) // 15 out of 20 = 75%
    }
}
