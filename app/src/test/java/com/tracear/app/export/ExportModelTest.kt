package com.tracear.app.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportModelTest {

    @Test
    fun testFrameFilenameFormatting() {
        assertEquals("frame_000000.jpg", ExportMath.formatFrameFilename(0))
        assertEquals("frame_000001.jpg", ExportMath.formatFrameFilename(1))
        assertEquals("frame_000125.jpg", ExportMath.formatFrameFilename(125))
        assertEquals("frame_000000.jpg", ExportMath.formatFrameFilename(-5)) // Negative clamped to 0
    }

    @Test
    fun testPhotoAndVideoFilenames() {
        val overlayName = ExportMath.formatPhotoFilename(CaptureType.WITH_OVERLAY)
        assertTrue(overlayName.startsWith("TraceAR_overlay_"))
        assertTrue(overlayName.endsWith(".jpg"))

        val drawingName = ExportMath.formatPhotoFilename(CaptureType.WITHOUT_OVERLAY)
        assertTrue(drawingName.startsWith("TraceAR_drawing_"))
        assertTrue(drawingName.endsWith(".jpg"))

        val scanName = ExportMath.formatPhotoFilename(CaptureType.RECTIFIED_PAPER)
        assertTrue(scanName.startsWith("TraceAR_scan_"))
        assertTrue(scanName.endsWith(".jpg"))

        val videoName = ExportMath.formatVideoFilename()
        assertTrue(videoName.startsWith("TraceAR_timelapse_"))
        assertTrue(videoName.endsWith(".mp4"))
    }

    @Test
    fun testDurationFormatting() {
        assertEquals("00:00", ExportMath.formatDurationSeconds(0))
        assertEquals("00:45", ExportMath.formatDurationSeconds(45))
        assertEquals("01:05", ExportMath.formatDurationSeconds(65))
        assertEquals("10:30", ExportMath.formatDurationSeconds(630))
    }

    @Test
    fun testVideoDurationCalculation() {
        // 240 frames at 24 fps = 10.0 seconds
        assertEquals(10.0f, ExportMath.calculateVideoDurationSeconds(240, 24), 1e-4f)
        // 120 frames at 30 fps = 4.0 seconds
        assertEquals(4.0f, ExportMath.calculateVideoDurationSeconds(120, 30), 1e-4f)
        // 0 frames
        assertEquals(0.0f, ExportMath.calculateVideoDurationSeconds(0, 24), 1e-4f)
    }

    @Test
    fun testFileSizeFormatting() {
        assertEquals("500 B", ExportMath.formatFileSize(500L))
        assertEquals("1.5 KB", ExportMath.formatFileSize(1536L))
        assertEquals("10.0 MB", ExportMath.formatFileSize(10L * 1024L * 1024L))
    }

    @Test
    fun testEvenDimensionsConstraint() {
        val (w1, h1) = ExportMath.ensureEvenDimensions(1081, 1920)
        assertEquals(1080, w1)
        assertEquals(1920, h1)

        val (w2, h2) = ExportMath.ensureEvenDimensions(721, 1281)
        assertEquals(720, w2)
        assertEquals(1280, h2)
    }

    @Test
    fun testRectifiedDimensions() {
        // A4 portrait ratio: 210 / 297 ~ 0.707
        val (w, h) = ExportMath.calculateRectifiedDimensions(210f / 297f, maxDimension = 2048)
        assertEquals(2048, h)
        assertEquals(0, w % 2) // Must be even
        assertTrue(w > 1400 && w < 1500) // ~1448 px
    }

    @Test
    fun testStorageCapsAndLowStorage() {
        // Less than 100MB free
        assertTrue(ExportMath.isStorageLow(50L * 1024L * 1024L))
        // More than 100MB free
        assertFalse(ExportMath.isStorageLow(200L * 1024L * 1024L))

        // Frame cap tests
        assertTrue(ExportMath.canRecordNextFrame(currentFrames = 100, currentBytes = 10L * 1024L * 1024L))
        assertFalse(ExportMath.canRecordNextFrame(currentFrames = 600, currentBytes = 10L * 1024L * 1024L))
        assertFalse(ExportMath.canRecordNextFrame(currentFrames = 10, currentBytes = 160L * 1024L * 1024L))
    }

    @Test
    fun testTimelapseStateLifecycle() {
        val state = TimelapseState().apply {
            intervalSeconds = 3
            playbackFps = 30
        }

        assertEquals(TimelapseStatus.IDLE, state.status)
        assertFalse(state.isRecording)

        state.startRecording()
        assertTrue(state.isRecording)
        assertFalse(state.isPaused)
        assertEquals(TimelapseStatus.RECORDING, state.status)

        state.pauseRecording("App minimized")
        assertTrue(state.isRecording)
        assertTrue(state.isPaused)
        assertEquals(TimelapseStatus.PAUSED, state.status)
        assertEquals("App minimized", state.userMessage)

        state.resumeRecording()
        assertTrue(state.isRecording)
        assertFalse(state.isPaused)
        assertEquals(TimelapseStatus.RECORDING, state.status)
        assertEquals(null, state.userMessage)

        state.recordedFrameCount = 60
        state.stopRecording()
        assertFalse(state.isRecording)
        assertEquals(TimelapseStatus.COMPLETED, state.status)
        assertEquals(2.0f, state.estimatedVideoLengthSeconds, 1e-4f)

        state.reset()
        assertEquals(TimelapseStatus.IDLE, state.status)
        assertEquals(0, state.recordedFrameCount)
    }
}
