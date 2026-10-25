package com.dynamicisland.app.model

import android.graphics.Bitmap

/**
 * Visual presentation modes of the Dynamic Island
 */
enum class IslandMode {
    HIDDEN,
    AMBIENT,            // Minimal notch coverage when no active task
    COLLAPSED_SINGLE,   // Standard compact pill
    COLLAPSED_SPLIT,    // iOS-style dual-island (Main Pill + Detached auxiliary bubble)
    COLLAPSED_TRIPPLE,  // Honor Magic Capsule multi-task stack
    EXPANDED            // Full interactive widget card
}

/**
 * Cutout position preset
 */
enum class CutoutPosition {
    CENTER_PUNCH_HOLE,
    LEFT_PUNCH_HOLE,
    RIGHT_PUNCH_HOLE,
    PILL_CUTOUT,
    CUSTOM
}

/**
 * Priority tiers for dynamic arbitration:
 * Higher priority preempts or takes the leading pill position.
 */
enum class TaskPriority(val value: Int) {
    TRANSIENT_ALERT(100), // Charging, Ringer, Bluetooth, Flashlight (3-5s timeout)
    INCOMING_CALL(90),    // Active ringing call
    ONGOING_CALL(80),     // Active telephone or VoIP call
    NAVIGATION(70),       // Turn-by-turn navigation prompt
    TIMER(60),            // Countdown timer or stopwatch
    MEDIA(50),            // Spotify, Apple Music, YouTube Music
    PROGRESS(40),         // File download, QuickShare, ride ETA
    NOTIFICATION(30)      // Normal app message or alert
}

/**
 * Base sealed interface for any task displayed in the Island
 */
sealed interface IslandTask {
    val id: String
    val priority: TaskPriority
    val timestamp: Long
    val autoDismissSeconds: Long? // If non-null, disappears after N seconds
}

/**
 * Transient Hardware / System Alerts
 */
data class ChargingAlert(
    override val id: String = "charging_alert",
    override val priority: TaskPriority = TaskPriority.TRANSIENT_ALERT,
    override val timestamp: Long = System.currentTimeMillis(),
    override val autoDismissSeconds: Long? = 4L,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val isFastCharging: Boolean = false,
    val wattage: String = "",
    val isLowBattery: Boolean = false,
    val isFullyCharged: Boolean = false
) : IslandTask

enum class RingerState { RING, VIBRATE, SILENT }

data class RingerAlert(
    override val id: String = "ringer_alert",
    override val priority: TaskPriority = TaskPriority.TRANSIENT_ALERT,
    override val timestamp: Long = System.currentTimeMillis(),
    override val autoDismissSeconds: Long? = 3L,
    val state: RingerState
) : IslandTask

data class BluetoothAlert(
    override val id: String = "bluetooth_alert",
    override val priority: TaskPriority = TaskPriority.TRANSIENT_ALERT,
    override val timestamp: Long = System.currentTimeMillis(),
    override val autoDismissSeconds: Long? = 4L,
    val deviceName: String,
    val batteryPercent: Int? = null,
    val isConnected: Boolean = true
) : IslandTask

data class FlashlightAlert(
    override val id: String = "flashlight_alert",
    override val priority: TaskPriority = TaskPriority.TRANSIENT_ALERT,
    override val timestamp: Long = System.currentTimeMillis(),
    override val autoDismissSeconds: Long? = 3L,
    val isEnabled: Boolean
) : IslandTask

/**
 * Now Playing Media Playback
 */
data class MediaTask(
    override val id: String = "media_task",
    override val priority: TaskPriority = TaskPriority.MEDIA,
    override val timestamp: Long = System.currentTimeMillis(),
    override val autoDismissSeconds: Long? = null,
    val packageName: String = "",
    val title: String = "No Title",
    val artist: String = "Unknown Artist",
    val isPlaying: Boolean = false,
    val playbackPositionMs: Long = 0L,
    val trackDurationMs: Long = 0L,
    val albumArt: Bitmap? = null,
    val audioWaveAmplitudes: List<Float> = listOf(0.3f, 0.7f, 0.5f, 0.9f, 0.4f),
    val accentColor: Long? = null
) : IslandTask

/**
 * Phone and VoIP Calls
 */
data class CallTask(
    override val id: String = "call_task",
    val callerName: String = "Caller",
    val callerNumber: String = "",
    val isIncoming: Boolean = false,
    val isVoip: Boolean = false,
    val appName: String = "Phone",
    val durationSeconds: Long = 0L,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    override val timestamp: Long = System.currentTimeMillis(),
    override val autoDismissSeconds: Long? = null
) : IslandTask {
    override val priority: TaskPriority
        get() = if (isIncoming) TaskPriority.INCOMING_CALL else TaskPriority.ONGOING_CALL
}

/**
 * Turn-by-Turn Navigation
 */
enum class NavDirection {
    TURN_LEFT, TURN_RIGHT, TURN_SLIGHT_LEFT, TURN_SLIGHT_RIGHT,
    STRAIGHT, ROUNDABOUT, U_TURN, DESTINATION
}

data class NavigationTask(
    override val id: String = "nav_task",
    override val priority: TaskPriority = TaskPriority.NAVIGATION,
    override val timestamp: Long = System.currentTimeMillis(),
    override val autoDismissSeconds: Long? = null,
    val direction: NavDirection = NavDirection.TURN_RIGHT,
    val distanceText: String = "200 m",
    val instruction: String = "Turn right onto Grand Avenue",
    val etaText: String = "12 min",
    val appName: String = "Google Maps"
) : IslandTask

/**
 * Timers and Stopwatch
 */
