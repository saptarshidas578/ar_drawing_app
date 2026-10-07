package com.tracear.app.ar

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max

/**
 * Supported line colors for Lines-only mode.
 */
enum class LineColorOption(val label: String, val color: Color) {
    CYAN("Cyan", Color(0xFF00E5FF)),
    YELLOW("Yellow", Color(0xFFFFD600)),
    RED("Red", Color(0xFFFF3D00)),
    GREEN("Green", Color(0xFF00E676)),
    BLUE("Blue", Color(0xFF2979FF)),
    WHITE("White", Color(0xFFFFFFFF)),
    BLACK("Black", Color(0xFF000000))
}

/**
 * ImageLineExtractor — extracts clean, transparent line art from a reference bitmap using OpenCV.
 *
 * Processing Pipeline:
 * 1. Downscales if max dimension > 2048px (prevents OOM and ensures fast edge detection).
 * 2. Converts bitmap to OpenCV grayscale Mat.
 * 3. Applies Gaussian blur to eliminate camera sensor noise and paper texture.
 * 4. Runs Canny edge detector with sensitivity-mapped thresholds.
 * 5. Applies morphological dilation for line thickness.
 * 6. Generates an ARGB_8888 bitmap with 100% transparent background and colored edge lines.
 */
object ImageLineExtractor {

    private const val TAG = "TraceAR"
    private const val MAX_DIMENSION = 2048

    /**
     * Extracts lines on a background thread.
     *
     * @param src Source reference bitmap.
     * @param sensitivity Edge sensitivity between 0.1f (fewer lines) and 1.0f (more detailed lines).
     * @param thickness Line thickness from 1 (thinnest) to 5 (thickest).
     * @param lineColor Chosen line stroke color.
     * @return ARGB_8888 bitmap with transparent background and colored line art, or null on error.
     */
    suspend fun extractLines(
        src: Bitmap,
        sensitivity: Float,
        thickness: Int,
        lineColor: Color
    ): Bitmap? = withContext(Dispatchers.Default) {
        val clampedSens = sensitivity.coerceIn(0.1f, 1.0f)
        val clampedThickness = thickness.coerceIn(1, 5)

        // 1. Scale down if larger than MAX_DIMENSION
        val maxDim = max(src.width, src.height)
        val scale = if (maxDim > MAX_DIMENSION) MAX_DIMENSION.toFloat() / maxDim else 1.0f
        val targetW = (src.width * scale).toInt().coerceAtLeast(1)
        val targetH = (src.height * scale).toInt().coerceAtLeast(1)

        val workingBmp = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(src, targetW, targetH, true)
        } else {
            src
        }

        var rgbaMat: Mat? = null
        var grayMat: Mat? = null
        var blurredMat: Mat? = null
        var edgesMat: Mat? = null
        var dilatedMat: Mat? = null
        var kernel: Mat? = null

        try {
            rgbaMat = Mat()
            Utils.bitmapToMat(workingBmp, rgbaMat)

            grayMat = Mat()
            Imgproc.cvtColor(rgbaMat, grayMat, Imgproc.COLOR_RGBA2GRAY)

            // 2. Gaussian blur
            blurredMat = Mat()
            Imgproc.GaussianBlur(grayMat, blurredMat, Size(3.0, 3.0), 0.0)

            // 3. Canny edge detector thresholds mapped from sensitivity
            // Low sensitivity -> high threshold (only strong lines)
            // High sensitivity -> low threshold (captures fine details)
            val highThreshold = (220.0 * (1.15 - clampedSens)).coerceIn(30.0, 240.0)
            val lowThreshold = highThreshold / 2.5

            edgesMat = Mat()
            Imgproc.Canny(blurredMat, edgesMat, lowThreshold, highThreshold)

            // 4. Line thickness dilation
            val finalEdges: Mat
            if (clampedThickness > 1) {
                val kSize = (clampedThickness * 2 - 1).toDouble() // 3, 5, 7, 9
                kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(kSize, kSize))
                dilatedMat = Mat()
                Imgproc.dilate(edgesMat, dilatedMat, kernel)
                finalEdges = dilatedMat
            } else {
                finalEdges = edgesMat
            }

            // 5. Build transparent ARGB_8888 bitmap
            val w = finalEdges.width()
            val h = finalEdges.height()
            val edgeBytes = ByteArray(w * h)
            finalEdges.get(0, 0, edgeBytes)

            val argbColor = lineColor.toArgb()
            val colorR = AndroidColor.red(argbColor)
            val colorG = AndroidColor.green(argbColor)
            val colorB = AndroidColor.blue(argbColor)

            val outPixels = IntArray(w * h)
            for (i in 0 until (w * h)) {
                val v = edgeBytes[i].toInt() and 0xFF
                if (v > 0) {
                    // Edge pixel: fully opaque chosen color
                    outPixels[i] = AndroidColor.argb(255, colorR, colorG, colorB)
                } else {
                    // Non-edge pixel: 100% transparent
                    outPixels[i] = 0
                }
            }

            val outBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            outBitmap.setPixels(outPixels, 0, w, 0, 0, w, h)
            return@withContext outBitmap

        } catch (e: Throwable) {
            Log.e(TAG, "extractLines error: ${e.message}", e)
            return@withContext null
        } finally {
            // Clean up all OpenCV native allocations
            rgbaMat?.release()
            grayMat?.release()
            blurredMat?.release()
            edgesMat?.release()
            dilatedMat?.release()
            kernel?.release()
            if (scale < 1.0f && workingBmp != src) {
                workingBmp.recycle()
            }
        }
    }
}
