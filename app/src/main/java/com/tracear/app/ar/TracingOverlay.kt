package com.tracear.app.ar

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.RenderableManager
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.google.android.filament.VertexBuffer
import com.google.ar.core.Anchor
import com.google.ar.core.Pose
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.node.MeshNode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// ── Image fit modes ──────────────────────────────────────────
enum class FitMode { STRETCH, FIT, FILL }

// ── Public entry point ───────────────────────────────────────

/**
 * Creates ONE AnchorNode with a child MeshNode that renders the reference image
 * (and optional section grid) as a custom quad mesh on the locked table plane.
 *
 * All 4 corners are expressed in the local coordinate space of the single table anchor.
 */
fun createTracingOverlayNode(
    engine: Engine,
    materialLoader: MaterialLoader,
    surfaceAnchor: Anchor,
    localCorners: List<Vector3f>,
    bitmap: Bitmap,
    opacity: Float,
    fitMode: FitMode = FitMode.STRETCH,
    debugMode: Boolean = false,
    gridState: GridState? = null,
    linesBitmap: Bitmap? = null,
    tonalBitmap: Bitmap? = null,
    adjustments: ImageAdjustments? = null,
    poseFilter: PoseFilter? = null,
    transform: com.tracear.app.data.NormalizedTransform? = null,
    crop: com.tracear.app.data.NormalizedCrop? = null,
    isRulerEnabled: Boolean = false,
    paperLockState: PaperLockState? = null,
    guidesState: GuidesState? = null,
    zoomScale: Float = 1f,
    ambientIntensity: Float = 0.5f
): AnchorNode? {
    if (localCorners.size < 4) return null

    val effectiveBitmap = tonalBitmap ?: (linesBitmap ?: bitmap)

    return try {
        // 1. Sort clockwise: TL, TR, BR, BL in plane local space
        val sorted = MathUtils.sortCornersClockwise(localCorners)

        // 2. Calculate physical paper aspect ratio and dimensions in meters
        val topW = (sorted[1] - sorted[0]).length()
        val botW = (sorted[2] - sorted[3]).length()
        val leftH = (sorted[3] - sorted[0]).length()
        val rightH = (sorted[2] - sorted[1]).length()
        val paperAspect = if (leftH + rightH > 0.001f) (topW + botW) / (leftH + rightH) else 1f
        val paperWidthMeters = (topW + botW) / 2f
        val paperHeightMeters = (leftH + rightH) / 2f

        val lift = 0.001f // 1 mm above the surface to prevent z-fighting

        // 3. Prepare bitmap with opacity, fit mode, adjustments, AR grid, crop, ruler, and drawing guides
        val finalBitmap = prepareBitmap(
            src = effectiveBitmap,
            opacity = opacity,
            fitMode = fitMode,
            paperAspect = paperAspect,
            paperWidthMeters = paperWidthMeters,
            paperHeightMeters = paperHeightMeters,
            gridState = gridState,
            debugMode = debugMode,
            adjustments = if (linesBitmap == null && tonalBitmap == null) adjustments else null,
            transform = transform,
            crop = crop,
            isRulerEnabled = isRulerEnabled,
            guidesState = guidesState,
            zoomScale = zoomScale,
            ambientIntensity = ambientIntensity
        )

        // 4. Vertex buffer: TL=(0,0)  TR=(1,0)  BR=(1,1)  BL=(0,1)
        val vbData = ByteBuffer.allocateDirect(4 * 5 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        val uvs = arrayOf(
            0f to 0f, // TL
            1f to 0f, // TR
            1f to 1f, // BR
            0f to 1f  // BL
        )

        sorted.forEachIndexed { i, p ->
            vbData.put(p.x)
            vbData.put(p.y + lift)
            vbData.put(p.z)
            vbData.put(uvs[i].first)
            vbData.put(uvs[i].second)
        }
        vbData.rewind()

        val ibData = ByteBuffer.allocateDirect(6 * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
        ibData.put(shortArrayOf(0, 1, 2, 0, 2, 3))
        ibData.rewind()

        // 5. Build Filament vertex & index buffers
        val vertexBuffer = VertexBuffer.Builder()
            .vertexCount(4)
            .bufferCount(1)
            .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 5 * 4)
            .attribute(VertexBuffer.VertexAttribute.UV0,      0, VertexBuffer.AttributeType.FLOAT2, 3 * 4, 5 * 4)
            .build(engine)
        vertexBuffer.setBufferAt(engine, 0, vbData)

        val indexBuffer = IndexBuffer.Builder()
            .indexCount(6)
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)
            .build(engine)
        indexBuffer.setBuffer(engine, ibData)

        // 6. Upload bitmap as Filament texture with mipmaps
        val texture = bitmapToFilamentTexture(engine, finalBitmap)
        // Immediately recycle JVM intermediate bitmap after uploading to GPU
        finalBitmap.recycle()

        // 7. Create material instance using trilinear filtering
        val sampler = TextureSampler(
            TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,
            TextureSampler.MagFilter.LINEAR,
            TextureSampler.WrapMode.CLAMP_TO_EDGE
        )
        val matInstance = materialLoader.createImageInstance(texture, sampler)

        // 8. Build bounding box from local vertices
        val allX = sorted.map { it.x }
        val allY = sorted.map { it.y }
        val allZ = sorted.map { it.z }
        val cx = (allX.min() + allX.max()) / 2f
        val cy = (allY.min() + allY.max()) / 2f + lift
        val cz = (allZ.min() + allZ.max()) / 2f
        val hx = (allX.max() - allX.min()) / 2f + 0.01f
        val hy = 0.01f
        val hz = (allZ.max() - allZ.min()) / 2f + 0.01f
        val boundingBox = Box(cx, cy, cz, hx, hy, hz)

        // 9. Create SceneView MeshNode
        val meshNode = MeshNode(
            engine           = engine,
            primitiveType    = RenderableManager.PrimitiveType.TRIANGLES,
            vertexBuffer     = vertexBuffer,
            indexBuffer      = indexBuffer,
            boundingBox      = boundingBox,
            materialInstance = matInstance
        )

        // 10. Attach MeshNode to the custom TracingOverlayNode that frees Filament resources on destroy
        val anchorNode = TracingOverlayNode(
            engine = engine,
            anchor = surfaceAnchor,
            texture = texture,
            vertexBuffer = vertexBuffer,
            indexBuffer = indexBuffer,
            materialInstance = matInstance
        )
        anchorNode.addChildNode(meshNode)

        val centroid = PaperFrame.calculateCentroid(sorted)
        val hasPoseFilter = poseFilter != null && poseFilter.mode != SmoothingMode.OFF
        val hasPaperLock = paperLockState != null

        if (hasPoseFilter || hasPaperLock) {
            anchorNode.updateAnchorPose = false

            fun computeCurrentPose(anchor: Anchor): Pose {
                val baseAnchorPose = if (hasPoseFilter) {
                    poseFilter!!.filter(anchor.pose, System.nanoTime())
                } else {
                    anchor.pose
                }

                val frame = paperLockState?.currentPose
                return if (paperLockState != null && paperLockState.isEnabled && frame != null && !frame.isIdentity()) {
                    val paperLocal = computePaperLocalPose(frame, centroid)
                    baseAnchorPose.compose(paperLocal)
                } else {
                    baseAnchorPose
                }
            }

            anchorNode.pose = computeCurrentPose(surfaceAnchor)
            anchorNode.onUpdated = { anchor ->
                anchorNode.pose = computeCurrentPose(anchor)
            }
        } else {
            anchorNode.updateAnchorPose = true
        }

        anchorNode
    } catch (e: Throwable) {
        Log.e("TraceAR", "createTracingOverlayNode failed: ${e.message}", e)
        null
    }
}

