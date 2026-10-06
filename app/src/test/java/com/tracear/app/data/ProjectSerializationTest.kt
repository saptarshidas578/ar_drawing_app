package com.tracear.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectSerializationTest {

    @Test
    fun testProjectSerialization_RoundTrip() {
        val original = ProjectData(
            id = "proj-1234-abcd",
            name = "Anime Portrait",
            lastModified = 1700000000000L,
            imageFileName = "image.png",
            thumbFileName = "thumb.png",
            fitMode = "FIT",
            opacity = 0.75f,
            isLinesOnly = true,
            edgeSensitivity = 0.85f,
            lineThickness = 3,
            lineColorName = "YELLOW",
            gridEnabled = true,
            gridCols = 4,
            gridRows = 4,
            doneCells = listOf(0, 1, 5, 8),
            transform = NormalizedTransform(
                offsetX = 0.05f,
                offsetY = -0.02f,
                scale = 1.1f,
                rotationDegrees = 5.0f
            ),
            brightness = 0.25f,
            contrast = 1.5f,
            invert = 1.0f,
            bwThreshold = 0.6f,
            smoothingMode = "HIGH"
        )

        val jsonString = original.toJson()
        assertNotNull(jsonString)

        val parsed = ProjectData.fromJson(jsonString)
        assertNotNull(parsed)
        assertEquals(original.id, parsed!!.id)
        assertEquals(original.name, parsed.name)
        assertEquals(original.lastModified, parsed.lastModified)
        assertEquals("FIT", parsed.fitMode)
        assertEquals(0.75f, parsed.opacity, 1e-4f)
        assertTrue(parsed.isLinesOnly)
        assertEquals(0.85f, parsed.edgeSensitivity, 1e-4f)
        assertEquals(3, parsed.lineThickness)
        assertEquals("YELLOW", parsed.lineColorName)
        assertTrue(parsed.gridEnabled)
        assertEquals(4, parsed.gridCols)
        assertEquals(4, parsed.gridRows)
        assertEquals(listOf(0, 1, 5, 8), parsed.doneCells)
        assertEquals(0.05f, parsed.transform.offsetX, 1e-4f)
        assertEquals(-0.02f, parsed.transform.offsetY, 1e-4f)
        assertEquals(1.1f, parsed.transform.scale, 1e-4f)
        assertEquals(5.0f, parsed.transform.rotationDegrees, 1e-4f)
        assertEquals(0.25f, parsed.brightness, 1e-4f)
        assertEquals(1.5f, parsed.contrast, 1e-4f)
        assertEquals(1.0f, parsed.invert, 1e-4f)
        assertEquals(0.6f, parsed.bwThreshold, 1e-4f)
        assertEquals("HIGH", parsed.smoothingMode)
    }

    @Test
    fun testProjectSerialization_DefaultValues() {
        val minimalJson = """
            {
                "id": "test-id",
                "name": "Quick Sketch"
            }
        """.trimIndent()

        val parsed = ProjectData.fromJson(minimalJson)
        assertNotNull(parsed)
        assertEquals("test-id", parsed!!.id)
        assertEquals("Quick Sketch", parsed.name)
        assertEquals("STRETCH", parsed.fitMode)
        assertEquals(0.5f, parsed.opacity, 1e-4f)
        assertFalse(parsed.isLinesOnly)
        assertEquals(3, parsed.gridCols)
        assertEquals(3, parsed.gridRows)
        assertTrue(parsed.doneCells.isEmpty())
        assertEquals(0f, parsed.transform.offsetX, 1e-4f)
        assertEquals(1f, parsed.transform.scale, 1e-4f)
    }

    @Test
    fun testProjectSerialization_CorruptedJson() {
        val brokenJson = "{ this is not valid json }"
        val parsed = ProjectData.fromJson(brokenJson)
        assertNull(parsed)
    }

    @Test
    fun testNormalizedTransform_Bounds() {
        val transform = NormalizedTransform(
            offsetX = 0.5f,
            offsetY = 0.5f,
            scale = 2.0f,
            rotationDegrees = 45f
        )
        val json = transform.toJson()
        val parsed = NormalizedTransform.fromJson(json)
        assertEquals(0.5f, parsed.offsetX, 1e-4f)
        assertEquals(0.5f, parsed.offsetY, 1e-4f)
        assertEquals(2.0f, parsed.scale, 1e-4f)
        assertEquals(45f, parsed.rotationDegrees, 1e-4f)
    }

    @Test
    fun testNormalizedCrop_Serialization() {
        val crop = NormalizedCrop(
            left = 0.1f,
            top = 0.15f,
            right = 0.85f,
            bottom = 0.9f
        )
        assertTrue(crop.isCropped)

        val json = crop.toJson()
        val parsed = NormalizedCrop.fromJson(json)
        assertEquals(0.1f, parsed.left, 1e-4f)
        assertEquals(0.15f, parsed.top, 1e-4f)
        assertEquals(0.85f, parsed.right, 1e-4f)
        assertEquals(0.9f, parsed.bottom, 1e-4f)
        assertTrue(parsed.isCropped)

        val defaultCrop = NormalizedCrop()
        assertFalse(defaultCrop.isCropped)
    }

    @Test
    fun testProjectData_WithCropAndRuler() {
        val p = ProjectData(
            id = "crop-test",
            name = "Cropped A4 Drawing",
            paperPresetName = "A4",
            isRulerEnabled = true,
            crop = NormalizedCrop(0.2f, 0.2f, 0.8f, 0.8f)
        )
        val json = p.toJson()
        val parsed = ProjectData.fromJson(json)
        assertNotNull(parsed)
        assertEquals("A4", parsed!!.paperPresetName)
        assertTrue(parsed.isRulerEnabled)
        assertTrue(parsed.crop.isCropped)
        assertEquals(0.2f, parsed.crop.left, 1e-4f)
        assertEquals(0.8f, parsed.crop.right, 1e-4f)
    }
}
