package com.tracear.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tracear.app.ui.TraceARApp
import org.opencv.android.OpenCVLoader

/**
 * MainActivity — the single Activity for the entire app.
 * All UI is built with Jetpack Compose, so this Activity just
 * hosts the root Composable.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OpenCV for paper contour detection with safe fallback
        try {
            if (OpenCVLoader.initLocal()) {
                Log.i("TraceAR", "OpenCV initialized successfully")
            } else {
                Log.w("TraceAR", "OpenCV initLocal returned false, will use manual corner placement")
            }
        } catch (e: Throwable) {
            Log.e("TraceAR", "OpenCV native load failed: ${e.message}", e)
        }

        // Let the app draw behind system bars for a fullscreen feel
        enableEdgeToEdge()
        // Set our root Composable as the entire screen content
        setContent {
            TraceARApp()
        }
    }
}
