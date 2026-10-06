package com.tracear.app.ar

import android.util.Log
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.DMatch
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.features2d.BFMatcher
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class PaperTrackingStatus {
    FOLLOWING,
    SEARCHING,
    PAUSED,
    OFF
}

enum class PaperSensitivity(val minConfidence: Float) {
    LOW(0.50f),
    NORMAL(0.65f),
    HIGH(0.80f)
}

data class IntrinsicsData(
    val fx: Float,
    val fy: Float,
    val cx: Float,
    val cy: Float,
    val width: Int,
    val height: Int
)

data class TrackerResult(
    val status: PaperTrackingStatus,
    val paperFrame: PaperFrame,
    val confidence: Float,
    val residualMm: Float,
    val detectedQuad: List<Vector3f>?,
    val predictedQuad: List<Vector3f>?,
    val cycleDurationMs: Long,
    val isTemplateReady: Boolean = false
)

/**
 * PaperTracker — Real-time 2D rigid tracking of paper on the locked table plane.
 *
 * Implements:
 * - Measurement A: Unprojected raw camera paper edge polygon matching (5% side error check)
 * - Measurement B: ORB feature matching with RANSAC against rectified 512px grayscale template
 * - Fusion, deadband (0.5mm, 0.1°), jump gating (3 cycles), and stationary template refresh.
 */
class PaperTracker {

    companion object {
        private const val TAG = "PaperTracker"
        const val TEMPLATE_SIZE = 512
        const val TEMPLATE_MARGIN_RATIO = 0.10f // 10% outer margin
        const val MAX_AGREEMENT_DIST_METERS = 0.015f // 1.5 cm agreement threshold
        const val MAX_AGREEMENT_ROT_DEG = 3.0f        // 3 degrees agreement threshold
    }

    private var baselineCorners: List<Vector3f>? = null
    private var baseCentroid: Vector3f = Vector3f.ZERO
    private var baselineWidthMeters: Float = 0.210f
    private var baselineHeightMeters: Float = 0.297f

    private var templateMat: Mat? = null
    private var templateKeyPoints: MatOfKeyPoint? = null
    private var templateDescriptors: Mat? = null

    private val filter = PaperLockFilter()
    private val orb = ORB.create(500)
    private val matcher = BFMatcher.create(Core.NORM_HAMMING, true)

    var lastResult: TrackerResult = TrackerResult(
        status = PaperTrackingStatus.OFF,
        paperFrame = PaperFrame.IDENTITY,
        confidence = 0f,
        residualMm = 0f,
        detectedQuad = null,
        predictedQuad = null,
        cycleDurationMs = 0L
    )
        private set

    /**
     * Initializes baseline paper corners when user or auto-detection calibrates.
     */
    fun initBaseline(corners: List<Vector3f>) {
        if (corners.size < 4) return
        val sorted = MathUtils.sortCornersClockwise(corners)
        baselineCorners = sorted
        baseCentroid = PaperFrame.calculateCentroid(sorted)

        val topW = (sorted[1] - sorted[0]).length()
        val botW = (sorted[2] - sorted[3]).length()
        val leftH = (sorted[3] - sorted[0]).length()
        val rightH = (sorted[2] - sorted[1]).length()
        baselineWidthMeters = (topW + botW) / 2f
        baselineHeightMeters = (leftH + rightH) / 2f

        filter.reset(PaperFrame.IDENTITY)
        releaseTemplate()

        lastResult = lastResult.copy(
            status = PaperTrackingStatus.SEARCHING,
            paperFrame = PaperFrame.IDENTITY,
            confidence = 1.0f,
            residualMm = 0f,
            detectedQuad = sorted,
            predictedQuad = sorted,
            isTemplateReady = false
        )
    }

