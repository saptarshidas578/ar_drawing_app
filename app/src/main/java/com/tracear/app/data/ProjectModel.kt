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
    val isRulerEnabled: Boolean = false
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
                    isRulerEnabled = root.optBoolean("isRulerEnabled", false)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
