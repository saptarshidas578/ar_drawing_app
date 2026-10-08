package com.tracear.app.ui.tracing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compact bottom/side dock (~56 dp, semi-transparent):
 * One row/column of icon buttons with large touch targets (>= 48 dp)
 * for the 6 primary categories: Opacity, Transform, Sections, Adjust, Lines, View.
 */
@Composable
fun TracingDock(
    activeCategory: DockCategory?,
    onSelectCategory: (DockCategory) -> Unit,
    isLandscape: Boolean = false,
    modifier: Modifier = Modifier
) {
    val categories = DockCategory.entries

    if (!isLandscape) {
        // --- Portrait: Horizontal Bottom Dock ---
        Row(
            modifier = modifier
                .fillMaxWidth(0.98f)
                .height(58.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xDD161B22))
                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(22.dp))
                .padding(horizontal = 2.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            categories.forEach { cat ->
                val isSelected = activeCategory == cat
                DockItem(
                    category = cat,
                    isSelected = isSelected,
                    onClick = { onSelectCategory(cat) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        // --- Landscape: Vertical Side Dock ---
        Column(
            modifier = modifier
                .width(62.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xDD161B22))
                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                .padding(vertical = 6.dp, horizontal = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = activeCategory == cat
                DockItem(
                    category = cat,
                    isSelected = isSelected,
                    onClick = { onSelectCategory(cat) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )
            }
        }
    }
}

@Composable
private fun DockItem(
    category: DockCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) Color(0xFF1F6FEB) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White)
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = category.icon,
                fontSize = 17.sp
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = category.label,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF8B949E)
            )
        }
    }
}
