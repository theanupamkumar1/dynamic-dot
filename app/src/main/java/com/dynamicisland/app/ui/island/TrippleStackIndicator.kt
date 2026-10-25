package com.dynamicisland.app.ui.island

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Honor Magic Capsule style multi-task pagination dots
 */
@Composable
fun TrippleStackIndicator(
    taskCount: Int,
    currentIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    if (taskCount <= 1) return

    val displayCount = taskCount.coerceAtMost(3)

    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until displayCount) {
            val isSelected = i == currentIndex
            val width by animateDpAsState(
                targetValue = if (isSelected) 8.dp else 4.dp,
                animationSpec = tween(250),
                label = "dot_w"
            )
            val color by animateColorAsState(
                targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.35f),
                animationSpec = tween(250),
                label = "dot_color"
            )

            Box(
                modifier = Modifier
                    .height(3.5.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
