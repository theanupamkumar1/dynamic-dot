package com.dynamicisland.app.service

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import android.view.animation.DecelerateInterpolator
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.dynamicisland.app.manager.IslandStateManager
import com.dynamicisland.app.manager.SettingsRepository
import com.dynamicisland.app.model.CutoutConfig
import com.dynamicisland.app.model.IslandMode
import com.dynamicisland.app.receiver.HardwareEventReceiver
import com.dynamicisland.app.ui.island.DynamicIslandOverlay
import com.dynamicisland.app.util.OrientationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * IslandAccessibilityService:
 * Enables WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY (Z-Order: 221,000).
 * This places the Dynamic Island ABOVE the Android Status Bar (Z-Order: 151,000).
 *
 * Handles:
 * 1. Landscape mode anchoring strictly to the camera hole when collapsed,
 *    and displaying expanded content only when clicked/expanded.
 * 2. Automatic hiding whenever the notification shade / quick settings panel is pulled down.
 * 3. Smooth origin expansion without sudden jumps to screen center.
 */
class IslandAccessibilityService : AccessibilityService(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val appViewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = appViewModelStore

    private lateinit var windowManager: WindowManager
    private lateinit var settingsRepo: SettingsRepository
    private val stateManager = IslandStateManager.instance
    private var composeView: ComposeView? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var hardwareReceiver: HardwareEventReceiver? = null
    private var closeDialogsReceiver: BroadcastReceiver? = null
    private var keyguardManager: KeyguardManager? = null
    private var isVisibleAndUnlocked = true

    private var offsetXAnimator: ValueAnimator? = null
    private var previousMode: IslandMode = IslandMode.HIDDEN

    override fun onCreate() {
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        super.onCreate()

        settingsRepo = SettingsRepository.getInstance(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        instance = this
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        Log.i(TAG, "IslandAccessibilityService connected. Initializing TYPE_ACCESSIBILITY_OVERLAY.")

        setupAccessibilityOverlayWindow()
        setupAppLaunchFallback()
        registerHardwareAndScreenListeners()
        observeStateForHitboxResizing()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "onConfigurationChanged orientation: ${newConfig.orientation}")
        if (isVisibleAndUnlocked) {
            applyWindowLayout(stateManager.islandMode.value, settingsRepo.configFlow.value)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            if (!pkg.isNullOrEmpty() && pkg != packageName) {
                stateManager.setForegroundPackage(pkg)
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "IslandAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceScope.cancel()
        offsetXAnimator?.cancel()
        unregisterHardwareAndScreenListeners()
        removeOverlay()
        appViewModelStore.clear()
        if (instance == this) {
            instance = null
        }
    }

    private fun setupAccessibilityOverlayWindow() {
        if (composeView != null) return

        val allowOnLock = settingsRepo.configFlow.value.showOnLockscreen
        val lockFlag = if (allowOnLock) WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED else 0
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    lockFlag,
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
            setViewTreeLifecycleOwner(this@IslandAccessibilityService)
            setViewTreeViewModelStoreOwner(this@IslandAccessibilityService)
            setViewTreeSavedStateRegistryOwner(this@IslandAccessibilityService)

            setOnTouchListener { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
                    if (stateManager.islandMode.value == IslandMode.EXPANDED) {
                        stateManager.collapseIsland()
                        return@setOnTouchListener true
                    }
                }
                false
            }

            setContent {
                DynamicIslandOverlay(
                    settingsRepo = settingsRepo,
                    stateManager = stateManager
                )
            }
        }

        try {
            windowManager.addView(composeView, params)
            Log.i(TAG, "Added TYPE_ACCESSIBILITY_OVERLAY window successfully at layer 221000")
            IslandOverlayService.removeOverlayIfPresent()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add accessibility overlay window", e)
        }
    }

    private val testReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val type = intent?.getStringExtra("type") ?: return
            Log.d(TAG, "testReceiver received type: $type")
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
                    stateManager.postTask(com.dynamicisland.app.model.MediaTask(title = "Starboy (feat. Daft Punk)", artist = "The Weeknd", isPlaying = true, playbackPositionMs = 65000, trackDurationMs = 230000, packageName = "app.morphe.android.apps.youtube.music", autoDismissSeconds = null, accentColor = 0xFF1DB954L))
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
            Log.e("IslandAccessibility", "safeRegisterReceiver failed for action ${try { filter.getAction(0) } catch (_: Exception) { "" }}: ${e.message}")
        }
    }

    private fun registerHardwareAndScreenListeners() {
        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isScreenOn = pm?.isInteractive ?: true
        val isLocked = keyguardManager?.isKeyguardLocked ?: false
        val allowOnLock = settingsRepo.configFlow.value.showOnLockscreen
        isVisibleAndUnlocked = isScreenOn && (!isLocked || allowOnLock)

        hardwareReceiver = HardwareEventReceiver(stateManager) { isInteractiveAndUnlocked ->
            updateOverlayVisibility(isInteractiveAndUnlocked)
        }
        val filter = HardwareEventReceiver.createIntentFilter()
        safeRegisterReceiver(hardwareReceiver, filter, exported = false)

        // Close system dialogs receiver to detect when notification shade / dialogs dismiss
        closeDialogsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                stateManager.setNotificationPanelOpen(false)
            }
        }
        val closeFilter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        safeRegisterReceiver(closeDialogsReceiver, closeFilter, exported = false)

        val testFilter = IntentFilter("com.dynamicisland.TRIGGER_TEST")
        safeRegisterReceiver(testReceiver, testFilter, exported = true)
    }

    private fun unregisterHardwareAndScreenListeners() {
        hardwareReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {}
            hardwareReceiver = null
        }
        closeDialogsReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {}
            closeDialogsReceiver = null
        }
        try {
            unregisterReceiver(testReceiver)
        } catch (_: Exception) {}
    }

    private fun updateOverlayVisibility(interactiveAndUnlocked: Boolean) {
        val allowOnLock = settingsRepo.configFlow.value.showOnLockscreen
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isScreenOn = pm?.isInteractive ?: true
        val isLocked = keyguardManager?.isKeyguardLocked ?: false
        val shouldShow = isScreenOn && (!isLocked || allowOnLock)
        isVisibleAndUnlocked = shouldShow
        if (!shouldShow) {
            composeView?.visibility = View.GONE
            if (stateManager.islandMode.value == IslandMode.EXPANDED) {
                stateManager.collapseIsland()
            }
        } else {
            val view = composeView ?: return
            view.visibility = View.VISIBLE
            applyWindowLayout(stateManager.islandMode.value, settingsRepo.configFlow.value)
        }
    }

    private fun setupAppLaunchFallback() {
        if (stateManager.onAppLaunchRequest == null) {
            stateManager.onAppLaunchRequest = { pkg ->
                try {
                    val optionsBundle = if (Build.VERSION.SDK_INT >= 34) {
                        android.app.ActivityOptions.makeBasic().apply {
                            setPendingIntentBackgroundActivityStartMode(android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                        }.toBundle()
                    } else null

                    val intent = if (pkg.isNotEmpty()) {
                        packageManager.getLaunchIntentForPackage(pkg)?.apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                        }
                    } else null

                    if (intent != null) {
                        startActivity(intent, optionsBundle)
                    } else {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(dialIntent, optionsBundle)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to launch fallback app: $pkg", e)
                }
            }
        }
    }

    private fun observeStateForHitboxResizing() {
        serviceScope.launch {
            kotlinx.coroutines.flow.combine(
                stateManager.islandMode,
                settingsRepo.configFlow
            ) { mode, config -> Pair(mode, config) }
                .collectLatest { (mode, config) ->
                    if (isVisibleAndUnlocked) {
                        applyWindowLayout(mode, config)
                    }
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
        view.visibility = View.VISIBLE

        val display = getSafeDisplay()
        val rotation = display?.rotation ?: Surface.ROTATION_0
        val isLandscape = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270 || resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
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

            // Smooth origin animation when transitioning between expanded and collapsed in portrait
            if (!isLandscape && previousMode == IslandMode.EXPANDED && !isExpanded && config.offsetX != 0f) {
                animateWindowX(params, view, fromX = 0, toX = bounds.x)
            } else if (!isLandscape && previousMode != IslandMode.EXPANDED && isExpanded && config.offsetX != 0f) {
                animateWindowX(params, view, fromX = bounds.x, toX = 0)
            } else {
                params.x = bounds.x
            }

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

        previousMode = mode

        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    private fun animateWindowX(
        params: WindowManager.LayoutParams,
        view: View,
        fromX: Int,
        toX: Int
    ) {
        offsetXAnimator?.cancel()
        offsetXAnimator = ValueAnimator.ofInt(fromX, toX).apply {
            duration = 320L
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                params.x = anim.animatedValue as Int
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (_: Exception) {}
            }
            start()
        }
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
        // 1. Try native DisplayCutout bounding rect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && display != null) {
            val cutout = display.cutout
            if (cutout != null && cutout.boundingRects.isNotEmpty()) {
                val rect = cutout.boundingRects.first()
                return Pair(rect.centerX().toFloat(), rect.centerY().toFloat())
            }
        }

        // 2. Deterministic rotation mapping from calibrated portrait coordinates
        val rotation = display?.rotation ?: Surface.ROTATION_0
        val portraitW = minOf(screenW, screenH).toFloat()
        val portraitH = maxOf(screenW, screenH).toFloat()

        val camX_p = (portraitW / 2f) + (config.offsetX * density)
        val camY_p = (config.offsetY * density) + ((config.pillHeight * density) / 2f)

        return when (rotation) {
            Surface.ROTATION_90 -> {
                // Rotated clockwise: Physical top edge is on the LEFT
                Pair(camY_p, portraitW - camX_p)
            }
            Surface.ROTATION_270 -> {
                // Rotated counter-clockwise: Physical top edge is on the RIGHT
                Pair(portraitH - camY_p, camX_p)
            }
            Surface.ROTATION_180 -> {
                Pair(portraitW - camX_p, portraitH - camY_p)
            }
            else -> {
                Pair(camX_p, camY_p)
            }
        }
    }

    private fun removeOverlay() {
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            composeView = null
        }
    }

    companion object {
        private const val TAG = "IslandAccessibility"
        var instance: IslandAccessibilityService? = null
            private set

        val isRunning: Boolean get() = instance != null
    }
}
