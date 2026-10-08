package com.tracear.app.ui.tracing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tracear.app.export.CaptureResult
import com.tracear.app.export.CaptureType
import com.tracear.app.export.ExportMath
import com.tracear.app.export.TimelapseState
import com.tracear.app.export.TimelapseStatus
import kotlin.math.roundToInt

@Composable
fun ExportDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    timelapseState: TimelapseState,
    lastCaptureResult: CaptureResult?,
    isCapturingPhoto: Boolean,
    onCapturePhoto: (CaptureType) -> Unit,
    onShareLastPhoto: () -> Unit,
    onToggleTimelapseRecording: () -> Unit,
    onExportTimelapseVideo: () -> Unit,
    onCancelTimelapseExport: () -> Unit,
    onShareTimelapseVideo: () -> Unit,
    onDiscardTimelapseFrames: () -> Unit,
    isLowStorage: Boolean
) {
    if (!isOpen) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = Color(0xFF161B22)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Header ───────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📸", fontSize = 20.sp)
                        Text(
                            text = "Capture & Timelapse",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF30363D))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = Color(0xFF8B949E), fontSize = 14.sp)
                    }
                }

                // Low storage warning
                if (isLowStorage) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x33DA3633))
                            .border(1.dp, Color(0xFFDA3633), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "⚠️ Low storage space remaining (<100MB). Free up storage before long recordings.",
                            color = Color(0xFFFF7B72),
                            fontSize = 11.sp
                        )
                    }
                }

                // ── Section 1: Photo Export ──────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF21262D))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Photo Capture",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onCapturePhoto(CaptureType.WITH_OVERLAY) },
                            enabled = !isCapturingPhoto,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F6FEB)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text(
                                text = if (isCapturingPhoto) "Saving..." else "With Overlay",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { onCapturePhoto(CaptureType.WITHOUT_OVERLAY) },
                            enabled = !isCapturingPhoto,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30363D)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text(
                                text = "Drawing Only",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Scan Paper option
                    Button(
                        onClick = { onCapturePhoto(CaptureType.RECTIFIED_PAPER) },
                        enabled = !isCapturingPhoto,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Text(
                            text = "📄 Rectified Paper Scan (Flat)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Last capture success readout & share button
                    if (lastCaptureResult != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF161B22))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Saved to Gallery ✓",
                                    color = Color(0xFF3FB950),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = lastCaptureResult.message,
                                    color = Color(0xFF8B949E),
                                    fontSize = 10.sp
                                )
                            }
                            Button(
                                onClick = onShareLastPhoto,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F6FEB)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Share 📤", fontSize = 10.sp)
                            }
                        }
                    }
                }

                // ── Section 2: Timelapse Recording ────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF21262D))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Timelapse Session",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (timelapseState.isRecording) {
                                    if (timelapseState.isPaused) "Paused" else "Recording in background"
                                } else "Capture drawing progress",
                                color = Color(0xFF8B949E),
                                fontSize = 10.sp
                            )
                        }

                        // Record Button Toggle
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (timelapseState.isRecording) Color(0xFFDA3633) else Color(0xFF238636)
                                )
                                .clickable { onToggleTimelapseRecording() }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (timelapseState.isRecording) "⏹ Stop" else "⏺ Record",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Recording Stats Chip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF161B22))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${timelapseState.recordedFrameCount} frames • ${ExportMath.formatFileSize(timelapseState.recordedBytes)}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Elapsed: ${timelapseState.formattedDuration} • Output: ~${"%.1f".format(timelapseState.estimatedVideoLengthSeconds)}s",
                                color = Color(0xFF58A6FF),
                                fontSize = 10.sp
                            )
                        }
                        if (timelapseState.recordedFrameCount > 0 && !timelapseState.isRecording) {
                            Text(
                                text = "Discard",
                                color = Color(0xFFF85149),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onDiscardTimelapseFrames() }
                                    .padding(4.dp)
                            )
                        }
                    }

                    // Setting 1: Interval slider
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Capture Interval", color = Color(0xFF8B949E), fontSize = 10.sp)
                            Text("1 frame every ${timelapseState.intervalSeconds}s", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = timelapseState.intervalSeconds.toFloat(),
                            onValueChange = { timelapseState.intervalSeconds = it.roundToInt().coerceIn(1, 30) },
                            valueRange = 1f..30f,
                            enabled = !timelapseState.isRecording,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF58A6FF), activeTrackColor = Color(0xFF58A6FF)),
                            modifier = Modifier.height(26.dp)
                        )
                    }

                    // Setting 2: Playback FPS
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Playback Speed", color = Color(0xFF8B949E), fontSize = 10.sp)
                            Text("${timelapseState.playbackFps} FPS", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = timelapseState.playbackFps.toFloat(),
                            onValueChange = { timelapseState.playbackFps = it.roundToInt().coerceIn(10, 30) },
                            valueRange = 10f..30f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF58A6FF), activeTrackColor = Color(0xFF58A6FF)),
                            modifier = Modifier.height(26.dp)
                        )
                    }

                    // Export / Encoding Progress Bar
                    if (timelapseState.status == TimelapseStatus.EXPORTING) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Encoding MP4 video...",
                                    color = Color(0xFF58A6FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${(timelapseState.exportProgress * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                            LinearProgressIndicator(
                                progress = { timelapseState.exportProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF1F6FEB),
                                trackColor = Color(0xFF30363D)
                            )
                            Button(
                                onClick = onCancelTimelapseExport,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30363D)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth().height(32.dp)
                            ) {
                                Text("Cancel Encoding", fontSize = 10.sp, color = Color(0xFFF85149))
                            }
                        }
                    } else {
                        // Export Video Button
                        Button(
                            onClick = onExportTimelapseVideo,
                            enabled = timelapseState.recordedFrameCount > 0 && !timelapseState.isRecording,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F6FEB)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text(
                                text = "🎬 Export MP4 Video (${timelapseState.recordedFrameCount} frames)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Video Export Success & Share
                    if (timelapseState.lastExportedFile != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF161B22))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Video Saved to Gallery ✓",
                                    color = Color(0xFF3FB950),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Movies/TraceAR",
                                    color = Color(0xFF8B949E),
                                    fontSize = 10.sp
                                )
                            }
                            Button(
                                onClick = onShareTimelapseVideo,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Share 📤", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Non-obstructive blinking Recording chip shown on screen while timelapse recording is active.
 */
@Composable
fun TimelapseRecordingChip(
    timelapseState: TimelapseState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!timelapseState.isRecording) return

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xCC000000))
            .border(1.dp, if (timelapseState.isPaused) Color(0xFFD29922) else Color(0xFFDA3633), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (timelapseState.isPaused) Color(0xFFD29922) else Color(0xFFDA3633))
            )
            Text(
                text = if (timelapseState.isPaused) "REC PAUSED" else "REC",
                color = if (timelapseState.isPaused) Color(0xFFD29922) else Color(0xFFFF7B72),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${timelapseState.formattedDuration} • ${timelapseState.recordedFrameCount}f",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
