package com.tracear.app.ui.tracing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracear.app.ar.FitMode
import com.tracear.app.ar.GridState
import com.tracear.app.ar.ImageAdjustments
import com.tracear.app.ar.LineColorOption
import com.tracear.app.ar.LinesOnlyState
import com.tracear.app.ar.PaperMath
import com.tracear.app.ar.PaperPreset
import com.tracear.app.ar.SmoothingMode
import com.tracear.app.data.NormalizedCrop
import com.tracear.app.data.NormalizedTransform
import kotlin.math.roundToInt

/**
 * Compact Category Bottom Sheet (max 30-40% screen height in portrait, semi-transparent):
 * Displays only the controls for the currently selected category.
 * Scrolls internally and includes a header with close button.
 */
@Composable
fun TracingCategorySheet(
    category: DockCategory,
    onClose: () -> Unit,
    onInteract: () -> Unit,
    // Opacity
    opacity: Float,
    onOpacityChange: (Float) -> Unit,
    // Transform
    transform: NormalizedTransform,
    onTransformChange: (NormalizedTransform) -> Unit,
    isLocked: Boolean,
    onToggleLock: () -> Unit,
    // Undo / Redo
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    // Paper Presets & Snapping
    paperPreset: PaperPreset = PaperPreset.A4,
    onSelectPreset: (PaperPreset) -> Unit = {},
    paperWidthMeters: Float = 0.210f,
    paperHeightMeters: Float = 0.297f,
    onSnapCorners: (PaperPreset, Float, Float) -> Unit = { _, _, _ -> },
    canUndoSnap: Boolean = false,
    onUndoSnap: () -> Unit = {},
    // Real-world size
    imageAspect: Float = 1f,
    onDrawingWidthCmChange: (Float) -> Unit = {},
    isRulerEnabled: Boolean = false,
    onToggleRuler: () -> Unit = {},
    // Crop
    crop: NormalizedCrop = NormalizedCrop(),
    onOpenCropDialog: () -> Unit = {},
    onResetCrop: () -> Unit = {},
    // Sections (Grid)
    gridState: GridState,
    onFocusActiveSection: () -> Unit,
    // Adjust
    adjustments: ImageAdjustments,
    onAdjustmentsChange: (ImageAdjustments) -> Unit,
    smoothingMode: SmoothingMode,
    onSmoothingModeChange: (SmoothingMode) -> Unit,
    // Lines
    linesOnlyState: LinesOnlyState,
    isAutoLineColor: Boolean,
    onToggleAutoLineColor: (Boolean) -> Unit,
    // View
    fitMode: FitMode,
    onFitModeChange: (FitMode) -> Unit,
    zoomScale: Float,
    onResetZoom: () -> Unit,
    isTorchSupported: Boolean,
    isTorchOn: Boolean,
    onToggleTorch: () -> Unit,
    isFullBrightness: Boolean,
    onToggleFullBrightness: () -> Unit,
    debugMode: Boolean,
    onToggleDebugMode: () -> Unit,
    // Paper Lock
    isPaperLockEnabled: Boolean = true,
    onTogglePaperLock: (Boolean) -> Unit = {},
    isPaperFrozen: Boolean = false,
    onToggleFreezePaper: (Boolean) -> Unit = {},
    paperSensitivity: com.tracear.app.ar.PaperSensitivity = com.tracear.app.ar.PaperSensitivity.NORMAL,
    onPaperSensitivityChange: (com.tracear.app.ar.PaperSensitivity) -> Unit = {},
    onRealignPaper: () -> Unit = {},
    isLandscape: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth(if (isLandscape) 0.55f else 0.96f)
            .fillMaxHeight(if (isLandscape) 0.55f else 0.38f)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
            .background(Color(0xEE161B22))
            .border(0.5.dp, Color(0x44FFFFFF), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // --- Sheet Header (Drag down to close) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 15f) {
                            onClose()
                        }
                    }
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = category.icon, fontSize = 16.sp)
                Text(
                    text = category.label,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Text("✕", color = Color(0xFF8B949E), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- Sheet Scrollable Content ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            when (category) {
                DockCategory.OPACITY -> {
                    OpacitySheetContent(
                        opacity = opacity,
                        onOpacityChange = {
                            onInteract()
                            onOpacityChange(it)
                        }
                    )
                }
                DockCategory.TRANSFORM -> {
                    TransformSheetContent(
                        transform = transform,
                        onTransformChange = {
                            onInteract()
                            onTransformChange(it)
                        },
                        isLocked = isLocked,
                        onToggleLock = {
                            onInteract()
                            onToggleLock()
                        },
                        canUndo = canUndo,
                        canRedo = canRedo,
                        onUndo = {
                            onInteract()
                            onUndo()
                        },
                        onRedo = {
                            onInteract()
                            onRedo()
                        },
                        paperPreset = paperPreset,
                        onSelectPreset = {
                            onInteract()
                            onSelectPreset(it)
                        },
                        paperWidthMeters = paperWidthMeters,
                        paperHeightMeters = paperHeightMeters,
                        onSnapCorners = { preset, w, h ->
                            onInteract()
                            onSnapCorners(preset, w, h)
                        },
                        canUndoSnap = canUndoSnap,
                        onUndoSnap = {
                            onInteract()
                            onUndoSnap()
                        },
                        imageAspect = imageAspect,
                        fitMode = fitMode,
                        onDrawingWidthCmChange = {
                            onInteract()
                            onDrawingWidthCmChange(it)
                        },
                        isRulerEnabled = isRulerEnabled,
                        onToggleRuler = {
                            onInteract()
                            onToggleRuler()
                        },
                        crop = crop,
                        onOpenCropDialog = {
                            onInteract()
                            onOpenCropDialog()
                        },
                        onResetCrop = {
                            onInteract()
                            onResetCrop()
                        }
                    )
                }
                DockCategory.SECTIONS -> {
                    SectionsSheetContent(
                        gridState = gridState,
                        onFocusActiveSection = {
                            onInteract()
                            onFocusActiveSection()
                        },
                        onInteract = onInteract
                    )
                }
                DockCategory.ADJUST -> {
                    AdjustSheetContent(
                        adjustments = adjustments,
                        onAdjustmentsChange = {
                            onInteract()
                            onAdjustmentsChange(it)
                        },
                        smoothingMode = smoothingMode,
                        onSmoothingModeChange = {
                            onInteract()
                            onSmoothingModeChange(it)
                        }
                    )
                }
                DockCategory.LINES -> {
                    LinesSheetContent(
                        linesOnlyState = linesOnlyState,
                        isAutoLineColor = isAutoLineColor,
                        onToggleAutoLineColor = {
                            onInteract()
                            onToggleAutoLineColor(it)
                        },
                        onInteract = onInteract
                    )
                }
                DockCategory.VIEW -> {
                    ViewSheetContent(
                        fitMode = fitMode,
                        onFitModeChange = {
                            onInteract()
                            onFitModeChange(it)
                        },
                        zoomScale = zoomScale,
                        onResetZoom = {
                            onInteract()
                            onResetZoom()
                        },
                        isTorchSupported = isTorchSupported,
                        isTorchOn = isTorchOn,
                        onToggleTorch = {
                            onInteract()
                            onToggleTorch()
                        },
                        isFullBrightness = isFullBrightness,
                        onToggleFullBrightness = {
                            onInteract()
                            onToggleFullBrightness()
                        },
                        debugMode = debugMode,
                        onToggleDebugMode = {
                            onInteract()
                            onToggleDebugMode()
                        },
                        isPaperLockEnabled = isPaperLockEnabled,
                        onTogglePaperLock = {
                            onInteract()
                            onTogglePaperLock(it)
                        },
                        isPaperFrozen = isPaperFrozen,
                        onToggleFreezePaper = {
                            onInteract()
                            onToggleFreezePaper(it)
                        },
                        paperSensitivity = paperSensitivity,
                        onPaperSensitivityChange = {
                            onInteract()
                            onPaperSensitivityChange(it)
                        },
                        onRealignPaper = {
                            onInteract()
                            onRealignPaper()
                        }
                    )
                }
            }
        }
    }
}

