package com.tracear.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Normalized paper transform relative to paper corners (0.0 to 1.0).
 * Never stores raw ARCore anchors or world coordinates.
 */
data class NormalizedTransform(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val flipH: Boolean = false,
    val flipV: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("offsetX", offsetX.toDouble())
        put("offsetY", offsetY.toDouble())
        put("scale", scale.toDouble())
        put("rotationDegrees", rotationDegrees.toDouble())
        put("flipH", flipH)
        put("flipV", flipV)
    }

    companion object {
        fun fromJson(json: JSONObject?): NormalizedTransform {
            if (json == null) return NormalizedTransform()
            return NormalizedTransform(
                offsetX = json.optDouble("offsetX", 0.0).toFloat(),
                offsetY = json.optDouble("offsetY", 0.0).toFloat(),
                scale = json.optDouble("scale", 1.0).toFloat(),
                rotationDegrees = json.optDouble("rotationDegrees", 0.0).toFloat(),
                flipH = json.optBoolean("flipH", false),
                flipV = json.optBoolean("flipV", false)
            )
        }
    }
}

/**
 * Normalized crop rectangle within reference image (0.0 to 1.0).
 */
data class NormalizedCrop(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val isCropped: Boolean
        get() = left > 0.001f || top > 0.001f || right < 0.999f || bottom < 0.999f

    fun toJson(): JSONObject = JSONObject().apply {
        put("left", left.toDouble())
        put("top", top.toDouble())
        put("right", right.toDouble())
        put("bottom", bottom.toDouble())
    }

    companion object {
        fun fromJson(json: JSONObject?): NormalizedCrop {
            if (json == null) return NormalizedCrop()
            return NormalizedCrop(
                left = json.optDouble("left", 0.0).toFloat().coerceIn(0f, 1f),
                top = json.optDouble("top", 0.0).toFloat().coerceIn(0f, 1f),
                right = json.optDouble("right", 1.0).toFloat().coerceIn(0f, 1f),
                bottom = json.optDouble("bottom", 1.0).toFloat().coerceIn(0f, 1f)
            )
        }
    }
}

/**
 * Normalized drawing guides configuration.
 * Always stored in paper-normalized coordinates (0.0 to 1.0).
 */
data class NormalizedGuides(
    val isGridEnabled: Boolean = false,
    val gridMode: String = "COUNT", // COUNT or REAL_SIZE
    val gridCols: Int = 4,
    val gridRows: Int = 4,
    val cellSizeCm: Float = 2.0f,
    val showLabels: Boolean = true,
    val showCenterH: Boolean = false,
    val showCenterV: Boolean = false,
    val showDiagonals: Boolean = false,
    val showThirds: Boolean = false,
    val showGoldenRatio: Boolean = false,
    val colorName: String = "CYAN",
    val thicknessDp: Float = 1.5f,
    val opacity: Float = 0.7f,
    val isAutoContrast: Boolean = false,
    val moveWithImage: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("isGridEnabled", isGridEnabled)
        put("gridMode", gridMode)
        put("gridCols", gridCols)
        put("gridRows", gridRows)
        put("cellSizeCm", cellSizeCm.toDouble())
        put("showLabels", showLabels)
        put("showCenterH", showCenterH)
        put("showCenterV", showCenterV)
        put("showDiagonals", showDiagonals)
        put("showThirds", showThirds)
        put("showGoldenRatio", showGoldenRatio)
        put("colorName", colorName)
        put("thicknessDp", thicknessDp.toDouble())
        put("opacity", opacity.toDouble())
        put("isAutoContrast", isAutoContrast)
        put("moveWithImage", moveWithImage)
    }

    companion object {
        fun fromJson(json: JSONObject?): NormalizedGuides {
            if (json == null) return NormalizedGuides()
            return NormalizedGuides(
                isGridEnabled = json.optBoolean("isGridEnabled", false),
                gridMode = json.optString("gridMode", "COUNT"),
                gridCols = json.optInt("gridCols", 4),
                gridRows = json.optInt("gridRows", 4),
                cellSizeCm = json.optDouble("cellSizeCm", 2.0).toFloat(),
                showLabels = json.optBoolean("showLabels", true),
                showCenterH = json.optBoolean("showCenterH", false),
                showCenterV = json.optBoolean("showCenterV", false),
                showDiagonals = json.optBoolean("showDiagonals", false),
                showThirds = json.optBoolean("showThirds", false),
                showGoldenRatio = json.optBoolean("showGoldenRatio", false),
                colorName = json.optString("colorName", "CYAN"),
                thicknessDp = json.optDouble("thicknessDp", 1.5).toFloat(),
                opacity = json.optDouble("opacity", 0.7).toFloat(),
                isAutoContrast = json.optBoolean("isAutoContrast", false),
                moveWithImage = json.optBoolean("moveWithImage", false)
            )
        }
    }
}