/**
 * TracingOverlayNode — AnchorNode subclass that explicitly destroys native Filament
 * GPU resources (Texture, VertexBuffer, IndexBuffer, MaterialInstance) when removed.
 */
class TracingOverlayNode(
    engine: Engine,
    anchor: Anchor,
    private val texture: Texture,
    private val vertexBuffer: VertexBuffer,
    private val indexBuffer: IndexBuffer,
    private val materialInstance: com.google.android.filament.MaterialInstance
) : AnchorNode(engine = engine, anchor = anchor) {

    override fun destroy() {
        super.destroy()
        try {
            engine.destroyTexture(texture)
        } catch (ignored: Throwable) {}
        try {
            engine.destroyVertexBuffer(vertexBuffer)
        } catch (ignored: Throwable) {}
        try {
            engine.destroyIndexBuffer(indexBuffer)
        } catch (ignored: Throwable) {}
        try {
            engine.destroyMaterialInstance(materialInstance)
        } catch (ignored: Throwable) {}
    }
}

private fun computePaperLocalPose(paperFrame: PaperFrame, centroid: Vector3f): Pose {
    val tMinus = Pose.makeTranslation(-centroid.x, -centroid.y, -centroid.z)
    val radHalf = Math.toRadians((paperFrame.rotationDegrees / 2.0)).toFloat()
    val rYaw = Pose.makeRotation(0f, kotlin.math.sin(radHalf), 0f, kotlin.math.cos(radHalf))
    val tPlus = Pose.makeTranslation(centroid.x, centroid.y, centroid.z)
    val tDelta = Pose.makeTranslation(paperFrame.dx, 0f, paperFrame.dz)
    return tDelta.compose(tPlus).compose(rYaw).compose(tMinus)
}