// =====================================================================
// Category 1: Opacity
// =====================================================================
@Composable
private fun OpacitySheetContent(
    opacity: Float,
    onOpacityChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Overlay Transparency", color = Color(0xFF8B949E), fontSize = 12.sp)
        Text("${(opacity * 100).toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }

    Slider(
        value = opacity,
        onValueChange = onOpacityChange,
        valueRange = 0.05f..1f,
        modifier = Modifier.fillMaxWidth().height(32.dp),
        colors = SliderDefaults.colors(
            thumbColor = Color(0xFF58A6FF),
            activeTrackColor = Color(0xFF58A6FF)
        )
    )

    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(0.25f, 0.50f, 0.75f, 1.0f).forEach { preset ->
            Button(
                onClick = { onOpacityChange(preset) },
                modifier = Modifier.weight(1f).height(32.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (kotlin.math.abs(opacity - preset) < 0.05f) Color(0xFF1F6FEB) else Color(0xFF21262D)
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text("${(preset * 100).toInt()}%", fontSize = 11.sp, color = Color.White)
            }
        }
    }
}

// =====================================================================
// Category 2: Transform (Nudge & Rotate, Paper & Size, Crop)
// =====================================================================
private enum class TransformSubTab(val label: String, val icon: String) {
    NUDGE("Nudge & Rotate", "🕹️"),
    PAPER("Paper & Size", "📐"),
    CROP("Crop", "✂️")
}

@Composable
private fun TransformSheetContent(
    transform: NormalizedTransform,
    onTransformChange: (NormalizedTransform) -> Unit,
    isLocked: Boolean,
    onToggleLock: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    paperPreset: PaperPreset,
    onSelectPreset: (PaperPreset) -> Unit,
    paperWidthMeters: Float,
    paperHeightMeters: Float,
    onSnapCorners: (PaperPreset, Float, Float) -> Unit,
    canUndoSnap: Boolean,
    onUndoSnap: () -> Unit,
    imageAspect: Float,
    fitMode: FitMode,
    onDrawingWidthCmChange: (Float) -> Unit,
    isRulerEnabled: Boolean,
    onToggleRuler: () -> Unit,
    crop: NormalizedCrop,
    onOpenCropDialog: () -> Unit,
    onResetCrop: () -> Unit
) {
    var activeSubTab by remember { mutableStateOf(TransformSubTab.NUDGE) }
    val focusManager = LocalFocusManager.current

    // Sub-tab Navigation Bar
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF21262D))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TransformSubTab.entries.forEach { tab ->
            val isSelected = (tab == activeSubTab)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) Color(0xFF1F6FEB) else Color.Transparent)
                    .clickable { activeSubTab = tab }
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${tab.icon} ${tab.label}",
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else Color(0xFF8B949E),
                    maxLines = 1
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    when (activeSubTab) {
        TransformSubTab.NUDGE -> {
            TransformNudgeSubTab(
                transform = transform,
                onTransformChange = onTransformChange,
                isLocked = isLocked,
                onToggleLock = onToggleLock,
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = onUndo,
                onRedo = onRedo,
                paperWidthMeters = paperWidthMeters,
                paperHeightMeters = paperHeightMeters
            )
        }
        TransformSubTab.PAPER -> {
            TransformPaperSubTab(
                paperPreset = paperPreset,
                onSelectPreset = onSelectPreset,
                paperWidthMeters = paperWidthMeters,
                paperHeightMeters = paperHeightMeters,
                onSnapCorners = onSnapCorners,
                canUndoSnap = canUndoSnap,
                onUndoSnap = onUndoSnap,
                transform = transform,
                imageAspect = imageAspect,
                fitMode = fitMode,
                onDrawingWidthCmChange = onDrawingWidthCmChange,
                isRulerEnabled = isRulerEnabled,
                onToggleRuler = onToggleRuler
            )
        }
        TransformSubTab.CROP -> {
            TransformCropSubTab(
                crop = crop,
                onOpenCropDialog = onOpenCropDialog,
                onResetCrop = onResetCrop
            )
        }
    }
}

