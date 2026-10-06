package com.tracear.app.ui.tutorial

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracear.app.R

data class TutorialStep(
    val iconEmoji: String,
    val titleRes: Int,
    val descRes: Int,
    val badgeColor: Color
)

/**
 * TutorialScreen — A friendly 5-step interactive onboarding walkthrough for TraceAR.
 */
@Composable
fun TutorialScreen(
    onDismiss: () -> Unit
) {
    val steps = remember {
        listOf(
            TutorialStep(
                iconEmoji = "📱",
                titleRes = R.string.tutorial_step_1_title,
                descRes = R.string.tutorial_step_1_desc,
                badgeColor = Color(0xFF58A6FF)
            ),
            TutorialStep(
                iconEmoji = "💡",
                titleRes = R.string.tutorial_step_2_title,
                descRes = R.string.tutorial_step_2_desc,
                badgeColor = Color(0xFFFFD600)
            ),
            TutorialStep(
                iconEmoji = "🎯",
                titleRes = R.string.tutorial_step_3_title,
                descRes = R.string.tutorial_step_3_desc,
                badgeColor = Color(0xFF3FB950)
            ),
            TutorialStep(
                iconEmoji = "📐",
                titleRes = R.string.tutorial_step_4_title,
                descRes = R.string.tutorial_step_4_desc,
                badgeColor = Color(0xFFBC8CFF)
            ),
            TutorialStep(
                iconEmoji = "✏️",
                titleRes = R.string.tutorial_step_5_title,
                descRes = R.string.tutorial_step_5_desc,
                badgeColor = Color(0xFF00E5FF)
            )
        )
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = steps[currentStepIndex]
    val isLastStep = currentStepIndex == steps.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E14),
                        Color(0xFF131923),
                        Color(0xFF0A0E14)
                    )
                )
            )
            .padding(24.dp)
    ) {
        // --- Top Row: App Label & Skip Button ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF58A6FF))
                )
                Text(
                    text = stringResource(R.string.app_name),
                    color = Color(0xFF8B949E),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            if (!isLastStep) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.tutorial_skip),
                        color = Color(0xFF8B949E),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // --- Center Content: Card with Illustration, Title, Description ---
        AnimatedContent(
            targetState = currentStepIndex,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.align(Alignment.Center),
            label = "TutorialStepAnimation"
        ) { targetIndex ->
            val cur = steps[targetIndex]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                // Large Visual Icon Container
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(cur.badgeColor.copy(alpha = 0.12f))
                        .border(2.dp, cur.badgeColor.copy(alpha = 0.40f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cur.iconEmoji,
                        fontSize = 58.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF0161B22)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF30363D), Color(0xFF21262D))
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(cur.titleRes),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = stringResource(cur.descRes),
                            color = Color(0xFFC9D1D9),
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // --- Bottom Controls: Progress Dots & Next/Back Buttons ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 5 Dot Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.indices.forEach { idx ->
                    val isActive = idx == currentStepIndex
                    Box(
                        modifier = Modifier
                            .width(if (isActive) 24.dp else 7.dp)
                            .height(7.dp)
                            .then(
                                if (isActive) {
                                    Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(step.badgeColor)
                                } else {
                                    Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF30363D))
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Back & Next/Get Started Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStepIndex > 0) {
                    OutlinedButton(
                        onClick = { currentStepIndex-- },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(
                            text = stringResource(R.string.tutorial_back),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Button(
                    onClick = {
                        if (isLastStep) {
                            onDismiss()
                        } else {
                            currentStepIndex++
                        }
                    },
                    modifier = Modifier
                        .weight(if (currentStepIndex > 0) 1.5f else 1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLastStep) Color(0xFF238636) else Color(0xFF1F6FEB)
                    )
                ) {
                    Text(
                        text = if (isLastStep) stringResource(R.string.tutorial_get_started) else stringResource(R.string.tutorial_next),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
