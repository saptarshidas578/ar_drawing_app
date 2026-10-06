package com.tracear.app.ar

import com.tracear.app.data.NormalizedCrop
import com.tracear.app.data.NormalizedTransform

/**
 * Complete immutable snapshot of the transform, fit mode, and crop state.
 */
data class TransformSnapshot(
    val transform: NormalizedTransform = NormalizedTransform(),
    val fitMode: FitMode = FitMode.STRETCH,
    val crop: NormalizedCrop = NormalizedCrop()
)

/**
 * Undo and Redo history manager for image transforms, fit modes, and crop operations.
 * Supports up to [maxHistory] steps (defaults to 50, exceeding the 30 step requirement).
 */
class TransformUndoManager(
    val maxHistory: Int = 50,
    initialSnapshot: TransformSnapshot = TransformSnapshot()
) {
    private val undoStack = ArrayDeque<TransformSnapshot>()
    private val redoStack = ArrayDeque<TransformSnapshot>()
    var current: TransformSnapshot = initialSnapshot
        private set

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    val undoCount: Int
        get() = undoStack.size

    val redoCount: Int
        get() = redoStack.size

    /**
     * Initializes or resets the manager with an initial snapshot.
     */
    fun reset(snapshot: TransformSnapshot) {
        undoStack.clear()
        redoStack.clear()
        current = snapshot
    }

    /**
     * Records a new state. If the new state equals the current state, it is ignored.
     * Clears the redo stack when a new operation occurs.
     */
    fun push(newSnapshot: TransformSnapshot) {
        if (newSnapshot == current) return

        undoStack.addLast(current)
        if (undoStack.size > maxHistory) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        current = newSnapshot
    }

    fun push(
        transform: NormalizedTransform,
        fitMode: FitMode = current.fitMode,
        crop: NormalizedCrop = current.crop
    ) {
        push(TransformSnapshot(transform, fitMode, crop))
    }

    /**
     * Reverts to the previous state in history.
     * Returns the restored snapshot, or null if cannot undo.
     */
    fun undo(): TransformSnapshot? {
        if (!canUndo) return null
        val previous = undoStack.removeLast()
        redoStack.addLast(current)
        current = previous
        return current
    }

    /**
     * Re-applies the most recently undone state.
     * Returns the restored snapshot, or null if cannot redo.
     */
    fun redo(): TransformSnapshot? {
        if (!canRedo) return null
        val next = redoStack.removeLast()
        undoStack.addLast(current)
        current = next
        return current
    }
}
