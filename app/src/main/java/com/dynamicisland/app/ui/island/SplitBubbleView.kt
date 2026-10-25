package com.dynamicisland.app.ui.island

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicisland.app.model.*

@Composable
fun SplitBubbleView(
    task: IslandTask?,
    sizeDp: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (task == null) return

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(Color.Black)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when (task) {
            is TimerTask -> {
                val min = task.remainingSeconds / 60
                val sec = task.remainingSeconds % 60
                Text(
                    text = String.format("%d:%02d", min, sec),
                    color = Color(0xFFFF9F0A), // iOS Amber
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            is MediaTask -> {
                AudioVisualizerView(
                    isPlaying = task.isPlaying,
                    barColor = Color(0xFF30D158),
                    width = 14.dp,
                    height = 12.dp
                )
            }
            is NavigationTask -> {
                Icon(
                    imageVector = when (task.direction) {
                        NavDirection.TURN_LEFT, NavDirection.TURN_SLIGHT_LEFT -> Icons.Default.TurnLeft
                        NavDirection.TURN_RIGHT, NavDirection.TURN_SLIGHT_RIGHT -> Icons.Default.TurnRight
                        NavDirection.U_TURN -> Icons.Default.UTurnLeft
                        NavDirection.ROUNDABOUT -> Icons.Default.Refresh
                        else -> Icons.Default.Straight
                    },
                    contentDescription = "Nav",
                    tint = Color(0xFF0A84FF), // iOS Blue
                    modifier = Modifier.size(16.dp)
                )
            }
            is CallTask -> {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    tint = Color(0xFF30D158),
                    modifier = Modifier.size(16.dp)
                )
            }
            is ChargingAlert -> {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Charging",
                    tint = Color(0xFF30D158),
                    modifier = Modifier.size(16.dp)
                )
            }
            is BluetoothAlert -> {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = "Bluetooth",
                    tint = Color(0xFF0A84FF),
                    modifier = Modifier.size(16.dp)
                )
            }
            is ProgressTask -> {
                Text(
                    text = "${task.progressPercent}%",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            is SecondaryCalibrationTask -> {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF30D158)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Alert",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
