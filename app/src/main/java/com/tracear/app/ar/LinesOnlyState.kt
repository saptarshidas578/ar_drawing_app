package com.tracear.app.ar

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * LinesOnlyState — manages settings, background processing, debouncing,
 * and cached bitmap for Lines-only mode.
 */
class LinesOnlyState {

    var isEnabled by mutableStateOf(false)
    var edgeSensitivity by mutableFloatStateOf(0.60f)
    var lineThickness by mutableIntStateOf(1)
    var selectedColorOption by mutableStateOf(LineColorOption.CYAN)
    var isProcessing by mutableStateOf(false)

    // The generated transparent lines bitmap
    var linesBitmap by mutableStateOf<Bitmap?>(null)
        private set

    private var debounceJob: Job? = null

    /**
     * Schedules line extraction on a background thread with ~150ms debouncing.
     */
    fun updateLines(scope: CoroutineScope, sourceBitmap: Bitmap?) {
        if (!isEnabled || sourceBitmap == null) return

        debounceJob?.cancel()
        debounceJob = scope.launch {
            isProcessing = true
            delay(150) // 150ms debounce for smooth slider gestures

            val result = ImageLineExtractor.extractLines(
                src = sourceBitmap,
                sensitivity = edgeSensitivity,
                thickness = lineThickness,
                lineColor = selectedColorOption.color
            )

            if (result != null) {
                val old = linesBitmap
                linesBitmap = result
                if (old != null && old != sourceBitmap && !old.isRecycled) {
                    old.recycle()
                }
            }
            isProcessing = false
        }
    }

    /**
     * Resets lines-only mode and frees bitmap memory.
     */
    fun reset() {
        debounceJob?.cancel()
        isEnabled = false
        edgeSensitivity = 0.60f
        lineThickness = 1
        selectedColorOption = LineColorOption.CYAN
        isProcessing = false
        linesBitmap?.recycle()
        linesBitmap = null
    }
}
