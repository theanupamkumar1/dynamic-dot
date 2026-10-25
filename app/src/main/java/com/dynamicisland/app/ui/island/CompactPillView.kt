package com.dynamicisland.app.ui.island

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicisland.app.model.*

/**
 * Compact Pill View perfectly implementing the Nothing OS Dynamic Island Concept:
 * - Minimal, functional, and uniquely Nothing monochrome UI language
 * - Dot-matrix elements & waveforms
 * - Red accents (#D71921) and Green (#00D26A) accents
 * - Left Wing: Rich glyph badge / Album art / Turn arrow
 * - Center: Clear information text / Dot-matrix waveform
 * - Right Wing: Live Action Controls / Circular status indicators / Pause buttons
 */
@Composable
fun CompactPillView(
    task: IslandTask?,
    cutoutGapDp: Float = 32f,
    theme: IslandTheme = IslandTheme.REGULAR,
    modifier: Modifier = Modifier
) {
    if (task == null) return

    val nothingRed = Color(0xFFD71921)
    val nothingGreen = Color(0xFF00D26A)
    val textFontFamily = if (theme == IslandTheme.NOTHING) FontFamily.Monospace else FontFamily.Default

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        when (task) {
            // 1. MUSIC & MEDIA PLAYBACK (AUTHENTIC APPLE iOS DYNAMIC ISLAND LOOK)
            is MediaTask -> {
                val mediaAccent = task.accentColor?.let { Color(it) } ?: Color(0xFF30D158)
                // Left Wing: Album Artwork or Apple Music Note
                Box(
                    modifier = Modifier.wrapContentSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (task.albumArt != null) {
                        Image(
                            bitmap = task.albumArt.asImageBitmap(),
                            contentDescription = "Album Art",
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFFFF2D55), Color(0xFFFA233B)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Music",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Center: Protected Camera Notch Cutout Gap (empty space so camera isn't obstructed)
                Spacer(modifier = Modifier.width(cutoutGapDp.dp))

                // Right Wing: Rhythm-Reactive Apple Animated Equalizer Bars
                Box(
                    modifier = Modifier.wrapContentSize(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    AudioVisualizerView(
                        isPlaying = task.isPlaying,
                        barColor = if (theme == IslandTheme.NOTHING) Color(0xFFD71921) else mediaAccent,
                        theme = theme,
                        width = 18.dp,
                        height = 14.dp
                    )
                }
            }

            // 2. INCOMING & ONGOING CALLS
            is CallTask -> {
                // Left: Phone Badge
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = nothingGreen,
                        modifier = Modifier.size(13.dp)
                    )
                }

                // Center: Caller Name
                Text(
                    text = task.callerName.ifEmpty { "Anupam" },
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = textFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Right: Accept & Decline Buttons for Incoming, Duration for Ongoing
                if (task.isIncoming) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Decline (Red Circle)
                        Box(
                            modifier = Modifier
                                .size(19.dp)
                                .clip(CircleShape)
                                .background(nothingRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        // Accept (Green Circle)
                        Box(
                            modifier = Modifier
                                .size(19.dp)
                                .clip(CircleShape)
                                .background(nothingGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Accept",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = formatSeconds(task.durationSeconds),
                        color = nothingGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = textFontFamily
                    )
                }
            }

            // 3. TIMER / STOPWATCH
            is TimerTask -> {
                // Left: Hourglass in badge
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = "Timer",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }

                // Center: Countdown Digits
                Text(
                    text = formatSeconds(task.remainingSeconds),
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = textFontFamily
                )

                // Right: Pause Icon
                Icon(
                    imageVector = if (task.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Toggle",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }

            // 4. NAVIGATION / MAPS
            is NavigationTask -> {
                // Left: Turn Arrow
                Icon(
                    imageVector = when (task.direction) {
                        NavDirection.TURN_LEFT, NavDirection.TURN_SLIGHT_LEFT -> Icons.Default.TurnLeft
                        else -> Icons.Default.TurnRight
                    },
                    contentDescription = "Maneuver",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )

                // Center: Distance
                Text(
                    text = task.distanceText,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = textFontFamily
                )

                // Right: Navigation / Compass Badge
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Compass",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            // 5. VOICE RECORDING
            is VoiceRecordingTask -> {
                // Left: Pulsing Red Recording Dot
                val transition = rememberInfiniteTransition(label = "rec_pulse")
                val alpha by transition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "rec_alpha"
                )
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(nothingRed.copy(alpha = alpha))
                )

                // Center: Duration
                Text(
                    text = formatSeconds(task.durationSeconds),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = textFontFamily
                )

                // Right: Mini waveform + chevron
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    AudioVisualizerView(
                        isPlaying = task.isRecording,
                        barColor = Color.White,
                        theme = IslandTheme.NOTHING,
                        width = 14.dp,
                        height = 11.dp
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "More",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // 6. CHARGING ALERT
            is ChargingAlert -> {
                val batteryColor = when {
                    task.isLowBattery -> Color(0xFFEF4444)
                    task.isFullyCharged -> Color(0xFF3B82F6)
                    else -> nothingGreen
                }
                // Left: Battery or Lightning Icon
                Icon(
                    imageVector = if (task.isLowBattery) Icons.Default.BatteryAlert else Icons.Default.Bolt,
                    contentDescription = "Battery Alert",
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )

                // Center: Percentage
                Text(
                    text = "${task.batteryPercent}%",
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = textFontFamily
                )

                // Right: Circular Progress Ring with Active Arc
                Canvas(modifier = Modifier.size(17.dp)) {
                    val strokeW = 2.dp.toPx()
                    // Track
                    drawCircle(
                        color = Color.White.copy(alpha = 0.2f),
                        style = Stroke(strokeW)
                    )
                    // Colored Arc
                    drawArc(
                        color = batteryColor,
                        startAngle = -90f,
                        sweepAngle = ((task.batteryPercent / 100f).coerceIn(0f, 1f)) * 360f,
                        useCenter = false,
                        style = Stroke(strokeW)
                    )
                }
            }

            // 7. EARBUDS CONNECTED (Nothing Ear)
            is EarbudsTask -> {
                // Left: Earbuds Icon
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = "Earbuds",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )

                // Center: L/R Battery levels
                Text(
                    text = "L ${task.leftBattery}%   R ${task.rightBattery}%",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = textFontFamily
                )

                // Right: Chevron
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Open",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }

            // 8. HOTSPOT / TETHERING
            is HotspotTask -> {
                // Left: Hotspot icon in badge
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiTethering,
                        contentDescription = "Hotspot",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }

                // Center: Connected devices count
                Text(
                    text = "${task.connectedDevices} device",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = textFontFamily
                )

                // Right: Signal wave badge
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Active",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }

            // 9. SILENT MODE / RINGER ALERTS
            is RingerAlert -> {
                // Left: Bell with slash in subtle red-tinted badge
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(nothingRed.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = "Silent",
                        tint = nothingRed,
                        modifier = Modifier.size(13.dp)
                    )
                }

                // Center: Silent mode
                Text(
                    text = when (task.state) {
                        RingerState.SILENT -> "Silent mode"
                        RingerState.VIBRATE -> "Vibrate mode"
                        RingerState.RING -> "Ring active"
                    },
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = textFontFamily
                )

                Spacer(modifier = Modifier.width(4.dp))
            }

            // 10. FACE UNLOCK
            is FaceUnlockAlert -> {
                // Left: Face scan icon
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = "Face Unlock",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )

                // Center: Unlocking...
                Text(
                    text = "Unlocking...",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = textFontFamily
                )

                // Right: Dotted spinner
                Canvas(modifier = Modifier.size(14.dp)) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        style = Stroke(1.5.dp.toPx())
                    )
                }
            }

            // 11. DO NOT DISTURB
            is DndAlert -> {
                // Left: Crescent moon in badge
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5E5CE6).copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = "DND",
                        tint = Color(0xFF5E5CE6),
                        modifier = Modifier.size(12.dp)
                    )
                }

                // Center: Do Not Disturb
                Text(
                    text = "Do Not Disturb",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = textFontFamily
                )

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Details",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }

            // 12. FLIGHT MODE
            is FlightModeAlert -> {
                // Left: Orange Airplane in badge
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9F0A).copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Flight,
                        contentDescription = "Flight",
                        tint = Color(0xFFFF9F0A),
                        modifier = Modifier.size(12.dp)
                    )
                }

                Text(
                    text = "Flight mode",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = textFontFamily
                )

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Details",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }

            // 13. BLUETOOTH CONNECTED
            is BluetoothAlert -> {
                // Left: Blue circle with Bluetooth icon
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = "Bluetooth",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }

                Text(
                    text = "Connected",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = textFontFamily
                )

                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Connected",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            // 14. SCREENSHOT SAVED
            is ScreenshotAlert -> {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = "System",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )

                Text(
                    text = "Screenshot saved",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = textFontFamily
                )

                // Thumbnail mini preview
                if (task.thumbnail != null) {
                    Image(
                        bitmap = task.thumbnail.asImageBitmap(),
                        contentDescription = "Preview",
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                    )
                }
            }

            // 15. NOTIFICATION TOAST
            is NotificationTask -> {
                if (task.largeIcon != null) {
                    Image(
                        bitmap = task.largeIcon.asImageBitmap(),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = "Message",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Text(
                    text = task.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = textFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(horizontal = 6.dp)
                )

                Text(
                    text = "now",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontFamily = textFontFamily,
                    maxLines = 1,
                    softWrap = false
                )
            }

            else -> {
                Spacer(modifier = Modifier.width(1.dp))
            }
        }
    }
}

private fun formatSeconds(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
