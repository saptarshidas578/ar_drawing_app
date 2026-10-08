package com.tracear.app.ar

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max

/**
 * Result of OpenCV tonal segmentation.
 */
data class TonalSegmentationResult(
    val width: Int,
    val height: Int,
    val toneCount: Int,
    val smoothingLevel: Float,
    val toneMap: ByteArray,
    val outlineEdges: ByteArray,
    val durationMs: Long
)

/**
 * TonalProcessor — OpenCV-based background processor for tonal layer segmentation,
 * edge preservation, and ultra-fast composite rendering.
 */
object TonalProcessor {

    private const val TAG = "TonalProcessor"
    private const val MAX_PROCESSING_DIMENSION = 1536

    // In-memory LRU cache storing recent segmentation maps to make adjustments instant
    private val segmentationCache = object : LruCache<String, TonalSegmentationResult>(4) {}

    /**
     * Performs bilateral smoothing and luminance quantization on a background thread.
     */
    suspend fun processTonalSegmentation(
        src: Bitmap,
        toneCount: Int,
        smoothingLevel: Float
    ): TonalSegmentationResult? = withContext(Dispatchers.Default) {
        val clampedCount = toneCount.coerceIn(2, 6)
        val clampedSmoothing = smoothingLevel.coerceIn(0.1f, 1.0f)

        val cacheKey = "${src.hashCode()}_${src.width}x${src.height}_${clampedCount}_${(clampedSmoothing * 100).toInt()}"
        segmentationCache.get(cacheKey)?.let {
            return@withContext it
        }

        val startTime = System.currentTimeMillis()

        // 1. Scale down if larger than MAX_PROCESSING_DIMENSION to protect memory
        val maxDim = max(src.width, src.height)
        val scale = if (maxDim > MAX_PROCESSING_DIMENSION) {
            MAX_PROCESSING_DIMENSION.toFloat() / maxDim
        } else {
            1.0f
        }
        val targetW = (src.width * scale).toInt().coerceAtLeast(1)
        val targetH = (src.height * scale).toInt().coerceAtLeast(1)

        val workingBmp = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(src, targetW, targetH, true)
        } else {
            src
        }

        var rgbaMat: Mat? = null
        var grayMat: Mat? = null
        var smoothedMat: Mat? = null
        var edgesMat: Mat? = null