// ── Helpers ──────────────────────────────────────────────────

/**
 * Prepares the reference bitmap by:
 * 1. Adjusting for FitMode (STRETCH, FIT, FILL) relative to physical paperAspect.
 * 2. Applying the user's opacity level and optional hardware-accelerated image adjustments.
 * 3. Drawing section grid lines, cell numbers, active section highlight, and done checkmarks in AR.
 */
private fun prepareBitmap(
    src: Bitmap,
    opacity: Float,
    fitMode: FitMode,
    paperAspect: Float,
    paperWidthMeters: Float = 0f,
    paperHeightMeters: Float = 0f,
    gridState: GridState?,
    debugMode: Boolean,
    adjustments: ImageAdjustments? = null,
    transform: com.tracear.app.data.NormalizedTransform? = null,
    crop: com.tracear.app.data.NormalizedCrop? = null,
    isRulerEnabled: Boolean = false,
    guidesState: GuidesState? = null,
    zoomScale: Float = 1f,
    ambientIntensity: Float = 0.5f
): Bitmap {
    val srcRect: android.graphics.Rect?
    val cropW: Int
    val cropH: Int

    if (crop != null && crop.isCropped) {
        val l = (crop.left.coerceIn(0f, 1f) * src.width).toInt().coerceIn(0, src.width - 1)
        val t = (crop.top.coerceIn(0f, 1f) * src.height).toInt().coerceIn(0, src.height - 1)
        val r = (crop.right.coerceIn(0f, 1f) * src.width).toInt().coerceIn(l + 1, src.width)
        val b = (crop.bottom.coerceIn(0f, 1f) * src.height).toInt().coerceIn(t + 1, src.height)
        srcRect = android.graphics.Rect(l, t, r, b)
        cropW = srcRect.width().coerceAtLeast(1)
        cropH = srcRect.height().coerceAtLeast(1)
    } else {
        srcRect = null
        cropW = src.width
        cropH = src.height
    }

    val targetW: Int
    val targetH: Int

    when (fitMode) {
        FitMode.STRETCH -> {
            targetW = cropW
            targetH = cropH
        }
        FitMode.FIT -> {
            val imgAspect = cropW.toFloat() / cropH.toFloat()
            if (imgAspect > paperAspect) {
                targetW = cropW
                targetH = (cropW / paperAspect).toInt().coerceAtLeast(1)
            } else {
                targetH = cropH
                targetW = (cropH * paperAspect).toInt().coerceAtLeast(1)
            }
        }
        FitMode.FILL -> {
            val imgAspect = cropW.toFloat() / cropH.toFloat()
            if (imgAspect > paperAspect) {
                targetH = cropH
                targetW = (cropH * paperAspect).toInt().coerceAtLeast(1)
            } else {
                targetW = cropW
                targetH = (cropW / paperAspect).toInt().coerceAtLeast(1)
            }
        }
    }

    val out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)

    // 1. Draw base image with opacity, adjustments, transform & fit mode
    val imagePaint = Paint().apply {
        alpha = (opacity.coerceIn(0.05f, 1f) * 255).toInt()
        isFilterBitmap = true
        if (adjustments != null && !adjustments.isDefault) {
            colorFilter = ColorMatrixColorFilter(adjustments.toColorMatrix())
        }
    }

    canvas.save()
    if (transform != null) {
        val cx = targetW / 2f
        val cy = targetH / 2f
        canvas.translate(transform.offsetX * targetW, transform.offsetY * targetH)
        canvas.rotate(transform.rotationDegrees, cx, cy)
        val sx = transform.scale * (if (transform.flipH) -1f else 1f)
        val sy = transform.scale * (if (transform.flipV) -1f else 1f)
        canvas.scale(sx, sy, cx, cy)
    }

    when (fitMode) {
        FitMode.STRETCH -> {
            val dstRect = RectF(0f, 0f, targetW.toFloat(), targetH.toFloat())
            canvas.drawBitmap(src, srcRect, dstRect, imagePaint)
        }
        FitMode.FIT -> {
            val scale = min(targetW.toFloat() / cropW, targetH.toFloat() / cropH)
            val drawW = cropW * scale
            val drawH = cropH * scale
            val left = (targetW - drawW) / 2f
            val top = (targetH - drawH) / 2f
            canvas.drawBitmap(src, srcRect, RectF(left, top, left + drawW, top + drawH), imagePaint)
        }
        FitMode.FILL -> {
            val scale = max(targetW.toFloat() / cropW, targetH.toFloat() / cropH)
            val drawW = cropW * scale
            val drawH = cropH * scale
            val left = (targetW - drawW) / 2f
            val top = (targetH - drawH) / 2f
            canvas.drawBitmap(src, srcRect, RectF(left, top, left + drawW, top + drawH), imagePaint)
        }
    }
    canvas.restore()

    // 2. Draw Section Grid if enabled
    if (gridState != null && gridState.isEnabled) {
        drawGridOverlay(canvas, targetW, targetH, gridState)
    }

    // 3. Draw On-Paper Metric Ruler if enabled
    if (isRulerEnabled && paperWidthMeters > 0.01f && paperHeightMeters > 0.01f) {
        drawRulerOverlay(canvas, targetW, targetH, paperWidthMeters, paperHeightMeters)
    }

    // 4. Draw Drawing Guides if enabled
    if (guidesState != null && guidesState.isAnyGuideActive) {
        val imgRect = when (fitMode) {
            FitMode.STRETCH -> RectF(0f, 0f, targetW.toFloat(), targetH.toFloat())
            FitMode.FIT -> {
                val scale = min(targetW.toFloat() / cropW, targetH.toFloat() / cropH)
                val drawW = cropW * scale
                val drawH = cropH * scale
                val left = (targetW - drawW) / 2f
                val top = (targetH - drawH) / 2f
                RectF(left, top, left + drawW, top + drawH)
            }
            FitMode.FILL -> {
                val scale = max(targetW.toFloat() / cropW, targetH.toFloat() / cropH)
                val drawW = cropW * scale
                val drawH = cropH * scale
                val left = (targetW - drawW) / 2f
                val top = (targetH - drawH) / 2f
                RectF(left, top, left + drawW, top + drawH)
            }
        }

        if (guidesState.moveWithImage) {
            canvas.save()
            if (transform != null) {
                val cx = targetW / 2f
                val cy = targetH / 2f
                canvas.translate(transform.offsetX * targetW, transform.offsetY * targetH)
                canvas.rotate(transform.rotationDegrees, cx, cy)
                val sx = transform.scale * (if (transform.flipH) -1f else 1f)
                val sy = transform.scale * (if (transform.flipV) -1f else 1f)
                canvas.scale(sx, sy, cx, cy)
            }
            drawDrawingGuidesOverlay(
                canvas = canvas,
                width = targetW,
                height = targetH,
                bounds = imgRect,
                guidesState = guidesState,
                zoomScale = zoomScale,
                ambientIntensity = ambientIntensity,
                paperWidthMeters = paperWidthMeters,
                paperHeightMeters = paperHeightMeters
            )
            canvas.restore()
        } else {
            val paperRect = RectF(0f, 0f, targetW.toFloat(), targetH.toFloat())
            drawDrawingGuidesOverlay(
                canvas = canvas,
                width = targetW,
                height = targetH,
                bounds = paperRect,
                guidesState = guidesState,
                zoomScale = zoomScale,
                ambientIntensity = ambientIntensity,
                paperWidthMeters = paperWidthMeters,
                paperHeightMeters = paperHeightMeters
            )
        }
    }

    // 4. Draw Debug Corner Border if debug mode on
    if (debugMode) {
        val debugPaint = Paint().apply {
            color = Color.YELLOW
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRect(0f, 0f, targetW.toFloat(), targetH.toFloat(), debugPaint)
    }

    return out
}

