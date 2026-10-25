package com.dynamicisland.app.ui.island

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dynamicisland.app.model.IslandTheme

@Composable
fun AudioVisualizerView(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 4,
    width: Dp = 18.dp,
    height: Dp = 16.dp,
    barColor: Color = Color(0xFF30D158), // Apple green
    theme: IslandTheme = IslandTheme.REGULAR
) {
    val heights = if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "audio_bars")

        val anim1 by infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(420, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar1"
        )

        val anim2 by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 0.30f,
            animationSpec = infiniteRepeatable(
                animation = tween(380, delayMillis = 100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar2"
        )

        val anim3 by infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(480, delayMillis = 200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar3"
        )

        val anim4 by infiniteTransition.animateFloat(
            initialValue = 0.70f,
            targetValue = 0.20f,
            animationSpec = infiniteRepeatable(
                animation = tween(340, delayMillis = 80, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar4"
        )

        listOf(anim1, anim2, anim3, anim4)
    } else {
        // When paused / not playing: static resting bars with ZERO motion
        listOf(0.25f, 0.45f, 0.30f, 0.20f)
    }

    val nothingRed = Color(0xFFD71921)

    Canvas(
        modifier = modifier
            .width(width)
            .height(height)
    ) {
        val totalWidth = size.width
        val totalHeight = size.height
        val barWidth = totalWidth / (barCount * 1.8f)
        val spacing = (totalWidth - (barWidth * barCount)) / (barCount - 1).coerceAtLeast(1)

        if (theme == IslandTheme.NOTHING) {
            // Nothing OS Glyph Equalizer: Matrix segments with Nothing Red peak dot
            val dotSize = barWidth
            val maxDots = (totalHeight / (dotSize * 1.5f)).toInt().coerceAtLeast(3)

            for (i in 0 until barCount) {
                val hFactor = heights[i % heights.size]
                val activeDots = (maxDots * hFactor).toInt().coerceIn(1, maxDots)
                val left = i * (barWidth + spacing)

                for (d in 0 until activeDots) {
                    val isPeak = d == activeDots - 1 && isPlaying
                    val dotColor = if (isPeak) nothingRed else Color.White
                    val dotTop = totalHeight - ((d + 1) * dotSize * 1.4f)
                    drawCircle(
                        color = dotColor.copy(alpha = if (isPlaying) 1f else 0.45f),
                        radius = dotSize / 2.2f,
                        center = Offset(left + dotSize / 2f, dotTop + dotSize / 2f)
                    )
                }
            }
        } else {
            // Regular Apple iOS Smooth Equalizer
            for (i in 0 until barCount) {
                val hFactor = heights[i % heights.size]
                val currentBarH = (totalHeight * hFactor).coerceAtLeast(barWidth)
                val left = i * (barWidth + spacing)
                val top = (totalHeight - currentBarH) / 2f

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            barColor,
                            barColor.copy(alpha = if (isPlaying) 0.85f else 0.45f)
                        )
                    ),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, currentBarH),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}
