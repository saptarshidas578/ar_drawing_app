package com.tracear.app.ui.tracing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Slim top bar (~48 dp, semi-transparent):
 * Back, project name, tracking status chip, Focus Mode toggle, and overflow menu (⋮).
 */
@Composable
fun TracingTopBar(
    projectName: String,
    trackingBadgeColor: Color,
    trackingBadgeText: String,
    trackingTip: String?,
    isLowLight: Boolean,
    isTorchSupported: Boolean,
    isTorchOn: Boolean,
    onToggleTorch: () -> Unit,
    onBack: () -> Unit,
    onToggleFocus: () -> Unit,
    onRecalibrate: () -> Unit,
    onChangeImage: () -> Unit,
    onSaveProject: () -> Unit,
    onToggleDebugPanel: () -> Unit,
    isDebugPanelVisible: Boolean,
    isLeftHanded: Boolean,
    onToggleLeftHanded: (Boolean) -> Unit,
    isAutoLineColor: Boolean,
    onToggleAutoLineColor: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(Color(0xCC161B22))
            .border(
                width = 0.5.dp,
                color = Color(0x33FFFFFF),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // --- Left: Back Button & Title ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = projectName.ifBlank { "TraceAR" },
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // --- Center: Tracking Status Chip ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x99000000))
                .border(1.dp, trackingBadgeColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(trackingBadgeColor)
            )
            Text(
                text = trackingBadgeText,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (isLowLight && isTorchSupported && !isTorchOn) {
                Text(
                    text = "🔦",
                    fontSize = 10.sp,
                    modifier = Modifier.clickable { onToggleTorch() }
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // --- Right: Focus button & Menu (⋮) ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Focus mode toggle button
            IconButton(
                onClick = onToggleFocus,
                modifier = Modifier.size(44.dp)
            ) {
                Text("👁️", fontSize = 16.sp)
            }

            // Menu button (⋮)
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Text("⋮", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(Color(0xF01C2128))
                ) {
                    DropdownMenuItem(
                        text = { Text("💾  Save Project", color = Color.White, fontSize = 13.sp) },
                        onClick = {
                            menuExpanded = false
                            onSaveProject()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🎯  Re-calibrate Paper", color = Color.White, fontSize = 13.sp) },
                        onClick = {
                            menuExpanded = false
                            onRecalibrate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🖼️  Change Image", color = Color.White, fontSize = 13.sp) },
                        onClick = {
                            menuExpanded = false
                            onChangeImage()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (isDebugPanelVisible) "🪲  Hide Debug Metrics" else "🪲  Show Debug Metrics",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleDebugPanel()
                        }
                    )

                    HorizontalDivider(color = Color(0x33FFFFFF), thickness = 0.5.dp)

                    // Left-handed mode toggle
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("✋  Left-Handed Mode", color = Color.White, fontSize = 13.sp)
                                Switch(
                                    checked = isLeftHanded,
                                    onCheckedChange = {
                                        onToggleLeftHanded(it)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF1F6FEB)
                                    )
                                )
                            }
                        },
                        onClick = {
                            onToggleLeftHanded(!isLeftHanded)
                        }
                    )

                    // Auto line color toggle
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("✨  Auto Line Color", color = Color.White, fontSize = 13.sp)
                                Switch(
                                    checked = isAutoLineColor,
                                    onCheckedChange = {
                                        onToggleAutoLineColor(it)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF238636)
                                    )
                                )
                            }
                        },
                        onClick = {
                            onToggleAutoLineColor(!isAutoLineColor)
                        }
                    )
                }
            }
        }
    }
}