/**
 * Draws the section grid, cell numbers, active cell highlight, and done checkmarks directly onto the AR overlay bitmap.
 */
private fun drawGridOverlay(canvas: Canvas, width: Int, height: Int, gridState: GridState) {
    val cols = gridState.cols
    val rows = gridState.rows
    val totalCells = gridState.totalCells
    val activeCell = gridState.activeCell

    val cellW = width.toFloat() / cols
    val cellH = height.toFloat() / rows

    val minDim = min(width, height).toFloat()
    val baseStroke = (minDim * 0.003f).coerceIn(2.5f, 6f)

    val dimPaint = Paint().apply {
        color = Color.argb(85, 0, 0, 0)
        style = Paint.Style.FILL
    }

    val donePaint = Paint().apply {
        color = Color.argb(60, 0, 230, 118)
        style = Paint.Style.FILL
    }

    val activeTintPaint = Paint().apply {
        color = Color.argb(40, 0, 229, 255)
        style = Paint.Style.FILL
    }

    val activeBorderPaint = Paint().apply {
        color = Color.rgb(0, 229, 255)
        style = Paint.Style.STROKE
        strokeWidth = (baseStroke * 2.5f).coerceIn(5f, 14f)
    }

    val gridShadowPaint = Paint().apply {
        color = Color.argb(160, 0, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = baseStroke + 2f
    }

    val gridLinePaint = Paint().apply {
        color = Color.argb(210, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = baseStroke
    }

    val textOutlinePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = (baseStroke * 1.5f).coerceIn(4f, 10f)
        textSize = (min(cellW, cellH) * 0.22f).coerceIn(22f, 72f)
        typeface = Typeface.DEFAULT_BOLD
    }

    val textFillPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textSize = textOutlinePaint.textSize
        typeface = Typeface.DEFAULT_BOLD
    }

    val checkmarkPaint = Paint().apply {
        color = Color.rgb(0, 230, 118)
        style = Paint.Style.FILL
        textSize = (min(cellW, cellH) * 0.28f).coerceIn(24f, 80f)
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(4f, 1f, 1f, Color.BLACK)
    }

    // Step A: Cell backgrounds (dim non-active, tint done, tint active)
    for (i in 0 until totalCells) {
        val (col, row) = gridState.cellToColRow(i)
        val left = col * cellW
        val top = row * cellH
        val right = left + cellW
        val bottom = top + cellH
        val rect = RectF(left, top, right, bottom)

        val isDone = gridState.doneCells.contains(i)
        val isActive = (i == activeCell)

        if (activeCell >= 0 && !isActive) {
            canvas.drawRect(rect, dimPaint)
        }

        if (isDone) {
            canvas.drawRect(rect, donePaint)
        }

        if (isActive) {
            canvas.drawRect(rect, activeTintPaint)
        }
    }

    // Step B: Grid lines
    for (c in 1 until cols) {
        val x = c * cellW
        canvas.drawLine(x, 0f, x, height.toFloat(), gridShadowPaint)
        canvas.drawLine(x, 0f, x, height.toFloat(), gridLinePaint)
    }
    for (r in 1 until rows) {
        val y = r * cellH
        canvas.drawLine(0f, y, width.toFloat(), y, gridShadowPaint)
        canvas.drawLine(0f, y, width.toFloat(), y, gridLinePaint)
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gridShadowPaint)
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gridLinePaint)

    // Step C: Cell numbers and badges
    for (i in 0 until totalCells) {
        val (col, row) = gridState.cellToColRow(i)
        val left = col * cellW
        val top = row * cellH
        val right = left + cellW
        val bottom = top + cellH

        val isDone = gridState.doneCells.contains(i)
        val isActive = (i == activeCell)

        val numStr = "${i + 1}"
        val numX = left + cellW * 0.08f
        val numY = top + textFillPaint.textSize * 1.05f

        canvas.drawText(numStr, numX, numY, textOutlinePaint)
        canvas.drawText(numStr, numX, numY, textFillPaint)

        if (isDone) {
            val checkX = right - checkmarkPaint.textSize * 1.1f
            val checkY = top + checkmarkPaint.textSize * 1.05f
            canvas.drawText("✓", checkX, checkY, checkmarkPaint)
        }

        if (isActive) {
            val inset = activeBorderPaint.strokeWidth / 2f
            canvas.drawRect(
                RectF(left + inset, top + inset, right - inset, bottom - inset),
                activeBorderPaint
            )
        }
    }
}