data class TimerTask(
    override val id: String = "timer_task",
    override val priority: TaskPriority = TaskPriority.TIMER,
    override val timestamp: Long = System.currentTimeMillis(),
    override val autoDismissSeconds: Long? = null,
    val label: String = "Timer",
    val remainingSeconds: Long = 180L,
    val totalSeconds: Long = 300L,
    val isRunning: Boolean = true
) : IslandTask

/**
 * File Sharing & Download Progress
 */
data class ProgressTask(
    override val id: String = "progress_task",
    override val priority: TaskPriority = TaskPriority.PROGRESS,
    override val timestamp: Long = System.currentTimeMillis(),
    override val autoDismissSeconds: Long? = 5L,
    val title: String = "Sharing via QuickShare",
    val progressPercent: Int = 45,
    val speedText: String = "24 MB/s"
) : IslandTask

/**
 * Inline action item for notifications
 */
data class NotificationActionItem(
    val title: String,
    val actionIntent: android.app.PendingIntent? = null
)

/**
 * Messaging / Notification toast
 */
data class NotificationTask(
    override val id: String,
    override val priority: TaskPriority = TaskPriority.NOTIFICATION,
    override val timestamp: Long = System.currentTimeMillis(),
    override val autoDismissSeconds: Long? = 4L,
    val title: String,
    val message: String,
    val packageName: String,
    val appName: String,
    val accentColor: Long? = null,
    val largeIcon: Bitmap? = null,
    val bigPicture: Bitmap? = null,
    val contentIntent: android.app.PendingIntent? = null,
    val replyActionIntent: android.app.PendingIntent? = null,
    val replyResultKey: String? = null,
    val replyLabel: String? = null,
    val actions: List<NotificationActionItem> = emptyList()
) : IslandTask

/**
 * Ambient state when no task is active, blending into the camera notch
 */
data class AmbientTask(
    override val id: String = "ambient_task",
    override val priority: TaskPriority = TaskPriority.NOTIFICATION,
    override val timestamp: Long = System.currentTimeMillis(),
    override val autoDismissSeconds: Long? = null,
    val text: String = ""
) : IslandTask

/**
 * Calibration preview task for real-time alignment with phone camera cutout
 */
data class CalibrationTask(
    override val id: String = "calibration_task",
    override val priority: TaskPriority = TaskPriority.NOTIFICATION,
    override val timestamp: Long = System.currentTimeMillis(),
    override val autoDismissSeconds: Long? = null,
    val showSplit: Boolean = false
) : IslandTask

data class SecondaryCalibrationTask(
    override val id: String = "secondary_calibration_task",
    override val priority: TaskPriority = TaskPriority.NOTIFICATION,
    override val timestamp: Long = System.currentTimeMillis(),
    override val autoDismissSeconds: Long? = null
) : IslandTask

/**
 * Nothing OS Voice Recording
 */
data class VoiceRecordingTask(
    override val id: String = "voice_recording_task",
    override val priority: TaskPriority = TaskPriority.TIMER,
    override val timestamp: Long = System.currentTimeMillis(),
    override val autoDismissSeconds: Long? = null,
    val durationSeconds: Long = 12L,
    val isRecording: Boolean = true
) : IslandTask

/**
 * Nothing Earbuds (Nothing Ear (2)) Connected
 */
data class EarbudsTask(
    override val id: String = "earbuds_task",
    override val priority: TaskPriority = TaskPriority.TRANSIENT_ALERT,
    override val timestamp: Long = System.currentTimeMillis(),
    override val autoDismissSeconds: Long? = 5L,
    val deviceName: String = "Nothing Ear (2)",
    val leftBattery: Int = 90,
    val rightBattery: Int = 80,
    val caseBattery: Int = 60,
    val isConnected: Boolean = true
) : IslandTask

/**
 * Hotspot / Tethering Active
 */
data class HotspotTask(
    override val id: String = "hotspot_task",
    override val priority: TaskPriority = TaskPriority.PROGRESS,
    override val timestamp: Long = System.currentTimeMillis(),
    override val autoDismissSeconds: Long? = null,
    val connectedDevices: Int = 1,
    val isEnabled: Boolean = true
) : IslandTask

/**
 * Face Unlock Alert
 */
data class FaceUnlockAlert(
    override val id: String = "face_unlock_alert",
    override val priority: TaskPriority = TaskPriority.TRANSIENT_ALERT,
    override val timestamp: Long = System.currentTimeMillis(),
    override val autoDismissSeconds: Long? = 3L,
    val isUnlocking: Boolean = true
) : IslandTask

/**
 * Do Not Disturb Alert
 */
data class DndAlert(
    override val id: String = "dnd_alert",
    override val priority: TaskPriority = TaskPriority.TRANSIENT_ALERT,
    override val timestamp: Long = System.currentTimeMillis(),
    override val autoDismissSeconds: Long? = 3L,
    val isEnabled: Boolean = true
) : IslandTask

/**
 * Flight Mode Alert
 */
data class FlightModeAlert(
    override val id: String = "flight_mode_alert",
    override val priority: TaskPriority = TaskPriority.TRANSIENT_ALERT,
    override val timestamp: Long = System.currentTimeMillis(),
    override val autoDismissSeconds: Long? = 3L,
    val isEnabled: Boolean = true
) : IslandTask

/**
 * Screenshot Saved Alert
 */
data class ScreenshotAlert(
    override val id: String = "screenshot_alert",
    override val priority: TaskPriority = TaskPriority.TRANSIENT_ALERT,
    override val timestamp: Long = System.currentTimeMillis(),
    override val autoDismissSeconds: Long? = 4L,
    val thumbnail: Bitmap? = null
) : IslandTask

