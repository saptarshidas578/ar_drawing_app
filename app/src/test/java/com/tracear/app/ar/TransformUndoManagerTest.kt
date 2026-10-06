package com.tracear.app.ar

import com.tracear.app.data.NormalizedCrop
import com.tracear.app.data.NormalizedTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransformUndoManagerTest {

    @Test
    fun testInitialState() {
        val manager = TransformUndoManager(maxHistory = 50)
        assertFalse(manager.canUndo)
        assertFalse(manager.canRedo)
        assertNull(manager.undo())
        assertNull(manager.redo())
    }

    @Test
    fun testPushAndUndoRedoSequence() {
        val manager = TransformUndoManager(maxHistory = 50)

        val t1 = NormalizedTransform(scale = 1.0f)
        val t2 = NormalizedTransform(scale = 1.2f)
        val t3 = NormalizedTransform(scale = 1.5f, rotationDegrees = 90f)

        manager.push(t1) // initial was (1.0f, default)
        manager.push(t2)
        manager.push(t3)

        assertTrue(manager.canUndo)
        assertFalse(manager.canRedo)
        assertEquals(t3, manager.current.transform)

        // Undo to t2
        val u1 = manager.undo()
        assertNotNull(u1)
        assertEquals(t2, u1!!.transform)
        assertTrue(manager.canUndo)
        assertTrue(manager.canRedo)

        // Undo to t1
        val u2 = manager.undo()
        assertNotNull(u2)
        assertEquals(t1, u2!!.transform)

        // Redo back to t2
        val r1 = manager.redo()
        assertNotNull(r1)
        assertEquals(t2, r1!!.transform)

        // Redo back to t3
        val r2 = manager.redo()
        assertNotNull(r2)
        assertEquals(t3, r2!!.transform)
        assertFalse(manager.canRedo)
    }

    @Test
    fun testCapacityLimit() {
        val manager = TransformUndoManager(maxHistory = 50)
        for (i in 1..65) {
            manager.push(NormalizedTransform(scale = 1.0f + i * 0.01f))
        }

        assertEquals(50, manager.undoCount)
        assertTrue(manager.canUndo)

        // Unwinding all 50 steps
        var undoSteps = 0
        while (manager.canUndo) {
            manager.undo()
            undoSteps++
        }
        assertEquals(50, undoSteps)
        assertFalse(manager.canUndo)
    }

    @Test
    fun testNewPushClearsRedo() {
        val manager = TransformUndoManager(maxHistory = 50)
        manager.push(NormalizedTransform(scale = 1.1f))
        manager.push(NormalizedTransform(scale = 1.2f))

        manager.undo()
        assertTrue(manager.canRedo)

        // Push new branch
        manager.push(NormalizedTransform(scale = 1.3f))
        assertFalse(manager.canRedo)
        assertEquals(1.3f, manager.current.transform.scale, 1e-4f)
    }

    @Test
    fun testDuplicatePushIgnored() {
        val manager = TransformUndoManager(maxHistory = 50)
        val t = NormalizedTransform(scale = 1.5f)
        manager.push(t)
        val countAfterFirst = manager.undoCount

        manager.push(t) // identical
        assertEquals(countAfterFirst, manager.undoCount)
    }

    @Test
    fun testFitModeAndCropUndoRedo() {
        val manager = TransformUndoManager(maxHistory = 50)
        val initialCrop = NormalizedCrop()
        val cropped = NormalizedCrop(0.1f, 0.1f, 0.9f, 0.9f)

        manager.push(TransformSnapshot(fitMode = FitMode.FIT, crop = initialCrop))
        manager.push(TransformSnapshot(fitMode = FitMode.FILL, crop = cropped))

        assertEquals(FitMode.FILL, manager.current.fitMode)
        assertTrue(manager.current.crop.isCropped)

        manager.undo()
        assertEquals(FitMode.FIT, manager.current.fitMode)
        assertFalse(manager.current.crop.isCropped)
    }
}
