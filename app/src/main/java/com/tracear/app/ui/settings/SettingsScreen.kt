package com.tracear.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracear.app.R
import com.tracear.app.ar.FitMode
import com.tracear.app.ar.LineColorOption
import com.tracear.app.ar.SmoothingMode
import com.tracear.app.data.AppSettings

/**
 * SettingsScreen — Persistent configuration for defaults, comfort, battery saver, and tutorial.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onOpenTutorial: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- Top App Bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF21262D))
                ) {
                    Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = stringResource(R.string.settings_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // --- Scrollable Settings Sections ---
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Tracing Defaults Section
                item {
                    SettingsSectionHeader(title = stringResource(R.string.settings_section_defaults))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Default Opacity
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.settings_default_opacity), color = Color.White, fontSize = 14.sp)
                                    Text(
                                        "%.0f%%".format(settings.defaultOpacity * 100f),
                                        color = Color(0xFF58A6FF),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Slider(
                                    value = settings.defaultOpacity,
                                    onValueChange = { settings.updateDefaultOpacity(it) },
                                    valueRange = 0.10f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF58A6FF),
                                        activeTrackColor = Color(0xFF1F6FEB),
                                        inactiveTrackColor = Color(0xFF30363D)
                                    )
                                )
                            }

                            // Default Fit Mode
                            Column {
                                Text(stringResource(R.string.settings_default_fit_mode), color = Color.White, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FitMode.entries.forEach { mode ->
                                        val isSelected = settings.defaultFitMode == mode
                                        Button(
                                            onClick = { settings.updateDefaultFitMode(mode) },
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) Color(0xFF1F6FEB) else Color(0xFF21262D)
                                            ),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                                fontSize = 12.sp,
                                                color = Color.White,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }

                            // Default Grid Size
                            Column {
                                Text(stringResource(R.string.settings_default_grid_size), color = Color.White, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    (2..6).forEach { size ->
                                        val isSelected = settings.defaultGridSize == size
                                        Button(
                                            onClick = { settings.updateDefaultGridSize(size) },
                                            modifier = Modifier.weight(1f).height(34.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) Color(0xFF1F6FEB) else Color(0xFF21262D)
                                            ),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                text = "${size}x${size}",
                                                fontSize = 12.sp,
                                                color = Color.White,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }

                            // Default Line Color
                            Column {
                                Text(stringResource(R.string.settings_default_line_color), color = Color.White, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    LineColorOption.entries.forEach { opt ->
                                        val isSelected = settings.defaultLineColor == opt
                                        val color = opt.color

                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFF58A6FF) else Color(0xFF484F58),
                                                    shape = CircleShape
                                                )
                                                .clickable { settings.updateDefaultLineColor(opt) }
                                        )
                                    }
                                }
                            }

                            // Overlay Smoothing Level
                            Column {
                                Text(stringResource(R.string.settings_smoothing_level), color = Color.White, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SmoothingMode.entries.forEach { mode ->
                                        val isSelected = settings.smoothingMode == mode
                                        Button(
                                            onClick = { settings.updateSmoothingMode(mode) },
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) Color(0xFF1F6FEB) else Color(0xFF21262D)
                                            ),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                                fontSize = 12.sp,
                                                color = Color.White,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Comfort, Battery & Behavior Section
                item {
                    SettingsSectionHeader(title = stringResource(R.string.settings_section_behavior))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Left-Handed Mode Toggle
                            SettingsToggleRow(
                                title = stringResource(R.string.settings_left_handed),
                                subtitle = stringResource(R.string.settings_left_handed_desc),
                                isChecked = settings.isLeftHanded,
                                onCheckedChange = { settings.updateLeftHanded(it) }
                            )

                            // Keep Screen On Toggle
                            SettingsToggleRow(
                                title = stringResource(R.string.settings_keep_screen_on),
                                subtitle = stringResource(R.string.settings_keep_screen_on_desc),
                                isChecked = settings.isKeepScreenOn,
                                onCheckedChange = { settings.updateKeepScreenOn(it) }
                            )

                            // Battery Saver Toggle
                            SettingsToggleRow(
                                title = stringResource(R.string.settings_battery_saver),
                                subtitle = stringResource(R.string.settings_battery_saver_desc),
                                isChecked = settings.isBatterySaver,
                                onCheckedChange = { settings.updateBatterySaver(it) }
                            )

                            // Show Debug Info Toggle
                            SettingsToggleRow(
                                title = stringResource(R.string.settings_show_debug),
                                subtitle = stringResource(R.string.settings_show_debug_desc),
                                isChecked = settings.showDebugInfo,
                                onCheckedChange = { settings.updateShowDebugInfo(it) }
                            )
                        }
                    }
                }

                // 3. Help & Maintenance Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
                    ) {
                        Column {
                            // How to Use (Tutorial) Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenTutorial() }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("📖", fontSize = 18.sp)
                                    Text(
                                        stringResource(R.string.settings_how_to_use),
                                        color = Color(0xFF58A6FF),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text("→", color = Color(0xFF8B949E), fontSize = 16.sp)
                            }

                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF21262D)))

                            // Reset All Settings Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showResetDialog = true }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("🔄", fontSize = 18.sp)
                                    Text(
                                        stringResource(R.string.settings_reset_all),
                                        color = Color(0xFFF85149),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. About Section
                item {
                    SettingsSectionHeader(title = stringResource(R.string.settings_section_about))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.app_full_name),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.app_version),
                                color = Color(0xFF8B949E),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Augmented reality pencil tracing on real paper. Built with ARCore, SceneView, and OpenCV.",
                                color = Color(0xFFC9D1D9),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // --- Reset Settings Confirmation Dialog ---
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = {
                    Text(
                        stringResource(R.string.settings_reset_confirm_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        stringResource(R.string.settings_reset_confirm_desc),
                        color = Color(0xFFC9D1D9),
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            settings.resetAllSettings()
                            showResetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDA3633))
                    ) {
                        Text(stringResource(R.string.settings_reset_button), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(stringResource(R.string.settings_cancel_button), color = Color(0xFF8B949E))
                    }
                },
                containerColor = Color(0xFF161B22)
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF8B949E),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = Color(0xFF8B949E), fontSize = 11.sp, lineHeight = 16.sp)
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF238636),
                uncheckedThumbColor = Color(0xFF8B949E),
                uncheckedTrackColor = Color(0xFF21262D)
            )
        )
    }
}
