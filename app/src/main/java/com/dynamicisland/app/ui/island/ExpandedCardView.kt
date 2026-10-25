package com.dynamicisland.app.ui.island

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicisland.app.manager.IslandStateManager
import com.dynamicisland.app.model.*
import com.dynamicisland.app.model.TimerTask
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-End Expanded Card View reflecting the Nothing OS Dynamic Island Concept:
 * - Pure OLED black background with subtle white specular glass border
 * - Dotted circular rings for Charging, Timers, Hotspots
 * - Dotted progress scrubbers (•••••••|••••••••)
 * - Solid White circular action buttons with black icons
 * - Iconic Nothing Ear transparent earbud graphic with red & white indicators
 * - Minimal, functional, dot-matrix inspired elements
 */
val LocalCardTheme = compositionLocalOf { IslandTheme.REGULAR }

@Composable
fun getCardFont(): FontFamily {
    return if (LocalCardTheme.current == IslandTheme.NOTHING) FontFamily.Monospace else FontFamily.Default
}

@Composable
fun ExpandedCardView(
    task: IslandTask?,
    stateManager: IslandStateManager,
    theme: IslandTheme = IslandTheme.REGULAR,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalCardTheme provides theme) {
        if (task == null || task is AmbientTask) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                ExpandedAmbientContent(stateManager = stateManager, onCollapse = onCollapse)
            }
            return@CompositionLocalProvider
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            when (task) {
                is MediaTask -> ExpandedMediaContent(task, stateManager, theme)
                is CallTask -> ExpandedCallContent(task, stateManager, onCollapse)
                is TimerTask -> ExpandedTimerContent(task, stateManager)
                is NavigationTask -> ExpandedNavigationContent(task, stateManager)
                is VoiceRecordingTask -> ExpandedVoiceRecordingContent(task, stateManager)
                is ChargingAlert -> ExpandedChargingContent(task)
                is EarbudsTask -> ExpandedEarbudsContent(task)
                is HotspotTask -> ExpandedHotspotContent(task, stateManager)
                is RingerAlert -> ExpandedRingerContent(task)
                is ProgressTask -> ExpandedProgressContent(task)
                is NotificationTask -> ExpandedNotificationContent(task, stateManager, onCollapse)
                else -> ExpandedGenericContent(task)
            }
        }
    }
}