    /**
     * Captures initial top-down rectified grayscale template from camera frame.
     */
    fun captureTemplate(
        grayMat: Mat,
        intrinsics: IntrinsicsData,
        camPoseTx: Float, camPoseTy: Float, camPoseTz: Float,
        camQx: Float, camQy: Float, camQz: Float, camQw: Float,
        anchorTx: Float, anchorTy: Float, anchorTz: Float,
        anchorQx: Float, anchorQy: Float, anchorQz: Float, anchorQw: Float
    ): Boolean {
        val base = baselineCorners ?: return false

        // Expand corners by margin ratio
        val marginCorners = expandCorners(base, baseCentroid, TEMPLATE_MARGIN_RATIO)

        // Project margin corners into camera image pixels
        val imgPoints = marginCorners.mapNotNull { localCorner ->
            val worldCorner = localToWorld(anchorTx, anchorTy, anchorTz, anchorQx, anchorQy, anchorQz, anchorQw, localCorner)
            MathUtils.projectWorldPointToCameraPixel(
                worldPoint = worldCorner,
                fx = intrinsics.fx, fy = intrinsics.fy, cx = intrinsics.cx, cy = intrinsics.cy,
                camTx = camPoseTx, camTy = camPoseTy, camTz = camPoseTz,
                camQx = camQx, camQy = camQy, camQz = camQz, camQw = camQw
            )
        }

        if (imgPoints.size != 4) return false

        // Check if all projected points are within image bounds
        val inBounds = imgPoints.all { (u, v) ->
            u >= 0f && u < intrinsics.width && v >= 0f && v < intrinsics.height
        }
        if (!inBounds) return false

        val srcPoints = MatOfPoint2f(
            Point(imgPoints[0].first.toDouble(), imgPoints[0].second.toDouble()),
            Point(imgPoints[1].first.toDouble(), imgPoints[1].second.toDouble()),
            Point(imgPoints[2].first.toDouble(), imgPoints[2].second.toDouble()),
            Point(imgPoints[3].first.toDouble(), imgPoints[3].second.toDouble())
        )

        val dstPoints = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(TEMPLATE_SIZE.toDouble(), 0.0),
            Point(TEMPLATE_SIZE.toDouble(), TEMPLATE_SIZE.toDouble()),
            Point(0.0, 0.0 + TEMPLATE_SIZE.toDouble())
        )

        val h = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
        val warped = Mat()
        Imgproc.warpPerspective(grayMat, warped, h, Size(TEMPLATE_SIZE.toDouble(), TEMPLATE_SIZE.toDouble()))

        releaseTemplate()
        templateMat = warped

        val kp = MatOfKeyPoint()
        val desc = Mat()
        orb.detectAndCompute(warped, Mat(), kp, desc)
        templateKeyPoints = kp
        templateDescriptors = desc

        srcPoints.release()
        dstPoints.release()
        h.release()

