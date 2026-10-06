package com.tracear.app.ar

import android.media.Image
import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.NotYetAvailableException
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Data class representing the result of paper detection.
 * @param screenCorners 4 corner points in screen view pixels (TL, TR, BR, BL).
 * @param isStable True when the detected quadrilateral has been stable for 10+ frames.
 */
data class DetectedPaper(
    val screenCorners: List<Offset>,
    val isStable: Boolean
)

/**
 * PaperDetector — uses OpenCV to detect white sheets of paper from ARCore camera frames.
 *
 * Algorithm:
 * 1. Read the grayscale (Y) channel directly from camera image.
 * 2. Downscale for 60fps performance.
 * 3. Blur, Canny edge detection, and morphological dilation.
 * 4. Find largest convex 4-corner polygon with plausible paper aspect ratio.
 * 5. Temporal stability filter: requires 10 consecutive stable frames before showing.
 * 6. Maps detected corners to screen view coordinates using ARCore coordinate transformation.
 */
class PaperDetector {

    companion object {
        private const val TAG = "TraceAR"
        private const val REQUIRED_STABLE_FRAMES = 10
        private const val MAX_CORNER_DRIFT_PX = 15f
    }

    private var stableFrameCount = 0
    private var lastRawCorners: List<Point>? = null
    var latestDetection: DetectedPaper? = null
        private set

    /**
     * Process an ARCore camera frame to detect paper outlines.
     * Safe to call every frame: handles buffer lifecycle and exceptions automatically.
     */
    fun processFrame(frame: Frame, viewportWidth: Int, viewportHeight: Int): DetectedPaper? {
        var image: Image? = null
        try {
            image = frame.acquireCameraImage()
            val yPlane = image.planes[0]
            val width = image.width
            val height = image.height
            val rowStride = yPlane.rowStride

            val yBuffer = yPlane.buffer
            val yBytes = ByteArray(yBuffer.remaining())
            yBuffer.get(yBytes)

            // Wrap Y-plane in an OpenCV Mat (CV_8UC1 is 8-bit single-channel grayscale)
            val fullMat = Mat(height, rowStride, CvType.CV_8UC1)
            fullMat.put(0, 0, yBytes)

            // Crop to actual image width if rowStride has padding
            val grayMat = if (rowStride > width) fullMat.submat(0, height, 0, width) else fullMat

            // Downscale by 2x for fast edge processing
            val scale = 0.5
            val smallWidth = (width * scale).toInt()
            val smallHeight = (height * scale).toInt()
            val smallMat = Mat()
            Imgproc.resize(grayMat, smallMat, Size(smallWidth.toDouble(), smallHeight.toDouble()))

            // Detect 4-corner paper polygon on downscaled image
            val detectedSmallPoints = findPaperCorners(smallMat)

            // Release temporary Mats
            fullMat.release()
            if (rowStride > width) grayMat.release()
            smallMat.release()

            if (detectedSmallPoints == null) {
                stableFrameCount = 0
                lastRawCorners = null
                latestDetection = null
                return null
            }

            // Scale corner points back to full camera resolution
            val fullCameraPoints = detectedSmallPoints.map {
                Point(it.x / scale, it.y / scale)
            }

            // Check temporal stability across consecutive frames
            val prevCorners = lastRawCorners
            if (prevCorners != null && prevCorners.size == 4) {
                var maxDrift = 0.0
                for (i in 0 until 4) {
                    val dx = fullCameraPoints[i].x - prevCorners[i].x
                    val dy = fullCameraPoints[i].y - prevCorners[i].y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist > maxDrift) maxDrift = dist
                }

                if (maxDrift < MAX_CORNER_DRIFT_PX) {
                    stableFrameCount++
                } else {
                    stableFrameCount = 0
                }
            } else {
                stableFrameCount = 1
            }
            lastRawCorners = fullCameraPoints

            val isStable = stableFrameCount >= REQUIRED_STABLE_FRAMES

            // Transform camera image coordinates into screen view coordinates
            val inCoords = FloatArray(8)
            for (i in 0 until 4) {
                inCoords[i * 2] = fullCameraPoints[i].x.toFloat()
                inCoords[i * 2 + 1] = fullCameraPoints[i].y.toFloat()
            }

            val outCoords = FloatArray(8)
            frame.transformCoordinates2d(
                Coordinates2d.IMAGE_PIXELS,
                inCoords,
                Coordinates2d.VIEW,
                outCoords
            )

            val screenCorners = listOf(
                Offset(outCoords[0], outCoords[1]),
                Offset(outCoords[2], outCoords[3]),
                Offset(outCoords[4], outCoords[5]),
                Offset(outCoords[6], outCoords[7])
            )

            val result = DetectedPaper(screenCorners = screenCorners, isStable = isStable)
            latestDetection = result
            return result

        } catch (e: NotYetAvailableException) {
            // Camera image not ready yet this frame
            return latestDetection
        } catch (e: Throwable) {
            Log.w(TAG, "Paper detection error: ${e.message}")
            return null
        } finally {
            // CRITICAL: Always close the acquired image to prevent camera buffer pool starvation!
            try {
                image?.close()
            } catch (ignored: Throwable) {}
        }
    }

    /**
     * Finds the largest convex 4-corner paper contour in a grayscale image.
     */
    private fun findPaperCorners(gray: Mat): List<Point>? {
        val blurred = Mat()
        val edges = Mat()
        val dilated = Mat()

        try {
            // 1. Smooth out noise
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)

            // 2. Canny edge detector
            Imgproc.Canny(blurred, edges, 50.0, 150.0)

            // 3. Dilate edges slightly to close small gaps in paper border
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
            Imgproc.dilate(edges, dilated, kernel)

            // 4. Find contours
            val contours = ArrayList<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(dilated, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
            hierarchy.release()
            kernel.release()

            val minArea = gray.width() * gray.height() * 0.05 // At least 5% of screen
            var bestCorners: List<Point>? = null
            var maxArea = 0.0

            for (contour in contours) {
                val area = Imgproc.contourArea(contour)
                if (area < minArea || area <= maxArea) continue

                val curve = MatOfPoint2f(*contour.toArray())
                val approx = MatOfPoint2f()
                val perimeter = Imgproc.arcLength(curve, true)

                // Approximate polygon with 2% tolerance
                Imgproc.approxPolyDP(curve, approx, 0.02 * perimeter, true)

                val points = approx.toArray()
                curve.release()
                approx.release()

                if (points.size == 4) {
                    val approxInt = MatOfPoint(*points)
                    val isConvex = Imgproc.isContourConvex(approxInt)
                    approxInt.release()

                    if (isConvex) {
                        // Check side lengths & aspect ratio
                        val d01 = distance(points[0], points[1])
                        val d12 = distance(points[1], points[2])
                        val d23 = distance(points[2], points[3])
                        val d30 = distance(points[3], points[0])

                        val avgW = (d01 + d23) / 2.0
                        val avgH = (d12 + d30) / 2.0
                        if (avgW > 10.0 && avgH > 10.0) {
                            val aspect = max(avgW, avgH) / min(avgW, avgH)
                            // Standard paper aspect ratios in perspective are between 1.05 and 2.0
                            if (aspect in 1.05..2.10) {
                                maxArea = area
                                bestCorners = points.toList()
                            }
                        }
                    }
                }
            }

            contours.forEach { it.release() }
            return bestCorners
        } finally {
            blurred.release()
            edges.release()
            dilated.release()
        }
    }

    private fun distance(p1: Point, p2: Point): Double {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }

    fun reset() {
        stableFrameCount = 0
        lastRawCorners = null
        latestDetection = null
    }
}
