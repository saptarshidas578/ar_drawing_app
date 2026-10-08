package com.tracear.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.tracear.app.data.AppSettings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.media.Image
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.tracear.app.ar.CalibrationState
import com.tracear.app.ar.DetectedPaper
import com.tracear.app.ar.FitMode
import com.tracear.app.ar.GridState
import com.tracear.app.ar.GuidesState
import com.tracear.app.ar.ImageAdjustments
import com.tracear.app.ar.ImageLineExtractor
import com.tracear.app.ar.LineColorOption
import com.tracear.app.ar.LinesOnlyState
import com.tracear.app.ar.MathUtils
import com.tracear.app.ar.PaperBannerType
import com.tracear.app.ar.PaperDetector
import com.tracear.app.ar.PaperLockState
import com.tracear.app.ar.PaperMath
import com.tracear.app.ar.PaperPreset
import com.tracear.app.ar.PaperSensitivity
import com.tracear.app.ar.PaperTracker
import com.tracear.app.ar.PaperTrackingStatus
import com.tracear.app.ar.PoseFilter
import com.tracear.app.ar.RayPlaneResult
import com.tracear.app.ar.RigidTransform2D
import com.tracear.app.ar.ScanQuality
import com.tracear.app.ar.SmoothingMode
import com.tracear.app.ar.SurfaceState
import com.tracear.app.ar.TransformSnapshot
import com.tracear.app.ar.TransformUndoManager
import com.tracear.app.ar.Vector3f
import com.tracear.app.ar.IntrinsicsData
import com.tracear.app.ar.TonalState
import com.tracear.app.ar.TonalProcessor
import com.tracear.app.ar.TonalSegmentationResult
import com.tracear.app.ar.createTracingOverlayNode
import org.opencv.core.CvType
import org.opencv.core.Mat
import com.tracear.app.data.NormalizedCrop
import com.tracear.app.data.NormalizedTransform
import com.tracear.app.data.ProjectData
import com.tracear.app.data.ProjectRepository
import com.tracear.app.ui.tracing.CropDialog
import com.tracear.app.ui.tracing.DockCategory
import com.tracear.app.ui.tracing.FocusModeRestoreButton
import com.tracear.app.ui.tracing.TracingCategorySheet
import com.tracear.app.ui.tracing.TracingDock
import com.tracear.app.ui.tracing.TracingQuickActions
import com.tracear.app.ui.tracing.TracingTopBar
import com.tracear.app.ui.tracing.TracingUiState
import com.tracear.app.export.CaptureResult
import com.tracear.app.export.CaptureType
import com.tracear.app.export.ExportManager
import com.tracear.app.export.ExportMath
import com.tracear.app.export.TimelapseEncoder
import com.tracear.app.export.TimelapseState
import com.tracear.app.export.TimelapseStatus
import com.tracear.app.ui.tracing.ExportDialog
import com.tracear.app.ui.tracing.TimelapseRecordingChip
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.collision.Vector3
import io.github.sceneview.node.CubeNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * ARScreen — handles surface selection with floor rejection, infinite-plane corner raycasting,
 * OpenCV paper auto-detection, view zoom, section grid, lines-only rendering, and project persistence.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ARScreen(
    project: ProjectData? = null,
    imageUri: Uri? = null,
    settings: AppSettings? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenTutorial: (() -> Unit)? = null,
    onChangeImage: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ProjectRepository(context) }
    val scope = rememberCoroutineScope()
    var currentProject by remember { mutableStateOf(project) }

    // --- ARCore Availability Check ---
    val arCoreAvailability = remember {
        try {
            com.google.ar.core.ArCoreApk.getInstance().checkAvailability(context)
        } catch (e: Throwable) {
            com.google.ar.core.ArCoreApk.Availability.SUPPORTED_INSTALLED
        }
    }

    if (arCoreAvailability == com.google.ar.core.ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
        ARCoreUnavailableScreen(isInstallRequired = false, onBack = onBack)
        return
    }
    if (arCoreAvailability == com.google.ar.core.ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED) {
        ARCoreUnavailableScreen(isInstallRequired = true, onBack = onBack)
        return
    }

    // --- Camera Permission ---
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    var isCameraPermissionRevoked by remember { mutableStateOf(false) }
    var arSessionErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    if (!cameraPermission.status.isGranted || isCameraPermissionRevoked) {
        PermissionScreen(
            isPermanentlyDenied = cameraPermission.status.shouldShowRationale,
            onRequestPermission = {
                isCameraPermissionRevoked = false
                cameraPermission.launchPermissionRequest()
            },
            onBack = onBack
        )
        return
    }

    // --- Screen Keep-On & Dimming Management ---
    val activity = LocalContext.current as? android.app.Activity
    DisposableEffect(settings?.isKeepScreenOn) {
        val keepOn = settings?.isKeepScreenOn ?: true
        if (keepOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val window = activity?.window
            if (window != null) {
                val lp = window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
        }
    }

    // --- Light & Comfort (Torch & Screen Brightness) ---
    var arSessionRef by remember { mutableStateOf<com.google.ar.core.Session?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isTorchSupported by remember { mutableStateOf(false) }
    var isFullBrightness by remember { mutableStateOf(false) }

    fun setFullBrightness(enabled: Boolean) {
        isFullBrightness = enabled
        val window = activity?.window ?: return
        val lp = window.attributes
        lp.screenBrightness = if (enabled) 1.0f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = lp
    }

    fun toggleTorch() {
        val session = arSessionRef ?: return
        try {
            val testConfig = session.config
            testConfig.flashMode = Config.FlashMode.TORCH
            if (!session.isSupported(testConfig)) {
                Toast.makeText(context, "Flash/torch is not supported on this camera", Toast.LENGTH_SHORT).show()
                return
            }
            val nextState = !isTorchOn
            val config = session.config
            config.flashMode = if (nextState) Config.FlashMode.TORCH else Config.FlashMode.OFF
            session.configure(config)
            isTorchOn = nextState
        } catch (e: Throwable) {
            Toast.makeText(context, "Unable to switch torch: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // --- State Holders ---
    val surfaceState = remember { SurfaceState() }
    val calibration = remember { CalibrationState() }
    val zoomState = remember { ZoomState() }
    val gridState = remember {
        GridState().apply {
            if (currentProject != null) {
                isEnabled = currentProject!!.gridEnabled
                cols = currentProject!!.gridCols
                rows = currentProject!!.gridRows
                doneCells.clear()
                doneCells.addAll(currentProject!!.doneCells)
            } else if (settings != null) {
                cols = settings.defaultGridSize
                rows = settings.defaultGridSize
            }
        }
    }
    val linesOnlyState = remember {
        LinesOnlyState().apply {
            if (currentProject != null) {
                isEnabled = currentProject!!.isLinesOnly
                edgeSensitivity = currentProject!!.edgeSensitivity
                lineThickness = currentProject!!.lineThickness
                selectedColorOption = LineColorOption.entries.find { it.name == currentProject!!.lineColorName } ?: LineColorOption.CYAN
            } else if (settings != null) {
                selectedColorOption = settings.defaultLineColor
            }
        }
    }
    val guidesState = remember {
        GuidesState().apply {
            if (currentProject != null) {
                applyData(currentProject!!.guides)
            }
        }
    }
    val tonalState = remember {
        TonalState().apply {
            if (currentProject != null) {
                applyData(currentProject!!.tones)
            }
        }
    }
    var tonalSegmentation by remember { mutableStateOf<TonalSegmentationResult?>(null) }
    val paperDetector = remember { PaperDetector() }
    val paperLockState = remember { PaperLockState(context) }
    val paperTracker = remember { PaperTracker() }
    val isProcessingTrackerCycle = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager }
    var lastPaperDetectionTime by remember { mutableLongStateOf(0L) }
    var sceneViewRef by remember { mutableStateOf<ARSceneView?>(null) }

    // --- Export, Photo Capture & Timelapse State ---
    var isExportDialogOpen by remember { mutableStateOf(false) }
    val timelapseState = remember { TimelapseState() }
    var lastCaptureResult by remember { mutableStateOf<CaptureResult?>(null) }
    var isCapturingPhoto by remember { mutableStateOf(false) }
    val timelapseCancelFlag = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    var isLowStorage by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    if (isTorchOn) {
                        try {
                            arSessionRef?.let { session ->
                                val config = session.config
                                config.flashMode = Config.FlashMode.OFF
                                session.configure(config)
                            }
                        } catch (ignored: Throwable) {}
                        isTorchOn = false
                    }
                    if (timelapseState.isRecording && !timelapseState.isPaused) {
                        timelapseState.pauseRecording("App minimized")
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    isCameraPermissionRevoked = !hasPerm
                    if (timelapseState.isRecording && timelapseState.isPaused && timelapseState.userMessage == "App minimized") {
                        timelapseState.resumeRecording()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(timelapseState.isRecording, timelapseState.isPaused, timelapseState.intervalSeconds) {
        if (timelapseState.isRecording && !timelapseState.isPaused) {
            while (timelapseState.isRecording && !timelapseState.isPaused) {
                val sv = sceneViewRef
                if (sv != null && sv.width > 0 && sv.height > 0) {
                    val available = ExportManager.getAvailableStorageBytes(context)
                    if (ExportMath.isStorageLow(available)) {
                        isLowStorage = true
                        timelapseState.pauseRecording("Low storage space (<100MB)")
                        break
                    }

                    val frameCount = timelapseState.recordedFrameCount
                    val currentBytes = timelapseState.recordedBytes
                    if (!ExportMath.canRecordNextFrame(frameCount, currentBytes)) {
                        timelapseState.stopRecording()
                        Toast.makeText(context, "Timelapse storage limit reached (600 frames)", Toast.LENGTH_LONG).show()
                        break
                    }

                    val targetW = 720
                    val targetH = (720f * (sv.height.toFloat() / sv.width.toFloat())).toInt()
                    val frameBmp = ExportManager.captureSurface(sv, targetW, targetH)
                    if (frameBmp != null) {
                        val savedFile = ExportManager.saveTimelapseFrame(context, frameBmp, frameCount)
                        if (savedFile != null) {
                            timelapseState.recordedFrameCount++
                            timelapseState.recordedBytes += savedFile.length()
                        }
                        frameBmp.recycle()
                    }
                }
                delay(timelapseState.intervalSeconds * 1000L)
            }
        }
    }

    // --- Overlay Smoothing (Off / Low / High) ---
    var smoothingMode by remember {
        mutableStateOf(
            SmoothingMode.entries.find { it.name == currentProject?.smoothingMode }
                ?: (settings?.smoothingMode ?: SmoothingMode.LOW)
        )
    }
    val poseFilter = remember { PoseFilter(smoothingMode) }
    LaunchedEffect(smoothingMode) {
        poseFilter.mode = smoothingMode
    }

    // --- Image Adjustments (Normal Mode) ---
    var brightness by remember { mutableFloatStateOf(currentProject?.brightness ?: 0f) }
    var contrast by remember { mutableFloatStateOf(currentProject?.contrast ?: 1.0f) }
    var invert by remember { mutableFloatStateOf(currentProject?.invert ?: 0f) }
    var bwThreshold by remember { mutableFloatStateOf(currentProject?.bwThreshold ?: 0f) }
    val adjustments = remember(brightness, contrast, invert, bwThreshold) {
        ImageAdjustments(brightness, contrast, invert, bwThreshold)
    }

    // --- Tracing Settings ---
    var opacity by remember { mutableFloatStateOf(currentProject?.opacity ?: (settings?.defaultOpacity ?: 0.5f)) }
    var isLocked by remember { mutableStateOf(false) }
    var fitMode by remember {
        mutableStateOf(
            if (currentProject != null) {
                when (currentProject!!.fitMode.uppercase()) {
                    "FIT" -> FitMode.FIT
                    "FILL" -> FitMode.FILL
                    else -> FitMode.STRETCH
                }
            } else {
                settings?.defaultFitMode ?: FitMode.STRETCH
            }
        )
    }
    var debugMode by remember { mutableStateOf(false) }

    // --- UI State & Transform ---
    val tracingUi = remember {
        TracingUiState(context).apply {
            if (settings != null) {
                isLeftHanded = settings.isLeftHanded
            }
        }
    }
    var transform by remember { mutableStateOf(currentProject?.transform ?: NormalizedTransform()) }
    var smoothedAmbientIntensity by remember { mutableFloatStateOf(0.5f) }
    var lastAutoColorChangeTime by remember { mutableLongStateOf(0L) }

    // --- Advanced Transform, Paper & Measurement State ---
    var crop by remember { mutableStateOf(currentProject?.crop ?: NormalizedCrop()) }
    var isCropDialogOpen by remember { mutableStateOf(false) }
    var paperPreset by remember { mutableStateOf(PaperPreset.fromName(currentProject?.paperPresetName)) }
    var previousCornersBeforeSnap by remember { mutableStateOf<List<Vector3f>?>(null) }
    var isRulerEnabled by remember { mutableStateOf(currentProject?.isRulerEnabled ?: false) }

    val (paperWidthMeters, paperHeightMeters) = remember(calibration.localCorners) {
        PaperMath.calculatePaperDimensions(calibration.localCorners)
    }

    val effectiveCorners = remember(calibration.localCorners.toList(), paperLockState.currentPose, paperLockState.isEnabled) {
        paperLockState.computeEffectiveCorners(calibration.localCorners)
    }

    fun realignPaper() {
        val detected = paperLockState.detectedQuadOnPlane
        if (detected != null && detected.size == 4) {
            val est = RigidTransform2D.estimateFromCorrespondingPoints(calibration.localCorners, detected)
            if (est != null) {
                paperLockState.currentPose = est.paperFrame
                paperLockState.bannerType = PaperBannerType.NONE
                Toast.makeText(context, "Paper realigned! ✓", Toast.LENGTH_SHORT).show()
                return
            }
        }
        paperLockState.resetToIdentity()
        if (calibration.localCorners.size == 4) {
            paperTracker.initBaseline(calibration.localCorners)
        }
        Toast.makeText(context, "Paper lock reset to baseline", Toast.LENGTH_SHORT).show()
    }

    var lastTrackerCycleTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(calibration.isCalibrated) {
        if (calibration.isCalibrated && calibration.localCorners.size == 4) {
            paperTracker.initBaseline(calibration.localCorners)
            paperLockState.resetToIdentity()
        }
    }

    val transformUndoManager = remember {
        TransformUndoManager(
            maxHistory = 50,
            initialSnapshot = TransformSnapshot(
                transform = currentProject?.transform ?: NormalizedTransform(),
                fitMode = fitMode,
                crop = crop
            )
        )
    }

    // 6-second inactivity auto-close timer for active category sheet
    LaunchedEffect(tracingUi.activeCategory, tracingUi.lastInteractionTime) {
        if (tracingUi.activeCategory != null) {
            delay(6000)
            tracingUi.closeSheet()
        }
    }

    // Tracking Status computation
    val trackingState = surfaceState.trackingState
    val failureReason = surfaceState.trackingFailureReason

    val isTrackingLost = trackingState != TrackingState.TRACKING
    val isTrackingLimited = !isTrackingLost && ((failureReason != null && failureReason != TrackingFailureReason.NONE) || surfaceState.scanQuality == ScanQuality.LOW)

    val (trackingBadgeColor, trackingBadgeText, trackingTip) = when {
        isTrackingLost -> {
            val reason = when (failureReason) {
                TrackingFailureReason.INSUFFICIENT_LIGHT -> "Too dark"
                TrackingFailureReason.EXCESSIVE_MOTION -> "Moving fast"
                TrackingFailureReason.INSUFFICIENT_FEATURES -> "Low texture"
                else -> "Lost surface"
            }
            val tip = when (failureReason) {
                TrackingFailureReason.INSUFFICIENT_LIGHT -> "Turn on torch or brighten room"
                TrackingFailureReason.EXCESSIVE_MOTION -> "Hold phone steady over paper"
                else -> "Point phone back towards paper"
            }
            Triple(Color(0xFFDA3633), "Lost: $reason", tip)
        }
        isTrackingLimited -> {
            val reason = when (failureReason) {
                TrackingFailureReason.EXCESSIVE_MOTION -> "Moving fast"
                TrackingFailureReason.INSUFFICIENT_LIGHT -> "Low light"
                TrackingFailureReason.INSUFFICIENT_FEATURES -> "Low detail"
                else -> "Scanning"
            }
            val tip = when (failureReason) {
                TrackingFailureReason.EXCESSIVE_MOTION -> "Slow down phone movement"
                TrackingFailureReason.INSUFFICIENT_LIGHT -> "Turn on torch or improve lighting"
                else -> "Point at paper texture"
            }
            Triple(Color(0xFFD29922), "Limited: $reason", tip)
        }
        else -> Triple(Color(0xFF3FB950), "Good", null)
    }

    val effectiveOpacity = when {
        tracingUi.isPeeking -> 0f
        isTrackingLost -> opacity * 0.30f
        else -> opacity
    }

    // --- Load the selected image at high quality (up to 4096px) ---
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(currentProject?.id, imageUri) {
        val p = currentProject
        if (p != null) {
            val file = repository.getProjectImageFile(p)
            if (file.exists()) {
                imageBitmap = loadHighResBitmapFromFile(file)
            }
        } else if (imageUri != null) {
            imageBitmap = loadHighResBitmap(context, imageUri)
        }
    }

    // Debounced Lines-only update
    LaunchedEffect(
        linesOnlyState.isEnabled,
        linesOnlyState.edgeSensitivity,
        linesOnlyState.lineThickness,
        linesOnlyState.selectedColorOption,
        imageBitmap
    ) {
        if (linesOnlyState.isEnabled && imageBitmap != null) {
            linesOnlyState.updateLines(scope, imageBitmap)
        }
    }

    // Debounced Tonal Segmentation computation (Bilateral Filter + Luminance Quantization)
    LaunchedEffect(
        tonalState.isEnabled,
        tonalState.toneCount,
        tonalState.smoothingLevel,
        imageBitmap
    ) {
        if (tonalState.isEnabled && imageBitmap != null) {
            delay(150)
            tonalState.isProcessing = true
            val seg = kotlinx.coroutines.withContext(Dispatchers.Default) {
                TonalProcessor.processTonalSegmentation(
                    src = imageBitmap!!,
                    toneCount = tonalState.toneCount,
                    smoothingLevel = tonalState.smoothingLevel
                )
            }
            tonalSegmentation = seg
            tonalState.processingTimeMs = seg?.durationMs?.toInt() ?: 0
            tonalState.isProcessing = false
        }
    }

    // Fast in-memory compositing of active tonal layers (< 15ms)
    val layerSignature = tonalState.layers.map {
        "${it.isVisible}_${it.colorArgb}_${it.opacity}"
    }.joinToString(";")

    LaunchedEffect(
        tonalState.isEnabled,
        tonalSegmentation,
        layerSignature,
        tonalState.isOutlineVisible,
        tonalState.outlineColorArgb,
        tonalState.outlineOpacity,
        tonalState.soloLayerId,
        tonalState.isStagesMode,
        tonalState.currentStage
    ) {
        val seg = tonalSegmentation
        if (tonalState.isEnabled && seg != null) {
            val comp = kotlinx.coroutines.withContext(Dispatchers.Default) {
                TonalProcessor.compositeLayers(
                    segmentation = seg,
                    layers = tonalState.layers.toList(),
                    isOutlineVisible = tonalState.isOutlineVisible,
                    outlineColorArgb = tonalState.outlineColorArgb,
                    outlineOpacity = tonalState.outlineOpacity,
                    soloLayerId = tonalState.soloLayerId,
                    isStagesMode = tonalState.isStagesMode,
                    currentStage = tonalState.currentStage
                )
            }
            tonalState.compositeBitmap = comp
        } else if (!tonalState.isEnabled) {
            tonalState.compositeBitmap = null
        }
    }

    // Helper: Save Project
    fun saveProjectData(showToast: Boolean = false) {
        val p = currentProject ?: return
        val updated = p.copy(
            opacity = opacity,
            fitMode = fitMode.name,
            isLinesOnly = linesOnlyState.isEnabled,
            edgeSensitivity = linesOnlyState.edgeSensitivity,
            lineThickness = linesOnlyState.lineThickness,
            lineColorName = linesOnlyState.selectedColorOption.name,
            gridEnabled = gridState.isEnabled,
            gridCols = gridState.cols,
            gridRows = gridState.rows,
            doneCells = gridState.doneCells.toList(),
            transform = transform,
            lastModified = System.currentTimeMillis(),
            brightness = brightness,
            contrast = contrast,
            invert = invert,
            bwThreshold = bwThreshold,
            smoothingMode = smoothingMode.name,
            crop = crop,
            paperPresetName = paperPreset.name,
            isRulerEnabled = isRulerEnabled,
            guides = guidesState.toData(),
            tones = tonalState.toData()
        )
        scope.launch(Dispatchers.IO) {
            repository.saveProject(updated)
            currentProject = updated
            if (showToast) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Project saved! ✓", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ── Export: Photo Capture Handler ────────────────────────
    fun performPhotoCapture(type: CaptureType) {
        val sv = sceneViewRef ?: return
        isCapturingPhoto = true
        scope.launch {
            try {
                when (type) {
                    CaptureType.WITH_OVERLAY -> {
                        val bmp = ExportManager.captureSurface(sv)
                        if (bmp != null) {
                            val filename = ExportMath.formatPhotoFilename(type)
                            val uri = ExportManager.savePhotoToGallery(context, bmp, filename)
                            lastCaptureResult = CaptureResult(uri, null, type, message = "Saved with AR overlay ($filename)")
                            Toast.makeText(context, "Photo saved to Gallery ✓", Toast.LENGTH_SHORT).show()
                            bmp.recycle()
                        } else {
                            Toast.makeText(context, "Unable to capture photo", Toast.LENGTH_SHORT).show()
                        }
                    }
                    CaptureType.WITHOUT_OVERLAY -> {
                        tracingUi.isPeeking = true
                        delay(50)
                        val bmp = ExportManager.captureSurface(sv)
                        tracingUi.isPeeking = false
                        if (bmp != null) {
                            val filename = ExportMath.formatPhotoFilename(type)
                            val uri = ExportManager.savePhotoToGallery(context, bmp, filename)
                            lastCaptureResult = CaptureResult(uri, null, type, message = "Saved drawing only ($filename)")
                            Toast.makeText(context, "Drawing photo saved to Gallery ✓", Toast.LENGTH_SHORT).show()
                            bmp.recycle()
                        } else {
                            Toast.makeText(context, "Unable to capture drawing", Toast.LENGTH_SHORT).show()
                        }
                    }
                    CaptureType.RECTIFIED_PAPER -> {
                        if (!calibration.isCalibrated || calibration.localCorners.size != 4) {
                            Toast.makeText(context, "Calibrate paper corners first to generate rectified scan", Toast.LENGTH_SHORT).show()
                            isCapturingPhoto = false
                            return@launch
                        }
                        tracingUi.isPeeking = true
                        delay(50)
                        val rawBmp = ExportManager.captureSurface(sv)
                        tracingUi.isPeeking = false

                        if (rawBmp != null) {
                            val anchor = calibration.surfaceAnchor
                            if (anchor != null) {
                                val sorted = MathUtils.sortCornersClockwise(effectiveCorners)
                                val worldCorners = sorted.map { localToWorld(anchor.pose, it) }
                                val s0 = sv.cameraNode.worldToScreenPoint(Vector3(worldCorners[0].x, worldCorners[0].y, worldCorners[0].z))
                                val s1 = sv.cameraNode.worldToScreenPoint(Vector3(worldCorners[1].x, worldCorners[1].y, worldCorners[1].z))
                                val s2 = sv.cameraNode.worldToScreenPoint(Vector3(worldCorners[2].x, worldCorners[2].y, worldCorners[2].z))
                                val s3 = sv.cameraNode.worldToScreenPoint(Vector3(worldCorners[3].x, worldCorners[3].y, worldCorners[3].z))

                                val scaleX = rawBmp.width.toFloat() / sv.width.toFloat()
                                val scaleY = rawBmp.height.toFloat() / sv.height.toFloat()
                                val screenPts = listOf(
                                    Offset(s0.x * scaleX, s0.y * scaleY),
                                    Offset(s1.x * scaleX, s1.y * scaleY),
                                    Offset(s2.x * scaleX, s2.y * scaleY),
                                    Offset(s3.x * scaleX, s3.y * scaleY)
                                )
                                val aspect = if (paperHeightMeters > 0) paperWidthMeters / paperHeightMeters else 0.707f
                                val rectifiedBmp = ExportManager.rectifyPaper(rawBmp, screenPts, aspect)

                                if (rectifiedBmp != null) {
                                    val filename = ExportMath.formatPhotoFilename(type)
                                    val uri = ExportManager.savePhotoToGallery(context, rectifiedBmp, filename)
                                    lastCaptureResult = CaptureResult(uri, null, type, message = "Saved top-down scan ($filename)")
                                    Toast.makeText(context, "Paper scan saved to Gallery ✓", Toast.LENGTH_SHORT).show()
                                    rectifiedBmp.recycle()
                                } else {
                                    Toast.makeText(context, "Failed to rectify paper scan", Toast.LENGTH_SHORT).show()
                                }
                            }
                            rawBmp.recycle()
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e("TraceAR", "Capture failed: ${e.message}", e)
                Toast.makeText(context, "Capture error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isCapturingPhoto = false
            }
        }
    }

    // ── Export: Timelapse Video Encoding Handler ─────────────
    fun performTimelapseExport() {
        timelapseCancelFlag.set(false)
        timelapseState.status = TimelapseStatus.EXPORTING
        timelapseState.exportProgress = 0f

        scope.launch {
            val frames = ExportManager.getTimelapseFrameFiles(context)
            if (frames.isEmpty()) {
                timelapseState.status = TimelapseStatus.ERROR
                Toast.makeText(context, "No recorded frames to export", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val outputMp4 = File(context.cacheDir, "timelapse_${System.currentTimeMillis()}.mp4")
            val sv = sceneViewRef
            val (w, h) = if (sv != null && sv.width > 0 && sv.height > 0) {
                ExportMath.ensureEvenDimensions(720, (720f * (sv.height.toFloat() / sv.width.toFloat())).toInt())
            } else {
                Pair(720, 1280)
            }

            val success = TimelapseEncoder.encodeFrames(
                frameFiles = frames,
                outputFile = outputMp4,
                width = w,
                height = h,
                fps = timelapseState.playbackFps,
                cancelFlag = timelapseCancelFlag,
                onProgress = { p -> timelapseState.exportProgress = p }
            )

            if (success && outputMp4.exists()) {
                val filename = ExportMath.formatVideoFilename()
                val galleryUri = ExportManager.saveVideoToGallery(context, outputMp4, filename)
                timelapseState.lastExportedFile = outputMp4
                timelapseState.lastExportedUri = galleryUri
                timelapseState.status = TimelapseStatus.COMPLETED
                Toast.makeText(context, "Timelapse saved to Gallery ✓", Toast.LENGTH_LONG).show()

                ExportManager.cleanupTimelapseFrames(context)
                timelapseState.recordedBytes = 0L
            } else if (timelapseCancelFlag.get()) {
                timelapseState.status = TimelapseStatus.IDLE
                Toast.makeText(context, "Timelapse export cancelled", Toast.LENGTH_SHORT).show()
            } else {
                timelapseState.status = TimelapseStatus.ERROR
                Toast.makeText(context, "Failed to encode timelapse video", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Lifecycle handling: Keep screen awake, reset brightness, turn off torch, autosave on exit
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val lp = activity?.window?.attributes
            if (lp != null) {
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                activity?.window?.attributes = lp
            }
            if (isTorchOn) {
                try {
                    val s = arSessionRef
                    if (s != null) {
                        val c = s.config
                        c.flashMode = Config.FlashMode.OFF
                        s.configure(c)
                    }
                } catch (ignored: Throwable) {}
            }
            saveProjectData(showToast = false)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val p = currentProject
                if (p != null) {
                    val imgFile = repository.getProjectImageFile(p)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(imgFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    val bmp = loadHighResBitmapFromFile(imgFile)
                    withContext(Dispatchers.Main) {
                        imageBitmap = bmp
                        if (linesOnlyState.isEnabled && bmp != null) {
                            linesOnlyState.updateLines(scope, bmp)
                        }
                    }
                } else {
                    val bmp = loadHighResBitmap(context, uri)
                    withContext(Dispatchers.Main) {
                        imageBitmap = bmp
                        if (linesOnlyState.isEnabled && bmp != null) {
                            linesOnlyState.updateLines(scope, bmp)
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs ---
    var showRecalibDialog by remember { mutableStateOf(false) }

    // Store references
    val latestFrameRef = remember { java.util.concurrent.atomic.AtomicReference<Frame?>(null) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var detectedPaper by remember { mutableStateOf<DetectedPaper?>(null) }

    // Pre-allocated matrices for raycasting (avoid allocating every frame)
    val projMatrix = remember { FloatArray(16) }
    val viewMatrix = remember { FloatArray(16) }
    val viewProjMatrix = remember { FloatArray(16) }
    val invViewProjMatrix = remember { FloatArray(16) }

    // --- Timer for 8-second tip timeout ---
    LaunchedEffect(surfaceState.isPlaneLocked) {
        if (!surfaceState.isPlaneLocked) {
            var elapsed = 0f
            while (!surfaceState.isPlaneLocked) {
                delay(500)
                elapsed += 0.5f
                surfaceState.searchDurationSeconds = elapsed
                if (elapsed >= SurfaceState.TIP_TIMEOUT_SECONDS) {
                    surfaceState.showScanTips = true
                }
            }
        }
    }

    // ── Helper: Focus Section ────────────────────────────────
    fun focusActiveSection() {
        val sv = sceneViewRef ?: return
        val anchor = calibration.surfaceAnchor ?: return
        if (!calibration.isCalibrated) return
        if (gridState.activeCell < 0) {
            gridState.activeCell = 0
        }

        val sorted = MathUtils.sortCornersClockwise(effectiveCorners)
        val worldCorners = sorted.map { localToWorld(anchor.pose, it) }

        val s0 = sv.cameraNode.worldToScreenPoint(Vector3(worldCorners[0].x, worldCorners[0].y, worldCorners[0].z))
        val s1 = sv.cameraNode.worldToScreenPoint(Vector3(worldCorners[1].x, worldCorners[1].y, worldCorners[1].z))
        val s2 = sv.cameraNode.worldToScreenPoint(Vector3(worldCorners[2].x, worldCorners[2].y, worldCorners[2].z))
        val s3 = sv.cameraNode.worldToScreenPoint(Vector3(worldCorners[3].x, worldCorners[3].y, worldCorners[3].z))

        val pTL = Offset(s0.x, s0.y)
        val pTR = Offset(s1.x, s1.y)
        val pBR = Offset(s2.x, s2.y)
        val pBL = Offset(s3.x, s3.y)

        val (col, row) = gridState.cellToColRow(gridState.activeCell)
        val uMid = (col + 0.5f) / gridState.cols
        val vMid = (row + 0.5f) / gridState.rows
        val cellCenter = bilinear(pTL, pTR, pBR, pBL, uMid, vMid)

        val viewW = containerSize.width.toFloat()
        val viewH = containerSize.height.toFloat()
        if (viewW <= 0f || viewH <= 0f) return

        val targetScale = (minOf(gridState.cols, gridState.rows) * 0.75f).coerceIn(1.8f, 3.8f)
        val centerX = viewW / 2f
        val centerY = viewH / 2f

        val targetOffsetX = -(cellCenter.x - centerX) * targetScale
        val targetOffsetY = -(cellCenter.y - centerY) * targetScale

        zoomState.scale = targetScale
        zoomState.offset = Offset(targetOffsetX, targetOffsetY)
        zoomState.clampOffset(viewW, viewH)
    }

    // ── Helper: Cell Tap Detection in Tracing Mode ───────────
    fun handleCellTap(tapPos: Offset) {
        val sv = sceneViewRef ?: return
        val anchor = calibration.surfaceAnchor ?: return
        if (!calibration.isCalibrated || !gridState.isEnabled) return

        val sorted = MathUtils.sortCornersClockwise(effectiveCorners)
        val worldCorners = sorted.map { localToWorld(anchor.pose, it) }

        val s0 = sv.cameraNode.worldToScreenPoint(Vector3(worldCorners[0].x, worldCorners[0].y, worldCorners[0].z))
        val s1 = sv.cameraNode.worldToScreenPoint(Vector3(worldCorners[1].x, worldCorners[1].y, worldCorners[1].z))
        val s2 = sv.cameraNode.worldToScreenPoint(Vector3(worldCorners[2].x, worldCorners[2].y, worldCorners[2].z))
        val s3 = sv.cameraNode.worldToScreenPoint(Vector3(worldCorners[3].x, worldCorners[3].y, worldCorners[3].z))

        val viewW = containerSize.width.toFloat()
        val viewH = containerSize.height.toFloat()
        val centerX = viewW / 2f
        val centerY = viewH / 2f

        // Transform tap from outer zoomed view into unscaled sceneview space
        val sceneX = (tapPos.x - centerX - zoomState.offset.x) / zoomState.scale + centerX
        val sceneY = (tapPos.y - centerY - zoomState.offset.y) / zoomState.scale + centerY
        val pt = Offset(sceneX, sceneY)

        val pTL = Offset(s0.x, s0.y)
        val pTR = Offset(s1.x, s1.y)
        val pBR = Offset(s2.x, s2.y)
        val pBL = Offset(s3.x, s3.y)

        val cols = gridState.cols
        val rows = gridState.rows

        for (c in 0 until cols) {
            for (r in 0 until rows) {
                val u0 = c.toFloat() / cols
                val u1 = (c + 1).toFloat() / cols
                val v0 = r.toFloat() / rows
                val v1 = (r + 1).toFloat() / rows

                val cTL = bilinear(pTL, pTR, pBR, pBL, u0, v0)
                val cTR = bilinear(pTL, pTR, pBR, pBL, u1, v0)
                val cBR = bilinear(pTL, pTR, pBR, pBL, u1, v1)
                val cBL = bilinear(pTL, pTR, pBR, pBL, u0, v1)

                if (isPointInQuad(pt, cTL, cTR, cBR, cBL)) {
                    val cellIdx = gridState.colRowToCell(c, r)
                    gridState.activeCell = if (gridState.activeCell == cellIdx) -1 else cellIdx
                    return
                }
            }
        }
    }

    // ── Helper: Value Eyedropper Sampling on AR Overlay ──────
    fun handleOverlayPickTap(tapPos: Offset): Boolean {
        val sv = sceneViewRef ?: return false
        val anchor = calibration.surfaceAnchor ?: return false
        val bmp = imageBitmap ?: return false
        if (!calibration.isCalibrated) return false

        val sorted = MathUtils.sortCornersClockwise(effectiveCorners)
        val worldCorners = sorted.map { localToWorld(anchor.pose, it) }

        val s0 = sv.cameraNode.worldToScreenPoint(Vector3(worldCorners[0].x, worldCorners[0].y, worldCorners[0].z))
        val s1 = sv.cameraNode.worldToScreenPoint(Vector3(worldCorners[1].x, worldCorners[1].y, worldCorners[1].z))
        val s2 = sv.cameraNode.worldToScreenPoint(Vector3(worldCorners[2].x, worldCorners[2].y, worldCorners[2].z))
        val s3 = sv.cameraNode.worldToScreenPoint(Vector3(worldCorners[3].x, worldCorners[3].y, worldCorners[3].z))

        val viewW = containerSize.width.toFloat()
        val viewH = containerSize.height.toFloat()
        val centerX = viewW / 2f
        val centerY = viewH / 2f

        val sceneX = (tapPos.x - centerX - zoomState.offset.x) / zoomState.scale + centerX
        val sceneY = (tapPos.y - centerY - zoomState.offset.y) / zoomState.scale + centerY
        val pt = Offset(sceneX, sceneY)

        val pTL = Offset(s0.x, s0.y)
        val pTR = Offset(s1.x, s1.y)
        val pBR = Offset(s2.x, s2.y)
        val pBL = Offset(s3.x, s3.y)

        val steps = 20
        for (c in 0 until steps) {
            for (r in 0 until steps) {
                val u0 = c.toFloat() / steps
                val u1 = (c + 1).toFloat() / steps
                val v0 = r.toFloat() / steps
                val v1 = (r + 1).toFloat() / steps

                val cTL = bilinear(pTL, pTR, pBR, pBL, u0, v0)
                val cTR = bilinear(pTL, pTR, pBR, pBL, u1, v0)
                val cBR = bilinear(pTL, pTR, pBR, pBL, u1, v1)
                val cBL = bilinear(pTL, pTR, pBR, pBL, u0, v1)

                if (isPointInQuad(pt, cTL, cTR, cBR, cBL)) {
                    val midU = (u0 + u1) / 2f
                    val midV = (v0 + v1) / 2f
                    val result = TonalProcessor.samplePixel(bmp, midU, midV, tonalState.toneCount)
                    tonalState.inspectedValue = result
                    Toast.makeText(context, "${result.toneName}: ${result.brightnessPercent}% (${result.hexCode})", Toast.LENGTH_SHORT).show()
                    return true
                }
            }
        }
        return false
    }

    // ── Helper: Process Manual Tap Raycasting on Infinite Plane ──
    fun handleManualCornerTap(screenTap: Offset, viewW: Float, viewH: Float) {
        val frame = latestFrameRef.get() ?: return
        val anchor = calibration.surfaceAnchor ?: return
        if (!surfaceState.isPlaneLocked || calibration.isCalibrated) return

        // 1. Get projection and view matrices
        frame.camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f)
        frame.camera.getViewMatrix(viewMatrix, 0)
        android.opengl.Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // 2. Invert View-Projection matrix
        if (!MathUtils.invertMatrix4(viewProjMatrix, invViewProjMatrix)) {
            Log.w("TraceAR", "Singular View-Projection matrix, ignoring tap")
            return
        }

        // 3. Unproject screen tap to 3D world ray
        val ray = MathUtils.unprojectScreenPointToRay(
            screenTap.x,
            screenTap.y,
            viewW,
            viewH,
            invViewProjMatrix
        )

        // 4. Intersect ray with locked infinite table plane
        val result = MathUtils.rayPlaneIntersection(
            ray,
            surfaceState.lockedPlanePoint,
            surfaceState.lockedPlaneNormal,
            maxDistance = 2.0f
        )

        when (result) {
            is RayPlaneResult.Hit -> {
                val localPos = worldToLocal(anchor.pose, result.point)
                calibration.addCorner(localPos)
                val logMsg = "Corner ${calibration.cornerCount} hit at: (%.2f, %.2f, %.2f), dist: %.2fm".format(
                    result.point.x, result.point.y, result.point.z, result.distance
                )
                surfaceState.lastRayIntersectionResult = logMsg
                Log.i("TraceAR", logMsg)
            }
            is RayPlaneResult.Parallel -> {
                surfaceState.lastRayIntersectionResult = "Parallel ray: tap too steep"
                Toast.makeText(context, "Tap is at too steep an angle. Tilt phone towards paper.", Toast.LENGTH_SHORT).show()
            }
            is RayPlaneResult.BehindCamera -> {
                surfaceState.lastRayIntersectionResult = "Behind camera: tap above horizon"
                Toast.makeText(context, "Point phone down towards the surface.", Toast.LENGTH_SHORT).show()
            }
            is RayPlaneResult.TooFar -> {
                val logMsg = "Too far: %.2fm (> 2m)".format(result.distance)
                surfaceState.lastRayIntersectionResult = logMsg
                Toast.makeText(context, "Tap is too far away. Move phone closer to the table.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapPos by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
                // Capture pinch-to-zoom, pan, grid taps, transform gestures, and double-tap for Focus mode
                .pointerInput(isLocked, gridState.isEnabled, calibration.isCalibrated, tracingUi.isFocusMode) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var pastTouchSlop = false
                        val touchSlop = viewConfiguration.touchSlop
                        var totalPan = Offset.Zero
                        val initialDown = down.position
                        val initialTransform = transform

                        do {
                            val event = awaitPointerEvent()
                            val activePointers = event.changes.filter { it.pressed }
                            if (activePointers.isEmpty()) break

                            if (isLocked && activePointers.size >= 2) {
                                pastTouchSlop = true
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()

                                if (zoom != 1f || pan != Offset.Zero) {
                                    zoomState.scale = (zoomState.scale * zoom).coerceIn(1f, 4f)
                                    zoomState.offset += pan
                                    zoomState.clampOffset(size.width.toFloat(), size.height.toFloat())
                                    event.changes.forEach { it.consume() }
                                }
                            } else if (isLocked && activePointers.size == 1) {
                                val change = activePointers.first()
                                val panChange = change.position - change.previousPosition
                                totalPan += panChange

                                if (!pastTouchSlop && totalPan.getDistance() > touchSlop) {
                                    pastTouchSlop = true
                                }

                                if (pastTouchSlop && zoomState.scale > 1f) {
                                    zoomState.offset += panChange
                                    zoomState.clampOffset(size.width.toFloat(), size.height.toFloat())
                                    change.consume()
                                }
                            } else if (!isLocked && calibration.isCalibrated) {
                                if (activePointers.size >= 2) {
                                    pastTouchSlop = true
                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()
                                    val rot = event.calculateRotation()

                                    val newScale = (transform.scale * zoom).coerceIn(0.05f, 10f)
                                    val newRot = (transform.rotationDegrees + rot) % 360f
                                    val newOffsetX = transform.offsetX + (pan.x / size.width)
                                    val newOffsetY = transform.offsetY + (pan.y / size.height)

                                    transform = transform.copy(
                                        offsetX = newOffsetX,
                                        offsetY = newOffsetY,
                                        scale = newScale,
                                        rotationDegrees = newRot
                                    )
                                    event.changes.forEach { it.consume() }
                                } else if (activePointers.size == 1) {
                                    val change = activePointers.first()
                                    val panChange = change.position - change.previousPosition
                                    totalPan += panChange

                                    if (!pastTouchSlop && totalPan.getDistance() > touchSlop) {
                                        pastTouchSlop = true
                                    }

                                    if (pastTouchSlop) {
                                        val newOffsetX = transform.offsetX + (panChange.x / size.width)
                                        val newOffsetY = transform.offsetY + (panChange.y / size.height)
                                        transform = transform.copy(offsetX = newOffsetX, offsetY = newOffsetY)
                                        change.consume()
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        // Single pinch or drag gesture counts as one undo step
                        if (pastTouchSlop && !isLocked && calibration.isCalibrated && transform != initialTransform) {
                            transformUndoManager.push(transform, fitMode, crop)
                        }

                        if (!pastTouchSlop) {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime < 350L && (initialDown - lastTapPos).getDistance() < 60f) {
                                // Double-tap on screen toggles Focus Mode!
                                tracingUi.toggleFocusMode()
                                tracingUi.notifyInteraction()
                                lastTapTime = 0L
                            } else {
                                lastTapTime = now
                                lastTapPos = initialDown
                                if (tonalState.isEyedropperActive && calibration.isCalibrated) {
                                    handleOverlayPickTap(initialDown)
                                } else if (isLocked && gridState.isEnabled && calibration.isCalibrated) {
                                    handleCellTap(initialDown)
                                }
                            }
                        }
                    }
                }
        ) {
        // ===== Zoomable AR Camera View Container =====
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomState.scale
                    scaleY = zoomState.scale
                    translationX = zoomState.offset.x
                    translationY = zoomState.offset.y
                }
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    ARSceneView(ctx).apply {
                        sceneViewRef = this
                        planeRenderer.isEnabled = true
                        sessionConfiguration = { session, config ->
                            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                            config.lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY

                            // Fallback: Automatic Depth Mode when supported
                            if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                                config.depthMode = Config.DepthMode.AUTOMATIC
                                surfaceState.isDepthModeEnabled = true
                            }

                            // Fallback: Instant Placement mode
                            config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                        }

                        onSessionUpdated = { session, frame ->
                            arSessionRef = session
                            latestFrameRef.set(frame)
                            surfaceState.trackingState = frame.camera.trackingState

                            // Check torch capability
                            if (!isTorchSupported) {
                                try {
                                    val testConfig = session.config
                                    testConfig.flashMode = Config.FlashMode.TORCH
                                    if (session.isSupported(testConfig)) {
                                        isTorchSupported = true
                                    }
                                } catch (ignored: Throwable) {}
                            }

                            // Check lighting condition for dark environment warning
                            // Check lighting condition for dark environment warning & Auto Line Color
                            try {
                                val lightEstimate = frame.lightEstimate
                                if (lightEstimate.state == com.google.ar.core.LightEstimate.State.VALID) {
                                    val pixelIntensity = lightEstimate.pixelIntensity
                                    surfaceState.isLowLight = pixelIntensity < 0.28f

                                    // Auto Line Color adaptation: slow low-pass filter to prevent flickering
                                    if (tracingUi.isAutoLineColor && linesOnlyState.isEnabled) {
                                        smoothedAmbientIntensity = smoothedAmbientIntensity * 0.95f + pixelIntensity * 0.05f
                                        val targetColor = if (smoothedAmbientIntensity > 0.45f) LineColorOption.BLACK else LineColorOption.WHITE
                                        if (linesOnlyState.selectedColorOption != targetColor) {
                                            val now = System.currentTimeMillis()
                                            if (now - lastAutoColorChangeTime > 1500L) {
                                                linesOnlyState.selectedColorOption = targetColor
                                                lastAutoColorChangeTime = now
                                            }
                                        }
                                    }

                                    // Auto Contrast for Drawing Guides
                                    if (guidesState.isAutoContrast && guidesState.isAnyGuideActive) {
                                        smoothedAmbientIntensity = smoothedAmbientIntensity * 0.95f + pixelIntensity * 0.05f
                                        val targetColor = if (smoothedAmbientIntensity > 0.45f) LineColorOption.BLACK else LineColorOption.WHITE
                                        if (guidesState.colorOption != targetColor) {
                                            val now = System.currentTimeMillis()
                                            if (now - lastAutoColorChangeTime > 1500L) {
                                                guidesState.colorOption = targetColor
                                                lastAutoColorChangeTime = now
                                            }
                                        }
                                    }
                                } else {
                                    surfaceState.isLowLight = frame.camera.trackingFailureReason == TrackingFailureReason.INSUFFICIENT_LIGHT
                                }
                            } catch (ignored: Throwable) {}

                            // 1. Crosshair Hit Testing & Floor Rejection Filter
                            if (!surfaceState.isPlaneLocked) {
                                val viewW = width.toFloat()
                                val viewH = height.toFloat()
                                if (viewW > 0 && viewH > 0) {
                                    val midX = viewW / 2f
                                    val midY = viewH / 2f

                                    // Hit test at screen center
                                    val hitResults = frame.hitTest(midX, midY)
                                    val candidate = hitResults.firstOrNull { hit ->
                                        val trackable = hit.trackable
                                        if (trackable !is Plane || trackable.type != Plane.Type.HORIZONTAL_UPWARD_FACING) {
                                            return@firstOrNull false
                                        }

                                        // Ensure hit point is within detected polygon
                                        if (!trackable.isPoseInPolygon(hit.hitPose)) {
                                            return@firstOrNull false
                                        }

                                        // Reject floor: check distance from camera to hit (< 1.2m)
                                        val dist = hit.distance
                                        if (dist > SurfaceState.MAX_SURFACE_DISTANCE) {
                                            return@firstOrNull false
                                        }

                                        // Reject if hit is vertically too far from plane center (< 0.15m)
                                        val deltaY = abs(hit.hitPose.ty() - trackable.centerPose.ty())
                                        if (deltaY > SurfaceState.MAX_PLANE_DELTA_Y) {
                                            return@firstOrNull false
                                        }

                                        // Reject slanted surfaces / vertical walls
                                        val q = hit.hitPose.rotationQuaternion
                                        val normal = MathUtils.extractPlaneNormal(q[0], q[1], q[2], q[3])
                                        if (!MathUtils.isRoughlyVertical(normal, SurfaceState.MAX_NORMAL_ANGLE_DEG)) {
                                            return@firstOrNull false
                                        }

                                        true
                                    }

                                    if (candidate != null) {
                                        surfaceState.candidatePlane = candidate.trackable as Plane
                                        surfaceState.candidateHitPose = candidate.hitPose
                                        surfaceState.candidateDistance = candidate.distance
                                        surfaceState.isCandidateValid = true
                                    } else {
                                        surfaceState.isCandidateValid = false
                                    }
                                }

                                // 2. Compute Scan Quality from Point Cloud
                                try {
                                    val pointCloud = frame.acquirePointCloud()
                                    val pointsBuf = pointCloud.points
                                    val count = pointsBuf.remaining() / 4
                                    surfaceState.totalPointCloudCount = count
                                    surfaceState.featurePointsNearCrosshair = count

                                    surfaceState.scanQuality = when {
                                        count < SurfaceState.SCAN_QUALITY_LOW_MAX -> ScanQuality.LOW
                                        count < SurfaceState.SCAN_QUALITY_MED_MAX -> ScanQuality.MEDIUM
                                        else -> ScanQuality.GOOD
                                    }
                                    pointCloud.release()
                                } catch (ignored: Throwable) {}
                            }

                            // 3. OpenCV Paper Auto-Detection (when locked but not yet calibrated)
                            if (surfaceState.isPlaneLocked && !calibration.isCalibrated) {
                                val isPowerSave = powerManager?.isPowerSaveMode == true || (settings?.isBatterySaver == true)
                                val now = System.currentTimeMillis()
                                if (!isPowerSave || now - lastPaperDetectionTime >= 250L) {
                                    lastPaperDetectionTime = now
                                    val det = paperDetector.processFrame(frame, width, height)
                                    detectedPaper = det
                                }
                            } else {
                                detectedPaper = null
                            }

                            // 4. Paper Lock Tracking (when calibrated and plane locked)
                            if (calibration.isCalibrated && surfaceState.isPlaneLocked) {
                                val anchor = calibration.surfaceAnchor
                                val isArTracking = frame.camera.trackingState == TrackingState.TRACKING
                                if (!paperLockState.isEnabled) {
                                    paperLockState.trackingStatus = PaperTrackingStatus.OFF
                                } else if (!isArTracking || paperLockState.isFrozen) {
                                    paperLockState.trackingStatus = PaperTrackingStatus.PAUSED
                                } else if (anchor != null) {
                                    val now = System.currentTimeMillis()
                                    val isPowerSave = powerManager?.isPowerSaveMode == true || (settings?.isBatterySaver == true)
                                    val minIntervalMs = if (isPowerSave) 250L else 100L

                                    if (now - lastTrackerCycleTime >= minIntervalMs) {
                                        if (isProcessingTrackerCycle.compareAndSet(false, true)) {
                                            lastTrackerCycleTime = now
                                            val camIntrinsics = try { frame.camera.imageIntrinsics } catch (e: Throwable) { null }
                                            if (camIntrinsics != null) {
                                                val f = camIntrinsics.focalLength
                                                val p = camIntrinsics.principalPoint
                                                val d = camIntrinsics.imageDimensions
                                                val intrinsicsData = IntrinsicsData(
                                                    fx = f[0], fy = f[1], cx = p[0], cy = p[1],
                                                    width = d[0], height = d[1]
                                                )
                                                val camPose = frame.camera.pose
                                                val camTx = camPose.tx(); val camTy = camPose.ty(); val camTz = camPose.tz()
                                                val camQx = camPose.qx(); val camQy = camPose.qy(); val camQz = camPose.qz(); val camQw = camPose.qw()

                                                val aPose = anchor.pose
                                                val aTx = aPose.tx(); val aTy = aPose.ty(); val aTz = aPose.tz()
                                                val aQx = aPose.qx(); val aQy = aPose.qy(); val aQz = aPose.qz(); val aQw = aPose.qw()

                                                val planePoint = Vector3f(aTx, aTy, aTz)
                                                val planeNormal = MathUtils.rotateVectorByQuaternion(Vector3f(0f, 1f, 0f), aQx, aQy, aQz, aQw)

                                                var image: Image? = null
                                                var extractedMat: Mat? = null
                                                try {
                                                    image = frame.acquireCameraImage()
                                                    val yPlane = image.planes[0]
                                                    val imgW = image.width
                                                    val imgH = image.height
                                                    val rowStride = yPlane.rowStride
                                                    val yBuf = yPlane.buffer
                                                    val yBytes = ByteArray(yBuf.remaining())
                                                    yBuf.get(yBytes)

                                                    val fullMat = Mat(imgH, rowStride, CvType.CV_8UC1)
                                                    fullMat.put(0, 0, yBytes)
                                                    extractedMat = if (rowStride > imgW) {
                                                        val sub = fullMat.submat(0, imgH, 0, imgW)
                                                        val cloned = sub.clone()
                                                        sub.release()
                                                        fullMat.release()
                                                        cloned
                                                    } else {
                                                        fullMat
                                                    }
                                                } catch (e: Throwable) {
                                                    extractedMat?.release()
                                                    extractedMat = null
                                                } finally {
                                                    image?.close()
                                                }

                                                if (extractedMat != null) {
                                                    val matToProcess = extractedMat
                                                    val curFrozen = paperLockState.isFrozen
                                                    val curEnabled = paperLockState.isEnabled
                                                    val curSens = paperLockState.sensitivity
                                                    scope.launch(Dispatchers.Default) {
                                                        try {
                                                            if (!paperTracker.isTemplateReady) {
                                                                paperTracker.captureTemplate(
                                                                    grayMat = matToProcess,
                                                                    intrinsics = intrinsicsData,
                                                                    camPoseTx = camTx, camPoseTy = camTy, camPoseTz = camTz,
                                                                    camQx = camQx, camQy = camQy, camQz = camQz, camQw = camQw,
                                                                    anchorTx = aTx, anchorTy = aTy, anchorTz = aTz,
                                                                    anchorQx = aQx, anchorQy = aQy, anchorQz = aQz, anchorQw = aQw
                                                                )
                                                            }
                                                            val result = paperTracker.processCycle(
                                                                grayMat = matToProcess,
                                                                intrinsics = intrinsicsData,
                                                                camPoseTx = camTx, camPoseTy = camTy, camPoseTz = camTz,
                                                                camQx = camQx, camQy = camQy, camQz = camQz, camQw = camQw,
                                                                anchorTx = aTx, anchorTy = aTy, anchorTz = aTz,
                                                                anchorQx = aQx, anchorQy = aQy, anchorQz = aQz, anchorQw = aQw,
                                                                planePoint = planePoint,
                                                                planeNormal = planeNormal,
                                                                isTracking = isArTracking,
                                                                isFrozen = curFrozen,
                                                                isEnabled = curEnabled,
                                                                sensitivity = curSens,
                                                                nowMs = System.currentTimeMillis()
                                                            )
                                                            withContext(Dispatchers.Main) {
                                                                paperLockState.updateFromResult(result)
                                                            }
                                                        } catch (e: Throwable) {
                                                            Log.e("TraceAR", "PaperTracker error: ${e.message}", e)
                                                        } finally {
                                                            matToProcess.release()
                                                            isProcessingTrackerCycle.set(false)
                                                        }
                                                    }
                                                } else {
                                                    isProcessingTrackerCycle.set(false)
                                                }
                                            } else {
                                                isProcessingTrackerCycle.set(false)
                                            }
                                        }
                                    }
                                }
                            }

                            // Count detected planes for debug panel
                            val allPlanes = session.getAllTrackables(Plane::class.java)
                            surfaceState.detectedPlaneCount = allPlanes.size
                        }

                        onTrackingFailureChanged = { reason ->
                            surfaceState.trackingFailureReason = reason
                        }

                        onTouchEvent = { motionEvent, _ ->
                            if (motionEvent.action == MotionEvent.ACTION_UP && !isLocked && surfaceState.isPlaneLocked && !calibration.isCalibrated) {
                                val tap = Offset(motionEvent.x, motionEvent.y)
                                handleManualCornerTap(tap, width.toFloat(), height.toFloat())
                            }
                            true
                        }

                        onSessionFailed = { exception ->
                            Log.e("TraceAR", "AR session failed: ${exception.message}", exception)
                            arSessionErrorMessage = exception.localizedMessage ?: "AR session could not start. Please ensure camera is available."
                        }
                    }
                },
                update = { sceneView ->
                    sceneViewRef = sceneView
                    sceneView.planeRenderer.isEnabled = !surfaceState.isPlaneLocked

                    // Destroy & clear previously added AnchorNodes to release Filament GPU resources
                    sceneView.childNodes.filterIsInstance<AnchorNode>().forEach {
                        it.destroy()
                        sceneView.removeChildNode(it)
                    }

                    val surfaceAnchor = calibration.surfaceAnchor

                    // Add placed corner markers during calibration phase
                    if (surfaceAnchor != null && !calibration.isCalibrated) {
                        val anchorNode = AnchorNode(engine = sceneView.engine, anchor = surfaceAnchor)
                        calibration.localCorners.forEach { localPos ->
                            val markerNode = CubeNode(
                                engine = sceneView.engine,
                                size = Float3(0.008f, 0.008f, 0.008f),
                                center = Float3(localPos.x, localPos.y + 0.001f, localPos.z)
                            )
                            anchorNode.addChildNode(markerNode)
                        }
                        sceneView.addChildNode(anchorNode)
                    }

                    // Add tracing overlay when calibrated
                    if (calibration.isCalibrated && surfaceAnchor != null && imageBitmap != null && !tracingUi.isPeeking) {
                        val activeLinesBmp = if (linesOnlyState.isEnabled) linesOnlyState.linesBitmap else null
                        val activeTonalBmp = if (tonalState.isEnabled) tonalState.compositeBitmap else null
                        val overlayNode = createTracingOverlayNode(
                            engine = sceneView.engine,
                            materialLoader = sceneView.materialLoader,
                            surfaceAnchor = surfaceAnchor,
                            localCorners = calibration.localCorners,
                            bitmap = imageBitmap!!,
                            opacity = effectiveOpacity,
                            fitMode = fitMode,
                            debugMode = debugMode,
                            gridState = if (gridState.isEnabled) gridState else null,
                            linesBitmap = activeLinesBmp,
                            tonalBitmap = activeTonalBmp,
                            adjustments = if (!linesOnlyState.isEnabled && !tonalState.isEnabled) adjustments else null,
                            poseFilter = poseFilter,
                            transform = transform,
                            crop = crop,
                            isRulerEnabled = isRulerEnabled,
                            paperLockState = paperLockState,
                            guidesState = if (guidesState.isAnyGuideActive) guidesState else null,
                            zoomScale = zoomState.scale,
                            ambientIntensity = smoothedAmbientIntensity
                        )
                        if (overlayNode != null) {
                            sceneView.addChildNode(overlayNode)
                        }
                    }
                },
                onRelease = { sceneView ->
                    sceneView.childNodes.filterIsInstance<AnchorNode>().forEach {
                        it.destroy()
                        sceneView.removeChildNode(it)
                    }
                    sceneViewRef = null
                }
            )
        }

        // ===== Live OpenCV Paper Detection Outline Overlay =====
        val paper = detectedPaper
        if (paper != null && paper.screenCorners.size == 4) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(paper.screenCorners[0].x, paper.screenCorners[0].y)
                    lineTo(paper.screenCorners[1].x, paper.screenCorners[1].y)
                    lineTo(paper.screenCorners[2].x, paper.screenCorners[2].y)
                    lineTo(paper.screenCorners[3].x, paper.screenCorners[3].y)
                    close()
                }
                val strokeColor = if (paper.isStable) Color(0xFF00E676) else Color(0xFFFFD600)
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
                // Draw corner dots
                paper.screenCorners.forEach { pt ->
                    drawCircle(color = strokeColor, radius = 10f, center = pt)
                }
            }
        }

        // ===== Paper Lock Debug Quad Outline (Cyan: Detected, Yellow: Predicted) =====
        if ((surfaceState.isDebugPanelVisible || debugMode) && calibration.isCalibrated) {
            val sv = sceneViewRef
            val anchor = calibration.surfaceAnchor
            if (sv != null && anchor != null) {
                val detectedQuad = paperLockState.detectedQuadOnPlane
                val predictedQuad = paperLockState.predictedQuadOnPlane

                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw detected quad in Cyan
                    if (detectedQuad != null && detectedQuad.size == 4) {
                        try {
                            val screenPts = detectedQuad.map { pt ->
                                val worldPt = localToWorld(anchor.pose, pt)
                                val sp = sv.cameraNode.worldToScreenPoint(Vector3(worldPt.x, worldPt.y, worldPt.z))
                                Offset(sp.x, sp.y)
                            }
                            val path = Path().apply {
                                moveTo(screenPts[0].x, screenPts[0].y)
                                lineTo(screenPts[1].x, screenPts[1].y)
                                lineTo(screenPts[2].x, screenPts[2].y)
                                lineTo(screenPts[3].x, screenPts[3].y)
                                close()
                            }
                            drawPath(path, color = Color(0xFF00E5FF), style = Stroke(width = 4f, cap = StrokeCap.Round))
                            screenPts.forEach { drawCircle(Color(0xFF00E5FF), radius = 6f, center = it) }
                        } catch (ignored: Throwable) {}
                    }

                    // Draw predicted quad in Yellow dashed lines
                    if (predictedQuad != null && predictedQuad.size == 4) {
                        try {
                            val screenPts = predictedQuad.map { pt ->
                                val worldPt = localToWorld(anchor.pose, pt)
                                val sp = sv.cameraNode.worldToScreenPoint(Vector3(worldPt.x, worldPt.y, worldPt.z))
                                Offset(sp.x, sp.y)
                            }
                            val path = Path().apply {
                                moveTo(screenPts[0].x, screenPts[0].y)
                                lineTo(screenPts[1].x, screenPts[1].y)
                                lineTo(screenPts[2].x, screenPts[2].y)
                                lineTo(screenPts[3].x, screenPts[3].y)
                                close()
                            }
                            drawPath(
                                path,
                                color = Color(0xFFFFD600),
                                style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f))
                            )
                        } catch (ignored: Throwable) {}
                    }
                }
            }
        }

        // ===== Phase 1: Center Crosshair (Before Plane is Locked) =====
        if (!surfaceState.isPlaneLocked) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(54.dp)) {
                    val strokeColor = if (surfaceState.isCandidateValid) Color(0xFF00E676) else Color.White
                    val strokeW = 4f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val armLength = 16f
                    val gap = 6f

                    // Crosshair arms
                    drawLine(strokeColor, Offset(center.x - armLength - gap, center.y), Offset(center.x - gap, center.y), strokeW)
                    drawLine(strokeColor, Offset(center.x + gap, center.y), Offset(center.x + armLength + gap, center.y), strokeW)
                    drawLine(strokeColor, Offset(center.x, center.y - armLength - gap), Offset(center.x, center.y - gap), strokeW)
                    drawLine(strokeColor, Offset(center.x, center.y + gap), Offset(center.x, center.y + armLength + gap), strokeW)
                    drawCircle(strokeColor, radius = 3f, center = center)
                }
            }
        }

        // ===== Pre-Calibration Status Guidance Messages (Top Center, below TopBar) =====
        if (!calibration.isCalibrated && !tracingUi.isFocusMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 58.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Resume Project Guidance Banner
                if (currentProject != null) {
                    Row(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xF01C2128))
                            .border(1.dp, Color(0xFF58A6FF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📋", fontSize = 16.sp)
                        Column {
                            Text(
                                text = "Resuming: ${currentProject?.name}",
                                color = Color(0xFF58A6FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap the same 4 corners of your paper in the same order to continue.",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                val statusMessage = when {
                    surfaceState.trackingFailureReason == TrackingFailureReason.INSUFFICIENT_LIGHT ->
                        "☀️ Too dark — turn on torch or improve lighting"
                    surfaceState.trackingFailureReason == TrackingFailureReason.EXCESSIVE_MOTION ->
                        "🐌 Moving too fast — slow down phone"
                    surfaceState.trackingFailureReason == TrackingFailureReason.INSUFFICIENT_FEATURES ->
                        "🔍 Low texture — point camera at paper"
                    isTrackingLost ->
                        "⚠️ Tracking lost — point camera at paper"
                    !surfaceState.isPlaneLocked ->
                        if (surfaceState.isCandidateValid) "✅ Table surface found! Tap 'Use this surface' below"
                        else "🎯 Point crosshair at paper and move phone slowly"
                    !calibration.isCalibrated -> {
                        val cornerLabels = listOf("top-left", "top-right", "bottom-right", "bottom-left")
                        val nextCorner = calibration.cornerCount
                        if (nextCorner < 4) {
                            "👆 Tap the ${cornerLabels[nextCorner]} corner on the paper (${nextCorner}/4)"
                        } else ""
                    }
                    else -> null
                }

                if (statusMessage != null) {
                    Text(
                        text = statusMessage,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color.White,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Scan Quality Indicator (before plane lock)
                if (!surfaceState.isPlaneLocked) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x99161B22))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        val (color, label) = when (surfaceState.scanQuality) {
                            ScanQuality.LOW -> Color(0xFFD29922) to "Scanning..."
                            ScanQuality.MEDIUM -> Color(0xFF58A6FF) to "Quality: Medium"
                            ScanQuality.GOOD -> Color(0xFF3FB950) to "Quality: Good"
                        }
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                        Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // 8-Second Guidance Tip Card
                if (surfaceState.showScanTips && !surfaceState.isPlaneLocked) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xEE2D1B00))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 Tip: Place paper on a textured surface, improve lighting, and move phone slowly.\nThe overlay follows the paper if it moves. Taping the paper still gives the best result.",
                            color = Color(0xFFFFD600),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // ===== Pre-Calibration Bottom Actions (Phase 1 & 2) =====
        if (!calibration.isCalibrated && !tracingUi.isFocusMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp, start = 14.dp, end = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Phase 1: "Use this surface" & Fallbacks
                if (!surfaceState.isPlaneLocked) {
                    if (surfaceState.isCandidateValid) {
                        Button(
                            onClick = {
                                val candidateHit = surfaceState.candidateHitPose
                                val frame = latestFrameRef.get()
                                if (candidateHit != null && frame != null) {
                                    val anchor = surfaceState.candidatePlane?.createAnchor(candidateHit)
                                    if (anchor != null) {
                                        surfaceState.lockCandidateSurface(anchor)
                                        calibration.bindSurfaceAnchor(anchor)
                                        Log.i("TraceAR", "Surface locked onto table plane")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(0.75f).height(48.dp)
                        ) {
                            Text("📐 Use this surface", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else if (surfaceState.showScanTips) {
                        Button(
                            onClick = {
                                val frame = latestFrameRef.get()
                                val viewW = containerSize.width.toFloat()
                                val viewH = containerSize.height.toFloat()
                                if (frame != null && viewW > 0 && viewH > 0) {
                                    val hitResults = frame.hitTestInstantPlacement(viewW / 2f, viewH / 2f, 0.5f)
                                    val hit = hitResults.firstOrNull()
                                    if (hit != null) {
                                        val anchor = hit.createAnchor()
                                        surfaceState.lockInstantPlacement(anchor, 0.5f, frame.camera.pose)
                                        calibration.bindSurfaceAnchor(anchor)
                                        Toast.makeText(context, "Instant surface locked (approximate)", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30363D)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("⚡ Instant placement (fallback)", fontSize = 12.sp, color = Color(0xFF58A6FF))
                        }
                    }
                }

                // Phase 2: Accept detected paper & Undo
                if (surfaceState.isPlaneLocked && !calibration.isCalibrated) {
                    val stablePaper = detectedPaper
                    if (stablePaper != null && stablePaper.isStable) {
                        Button(
                            onClick = {
                                val anchor = calibration.surfaceAnchor
                                val viewW = containerSize.width.toFloat()
                                val viewH = containerSize.height.toFloat()
                                if (anchor != null && viewW > 0 && viewH > 0) {
                                    val pts = mutableListOf<Vector3f>()
                                    stablePaper.screenCorners.forEach { screenPt ->
                                        val ray = MathUtils.unprojectScreenPointToRay(
                                            screenPt.x,
                                            screenPt.y,
                                            viewW,
                                            viewH,
                                            invViewProjMatrix
                                        )
                                        val res = MathUtils.rayPlaneIntersection(
                                            ray,
                                            surfaceState.lockedPlanePoint,
                                            surfaceState.lockedPlaneNormal,
                                            maxDistance = 2.0f
                                        )
                                        if (res is RayPlaneResult.Hit) {
                                            pts.add(worldToLocal(anchor.pose, res.point))
                                        }
                                    }
                                    if (pts.size == 4) {
                                        calibration.setAllCorners(pts)
                                        Toast.makeText(context, "Paper corners detected & placed!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text("✨ Accept Detected Paper", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    if (calibration.cornerCount > 0) {
                        Button(
                            onClick = { calibration.undoLastCorner() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xCCDA3633)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("↩️  Undo last corner (${calibration.cornerCount}/4)")
                        }
                    }
                }
            }
        }

        // ===== Slim Top Bar (Hidden in Focus Mode) =====
        if (!tracingUi.isFocusMode) {
            TracingTopBar(
                projectName = currentProject?.name ?: "TraceAR Project",
                trackingBadgeColor = trackingBadgeColor,
                trackingBadgeText = trackingBadgeText,
                trackingTip = trackingTip,
                isLowLight = surfaceState.isLowLight,
                isTorchSupported = isTorchSupported,
                isTorchOn = isTorchOn,
                onToggleTorch = { toggleTorch() },
                onBack = {
                    saveProjectData(showToast = false)
                    onBack()
                },
                onToggleFocus = { tracingUi.toggleFocusMode() },
                onRecalibrate = {
                    if (calibration.isCalibrated || surfaceState.isPlaneLocked) {
                        showRecalibDialog = true
                    } else {
                        calibration.reset()
                        surfaceState.reset()
                    }
                },
                onChangeImage = { imagePickerLauncher.launch("image/*") },
                onSaveProject = { saveProjectData(showToast = true) },
                onToggleDebugPanel = { surfaceState.isDebugPanelVisible = !surfaceState.isDebugPanelVisible },
                isDebugPanelVisible = surfaceState.isDebugPanelVisible,
                isLeftHanded = tracingUi.isLeftHanded,
                onToggleLeftHanded = { tracingUi.updateLeftHanded(it) },
                isAutoLineColor = tracingUi.isAutoLineColor,
                onToggleAutoLineColor = { tracingUi.updateAutoLineColor(it) },
                paperTrackingStatus = if (paperLockState.isEnabled) paperLockState.trackingStatus else com.tracear.app.ar.PaperTrackingStatus.OFF,
                onRealignPaper = { realignPaper() },
                onOpenSettings = onOpenSettings,
                onOpenTutorial = onOpenTutorial,
                onOpenExport = { isExportDialogOpen = true },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // On-screen recording indicator chip
            if (timelapseState.isRecording) {
                TimelapseRecordingChip(
                    timelapseState = timelapseState,
                    onClick = { isExportDialogOpen = true },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 56.dp)
                )
            }
        }

        // ===== Paper Lock Non-Blocking Banners =====
        if (calibration.isCalibrated && paperLockState.isEnabled && !tracingUi.isFocusMode && paperLockState.bannerType != PaperBannerType.NONE) {
            val isFound = paperLockState.bannerType == PaperBannerType.PAPER_FOUND
            val bannerBorderColor = if (isFound) Color(0xFF3FB950) else Color(0xFFD29922)
            val bannerTextColor = if (isFound) Color(0xFF3FB950) else Color(0xFFFFD600)
            val bannerText = if (isFound) "✨ Paper found. Re-align here?" else "⚠️ Paper moved? Tap to re-align."

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xF01C2128))
                    .border(1.dp, bannerBorderColor, RoundedCornerShape(12.dp))
                    .clickable { realignPaper() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(bannerText, color = bannerTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("🔄 Tap", color = Color.White, fontSize = 11.sp, textDecoration = TextDecoration.Underline)
            }
        }

        // ===== Tracking Lost Floating Banner (When Calibrated) =====
        if (calibration.isCalibrated && !tracingUi.isFocusMode && isTrackingLost && paperLockState.bannerType == PaperBannerType.NONE) {
            val bannerText = when (failureReason) {
                TrackingFailureReason.INSUFFICIENT_LIGHT -> "☀️ Too dark — turn on torch or brighten room"
                TrackingFailureReason.EXCESSIVE_MOTION -> "🐌 Moving too fast — hold phone steady"
                else -> "⚠️ Tracking lost — point camera at paper"
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xF01C2128))
                    .border(1.dp, Color(0xFFDA3633), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(bannerText, color = Color(0xFFFF7B72), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // ===== Debug Panel Overlay =====
        if (surfaceState.isDebugPanelVisible && !tracingUi.isFocusMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 58.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xDD000000))
                    .padding(10.dp)
            ) {
                Text("🛠️ Debug Metrics", color = Color(0xFF58A6FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("State: ${surfaceState.trackingState}", color = Color.White, fontSize = 10.sp)
                Text("Failure: ${surfaceState.trackingFailureReason ?: "None"}", color = Color.White, fontSize = 10.sp)
                Text("Planes detected: ${surfaceState.detectedPlaneCount}", color = Color.White, fontSize = 10.sp)
                Text("Candidate dist: %.2fm".format(surfaceState.candidateDistance), color = Color.White, fontSize = 10.sp)
                Text("Surface locked: ${surfaceState.isPlaneLocked}", color = Color.White, fontSize = 10.sp)
                Text("Point cloud: ${surfaceState.totalPointCloudCount} pts", color = Color.White, fontSize = 10.sp)
                Text("Depth mode: ${surfaceState.isDepthModeEnabled}", color = Color.White, fontSize = 10.sp)
                Text("Last ray: ${surfaceState.lastRayIntersectionResult}", color = Color(0xFF7EE787), fontSize = 9.sp)

                if (calibration.isCalibrated) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("📄 Paper Lock: ${paperLockState.trackingStatus}", color = Color(0xFF58A6FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Confidence: %.0f%%".format(paperLockState.confidence * 100f), color = Color.White, fontSize = 10.sp)
                    Text("Residual: %.1f mm".format(paperLockState.residualMm), color = Color.White, fontSize = 10.sp)
                    Text("Rate: %.1f Hz (${paperLockState.cycleDurationMs} ms)".format(paperLockState.updateRateHz), color = Color.White, fontSize = 10.sp)

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            paperTracker.simulatePaperShift(0.02f, 0f, 0f)
                            paperLockState.currentPose = paperTracker.lastResult.paperFrame
                            Toast.makeText(context, "Simulated 2cm paper shift!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.height(26.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8957E5)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text("⚡ Simulate shift (2cm)", fontSize = 9.sp, color = Color.White)
                    }
                }
            }
        }

        // ===== Calibrated Tracing UI Controls =====
        if (calibration.isCalibrated) {
            if (!tracingUi.isFocusMode) {
                // 1. Always-visible Quick Actions (Lock/Unlock & Hold-to-Peek)
                TracingQuickActions(
                    isLocked = isLocked,
                    onToggleLock = {
                        isLocked = !isLocked
                        tracingUi.notifyInteraction()
                    },
                    onPeekStart = { tracingUi.isPeeking = true },
                    onPeekEnd = { tracingUi.isPeeking = false },
                    onOpenExport = { isExportDialogOpen = true },
                    isLeftHanded = tracingUi.isLeftHanded,
                    modifier = Modifier
                        .align(
                            if (isLandscape) {
                                if (tracingUi.isLeftHanded) Alignment.CenterStart else Alignment.CenterEnd
                            } else {
                                if (tracingUi.isLeftHanded) Alignment.BottomStart else Alignment.BottomEnd
                            }
                        )
                        .padding(
                            bottom = if (!isLandscape) 76.dp else 16.dp,
                            start = if (tracingUi.isLeftHanded) 8.dp else 0.dp,
                            end = if (!tracingUi.isLeftHanded) 8.dp else 0.dp
                        )
                )

                // 2. Active Category Bottom Sheet
                if (tracingUi.activeCategory != null) {
                    // Full-screen backdrop to detect tap outside to close sheet
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                tracingUi.closeSheet()
                            }
                    )

                    TracingCategorySheet(
                        category = tracingUi.activeCategory!!,
                        onClose = { tracingUi.closeSheet() },
                        onInteract = { tracingUi.notifyInteraction() },
                        opacity = opacity,
                        onOpacityChange = { opacity = it },
                        transform = transform,
                        onTransformChange = { newTransform ->
                            transform = newTransform
                            transformUndoManager.push(newTransform, fitMode, crop)
                        },
                        isLocked = isLocked,
                        onToggleLock = { isLocked = !isLocked },
                        canUndo = transformUndoManager.canUndo,
                        canRedo = transformUndoManager.canRedo,
                        onUndo = {
                            transformUndoManager.undo()?.let { snapshot ->
                                transform = snapshot.transform
                                fitMode = snapshot.fitMode
                                crop = snapshot.crop
                            }
                        },
                        onRedo = {
                            transformUndoManager.redo()?.let { snapshot ->
                                transform = snapshot.transform
                                fitMode = snapshot.fitMode
                                crop = snapshot.crop
                            }
                        },
                        paperPreset = paperPreset,
                        onSelectPreset = { paperPreset = it },
                        paperWidthMeters = paperWidthMeters,
                        paperHeightMeters = paperHeightMeters,
                        onSnapCorners = { preset, targetW, targetH ->
                            previousCornersBeforeSnap = calibration.localCorners.toList()
                            paperPreset = preset
                            val snapped = PaperMath.snapCornersToPreset(calibration.localCorners, targetW, targetH)
                            calibration.setAllCorners(snapped)
                            paperTracker.initBaseline(snapped)
                            paperLockState.resetToIdentity()
                            Toast.makeText(context, "Snapped corners to ${preset.displayName}!", Toast.LENGTH_SHORT).show()
                        },
                        canUndoSnap = (previousCornersBeforeSnap != null),
                        onUndoSnap = {
                            val prev = previousCornersBeforeSnap
                            if (prev != null) {
                                calibration.setAllCorners(prev)
                                paperTracker.initBaseline(prev)
                                paperLockState.resetToIdentity()
                                previousCornersBeforeSnap = null
                                Toast.makeText(context, "Restored original corners", Toast.LENGTH_SHORT).show()
                            }
                        },
                        imageAspect = if (imageBitmap != null) imageBitmap!!.width.toFloat() / imageBitmap!!.height.toFloat() else 1f,
                        onDrawingWidthCmChange = { targetWidthCm ->
                            val imgAspect = if (imageBitmap != null) imageBitmap!!.width.toFloat() / imageBitmap!!.height.toFloat() else 1f
                            val newScale = PaperMath.calculateScaleForDrawingWidth(
                                targetWidthMeters = targetWidthCm / 100f,
                                paperWidthMeters = paperWidthMeters,
                                paperHeightMeters = paperHeightMeters,
                                imageAspect = imgAspect,
                                fitMode = fitMode
                            )
                            val newTransform = transform.copy(scale = newScale)
                            transform = newTransform
                            transformUndoManager.push(newTransform, fitMode, crop)
                        },
                        isRulerEnabled = isRulerEnabled,
                        onToggleRuler = { isRulerEnabled = !isRulerEnabled },
                        crop = crop,
                        onOpenCropDialog = { isCropDialogOpen = true },
                        onResetCrop = {
                            val newCrop = NormalizedCrop()
                            crop = newCrop
                            transformUndoManager.push(transform, fitMode, newCrop)
                            Toast.makeText(context, "Crop reset to full image", Toast.LENGTH_SHORT).show()
                        },
                        gridState = gridState,
                        onFocusActiveSection = { focusActiveSection() },
                        guidesState = guidesState,
                        tonalState = tonalState,
                        sourceBitmap = imageBitmap,
                        adjustments = adjustments,
                        onAdjustmentsChange = { adj ->
                            brightness = adj.brightness
                            contrast = adj.contrast
                            invert = adj.invert
                            bwThreshold = adj.bwThreshold
                        },
                        smoothingMode = smoothingMode,
                        onSmoothingModeChange = { smoothingMode = it },
                        linesOnlyState = linesOnlyState,
                        isAutoLineColor = tracingUi.isAutoLineColor,
                        onToggleAutoLineColor = { tracingUi.updateAutoLineColor(it) },
                        fitMode = fitMode,
                        onFitModeChange = { newMode ->
                            fitMode = newMode
                            transformUndoManager.push(transform, newMode, crop)
                        },
                        zoomScale = zoomState.scale,
                        onResetZoom = { zoomState.reset() },
                        isTorchSupported = isTorchSupported,
                        isTorchOn = isTorchOn,
                        onToggleTorch = { toggleTorch() },
                        isFullBrightness = isFullBrightness,
                        onToggleFullBrightness = { setFullBrightness(!isFullBrightness) },
                        debugMode = debugMode,
                        onToggleDebugMode = { debugMode = !debugMode },
                        isPaperLockEnabled = paperLockState.isEnabled,
                        onTogglePaperLock = { paperLockState.isEnabled = it },
                        isPaperFrozen = paperLockState.isFrozen,
                        onToggleFreezePaper = { paperLockState.isFrozen = it },
                        paperSensitivity = paperLockState.sensitivity,
                        onPaperSensitivityChange = { paperLockState.sensitivity = it },
                        onRealignPaper = { realignPaper() },
                        isLandscape = isLandscape,
                        modifier = Modifier
                            .align(
                                if (isLandscape) {
                                    Alignment.Center
                                } else {
                                    Alignment.BottomCenter
                                }
                            )
                            .padding(bottom = if (!isLandscape) 76.dp else 0.dp)
                    )
                }

                // 3. Compact Dock (Bottom in Portrait, Side in Landscape)
                TracingDock(
                    activeCategory = tracingUi.activeCategory,
                    onSelectCategory = { cat ->
                        tracingUi.toggleCategory(cat)
                    },
                    isLandscape = isLandscape,
                    modifier = Modifier
                        .align(
                            if (isLandscape) {
                                if (tracingUi.isLeftHanded) Alignment.CenterEnd else Alignment.CenterStart
                            } else {
                                Alignment.BottomCenter
                            }
                        )
                        .padding(
                            bottom = if (!isLandscape) 12.dp else 0.dp,
                            start = if (isLandscape && !tracingUi.isLeftHanded) 8.dp else 0.dp,
                            end = if (isLandscape && tracingUi.isLeftHanded) 8.dp else 0.dp
                        )
                )
            } else {
                // Focus Mode: Only tiny restore button visible
                FocusModeRestoreButton(
                    onExitFocus = { tracingUi.isFocusMode = false },
                    isLeftHanded = tracingUi.isLeftHanded,
                    modifier = Modifier.align(if (tracingUi.isLeftHanded) Alignment.TopStart else Alignment.TopEnd)
                )
            }
        }
    }
}

    // --- Crop Tool Dialog ---
    if (isCropDialogOpen && imageBitmap != null) {
        CropDialog(
            bitmap = imageBitmap!!,
            initialCrop = crop,
            onApplyCrop = { newCrop ->
                crop = newCrop
                transformUndoManager.push(transform, fitMode, newCrop)
                isCropDialogOpen = false
            },
            onDismiss = { isCropDialogOpen = false }
        )
    }

    // --- Export, Photo Capture & Timelapse Dialog ---
    ExportDialog(
        isOpen = isExportDialogOpen,
        onDismiss = { isExportDialogOpen = false },
        timelapseState = timelapseState,
        lastCaptureResult = lastCaptureResult,
        isCapturingPhoto = isCapturingPhoto,
        onCapturePhoto = { type -> performPhotoCapture(type) },
        onShareLastPhoto = {
            val r = lastCaptureResult
            if (r?.uri != null) {
                ExportManager.shareUri(context, r.uri, "image/jpeg", "Share Tracing Photo")
            }
        },
        onToggleTimelapseRecording = {
            if (timelapseState.isRecording) {
                timelapseState.stopRecording()
            } else {
                timelapseState.startRecording()
            }
        },
        onExportTimelapseVideo = { performTimelapseExport() },
        onCancelTimelapseExport = { timelapseCancelFlag.set(true) },
        onShareTimelapseVideo = {
            val f = timelapseState.lastExportedFile
            val u = timelapseState.lastExportedUri
            if (f != null && f.exists()) {
                ExportManager.shareFile(context, f, "video/mp4", "Share Timelapse Video")
            } else if (u != null) {
                ExportManager.shareUri(context, u, "video/mp4", "Share Timelapse Video")
            }
        },
        onDiscardTimelapseFrames = {
            scope.launch {
                ExportManager.cleanupTimelapseFrames(context)
                timelapseState.reset()
                Toast.makeText(context, "Timelapse frames discarded", Toast.LENGTH_SHORT).show()
            }
        },
        isLowStorage = isLowStorage
    )

    // --- Re-calibration Confirmation Dialog ---
    if (showRecalibDialog) {
        AlertDialog(
            onDismissRequest = { showRecalibDialog = false },
            title = {
                Text("Re-calibrate Surface?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will unlock the surface, clear your paper corners, and reset grid progress. Are you sure?",
                    color = Color(0xFFC9D1D9)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        sceneViewRef?.let { sv ->
                            sv.childNodes.filterIsInstance<AnchorNode>().forEach { node ->
                                node.destroy()
                                sv.removeChildNode(node)
                            }
                        }
                        calibration.reset()
                        surfaceState.reset()
                        gridState.reset()
                        zoomState.reset()
                        paperDetector.reset()
                        paperLockState.resetToIdentity()
                        paperTracker.initBaseline(emptyList())
                        transform = NormalizedTransform()
                        tracingUi.closeSheet()
                        showRecalibDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDA3633))
                ) {
                    Text("Reset", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecalibDialog = false }) {
                    Text("Cancel", color = Color(0xFF58A6FF))
                }
            },
            containerColor = Color(0xFF1C2128),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // --- AR Session Error Dialog ---
    if (arSessionErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { arSessionErrorMessage = null },
            title = {
                Text("AR Camera Error", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    arSessionErrorMessage ?: "The AR camera session encountered an error. Please ensure camera access is granted and restart.",
                    color = Color(0xFFC9D1D9)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        arSessionErrorMessage = null
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636))
                ) {
                    Text("Return to Projects", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { arSessionErrorMessage = null }) {
                    Text("Dismiss", color = Color(0xFF58A6FF))
                }
            },
            containerColor = Color(0xFF1C2128),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ── Coordinate Conversion Helpers ────────────────────────────

private fun worldToLocal(anchorPose: Pose, worldPoint: Vector3f): Vector3f {
    val q = anchorPose.rotationQuaternion
    val qw = q[3]; val qx = -q[0]; val qy = -q[1]; val qz = -q[2]
    val dx = worldPoint.x - anchorPose.tx()
    val dy = worldPoint.y - anchorPose.ty()
    val dz = worldPoint.z - anchorPose.tz()
    val tx = 2f * (qy * dz - qz * dy)
    val ty = 2f * (qz * dx - qx * dz)
    val tz = 2f * (qx * dy - qy * dx)
    return Vector3f(
        dx + qw * tx + qy * tz - qz * ty,
        dy + qw * ty + qz * tx - qx * tz,
        dz + qw * tz + qx * ty - qy * tx
    )
}

private fun localToWorld(anchorPose: Pose, localPoint: Vector3f): Vector3f {
    val q = anchorPose.rotationQuaternion
    val qw = q[3]; val qx = q[0]; val qy = q[1]; val qz = q[2]
    val lx = localPoint.x; val ly = localPoint.y; val lz = localPoint.z
    val tx = 2f * (qy * lz - qz * ly)
    val ty = 2f * (qz * lx - qx * lz)
    val tz = 2f * (qx * ly - qy * lx)
    return Vector3f(
        anchorPose.tx() + lx + qw * tx + qy * tz - qz * ty,
        anchorPose.ty() + ly + qw * ty + qz * tx - qx * tz,
        anchorPose.tz() + lz + qw * tz + qx * ty - qy * tx
    )
}

/**
 * Geometric bilinear interpolation inside a quadrilateral.
 */
private fun bilinear(tl: Offset, tr: Offset, br: Offset, bl: Offset, u: Float, v: Float): Offset {
    val topX = tl.x + (tr.x - tl.x) * u
    val topY = tl.y + (tr.y - tl.y) * u
    val botX = bl.x + (br.x - bl.x) * u
    val botY = bl.y + (br.y - bl.y) * u
    return Offset(
        topX + (botX - topX) * v,
        topY + (botY - topY) * v
    )
}

/**
 * Checks if a 2D point is inside a convex 4-point polygon.
 */
private fun isPointInQuad(p: Offset, p0: Offset, p1: Offset, p2: Offset, p3: Offset): Boolean {
    val c0 = crossProduct(p0, p1, p)
    val c1 = crossProduct(p1, p2, p)
    val c2 = crossProduct(p2, p3, p)
    val c3 = crossProduct(p3, p0, p)
    return (c0 >= 0 && c1 >= 0 && c2 >= 0 && c3 >= 0) ||
           (c0 <= 0 && c1 <= 0 && c2 <= 0 && c3 <= 0)
}

private fun crossProduct(a: Offset, b: Offset, p: Offset): Float {
    return (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)
}

/**
 * PermissionScreen — shown when camera permission is not granted.
 */
@Composable
fun PermissionScreen(
    isPermanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "📷", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Camera Permission Required",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isPermanentlyDenied) {
                    "Camera permission was denied. Tap 'Open App Settings' below to allow camera access so TraceAR can overlay your drawing."
                } else {
                    "TraceAR needs camera access to show the AR view and overlay your image on real paper."
                },
                color = Color(0xFF8B949E),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (!isPermanentlyDenied) {
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant Permission", color = Color.White)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F6FEB)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("⚙️  Open App Settings", color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30363D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Go Back", color = Color.White)
            }
        }
    }
}

/**
 * ARCoreUnavailableScreen — shown when device does not have ARCore installed or supported.
 */
@Composable
fun ARCoreUnavailableScreen(
    isInstallRequired: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "👓", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isInstallRequired) "AR Services Needed" else "AR Not Supported",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isInstallRequired) {
                    "Google Play Services for AR is required for 3D surface tracking. Please install or update it from Google Play."
                } else {
                    "This device does not support ARCore. Augmented reality tracing requires ARCore-compatible hardware."
                },
                color = Color(0xFF8B949E),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (isInstallRequired) {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.ar.core"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.ar.core"))
                            context.startActivity(intent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Install AR Services", color = Color.White)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30363D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Go Back", color = Color.White)
            }
        }
    }
}


/**
 * Loads a bitmap from URI scaled up to maxDimension (default 4096) for sharp tracing detail.
 * Gracefully falls back if device memory is constrained.
 */
fun loadHighResBitmap(context: Context, uri: Uri, maxDimension: Int = 4096): Bitmap? {
    return try {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }

        var sampleSize = 1
        while (boundsOptions.outWidth / sampleSize > maxDimension || boundsOptions.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (oom: OutOfMemoryError) {
            val fallbackOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize * 2
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, fallbackOpts)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Loads a bitmap from a local file scaled up to maxDimension (default 4096).
 */
fun loadHighResBitmapFromFile(file: File, maxDimension: Int = 4096): Bitmap? {
    return try {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

        var sampleSize = 1
        while (boundsOptions.outWidth / sampleSize > maxDimension || boundsOptions.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        try {
            BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        } catch (oom: OutOfMemoryError) {
            val fallbackOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize * 2
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, fallbackOpts)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
