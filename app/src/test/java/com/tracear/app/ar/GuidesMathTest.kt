package com.tracear.app.ar

import com.tracear.app.data.NormalizedGuides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidesMathTest {

    @Test
    fun testColumnLabels() {
        assertEquals("A", GuidesMath.getColumnLabel(0))
        assertEquals("B", GuidesMath.getColumnLabel(1))
        assertEquals("C", GuidesMath.getColumnLabel(2))
        assertEquals("Z", GuidesMath.getColumnLabel(25))
        assertEquals("AA", GuidesMath.getColumnLabel(26))
        assertEquals("AB", GuidesMath.getColumnLabel(27))
        assertEquals("AZ", GuidesMath.getColumnLabel(51))
        assertEquals("BA", GuidesMath.getColumnLabel(52))
        assertEquals("", GuidesMath.getColumnLabel(-1))
    }

    @Test
    fun testRowLabels() {
        assertEquals("1", GuidesMath.getRowLabel(0))
        assertEquals("2", GuidesMath.getRowLabel(1))
        assertEquals("10", GuidesMath.getRowLabel(9))
        assertEquals("25", GuidesMath.getRowLabel(24))
        assertEquals("", GuidesMath.getRowLabel(-1))
    }

    @Test
    fun testRealSizeCalculationsA4() {
        // A4 Paper: 21.0 cm x 29.7 cm
        val paperW = 0.210f
        val paperH = 0.297f

        // Cell size 2 cm -> 21 / 2 = 10 cols, 29.7 / 2 = 14 rows
        assertEquals(10, GuidesMath.calculateRealSizeCols(paperW, 2.0f))
        assertEquals(14, GuidesMath.calculateRealSizeRows(paperH, 2.0f))

        // Cell size 1 cm -> 21 cols, 29 rows
        assertEquals(21, GuidesMath.calculateRealSizeCols(paperW, 1.0f))
        assertEquals(29, GuidesMath.calculateRealSizeRows(paperH, 1.0f))

        // Cell size 5 cm -> 4 cols, 5 rows
        assertEquals(4, GuidesMath.calculateRealSizeCols(paperW, 5.0f))
        assertEquals(5, GuidesMath.calculateRealSizeRows(paperH, 5.0f))
    }

    @Test
    fun testRealSizeEdgeCases() {
        // Zero or invalid dimensions should return safe fallback >= 1
        assertEquals(1, GuidesMath.calculateRealSizeCols(0f, 2.0f))
        assertEquals(1, GuidesMath.calculateRealSizeRows(-1f, 2.0f))
        assertEquals(1, GuidesMath.calculateRealSizeCols(0.210f, 0f))
        assertEquals(1, GuidesMath.calculateRealSizeRows(0.297f, -5f))
    }

    @Test
    fun testGoldenRatioConstants() {
        assertEquals(1.0f, GuidesMath.GOLDEN_RATIO_INV + GuidesMath.GOLDEN_RATIO_COMP, 1e-6f)
        assertEquals(0.618034f, GuidesMath.GOLDEN_RATIO_INV, 1e-4f)
        assertEquals(0.381966f, GuidesMath.GOLDEN_RATIO_COMP, 1e-4f)
    }

    @Test
    fun testLabelZoomFade() {
        // Zoom <= 2.5x -> fully visible (1.0f)
        assertEquals(1.0f, GuidesMath.calculateLabelZoomFade(1.0f), 1e-5f)
        assertEquals(1.0f, GuidesMath.calculateLabelZoomFade(2.0f), 1e-5f)
        assertEquals(1.0f, GuidesMath.calculateLabelZoomFade(2.5f), 1e-5f)

        // Mid zoom: 3.25x -> exactly half opacity (0.5f)
        assertEquals(0.5f, GuidesMath.calculateLabelZoomFade(3.25f), 1e-5f)

        // Zoom >= 4.0x -> fully hidden (0.0f)
        assertEquals(0.0f, GuidesMath.calculateLabelZoomFade(4.0f), 1e-5f)
        assertEquals(0.0f, GuidesMath.calculateLabelZoomFade(5.0f), 1e-5f)
    }

    @Test
    fun testGuidesStateRoundTrip() {
        val state = GuidesState().apply {
            isGridEnabled = true
            gridMode = GuideGridMode.REAL_SIZE
            gridCols = 8
            gridRows = 10
            cellSizeCm = 3.5f
            showLabels = true
            showCenterH = true
            showCenterV = true
            showDiagonals = true
            showThirds = true
            showGoldenRatio = true
            colorOption = LineColorOption.YELLOW
            thicknessDp = 2.5f
            opacity = 0.85f
            isAutoContrast = true
            moveWithImage = true
        }

        assertTrue(state.isAnyGuideActive)

        val data = state.toData()
        assertEquals(true, data.isGridEnabled)
        assertEquals("REAL_SIZE", data.gridMode)
        assertEquals(8, data.gridCols)
        assertEquals(10, data.gridRows)
        assertEquals(3.5f, data.cellSizeCm, 1e-4f)
        assertEquals(true, data.showLabels)
        assertEquals(true, data.showCenterH)
        assertEquals(true, data.showCenterV)
        assertEquals(true, data.showDiagonals)
        assertEquals(true, data.showThirds)
        assertEquals(true, data.showGoldenRatio)
        assertEquals("YELLOW", data.colorName)
        assertEquals(2.5f, data.thicknessDp, 1e-4f)
        assertEquals(0.85f, data.opacity, 1e-4f)
        assertEquals(true, data.isAutoContrast)
        assertEquals(true, data.moveWithImage)

        // Apply data to a clean state
        val restored = GuidesState()
        restored.applyData(data)
        assertEquals(true, restored.isGridEnabled)
        assertEquals(GuideGridMode.REAL_SIZE, restored.gridMode)
        assertEquals(8, restored.gridCols)
        assertEquals(10, restored.gridRows)
        assertEquals(3.5f, restored.cellSizeCm, 1e-4f)
        assertEquals(LineColorOption.YELLOW, restored.colorOption)
        assertEquals(2.5f, restored.thicknessDp, 1e-4f)
        assertEquals(0.85f, restored.opacity, 1e-4f)
        assertEquals(true, restored.isAutoContrast)
        assertEquals(true, restored.moveWithImage)

        // Reset
        restored.reset()
        assertFalse(restored.isGridEnabled)
        assertFalse(restored.isAnyGuideActive)
        assertEquals(GuideGridMode.COUNT, restored.gridMode)
    }
}
