package com.dynamicisland.app.ui.island

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import com.dynamicisland.app.manager.IslandStateManager
import com.dynamicisland.app.manager.SettingsRepository
import com.dynamicisland.app.model.BluetoothAlert
import com.dynamicisland.app.model.CalibrationTask
import com.dynamicisland.app.model.CallTask
import com.dynamicisland.app.model.ChargingAlert
import com.dynamicisland.app.model.EarbudsTask
import com.dynamicisland.app.model.IslandMode
import com.dynamicisland.app.model.MediaTask
import com.dynamicisland.app.model.NotificationTask
import com.dynamicisland.app.model.ScreenshotAlert
import com.dynamicisland.app.model.IslandTheme
import kotlin.math.abs

@Composable
fun DynamicIslandOverlay(
    settingsRepo: SettingsRepository,
    stateManager: IslandStateManager,
    modifier: Modifier = Modifier
) {
    val config by settingsRepo.configFlow.collectAsState()
    val mode by stateManager.islandMode.collectAsState()
    val primaryTask by stateManager.primaryTask.collectAsState()
    val secondaryTask by stateManager.secondaryTask.collectAsState()
    val taskCount by stateManager.taskCount.collectAsState()

    val view = LocalView.current
    val isForegroundMatching by stateManager.isForegroundAppMatching.collectAsState()

    if (mode == IslandMode.HIDDEN || primaryTask == null || (config.hideInForeground && isForegroundMatching)) {
        return
    }

    val isCalibrating = primaryTask is CalibrationTask

    // Live calibration slider drag: snap instantly (0ms) so position/size changes reflect live at 120 FPS!
    // Normal usage: Apple rubber-band bouncy spring!
    val islandSpring = if (isCalibrating) {
        snap()
    } else if (config.fastAnimations) {
        spring<androidx.compose.ui.unit.Dp>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        )
    } else {
        spring<androidx.compose.ui.unit.Dp>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    }

    // Animated dimensions based on mode
    val targetWidth = when {
        mode == IslandMode.EXPANDED -> config.expandedWidth.dp
        mode == IslandMode.COLLAPSED_SPLIT || (primaryTask as? CalibrationTask)?.showSplit == true -> (config.pillWidth - 32f).dp
        isCalibrating -> config.pillWidth.dp
        mode == IslandMode.AMBIENT -> (config.cutoutGap + 16f).dp
        primaryTask is NotificationTask -> (config.pillWidth + 72f).dp.coerceAtLeast(180.dp)
        primaryTask is CallTask -> (config.pillWidth + 60f).dp.coerceAtLeast(170.dp)
        primaryTask is EarbudsTask -> (config.pillWidth + 70f).dp.coerceAtLeast(180.dp)
        primaryTask is ScreenshotAlert -> (config.pillWidth + 60f).dp.coerceAtLeast(170.dp)
        else -> config.pillWidth.dp
    }

    val targetHeight = when {
        mode == IslandMode.EXPANDED -> config.expandedHeight.dp
        else -> config.pillHeight.dp
    }

    val targetRadius = when {
        mode == IslandMode.EXPANDED -> 32.dp
        else -> config.pillRadius.dp
    }

    val animatedWidth by animateDpAsState(targetValue = targetWidth, animationSpec = islandSpring, label = "island_w")
    val animatedHeight by animateDpAsState(targetValue = targetHeight, animationSpec = islandSpring, label = "island_h")
    val animatedRadius by animateDpAsState(targetValue = targetRadius, animationSpec = islandSpring, label = "island_r")

    val isExpanded = mode == IslandMode.EXPANDED
    val nothingRed = Color(0xFFD71921)

    // dynamicSpot Glow Ring pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "glow_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val mediaAccentColor = (primaryTask as? MediaTask)?.accentColor?.let { Color(it) }
    val isGlowRingActive = config.enableGlowRing && (primaryTask is ChargingAlert || primaryTask is NotificationTask || primaryTask is BluetoothAlert)

    // Gesture handling:
    // Collapsed:
    //   - Drag down / Long press: Expand
    //   - Drag left (taskCount > 1): Switch to next activity (Honor / Apple multitasking)
    //   - Drag right (taskCount > 1): Switch to previous activity
    //   - Tap: Expand or open app
    // Expanded:
    //   - Drag up: Collapse
    //   - Drag left: Remove from island AND stop that task/activity
    // Gesture handling:
    // Collapsed (Single, Split, Tripple, Ambient):
    //   - Tap: Expand island immediately!
    //   - Drag down: Expand island
    //   - Horizontal swipe (taskCount > 1): Switch to next/previous activity
    // Expanded:
    //   - Drag up: Collapse
    //   - Drag left: Remove from island AND stop that task/activity
    val collapsedGestureModifier = Modifier.pointerInput(mode, taskCount) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val startX = down.position.x
            val startY = down.position.y
            var isActionTaken = false

            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                val currentX = change.position.x
                val currentY = change.position.y
                val diffX = currentX - startX
                val diffY = currentY - startY

                // Long press detection: after holding without moving
                if (!isActionTaken && change.uptimeMillis - down.uptimeMillis > 450L && abs(diffX) < 15f && abs(diffY) < 15f) {
                    isActionTaken = true
                    change.consume()
                    if (config.enableHaptics) {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    when (config.longPressAction) {
                        0 -> stateManager.onIslandTapped()
                        1 -> stateManager.expandIsland(autoCollapseSeconds = if (config.expandedTimeoutSeconds > 0) config.expandedTimeoutSeconds else null)
                        2 -> primaryTask?.let { stateManager.removeTask(it.id) }
                    }
                    break
                }

                // Downward drag to expand
                if (!isActionTaken && diffY > 20f && diffY > abs(diffX)) {
                    isActionTaken = true
                    change.consume()
                    if (config.enableHaptics) {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    stateManager.expandIsland(autoCollapseSeconds = if (config.expandedTimeoutSeconds > 0) config.expandedTimeoutSeconds else null)
                    break
                }

                // Horizontal drag to switch activities (when multiple tasks exist)
                if (!isActionTaken && taskCount > 1 && abs(diffX) > 30f && abs(diffX) > abs(diffY) * 1.2f) {
                    isActionTaken = true
                    change.consume()
                    if (config.enableHaptics) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    if (diffX < 0) {
                        stateManager.cycleStackedTasks(forward = true)
                    } else {
                        stateManager.cycleStackedTasks(forward = false)
                    }
                    break
                }

                // Horizontal swipe to dismiss active task when single task exists
                if (!isActionTaken && taskCount == 1 && config.enableSwipeDismiss && abs(diffX) > 35f && abs(diffX) > abs(diffY) * 1.2f) {
                    isActionTaken = true
                    change.consume()
                    if (config.enableHaptics) {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    primaryTask?.let { stateManager.removeTask(it.id) }
                    break
                }

                // Finger lifted (UP) or gesture completed:
                if (!change.pressed) {
                    if (!isActionTaken) {
                        change.consume()
                        if (config.enableHaptics) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                        when (config.singleTapAction) {
                            0 -> stateManager.onIslandTapped()
                            1 -> stateManager.expandIsland(autoCollapseSeconds = if (config.expandedTimeoutSeconds > 0) config.expandedTimeoutSeconds else null)
                            else -> {}
                        }
                    }
                    break
                }
            }
        }
    }

    val expandedGestureModifier = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val startX = down.position.x
            val startY = down.position.y
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                val currentX = change.position.x
                val currentY = change.position.y
                val diffX = currentX - startX
                val diffY = currentY - startY

                // Upward swipe to collapse
                if (diffY < -25f && abs(diffY) > abs(diffX)) {
                    change.consume()
                    if (config.enableHaptics) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    stateManager.collapseIsland()
                    break
                }

                // Left swipe on expanded card: Remove from island AND stop that task/activity!
                if (diffX < -35f && abs(diffX) > abs(diffY) * 1.4f) {
                    change.consume()
                    if (config.enableHaptics) {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    stateManager.stopAndDismissActiveTask()
                    break
                }

                if (!change.pressed) break
            }
        }
    }

    val outsideTapModifier = if (isExpanded && config.touchOutsideCollapse) {
        Modifier.pointerInput(Unit) {
            detectTapGestures {
                if (config.enableHaptics) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                stateManager.collapseIsland()
            }
        }
    } else Modifier

    val innerPillConsumeModifier = if (isExpanded) {
        Modifier.pointerInput(Unit) {
            detectTapGestures {
                // Consumed so outside dismiss does not trigger when clicking inside card
            }
        }
    } else Modifier

    // Anchored at TopCenter so expansion animates DOWNWARD and OUTWARD from the punch-hole,
    // NEVER jumping into the middle of the screen!
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (!isExpanded) collapsedGestureModifier else outsideTapModifier),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            modifier = Modifier
                .wrapContentSize()
                .padding(top = if (!isExpanded) 4.dp else 0.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center
        ) {
            // Main Pill Container
            Box(
                modifier = Modifier
                    .width(animatedWidth)
                    .height(animatedHeight)
                    .then(innerPillConsumeModifier)
                    .graphicsLayer {
                        // Smooth origin anchor at top center
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
                    .shadow(
                        elevation = if (mode == IslandMode.EXPANDED) 24.dp else 6.dp,
                        shape = RoundedCornerShape(animatedRadius),
                        ambientColor = Color(0x66000000),
                        spotColor = Color(0x88000000),
                        clip = false
                    )
                    .clip(RoundedCornerShape(animatedRadius))
                    .background(Color.Black)
                    .then(
                        if (isCalibrating) {
                            Modifier.border(
                                width = 1.5.dp,
                                color = Color(0xFF007AFF),
                                shape = RoundedCornerShape(animatedRadius)
                            )
                        } else if (isGlowRingActive) {
                            // dynamicSpot Camera Glow Ring / Notification Light
                            val glowBase = when (val t = primaryTask) {
                                is ChargingAlert -> {
                                    when {
                                        t.isLowBattery -> Color(config.glowLowBatteryColor)
                                        t.isFullyCharged -> Color(config.glowFullBatteryColor)
                                        else -> Color(config.glowChargingColor)
                                    }
                                }
                                is NotificationTask -> {
                                    if (config.colorizeFromArtwork && t.accentColor != null) {
                                        Color(t.accentColor)
                                    } else {
                                        Color(config.glowNotificationColor)
                                    }
                                }
                                is MediaTask -> {
                                    if (config.colorizeFromArtwork && t.accentColor != null) {
                                        Color(t.accentColor)
                                    } else {
                                        Color(config.glowNotificationColor)
                                    }
                                }
                                else -> Color(config.glowNotificationColor)
                            }
                            Modifier.border(
                                width = 1.8.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        glowBase.copy(alpha = pulseAlpha),
                                        glowBase.copy(alpha = pulseAlpha * 0.25f)
                                    )
                                ),
                                shape = RoundedCornerShape(animatedRadius)
                            )
                        } else if (config.colorizeFromArtwork && mediaAccentColor != null) {
                            // dynamicSpot Artwork Accent Tinting
                            Modifier.border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        mediaAccentColor.copy(alpha = 0.70f),
                                        mediaAccentColor.copy(alpha = 0.15f)
                                    )
                                ),
                                shape = RoundedCornerShape(animatedRadius)
                            )
                        } else if (config.theme == IslandTheme.NOTHING) {
                            Modifier.border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        if (primaryTask is ChargingAlert) nothingRed else Color.White.copy(alpha = 0.35f),
                                        if (primaryTask is ChargingAlert) nothingRed.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.10f)
                                    )
                                ),
                                shape = RoundedCornerShape(animatedRadius)
                            )
                        } else if (config.enableLiquidGlow && primaryTask is ChargingAlert) {
                            Modifier.border(
                                width = 1.5.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF30D158),
                                        Color(0x3330D158)
                                    )
                                ),
                                shape = RoundedCornerShape(animatedRadius)
                            )
                        } else {
                            // Genuine Apple Specular Dual-Gradient Glass Rim
                            Modifier.border(
                                width = 0.75.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.32f), // Top specular reflection
                                        Color.White.copy(alpha = 0.08f)  // Bottom diffuse fade
                                    )
                                ),
                                shape = RoundedCornerShape(animatedRadius)
                            )
                        }
                    )
                    .then(if (isExpanded) expandedGestureModifier else Modifier),
                contentAlignment = Alignment.TopCenter
            ) {
                if (mode == IslandMode.EXPANDED) {
                    ExpandedCardView(
                        task = primaryTask,
                        stateManager = stateManager,
                        theme = config.theme,
                        onCollapse = { stateManager.collapseIsland() }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CompactPillView(
                            task = primaryTask,
                            cutoutGapDp = config.cutoutGap,
                            theme = config.theme
                        )

                        // Tripple Stack Pagination Dots (Honor Magic Capsule)
                        if (mode == IslandMode.COLLAPSED_TRIPPLE) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 3.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                TrippleStackIndicator(taskCount = taskCount)
                            }
                        }
                    }
                }
            }

            // Secondary Detached Circle Bubble (Apple iOS Dual-Island Split)
            if (mode == IslandMode.COLLAPSED_SPLIT && secondaryTask != null && config.enableAutoSplit) {
                Spacer(modifier = Modifier.width(config.splitSpacing.dp))
                SplitBubbleView(
                    task = secondaryTask,
                    sizeDp = config.secondarySize,
                    onClick = {
                        if (config.enableHaptics) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                        // Swap secondary into primary focus
                        stateManager.cycleStackedTasks(forward = true)
                    }
                )
            }
        }
    }
}