/**
 * Draws an on-paper metric ruler with centimeter and millimeter tick marks
 * along the top and left edges of the marked paper area.
 */
private fun drawRulerOverlay(
    canvas: Canvas,
    width: Int,
    height: Int,
    paperWidthMeters: Float,
    paperHeightMeters: Float
) {
    val totalCmX = paperWidthMeters * 100f
    val totalCmY = paperHeightMeters * 100f
    if (totalCmX <= 0.1f || totalCmY <= 0.1f) return

    val pxPerCmX = width.toFloat() / totalCmX
    val pxPerCmY = height.toFloat() / totalCmY

    val minDim = min(width, height).toFloat()
    val bandSize = (minDim * 0.042f).coerceIn(28f, 54f)
    val strokeBase = (minDim * 0.002f).coerceIn(1.5f, 3.5f)

    val bandPaint = Paint().apply {
        color = Color.argb(185, 15, 20, 26)
        style = Paint.Style.FILL
    }

    val borderPaint = Paint().apply {
        color = Color.argb(220, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = strokeBase
    }

    val tickMajorPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = strokeBase * 1.3f
    }

    val tickMedPaint = Paint().apply {
        color = Color.argb(230, 200, 220, 240)
        style = Paint.Style.STROKE
        strokeWidth = strokeBase
    }

    val tickMinorPaint = Paint().apply {
        color = Color.argb(160, 160, 175, 190)
        style = Paint.Style.STROKE
        strokeWidth = (strokeBase * 0.75f).coerceAtLeast(1f)
    }

    val textFillPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textSize = (bandSize * 0.38f).coerceIn(13f, 24f)
        typeface = Typeface.DEFAULT_BOLD
    }

    val textShadowPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = strokeBase * 1.2f
        textSize = textFillPaint.textSize
        typeface = Typeface.DEFAULT_BOLD
    }

    // 1. Top and left background bands
    canvas.drawRect(0f, 0f, width.toFloat(), bandSize, bandPaint)
    canvas.drawRect(0f, 0f, bandSize, height.toFloat(), bandPaint)

    // 2. Outer ruler borders
    canvas.drawLine(0f, bandSize, width.toFloat(), bandSize, borderPaint)
    canvas.drawLine(bandSize, 0f, bandSize, height.toFloat(), borderPaint)

    // 3. Corner badge "cm"
    val badgeX = bandSize * 0.15f
    val badgeY = bandSize * 0.65f
    canvas.drawText("cm", badgeX, badgeY, textShadowPaint)
    canvas.drawText("cm", badgeX, badgeY, textFillPaint)

    // 4. Top Ruler Ticks (X axis)
    val maxMmX = (totalCmX * 10f).toInt()
    for (m in 0..maxMmX) {
        val x = m * (pxPerCmX / 10f)
        if (x > width) break

        val isCm = (m % 10 == 0)
        val isHalfCm = (m % 5 == 0)

        val tickH = when {
            isCm -> bandSize * 0.55f
            isHalfCm -> bandSize * 0.35f
            else -> bandSize * 0.20f
        }
        val paint = when {
            isCm -> tickMajorPaint
            isHalfCm -> tickMedPaint
            else -> tickMinorPaint
        }

        canvas.drawLine(x, 0f, x, tickH, paint)

        if (isCm && m > 0 && x > bandSize + 8f) {
            val cmVal = "${m / 10}"
            val tx = x + (strokeBase * 1.5f)
            val ty = bandSize * 0.88f
            canvas.drawText(cmVal, tx, ty, textShadowPaint)
            canvas.drawText(cmVal, tx, ty, textFillPaint)
        }
    }

    // 5. Left Ruler Ticks (Y axis)
    val maxMmY = (totalCmY * 10f).toInt()
    for (m in 0..maxMmY) {
        val y = m * (pxPerCmY / 10f)
        if (y > height) break

        val isCm = (m % 10 == 0)
        val isHalfCm = (m % 5 == 0)

        val tickW = when {
            isCm -> bandSize * 0.55f
            isHalfCm -> bandSize * 0.35f
            else -> bandSize * 0.20f
        }
        val paint = when {
            isCm -> tickMajorPaint
            isHalfCm -> tickMedPaint
            else -> tickMinorPaint
        }

        canvas.drawLine(0f, y, tickW, y, paint)

        if (isCm && m > 0 && y > bandSize + 8f) {
            val cmVal = "${m / 10}"
            val tx = bandSize * 0.58f
            val ty = y + textFillPaint.textSize * 0.35f
            canvas.drawText(cmVal, tx, ty, textShadowPaint)
            canvas.drawText(cmVal, tx, ty, textFillPaint)
        }
    }
}

