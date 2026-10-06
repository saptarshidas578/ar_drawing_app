package com.tracear.app.ar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * GridState — holds all section-grid UI state as observable Compose state.
 *
 * Grid cells are numbered with a flat index (0 until totalCells).
 * Snake order: even rows go L→R, odd rows go R→L.
 */
class GridState {

    // ── Grid on/off ─────────────────────────────────────────
    var isEnabled by mutableStateOf(false)

    // ── Grid size (cols × rows) ──────────────────────────────
    var cols by mutableIntStateOf(3)
    var rows by mutableIntStateOf(3)

    // ── Active cell (flat index), -1 = nothing selected ──────
    var activeCell by mutableIntStateOf(-1)

    // ── Done cells (flat indices) ─────────────────────────────
    val doneCells = mutableStateListOf<Int>()

    // ── Computed helpers ──────────────────────────────────────
    val totalCells: Int get() = cols * rows
    val doneCount:  Int get() = doneCells.size

    /**
     * Convert a flat index to (col, row).
     * Snake order: even rows are L→R, odd rows are R→L.
     */
    fun cellToColRow(index: Int): Pair<Int, Int> {
        val clamped = index.coerceIn(0, totalCells - 1)
        val row = clamped / cols
        val col = if (row % 2 == 0) clamped % cols else cols - 1 - (clamped % cols)
        return col to row
    }

    /**
     * Convert (col, row) to flat index in snake order.
     */
    fun colRowToCell(col: Int, row: Int): Int {
        val c = col.coerceIn(0, cols - 1)
        val r = row.coerceIn(0, rows - 1)
        val snakeCol = if (r % 2 == 0) c else (cols - 1 - c)
        return r * cols + snakeCol
    }

    /** Move to the next cell in snake order, clamped to last cell. */
    fun nextSnakeIndex(current: Int): Int {
        if (current < 0) return 0
        return (current + 1).coerceAtMost(totalCells - 1)
    }

    /** Move to the previous cell in snake order, clamped to 0. */
    fun prevSnakeIndex(current: Int): Int {
        if (current < 0) return 0
        return (current - 1).coerceAtLeast(0)
    }

    /** Toggle the done-state of a cell. */
    fun toggleDone(cellIndex: Int) {
        if (doneCells.contains(cellIndex)) {
            doneCells.remove(cellIndex)
        } else {
            doneCells.add(cellIndex)
        }
    }

    /** Change grid dimensions keeping them square (2×2 up to 6×6). */
    fun setGridDimension(dim: Int) {
        val d = dim.coerceIn(2, 6)
        cols = d
        rows = d
        clampActiveCell()
    }

    fun increaseSize() {
        if (cols < 6) {
            cols++
            rows++
            clampActiveCell()
        }
    }

    fun decreaseSize() {
        if (cols > 2) {
            cols--
            rows--
            clampActiveCell()
        }
    }

    /** Keep activeCell in bounds after size changes. */
    private fun clampActiveCell() {
        if (activeCell >= totalCells) activeCell = totalCells - 1
    }

    /** Full reset — called on re-calibration. */
    fun reset() {
        isEnabled = false
        cols = 3
        rows = 3
        activeCell = -1
        doneCells.clear()
    }
}
