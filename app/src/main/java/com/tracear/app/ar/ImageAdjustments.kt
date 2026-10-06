package com.tracear.app.ar

import android.graphics.ColorMatrix

/**
 * Image adjustment parameters applied to the reference image in normal tracing mode.
 *
 * All transformations are evaluated entirely on the GPU via [android.graphics.ColorMatrixColorFilter]
 * with zero per-pixel loops on the main thread.
 *
 * @param brightness Offset from -1.0f (darkest) to +1.0f (brightest), default 0.0f.
 * @param contrast Scale from 0.2f (flat) to 3.0f (high contrast), default 1.0f.
 * @param invert Inversion factor from 0.0f (normal) to 1.0f (fully inverted), default 0.0f.
 * @param bwThreshold Binary black-and-white cutoff from 0.0f (disabled) to 1.0f, default 0.0f.
 */
data class ImageAdjustments(
    val brightness: Float = 0f,
    val contrast: Float = 1.0f,
    val invert: Float = 0f,
    val bwThreshold: Float = 0f
) {
    /**
     * Checks if adjustments differ from standard defaults.
     */
    val isDefault: Boolean
        get() = brightness == 0f && contrast == 1.0f && invert == 0f && bwThreshold <= 0.01f

    /**
     * Constructs a composite hardware-accelerated [ColorMatrix] combining all active adjustments.
     */
    fun toColorMatrix(): ColorMatrix {
        val composite = ColorMatrix()

        // 1. Black and White Threshold mode (if enabled > 0.01f)
        if (bwThreshold > 0.01f) {
            val thresholdVal = bwThreshold * 255f
            // High gain factor to create a sharp step at the threshold
            val gain = 100f
            val offset = -gain * thresholdVal

            // Grayscale luminance coefficients: 0.299R + 0.587G + 0.114B
            val bwMatrix = ColorMatrix(
                floatArrayOf(
                    0.299f * gain, 0.587f * gain, 0.114f * gain, 0f, offset,
                    0.299f * gain, 0.587f * gain, 0.114f * gain, 0f, offset,
                    0.299f * gain, 0.587f * gain, 0.114f * gain, 0f, offset,
                    0f,            0f,            0f,            1f, 0f
                )
            )
            composite.postConcat(bwMatrix)
        } else {
            // Contrast matrix: R' = c*(R - 128) + 128 = c*R + 128*(1 - c)
            if (contrast != 1.0f) {
                val c = contrast.coerceIn(0.2f, 3.0f)
                val cOffset = 128f * (1f - c)
                val contrastMatrix = ColorMatrix(
                    floatArrayOf(
                        c,  0f, 0f, 0f, cOffset,
                        0f, c,  0f, 0f, cOffset,
                        0f, 0f, c,  0f, cOffset,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                composite.postConcat(contrastMatrix)
            }
        }

        // 2. Brightness adjustment: add b * 255 to R, G, B
        if (brightness != 0f) {
            val bOffset = brightness.coerceIn(-1f, 1f) * 255f
            val brightnessMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, bOffset,
                    0f, 1f, 0f, 0f, bOffset,
                    0f, 0f, 1f, 0f, bOffset,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            composite.postConcat(brightnessMatrix)
        }

        // 3. Inversion adjustment: R' = (1 - 2*inv)*R + 255*inv
        if (invert > 0.01f) {
            val inv = invert.coerceIn(0f, 1f)
            val scale = 1f - 2f * inv
            val invOffset = 255f * inv
            val invertMatrix = ColorMatrix(
                floatArrayOf(
                    scale, 0f,    0f,    0f, invOffset,
                    0f,    scale, 0f,    0f, invOffset,
                    0f,    0f,    scale, 0f, invOffset,
                    0f,    0f,    0f,    1f, 0f
                )
            )
            composite.postConcat(invertMatrix)
        }

        return composite
    }
}
