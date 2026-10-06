package com.tracear.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * ZoomState — manages view zoom and pan transformation for tracing detail.
 *
 * Zoom is applied to the AR view container via graphicsLayer,
 * scaling camera preview and 3D overlay simultaneously.
 */
class ZoomState {
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    /**
     * Clamps translation offset so the zoomed view cannot be panned
     * outside the visible screen area (no black/empty borders).
     */
    fun clampOffset(viewWidth: Float, viewHeight: Float) {
        if (scale <= 1f || viewWidth <= 0f || viewHeight <= 0f) {
            offset = Offset.Zero
            return
        }
        val maxX = viewWidth * (scale - 1f) / 2f
        val maxY = viewHeight * (scale - 1f) / 2f
        offset = Offset(
            offset.x.coerceIn(-maxX, maxX),
            offset.y.coerceIn(-maxY, maxY)
        )
    }
}