/**
 * ProjectData — complete persistent project configuration.
 */
data class ProjectData(
    val id: String,
    val name: String,
    val lastModified: Long = System.currentTimeMillis(),
    val imageFileName: String = "image.png",
    val thumbFileName: String = "thumb.png",
    val fitMode: String = "STRETCH", // STRETCH, FIT, FILL
    val opacity: Float = 0.5f,
    val isLinesOnly: Boolean = false,
    val edgeSensitivity: Float = 0.6f,
    val lineThickness: Int = 1,
    val lineColorName: String = "CYAN", // CYAN, YELLOW, RED, WHITE, BLACK
    val gridEnabled: Boolean = false,
    val gridCols: Int = 3,
    val gridRows: Int = 3,
    val doneCells: List<Int> = emptyList(),
    val transform: NormalizedTransform = NormalizedTransform(),
    // Image Adjustments
    val brightness: Float = 0f,
    val contrast: Float = 1.0f,
    val invert: Float = 0f,
    val bwThreshold: Float = 0f,
    // Overlay Smoothing
    val smoothingMode: String = "LOW", // OFF, LOW, HIGH
    // Advanced Transform, Paper & Measurement
    val crop: NormalizedCrop = NormalizedCrop(),
    val paperPresetName: String = "Custom",
    val isRulerEnabled: Boolean = false,
    // Drawing Guides
    val guides: NormalizedGuides = NormalizedGuides(),
    // Tonal Layers
    val tones: NormalizedTones = NormalizedTones()
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("id", id)
        root.put("name", name)
        root.put("lastModified", lastModified)
        root.put("imageFileName", imageFileName)
        root.put("thumbFileName", thumbFileName)
        root.put("fitMode", fitMode)
        root.put("opacity", opacity.toDouble())
        root.put("isLinesOnly", isLinesOnly)
        root.put("edgeSensitivity", edgeSensitivity.toDouble())
        root.put("lineThickness", lineThickness)
        root.put("lineColorName", lineColorName)
        root.put("gridEnabled", gridEnabled)
        root.put("gridCols", gridCols)
        root.put("gridRows", gridRows)

        val doneArray = JSONArray()
        doneCells.forEach { doneArray.put(it) }
        root.put("doneCells", doneArray)

        root.put("transform", transform.toJson())

        // Image adjustments & smoothing
        root.put("brightness", brightness.toDouble())
        root.put("contrast", contrast.toDouble())
        root.put("invert", invert.toDouble())
        root.put("bwThreshold", bwThreshold.toDouble())
        root.put("smoothingMode", smoothingMode)

        // Advanced Transform, Paper & Measurement
        root.put("crop", crop.toJson())
        root.put("paperPresetName", paperPresetName)
        root.put("isRulerEnabled", isRulerEnabled)
        root.put("guides", guides.toJson())
        root.put("tones", tones.toJson())

        return root.toString(2)
    }

    companion object {
        fun fromJson(jsonStr: String): ProjectData? {
            return try {
                val root = JSONObject(jsonStr)
                val doneList = mutableListOf<Int>()
                val doneArr = root.optJSONArray("doneCells")
                if (doneArr != null) {
                    for (i in 0 until doneArr.length()) {
                        doneList.add(doneArr.getInt(i))
                    }
                }

                ProjectData(
                    id = root.getString("id"),
                    name = root.getString("name"),
                    lastModified = root.optLong("lastModified", System.currentTimeMillis()),
                    imageFileName = root.optString("imageFileName", "image.png"),
                    thumbFileName = root.optString("thumbFileName", "thumb.png"),
                    fitMode = root.optString("fitMode", "STRETCH"),
                    opacity = root.optDouble("opacity", 0.5).toFloat(),
                    isLinesOnly = root.optBoolean("isLinesOnly", false),
                    edgeSensitivity = root.optDouble("edgeSensitivity", 0.6).toFloat(),
                    lineThickness = root.optInt("lineThickness", 1),
                    lineColorName = root.optString("lineColorName", "CYAN"),
                    gridEnabled = root.optBoolean("gridEnabled", false),
                    gridCols = root.optInt("gridCols", 3),
                    gridRows = root.optInt("gridRows", 3),
                    doneCells = doneList,
                    transform = NormalizedTransform.fromJson(root.optJSONObject("transform")),
                    brightness = root.optDouble("brightness", 0.0).toFloat(),
                    contrast = root.optDouble("contrast", 1.0).toFloat(),
                    invert = root.optDouble("invert", 0.0).toFloat(),
                    bwThreshold = root.optDouble("bwThreshold", 0.0).toFloat(),
                    smoothingMode = root.optString("smoothingMode", "LOW"),
                    crop = NormalizedCrop.fromJson(root.optJSONObject("crop")),
                    paperPresetName = root.optString("paperPresetName", "Custom"),
                    isRulerEnabled = root.optBoolean("isRulerEnabled", false),
                    guides = NormalizedGuides.fromJson(root.optJSONObject("guides")),
                    tones = NormalizedTones.fromJson(root.optJSONObject("tones"))
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Normalized tonal layers configuration for shading and portrait drawing.
 */
data class NormalizedTones(
    val isEnabled: Boolean = false,
    val toneCount: Int = 4,
    val smoothingLevel: Float = 0.5f,
    val layerVisibilities: List<Boolean> = emptyList(),
    val layerColors: List<Int> = emptyList(),
    val layerOpacities: List<Float> = emptyList(),
    val isOutlineVisible: Boolean = true,
    val outlineColorArgb: Int = 0xFF00E5FF.toInt(),
    val outlineOpacity: Float = 1.0f,
    val isStagesMode: Boolean = false,
    val currentStage: Int = 0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("isEnabled", isEnabled)
        put("toneCount", toneCount)
        put("smoothingLevel", smoothingLevel.toDouble())

        val visArray = JSONArray()
        layerVisibilities.forEach { visArray.put(it) }
        put("layerVisibilities", visArray)

        val colArray = JSONArray()
        layerColors.forEach { colArray.put(it) }
        put("layerColors", colArray)

        val opArray = JSONArray()
        layerOpacities.forEach { opArray.put(it.toDouble()) }
        put("layerOpacities", opArray)

        put("isOutlineVisible", isOutlineVisible)
        put("outlineColorArgb", outlineColorArgb)
        put("outlineOpacity", outlineOpacity.toDouble())
        put("isStagesMode", isStagesMode)
        put("currentStage", currentStage)
    }

    companion object {
        fun fromJson(json: JSONObject?): NormalizedTones {
            if (json == null) return NormalizedTones()

            val visList = mutableListOf<Boolean>()
            val visArr = json.optJSONArray("layerVisibilities")
            if (visArr != null) {
                for (i in 0 until visArr.length()) visList.add(visArr.optBoolean(i, true))
            }

            val colList = mutableListOf<Int>()
            val colArr = json.optJSONArray("layerColors")
            if (colArr != null) {
                for (i in 0 until colArr.length()) colList.add(colArr.optInt(i))
            }

            val opList = mutableListOf<Float>()
            val opArr = json.optJSONArray("layerOpacities")
            if (opArr != null) {
                for (i in 0 until opArr.length()) opList.add(opArr.optDouble(i, 0.7).toFloat())
            }

            return NormalizedTones(
                isEnabled = json.optBoolean("isEnabled", false),
                toneCount = json.optInt("toneCount", 4),
                smoothingLevel = json.optDouble("smoothingLevel", 0.5).toFloat(),
                layerVisibilities = visList,
                layerColors = colList,
                layerOpacities = opList,
                isOutlineVisible = json.optBoolean("isOutlineVisible", true),
                outlineColorArgb = json.optInt("outlineColorArgb", 0xFF00E5FF.toInt()),
                outlineOpacity = json.optDouble("outlineOpacity", 1.0).toFloat(),
                isStagesMode = json.optBoolean("isStagesMode", false),
                currentStage = json.optInt("currentStage", 0)
            )
        }
    }
}

