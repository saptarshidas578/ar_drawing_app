package com.tracear.app.ar

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.tracear.app.data.NormalizedTones
import kotlin.math.roundToInt

/**
 * Configuration for an individual tonal layer.
 */
class TonalLayerConfig(
    val index: Int,
    name: String,
    isVisible: Boolean = true,
    colorArgb: Int = 0xFF888888.toInt(),
    opacity: Float = 0.7f
) {
    var name by mutableStateOf(name)
    var isVisible by mutableStateOf(isVisible)
    var colorArgb by mutableIntStateOf(colorArgb)
    var opacity by mutableFloatStateOf(opacity)
}

/**
 * Result of sampling a pixel for value, tone, and hex code.
 */
data class ValueInspectionResult(
    val x: Int,
    val y: Int,
    val u: Float,
    val v: Float,
    val colorArgb: Int,
    val hexCode: String,
    val brightnessPercent: Int,
    val rawLuminance: Int,
    val toneIndex: Int,
    val toneName: String
)

/**
 * Pure mathematical routines and color defaults for tonal layers.
 */
object TonalMath {

    /**
     * Standard Rec.601 luminance calculation (0 to 255).
     */
    fun computeLuminance(r: Int, g: Int, b: Int): Int {
        return (0.299 * r + 0.587 * g + 0.114 * b).roundToInt().coerceIn(0, 255)
    }

    /**
     * Quantizes luminance into N tones (from 0 = Lightest/Highlight to N-1 = Darkest/Shadow).
     */
    fun quantizeLuminance(luminance: Int, toneCount: Int): Int {
        if (toneCount <= 1) return 0
        val clampedCount = toneCount.coerceIn(2, 6)
        // Invert luminance so 255 (white) maps to 0 (Highlights) and 0 (black) maps to N-1 (Shadows)
        val inverted = 255 - luminance.coerceIn(0, 255)
        return ((inverted * clampedCount) / 256).coerceIn(0, clampedCount - 1)
    }

    /**
     * Formats ARGB integer to standard hex code "#RRGGBB".
     */
    fun toHex(colorArgb: Int): String {
        val r = (colorArgb shr 16) and 0xFF
        val g = (colorArgb shr 8) and 0xFF
        val b = colorArgb and 0xFF
        return "#%02X%02X%02X".format(r, g, b)
    }

    /**
     * Returns a human-friendly tone label based on index and total count.
     */
    fun getToneName(toneIndex: Int, totalTones: Int): String {
        return when (totalTones) {
            2 -> if (toneIndex == 0) "Lights" else "Shadows"
            3 -> when (toneIndex) {
                0 -> "Highlights"
                1 -> "Midtones"
                else -> "Shadows"
            }
            4 -> when (toneIndex) {
                0 -> "Highlights"
                1 -> "Light Midtone"
                2 -> "Dark Midtone"
                else -> "Shadows"
            }
            5 -> when (toneIndex) {
                0 -> "Highlights"
                1 -> "Quarter-tone"
                2 -> "Midtone"
                3 -> "Three-quarter"
                else -> "Shadows"
            }
            6 -> when (toneIndex) {
                0 -> "Highlights"
                1 -> "Light Tone"
                2 -> "Mid-Light"
                3 -> "Mid-Dark"
                4 -> "Dark Tone"
                else -> "Deep Shadows"
            }
            else -> "Tone ${toneIndex + 1}"
        }
    }

    /**
     * Default contrasting color and opacity ramp for white tracing paper.
     * Tone 0 (highlights) is light gray/slate, progressing to deep charcoal for shadows.
     */
    fun getDefaultLayerConfigs(count: Int): List<TonalLayerConfig> {
        val clamped = count.coerceIn(2, 6)
        val defaultColors = listOf(
            0xFFCBD5E1.toInt(), // Slate 300 (Lightest)
            0xFF94A3B8.toInt(), // Slate 400
            0xFF64748B.toInt(), // Slate 500
            0xFF334155.toInt(), // Slate 700
            0xFF1E293B.toInt(), // Slate 800
            0xFF0F172A.toInt()  // Slate 900 (Deepest)
        )

        return (0 until clamped).map { i ->
            val fraction = i.toFloat() / (clamped - 1).coerceAtLeast(1)
            val colorIndex = (fraction * (defaultColors.size - 1)).roundToInt().coerceIn(0, defaultColors.size - 1)
            val opacity = (0.35f + fraction * 0.60f).coerceIn(0.2f, 1.0f)
            TonalLayerConfig(
                index = i,
                name = getToneName(i, clamped),
                isVisible = true,
                colorArgb = defaultColors[colorIndex],
                opacity = opacity
            )
        }
    }
}

/**
 * TonalState — observable state container for Tonal Layers and shading workflow.
 */
class TonalState {

    var isEnabled by mutableStateOf(false)
    var toneCount by mutableIntStateOf(4)           // 2 to 6
    var smoothingLevel by mutableFloatStateOf(0.5f)  // 0.1f to 1.0f

