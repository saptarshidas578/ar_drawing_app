package com.tracear.app.ar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PaperPresetsTest {

    @Test
    fun testCentroidPreservation() {
        val originalCorners = listOf(
            Vector3f(0.10f, 0.0f, 0.20f),
            Vector3f(0.35f, 0.0f, 0.22f),
            Vector3f(0.33f, 0.0f, 0.55f),
            Vector3f(0.08f, 0.0f, 0.53f)
        )
        val origCentroid = PaperMath.calculateCentroid(originalCorners)

        val snapped = PaperMath.snapCornersToPreset(originalCorners, PaperPreset.A4.widthMeters, PaperPreset.A4.heightMeters)
        val newCentroid = PaperMath.calculateCentroid(snapped)

        assertEquals(origCentroid.x, newCentroid.x, 1e-4f)
        assertEquals(origCentroid.y, newCentroid.y, 1e-4f)
        assertEquals(origCentroid.z, newCentroid.z, 1e-4f)
    }

    @Test
    fun testExactRectangleDimensionsA4() {
        // Portrait marked corners: width ~0.24m, height ~0.33m
        val originalCorners = listOf(
            Vector3f(-0.12f, 0.0f, -0.16f),
            Vector3f(0.12f, 0.0f, -0.16f),
            Vector3f(0.12f, 0.0f, 0.17f),
            Vector3f(-0.12f, 0.0f, 0.17f)
        )

        val snapped = PaperMath.snapCornersToPreset(originalCorners, 0.210f, 0.297f)
        assertEquals(4, snapped.size)

        val tl = snapped[0]
        val tr = snapped[1]
        val br = snapped[2]
        val bl = snapped[3]

        val width = (tr - tl).length()
        val height = (bl - tl).length()

        assertEquals(0.210f, width, 1e-3f)
        assertEquals(0.297f, height, 1e-3f)

        // Orthogonality: top edge and left edge must be perpendicular (dot product = 0)
        val u = tr - tl
        val v = bl - tl
        assertEquals(0f, u.dot(v), 1e-4f)

        // Opposite sides must be equal
        assertEquals((br - bl).length(), width, 1e-4f)
        assertEquals((br - tr).length(), height, 1e-4f)
    }

    @Test
    fun testLandscapePreservation() {
        // Landscape marked corners: width ~0.35m, height ~0.22m
        val landscapeCorners = listOf(
            Vector3f(-0.17f, 0.0f, -0.11f),
            Vector3f(0.18f, 0.0f, -0.11f),
            Vector3f(0.18f, 0.0f, 0.11f),
            Vector3f(-0.17f, 0.0f, 0.11f)
        )

        val snapped = PaperMath.snapCornersToPreset(landscapeCorners, 0.210f, 0.297f)
        val tl = snapped[0]
        val tr = snapped[1]
        val bl = snapped[3]

        val width = (tr - tl).length()
        val height = (bl - tl).length()

        // In landscape, width should snap to the larger dimension (29.7cm), height to 21.0cm
        assertEquals(0.297f, width, 1e-3f)
        assertEquals(0.210f, height, 1e-3f)
    }

    @Test
    fun testUnitConversions() {
        assertEquals(21.0f, PaperMath.metersToCm(0.210f), 1e-4f)
        assertEquals(0.297f, PaperMath.cmToMeters(29.7f), 1e-4f)
    }

    @Test
    fun testRealWorldDrawingScaleCalculations() {
        // A4 Paper: 21cm x 29.7cm, square image (aspect = 1.0) in FIT mode
        // In FIT mode, square image fits paper width (21cm).
        val paperW = 0.210f
        val paperH = 0.297f
        val imgAspect = 1.0f

        val baseWidth = PaperMath.calculateBaseDrawingWidthMeters(paperW, paperH, imgAspect, FitMode.FIT)
        assertEquals(0.210f, baseWidth, 1e-4f)

        // If target drawing width is 10.5cm (0.105m), scale should be exactly 0.5f (50%)
        val scaleFor10_5cm = PaperMath.calculateScaleForDrawingWidth(0.105f, paperW, paperH, imgAspect, FitMode.FIT)
        assertEquals(0.5f, scaleFor10_5cm, 1e-3f)

        // Current width cm for scale = 0.5 should be 10.5 cm
        val currentCm = PaperMath.getCurrentDrawingWidthCm(paperW, paperH, imgAspect, FitMode.FIT, 0.5f)
        assertEquals(10.5f, currentCm, 1e-3f)
    }
}
