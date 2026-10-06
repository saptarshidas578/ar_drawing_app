package com.tracear.app.ui.tracing

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tracear.app.data.NormalizedCrop
import kotlin.math.max
import kotlin.math.min

/**
 * Interactive Dialog for cropping the reference image.
 * Displays draggable rectangle handles over the image with live aspect ratio,
 * allowing precise framing before tracing.
 */
@Composable
fun CropDialog(
    bitmap: Bitmap,
    initialCrop: NormalizedCrop,
    onApplyCrop: (NormalizedCrop) -> Unit,
    onDismiss: () -> Unit
) {
    var crop by remember { mutableStateOf(initialCrop) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFA101418))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✂️ Crop Reference Image",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Text("✕", color = Color(0xFF8B949E), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Interactive Cropper Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val imgAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                        val containerAspect = maxWidth.value / maxHeight.value

                        val (viewW, viewH) = if (imgAspect > containerAspect) {
                            maxWidth to (maxWidth / imgAspect)
                        } else {
                            (maxHeight * imgAspect) to maxHeight
                        }

                        Box(
                            modifier = Modifier
                                .size(viewW, viewH)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            // 1. Base Image
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Source Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )

                            // 2. Interactive Scrim & Draggable Crop Overlay
                            CropOverlay(
                                crop = crop,
                                onCropChange = { crop = it },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cropped Dimension Info
                val origW = bitmap.width
                val origH = bitmap.height
                val activeW = ((crop.right - crop.left) * origW).toInt()
                val activeH = ((crop.bottom - crop.top) * origH).toInt()
                Text(
                    text = "Region: $activeW × $activeH px (${String.format("%.1f", (crop.right - crop.left) * 100f)}% × ${String.format("%.1f", (crop.bottom - crop.top) * 100f)}%)",
                    color = Color(0xFF8B949E),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { crop = NormalizedCrop() },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D))
                    ) {
                        Text("Reset Crop", color = Color(0xFFF0883E), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onApplyCrop(crop)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.3f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636))
                    ) {
                        Text("Apply Crop", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private enum class DragHandle { NONE, INSIDE, TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT, TOP, BOTTOM, LEFT, RIGHT }

@Composable
private fun CropOverlay(
    crop: NormalizedCrop,
    onCropChange: (NormalizedCrop) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeHandle by remember { mutableStateOf(DragHandle.NONE) }

    Canvas(
        modifier = modifier.pointerInput(crop) {
            val minDim = 0.06f // minimum 6% normalized size

            detectDragGestures(
                onDragStart = { pos ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    if (w <= 0f || h <= 0f) return@detectDragGestures

                    val x = pos.x / w
                    val y = pos.y / h
                    val hitRadius = 28f / min(w, h)

                    val nearL = kotlin.math.abs(x - crop.left) < hitRadius
                    val nearR = kotlin.math.abs(x - crop.right) < hitRadius
                    val nearT = kotlin.math.abs(y - crop.top) < hitRadius
                    val nearB = kotlin.math.abs(y - crop.bottom) < hitRadius

                    activeHandle = when {
                        nearL && nearT -> DragHandle.TOP_LEFT
                        nearR && nearT -> DragHandle.TOP_RIGHT
                        nearR && nearB -> DragHandle.BOTTOM_RIGHT
                        nearL && nearB -> DragHandle.BOTTOM_LEFT
                        nearT && x > crop.left && x < crop.right -> DragHandle.TOP
                        nearB && x > crop.left && x < crop.right -> DragHandle.BOTTOM
                        nearL && y > crop.top && y < crop.bottom -> DragHandle.LEFT
                        nearR && y > crop.top && y < crop.bottom -> DragHandle.RIGHT
                        x > crop.left && x < crop.right && y > crop.top && y < crop.bottom -> DragHandle.INSIDE
                        else -> DragHandle.NONE
                    }
                },
                onDragEnd = { activeHandle = DragHandle.NONE },
                onDragCancel = { activeHandle = DragHandle.NONE },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    if (w <= 0f || h <= 0f) return@detectDragGestures

                    val dx = dragAmount.x / w
                    val dy = dragAmount.y / h

                    var l = crop.left
                    var t = crop.top
                    var r = crop.right
                    var b = crop.bottom

                    when (activeHandle) {
                        DragHandle.INSIDE -> {
                            val boxW = r - l
                            val boxH = b - t
                            l = (l + dx).coerceIn(0f, 1f - boxW)
                            r = l + boxW
                            t = (t + dy).coerceIn(0f, 1f - boxH)
                            b = t + boxH
                        }
                        DragHandle.TOP_LEFT -> {
                            l = (l + dx).coerceIn(0f, r - minDim)
                            t = (t + dy).coerceIn(0f, b - minDim)
                        }
                        DragHandle.TOP_RIGHT -> {
                            r = (r + dx).coerceIn(l + minDim, 1f)
                            t = (t + dy).coerceIn(0f, b - minDim)
                        }
                        DragHandle.BOTTOM_RIGHT -> {
                            r = (r + dx).coerceIn(l + minDim, 1f)
                            b = (b + dy).coerceIn(t + minDim, 1f)
                        }
                        DragHandle.BOTTOM_LEFT -> {
                            l = (l + dx).coerceIn(0f, r - minDim)
                            b = (b + dy).coerceIn(t + minDim, 1f)
                        }
                        DragHandle.TOP -> {
                            t = (t + dy).coerceIn(0f, b - minDim)
                        }
                        DragHandle.BOTTOM -> {
                            b = (b + dy).coerceIn(t + minDim, 1f)
                        }
                        DragHandle.LEFT -> {
                            l = (l + dx).coerceIn(0f, r - minDim)
                        }
                        DragHandle.RIGHT -> {
                            r = (r + dx).coerceIn(l + minDim, 1f)
                        }
                        DragHandle.NONE -> {}
                    }

                    onCropChange(NormalizedCrop(l, t, r, b))
                }
            )
        }
    ) {
        val w = size.width
        val h = size.height

        val l = crop.left * w
        val t = crop.top * h
        val r = crop.right * w
        val b = crop.bottom * h

        val scrimColor = Color(0x99000000)

        // Draw 4 outer dimming scrims
        drawRect(scrimColor, Offset(0f, 0f), Size(w, t)) // Top
        drawRect(scrimColor, Offset(0f, b), Size(w, h - b)) // Bottom
        drawRect(scrimColor, Offset(0f, t), Size(l, b - t)) // Left
        drawRect(scrimColor, Offset(r, t), Size(w - r, b - t)) // Right

        // Draw Crop Border
        val borderColor = Color(0xFF58A6FF)
        drawRect(
            color = borderColor,
            topLeft = Offset(l, t),
            size = Size(r - l, b - t),
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw Rule of Thirds Gridlines inside Crop Box
        val gridColor = Color(0x55FFFFFF)
        val thirdW = (r - l) / 3f
        val thirdH = (b - t) / 3f

        drawLine(gridColor, Offset(l + thirdW, t), Offset(l + thirdW, b), 1.5f)
        drawLine(gridColor, Offset(l + 2 * thirdW, t), Offset(l + 2 * thirdW, b), 1.5f)
        drawLine(gridColor, Offset(l, t + thirdH), Offset(r, t + thirdH), 1.5f)
        drawLine(gridColor, Offset(l, t + 2 * thirdH), Offset(r, t + 2 * thirdH), 1.5f)

        // Draw Corner and Edge Handle Markers
        val handleColor = Color.White
        val handleRadius = 6.dp.toPx()

        drawCircle(handleColor, handleRadius, Offset(l, t))
        drawCircle(handleColor, handleRadius, Offset(r, t))
        drawCircle(handleColor, handleRadius, Offset(r, b))
        drawCircle(handleColor, handleRadius, Offset(l, b))

        val edgeRadius = 4.dp.toPx()
        drawCircle(handleColor, edgeRadius, Offset((l + r) / 2f, t))
        drawCircle(handleColor, edgeRadius, Offset((l + r) / 2f, b))
        drawCircle(handleColor, edgeRadius, Offset(l, (t + b) / 2f))
        drawCircle(handleColor, edgeRadius, Offset(r, (t + b) / 2f))
    }
}
