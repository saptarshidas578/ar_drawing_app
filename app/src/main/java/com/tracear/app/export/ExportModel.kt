package com.tracear.app.export

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Types of photo capture available to the user.
 */
enum class CaptureType(val label: String, val description: String) {
    WITH_OVERLAY("With Overlay", "Captures what you see on screen with the AR drawing overlay"),
    WITHOUT_OVERLAY("Without Overlay", "Captures clean view of the physical paper and your pencil drawing"),
    RECTIFIED_PAPER("Rectified Scan", "Perspective-corrected, top-down flat scan of the paper")
}

/**
 * Status of the timelapse recording and export pipeline.
 */
enum class TimelapseStatus {
    IDLE,
    RECORDING,
    PAUSED,
    EXPORTING,
    COMPLETED,
    ERROR
}

/**
 * Result data class for captured photo.
 */
data class CaptureResult(
    val uri: Uri?,
    val file: File?,
    val captureType: CaptureType,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String = ""
)

/**
 * Pure mathematical formulas and string formatters for capture & timelapse.
 */
object ExportMath {

    const val DEFAULT_INTERVAL_SECONDS = 5
    const val DEFAULT_PLAYBACK_FPS = 24
    const val MAX_FRAMES_CAP = 600
    const val MAX_STORAGE_BYTES_CAP = 150L * 1024L * 1024L // 150 MB
    const val MIN_STORAGE_FREE_BYTES = 100L * 1024L * 1024L // 100 MB free guard

    fun formatFrameFilename(index: Int): String {
        return "frame_%06d.jpg".format(Locale.US, index.coerceAtLeast(0))
    }

    fun formatPhotoFilename(type: CaptureType): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val suffix = when (type) {
            CaptureType.WITH_OVERLAY -> "overlay"
            CaptureType.WITHOUT_OVERLAY -> "drawing"
            CaptureType.RECTIFIED_PAPER -> "scan"
        }
        return "TraceAR_${suffix}_$stamp.jpg"
    }

    fun formatVideoFilename(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "TraceAR_timelapse_$stamp.mp4"
    }

    fun calculateVideoDurationSeconds(frameCount: Int, fps: Int): Float {
        if (fps <= 0) return 0f
        return frameCount.toFloat() / fps.toFloat()
    }

    fun formatDurationSeconds(totalSeconds: Int): String {
        val s = totalSeconds.coerceAtLeast(0)
        val m = s / 60
        val remS = s % 60
        return "%02d:%02d".format(Locale.US, m, remS)
    }

    fun formatFileSize(bytes: Long): String {
        val b = bytes.coerceAtLeast(0L)
        return when {
            b < 1024L -> "$b B"
            b < 1024L * 1024L -> "%.1f KB".format(Locale.US, b.toDouble() / 1024.0)
            else -> "%.1f MB".format(Locale.US, b.toDouble() / (1024.0 * 1024.0))
        }
    }

    /**
     * H.264 video encoders require width and height to be even numbers (divisible by 2).
     */
    fun ensureEvenDimensions(w: Int, h: Int): Pair<Int, Int> {
        val evenW = ((w / 2) * 2).coerceAtLeast(2)
        val evenH = ((h / 2) * 2).coerceAtLeast(2)
        return Pair(evenW, evenH)
    }

    /**
     * Calculates rectified rectangular dimensions for paper scan given aspect ratio (W / H).
     */
    fun calculateRectifiedDimensions(aspectRatio: Float, maxDimension: Int = 2048): Pair<Int, Int> {
        val clampedAspect = aspectRatio.coerceIn(0.2f, 5.0f)
        val targetW: Int
        val targetH: Int
        if (clampedAspect <= 1.0f) {
            // Portrait
            targetH = maxDimension
            targetW = (maxDimension * clampedAspect).toInt().coerceAtLeast(2)
        } else {
            // Landscape
            targetW = maxDimension
            targetH = (maxDimension / clampedAspect).toInt().coerceAtLeast(2)
        }
        return ensureEvenDimensions(targetW, targetH)
    }

    fun isStorageLow(availableBytes: Long, thresholdBytes: Long = MIN_STORAGE_FREE_BYTES): Boolean {
        return availableBytes < thresholdBytes
    }

    fun canRecordNextFrame(
        currentFrames: Int,
        currentBytes: Long,
        maxFrames: Int = MAX_FRAMES_CAP,
        maxBytes: Long = MAX_STORAGE_BYTES_CAP
    ): Boolean {
        return currentFrames < maxFrames && currentBytes < maxBytes
    }
}

/**
 * Observable UI state for Timelapse recording and export session.
 */
class TimelapseState {

    var isRecording by mutableStateOf(false)
    var isPaused by mutableStateOf(false)

    var intervalSeconds by mutableIntStateOf(ExportMath.DEFAULT_INTERVAL_SECONDS) // 1 to 30
    var playbackFps by mutableIntStateOf(ExportMath.DEFAULT_PLAYBACK_FPS)        // 10 to 30

    var recordedFrameCount by mutableIntStateOf(0)
    var recordedBytes by mutableLongStateOf(0L)
    var recordingStartTimestamp by mutableLongStateOf(0L)

    var status by mutableStateOf(TimelapseStatus.IDLE)
    var exportProgress by mutableFloatStateOf(0f) // 0f to 1f

    var lastExportedUri by mutableStateOf<Uri?>(null)
    var lastExportedFile by mutableStateOf<File?>(null)

    var userMessage by mutableStateOf<String?>(null)

    fun startRecording() {
        isRecording = true
        isPaused = false
        status = TimelapseStatus.RECORDING
        recordingStartTimestamp = System.currentTimeMillis()
        userMessage = null
    }

    fun pauseRecording(reason: String? = null) {
        if (!isRecording) return
        isPaused = true
        status = TimelapseStatus.PAUSED
        if (reason != null) {
            userMessage = reason
        }
    }

    fun resumeRecording() {
        if (!isRecording) return
        isPaused = false
        status = TimelapseStatus.RECORDING
        userMessage = null
    }

    fun stopRecording() {
        isRecording = false
        isPaused = false
        status = if (recordedFrameCount > 0) TimelapseStatus.COMPLETED else TimelapseStatus.IDLE
    }

    fun reset() {
        isRecording = false
        isPaused = false
        recordedFrameCount = 0
        recordedBytes = 0L
        recordingStartTimestamp = 0L
        status = TimelapseStatus.IDLE
        exportProgress = 0f
        lastExportedUri = null
        lastExportedFile = null
        userMessage = null
    }

    val elapsedSeconds: Int
        get() {
            if (recordingStartTimestamp <= 0L) return 0
            return ((System.currentTimeMillis() - recordingStartTimestamp) / 1000L).toInt().coerceAtLeast(0)
        }

    val formattedDuration: String
        get() = ExportMath.formatDurationSeconds(elapsedSeconds)

    val estimatedVideoLengthSeconds: Float
        get() = ExportMath.calculateVideoDurationSeconds(recordedFrameCount, playbackFps)
}
