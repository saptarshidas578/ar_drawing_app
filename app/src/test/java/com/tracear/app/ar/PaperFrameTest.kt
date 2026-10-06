package com.tracear.app.ar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PaperFrameTest {

    private val eps = 1e-4f

    @Test
    fun testIdentityTransform() {
        val frame = PaperFrame.IDENTITY
        assertTrue(frame.isIdentity())

        val p = Vector3f(0.1f, 0.0f, 0.2f)
        val centroid = Vector3f(0.0f, 0.0f, 0.0f)

        val transformed = frame.transform(p, centroid)
        assertEquals(p.x, transformed.x, eps)
        assertEquals(p.y, transformed.y, eps)
        assertEquals(p.z, transformed.z, eps)
    }

    @Test
    fun testPureTranslation() {
        val frame = PaperFrame(dx = 0.02f, dz = -0.015f, rotationDegrees = 0f)
        val p = Vector3f(0.10f, 0.0f, 0.20f)
        val centroid = Vector3f(0.10f, 0.0f, 0.20f)

        val transformed = frame.transform(p, centroid)
        assertEquals(0.12f, transformed.x, eps)
        assertEquals(0.185f, transformed.z, eps)

        val reverted = frame.inverseTransform(transformed, centroid)
        assertEquals(p.x, reverted.x, eps)
        assertEquals(p.z, reverted.z, eps)
    }

    @Test
    fun testPureRotationAroundCentroid() {
        // Rotate 90 degrees around origin
        val frame = PaperFrame(dx = 0f, dz = 0f, rotationDegrees = 90f)
        val centroid = Vector3f(0f, 0f, 0f)
        val p = Vector3f(1.0f, 0f, 0.0f) // On +X

        val transformed = frame.transform(p, centroid)
        // In X-Z plane, rotating (1, 0) by +90 deg: X=cos(90)=0, Z=sin(90)=1
        assertEquals(0.0f, transformed.x, eps)
        assertEquals(1.0f, transformed.z, eps)

        val reverted = frame.inverseTransform(transformed, centroid)
        assertEquals(1.0f, reverted.x, eps)
        assertEquals(0.0f, reverted.z, eps)
    }

    @Test
    fun testCombinedRotationAndTranslation() {
        val frame = PaperFrame(dx = 0.05f, dz = 0.02f, rotationDegrees = 45f)
        val centroid = Vector3f(0.2f, 0.0f, 0.3f)
        val p = Vector3f(0.3f, 0.0f, 0.4f)

        val transformed = frame.transform(p, centroid)
        val restored = frame.inverseTransform(transformed, centroid)

        assertEquals(p.x, restored.x, eps)
        assertEquals(p.z, restored.z, eps)
    }
}
