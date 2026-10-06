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
import androidx.compose.ui.platform.LocalContext
import com.tracear.app.data.AppSettings
import com.tracear.app.data.ProjectData
import com.tracear.app.ui.settings.SettingsScreen
import com.tracear.app.ui.tutorial.TutorialScreen

/**
 * TraceARApp — the root composable that manages navigation between
 * TutorialScreen, HomeScreen, ARScreen, and SettingsScreen.
 */
@Composable
fun TraceARApp() {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }

    var showTutorial by remember { mutableStateOf(!settings.hasSeenTutorial) }
    var showSettings by remember { mutableStateOf(false) }
    var activeProject by remember { mutableStateOf<ProjectData?>(null) }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                showTutorial -> {
                    TutorialScreen(
                        onDismiss = {
                            settings.setTutorialSeen(true)
                            showTutorial = false
                        }
                    )
                }

                showSettings -> {
                    SettingsScreen(
                        settings = settings,
                        onBack = { showSettings = false },
                        onOpenTutorial = { showTutorial = true }
                    )
                }

                activeProject == null -> {
                    HomeScreen(
                        settings = settings,
                        onOpenProject = { project -> activeProject = project },
                        onOpenSettings = { showSettings = true },
                        onOpenTutorial = { showTutorial = true }
                    )
                }

                else -> {
                    ARScreen(
                        project = activeProject!!,
                        settings = settings,
                        onBack = { activeProject = null },
                        onOpenSettings = { showSettings = true },
                        onOpenTutorial = { showTutorial = true }
                    )
                }
            }
        }
    }
}
