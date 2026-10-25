package com.dynamicisland.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Display
import android.view.Gravity
import android.view.Surface
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.dynamicisland.app.MainActivity
import com.dynamicisland.app.R
import com.dynamicisland.app.manager.IslandStateManager
import com.dynamicisland.app.manager.SettingsRepository
import com.dynamicisland.app.model.CutoutConfig
import com.dynamicisland.app.model.IslandMode
import com.dynamicisland.app.receiver.HardwareEventReceiver
import com.dynamicisland.app.ui.island.DynamicIslandOverlay
import com.dynamicisland.app.util.OrientationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class IslandOverlayService : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val appViewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore
        get() = appViewModelStore

    private lateinit var windowManager: WindowManager
    private lateinit var settingsRepo: SettingsRepository
    private val stateManager = IslandStateManager.instance
    private var composeView: ComposeView? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    private var hardwareReceiver: HardwareEventReceiver? = null
    private val testReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val type = intent?.getStringExtra("type") ?: return
            Log.d("IslandOverlayService", "testReceiver received type: $type")
            when (type) {
                "charging" -> {
                    stateManager.clearAllTasks()
                    stateManager.postTask(com.dynamicisland.app.model.ChargingAlert(batteryPercent = 85, isCharging = true, isFastCharging = true, wattage = "67W Fast Charge", autoDismissSeconds = null))
                }
                "ringer_silent" -> {
                    stateManager.clearAllTasks()
                    stateManager.postTask(com.dynamicisland.app.model.RingerAlert(state = com.dynamicisland.app.model.RingerState.SILENT, autoDismissSeconds = null))
                }
                "ringer_ring" -> {
                    stateManager.clearAllTasks()
                    stateManager.postTask(com.dynamicisland.app.model.RingerAlert(state = com.dynamicisland.app.model.RingerState.RING, autoDismissSeconds = null))
                }
                "ringer_vibrate" -> {
                    stateManager.clearAllTasks()
                    stateManager.postTask(com.dynamicisland.app.model.RingerAlert(state = com.dynamicisland.app.model.RingerState.VIBRATE, autoDismissSeconds = null))
                }
                "bluetooth" -> {
                    stateManager.clearAllTasks()
                    stateManager.postTask(com.dynamicisland.app.model.BluetoothAlert(deviceName = "Nothing Ear (2)", batteryPercent = 90, isConnected = true, autoDismissSeconds = null))
                }
                "music" -> {
                    stateManager.clearAllTasks()
                    stateManager.postTask(com.dynamicisland.app.model.MediaTask(title = "Starboy (feat. Daft Punk)", artist = "The Weeknd", isPlaying = true, playbackPositionMs = 65000, trackDurationMs = 230000, packageName = "app.morphe.android.apps.youtube.music", autoDismissSeconds = null))
                }
                "music_paused" -> {
                    stateManager.clearAllTasks()
                    stateManager.postTask(com.dynamicisland.app.model.MediaTask(title = "Starboy (feat. Daft Punk)", artist = "The Weeknd", isPlaying = false, playbackPositionMs = 65000, trackDurationMs = 230000, packageName = "app.morphe.android.apps.youtube.music", autoDismissSeconds = null))
                }
                "call_incoming" -> {
                    stateManager.clearAllTasks()
                    stateManager.postTask(com.dynamicisland.app.model.CallTask(callerName = "Sarah Jenkins", callerNumber = "+1 (555) 019-2834", isIncoming = true, isVoip = true, appName = "WhatsApp", autoDismissSeconds = null))
                }
                "call_ongoing" -> {
                    stateManager.clearAllTasks()
                    stateManager.postTask(com.dynamicisland.app.model.CallTask(callerName = "Alex Rivera", callerNumber = "+1 (555) 438-9201", isIncoming = false, durationSeconds = 345, isMuted = false, isSpeakerOn = true, appName = "Phone", autoDismissSeconds = null))
                }
                "nav" -> {
                    stateManager.clearAllTasks()
                    stateManager.postTask(com.dynamicisland.app.model.NavigationTask(direction = com.dynamicisland.app.model.NavDirection.TURN_RIGHT, distanceText = "150 m", instruction = "Turn right onto Silicon Valley Blvd", etaText = "14 min", autoDismissSeconds = null))
                }
                "timer" -> {
                    stateManager.clearAllTasks()
                    stateManager.postTask(com.dynamicisland.app.model.TimerTask(label = "Tea Timer", remainingSeconds = 120, totalSeconds = 300, isRunning = true, autoDismissSeconds = null))
                }
                "progress" -> {
                    stateManager.clearAllTasks()
                    stateManager.postTask(com.dynamicisland.app.model.ProgressTask(title = "Sharing via QuickShare", progressPercent = 68, speedText = "32 MB/s", autoDismissSeconds = null))
                }
                "notification" -> {
                    stateManager.clearAllTasks()
                    stateManager.postTask(com.dynamicisland.app.model.NotificationTask(id = "msg_1", title = "Liam Vance", message = "See you at the coffee shop in 10 mins!", packageName = "com.whatsapp", appName = "WhatsApp", autoDismissSeconds = null))
                }
                "split" -> {
                    stateManager.clearAllTasks()
                    stateManager.postTask(com.dynamicisland.app.model.MediaTask(title = "Blinding Lights", artist = "The Weeknd", isPlaying = true, packageName = "app.morphe.android.apps.youtube.music", autoDismissSeconds = null))
                    stateManager.postTask(com.dynamicisland.app.model.TimerTask(label = "Oven", remainingSeconds = 250, autoDismissSeconds = null))
                }
                "tripple" -> {
                    stateManager.clearAllTasks()
                    stateManager.postTask(com.dynamicisland.app.model.MediaTask(title = "Blinding Lights", artist = "The Weeknd", isPlaying = true, packageName = "app.morphe.android.apps.youtube.music", autoDismissSeconds = null))
                    stateManager.postTask(com.dynamicisland.app.model.NavigationTask(direction = com.dynamicisland.app.model.NavDirection.TURN_LEFT, distanceText = "300 m", instruction = "Turn left on 5th Ave", autoDismissSeconds = null))
                    stateManager.postTask(com.dynamicisland.app.model.TimerTask(label = "Workout", remainingSeconds = 45, autoDismissSeconds = null))
                }
                "ambient" -> {
                    stateManager.clearAllTasks()
                    stateManager.collapseIsland()
                }
                "center" -> {
                    settingsRepo.applyPreset(com.dynamicisland.app.model.CutoutPosition.CENTER_PUNCH_HOLE)
                    stateManager.clearAllTasks()
                    stateManager.collapseIsland()
                }
                "demo_all", "run_all" -> stateManager.startAutomatedDemo()
                "stop_demo" -> stateManager.stopAutomatedDemo()
                "expand" -> stateManager.expandIsland()
                "collapse" -> stateManager.collapseIsland()
                "clear" -> {
                    stateManager.clearAllTasks()
                    stateManager.collapseIsland()
                }
            }
        }
    }
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        savedStateRegistryController.performRestore(Bundle())
        super.onCreate()
        instance = this

        settingsRepo = SettingsRepository.getInstance(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        startInForeground()
        registerHardwareListeners()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(testReceiver, IntentFilter("com.dynamicisland.TRIGGER_TEST"), Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(testReceiver, IntentFilter("com.dynamicisland.TRIGGER_TEST"))
            }
        } catch (_: Exception) {}
        setupOverlayWindow()
        observeStateForHitboxResizing()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        unregisterHardwareListeners()
        try {
            unregisterReceiver(testReceiver)
        } catch (_: Exception) {}
        removeOverlay()
        appViewModelStore.clear()
        if (instance == this) {
            instance = null
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!IslandAccessibilityService.isRunning) {
            applyWindowLayout(stateManager.islandMode.value, settingsRepo.configFlow.value)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startInForeground()
        return START_STICKY
    }

    fun removeOverlay() {
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            composeView = null
        }
    }

    private fun startInForeground() {
        val channelId = "dynamic_island_core_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Dynamic Island Core Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Dynamic Island running smoothly"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_desc))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (_: Exception) {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("IslandOverlayService", "startForeground error", e)
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        return try {
            val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
            val enabledServices = am?.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            enabledServices?.any {
                it.resolveInfo.serviceInfo.packageName == packageName &&
                it.resolveInfo.serviceInfo.name.contains("IslandAccessibilityService")
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun setupOverlayWindow() {
        if (IslandAccessibilityService.isRunning || isAccessibilityEnabled()) {
            removeOverlay()
            return
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
        windowLayoutParams = params

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@IslandOverlayService)
            setViewTreeViewModelStoreOwner(this@IslandOverlayService)
            setViewTreeSavedStateRegistryOwner(this@IslandOverlayService)

            setContent {
                DynamicIslandOverlay(
                    settingsRepo = settingsRepo,
                    stateManager = stateManager
                )
            }
        }

        try {
            windowManager.addView(composeView, params)
        } catch (_: Exception) {}
    }

    private fun observeStateForHitboxResizing() {
        serviceScope.launch {
            kotlinx.coroutines.flow.combine(
                stateManager.islandMode,
                settingsRepo.configFlow
            ) { mode, config -> Pair(mode, config) }
                .collectLatest { (mode, config) ->
                    if (IslandAccessibilityService.isRunning || isAccessibilityEnabled()) {
                        removeOverlay()
                        return@collectLatest
                    }
                    composeView?.visibility = View.VISIBLE
                    applyWindowLayout(mode, config)
                }
        }
    }

    private fun applyWindowLayout(mode: IslandMode, config: CutoutConfig) {
        val params = windowLayoutParams ?: return
        val view = composeView ?: return
        val density = resources.displayMetrics.density
        val screenMetrics = resources.displayMetrics
        val screenW = screenMetrics.widthPixels
        val screenH = screenMetrics.heightPixels

        stateManager.alwaysShowAmbient = config.alwaysShowAmbientPill

        val display = getSafeDisplay()
        val rotation = display?.rotation ?: Surface.ROTATION_0
        val isExpanded = mode == IslandMode.EXPANDED

        if (mode == IslandMode.HIDDEN) {
            params.width = 1
            params.height = 1
            params.x = 0
            params.y = 0
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        } else {
            val bounds = OrientationHelper.calculateWindowBounds(
                rotation = rotation,
                isExpanded = isExpanded,
                config = config,
                density = density,
                screenW = screenW,
                screenH = screenH
            )
            params.gravity = bounds.gravity
            params.width = bounds.width
            params.height = bounds.height
            params.x = bounds.x
            params.y = bounds.y
            params.flags = if (isExpanded) {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            } else {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            }
        }

        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    private fun getSafeDisplay(): Display? {
        return try {
            val dm = getSystemService(Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
            dm?.getDisplay(Display.DEFAULT_DISPLAY) ?: @Suppress("DEPRECATION") windowManager.defaultDisplay
        } catch (_: Exception) {
            try {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun getLandscapeCameraCenter(
        display: Display?,
        config: CutoutConfig,
        density: Float,
        screenW: Int,
        screenH: Int
    ): Pair<Float, Float> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && display != null) {
            val cutout = display.cutout
            if (cutout != null && cutout.boundingRects.isNotEmpty()) {
                val rect = cutout.boundingRects.first()
                return Pair(rect.centerX().toFloat(), rect.centerY().toFloat())
            }
        }

        val rotation = display?.rotation ?: Surface.ROTATION_0
        val portraitW = minOf(screenW, screenH).toFloat()
        val portraitH = maxOf(screenW, screenH).toFloat()

        val camX_p = (portraitW / 2f) + (config.offsetX * density)
        val camY_p = (config.offsetY * density) + ((config.pillHeight * density) / 2f)

        return when (rotation) {
            Surface.ROTATION_90 -> Pair(camY_p, portraitW - camX_p)
            Surface.ROTATION_270 -> Pair(portraitH - camY_p, camX_p)
            Surface.ROTATION_180 -> Pair(portraitW - camX_p, portraitH - camY_p)
            else -> Pair(camX_p, camY_p)
        }
    }

    private fun safeRegisterReceiver(receiver: BroadcastReceiver?, filter: IntentFilter, exported: Boolean = false) {
        if (receiver == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val flags = if (exported) Context.RECEIVER_EXPORTED else Context.RECEIVER_NOT_EXPORTED
                registerReceiver(receiver, filter, flags)
            } else {
                registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            Log.e("IslandOverlayService", "safeRegisterReceiver failed: ${e.message}")
        }
    }

    private fun registerHardwareListeners() {
        hardwareReceiver = HardwareEventReceiver(stateManager) { isInteractiveAndUnlocked ->
            if (!isInteractiveAndUnlocked) {
                if (stateManager.islandMode.value == IslandMode.EXPANDED) {
                    stateManager.collapseIsland()
                }
            } else {
                composeView?.visibility = View.VISIBLE
                applyWindowLayout(stateManager.islandMode.value, settingsRepo.configFlow.value)
            }
        }
        val filter = HardwareEventReceiver.createIntentFilter()
        safeRegisterReceiver(hardwareReceiver, filter, exported = false)
    }

    private fun unregisterHardwareListeners() {
        hardwareReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {}
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        var instance: IslandOverlayService? = null
            private set

        fun removeOverlayIfPresent() {
            instance?.removeOverlay()
        }
    }
}
