package com.dynamicisland.app.model

/**
 * Visual design theme for the Dynamic Island
 */
enum class IslandTheme {
    REGULAR,    // Apple iOS Fluid Glass (pure black, smooth curves, iOS accents)
    NOTHING     // Nothing OS Aesthetic (monochrome, dot-matrix typography, Nothing Red accents #D71921)
}

/**
 * User-saved manual calibration preset
 */
data class CustomPreset(
    val id: String,
    val name: String,
    val offsetX: Float,
    val offsetY: Float,
    val pillWidth: Float,
    val pillHeight: Float,
    val pillRadius: Float,
    val cutoutGap: Float,
    val splitSpacing: Float
)

/**
 * Cutout position and sizing calibration configuration.
 * All dimension values are stored in Density-Independent Pixels (DP).
 */
data class CutoutConfig(
    val position: CutoutPosition = CutoutPosition.CENTER_PUNCH_HOLE,
    val offsetX: Float = 0f,          // Horizontal shift from center/anchor
    val offsetY: Float = 9f,          // Distance from top of the screen (calibrated to center over punch hole)
    val pillWidth: Float = 108f,      // Compact active width (Left wing + Cutout Gap + Right wing)
    val pillHeight: Float = 34f,      // Compact active height
    val pillRadius: Float = 17f,      // Corner radius
    val cutoutGap: Float = 32f,       // Protected black gap directly behind the camera punch hole
    val ambientWidth: Float = 48f,    // Idle ambient cover width
    val ambientHeight: Float = 28f,   // Idle ambient cover height
    val ambientRadius: Float = 14f,   // Idle ambient cover corner radius
    val splitSpacing: Float = 10f,    // Gap between primary pill & secondary bubble
    val secondarySize: Float = 34f,   // Size of detached circular bubble
    val enableHaptics: Boolean = true,
    val enableAutoSplit: Boolean = true,
    val enableTrippleStack: Boolean = true,
    val enableSwipeDismiss: Boolean = true,
    val enableLiquidGlow: Boolean = true,
    val alwaysShowAmbientPill: Boolean = true, // Always keep camera notch covered like built-in hardware
    val showCalibrationPreview: Boolean = false, // Live calibration test pill
    val expandedWidth: Float = 364f,
    val expandedHeight: Float = 175f,
    val theme: IslandTheme = IslandTheme.REGULAR,
    val activePresetName: String? = null,
    val hideInForeground: Boolean = true,       // dynamicSpot Feature: Auto-hide island when the active app is open on screen
    val enableGlowRing: Boolean = true,         // dynamicSpot Feature: Pulsing camera cutout glow ring for notifications & charging
    val colorizeFromArtwork: Boolean = true,    // dynamicSpot Feature: Dynamic accent color extracted from album art or app icons
    val ignoreDnd: Boolean = true,              // dynamicSpot Feature: Do not show notifications during Do Not Disturb
    val ignoreOngoing: Boolean = true,          // dynamicSpot Feature: Ignore persistent/ongoing notifications (downloads, services)
    val autoExpandNotifications: Boolean = false, // dynamicSpot Feature: Automatically expand notification card on receive
    val touchOutsideCollapse: Boolean = true,   // dynamicSpot Feature: Tap outside expanded island to collapse
    // --- Additional Reverse-Engineered dynamicSpot Features ---
    val allowedPackages: Set<String> = emptySet(), // prefAppSelect: Filter notifications to only allowed apps (empty = all allowed)
    val batteryShowCharging: Boolean = true,     // prefBatSelect: Show island on power connect
    val batteryShowWattage: Boolean = true,      // prefBatSelect: Show fast charging wattage
    val batteryShowLow: Boolean = true,          // prefBatSelect: Show low battery warning (<15%)
    val batteryShowFull: Boolean = true,         // prefBatSelect: Show fully charged (100%)
    val showOnLockscreen: Boolean = true,        // prefPopupLockscreen: Show island on keyguard/lockscreen
    val hideSensitiveOnLockscreen: Boolean = true, // prefIgnoreSensitive: Mask sensitive message contents on lockscreen
    val pillTimeoutSeconds: Int = 4,             // seekSpotTimeout: Compact pill auto-dismiss timeout (0 = never auto-hide)
    val expandedTimeoutSeconds: Int = 4,         // seekPopupTimeoutNew: Expanded card auto-collapse timeout (0 = never collapse)
    val singleTapAction: Int = 1,                // prefPopupInterSingle: 0 = Open App, 1 = Expand Island, 2 = None
    val longPressAction: Int = 0,                // prefPopupInterLong: 0 = Open App, 1 = Expand Island, 2 = Dismiss
    val enableInlineReply: Boolean = true,       // prefActionReply: Quick reply text box in expanded notification card
    val maxNotificationLines: Int = 6,           // seekPopupLines: Maximum text lines for notification body
    val showNotificationImages: Boolean = true,  // prefPopupImages: Show inline picture attachments in expanded card
    val blockNativeHeadsUp: Boolean = false,     // prefBlockPopupNew: Suppress system floating heads-up notifications
    val fastAnimations: Boolean = false,         // prefPopupFast: Fast low-latency spring animations
    val glowChargingColor: Long = 0xFF10B981L,   // prefGlowBatteryColor: Emerald green for charging
    val glowLowBatteryColor: Long = 0xFFEF4444L, // prefGlowBatteryColor: Crimson red for low battery
    val glowFullBatteryColor: Long = 0xFF3B82F6L,// prefGlowBatteryColor: Electric blue for full battery
    val glowNotificationColor: Long = 0xFF8B5CF6L // prefGlowAppColor: Purple accent for notifications
)
