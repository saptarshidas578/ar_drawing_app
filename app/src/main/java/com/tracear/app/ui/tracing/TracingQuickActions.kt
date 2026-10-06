package com.tracear.app.ui.tracing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Always-visible floating quick action buttons docked to the screen edge:
 * 1. Lock/Unlock toggle.
 * 2. Hold to Peek (fades overlay out while held, restores on release).
 */
@Composable
fun TracingQuickActions(
    isLocked: Boolean,
    onToggleLock: () -> Unit,
    onPeekStart: () -> Unit,
    onPeekEnd: () -> Unit,
    isLeftHanded: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = if (isLeftHanded) Alignment.Start else Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- Lock / Unlock Button ---
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isLocked) Color(0xEE1F6FEB) else Color(0xCC21262D))
                .border(1.dp, Color(0x44FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onToggleLock,
                modifier = Modifier.size(48.dp)
            ) {
                Text(
                    text = if (isLocked) "🔒" else "🔓",
                    fontSize = 18.sp
                )
            }
        }

        // --- Hold to Peek Button ---
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xDD161B22))
                .border(1.dp, Color(0xFF58A6FF).copy(alpha = 0.6f), CircleShape)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        onPeekStart()
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                        onPeekEnd()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("👁️", fontSize = 16.sp)
                Text(
                    text = "PEEK",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF58A6FF)
                )
            }
        }
    }
}

/**
 * Tiny restore button displayed ONLY in Focus Mode.
 */
@Composable
fun FocusModeRestoreButton(
    onExitFocus: () -> Unit,
    isLeftHanded: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(14.dp)
            .size(42.dp)
            .clip(CircleShape)
            .background(Color(0xAA161B22))
            .border(1.dp, Color(0x55FFFFFF), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onExitFocus,
            modifier = Modifier.size(42.dp)
        ) {
            Text("👁️", fontSize = 16.sp)
        }
    }
}