/**
 * Upload a Bitmap to a Filament RGBA8 Texture with hardware mipmap generation.
 */
private fun bitmapToFilamentTexture(engine: Engine, bitmap: Bitmap): Texture {
    val w = bitmap.width
    val h = bitmap.height

    val maxDim = max(w, h)
    val levelCount = (1 + floor(log2(maxDim.toDouble()))).toInt().coerceAtLeast(1)

    val texture = Texture.Builder()
        .width(w)
        .height(h)
        .levels(levelCount)
        .sampler(Texture.Sampler.SAMPLER_2D)
        .format(Texture.InternalFormat.RGBA8)
        .build(engine)

    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    val buf = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder())
    for (p in pixels) {
        buf.put(((p shr 16) and 0xFF).toByte())
        buf.put(((p shr  8) and 0xFF).toByte())
        buf.put(( p         and 0xFF).toByte())
        buf.put(((p shr 24) and 0xFF).toByte())
    }
    buf.rewind()

    texture.setImage(
        engine,
        0,
        Texture.PixelBufferDescriptor(buf, Texture.Format.RGBA, Texture.Type.UBYTE)
    )

    if (levelCount > 1) {
        try {
            texture.generateMipmaps(engine)
        } catch (e: Throwable) {
            Log.w("TraceAR", "generateMipmaps failed: ${e.message}")
        }
    }

    return texture
}