@Composable
private fun TransformNudgeSubTab(
    transform: NormalizedTransform,
    onTransformChange: (NormalizedTransform) -> Unit,
    isLocked: Boolean,
    onToggleLock: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    paperWidthMeters: Float,
    paperHeightMeters: Float
) {
    val focusManager = LocalFocusManager.current

    // Row 1: Flips, Lock, Undo, Redo
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = { onTransformChange(transform.copy(flipH = !transform.flipH)) },
            modifier = Modifier.weight(1f).height(32.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (transform.flipH) Color(0xFF1F6FEB) else Color(0xFF21262D)
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text("↔ Flip H", fontSize = 10.sp, color = Color.White)
        }

        Button(
            onClick = { onTransformChange(transform.copy(flipV = !transform.flipV)) },
            modifier = Modifier.weight(1f).height(32.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (transform.flipV) Color(0xFF1F6FEB) else Color(0xFF21262D)
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text("↕ Flip V", fontSize = 10.sp, color = Color.White)
        }

        Button(
            onClick = onToggleLock,
            modifier = Modifier.weight(1f).height(32.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isLocked) Color(0xFF238636) else Color(0xFF21262D)
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text(if (isLocked) "🔒 Locked" else "🔓 Edit", fontSize = 10.sp, color = Color.White)
        }

        Button(
            onClick = onUndo,
            enabled = canUndo,
            modifier = Modifier.weight(0.9f).height(32.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF30363D),
                disabledContainerColor = Color(0xFF161B22)
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text("↩ Undo", fontSize = 10.sp, color = if (canUndo) Color.White else Color(0xFF484F58))
        }

        Button(
            onClick = onRedo,
            enabled = canRedo,
            modifier = Modifier.weight(0.9f).height(32.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF30363D),
                disabledContainerColor = Color(0xFF161B22)
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text("↪ Redo", fontSize = 10.sp, color = if (canRedo) Color.White else Color(0xFF484F58))
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Row 2: 1 mm Directional Nudge Pad (Hold to repeat)
    val deltaX = 0.001f / kotlin.math.max(0.01f, paperWidthMeters)
    val deltaY = 0.001f / kotlin.math.max(0.01f, paperHeightMeters)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Nudge 1mm:", color = Color(0xFF8B949E), fontSize = 10.sp, fontWeight = FontWeight.Bold)

        RepeatingButton(
            onClick = { onTransformChange(transform.copy(offsetX = transform.offsetX - deltaX)) },
            modifier = Modifier.weight(1f).height(30.dp),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text("⬅️ Left", fontSize = 10.sp, color = Color.White)
        }

        RepeatingButton(
            onClick = { onTransformChange(transform.copy(offsetY = transform.offsetY - deltaY)) },
            modifier = Modifier.weight(1f).height(30.dp),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text("⬆️ Up", fontSize = 10.sp, color = Color.White)
        }

        RepeatingButton(
            onClick = { onTransformChange(transform.copy(offsetY = transform.offsetY + deltaY)) },
            modifier = Modifier.weight(1f).height(30.dp),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text("⬇️ Down", fontSize = 10.sp, color = Color.White)
        }

        RepeatingButton(
            onClick = { onTransformChange(transform.copy(offsetX = transform.offsetX + deltaX)) },
            modifier = Modifier.weight(1f).height(30.dp),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text("➡️ Right", fontSize = 10.sp, color = Color.White)
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Row 3: Rotation Snaps & Straighten
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(0f, 90f, 180f, 270f).forEach { deg ->
            Button(
                onClick = { onTransformChange(transform.copy(rotationDegrees = deg)) },
                modifier = Modifier.weight(1f).height(28.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text("${deg.toInt()}°", fontSize = 10.sp, color = Color.White)
            }
        }

        // Straighten: align to nearest 90°
        Button(
            onClick = {
                val nearest = (transform.rotationDegrees / 90f).roundToInt() * 90f
                val aligned = ((nearest % 360f) + 360f) % 360f
                onTransformChange(transform.copy(rotationDegrees = aligned))
            },
            modifier = Modifier.weight(1.4f).height(28.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text("📐 Straighten", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Row 4: Fine 0.1° Rotation & Exact Degrees Input
    var exactDegText by remember(transform.rotationDegrees) {
        mutableStateOf(String.format("%.1f", transform.rotationDegrees))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RepeatingButton(
            onClick = {
                val newDeg = ((transform.rotationDegrees - 0.1f) % 360f + 360f) % 360f
                onTransformChange(transform.copy(rotationDegrees = newDeg))
            },
            modifier = Modifier.weight(1.1f).height(32.dp),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text("↺ -0.1°", fontSize = 10.sp, color = Color.White)
        }

        OutlinedTextField(
            value = exactDegText,
            onValueChange = { str ->
                exactDegText = str
                val parsed = str.toFloatOrNull()
                if (parsed != null) {
                    onTransformChange(transform.copy(rotationDegrees = ((parsed % 360f) + 360f) % 360f))
                }
            },
            modifier = Modifier.weight(1.2f).height(46.dp),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            suffix = { Text("°", color = Color(0xFF8B949E), fontSize = 11.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF58A6FF),
                unfocusedBorderColor = Color(0xFF30363D)
            )
        )

        RepeatingButton(
            onClick = {
                val newDeg = (transform.rotationDegrees + 0.1f) % 360f
                onTransformChange(transform.copy(rotationDegrees = newDeg))
            },
            modifier = Modifier.weight(1.1f).height(32.dp),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text("↻ +0.1°", fontSize = 10.sp, color = Color.White)
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Row 5: Fine 0.5% Scale, Numeric Scale %, Quick 10%, and Reset
    var exactScaleText by remember(transform.scale) {
        mutableStateOf(String.format("%.1f", transform.scale * 100f))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RepeatingButton(
            onClick = {
                val s = (transform.scale - 0.005f).coerceIn(0.05f, 10f)
                onTransformChange(transform.copy(scale = s))
            },
            modifier = Modifier.weight(1f).height(32.dp),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text("−0.5%", fontSize = 10.sp, color = Color.White)
        }

        OutlinedTextField(
            value = exactScaleText,
            onValueChange = { str ->
                exactScaleText = str
                val parsed = str.toFloatOrNull()
                if (parsed != null && parsed > 0f) {
                    onTransformChange(transform.copy(scale = (parsed / 100f).coerceIn(0.05f, 10f)))
                }
            },
            modifier = Modifier.weight(1.2f).height(46.dp),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            suffix = { Text("%", color = Color(0xFF8B949E), fontSize = 11.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF58A6FF),
                unfocusedBorderColor = Color(0xFF30363D)
            )
        )

        RepeatingButton(
            onClick = {
                val s = (transform.scale + 0.005f).coerceIn(0.05f, 10f)
                onTransformChange(transform.copy(scale = s))
            },
            modifier = Modifier.weight(1f).height(32.dp),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text("+0.5%", fontSize = 10.sp, color = Color.White)
        }

        Button(
            onClick = { onTransformChange(transform.copy(scale = (transform.scale - 0.1f).coerceIn(0.05f, 10f))) },
            modifier = Modifier.weight(0.9f).height(32.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text("−10%", fontSize = 10.sp, color = Color.White)
        }

        Button(
            onClick = { onTransformChange(transform.copy(scale = (transform.scale + 0.1f).coerceIn(0.05f, 10f))) },
            modifier = Modifier.weight(0.9f).height(32.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text("+10%", fontSize = 10.sp, color = Color.White)
        }

        Button(
            onClick = { onTransformChange(NormalizedTransform()) },
            modifier = Modifier.weight(1f).height(32.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33F0883E)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text("Reset", fontSize = 10.sp, color = Color(0xFFF0883E), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TransformPaperSubTab(
    paperPreset: PaperPreset,
    onSelectPreset: (PaperPreset) -> Unit,
    paperWidthMeters: Float,
    paperHeightMeters: Float,
    onSnapCorners: (PaperPreset, Float, Float) -> Unit,
    canUndoSnap: Boolean,
    onUndoSnap: () -> Unit,
    transform: NormalizedTransform,
    imageAspect: Float,
    fitMode: FitMode,
    onDrawingWidthCmChange: (Float) -> Unit,
    isRulerEnabled: Boolean,
    onToggleRuler: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var showSnapDialog by remember { mutableStateOf(false) }

    var customWidthCm by remember(paperWidthMeters) {
        mutableStateOf(String.format("%.1f", paperWidthMeters * 100f))
    }
    var customHeightCm by remember(paperHeightMeters) {
        mutableStateOf(String.format("%.1f", paperHeightMeters * 100f))
    }

    // 1. Preset Selector Chips
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PaperPreset.entries.forEach { p ->
            val isSelected = (p == paperPreset)
            Button(
                onClick = { onSelectPreset(p) },
                modifier = Modifier.weight(1f).height(30.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color(0xFF1F6FEB) else Color(0xFF21262D)
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text(
                    text = p.displayName,
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // 2. Custom Size Fields (if Custom chosen)
    if (paperPreset == PaperPreset.CUSTOM) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customWidthCm,
                onValueChange = { customWidthCm = it },
                label = { Text("Width (cm)", fontSize = 10.sp) },
                modifier = Modifier.weight(1f).height(50.dp),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            OutlinedTextField(
                value = customHeightCm,
                onValueChange = { customHeightCm = it },
                label = { Text("Height (cm)", fontSize = 10.sp) },
                modifier = Modifier.weight(1f).height(50.dp),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
    }

    // 3. Size Label and Snap Corners Button
    val targetW = if (paperPreset == PaperPreset.CUSTOM) (customWidthCm.toFloatOrNull() ?: 21f) / 100f else paperWidthMeters
    val targetH = if (paperPreset == PaperPreset.CUSTOM) (customHeightCm.toFloatOrNull() ?: 29.7f) / 100f else paperHeightMeters
    val sizeLabel = "${paperPreset.displayName} (${String.format("%.1f × %.1f cm", targetW * 100f, targetH * 100f)})"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = { showSnapDialog = true },
            modifier = Modifier.weight(1.3f).height(36.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636))
        ) {
            Text("📐 Snap to $sizeLabel", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
        }

        if (canUndoSnap) {
            Button(
                onClick = onUndoSnap,
                modifier = Modifier.weight(0.8f).height(36.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30363D))
            ) {
                Text("↩️ Undo snap", fontSize = 11.sp, color = Color(0xFF58A6FF))
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 4. Real-world Drawing Width Control (cm)
    val curWidthCm = PaperMath.getCurrentDrawingWidthCm(paperWidthMeters, paperHeightMeters, imageAspect, fitMode, transform.scale)
    var drawingWidthInput by remember(curWidthCm) {
        mutableStateOf(String.format("%.1f", curWidthCm))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Drawing Width:", color = Color(0xFF8B949E), fontSize = 11.sp, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = drawingWidthInput,
            onValueChange = { str ->
                drawingWidthInput = str
                val parsed = str.toFloatOrNull()
                if (parsed != null && parsed > 0.5f) {
                    onDrawingWidthCmChange(parsed)
                }
            },
            modifier = Modifier.weight(1f).height(46.dp),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
            suffix = { Text("cm", color = Color(0xFF8B949E), fontSize = 11.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF58A6FF),
                unfocusedBorderColor = Color(0xFF30363D)
            )
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 5. On-Paper Ruler Toggle
    Button(
        onClick = onToggleRuler,
        modifier = Modifier.fillMaxWidth().height(36.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRulerEnabled) Color(0xFF1F6FEB) else Color(0xFF21262D)
        )
    ) {
        Text(
            text = if (isRulerEnabled) "📏 On-Paper Ruler: ON" else "📏 On-Paper Ruler: OFF",
            fontSize = 12.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }

    // Confirmation Alert Dialog for Snapping Corners
    if (showSnapDialog) {
        AlertDialog(
            onDismissRequest = { showSnapDialog = false },
            title = {
                Text("Snap Corners to $sizeLabel?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will snap your marked 4 corners into an exact rectangle of that physical size in the plane coordinates, preserving the marked center and orientation.",
                    color = Color(0xFFC9D1D9),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val effW = if (paperPreset == PaperPreset.CUSTOM) (customWidthCm.toFloatOrNull() ?: 21f) / 100f else paperPreset.widthMeters
                        val effH = if (paperPreset == PaperPreset.CUSTOM) (customHeightCm.toFloatOrNull() ?: 29.7f) / 100f else paperPreset.heightMeters
                        onSnapCorners(paperPreset, effW, effH)
                        showSnapDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636))
                ) {
                    Text("Snap Corners", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSnapDialog = false }) {
                    Text("Cancel", color = Color(0xFF58A6FF))
                }
            },
            containerColor = Color(0xFF1C2128),
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
private fun TransformCropSubTab(
    crop: NormalizedCrop,
    onOpenCropDialog: () -> Unit,
    onResetCrop: () -> Unit
) {
    // 1. Crop Region Status Card
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF21262D))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (crop.isCropped) "✂️ Active Crop Region" else "🖼️ Full Image",
                color = if (crop.isCropped) Color(0xFF58A6FF) else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (crop.isCropped)
                    "Tracing ${(crop.right - crop.left) * 100}% × ${(crop.bottom - crop.top) * 100}% region"
                else "Whole image is visible for tracing",
                color = Color(0xFF8B949E),
                fontSize = 11.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 2. Action Buttons: Edit Crop and Reset
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onOpenCropDialog,
            modifier = Modifier.weight(1.3f).height(38.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F6FEB))
        ) {
            Text("✂️ Edit Crop Region", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }

        if (crop.isCropped) {
            Button(
                onClick = onResetCrop,
                modifier = Modifier.weight(1f).height(38.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30363D))
            ) {
                Text("Reset Crop", fontSize = 12.sp, color = Color(0xFFF0883E))
            }
        }
    }
}

// =====================================================================
// Category 3: Sections (Grid)
// =====================================================================
@Composable
private fun SectionsSheetContent(
    gridState: GridState,
    onFocusActiveSection: () -> Unit,
    onInteract: () -> Unit
) {
    // Row 1: Grid Toggle & Grid Size Stepper
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = {
                onInteract()
                gridState.isEnabled = !gridState.isEnabled
            },
            modifier = Modifier.height(34.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (gridState.isEnabled) Color(0xFF238636) else Color(0xFF21262D)
            )
        ) {
            Text(if (gridState.isEnabled) "📐 Grid ON" else "📐 Grid OFF", fontSize = 11.sp, color = Color.White)
        }

        if (gridState.isEnabled) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF21262D))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "−",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        onInteract()
                        gridState.decreaseSize()
                    }.padding(horizontal = 6.dp, vertical = 4.dp)
                )
                Text("${gridState.cols}×${gridState.rows}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "+",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        onInteract()
                        gridState.increaseSize()
                    }.padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }

            Text(
                text = "✓ ${gridState.doneCount}/${gridState.totalCells}",
                color = Color(0xFF3FB950),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (gridState.isEnabled) {
        Spacer(modifier = Modifier.height(6.dp))

        // Row 2: Snake Navigation, Focus & Mark Done
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    onInteract()
                    gridState.activeCell = gridState.prevSnakeIndex(gridState.activeCell)
                },
                modifier = Modifier.weight(1f).height(32.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30363D)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text("◀", fontSize = 12.sp, color = Color.White)
            }

            val secLabel = if (gridState.activeCell >= 0) "Sec ${gridState.activeCell + 1}" else "All"
            Text(secLabel, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)

            Button(
                onClick = {
                    onInteract()
                    gridState.activeCell = gridState.nextSnakeIndex(gridState.activeCell)
                },
                modifier = Modifier.weight(1f).height(32.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30363D)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text("▶", fontSize = 12.sp, color = Color.White)
            }

            Button(
                onClick = onFocusActiveSection,
                modifier = Modifier.weight(1.3f).height(32.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F6FEB)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text("🎯 Focus", fontSize = 11.sp, color = Color.White)
            }

            val isDone = gridState.activeCell >= 0 && gridState.doneCells.contains(gridState.activeCell)
            Button(
                onClick = {
                    onInteract()
                    if (gridState.activeCell >= 0) gridState.toggleDone(gridState.activeCell)
                },
                modifier = Modifier.weight(1.4f).height(32.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDone) Color(0xFF238636) else Color(0xFF30363D)
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text(if (isDone) "✓ Done" else "Mark Done", fontSize = 11.sp, color = Color.White)
            }
        }
    }
}

// =====================================================================
// Category 4: Adjust
// =====================================================================
@Composable
private fun AdjustSheetContent(
    adjustments: ImageAdjustments,
    onAdjustmentsChange: (ImageAdjustments) -> Unit,
    smoothingMode: SmoothingMode,
    onSmoothingModeChange: (SmoothingMode) -> Unit
) {
    // Smoothing row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Overlay Smoothing", color = Color(0xFF8B949E), fontSize = 11.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(SmoothingMode.OFF, SmoothingMode.LOW, SmoothingMode.HIGH).forEach { mode ->
                Button(
                    onClick = { onSmoothingModeChange(mode) },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (smoothingMode == mode) Color(0xFF58A6FF) else Color(0xFF21262D)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text(mode.label, fontSize = 10.sp, color = Color.White)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Brightness Slider
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Brightness", color = Color(0xFF8B949E), fontSize = 11.sp)
        Text("${(adjustments.brightness * 100).toInt()}%", color = Color.White, fontSize = 11.sp)
    }
    Slider(
        value = adjustments.brightness,
        onValueChange = { onAdjustmentsChange(adjustments.copy(brightness = it)) },
        valueRange = -1.0f..1.0f,
        modifier = Modifier.fillMaxWidth().height(26.dp),
        colors = SliderDefaults.colors(thumbColor = Color(0xFF58A6FF), activeTrackColor = Color(0xFF58A6FF))
    )

    // Contrast Slider
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Contrast", color = Color(0xFF8B949E), fontSize = 11.sp)
        Text("${(adjustments.contrast * 100).toInt()}%", color = Color.White, fontSize = 11.sp)
    }
    Slider(
        value = adjustments.contrast,
        onValueChange = { onAdjustmentsChange(adjustments.copy(contrast = it)) },
        valueRange = 0.2f..3.0f,
        modifier = Modifier.fillMaxWidth().height(26.dp),
        colors = SliderDefaults.colors(thumbColor = Color(0xFF58A6FF), activeTrackColor = Color(0xFF58A6FF))
    )

    // Invert Slider
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Invert", color = Color(0xFF8B949E), fontSize = 11.sp)
        Text("${(adjustments.invert * 100).toInt()}%", color = Color.White, fontSize = 11.sp)
    }
    Slider(
        value = adjustments.invert,
        onValueChange = { onAdjustmentsChange(adjustments.copy(invert = it)) },
        valueRange = 0.0f..1.0f,
        modifier = Modifier.fillMaxWidth().height(26.dp),
        colors = SliderDefaults.colors(thumbColor = Color(0xFF58A6FF), activeTrackColor = Color(0xFF58A6FF))
    )

    // B&W Threshold Slider
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("B&W Threshold", color = Color(0xFF8B949E), fontSize = 11.sp)
        Text(if (adjustments.bwThreshold <= 0.01f) "Off" else "${(adjustments.bwThreshold * 100).toInt()}%", color = Color.White, fontSize = 11.sp)
    }
    Slider(
        value = adjustments.bwThreshold,
        onValueChange = { onAdjustmentsChange(adjustments.copy(bwThreshold = it)) },
        valueRange = 0.0f..1.0f,
        modifier = Modifier.fillMaxWidth().height(26.dp),
        colors = SliderDefaults.colors(thumbColor = Color(0xFF58A6FF), activeTrackColor = Color(0xFF58A6FF))
    )

    if (!adjustments.isDefault) {
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = { onAdjustmentsChange(ImageAdjustments()) },
            modifier = Modifier.fillMaxWidth().height(30.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33F0883E))
        ) {
            Text("Reset Adjustments", fontSize = 11.sp, color = Color(0xFFF0883E), fontWeight = FontWeight.Bold)
        }
    }
}

// =====================================================================
// Category 5: Lines (Lines-Only Mode)
// =====================================================================
@Composable
private fun LinesSheetContent(
    linesOnlyState: LinesOnlyState,
    isAutoLineColor: Boolean,
    onToggleAutoLineColor: (Boolean) -> Unit,
    onInteract: () -> Unit
) {
    // Row 1: Lines Toggle & Auto Line Color
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = {
                onInteract()
                linesOnlyState.isEnabled = !linesOnlyState.isEnabled
            },
            modifier = Modifier.height(32.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (linesOnlyState.isEnabled) Color(0xFF238636) else Color(0xFF21262D)
            )
        ) {
            Text(if (linesOnlyState.isEnabled) "✏️ Lines ON" else "✏️ Lines OFF", fontSize = 11.sp, color = Color.White)
        }

        Button(
            onClick = {
                onInteract()
                onToggleAutoLineColor(!isAutoLineColor)
            },
            modifier = Modifier.height(32.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAutoLineColor) Color(0xFF1F6FEB) else Color(0xFF21262D)
            )
        ) {
            Text("⚡ Auto Color", fontSize = 11.sp, color = Color.White)
        }
    }

    if (linesOnlyState.isEnabled) {
        Spacer(modifier = Modifier.height(6.dp))

        // Detail / Sensitivity Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Detail / Sensitivity", color = Color(0xFF8B949E), fontSize = 11.sp)
            Text("${(linesOnlyState.edgeSensitivity * 100).toInt()}%", color = Color.White, fontSize = 11.sp)
        }
        Slider(
            value = linesOnlyState.edgeSensitivity,
            onValueChange = {
                onInteract()
                linesOnlyState.edgeSensitivity = it
            },
            valueRange = 0.1f..1.0f,
            modifier = Modifier.fillMaxWidth().height(26.dp),
            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Line Thickness Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Thickness", color = Color(0xFF8B949E), fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..5).forEach { th ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (linesOnlyState.lineThickness == th) Color(0xFF58A6FF) else Color(0xFF30363D))
                            .clickable {
                                onInteract()
                                linesOnlyState.lineThickness = th
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$th", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Color Palette Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Line Color", color = Color(0xFF8B949E), fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LineColorOption.entries.forEach { option ->
                    val isSelected = linesOnlyState.selectedColorOption == option
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(option.color)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF58A6FF) else Color(0xFF484F58),
                                shape = CircleShape
                            )
                            .clickable {
                                onInteract()
                                linesOnlyState.selectedColorOption = option
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Text(
                                "✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (option == LineColorOption.WHITE || option == LineColorOption.YELLOW || option == LineColorOption.CYAN) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
// Category 6: View
// =====================================================================
@Composable
private fun ViewSheetContent(
    fitMode: FitMode,
    onFitModeChange: (FitMode) -> Unit,
    zoomScale: Float,
    onResetZoom: () -> Unit,
    isTorchSupported: Boolean,
    isTorchOn: Boolean,
    onToggleTorch: () -> Unit,
    isFullBrightness: Boolean,
    onToggleFullBrightness: () -> Unit,
    debugMode: Boolean,
    onToggleDebugMode: () -> Unit,
    isPaperLockEnabled: Boolean = true,
    onTogglePaperLock: (Boolean) -> Unit = {},
    isPaperFrozen: Boolean = false,
    onToggleFreezePaper: (Boolean) -> Unit = {},
    paperSensitivity: com.tracear.app.ar.PaperSensitivity = com.tracear.app.ar.PaperSensitivity.NORMAL,
    onPaperSensitivityChange: (com.tracear.app.ar.PaperSensitivity) -> Unit = {},
    onRealignPaper: () -> Unit = {}
) {
    // Fit Mode row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(FitMode.STRETCH, FitMode.FIT, FitMode.FILL).forEach { mode ->
            val label = when (mode) {
                FitMode.STRETCH -> "Stretch"
                FitMode.FIT -> "Fit"
                FitMode.FILL -> "Fill"
            }
            Button(
                onClick = { onFitModeChange(mode) },
                modifier = Modifier.weight(1f).height(32.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (fitMode == mode) Color(0xFF58A6FF) else Color(0xFF21262D)
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text(label, fontSize = 11.sp, color = Color.White)
            }
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Zoom & Reset row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Zoom Level:", color = Color(0xFF8B949E), fontSize = 11.sp)
            Text("%.1fx".format(zoomScale), color = Color(0xFF58A6FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        if (zoomScale > 1.05f) {
            Button(
                onClick = onResetZoom,
                modifier = Modifier.height(28.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33F0883E)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("Reset Zoom", fontSize = 10.sp, color = Color(0xFFF0883E), fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Toggles: Torch, Full Brightness, Wireframe
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isTorchSupported) {
            Button(
                onClick = onToggleTorch,
                modifier = Modifier.weight(1f).height(32.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTorchOn) Color(0xFFFFD600) else Color(0xFF21262D)
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text(if (isTorchOn) "🔦 Torch ON" else "🔦 Torch", fontSize = 11.sp, color = if (isTorchOn) Color.Black else Color.White)
            }
        }

        Button(
            onClick = onToggleFullBrightness,
            modifier = Modifier.weight(1f).height(32.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFullBrightness) Color(0xFFF0883E) else Color(0xFF21262D)
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text(if (isFullBrightness) "☀️ Max ON" else "☀️ Brightness", fontSize = 11.sp, color = Color.White)
        }

        Button(
            onClick = onToggleDebugMode,
            modifier = Modifier.weight(1f).height(32.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (debugMode) Color(0xFFD29922) else Color(0xFF21262D)
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text(if (debugMode) "🔲 Wire ON" else "🔲 Wireframe", fontSize = 11.sp, color = Color.White)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // ── Paper Lock Section ─────────────────────────────────────
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("📄 Paper Lock", color = Color(0xFF58A6FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { onTogglePaperLock(!isPaperLockEnabled) },
                modifier = Modifier.height(28.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaperLockEnabled) Color(0xFF238636) else Color(0xFF21262D)
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(if (isPaperLockEnabled) "Lock: ON" else "Lock: OFF", fontSize = 10.sp, color = Color.White)
            }

            if (isPaperLockEnabled) {
                Button(
                    onClick = { onToggleFreezePaper(!isPaperFrozen) },
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPaperFrozen) Color(0xFFD29922) else Color(0xFF21262D)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(if (isPaperFrozen) "❄️ Frozen" else "Freeze", fontSize = 10.sp, color = Color.White)
                }
            }
        }
    }

    if (isPaperLockEnabled) {
        Spacer(modifier = Modifier.height(6.dp))

        // Sensitivity selector & Re-align button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.tracear.app.ar.PaperSensitivity.entries.forEach { sens ->
                val isSelected = paperSensitivity == sens
                Button(
                    onClick = { onPaperSensitivityChange(sens) },
                    modifier = Modifier.weight(1f).height(28.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color(0xFF1F6FEB) else Color(0xFF21262D)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text(sens.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp, color = Color.White)
                }
            }

            Button(
                onClick = onRealignPaper,
                modifier = Modifier.height(28.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30363D)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("🔄 Re-align", fontSize = 10.sp, color = Color(0xFF58A6FF), fontWeight = FontWeight.Bold)
            }
        }
    }
}
