package com.tracear.app.ui.tracing

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 6 primary categories in the bottom tracing dock.
 */
enum class DockCategory(val label: String, val icon: String) {
    OPACITY("Opacity", "💧"),
    TRANSFORM("Transform", "📐"),
    SECTIONS("Sections", "▦"),
    ADJUST("Adjust", "🎨"),
    LINES("Lines", "✏️"),
    VIEW("View", "🔍")
}

/**
 * State holder for tracing screen UI interaction, comfort features,
 * and category sheet lifecycle.
 */
class TracingUiState(context: Context? = null) {

    private val prefs = context?.getSharedPreferences("tracear_prefs", Context.MODE_PRIVATE)

    // Active bottom sheet category (null = no sheet open)
    var activeCategory by mutableStateOf<DockCategory?>(null)

    // Focus mode: hides all UI chrome except a tiny restore toggle
    var isFocusMode by mutableStateOf(false)

    // Hold-to-Peek: when true, overlay opacity temporarily drops to 0%
    var isPeeking by mutableStateOf(false)

    // Left-handed mode: mirrors dock and floating action controls to the left edge
    var isLeftHanded by mutableStateOf(prefs?.getBoolean("left_handed_mode", false) ?: false)

    // Auto line color: automatically picks contrasting line color (light vs dark)
    var isAutoLineColor by mutableStateOf(prefs?.getBoolean("auto_line_color", false) ?: false)

    // Inactivity tracking for 6-second auto-close
    var lastInteractionTime by mutableLongStateOf(System.currentTimeMillis())

    fun notifyInteraction() {
        lastInteractionTime = System.currentTimeMillis()
    }

    fun toggleCategory(category: DockCategory) {
        notifyInteraction()
        activeCategory = if (activeCategory == category) null else category
    }

    fun closeSheet() {
        activeCategory = null
    }

    fun toggleFocusMode() {
        isFocusMode = !isFocusMode
        if (isFocusMode) {
            closeSheet()
        }
    }

    fun updateLeftHanded(enabled: Boolean) {
        isLeftHanded = enabled
        prefs?.edit()?.putBoolean("left_handed_mode", enabled)?.apply()
    }

    fun updateAutoLineColor(enabled: Boolean) {
        isAutoLineColor = enabled
        prefs?.edit()?.putBoolean("auto_line_color", enabled)?.apply()
    }
}
