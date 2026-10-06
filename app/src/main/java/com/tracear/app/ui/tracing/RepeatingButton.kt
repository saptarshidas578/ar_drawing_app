package com.tracear.app.ui.tracing

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A Button that triggers once on initial press and continuously repeats
 * its action while being held down.
 */
@Composable
fun RepeatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    initialDelayMs: Long = 320L,
    repeatIntervalMs: Long = 65L,
    shape: Shape = RoundedCornerShape(8.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFF30363D),
        contentColor = Color.White
    ),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed, enabled) {
        if (isPressed && enabled) {
            currentOnClick()
            delay(initialDelayMs)
            while (isPressed && enabled) {
                currentOnClick()
                delay(repeatIntervalMs)
            }
        }
    }

    Button(
        onClick = {},
        interactionSource = interactionSource,
        enabled = enabled,
        modifier = modifier,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding
    ) {
        content()
    }
}