        try {
            rgbaMat = Mat()
            Utils.bitmapToMat(workingBmp, rgbaMat)

            grayMat = Mat()
            Imgproc.cvtColor(rgbaMat, grayMat, Imgproc.COLOR_RGBA2GRAY)

            // 2. Edge-preserving bilateral filter
            // Keeps face contours/sharp edges while smoothing away paper texture/noise into clean planes
            val d = 9
            val sigma = (35.0 + clampedSmoothing * 65.0) // 41.5 .. 100.0
            smoothedMat = Mat()
            Imgproc.bilateralFilter(grayMat, smoothedMat, d, sigma, sigma)

            // 3. Extract outline layer edges via Canny on the smoothed mat
            edgesMat = Mat()
            Imgproc.Canny(smoothedMat, edgesMat, 50.0, 130.0)

            val w = smoothedMat.width()
            val h = smoothedMat.height()
            val totalPixels = w * h

            val grayBytes = ByteArray(totalPixels)
            smoothedMat.get(0, 0, grayBytes)

            val outlineBytes = ByteArray(totalPixels)
            edgesMat.get(0, 0, outlineBytes)

            // 4. Quantize luminance into N tone bins
            val toneMap = ByteArray(totalPixels)
            for (i in 0 until totalPixels) {
                val lum = grayBytes[i].toInt() and 0xFF
                val toneIndex = TonalMath.quantizeLuminance(lum, clampedCount)
                toneMap[i] = toneIndex.toByte()
            }

            val elapsed = System.currentTimeMillis() - startTime
            val result = TonalSegmentationResult(
                width = w,
                height = h,
                toneCount = clampedCount,
                smoothingLevel = clampedSmoothing,
                toneMap = toneMap,
                outlineEdges = outlineBytes,
                durationMs = elapsed
            )

            segmentationCache.put(cacheKey, result)
            Log.d(TAG, "processTonalSegmentation finished in ${elapsed}ms for ${w}x$h ($clampedCount tones)")
            return@withContext result

        } catch (e: Throwable) {
            Log.e(TAG, "processTonalSegmentation error: ${e.message}", e)
            return@withContext null
        } finally {
            rgbaMat?.release()
            grayMat?.release()
            smoothedMat?.release()
            edgesMat?.release()
            if (scale < 1.0f && workingBmp != src) {
                workingBmp.recycle()
            }
        }
    }

    /**
     * Composites active tonal layers and outline layer into a single ARGB_8888 bitmap.
     * Operates purely in-memory (< 15 ms).
     */
    fun compositeLayers(
        segmentation: TonalSegmentationResult,
        layers: List<TonalLayerConfig>,
        isOutlineVisible: Boolean,
        outlineColorArgb: Int,
        outlineOpacity: Float,
        soloLayerId: Int?,
        isStagesMode: Boolean,
        currentStage: Int
    ): Bitmap {
        val w = segmentation.width
        val h = segmentation.height
        val total = w * h
        val outPixels = IntArray(total)

        // Precompute ARGB values for each tone
        val precomputedToneColors = IntArray(segmentation.toneCount)
        for (k in 0 until segmentation.toneCount) {
            val isActive = when {
                isStagesMode -> currentStage == (k + 1)
                soloLayerId != null -> soloLayerId == k
                else -> layers.getOrNull(k)?.isVisible ?: true
            }

            if (isActive) {
                val cfg = layers.getOrNull(k)
                val c = cfg?.colorArgb ?: 0xFF888888.toInt()
                val a = ((cfg?.opacity ?: 0.7f).coerceIn(0.05f, 1.0f) * 255).toInt()
                precomputedToneColors[k] = AndroidColor.argb(a, AndroidColor.red(c), AndroidColor.green(c), AndroidColor.blue(c))
            } else {
                precomputedToneColors[k] = 0 // transparent
            }
        }

        // Precompute outline layer color
        val isOutlineActive = when {
            isStagesMode -> currentStage == 0
            soloLayerId != null -> soloLayerId == -1
            else -> isOutlineVisible
        }
        val outlineAlpha = (outlineOpacity.coerceIn(0.05f, 1.0f) * 255).toInt()
        val outlineArgb = AndroidColor.argb(
            outlineAlpha,
            AndroidColor.red(outlineColorArgb),
            AndroidColor.green(outlineColorArgb),
            AndroidColor.blue(outlineColorArgb)
        )

        val toneMap = segmentation.toneMap
        val outlineEdges = segmentation.outlineEdges

        for (i in 0 until total) {
            val isEdge = isOutlineActive && ((outlineEdges[i].toInt() and 0xFF) > 0)
            if (isEdge) {
                outPixels[i] = outlineArgb
            } else {
                val toneIdx = toneMap[i].toInt() and 0xFF
                outPixels[i] = if (toneIdx < precomputedToneColors.size) precomputedToneColors[toneIdx] else 0
            }
        }

        val outBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(outPixels, 0, w, 0, 0, w, h)
        return outBitmap
    }

    /**
     * Samples a pixel from the source reference image at normalized (u, v) coordinates.
     */
    fun samplePixel(src: Bitmap, u: Float, v: Float, toneCount: Int): ValueInspectionResult {
        val clampedU = u.coerceIn(0.0f, 1.0f)
        val clampedV = v.coerceIn(0.0f, 1.0f)
        val px = (clampedU * (src.width - 1)).toInt().coerceIn(0, src.width - 1)
        val py = (clampedV * (src.height - 1)).toInt().coerceIn(0, src.height - 1)

        val color = src.getPixel(px, py)
        val r = AndroidColor.red(color)
        val g = AndroidColor.green(color)
        val b = AndroidColor.blue(color)

        val lum = TonalMath.computeLuminance(r, g, b)
        val pct = ((lum / 255f) * 100).toInt()
        val toneIdx = TonalMath.quantizeLuminance(lum, toneCount)
        val hex = TonalMath.toHex(color)
        val toneName = TonalMath.getToneName(toneIdx, toneCount)

        return ValueInspectionResult(
            x = px,
            y = py,
            u = clampedU,
            v = clampedV,
            colorArgb = color,
            hexCode = hex,
            brightnessPercent = pct,
            rawLuminance = lum,
            toneIndex = toneIdx,
            toneName = toneName
        )
    }

    /**
     * Clears cached segmentation data.
     */
    fun clearCache() {
        segmentationCache.evictAll()
    }
}