    // Background processing status
    var isProcessing by mutableStateOf(false)
    var processingTimeMs by mutableIntStateOf(0)

    // Layers (Tone 0 .. Tone N-1)
    val layers = mutableStateListOf<TonalLayerConfig>().apply {
        addAll(TonalMath.getDefaultLayerConfigs(4))
    }

    // Outline Layer (Lines-only integration)
    var isOutlineVisible by mutableStateOf(true)
    var outlineColorArgb by mutableIntStateOf(0xFF00E5FF.toInt()) // Cyan default
    var outlineOpacity by mutableFloatStateOf(1.0f)

    // Solo mode: null = normal; -1 = solo outline; 0..N-1 = solo that tone
    var soloLayerId by mutableStateOf<Int?>(null)

    // Stages mode (guided stepper)
    var isStagesMode by mutableStateOf(false)
    var currentStage by mutableIntStateOf(0) // 0 = Outline, 1..N = Tones

    // Value Eyedropper & Inspector
    var isEyedropperActive by mutableStateOf(false)
    var inspectedValue by mutableStateOf<ValueInspectionResult?>(null)

    // Cached composite bitmap ready for Filament quad rendering
    var compositeBitmap by mutableStateOf<Bitmap?>(null)

    /**
     * Rebuilds layers list to match toneCount while preserving existing customizations.
     */
    fun updateToneCount(newCount: Int) {
        val clamped = newCount.coerceIn(2, 6)
        if (toneCount == clamped && layers.size == clamped) return
        toneCount = clamped

        val defaults = TonalMath.getDefaultLayerConfigs(clamped)
        layers.clear()
        layers.addAll(defaults)

        if (soloLayerId != null && soloLayerId!! >= clamped) {
            soloLayerId = null
        }
        if (currentStage > clamped) {
            currentStage = clamped
        }
    }

    /**
     * Toggles solo state for a layer (-1 = outline, 0..N-1 = tone).
     */
    fun toggleSolo(layerId: Int) {
        soloLayerId = if (soloLayerId == layerId) null else layerId
    }

    /**
     * Advances to the next stage in Stages mode.
     */
    fun nextStage() {
        if (currentStage < toneCount) {
            currentStage++
        }
    }

    /**
     * Moves to the previous stage in Stages mode.
     */
    fun prevStage() {
        if (currentStage > 0) {
            currentStage--
        }
    }

    /**
     * Stage title for display in the stepper chip.
     */
    val currentStageLabel: String
        get() = when (currentStage) {
            0 -> "Stage 1/${toneCount + 1}: Outline"
            else -> "Stage ${currentStage + 1}/${toneCount + 1}: ${TonalMath.getToneName(currentStage - 1, toneCount)}"
        }

    fun toData(): NormalizedTones = NormalizedTones(
        isEnabled = isEnabled,
        toneCount = toneCount,
        smoothingLevel = smoothingLevel,
        layerVisibilities = layers.map { it.isVisible },
        layerColors = layers.map { it.colorArgb },
        layerOpacities = layers.map { it.opacity },
        isOutlineVisible = isOutlineVisible,
        outlineColorArgb = outlineColorArgb,
        outlineOpacity = outlineOpacity,
        isStagesMode = isStagesMode,
        currentStage = currentStage
    )

    fun applyData(data: NormalizedTones) {
        isEnabled = data.isEnabled
        toneCount = data.toneCount.coerceIn(2, 6)
        smoothingLevel = data.smoothingLevel.coerceIn(0.1f, 1.0f)
        isOutlineVisible = data.isOutlineVisible
        outlineColorArgb = data.outlineColorArgb
        outlineOpacity = data.outlineOpacity.coerceIn(0.1f, 1.0f)
        isStagesMode = data.isStagesMode
        currentStage = data.currentStage.coerceIn(0, toneCount)

        val defaults = TonalMath.getDefaultLayerConfigs(toneCount)
        layers.clear()
        for (i in 0 until toneCount) {
            val vis = data.layerVisibilities.getOrNull(i) ?: defaults[i].isVisible
            val col = data.layerColors.getOrNull(i) ?: defaults[i].colorArgb
            val op = data.layerOpacities.getOrNull(i) ?: defaults[i].opacity
            layers.add(
                TonalLayerConfig(
                    index = i,
                    name = TonalMath.getToneName(i, toneCount),
                    isVisible = vis,
                    colorArgb = col,
                    opacity = op
                )
            )
        }
    }

    fun reset() {
        isEnabled = false
        toneCount = 4
        smoothingLevel = 0.5f
        isProcessing = false
        isStagesMode = false
        currentStage = 0
        soloLayerId = null
        isEyedropperActive = false
        inspectedValue = null
        isOutlineVisible = true
        outlineColorArgb = 0xFF00E5FF.toInt()
        outlineOpacity = 1.0f
        layers.clear()
        layers.addAll(TonalMath.getDefaultLayerConfigs(4))
        compositeBitmap?.recycle()
        compositeBitmap = null
    }
}