        val ready = desc.rows() >= 15
        lastResult = lastResult.copy(isTemplateReady = ready)
        return ready
    }

    /**
     * Executes one tracking cycle (5 to 10 Hz) on background thread.
     */
    fun processCycle(
        grayMat: Mat,
        intrinsics: IntrinsicsData,
        camPoseTx: Float, camPoseTy: Float, camPoseTz: Float,
        camQx: Float, camQy: Float, camQz: Float, camQw: Float,
        anchorTx: Float, anchorTy: Float, anchorTz: Float,
        anchorQx: Float, anchorQy: Float, anchorQz: Float, anchorQw: Float,
        planePoint: Vector3f,
        planeNormal: Vector3f,
        isTracking: Boolean,
        isFrozen: Boolean,
        isEnabled: Boolean,
        sensitivity: PaperSensitivity,
        nowMs: Long = System.currentTimeMillis()
    ): TrackerResult {
        val startTime = System.currentTimeMillis()

        if (!isEnabled) {
            val res = TrackerResult(
                status = PaperTrackingStatus.OFF,
                paperFrame = PaperFrame.IDENTITY,
                confidence = 0f,
                residualMm = 0f,
                detectedQuad = null,
                predictedQuad = baselineCorners,
                cycleDurationMs = 0L
            )
            lastResult = res
            return res
        }

        if (!isTracking || isFrozen || baselineCorners == null) {
            val status = if (isFrozen) PaperTrackingStatus.PAUSED else PaperTrackingStatus.SEARCHING
            val res = lastResult.copy(status = status, cycleDurationMs = System.currentTimeMillis() - startTime)
            lastResult = res
            return res
        }

        val base = baselineCorners!!

        // --- Measurement A: Paper Edge Detection ---
        var measA: RigidResult? = null
        var detectedPlaneCorners: List<Vector3f>? = null

        val detectedImgCorners = findPaperCornersInImage(grayMat)
        if (detectedImgCorners != null && detectedImgCorners.size == 4) {
            // Unproject detected corners to locked table plane in anchor-local coordinates
            val planeCorners = detectedImgCorners.mapNotNull { pt ->
                val ray = MathUtils.unprojectCameraPixelToRay(
                    u = pt.x.toFloat(), v = pt.y.toFloat(),
                    fx = intrinsics.fx, fy = intrinsics.fy, cx = intrinsics.cx, cy = intrinsics.cy,
                    camTx = camPoseTx, camTy = camPoseTy, camTz = camPoseTz,
                    camQx = camQx, camQy = camQy, camQz = camQz, camQw = camQw
                )
                val hitResult = MathUtils.rayPlaneIntersection(ray, planePoint, planeNormal, maxDistance = 2.5f)
                if (hitResult is RayPlaneResult.Hit) {
                    worldToLocal(anchorTx, anchorTy, anchorTz, anchorQx, anchorQy, anchorQz, anchorQw, hitResult.point)
                } else null
            }

            if (planeCorners.size == 4) {
                val sortedDetected = MathUtils.sortCornersClockwise(planeCorners)
                // Check if side lengths match baseline within 5%
                if (RigidTransform2D.validateCornerLengths(base, sortedDetected, RigidTransform2D.MAX_EDGE_SIDE_ERROR)) {
                    val est = RigidTransform2D.estimateFromCorrespondingPoints(base, sortedDetected)
                    if (est != null && est.residualMeters < 0.008f) { // Under 8mm RMS error
                        measA = est
                        detectedPlaneCorners = sortedDetected
                    }
                }
            }
        }

        // --- Measurement B: Template Matching with ORB & RANSAC ---
        var measB: RigidResult? = null
        if (templateMat != null && templateDescriptors != null && !templateDescriptors!!.empty()) {
            val (currentWarped, H) = warpCurrentView(
                grayMat = grayMat,
                paperFrame = filter.currentPose,
                base = base,
                centroid = baseCentroid,
                intrinsics = intrinsics,
                camPoseTx = camPoseTx, camPoseTy = camPoseTy, camPoseTz = camPoseTz,
                camQx = camQx, camQy = camQy, camQz = camQz, camQw = camQw,
                anchorTx = anchorTx, anchorTy = anchorTy, anchorTz = anchorTz,
                anchorQx = anchorQx, anchorQy = anchorQy, anchorQz = anchorQz, anchorQw = anchorQw
            )

            if (currentWarped != null) {
                measB = registerWithTemplate(currentWarped)
                currentWarped.release()
                H?.release()
            }
        }

        // --- Fusion & Gating ---
        var targetPose: PaperFrame? = null
        var confidence = 0f
        var residualMeters = 0f

        if (measA != null && measB != null) {
            val dist = sqrt(
                (measA.paperFrame.dx - measB.paperFrame.dx) * (measA.paperFrame.dx - measB.paperFrame.dx) +
                        (measA.paperFrame.dz - measB.paperFrame.dz) * (measA.paperFrame.dz - measB.paperFrame.dz)
            )
            val rotDiff = abs(measA.paperFrame.rotationDegrees - measB.paperFrame.rotationDegrees)

            if (dist < MAX_AGREEMENT_DIST_METERS && rotDiff < MAX_AGREEMENT_ROT_DEG) {
                // High consensus: prefer A
                targetPose = measA.paperFrame
                confidence = 0.95f
                residualMeters = measA.residualMeters
            } else {
                // Disagree: prefer A if side lengths matched exactly, otherwise hold
                targetPose = measA.paperFrame
                confidence = 0.70f
                residualMeters = measA.residualMeters
            }
        } else if (measA != null) {
            targetPose = measA.paperFrame
            confidence = 0.90f
            residualMeters = measA.residualMeters
        } else if (measB != null && measB.inlierRatio >= sensitivity.minConfidence) {
            targetPose = measB.paperFrame
            confidence = measB.inlierRatio
            residualMeters = measB.residualMeters
        }

        val elapsed = System.currentTimeMillis() - startTime

        if (targetPose != null && confidence >= sensitivity.minConfidence) {
            val smoothed = filter.filter(targetPose, nowMs)
            val predicted = base.map { smoothed.transform(it, baseCentroid) }

            // Template refresh check
            if (filter.shouldRefreshTemplate(confidence, nowMs) && templateMat == null) {
                captureTemplate(
                    grayMat = grayMat, intrinsics = intrinsics,
                    camPoseTx = camPoseTx, camPoseTy = camPoseTy, camPoseTz = camPoseTz,
                    camQx = camQx, camQy = camQy, camQz = camQz, camQw = camQw,
                    anchorTx = anchorTx, anchorTy = anchorTy, anchorTz = anchorTz,
                    anchorQx = anchorQx, anchorQy = anchorQy, anchorQz = anchorQz, anchorQw = anchorQw
                )
            }

            val res = TrackerResult(
                status = PaperTrackingStatus.FOLLOWING,
                paperFrame = smoothed,
                confidence = confidence,
                residualMm = residualMeters * 1000f,
                detectedQuad = detectedPlaneCorners ?: predicted,
                predictedQuad = predicted,
                cycleDurationMs = elapsed,
                isTemplateReady = (templateMat != null)
            )
            lastResult = res
            return res
        } else {
            // Low confidence: hold last smoothed pose
            val current = filter.currentPose
            val predicted = base.map { current.transform(it, baseCentroid) }
            val res = TrackerResult(
                status = PaperTrackingStatus.SEARCHING,
                paperFrame = current,
                confidence = confidence,
                residualMm = residualMeters * 1000f,
                detectedQuad = detectedPlaneCorners,
                predictedQuad = predicted,
                cycleDurationMs = elapsed,
                isTemplateReady = (templateMat != null)
            )
            lastResult = res
            return res
        }
    }

    private fun registerWithTemplate(currentWarped: Mat): RigidResult? {
        val tDesc = templateDescriptors ?: return null
        val tKp = templateKeyPoints ?: return null
        if (tDesc.empty()) return null

        val currKp = MatOfKeyPoint()
        val currDesc = Mat()
        orb.detectAndCompute(currentWarped, Mat(), currKp, currDesc)

        if (currDesc.empty() || currDesc.rows() < 10) {
            currKp.release()
            currDesc.release()
            return null
        }

        val matches = MatOfDMatch()
        matcher.match(tDesc, currDesc, matches)

        val matchList = matches.toList()
        matches.release()

        val goodMatches = matchList.filter { it.distance < 45.0f }
        if (goodMatches.size < 6) {
            currKp.release()
            currDesc.release()
            return null
        }

        val tKpList = tKp.toList()
        val currKpList = currKp.toList()

        // Convert keypoints in 512x512 warped template to paper meters in plane coordinates
        val basePoints = mutableListOf<Vector3f>()
        val currPoints = mutableListOf<Vector3f>()

        val marginW = baselineWidthMeters * (1f + 2f * TEMPLATE_MARGIN_RATIO)
        val marginH = baselineHeightMeters * (1f + 2f * TEMPLATE_MARGIN_RATIO)

        for (m in goodMatches) {
            val tp = tKpList[m.queryIdx].pt
            val cp = currKpList[m.trainIdx].pt

            val bx = ((tp.x / TEMPLATE_SIZE - 0.5f) * marginW).toFloat()
            val bz = ((tp.y / TEMPLATE_SIZE - 0.5f) * marginH).toFloat()

            val cx = ((cp.x / TEMPLATE_SIZE - 0.5f) * marginW).toFloat()
            val cz = ((cp.y / TEMPLATE_SIZE - 0.5f) * marginH).toFloat()

            basePoints.add(Vector3f(bx, 0f, bz))
            currPoints.add(Vector3f(cx, 0f, cz))
        }

        currKp.release()
        currDesc.release()

        val deltaResult = RigidTransform2D.estimateWithRansac(
            baselinePoints = basePoints,
            observedPoints = currPoints,
            maxInlierDistMeters = 0.006f,
            minInlierRatio = 0.35f
        ) ?: return null

        // Add delta to current frame pose
        val currentPose = filter.currentPose
        val combined = PaperFrame(
            dx = currentPose.dx + deltaResult.paperFrame.dx,
            dz = currentPose.dz + deltaResult.paperFrame.dz,
            rotationDegrees = currentPose.rotationDegrees + deltaResult.paperFrame.rotationDegrees
        )

        return deltaResult.copy(paperFrame = combined)
    }

    private fun warpCurrentView(
        grayMat: Mat,
        paperFrame: PaperFrame,
        base: List<Vector3f>,
        centroid: Vector3f,
        intrinsics: IntrinsicsData,
        camPoseTx: Float, camPoseTy: Float, camPoseTz: Float,
        camQx: Float, camQy: Float, camQz: Float, camQw: Float,
        anchorTx: Float, anchorTy: Float, anchorTz: Float,
        anchorQx: Float, anchorQy: Float, anchorQz: Float, anchorQw: Float
    ): Pair<Mat?, Mat?> {
        val marginCorners = expandCorners(base, centroid, TEMPLATE_MARGIN_RATIO)
        val transformedCorners = marginCorners.map { paperFrame.transform(it, centroid) }

        val imgPoints = transformedCorners.mapNotNull { localCorner ->
            val worldCorner = localToWorld(anchorTx, anchorTy, anchorTz, anchorQx, anchorQy, anchorQz, anchorQw, localCorner)
            MathUtils.projectWorldPointToCameraPixel(
                worldPoint = worldCorner,
                fx = intrinsics.fx, fy = intrinsics.fy, cx = intrinsics.cx, cy = intrinsics.cy,
                camTx = camPoseTx, camTy = camPoseTy, camTz = camPoseTz,
                camQx = camQx, camQy = camQy, camQz = camQz, camQw = camQw
            )
        }

        if (imgPoints.size != 4) return Pair(null, null)

        val srcPoints = MatOfPoint2f(
            Point(imgPoints[0].first.toDouble(), imgPoints[0].second.toDouble()),
            Point(imgPoints[1].first.toDouble(), imgPoints[1].second.toDouble()),
            Point(imgPoints[2].first.toDouble(), imgPoints[2].second.toDouble()),
            Point(imgPoints[3].first.toDouble(), imgPoints[3].second.toDouble())
        )

        val dstPoints = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(TEMPLATE_SIZE.toDouble(), 0.0),
            Point(TEMPLATE_SIZE.toDouble(), TEMPLATE_SIZE.toDouble()),
            Point(0.0, TEMPLATE_SIZE.toDouble())
        )

        val h = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
        val warped = Mat()
        Imgproc.warpPerspective(grayMat, warped, h, Size(TEMPLATE_SIZE.toDouble(), TEMPLATE_SIZE.toDouble()))

        srcPoints.release()
        dstPoints.release()

        return Pair(warped, h)
    }

    private fun findPaperCornersInImage(gray: Mat): List<Point>? {
        val small = Mat()
        val scale = 0.5
        Imgproc.resize(gray, small, Size(gray.width() * scale, gray.height() * scale))

        val blurred = Mat()
        val edges = Mat()
        val dilated = Mat()
        try {
            Imgproc.GaussianBlur(small, blurred, Size(5.0, 5.0), 0.0)
            Imgproc.Canny(blurred, edges, 50.0, 150.0)
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
            Imgproc.dilate(edges, dilated, kernel)
            kernel.release()

            val contours = ArrayList<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(dilated, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
            hierarchy.release()

            val minArea = small.width() * small.height() * 0.04
            var bestCorners: List<Point>? = null
            var maxArea = 0.0

            for (c in contours) {
                val area = Imgproc.contourArea(c)
                if (area < minArea || area <= maxArea) continue

                val curve = MatOfPoint2f(*c.toArray())
                val approx = MatOfPoint2f()
                val perimeter = Imgproc.arcLength(curve, true)
                Imgproc.approxPolyDP(curve, approx, 0.02 * perimeter, true)

                val points = approx.toArray()
                curve.release()
                approx.release()

                if (points.size == 4) {
                    val approxInt = MatOfPoint(*points)
                    val isConvex = Imgproc.isContourConvex(approxInt)
                    approxInt.release()

                    if (isConvex) {
                        maxArea = area
                        bestCorners = points.map { Point(it.x / scale, it.y / scale) }
                    }
                }
            }
            contours.forEach { it.release() }
            return bestCorners
        } finally {
            small.release()
            blurred.release()
            edges.release()
            dilated.release()
        }
    }

    private fun expandCorners(corners: List<Vector3f>, centroid: Vector3f, marginRatio: Float): List<Vector3f> {
        return corners.map { p ->
            val dx = p.x - centroid.x
            val dz = p.z - centroid.z
            Vector3f(
                x = centroid.x + dx * (1f + marginRatio),
                y = p.y,
                z = centroid.z + dz * (1f + marginRatio)
            )
        }
    }

    private fun localToWorld(
        anchorTx: Float, anchorTy: Float, anchorTz: Float,
        anchorQx: Float, anchorQy: Float, anchorQz: Float, anchorQw: Float,
        localPoint: Vector3f
    ): Vector3f {
        val rot = MathUtils.rotateVectorByQuaternion(localPoint, anchorQx, anchorQy, anchorQz, anchorQw)
        return Vector3f(anchorTx + rot.x, anchorTy + rot.y, anchorTz + rot.z)
    }

    private fun worldToLocal(
        anchorTx: Float, anchorTy: Float, anchorTz: Float,
        anchorQx: Float, anchorQy: Float, anchorQz: Float, anchorQw: Float,
        worldPoint: Vector3f
    ): Vector3f {
        val diff = Vector3f(worldPoint.x - anchorTx, worldPoint.y - anchorTy, worldPoint.z - anchorTz)
        return MathUtils.rotateVectorByQuaternion(diff, -anchorQx, -anchorQy, -anchorQz, anchorQw)
    }

    fun simulatePaperShift(shiftMetersX: Float = 0.02f, shiftMetersZ: Float = 0.0f) {
        val current = filter.currentPose
        filter.reset(
            pose = PaperFrame(
                dx = current.dx + shiftMetersX,
                dz = current.dz + shiftMetersZ,
                rotationDegrees = current.rotationDegrees
            )
        )
    }

    fun releaseTemplate() {
        templateMat?.release()
        templateMat = null
        templateKeyPoints?.release()
        templateKeyPoints = null
        templateDescriptors?.release()
        templateDescriptors = null
    }

    fun destroy() {
        releaseTemplate()
        matcher.clear()
    }
}
