package com.tracear.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tracear.app.ar.FitMode
import com.tracear.app.ar.LineColorOption
import com.tracear.app.ar.SmoothingMode

/**
 * AppSettings — Central persistent preferences manager for TraceAR.
 *
 * Backed by SharedPreferences with reactive Compose state for seamless UI updates.
 */
class AppSettings(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "tracear_app_settings"

        private const val KEY_HAS_SEEN_TUTORIAL = "has_seen_tutorial"
        private const val KEY_DEFAULT_OPACITY = "default_opacity"
        private const val KEY_DEFAULT_FIT_MODE = "default_fit_mode"
        private const val KEY_DEFAULT_GRID_SIZE = "default_grid_size"
        private const val KEY_DEFAULT_LINE_COLOR = "default_line_color"
        private const val KEY_SMOOTHING_MODE = "smoothing_mode"
        private const val KEY_LEFT_HANDED = "left_handed"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_BATTERY_SAVER = "battery_saver"
        private const val KEY_SHOW_DEBUG = "show_debug"
    }

    var hasSeenTutorial by mutableStateOf(prefs.getBoolean(KEY_HAS_SEEN_TUTORIAL, false))
        private set

    var defaultOpacity by mutableFloatStateOf(prefs.getFloat(KEY_DEFAULT_OPACITY, 0.5f))
        private set

    var defaultFitMode by mutableStateOf(
        FitMode.entries.find { it.name == prefs.getString(KEY_DEFAULT_FIT_MODE, FitMode.STRETCH.name) }
            ?: FitMode.STRETCH
    )
        private set

    var defaultGridSize by mutableIntStateOf(prefs.getInt(KEY_DEFAULT_GRID_SIZE, 3))
        private set

    var defaultLineColor by mutableStateOf(
        LineColorOption.entries.find { it.name == prefs.getString(KEY_DEFAULT_LINE_COLOR, LineColorOption.CYAN.name) }
            ?: LineColorOption.CYAN
    )
        private set

    var smoothingMode by mutableStateOf(
        SmoothingMode.entries.find { it.name == prefs.getString(KEY_SMOOTHING_MODE, SmoothingMode.LOW.name) }
            ?: SmoothingMode.LOW
    )
        private set

    var isLeftHanded by mutableStateOf(prefs.getBoolean(KEY_LEFT_HANDED, false))
        private set

    var isKeepScreenOn by mutableStateOf(prefs.getBoolean(KEY_KEEP_SCREEN_ON, true))
        private set

    var isBatterySaver by mutableStateOf(prefs.getBoolean(KEY_BATTERY_SAVER, false))
        private set

    var showDebugInfo by mutableStateOf(prefs.getBoolean(KEY_SHOW_DEBUG, false))
        private set

    // --- Mutators with immediate persistence ---

    fun setTutorialSeen(seen: Boolean) {
        hasSeenTutorial = seen
        prefs.edit().putBoolean(KEY_HAS_SEEN_TUTORIAL, seen).apply()
    }

    fun updateDefaultOpacity(value: Float) {
        val clamped = value.coerceIn(0.1f, 1.0f)
        defaultOpacity = clamped
        prefs.edit().putFloat(KEY_DEFAULT_OPACITY, clamped).apply()
    }

    fun updateDefaultFitMode(mode: FitMode) {
        defaultFitMode = mode
        prefs.edit().putString(KEY_DEFAULT_FIT_MODE, mode.name).apply()
    }

    fun updateDefaultGridSize(size: Int) {
        val clamped = size.coerceIn(2, 6)
        defaultGridSize = clamped
        prefs.edit().putInt(KEY_DEFAULT_GRID_SIZE, clamped).apply()
    }

    fun updateDefaultLineColor(color: LineColorOption) {
        defaultLineColor = color
        prefs.edit().putString(KEY_DEFAULT_LINE_COLOR, color.name).apply()
    }

    fun updateSmoothingMode(mode: SmoothingMode) {
        smoothingMode = mode
        prefs.edit().putString(KEY_SMOOTHING_MODE, mode.name).apply()
    }

    fun updateLeftHanded(enabled: Boolean) {
        isLeftHanded = enabled
        prefs.edit().putBoolean(KEY_LEFT_HANDED, enabled).apply()
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        isKeepScreenOn = enabled
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()
    }

    fun updateBatterySaver(enabled: Boolean) {
        isBatterySaver = enabled
        prefs.edit().putBoolean(KEY_BATTERY_SAVER, enabled).apply()
    }

    fun updateShowDebugInfo(enabled: Boolean) {
        showDebugInfo = enabled
        prefs.edit().putBoolean(KEY_SHOW_DEBUG, enabled).apply()
    }

    fun resetAllSettings() {
        prefs.edit().clear().putBoolean(KEY_HAS_SEEN_TUTORIAL, true).apply()
        defaultOpacity = 0.5f
        defaultFitMode = FitMode.STRETCH
        defaultGridSize = 3
        defaultLineColor = LineColorOption.CYAN
        smoothingMode = SmoothingMode.LOW
        isLeftHanded = false
        isKeepScreenOn = true
        isBatterySaver = false
        showDebugInfo = false
    }
}