/**
 * Draws classic drawing guides (proportion grid, chessboard labels, center cross,
 * diagonals, rule of thirds, and golden ratio lines) directly onto the AR overlay bitmap.
 */
private fun drawDrawingGuidesOverlay(
    canvas: Canvas,
    width: Int,
    height: Int,
    bounds: RectF,
    guidesState: GuidesState,
    zoomScale: Float,
    ambientIntensity: Float,
    paperWidthMeters: Float,
    paperHeightMeters: Float
) {
    val baseColorInt = if (guidesState.isAutoContrast) {
        if (ambientIntensity > 0.45f) Color.rgb(20, 20, 20) else Color.rgb(250, 250, 250)
    } else {
        val c = guidesState.colorOption.color
        Color.argb((c.alpha * 255).toInt(), (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
    }

    val alphaVal = (guidesState.opacity.coerceIn(0.05f, 1.0f) * 255).toInt()
    val effectiveLineColor = Color.argb(
        alphaVal,
        Color.red(baseColorInt),
        Color.green(baseColorInt),
        Color.blue(baseColorInt)
    )

    val isBrightColor = (Color.red(baseColorInt) + Color.green(baseColorInt) + Color.blue(baseColorInt)) > 380
    val shadowColor = if (isBrightColor) {
        Color.argb((alphaVal * 0.7f).toInt(), 0, 0, 0)
    } else {
        Color.argb((alphaVal * 0.7f).toInt(), 255, 255, 255)
    }

    val strokePx = (guidesState.thicknessDp * (min(width, height) / 400f)).coerceIn(1.5f, 10f)

    val linePaint = Paint().apply {
        color = effectiveLineColor
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        isAntiAlias = true
    }

    val shadowPaint = Paint().apply {
        color = shadowColor
        style = Paint.Style.STROKE
        strokeWidth = strokePx + 1.5f
        isAntiAlias = true
    }

    fun drawLineWithShadow(x1: Float, y1: Float, x2: Float, y2: Float) {
        canvas.drawLine(x1, y1, x2, y2, shadowPaint)
        canvas.drawLine(x1, y1, x2, y2, linePaint)
    }

    // ── 1. Proportion Grid ──────────────────────────────────────────
    if (guidesState.isGridEnabled) {
        val (cols, rows) = when (guidesState.gridMode) {
            GuideGridMode.COUNT -> {
                guidesState.gridCols.coerceIn(2, 20) to guidesState.gridRows.coerceIn(2, 20)
            }
            GuideGridMode.REAL_SIZE -> {
                if (paperWidthMeters > 0.01f && paperHeightMeters > 0.01f) {
                    GuidesMath.calculateRealSizeCols(paperWidthMeters, guidesState.cellSizeCm).coerceIn(1, 100) to
                    GuidesMath.calculateRealSizeRows(paperHeightMeters, guidesState.cellSizeCm).coerceIn(1, 100)
                } else {
                    guidesState.gridCols.coerceIn(2, 20) to guidesState.gridRows.coerceIn(2, 20)
                }
            }
        }

        val bW = bounds.width()
        val bH = bounds.height()
        val cellW = bW / cols
        val cellH = bH / rows

        // Draw outer boundary
        canvas.drawRect(bounds, shadowPaint)
        canvas.drawRect(bounds, linePaint)

        // Draw vertical grid lines
        for (c in 1 until cols) {
            val x = bounds.left + c * cellW
            drawLineWithShadow(x, bounds.top, x, bounds.bottom)
        }

        // Draw horizontal grid lines
        for (r in 1 until rows) {
            val y = bounds.top + r * cellH
            drawLineWithShadow(bounds.left, y, bounds.right, y)
        }

        // Chessboard Labels (fades out smoothly as zoom exceeds 2.5x)
        if (guidesState.showLabels) {
            val labelFade = GuidesMath.calculateLabelZoomFade(zoomScale)
            if (labelFade > 0.02f) {
                val textAlpha = (alphaVal * labelFade).toInt().coerceIn(0, 255)
                val textSize = (min(cellW, cellH) * 0.28f).coerceIn(12f, 40f)

                val textFillPaint = Paint().apply {
                    color = Color.argb(textAlpha, Color.red(baseColorInt), Color.green(baseColorInt), Color.blue(baseColorInt))
                    this.textSize = textSize
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }

                val textShadowPaint = Paint().apply {
                    color = Color.argb((textAlpha * 0.75f).toInt(), Color.red(shadowColor), Color.green(shadowColor), Color.blue(shadowColor))
                    this.textSize = textSize
                    typeface = Typeface.DEFAULT_BOLD
                    style = Paint.Style.STROKE
                    strokeWidth = 2.5f
                    isAntiAlias = true
                }

                for (c in 0 until cols) {
                    for (r in 0 until rows) {
                        val label = "${GuidesMath.getColumnLabel(c)}${GuidesMath.getRowLabel(r)}"
                        val tx = bounds.left + c * cellW + (cellW * 0.08f).coerceIn(4f, 16f)
                        val ty = bounds.top + r * cellH + (cellH * 0.22f).coerceIn(14f, 32f)
                        canvas.drawText(label, tx, ty, textShadowPaint)
                        canvas.drawText(label, tx, ty, textFillPaint)
                    }
                }
            }
        }
    }

    // ── 2. Construction Lines ──────────────────────────────────────
    // Center Horizontal
    if (guidesState.showCenterH) {
        val y = bounds.centerY()
        drawLineWithShadow(bounds.left, y, bounds.right, y)
    }

    // Center Vertical
    if (guidesState.showCenterV) {
        val x = bounds.centerX()
        drawLineWithShadow(x, bounds.top, x, bounds.bottom)
    }

    // Diagonals
    if (guidesState.showDiagonals) {
        drawLineWithShadow(bounds.left, bounds.top, bounds.right, bounds.bottom)
        drawLineWithShadow(bounds.left, bounds.bottom, bounds.right, bounds.top)
    }

    // Rule of Thirds
    if (guidesState.showThirds) {
        val x1 = bounds.left + bounds.width() / 3f
        val x2 = bounds.left + 2f * bounds.width() / 3f
        val y1 = bounds.top + bounds.height() / 3f
        val y2 = bounds.top + 2f * bounds.height() / 3f

        drawLineWithShadow(x1, bounds.top, x1, bounds.bottom)
        drawLineWithShadow(x2, bounds.top, x2, bounds.bottom)
        drawLineWithShadow(bounds.left, y1, bounds.right, y1)
        drawLineWithShadow(bounds.left, y2, bounds.right, y2)
    }

    // Golden Ratio Lines (0.382 and 0.618)
    if (guidesState.showGoldenRatio) {
        val x1 = bounds.left + bounds.width() * GuidesMath.GOLDEN_RATIO_COMP
        val x2 = bounds.left + bounds.width() * GuidesMath.GOLDEN_RATIO_INV
        val y1 = bounds.top + bounds.height() * GuidesMath.GOLDEN_RATIO_COMP
        val y2 = bounds.top + bounds.height() * GuidesMath.GOLDEN_RATIO_INV

        drawLineWithShadow(x1, bounds.top, x1, bounds.bottom)
        drawLineWithShadow(x2, bounds.top, x2, bounds.bottom)
        drawLineWithShadow(bounds.left, y1, bounds.right, y1)
        drawLineWithShadow(bounds.left, y2, bounds.right, y2)
    }
}