// ==========================================
// 1. MUSIC & MEDIA PLAYBACK (EXPANDED - APPLE iOS DYNAMIC ISLAND)
// ==========================================
@Composable
private fun ExpandedMediaContent(
    task: MediaTask,
    stateManager: IslandStateManager,
    theme: IslandTheme = IslandTheme.REGULAR
) {
    val durationMs = task.trackDurationMs.coerceAtLeast(1L)
    val currentMs = task.playbackPositionMs.coerceIn(0L, durationMs)
    val progressFraction = (currentMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    val mediaAccent = task.accentColor?.let { Color(it) } ?: Color(0xFF30D158)
    val font = if (theme == IslandTheme.NOTHING) FontFamily.Monospace else FontFamily.Default

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top row: Large Album Art + Track Info + iOS Audio Visualizer (Click opens player app)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                    stateManager.collapseIsland()
                    stateManager.onAppLaunchRequest?.invoke(task.packageName)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Artwork (iOS squircle with specular glass rim)
            if (task.albumArt != null) {
                Image(
                    bitmap = task.albumArt.asImageBitmap(),
                    contentDescription = "Cover",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .border(0.75.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(13.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFF2D55), Color(0xFFFA233B))
                            )
                        )
                        .border(0.75.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Track title & Artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title.ifEmpty { "Not Playing" },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = font,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = task.artist.ifEmpty { "Music" },
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 13.5.sp,
                    fontFamily = font,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Apple Fluid Audio Visualizer
            AudioVisualizerView(
                isPlaying = task.isPlaying,
                barColor = if (theme == IslandTheme.NOTHING) Color(0xFFD71921) else mediaAccent,
                theme = theme,
                width = 24.dp,
                height = 20.dp
            )
        }

        // Middle: Genuine Apple iOS Continuous Progress Scrubber with Scrub Handle & Timestamps
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .pointerInput(durationMs) {
                        detectTapGestures { offset ->
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val targetMs = (fraction * durationMs).toLong()
                            stateManager.onMediaSeek?.invoke(targetMs)
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                val totalWidth = maxWidth
                val currentWidth = totalWidth * progressFraction

                // Inactive track (smooth translucent pill)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.24f))
                )
                // Active track (Pure White or Artwork Accent)
                Box(
                    modifier = Modifier
                        .width(currentWidth)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                // Apple circular scrubber thumb handle
                Box(
                    modifier = Modifier
                        .offset(x = (currentWidth - 5.dp).coerceAtLeast(0.dp))
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }

            // iOS Timestamps: Elapsed on left, Remaining on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentMs),
                    color = Color.White.copy(alpha = 0.50f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = font
                )
                Text(
                    text = "-${formatTime((durationMs - currentMs).coerceAtLeast(0L))}",
                    color = Color.White.copy(alpha = 0.50f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = font
                )
            }
        }

        // Bottom: Apple iOS Media Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: AirPlay / Audio output button
            IconButton(
                onClick = {
                    stateManager.collapseIsland()
                    stateManager.onAppLaunchRequest?.invoke(task.packageName)
                },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Airplay,
                    contentDescription = "AirPlay",
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Center: Prev, Play/Pause, Next
            Row(
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip Previous
                IconButton(
                    onClick = { stateManager.onMediaPrevious?.invoke() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play / Pause (Apple style solid white circular button with black icon)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { stateManager.onMediaPlayPauseToggle?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (task.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Skip Next
                IconButton(
                    onClick = { stateManager.onMediaNext?.invoke() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Right: Headphones / Output route icon
            IconButton(
                onClick = {
                    stateManager.collapseIsland()
                    stateManager.onAppLaunchRequest?.invoke(task.packageName)
                },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = "Audio Output",
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==========================================
// 2. INCOMING & ONGOING CALLS (EXPANDED)
// ==========================================
@Composable
private fun ExpandedCallContent(
    task: CallTask,
    stateManager: IslandStateManager,
    onCollapse: () -> Unit
) {
    val nothingRed = Color(0xFFD71921)
    val nothingGreen = Color(0xFF00D26A)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Avatar with Letter Initial
        val initial = (task.callerName.firstOrNull() ?: 'A').uppercaseChar()
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color(0xFF282828))
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial.toString(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = getCardFont()
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Center: Caller Name + Subtext
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.callerName.ifEmpty { "Anupam" },
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = getCardFont(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (task.isIncoming) "Incoming call" else "Active • ${formatSeconds(task.durationSeconds)}",
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 13.sp,
                fontFamily = getCardFont()
            )
        }

        // Right: Circular Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decline / Hangup (Red Circle)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(nothingRed)
                    .clickable {
                        stateManager.onCallHangup?.invoke()
                        stateManager.removeTask(task.id)
                        onCollapse()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Decline",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Accept (Green Circle) for incoming
            if (task.isIncoming) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(nothingGreen)
                        .clickable {
                            stateManager.onCallAnswer?.invoke()
                            stateManager.removeTask(task.id)
                            stateManager.postTask(task.copy(isIncoming = false))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Accept",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. TIMER / STOPWATCH (EXPANDED)
// ==========================================
@Composable
private fun ExpandedTimerContent(
    task: TimerTask,
    stateManager: IslandStateManager
) {
    val total = task.totalSeconds.coerceAtLeast(1L)
    val progress = (task.remainingSeconds.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Nothing Dot Ring with Hourglass
        NothingDotRing(
            progress = progress,
            sizeDp = 58.dp,
            dotCount = 28,
            activeColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.HourglassEmpty,
                contentDescription = "Timer",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Center: Label + Large Time
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.label.ifEmpty { "Timer" },
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 12.sp,
                fontFamily = getCardFont()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatSeconds(task.remainingSeconds),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = getCardFont()
            )
        }

        // Right Action Buttons: Cancel (X) and Pause/Resume (Solid White Circle)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel (X)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(0.75.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                .clickable {
                    stateManager.onTimerCancel?.invoke()
                    stateManager.removeTask(task.id)
                    stateManager.collapseIsland()
                },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Pause / Resume (Solid White Circle with Black Icon)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable {
                        stateManager.postTask(task.copy(isRunning = !task.isRunning))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (task.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==========================================
// 4. NAVIGATION / MAPS (EXPANDED)
// ==========================================
@Composable
private fun ExpandedNavigationContent(
    task: NavigationTask,
    stateManager: IslandStateManager
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bold Turn Right/Left Arrow
            Icon(
                imageVector = when (task.direction) {
                    NavDirection.TURN_LEFT, NavDirection.TURN_SLIGHT_LEFT -> Icons.Default.TurnLeft
                    else -> Icons.Default.TurnRight
                },
                contentDescription = "Turn",
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.distanceText,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = getCardFont()
                )
                Text(
                    text = task.instruction.ifEmpty { "Turn right" },
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.5.sp,
                    fontFamily = getCardFont()
                )
                Text(
                    text = "3.2 km • ${task.etaText}",
                    color = Color.White.copy(alpha = 0.50f),
                    fontSize = 11.5.sp,
                    fontFamily = getCardFont()
                )
            }
        }

        // Bottom Progress Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.20f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.65f)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

// ==========================================
// 5. VOICE RECORDING (EXPANDED)
// ==========================================
@Composable
private fun ExpandedVoiceRecordingContent(
    task: VoiceRecordingTask,
    stateManager: IslandStateManager
) {
    val nothingRed = Color(0xFFD71921)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(nothingRed)
            )
            Text(
                text = "Recording...",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 11.5.sp,
                fontFamily = getCardFont()
            )
        }

        // Center: Audio Waveform + Duration
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AudioVisualizerView(
                isPlaying = task.isRecording,
                barColor = Color.White,
                theme = IslandTheme.NOTHING,
                width = 54.dp,
                height = 28.dp
            )

            Text(
                text = formatSeconds(task.durationSeconds),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = getCardFont()
            )

            // Right Action Buttons: Stop (White Circle) & Pause
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stop (Solid White Circle with Black Square)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            stateManager.removeTask(task.id)
                            stateManager.collapseIsland()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.Black)
                    )
                }

                // Pause
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(0.75.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable {
                            stateManager.postTask(task.copy(isRecording = !task.isRecording))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (task.isRecording) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 6. CHARGING ALERT (EXPANDED)
// ==========================================
@Composable
private fun ExpandedChargingContent(task: ChargingAlert) {
    val ringColor = when {
        task.isLowBattery -> Color(0xFFEF4444)
        task.isFullyCharged -> Color(0xFF3B82F6)
        else -> Color(0xFF00D26A)
    }

    val subtitle = when {
        task.isLowBattery -> "Low Battery (< 15%)"
        task.isFullyCharged -> "Battery Full (100%)"
        task.wattage.isNotEmpty() -> task.wattage
        else -> "Charging"
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Nothing Dot Ring with Lightning Bolt
        NothingDotRing(
            progress = (task.batteryPercent / 100f).coerceIn(0f, 1f),
            sizeDp = 58.dp,
            dotCount = 28,
            activeColor = ringColor
        ) {
            Icon(
                imageVector = if (task.isLowBattery) Icons.Default.BatteryAlert else Icons.Default.Bolt,
                contentDescription = "Battery Alert",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(20.dp))

        // Right: Large Percentage + Charging Subtitle
        Column {
            Text(
                text = "${task.batteryPercent}%",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = getCardFont(),
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 13.5.sp,
                fontFamily = getCardFont()
            )
        }
    }
}

// ==========================================
// 7. EARBUDS CONNECTED (Nothing Ear) (EXPANDED)
// ==========================================
@Composable
private fun ExpandedEarbudsContent(task: EarbudsTask) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Nothing Ear Graphic
        NothingEarGraphic(sizeDp = 52.dp)

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.deviceName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = getCardFont()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Battery Pills Row (L, R, Case)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                NothingBatteryPill(label = "L ${task.leftBattery}%", percent = task.leftBattery)
                NothingBatteryPill(label = "R ${task.rightBattery}%", percent = task.rightBattery)
                NothingBatteryPill(label = "Case ${task.caseBattery}%", percent = task.caseBattery)
            }
        }
    }
}

// ==========================================
// 8. HOTSPOT / TETHERING (EXPANDED)
// ==========================================
@Composable
private fun ExpandedHotspotContent(
    task: HotspotTask,
    stateManager: IslandStateManager
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Dotted Antenna Waves
        NothingHotspotWaves(sizeDp = 54.dp)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hotspot on",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = getCardFont()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${task.connectedDevices} device connected",
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 12.5.sp,
                fontFamily = getCardFont()
            )
        }

        // Right: "Turn off" Pill Button
        Button(
            onClick = {
                stateManager.removeTask(task.id)
                stateManager.collapseIsland()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202020)),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text(
                text = "Turn off",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = getCardFont(),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ==========================================
// 9. SILENT MODE / RINGER (EXPANDED)
// ==========================================
@Composable
private fun ExpandedRingerContent(task: RingerAlert) {
    val nothingRed = Color(0xFFD71921)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(nothingRed.copy(alpha = 0.18f))
                .border(1.dp, nothingRed.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsOff,
                contentDescription = "Silent",
                tint = nothingRed,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = "Silent mode on",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = getCardFont()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Notifications will be muted",
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 12.5.sp,
                fontFamily = getCardFont()
            )
        }
    }
}

// ==========================================
// 10. PROGRESS & SHARING (EXPANDED)
// ==========================================
@Composable
private fun ExpandedProgressContent(task: ProgressTask) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = task.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = getCardFont()
            )
            Text(
                text = "${task.progressPercent}%",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = getCardFont()
            )
        }

        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.20f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((task.progressPercent / 100f).coerceIn(0f, 1f))
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }

        Text(
            text = task.speedText,
            color = Color.White.copy(alpha = 0.50f),
            fontSize = 12.sp,
            fontFamily = getCardFont()
        )
    }
}

// ==========================================
// 11. NOTIFICATIONS (EXPANDED)
// ==========================================
@Composable
private fun ExpandedNotificationContent(
    task: NotificationTask,
    stateManager: IslandStateManager,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    var replyText by remember { mutableStateOf("") }
    val config = remember {
        try {
            com.dynamicisland.app.manager.SettingsRepository.getInstance(context).configFlow.value
        } catch (_: Exception) {
            com.dynamicisland.app.model.CutoutConfig()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header Row: Avatar / Icon + App Name & Title + Action to open app
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onCollapse()
                    if (task.contentIntent != null) {
                        try {
                            task.contentIntent.send()
                        } catch (_: Exception) {
                            stateManager.onAppLaunchRequest?.invoke(task.packageName)
                        }
                    } else {
                        stateManager.onAppLaunchRequest?.invoke(task.packageName)
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar or App Icon
            if (task.largeIcon != null) {
                Image(
                    bitmap = task.largeIcon.asImageBitmap(),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(0.75.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF).copy(alpha = 0.2f))
                        .border(0.75.dp, Color(0xFF007AFF).copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = "Message",
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title.ifEmpty { task.appName },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = getCardFont(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = task.appName,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontFamily = getCardFont()
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = task.message,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontFamily = getCardFont(),
                    maxLines = config.maxNotificationLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Optional BigPicture Image (e.g. WhatsApp photo, screenshot attachment)
        if (config.showNotificationImages && task.bigPicture != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Image(
                bitmap = task.bigPicture.asImageBitmap(),
                contentDescription = "Attachment",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            )
        }

        // Optional Action Buttons (e.g. "Mark as read", "Archive")
        if (task.actions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                task.actions.take(2).forEach { act ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable {
                                try {
                                    act.actionIntent?.send()
                                    onCollapse()
                                    stateManager.removeTask(task.id)
                                } catch (_: Exception) {}
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = act.title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = getCardFont()
                        )
                    }
                }
            }
        }

        // dynamicSpot prefActionReply: Quick Inline Reply text field with Send button
        if (config.enableInlineReply && task.replyActionIntent != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 13.sp,
                        fontFamily = getCardFont()
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF007AFF)),
                    decorationBox = { innerTextField ->
                        if (replyText.isEmpty()) {
                            Text(
                                text = task.replyLabel ?: "Reply to ${task.appName}...",
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 13.sp,
                                fontFamily = getCardFont()
                            )
                        }
                        innerTextField()
                    }
                )

                if (replyText.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF))
                            .clickable {
                                stateManager.sendInlineReply(
                                    context = context,
                                    replyIntent = task.replyActionIntent,
                                    resultKey = task.replyResultKey,
                                    replyText = replyText
                                )
                                replyText = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 12. AMBIENT NOTCH IDLE (EXPANDED)
// ==========================================
@Composable
private fun ExpandedAmbientContent(
    stateManager: IslandStateManager,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    val sdfTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val sdfDate = remember { SimpleDateFormat("EEE, dd MMM", Locale.getDefault()) }
    var timeText by remember { mutableStateOf(sdfTime.format(Date())) }
    var dateText by remember { mutableStateOf(sdfDate.format(Date())) }

    LaunchedEffect(Unit) {
        while (true) {
            timeText = sdfTime.format(Date())
            dateText = sdfDate.format(Date())
            kotlinx.coroutines.delay(1000L)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Center: Time and Date
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeText,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = getCardFont(),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateText,
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 13.sp,
                fontFamily = getCardFont()
            )
        }

        // Bottom Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nothing OS 3.0",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 11.sp,
                fontFamily = getCardFont()
            )

            Button(
                onClick = {
                    onCollapse()
                    try {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(launchIntent)
                    } catch (_: Exception) {}
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontFamily = getCardFont()
                )
            }
        }
    }
}

// ==========================================
// GENERIC FALLBACK
// ==========================================
@Composable
private fun ExpandedGenericContent(task: IslandTask) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = task.id,
            color = Color.White,
            fontSize = 15.sp,
            fontFamily = getCardFont()
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format("%d:%02d", mins, secs)
}

private fun formatSeconds(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}
