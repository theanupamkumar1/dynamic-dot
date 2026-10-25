package com.dynamicisland.app.ui.settings

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicisland.app.manager.IslandStateManager
import com.dynamicisland.app.manager.SettingsRepository
import com.dynamicisland.app.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepo: SettingsRepository,
    stateManager: IslandStateManager,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    val context = LocalContext.current
    val config by settingsRepo.configFlow.collectAsState()
    val isServiceActive by settingsRepo.isServiceActiveFlow.collectAsState()
    val savedPresets by settingsRepo.savedPresetsFlow.collectAsState()

    var showSavePresetDialog by remember { mutableStateOf(false) }
    var showAppSelectDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasNotificationAccess by remember {
        mutableStateOf(
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                ?.contains(context.packageName) == true
        )
    }
    var hasAccessibilityPermission by remember {
        mutableStateOf(
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?.contains(context.packageName) == true
        )
    }

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    var isBatteryOptIgnored by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
            } else true
        )
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    fun refreshPermissions() {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        hasNotificationAccess = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            ?.contains(context.packageName) == true
        hasAccessibilityPermission = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?.contains(context.packageName) == true
        isBatteryOptIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stateManager.removeTask("calibration_task")
            stateManager.removeTask("secondary_calibration_task")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (config.theme == IslandTheme.NOTHING) {
                                        Brush.linearGradient(listOf(Color(0xFFD71921), Color(0xFF1E1E1E)))
                                    } else {
                                        Brush.linearGradient(listOf(Color(0xFF007AFF), Color(0xFF5856D6)))
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Widgets,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Dynamic Island",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White,
                            fontFamily = if (config.theme == IslandTheme.NOTHING) FontFamily.Monospace else FontFamily.Default
                        )
                    }
                },
                actions = {
                    Switch(
                        checked = isServiceActive,
                        onCheckedChange = { active ->
                            settingsRepo.setServiceActive(active)
                            if (active) onStartService() else onStopService()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = if (config.theme == IslandTheme.NOTHING) Color(0xFFD71921) else Color(0xFF30D158),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color(0xFF2C2C2E)
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B0E14))
            )
        },
        containerColor = Color(0xFF0B0E14)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // AUTOMATED FEATURE TEST & SHOWCASE CARD
            val isDemoRunning by stateManager.isDemoRunning.collectAsState()
            val demoCurrentStep by stateManager.demoCurrentStep.collectAsState()

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDemoRunning) Color(0xFF102619) else Color(0xFF141923)
                ),
                border = BorderStroke(
                    1.dp,
                    if (isDemoRunning) Color(0xFF30D158).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.12f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isDemoRunning) Color(0xFF30D158).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDemoRunning) Icons.Default.PlayArrow else Icons.Default.Science,
                                contentDescription = "Test Engine",
                                tint = if (isDemoRunning) Color(0xFF30D158) else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AUTOMATED FEATURE DEMO",
                                color = if (isDemoRunning) Color(0xFF30D158) else Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (isDemoRunning) demoCurrentStep.ifEmpty { "Testing active..." } else "1-Click test for all 16 dynamic island states",
                                color = if (isDemoRunning) Color(0xFF30D158).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (isDemoRunning) {
                                stateManager.stopAutomatedDemo()
                            } else {
                                stateManager.startAutomatedDemo()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDemoRunning) Color(0xFFFF453A) else Color(0xFF30D158)
                        )
                    ) {
                        Icon(
                            imageVector = if (isDemoRunning) Icons.Default.Stop else Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = if (isDemoRunning) Color.White else Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDemoRunning) "Stop Test Sequence" else "Run All 16 Feature Tests Live",
                            color = if (isDemoRunning) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Accessibility Service Warning Card (Crucial for rendering ABOVE status bar)
            if (!hasAccessibilityPermission) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1C0A)),
                    border = BorderStroke(1.dp, Color(0xFFFF9500).copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PriorityHigh,
                            contentDescription = null,
                            tint = Color(0xFFFF9500),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Status Bar Priority: Disabled",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Without Accessibility, Dynamic Island renders UNDER the status bar. Enable it to draw ABOVE status bar.",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.5.sp
                            )
                        }
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Enable", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 2. Notification Listener Permission Card
            if (!hasNotificationAccess) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                    border = BorderStroke(1.dp, Color(0xFF007AFF).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Live Activities: Disabled",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Enable Notification Access to show live music, calls, and active phone activities.",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.5.sp
                            )
                        }
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Enable", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Battery Optimization Warning Card (Prevents Nothing OS from killing background service)
            if (!isBatteryOptIgnored) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF281315)),
                    border = BorderStroke(1.dp, Color(0xFFFF453A).copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF453A),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Battery Optimization Active",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Nothing OS will close Dynamic Island. Tap Fix to allow unrestricted background run.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.5.sp
                            )
                        }
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    try {
                                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Fix", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Theme Selection Section
            SectionHeader(title = "THEME & DESIGN LANGUAGE")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Choose your visual experience:",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Regular (Apple iOS Fluid Glass)
                        ThemeSelectionCard(
                            title = "Regular",
                            subtitle = "iOS Fluid Glass",
                            description = "Pure black squircle, translucent glass border, and Apple system accents",
                            isSelected = config.theme == IslandTheme.REGULAR,
                            accentColor = Color(0xFF007AFF),
                            isNothing = false,
                            onClick = { settingsRepo.setTheme(IslandTheme.REGULAR) },
                            modifier = Modifier.weight(1f)
                        )

                        // Nothing OS (Monochrome & Nothing Red)
                        ThemeSelectionCard(
                            title = "Nothing OS",
                            subtitle = "Glyph & Dot-Matrix",
                            description = "Monochrome, NDot typography, and iconic Nothing Red accents (#D71921)",
                            isSelected = config.theme == IslandTheme.NOTHING,
                            accentColor = Color(0xFFD71921),
                            isNothing = true,
                            onClick = { settingsRepo.setTheme(IslandTheme.NOTHING) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Saved Manual Position Presets Section
            SectionHeader(title = "SAVED POSITION PRESETS")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "1-Click Custom Presets",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Save your notch alignment once, apply anytime in 1 tap",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = {
                                newPresetName = "Setup ${savedPresets.size + 1}"
                                showSavePresetDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (config.theme == IslandTheme.NOTHING) Color(0xFFD71921) else Color(0xFF007AFF)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Current", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (savedPresets.isEmpty()) {
                        Text(
                            text = "No saved presets yet. Calibrate below and tap 'Save Current'!",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(savedPresets) { preset ->
                                val isCurrentActive = config.activePresetName == preset.name
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrentActive) {
                                            if (config.theme == IslandTheme.NOTHING) Color(0xFFD71921).copy(alpha = 0.2f) else Color(0xFF007AFF).copy(alpha = 0.2f)
                                        } else Color(0xFF21262D)
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCurrentActive) {
                                            if (config.theme == IslandTheme.NOTHING) Color(0xFFD71921) else Color(0xFF007AFF)
                                        } else Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier
                                        .width(170.dp)
                                        .clickable {
                                            settingsRepo.applyCustomPreset(preset)
                                            stateManager.postTask(CalibrationTask())
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = preset.name,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            IconButton(
                                                onClick = { settingsRepo.deleteCustomPreset(preset.id) },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Delete",
                                                    tint = Color.White.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "X: ${preset.offsetX.toInt()} | Y: ${preset.offsetY.toInt()} | W: ${preset.pillWidth.toInt()}",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (isCurrentActive) "● Active" else "Tap to Apply",
                                            color = if (isCurrentActive) {
                                                if (config.theme == IslandTheme.NOTHING) Color(0xFFD71921) else Color(0xFF30D158)
                                            } else Color.White.copy(alpha = 0.5f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cutout Calibration Studio Section
            SectionHeader(title = "CUTOUT CALIBRATION STUDIO")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Camera Cutout Presets",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetChip(
                            label = "Center Hole",
                            selected = config.position == CutoutPosition.CENTER_PUNCH_HOLE,
                            onClick = {
                                settingsRepo.applyPreset(CutoutPosition.CENTER_PUNCH_HOLE)
                                stateManager.removeTask("secondary_calibration_task")
                                stateManager.postTask(CalibrationTask())
                            },
                            modifier = Modifier.weight(1f)
                        )
                        PresetChip(
                            label = "Left Hole",
                            selected = config.position == CutoutPosition.LEFT_PUNCH_HOLE,
                            onClick = {
                                settingsRepo.applyPreset(CutoutPosition.LEFT_PUNCH_HOLE)
                                stateManager.removeTask("secondary_calibration_task")
                                stateManager.postTask(CalibrationTask())
                            },
                            modifier = Modifier.weight(1f)
                        )
                        PresetChip(
                            label = "Right Hole",
                            selected = config.position == CutoutPosition.RIGHT_PUNCH_HOLE,
                            onClick = {
                                settingsRepo.applyPreset(CutoutPosition.RIGHT_PUNCH_HOLE)
                                stateManager.removeTask("secondary_calibration_task")
                                stateManager.postTask(CalibrationTask())
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))

                    CalibrationSlider(
                        label = "Horizontal Shift (X Offset)",
                        value = config.offsetX,
                        valueRange = -180f..180f,
                        unit = "dp",
                        onValueChange = {
                            settingsRepo.updateConfig(config.copy(offsetX = it, activePresetName = null))
                            stateManager.removeTask("secondary_calibration_task")
                            stateManager.postTask(CalibrationTask())
                        }
                    )

                    CalibrationSlider(
                        label = "Vertical Margin (Y Offset)",
                        value = config.offsetY,
                        valueRange = 0f..60f,
                        unit = "dp",
                        onValueChange = {
                            settingsRepo.updateConfig(config.copy(offsetY = it, activePresetName = null))
                            stateManager.removeTask("secondary_calibration_task")
                            stateManager.postTask(CalibrationTask())
                        }
                    )

                    CalibrationSlider(
                        label = "Pill Width",
                        value = config.pillWidth,
                        valueRange = 90f..220f,
                        unit = "dp",
                        onValueChange = {
                            settingsRepo.updateConfig(config.copy(pillWidth = it, activePresetName = null))
                            stateManager.removeTask("secondary_calibration_task")
                            stateManager.postTask(CalibrationTask())
                        }
                    )

                    CalibrationSlider(
                        label = "Pill Height",
                        value = config.pillHeight,
                        valueRange = 28f..50f,
                        unit = "dp",
                        onValueChange = {
                            settingsRepo.updateConfig(config.copy(pillHeight = it, activePresetName = null))
                            stateManager.removeTask("secondary_calibration_task")
                            stateManager.postTask(CalibrationTask())
                        }
                    )

                    CalibrationSlider(
                        label = "Corner Radius",
                        value = config.pillRadius,
                        valueRange = 10f..25f,
                        unit = "dp",
                        onValueChange = {
                            settingsRepo.updateConfig(config.copy(pillRadius = it, activePresetName = null))
                            stateManager.removeTask("secondary_calibration_task")
                            stateManager.postTask(CalibrationTask())
                        }
                    )

                    CalibrationSlider(
                        label = "Camera Cutout Gap",
                        value = config.cutoutGap,
                        valueRange = 16f..60f,
                        unit = "dp",
                        onValueChange = {
                            settingsRepo.updateConfig(config.copy(cutoutGap = it, activePresetName = null))
                            stateManager.removeTask("secondary_calibration_task")
                            stateManager.postTask(CalibrationTask())
                        }
                    )

                    CalibrationSlider(
                        label = "Dual Split Spacing",
                        value = config.splitSpacing,
                        valueRange = 4f..28f,
                        unit = "dp",
                        onValueChange = {
                            settingsRepo.updateConfig(config.copy(splitSpacing = it, activePresetName = null))
                            stateManager.postTask(CalibrationTask(showSplit = true))
                            stateManager.postTask(SecondaryCalibrationTask())
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val primaryTask by stateManager.primaryTask.collectAsState()
                        val isCalibrating = primaryTask is CalibrationTask

                        Button(
                            onClick = {
                                onStartService()
                                if (isCalibrating) {
                                    stateManager.removeTask("calibration_task")
                                    stateManager.removeTask("secondary_calibration_task")
                                } else {
                                    stateManager.postTask(CalibrationTask())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCalibrating) {
                                    if (config.theme == IslandTheme.NOTHING) Color(0xFFD71921) else Color(0xFF30D158)
                                } else Color(0xFF007AFF)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FilterCenterFocus, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isCalibrating) "Hide Alignment" else "Show Alignment Notch", fontSize = 12.sp)
                        }

                        TextButton(onClick = {
                            settingsRepo.resetToDefaults()
                            stateManager.postTask(CalibrationTask())
                        }) {
                            Text("Reset Defaults", color = Color(0xFFFF453A))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. NOTIFICATIONS & APPS (dynamicSpot)
            SectionHeader(title = "NOTIFICATIONS & APPS (DYNAMICSPOT)")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // App Selection Filter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF21262D))
                            .clickable { showAppSelectDialog = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Apps, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "App Notification Filter", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (config.allowedPackages.isEmpty()) "All apps enabled (Click to customize)" else "${config.allowedPackages.size} apps selected",
                                color = Color(0xFF007AFF),
                                fontSize = 12.sp
                            )
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    Text(text = "BATTERY NOTIFICATION EVENTS", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    FeatureToggleRow(
                        title = "Phone is Charging",
                        subtitle = "Show island alert whenever device is plugged into power",
                        checked = config.batteryShowCharging,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(batteryShowCharging = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    FeatureToggleRow(
                        title = "Fast Charging Wattage",
                        subtitle = "Display SuperVOOC / USB-PD fast charging wattage indicator",
                        checked = config.batteryShowWattage,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(batteryShowWattage = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    FeatureToggleRow(
                        title = "Low Battery Warning (< 15%)",
                        subtitle = "Alert with red halo ring when battery drops below 15%",
                        checked = config.batteryShowLow,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(batteryShowLow = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    FeatureToggleRow(
                        title = "Battery Fully Charged (100%)",
                        subtitle = "Alert with blue halo ring when battery reaches 100% full",
                        checked = config.batteryShowFull,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(batteryShowFull = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    Text(text = "FILTERING & PRIVACY", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    FeatureToggleRow(
                        title = "Do Not Disturb Filter",
                        subtitle = "Don't show notification popups while system Do Not Disturb (DND) is active",
                        checked = config.ignoreDnd,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(ignoreDnd = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    FeatureToggleRow(
                        title = "Ignore Ongoing Notifications",
                        subtitle = "Suppress non-dismissible background services and downloads from taking over the island",
                        checked = config.ignoreOngoing,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(ignoreOngoing = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    FeatureToggleRow(
                        title = "Show on Lockscreen",
                        subtitle = "Display dynamic island popups on the lockscreen & AOD",
                        checked = config.showOnLockscreen,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(showOnLockscreen = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    FeatureToggleRow(
                        title = "Hide Sensitive Content on Lockscreen",
                        subtitle = "Mask notification messages until device is unlocked",
                        checked = config.hideSensitiveOnLockscreen,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(hideSensitiveOnLockscreen = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    FeatureToggleRow(
                        title = "Block Native Heads-Up Notifications",
                        subtitle = "Prevent Android's floating banners from appearing twice alongside the island",
                        checked = config.blockNativeHeadsUp,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(blockNativeHeadsUp = it)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. BEHAVIOR & GESTURES (dynamicSpot)
            SectionHeader(title = "BEHAVIOR & GESTURES (DYNAMICSPOT)")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    FeatureToggleRow(
                        title = "Auto-Expand Incoming Notifications",
                        subtitle = "Automatically open full preview card when a new message arrives",
                        checked = config.autoExpandNotifications,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(autoExpandNotifications = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    FeatureToggleRow(
                        title = "Inline Quick Reply Box",
                        subtitle = "Type replies to WhatsApp, Telegram, and SMS directly in the expanded card",
                        checked = config.enableInlineReply,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(enableInlineReply = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    FeatureToggleRow(
                        title = "Hide in Foreground App",
                        subtitle = "Auto-hide the island when the active app (YouTube Music, Spotify, Phone) is open",
                        checked = config.hideInForeground,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(hideInForeground = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    FeatureToggleRow(
                        title = "Dual-Island Split (Apple Style)",
                        subtitle = "Detached secondary bubble when 2 activities run at once",
                        checked = config.enableAutoSplit,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(enableAutoSplit = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    FeatureToggleRow(
                        title = "Honor Magic Capsule Tripple-Stack",
                        subtitle = "Stack up to 3 background tasks with horizontal swipe rotation",
                        checked = config.enableTrippleStack,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(enableTrippleStack = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    FeatureToggleRow(
                        title = "Touch Outside to Collapse",
                        subtitle = "Tap anywhere outside the expanded card to collapse back into the pill",
                        checked = config.touchOutsideCollapse,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(touchOutsideCollapse = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    IntegerSliderRow(
                        label = "Compact Pill Auto-Dismiss",
                        subtitle = "How long small notification pills stay visible before auto-hiding",
                        value = config.pillTimeoutSeconds,
                        range = 0f..20f,
                        unit = "sec",
                        onValueChange = { settingsRepo.updateConfig(config.copy(pillTimeoutSeconds = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    IntegerSliderRow(
                        label = "Expanded Card Auto-Collapse",
                        subtitle = "How long the expanded card stays open before collapsing back to pill",
                        value = config.expandedTimeoutSeconds,
                        range = 0f..15f,
                        unit = "sec",
                        onValueChange = { settingsRepo.updateConfig(config.copy(expandedTimeoutSeconds = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    ActionChipRow(
                        title = "Single Tap Interaction",
                        subtitle = "What happens when you tap the compact pill",
                        selectedIndex = config.singleTapAction,
                        options = listOf("Open App", "Expand Island", "None"),
                        onSelect = { settingsRepo.updateConfig(config.copy(singleTapAction = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    ActionChipRow(
                        title = "Long Press Interaction",
                        subtitle = "What happens when you hold the compact pill",
                        selectedIndex = config.longPressAction,
                        options = listOf("Open App", "Expand Island", "Dismiss"),
                        onSelect = { settingsRepo.updateConfig(config.copy(longPressAction = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    FeatureToggleRow(
                        title = "Swipe to Dismiss",
                        subtitle = "Swipe horizontally to clear active island alerts",
                        checked = config.enableSwipeDismiss,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(enableSwipeDismiss = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    FeatureToggleRow(
                        title = "Haptic Vibration Feedback",
                        subtitle = "Vibrate on island touch, long-press, and swipe gestures",
                        checked = config.enableHaptics,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(enableHaptics = it)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. APPEARANCE & VISUALS (dynamicSpot)
            SectionHeader(title = "APPEARANCE & VISUALS (DYNAMICSPOT)")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    FeatureToggleRow(
                        title = "Camera Glow Ring & Halo Light",
                        subtitle = "Pulsing ambient halo ring around camera cutout for alerts, notifications, and charging",
                        checked = config.enableGlowRing,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(enableGlowRing = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    FeatureToggleRow(
                        title = "Dynamic Artwork Color Tinting",
                        subtitle = "Colorize visualizer and glass rim with vibrant colors extracted from album artwork",
                        checked = config.colorizeFromArtwork,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(colorizeFromArtwork = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    FeatureToggleRow(
                        title = "Fast Low-Latency Animations",
                        subtitle = "High stiffness snappy transitions without rubber-band bounce",
                        checked = config.fastAnimations,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(fastAnimations = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    IntegerSliderRow(
                        label = "Max Notification Text Lines",
                        subtitle = "Maximum lines of message text shown in the expanded card",
                        value = config.maxNotificationLines,
                        range = 1f..10f,
                        unit = "lines",
                        onValueChange = { settingsRepo.updateConfig(config.copy(maxNotificationLines = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    FeatureToggleRow(
                        title = "Show Picture Attachments",
                        subtitle = "Display inline images and photos inside expanded notification cards",
                        checked = config.showNotificationImages,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(showNotificationImages = it)) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

                    FeatureToggleRow(
                        title = "Always-On Ambient Notch Cover",
                        subtitle = "Keep camera punch hole covered in sleek pure black even when idle",
                        checked = config.alwaysShowAmbientPill,
                        onCheckedChange = { settingsRepo.updateConfig(config.copy(alwaysShowAmbientPill = it)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Interactive Live Feature Simulator
            SectionHeader(title = "LIVE SIMULATOR (TEST ON YOUR SCREEN)")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Trigger any feature to test morphing & gestures right now:",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val triggerTest: (IslandTask) -> Unit = { task ->
                        if (!Settings.canDrawOverlays(context)) {
                            android.widget.Toast.makeText(
                                context,
                                "Please allow 'Display Over Other Apps' permission above first!",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        } else {
                            onStartService()
                            stateManager.postTask(task)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SimButton(
                            label = "Charging",
                            icon = Icons.Default.Bolt,
                            color = if (config.theme == IslandTheme.NOTHING) Color(0xFFD71921) else Color(0xFF30D158),
                            onClick = {
                                triggerTest(
                                    ChargingAlert(
                                        batteryPercent = 78,
                                        isCharging = true,
                                        isFastCharging = true,
                                        wattage = "67W Fast Charging"
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        SimButton(
                            label = "Music",
                            icon = Icons.Default.MusicNote,
                            color = if (config.theme == IslandTheme.NOTHING) Color.White else Color(0xFFE0245E),
                            onClick = {
                                triggerTest(
                                    MediaTask(
                                        title = "Starboy (feat. Daft Punk)",
                                        artist = "The Weeknd",
                                        isPlaying = true,
                                        playbackPositionMs = 45000,
                                        trackDurationMs = 230000,
                                        accentColor = 0xFFFF2D55L
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SimButton(
                            label = "Incoming Call",
                            icon = Icons.Default.Call,
                            color = if (config.theme == IslandTheme.NOTHING) Color(0xFFD71921) else Color(0xFFFF9F0A),
                            onClick = {
                                triggerTest(
                                    CallTask(
                                        callerName = "Sarah Jenkins",
                                        isIncoming = true,
                                        isVoip = true,
                                        appName = "WhatsApp"
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        SimButton(
                            label = "Navigation",
                            icon = Icons.Default.Navigation,
                            color = Color(0xFF0A84FF),
                            onClick = {
                                triggerTest(
                                    NavigationTask(
                                        direction = NavDirection.TURN_RIGHT,
                                        distanceText = "150 m",
                                        instruction = "Turn right onto Silicon Valley Blvd",
                                        etaText = "8 min"
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SimButton(
                            label = "Dual Split",
                            icon = Icons.Default.ViewAgenda,
                            color = Color(0xFFBF5AF2),
                            onClick = {
                                triggerTest(
                                    MediaTask(
                                        title = "Blinding Lights",
                                        artist = "The Weeknd",
                                        isPlaying = true
                                    )
                                )
                                triggerTest(
                                    TimerTask(
                                        label = "Oven",
                                        remainingSeconds = 125,
                                        totalSeconds = 300
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        SimButton(
                            label = "Multitask (Swipe Left)",
                            icon = Icons.Default.Layers,
                            color = Color(0xFF64D2FF),
                            onClick = {
                                triggerTest(
                                    MediaTask(title = "Spotify Live", artist = "The Weeknd", isPlaying = true)
                                )
                                triggerTest(
                                    TimerTask(label = "Tea Timer", remainingSeconds = 180)
                                )
                                triggerTest(
                                    NavigationTask(
                                        direction = NavDirection.TURN_LEFT,
                                        distanceText = "300 m",
                                        instruction = "Keep left towards Highway 101"
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            stateManager.clearAllTasks()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
                    ) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All Active Island Tasks")
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Save Preset Dialog
    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = {
                Text("Save Current Preset", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Give this calibration a recognizable name (e.g. 'My Phone 2 Hole'):",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = if (config.theme == IslandTheme.NOTHING) Color(0xFFD71921) else Color(0xFF007AFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        settingsRepo.saveCurrentAsCustomPreset(newPresetName)
                        showSavePresetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (config.theme == IslandTheme.NOTHING) Color(0xFFD71921) else Color(0xFF007AFF)
                    )
                ) {
                    Text("Save Preset", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF161B22)
        )
    }

    if (showAppSelectDialog) {
        AppSelectionDialog(
            context = context,
            selectedPackages = config.allowedPackages,
            onDismiss = { showAppSelectDialog = false },
            onSave = { updatedPackages ->
                settingsRepo.setAllowedPackages(updatedPackages)
            }
        )
    }
}

@Composable
private fun ThemeSelectionCard(
    title: String,
    subtitle: String,
    description: String,
    isSelected: Boolean,
    accentColor: Color,
    isNothing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentColor.copy(alpha = 0.15f) else Color(0xFF21262D)
        ),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) accentColor else Color.White.copy(alpha = 0.1f)
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (isNothing) FontFamily.Monospace else FontFamily.Default
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (isNothing) FontFamily.Monospace else FontFamily.Default
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF007AFF),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFF007AFF) else Color(0xFF21262D))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun SimButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CalibrationSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            Text(
                text = "${value.toInt()} $unit",
                color = Color(0xFF007AFF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF007AFF),
                activeTrackColor = Color(0xFF007AFF),
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
private fun FeatureToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF30D158),
                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                uncheckedTrackColor = Color(0xFF2C2C2E)
            )
        )
    }
}

@Composable
private fun IntegerSliderRow(
    label: String,
    subtitle: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 11.5.sp)
            }
            Text(
                text = if (value == 0) "Never / Off" else "$value $unit",
                color = Color(0xFF007AFF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF007AFF),
                inactiveTrackColor = Color(0xFF30363D)
            )
        )
    }
}

@Composable
private fun ActionChipRow(
    title: String,
    subtitle: String,
    selectedIndex: Int,
    options: List<String>,
    onSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = title, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
        Text(text = subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 11.5.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFF007AFF) else Color(0xFF21262D))
                        .clickable { onSelect(index) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
    if (drawable is android.graphics.drawable.BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 48
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 48
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

private data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Bitmap?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppSelectionDialog(
    context: Context,
    selectedPackages: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val currentSelected = remember {
        mutableStateMapOf<String, Boolean>().apply {
            selectedPackages.forEach { put(it, true) }
        }
    }

    var installedApps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val list = pm.queryIntentActivities(intent, 0)
                .mapNotNull { resolveInfo ->
                    val pkg = resolveInfo.activityInfo.packageName
                    if (pkg == context.packageName) return@mapNotNull null
                    val label = resolveInfo.loadLabel(pm).toString()
                    val drawable = resolveInfo.loadIcon(pm)
                    val bmp = drawableToBitmap(drawable)
                    AppEntry(packageName = pkg, label = label, icon = bmp)
                }
                .sortedBy { it.label.lowercase() }
            installedApps = list
            isLoading = false
        }
    }

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Select Apps for Island", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (currentSelected.isEmpty()) "All apps currently enabled" else "${currentSelected.size} apps enabled",
                    color = Color(0xFF007AFF),
                    fontSize = 12.5.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(380.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed apps...", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF007AFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        installedApps.forEach { currentSelected[it.packageName] = true }
                    }) {
                        Text("Select All", color = Color(0xFF007AFF), fontSize = 12.sp)
                    }
                    TextButton(onClick = {
                        currentSelected.clear()
                    }) {
                        Text("Clear All (Allow All)", color = Color(0xFFFF453A), fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF007AFF))
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredApps) { app ->
                            val isChecked = currentSelected[app.packageName] == true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) {
                                            currentSelected.remove(app.packageName)
                                        } else {
                                            currentSelected[app.packageName] = true
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (app.icon != null) {
                                    Image(
                                        bitmap = app.icon.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(modifier = Modifier.size(36.dp).background(Color(0xFF21262D), RoundedCornerShape(8.dp)))
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = app.label, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(text = app.packageName, color = Color.White.copy(alpha = 0.45f), fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }

                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) currentSelected[app.packageName] = true
                                        else currentSelected.remove(app.packageName)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF007AFF),
                                        checkmarkColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(currentSelected.keys.toSet())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
            ) {
                Text("Done", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = Color(0xFF161B22)
    )
}
