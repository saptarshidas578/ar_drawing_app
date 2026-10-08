package com.tracear.app.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.provider.MediaStore
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import androidx.compose.ui.geometry.Offset
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

/**
 * Orchestrates photo capture, rectified paper scanning, MediaStore saving,
 * FileProvider system sharing, and timelapse temporary frame lifecycle.
 */
object ExportManager {

    private const val TAG = "ExportManager"
    private const val TIMELAPSE_DIR = "timelapse_frames"

    // ── 1. PixelCopy Capture on SurfaceView ───────────────────

    /**
     * Captures a bitmap directly from the SurfaceView using the hardware-accelerated PixelCopy API.
     */
    suspend fun captureSurface(
        surfaceView: SurfaceView,
        targetWidth: Int? = null,
        targetHeight: Int? = null
    ): Bitmap? = suspendCancellableCoroutine { continuation ->
        val w = targetWidth ?: surfaceView.width
        val h = targetHeight ?: surfaceView.height

        if (w <= 0 || h <= 0) {
            Log.w(TAG, "Cannot capture SurfaceView with invalid dimensions: ${w}x$h")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val handler = Handler(Looper.getMainLooper())

        try {
            PixelCopy.request(
                surfaceView,
                bitmap,
                { result ->
                    if (result == PixelCopy.SUCCESS) {
                        continuation.resume(bitmap)
                    } else {
                        Log.e(TAG, "PixelCopy failed with error code: $result")
                        bitmap.recycle()
                        continuation.resume(null)
                    }
                },
                handler
            )
        } catch (e: Throwable) {
            Log.e(TAG, "PixelCopy request exception: ${e.message}", e)
            bitmap.recycle()
            continuation.resume(null)
        }
    }

    // ── 2. Rectified Paper Scan (Perspective Warp) ────────────

    /**
     * Generates a top-down, rectified flat scan of the paper using OpenCV perspective warp.
     *
     * @param srcBitmap Captured photo containing the paper
     * @param screenCorners Clockwise 4 corners on the screen: TL, TR, BR, BL
     * @param paperAspect Paper width / height ratio (e.g. 0.707 for A4 portrait)
     */
    suspend fun rectifyPaper(
        srcBitmap: Bitmap,
        screenCorners: List<Offset>,
        paperAspect: Float = 210f / 297f
    ): Bitmap? = withContext(Dispatchers.Default) {
        if (screenCorners.size != 4) {
            Log.w(TAG, "Rectify requires exactly 4 corners, got: ${screenCorners.size}")
            return@withContext null
        }

        var srcMat: Mat? = null
        var dstMat: Mat? = null
        var transformMat: Mat? = null
        var srcPoints: MatOfPoint2f? = null
        var dstPoints: MatOfPoint2f? = null

        try {
            val (targetW, targetH) = ExportMath.calculateRectifiedDimensions(paperAspect, maxDimension = 2048)

            srcMat = Mat()
            Utils.bitmapToMat(srcBitmap, srcMat)

            // Source points: TL, TR, BR, BL
            val srcPts = arrayOf(
                Point(screenCorners[0].x.toDouble(), screenCorners[0].y.toDouble()),
                Point(screenCorners[1].x.toDouble(), screenCorners[1].y.toDouble()),
                Point(screenCorners[2].x.toDouble(), screenCorners[2].y.toDouble()),
                Point(screenCorners[3].x.toDouble(), screenCorners[3].y.toDouble())
            )
            srcPoints = MatOfPoint2f(*srcPts)

            // Destination flat quad: (0,0), (W,0), (W,H), (0,H)
            val dstPts = arrayOf(
                Point(0.0, 0.0),
                Point(targetW.toDouble(), 0.0),
                Point(targetW.toDouble(), targetH.toDouble()),
                Point(0.0, targetH.toDouble())
            )
            dstPoints = MatOfPoint2f(*dstPts)

            transformMat = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)

            dstMat = Mat(targetH, targetW, CvType.CV_8UC4)
            Imgproc.warpPerspective(
                srcMat,
                dstMat,
                transformMat,
                Size(targetW.toDouble(), targetH.toDouble()),
                Imgproc.INTER_LINEAR
            )

            val outBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(dstMat, outBitmap)
            return@withContext outBitmap

        } catch (e: Throwable) {
            Log.e(TAG, "Failed to rectify paper scan: ${e.message}", e)
            return@withContext null
        } finally {
            srcMat?.release()
            dstMat?.release()
            transformMat?.release()
            srcPoints?.release()
            dstPoints?.release()
        }
    }

    // ── 3. MediaStore Export (Gallery) ────────────────────────

    /**
     * Saves a photo to the device gallery using modern MediaStore (no storage permissions needed).
     */
    suspend fun savePhotoToGallery(
        context: Context,
        bitmap: Bitmap,
        filename: String
    ): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TraceAR")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null

        try {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            Log.i(TAG, "Photo saved to gallery: $uri")
            return@withContext uri
        } catch (e: Throwable) {
            Log.e(TAG, "Error saving photo to gallery: ${e.message}", e)
            resolver.delete(uri, null, null)
            return@withContext null
        }
    }

    /**
     * Saves an MP4 video to the device gallery under Movies/TraceAR.
     */
    suspend fun saveVideoToGallery(
        context: Context,
        videoFile: File,
        filename: String
    ): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, filename)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/TraceAR")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null

        try {
            resolver.openOutputStream(uri)?.use { out ->
                videoFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            Log.i(TAG, "Video saved to gallery: $uri")
            return@withContext uri
        } catch (e: Throwable) {
            Log.e(TAG, "Error saving video to gallery: ${e.message}", e)
            resolver.delete(uri, null, null)
            return@withContext null
        }
    }

    // ── 4. System Share Sheet via FileProvider ────────────────

    fun shareUri(context: Context, uri: Uri, mimeType: String, title: String = "Share Tracing") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String = "Share Tracing") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        shareUri(context, uri, mimeType, title)
    }

    // ── 5. Temporary Timelapse Frames Lifecycle ───────────────

    fun getTimelapseDir(context: Context): File {
        val dir = File(context.cacheDir, TIMELAPSE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun saveTimelapseFrame(
        context: Context,
        bitmap: Bitmap,
        frameIndex: Int
    ): File? = withContext(Dispatchers.IO) {
        val dir = getTimelapseDir(context)
        val filename = ExportMath.formatFrameFilename(frameIndex)
        val file = File(dir, filename)

        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            return@withContext file
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to save timelapse frame: ${e.message}", e)
            return@withContext null
        }
    }

    fun getTimelapseFrameFiles(context: Context): List<File> {
        val dir = getTimelapseDir(context)
        return dir.listFiles { f -> f.isFile && f.name.startsWith("frame_") && f.name.endsWith(".jpg") }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    fun getTimelapseStorageUsage(context: Context): Long {
        return getTimelapseFrameFiles(context).sumOf { it.length() }
    }

    fun getAvailableStorageBytes(context: Context): Long {
        return try {
            val stat = StatFs(context.cacheDir.absolutePath)
            stat.availableBytes
        } catch (e: Throwable) {
            Long.MAX_VALUE
        }
    }

    suspend fun cleanupTimelapseFrames(context: Context) = withContext(Dispatchers.IO) {
        val dir = getTimelapseDir(context)
        dir.listFiles()?.forEach { file ->
            try { file.delete() } catch (ignored: Throwable) {}
        }
        Log.i(TAG, "Cleaned up temporary timelapse frames")
    }
}
