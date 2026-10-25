package com.dynamicisland.app.ui.island

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Nothing OS Dotted Circular Progress Ring
 * Used for Charging, Timer, Hotspot, and Countdown features.
 */
@Composable
fun NothingDotRing(
    progress: Float, // 0f to 1f
    modifier: Modifier = Modifier,
    sizeDp: Dp = 56.dp,
    dotCount: Int = 26,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.20f),
    content: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) - 4.dp.toPx()
            val dotRadius = 1.8.dp.toPx()
            val activeIndex = (dotCount * progress.coerceIn(0f, 1f)).toInt()

            for (i in 0 until dotCount) {
                // Start from top (-90 degrees) and rotate clockwise
                val angle = Math.toRadians((i * (360.0 / dotCount)) - 90.0)
                val x = center.x + (radius * cos(angle)).toFloat()
                val y = center.y + (radius * sin(angle)).toFloat()
                val isActive = i <= activeIndex
                val color = if (isActive) activeColor else inactiveColor

                drawCircle(
                    color = color,
                    radius = if (isActive) dotRadius * 1.15f else dotRadius,
                    center = Offset(x, y)
                )
            }
        }

        content?.invoke()
    }
}

/**
 * Nothing OS Dotted Horizontal Scrubber Line
 * A track of dots where played dots are bright white,
 * the scrubber cursor is a clean vertical bar,
 * and remaining dots are dim gray.
 */
@Composable
fun NothingDotScrubber(
    currentMs: Long,
    durationMs: Long,
    onSeek: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val duration = durationMs.coerceAtLeast(1L)
    val current = currentMs.coerceIn(0L, duration)
    val progress = (current.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .pointerInput(duration) {
                if (onSeek != null) {
                    detectTapGestures { offset ->
                        val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek((frac * duration).toLong())
                    }
                }
            }
            .pointerInput(duration) {
                if (onSeek != null) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek((frac * duration).toLong())
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalW = size.width
            val centerY = size.height / 2f
            val dotSpacing = 7.dp.toPx()
            val dotCount = (totalW / dotSpacing).toInt().coerceAtLeast(10)
            val dotRadius = 1.6.dp.toPx()
            val activeIndex = (dotCount * progress).toInt()

            for (i in 0 until dotCount) {
                val x = (i * dotSpacing) + (dotSpacing / 2f)
                val isActive = i < activeIndex
                drawCircle(
                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.22f),
                    radius = dotRadius,
                    center = Offset(x, centerY)
                )
            }

            // Scrubber Cursor Bar (|)
            val cursorX = (totalW * progress).coerceIn(4f, totalW - 4f)
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(cursorX - 1.5.dp.toPx(), centerY - 5.dp.toPx()),
                size = Size(3.dp.toPx(), 10.dp.toPx()),
                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
            )
        }
    }
}

/**
 * Nothing OS Earbuds Battery Indicator Pill
 * (e.g. "L 90%" with green level bar)
 */
@Composable
fun NothingBatteryPill(
    label: String, // e.g. "L 90%"
    percent: Int,
    modifier: Modifier = Modifier
) {
    val nothingGreen = Color(0xFF00D26A)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            letterSpacing = (-0.2).sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        // Rounded Mini Level Bar
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.20f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
                    .clip(CircleShape)
                    .background(nothingGreen)
            )
        }
    }
}

/**
 * Nothing Ear (2) Iconic Graphic
 * Left earbud: white accent dot.
 * Right earbud: Nothing Red accent dot.
 */
@Composable
fun NothingEarGraphic(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 48.dp
) {
    val nothingRed = Color(0xFFD71921)

    Canvas(modifier = modifier.size(sizeDp)) {
        val w = size.width
        val h = size.height
        val stemW = w * 0.16f
        val stemH = h * 0.58f

        // Left Earbud (White indicator)
        val leftX = w * 0.28f
        val leftY = h * 0.20f
        // Bud head
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = w * 0.16f,
            center = Offset(leftX, leftY)
        )
        // Transparent stem
        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            topLeft = Offset(leftX - stemW / 2f, leftY + w * 0.08f),
            size = Size(stemW, stemH),
            cornerRadius = CornerRadius(stemW / 2f, stemW / 2f),
            style = Stroke(width = 1.5.dp.toPx())
        )
        // White dot on left stem
        drawCircle(
            color = Color.White,
            radius = stemW * 0.35f,
            center = Offset(leftX, leftY + stemH * 0.55f)
        )

        // Right Earbud (Red indicator)
        val rightX = w * 0.72f
        val rightY = h * 0.20f
        // Bud head
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = w * 0.16f,
            center = Offset(rightX, rightY)
        )
        // Transparent stem
        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            topLeft = Offset(rightX - stemW / 2f, rightY + w * 0.08f),
            size = Size(stemW, stemH),
            cornerRadius = CornerRadius(stemW / 2f, stemW / 2f),
            style = Stroke(width = 1.5.dp.toPx())
        )
        // Nothing Red dot on right stem
        drawCircle(
            color = nothingRed,
            radius = stemW * 0.35f,
            center = Offset(rightX, rightY + stemH * 0.55f)
        )
    }
}

/**
 * Concentric Dotted Antenna Waves for Hotspot Feature
 */
@Composable
fun NothingHotspotWaves(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 56.dp
) {
    Canvas(modifier = modifier.size(sizeDp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radii = listOf(size.width * 0.22f, size.width * 0.36f, size.width * 0.48f)

        radii.forEachIndexed { ringIdx, r ->
            val count = 8 + (ringIdx * 6)
            for (i in 0 until count) {
                val angle = Math.toRadians((i * (360.0 / count)))
                val x = center.x + (r * cos(angle)).toFloat()
                val y = center.y + (r * sin(angle)).toFloat()
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f + (ringIdx * 0.25f)),
                    radius = 1.6.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}
