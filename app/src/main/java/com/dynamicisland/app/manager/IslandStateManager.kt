package com.dynamicisland.app.manager

import com.dynamicisland.app.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IslandStateManager private constructor() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val activeTasks = mutableListOf<IslandTask>()

    private val _primaryTask = MutableStateFlow<IslandTask?>(null)
    val primaryTask: StateFlow<IslandTask?> = _primaryTask.asStateFlow()

    private val _secondaryTask = MutableStateFlow<IslandTask?>(null)
    val secondaryTask: StateFlow<IslandTask?> = _secondaryTask.asStateFlow()

    private val _tertiaryTask = MutableStateFlow<IslandTask?>(null)
    val tertiaryTask: StateFlow<IslandTask?> = _tertiaryTask.asStateFlow()

    private val _islandMode = MutableStateFlow(IslandMode.HIDDEN)
    val islandMode: StateFlow<IslandMode> = _islandMode.asStateFlow()

    private val _taskCount = MutableStateFlow(0)
    val taskCount: StateFlow<Int> = _taskCount.asStateFlow()

    private val _isNotificationPanelOpen = MutableStateFlow(false)
    val isNotificationPanelOpen: StateFlow<Boolean> = _isNotificationPanelOpen.asStateFlow()

    // Listener callbacks for system interactions
    var onMediaPlayPauseToggle: (() -> Unit)? = null
    var onMediaNext: (() -> Unit)? = null
    var onMediaPrevious: (() -> Unit)? = null
    var onMediaSeek: ((Long) -> Unit)? = null
    var onMediaStop: (() -> Unit)? = null
    var onCallHangup: (() -> Unit)? = null
    var onCallAnswer: (() -> Unit)? = null
    var onCallMuteToggle: (() -> Unit)? = null
    var onCallSpeakerToggle: (() -> Unit)? = null
    var onTimerCancel: (() -> Unit)? = null
    var onAppLaunchRequest: ((String) -> Unit)? = null

    init {
        recomputeMode()
    }

    private val autoDismissJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()

    /**
     * Submit or update an active task in the island
     */
    fun postTask(task: IslandTask) {
        android.util.Log.d("DynamicIsland", "postTask: ${task.id}, priority: ${task.priority}")
        synchronized(activeTasks) {
            val existingIndex = activeTasks.indexOfFirst { it.id == task.id }
            if (existingIndex >= 0) {
                activeTasks[existingIndex] = task
            } else {
                activeTasks.add(task)
            }
            sortAndRecomputeState()
        }

        // Schedule auto-dismiss for transient alerts independently per taskId
        task.autoDismissSeconds?.let { seconds ->
            autoDismissJobs[task.id]?.cancel()
            autoDismissJobs[task.id] = scope.launch {
                delay(seconds * 1000)
                removeTask(task.id)
                autoDismissJobs.remove(task.id)
            }
        }
    }

    /**
     * Remove a task from the island
     */
    fun removeTask(taskId: String) {
        autoDismissJobs.remove(taskId)?.cancel()
        synchronized(activeTasks) {
            activeTasks.removeAll { it.id == taskId }
            sortAndRecomputeState()
        }
    }

    /**
     * Clear all active tasks from the island
     */
    fun clearAllTasks() {
        autoDismissJobs.values.forEach { it.cancel() }
        autoDismissJobs.clear()
        synchronized(activeTasks) {
            activeTasks.clear()
            sortAndRecomputeState()
        }
    }

    /**
     * Stops the active task/activity and dismisses it from the island
     * (e.g. on left swipe of expanded card)
     */
    fun stopAndDismissActiveTask() {
        val task = _primaryTask.value ?: return
        when (task) {
            is MediaTask -> {
                onMediaStop?.invoke()
                removeTask(task.id)
                collapseIsland()
            }
            is CallTask -> {
                onCallHangup?.invoke()
                removeTask(task.id)
                collapseIsland()
            }
            is TimerTask -> {
                onTimerCancel?.invoke()
                removeTask(task.id)
                collapseIsland()
            }
            else -> {
                removeTask(task.id)
                collapseIsland()
            }
        }
    }

    /**
     * Set notification panel state: when notification shade / quick settings is open,
     * the island is hidden completely.
     */
    fun setNotificationPanelOpen(open: Boolean) {
        if (_isNotificationPanelOpen.value != open) {
            _isNotificationPanelOpen.value = open
            if (open && _islandMode.value == IslandMode.EXPANDED) {
                // If shade is pulled down while island is expanded, collapse to compact pill
                collapseIsland()
            }
        }
    }

    /**
     * Honor Magic Capsule & Multitasking:
     * Swipe left or right on the compact pill to cycle through active tasks
     */
    fun cycleStackedTasks(forward: Boolean) {
        synchronized(activeTasks) {
            if (activeTasks.size <= 1) return
            if (forward) {
                val first = activeTasks.removeAt(0)
                activeTasks.add(first)
            } else {
                val last = activeTasks.removeAt(activeTasks.size - 1)
                activeTasks.add(0, last)
            }
            recomputeStateWithoutSorting()
        }
    }

    /**
     * Tap action: Launch the originating application
     */
    fun onIslandTapped() {
        val current = _primaryTask.value ?: return
        when (current) {
            is MediaTask -> onAppLaunchRequest?.invoke(current.packageName)
            is NavigationTask -> onAppLaunchRequest?.invoke("com.google.android.apps.maps")
            is CallTask -> onAppLaunchRequest?.invoke("")
            else -> expandIsland()
        }
    }

    private fun sortAndRecomputeState() {
        // Sort descending by priority: highest priority comes first
        activeTasks.sortByDescending { it.priority.value }
        recomputeStateWithoutSorting()
    }

    private fun recomputeStateWithoutSorting() {
        _taskCount.value = activeTasks.size
        _primaryTask.value = activeTasks.getOrNull(0)
        _secondaryTask.value = activeTasks.getOrNull(1)
        _tertiaryTask.value = activeTasks.getOrNull(2)

        checkForegroundMatch()

        if (_islandMode.value != IslandMode.EXPANDED) {
            recomputeMode()
        }
    }

    val foregroundPackage = MutableStateFlow<String?>(null)
    val isForegroundAppMatching = MutableStateFlow(false)

    /**
     * dynamicSpot Feature: Detect current foreground app to auto-hide the island
     * when looking at the originating app (e.g. YouTube Music, Spotify).
     */
    fun setForegroundPackage(pkg: String?) {
        if (foregroundPackage.value != pkg) {
            foregroundPackage.value = pkg
            checkForegroundMatch()
        }
    }

    private fun checkForegroundMatch() {
        val fg = foregroundPackage.value ?: run {
            isForegroundAppMatching.value = false
            return
        }
        val task = _primaryTask.value
        if (task == null) {
            isForegroundAppMatching.value = false
            return
        }
        val matches = when (task) {
            is MediaTask -> task.packageName.isNotEmpty() && task.packageName == fg
            is CallTask -> fg.contains("dialer") || fg.contains("telecom") || fg.contains("phone")
            is NavigationTask -> fg.contains("maps")
            is TimerTask -> fg.contains("clock")
            is NotificationTask -> task.packageName.isNotEmpty() && task.packageName == fg
            else -> false
        }
        isForegroundAppMatching.value = matches
    }

    var alwaysShowAmbient: Boolean = true

    private var expandJob: Job? = null

    fun expandIsland(autoCollapseSeconds: Int? = null) {
        _islandMode.value = IslandMode.EXPANDED
        expandJob?.cancel()
        if (autoCollapseSeconds != null && autoCollapseSeconds > 0) {
            expandJob = scope.launch {
                delay(autoCollapseSeconds * 1000L)
                collapseIsland()
            }
        }
    }

    fun collapseIsland() {
        expandJob?.cancel()
        recomputeMode()
    }

    /**
     * Sends an inline RemoteInput reply to WhatsApp, Telegram, Messages, etc.
     */
    fun sendInlineReply(
        context: android.content.Context,
        replyIntent: android.app.PendingIntent?,
        resultKey: String?,
        replyText: String
    ) {
        if (replyIntent == null || resultKey == null || replyText.isBlank()) return
        try {
            val intent = android.content.Intent()
            val bundle = android.os.Bundle()
            bundle.putCharSequence(resultKey, replyText)
            android.app.RemoteInput.addResultsToIntent(
                arrayOf(android.app.RemoteInput.Builder(resultKey).build()),
                intent,
                bundle
            )
            replyIntent.send(context, 0, intent)
            val current = _primaryTask.value
            if (current != null) {
                removeTask(current.id)
            }
            collapseIsland()
        } catch (e: Exception) {
            android.util.Log.e("DynamicIsland", "Error sending inline reply", e)
        }
    }

    private fun recomputeMode() {
        val count = activeTasks.size
        if (count == 0) {
            if (alwaysShowAmbient) {
                _islandMode.value = IslandMode.AMBIENT
                _primaryTask.value = AmbientTask()
            } else {
                _islandMode.value = IslandMode.HIDDEN
                _primaryTask.value = null
            }
        } else {
            _islandMode.value = when (count) {
                1 -> IslandMode.COLLAPSED_SINGLE
                2 -> IslandMode.COLLAPSED_SPLIT
                else -> IslandMode.COLLAPSED_TRIPPLE
            }
        }
    }

    // Automated Feature Test & Demo Runner
    private var demoJob: Job? = null
    val isDemoRunning = MutableStateFlow(false)
    val demoCurrentStep = MutableStateFlow("")

    fun startAutomatedDemo() {
        demoJob?.cancel()
        demoJob = scope.launch {
            isDemoRunning.value = true
            try {
                clearAllTasks()
                collapseIsland()

                // 1. Ambient Notch Cover (Idle)
                demoCurrentStep.value = "1. Ambient Notch Mask (Idle)"
                delay(2200)

                // 2. Music Playing (Minimised) - Sparks by Coldplay
                clearAllTasks()
                collapseIsland()
                demoCurrentStep.value = "2. Music: Sparks - Coldplay (Minimised)"
                postTask(MediaTask(
                    title = "Sparks",
                    artist = "Coldplay",
                    isPlaying = true,
                    playbackPositionMs = 84000,
                    trackDurationMs = 228000,
                    packageName = "app.morphe.android.apps.youtube.music",
                    autoDismissSeconds = null
                ))
                delay(3000)

                // 3. Music Playing (Expanded)
                demoCurrentStep.value = "3. Music: Sparks (Expanded Dotted Scrubber)"
                expandIsland()
                delay(4000)
                collapseIsland()
                delay(600)

                // 4. Incoming Call: Anupam (Minimised)
                clearAllTasks()
                collapseIsland()
                demoCurrentStep.value = "4. Incoming Call: Anupam (Minimised)"
                postTask(CallTask(
                    callerName = "Anupam",
                    callerNumber = "+1 (555) 019-2834",
                    isIncoming = true,
                    isVoip = true,
                    appName = "Phone",
                    autoDismissSeconds = null
                ))
                delay(3000)

                // 5. Incoming Call (Expanded)
                demoCurrentStep.value = "5. Incoming Call (Expanded Avatar Card)"
                expandIsland()
                delay(3800)
                collapseIsland()
                delay(600)

                // 6. Timer 03:27 (Minimised)
                clearAllTasks()
                collapseIsland()
                demoCurrentStep.value = "6. Timer: 03:27 (Minimised)"
                postTask(TimerTask(
                    label = "Timer",
                    remainingSeconds = 207,
                    totalSeconds = 300,
                    isRunning = true,
                    autoDismissSeconds = null
                ))
                delay(3000)

                // 7. Timer (Expanded) - Nothing Dotted Ring
                demoCurrentStep.value = "7. Timer (Expanded Dotted Circular Ring)"
                expandIsland()
                delay(3800)
                collapseIsland()
                delay(600)

                // 8. Navigation (Minimised) - 200 m
                clearAllTasks()
                collapseIsland()
                demoCurrentStep.value = "8. Navigation: 200 m Turn Right"
                postTask(NavigationTask(
                    direction = NavDirection.TURN_RIGHT,
                    distanceText = "200 m",
                    instruction = "Turn right",
                    etaText = "12 min",
                    autoDismissSeconds = null
                ))
                delay(3000)

                // 9. Navigation (Expanded)
                demoCurrentStep.value = "9. Navigation (Expanded Route Track)"
                expandIsland()
                delay(3600)
                collapseIsland()
                delay(600)

                // 10. Voice Recording 00:12 (Minimised)
                clearAllTasks()
                collapseIsland()
                demoCurrentStep.value = "10. Voice Recording (Minimised Wave)"
                postTask(VoiceRecordingTask(
                    durationSeconds = 12L,
                    isRecording = true,
                    autoDismissSeconds = null
                ))
                delay(3000)

                // 11. Voice Recording (Expanded)
                demoCurrentStep.value = "11. Voice Recording (Expanded Card)"
                expandIsland()
                delay(3600)
                collapseIsland()
                delay(600)

                // 12. Charging 62% (Minimised)
                clearAllTasks()
                collapseIsland()
                demoCurrentStep.value = "12. Charging 62% (Minimised Arc)"
                postTask(ChargingAlert(
                    batteryPercent = 62,
                    isCharging = true,
                    isFastCharging = true,
                    wattage = "Charging",
                    autoDismissSeconds = null
                ))
                delay(3000)

                // 13. Charging 62% (Expanded) - Nothing Dot Ring + Bolt
                demoCurrentStep.value = "13. Charging 62% (Expanded Dot Ring)"
                expandIsland()
                delay(3800)
                collapseIsland()
                delay(600)

                // 14. Earbuds Connected: Nothing Ear (2) (Minimised)
                clearAllTasks()
                collapseIsland()
                demoCurrentStep.value = "14. Nothing Ear (2) Connected"
                postTask(EarbudsTask(
                    deviceName = "Nothing Ear (2)",
                    leftBattery = 90,
                    rightBattery = 80,
                    caseBattery = 60,
                    autoDismissSeconds = null
                ))
                delay(3000)

                // 15. Earbuds Connected (Expanded) - Iconic Graphic + L/R/Case Pills
                demoCurrentStep.value = "15. Nothing Ear (2) (Expanded Transparent Stem)"
                expandIsland()
                delay(4000)
                collapseIsland()
                delay(600)

                // 16. Hotspot / Tethering 1 device (Minimised)
                clearAllTasks()
                collapseIsland()
                demoCurrentStep.value = "16. Hotspot (1 Device Connected)"
                postTask(HotspotTask(
                    connectedDevices = 1,
                    isEnabled = true,
                    autoDismissSeconds = null
                ))
                delay(3000)

                // 17. Hotspot (Expanded) - Concentric Antenna Waves
                demoCurrentStep.value = "17. Hotspot (Expanded Concentric Dot Waves)"
                expandIsland()
                delay(3800)
                collapseIsland()
                delay(600)

                // 18. Video Playing / YouTube (Minimised)
                clearAllTasks()
                collapseIsland()
                demoCurrentStep.value = "18. YouTube Video Playing"
                postTask(MediaTask(
                    title = "Nothing OS 3.0 Official Trailer",
                    artist = "YouTube",
                    isPlaying = true,
                    playbackPositionMs = 45000,
                    trackDurationMs = 180000,
                    packageName = "com.google.android.youtube",
                    autoDismissSeconds = null
                ))
                delay(3000)

                // 19. Silent Mode (Minimised)
                clearAllTasks()
                collapseIsland()
                demoCurrentStep.value = "19. Silent Mode On (Minimised)"
                postTask(RingerAlert(state = RingerState.SILENT, autoDismissSeconds = null))
                delay(2800)

                // 20. Silent Mode (Expanded)
                demoCurrentStep.value = "20. Silent Mode (Expanded Red Bell)"
                expandIsland()
                delay(3400)
                collapseIsland()
                delay(600)

                // 21. Face Unlock Alert (Minimised)
                clearAllTasks()
                collapseIsland()
                demoCurrentStep.value = "21. Face Unlock (Unlocking...)"
                postTask(FaceUnlockAlert(isUnlocking = true, autoDismissSeconds = null))
                delay(2600)

                // 22. Do Not Disturb (Minimised)
                clearAllTasks()
                collapseIsland()
                demoCurrentStep.value = "22. Do Not Disturb (Minimised)"
                postTask(DndAlert(isEnabled = true, autoDismissSeconds = null))
                delay(2600)

                // 23. Flight Mode (Minimised)
                clearAllTasks()
                collapseIsland()
                demoCurrentStep.value = "23. Flight Mode (Minimised)"
                postTask(FlightModeAlert(isEnabled = true, autoDismissSeconds = null))
                delay(2600)

                // 24. Bluetooth Connection (Minimised)
                clearAllTasks()
                collapseIsland()
                demoCurrentStep.value = "24. Bluetooth Connected"
                postTask(BluetoothAlert(deviceName = "Bluetooth", isConnected = true, autoDismissSeconds = null))
                delay(2600)

                // 25. System Alert: Screenshot Saved (Minimised)
                clearAllTasks()
                collapseIsland()
                demoCurrentStep.value = "25. System Alert: Screenshot Saved"
                postTask(ScreenshotAlert(autoDismissSeconds = null))
                delay(2800)

                // 26. Multiple Activities / Multi-task Stack (Minimised)
                clearAllTasks()
                collapseIsland()
                demoCurrentStep.value = "26. Multiple Activities Stack"
                postTask(MediaTask(
                    title = "Sparks",
                    artist = "Coldplay",
                    isPlaying = true,
                    packageName = "app.morphe.android.apps.youtube.music",
                    autoDismissSeconds = null
                ))
                postTask(TimerTask(
                    label = "Timer",
                    remainingSeconds = 207,
                    autoDismissSeconds = null
                ))
                postTask(NavigationTask(
                    direction = NavDirection.TURN_RIGHT,
                    distanceText = "200 m",
                    autoDismissSeconds = null
                ))
                delay(4000)

                // 27. Finished! Reset to Ambient
                demoCurrentStep.value = "Nothing OS Concept Demo Complete!"
                clearAllTasks()
                collapseIsland()
                delay(1500)
                demoCurrentStep.value = ""
            } finally {
                isDemoRunning.value = false
            }
        }
    }

    fun stopAutomatedDemo() {
        demoJob?.cancel()
        demoJob = null
        isDemoRunning.value = false
        demoCurrentStep.value = ""
        clearAllTasks()
        collapseIsland()
    }

    companion object {
        val instance: IslandStateManager by lazy { IslandStateManager() }
    }
}
