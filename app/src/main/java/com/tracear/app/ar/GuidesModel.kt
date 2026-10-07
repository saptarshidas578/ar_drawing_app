package com.tracear.app.ar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tracear.app.data.NormalizedGuides

/**
 * Grid mode for proportion grid guides:
 * - COUNT: Fixed number of columns and rows (2 to 20).
 * - REAL_SIZE: Grid spaced by physical centimeter units (e.g. 1cm, 2cm, 5cm).
 */
enum class GuideGridMode(val label: String) {
    COUNT("By Count"),
    REAL_SIZE("Real Size (cm)")
}

/**
 * GuidesState — observable state container for drawing guides in Compose.
 */
class GuidesState {

    // ── Proportion Grid ──────────────────────────────────────────
    var isGridEnabled by mutableStateOf(false)
    var gridMode by mutableStateOf(GuideGridMode.COUNT)
    var gridCols by mutableIntStateOf(4)       // 2 to 20
    var gridRows by mutableIntStateOf(4)       // 2 to 20
    var cellSizeCm by mutableFloatStateOf(2.0f) // 1 to 10 cm
    var showLabels by mutableStateOf(true)

    // ── Construction Lines (individual toggles) ──────────────────
    var showCenterH by mutableStateOf(false)
    var showCenterV by mutableStateOf(false)
    var showDiagonals by mutableStateOf(false)
    var showThirds by mutableStateOf(false)
    var showGoldenRatio by mutableStateOf(false)

    // ── Style & Placement ────────────────────────────────────────
    var colorOption by mutableStateOf(LineColorOption.CYAN)
    var thicknessDp by mutableFloatStateOf(1.5f) // 1.0f .. 4.0f
    var opacity by mutableFloatStateOf(0.7f)     // 0.1f .. 1.0f
    var isAutoContrast by mutableStateOf(false)
    var moveWithImage by mutableStateOf(false)

    /**
     * Checks if any guide line or grid is active.
     */
    val isAnyGuideActive: Boolean
        get() = isGridEnabled || showCenterH || showCenterV || showDiagonals || showThirds || showGoldenRatio

    fun toData(): NormalizedGuides = NormalizedGuides(
        isGridEnabled = isGridEnabled,
        gridMode = gridMode.name,
        gridCols = gridCols,
        gridRows = gridRows,
        cellSizeCm = cellSizeCm,
        showLabels = showLabels,
        showCenterH = showCenterH,
        showCenterV = showCenterV,
        showDiagonals = showDiagonals,
        showThirds = showThirds,
        showGoldenRatio = showGoldenRatio,
        colorName = colorOption.name,
        thicknessDp = thicknessDp,
        opacity = opacity,
        isAutoContrast = isAutoContrast,
        moveWithImage = moveWithImage
    )

    fun applyData(data: NormalizedGuides) {
        isGridEnabled = data.isGridEnabled
        gridMode = try { GuideGridMode.valueOf(data.gridMode) } catch (e: Exception) { GuideGridMode.COUNT }
        gridCols = data.gridCols.coerceIn(2, 20)
        gridRows = data.gridRows.coerceIn(2, 20)
        cellSizeCm = data.cellSizeCm.coerceIn(0.5f, 20.0f)
        showLabels = data.showLabels
        showCenterH = data.showCenterH
        showCenterV = data.showCenterV
        showDiagonals = data.showDiagonals
        showThirds = data.showThirds
        showGoldenRatio = data.showGoldenRatio
        colorOption = LineColorOption.entries.find { it.name == data.colorName } ?: LineColorOption.CYAN
        thicknessDp = data.thicknessDp.coerceIn(0.5f, 6.0f)
        opacity = data.opacity.coerceIn(0.05f, 1.0f)
        isAutoContrast = data.isAutoContrast
        moveWithImage = data.moveWithImage
    }

    fun reset() {
        isGridEnabled = false
        gridMode = GuideGridMode.COUNT
        gridCols = 4
        gridRows = 4
        cellSizeCm = 2.0f
        showLabels = true
        showCenterH = false
        showCenterV = false
        showDiagonals = false
        showThirds = false
        showGoldenRatio = false
        colorOption = LineColorOption.CYAN
        thicknessDp = 1.5f
        opacity = 0.7f
        isAutoContrast = false
        moveWithImage = false
    }
}

/**
 * Pure mathematical routines for drawing guides.
 */
object GuidesMath {

    const val GOLDEN_RATIO_INV = 0.6180339887f
    const val GOLDEN_RATIO_COMP = 0.3819660113f

    /**
     * Converts a 0-indexed column index to chessboard letter label (0 -> "A", 1 -> "B", ..., 25 -> "Z").
     */
    fun getColumnLabel(index: Int): String {
        if (index < 0) return ""
        val sb = StringBuilder()
        var n = index
        while (n >= 0) {
            sb.insert(0, ('A'.code + (n % 26)).toChar())
            n = (n / 26) - 1
        }
        return sb.toString()
    }

    /**
     * Converts a 0-indexed row index to chessboard number label (0 -> "1", 1 -> "2", etc.).
     */
    fun getRowLabel(index: Int): String {
        if (index < 0) return ""
        return (index + 1).toString()
    }

    /**
     * Calculates column count for real-size cm mode given physical width in meters and desired cell size in cm.
     */
    fun calculateRealSizeCols(paperWidthMeters: Float, cellSizeCm: Float): Int {
        val widthCm = paperWidthMeters * 100f
        if (widthCm <= 0f || cellSizeCm <= 0.01f) return 1
        return (widthCm / cellSizeCm).toInt().coerceIn(1, 100)
    }

    /**
     * Calculates row count for real-size cm mode given physical height in meters and desired cell size in cm.
     */
    fun calculateRealSizeRows(paperHeightMeters: Float, cellSizeCm: Float): Int {
        val heightCm = paperHeightMeters * 100f
        if (heightCm <= 0f || cellSizeCm <= 0.01f) return 1
        return (heightCm / cellSizeCm).toInt().coerceIn(1, 100)
    }

    /**
     * Computes the label opacity attenuation based on view zoom scale.
     * Starts fading out at zoom 2.5x and fades to 0 at 4.0x so fine pencil tracing isn't obstructed.
     */
    fun calculateLabelZoomFade(zoomScale: Float): Float {
        if (zoomScale <= 2.5f) return 1.0f
        return (1.0f - (zoomScale - 2.5f) / 1.5f).coerceIn(0.0f, 1.0f)
    }
}
