package com.tracear.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tracear.app.data.ProjectData

/**
 * TraceARApp — the root composable that manages navigation between HomeScreen and ARScreen.
 */
@Composable
fun TraceARApp() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            var activeProject by remember { mutableStateOf<ProjectData?>(null) }

            if (activeProject == null) {
                HomeScreen(
                    onOpenProject = { project ->
                        activeProject = project
                    }
                )
            } else {
                ARScreen(
                    project = activeProject!!,
                    onBack = { activeProject = null }
                )
            }
        }
    }
}
