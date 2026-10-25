package com.dynamicisland.app.manager

import android.content.Context
import android.content.SharedPreferences
import com.dynamicisland.app.model.CustomPreset
import com.dynamicisland.app.model.CutoutConfig
import com.dynamicisland.app.model.CutoutPosition
import com.dynamicisland.app.model.IslandTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<CutoutConfig> = _configFlow.asStateFlow()

    private val _isServiceActiveFlow = MutableStateFlow(prefs.getBoolean(KEY_SERVICE_ACTIVE, true))
    val isServiceActiveFlow: StateFlow<Boolean> = _isServiceActiveFlow.asStateFlow()

    private val _savedPresetsFlow = MutableStateFlow(loadSavedPresets())
    val savedPresetsFlow: StateFlow<List<CustomPreset>> = _savedPresetsFlow.asStateFlow()

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        _configFlow.value = loadConfig()
        _isServiceActiveFlow.value = prefs.getBoolean(KEY_SERVICE_ACTIVE, true)
        if (key == KEY_SAVED_PRESETS) {
            _savedPresetsFlow.value = loadSavedPresets()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        // Seed an initial preset if empty so user has an immediate example
        if (_savedPresetsFlow.value.isEmpty()) {
            val defaultPreset = CustomPreset(
                id = "default_preset_1",
                name = "My Calibrated Notch",
                offsetX = _configFlow.value.offsetX,
                offsetY = _configFlow.value.offsetY,
                pillWidth = _configFlow.value.pillWidth,
                pillHeight = _configFlow.value.pillHeight,
                pillRadius = _configFlow.value.pillRadius,
                cutoutGap = _configFlow.value.cutoutGap,
                splitSpacing = _configFlow.value.splitSpacing
            )
            savePresetsList(listOf(defaultPreset))
        }
    }

    fun updateConfig(config: CutoutConfig) {
        prefs.edit().apply {
            putString(KEY_POSITION, config.position.name)
            putFloat(KEY_OFFSET_X, config.offsetX)
            putFloat(KEY_OFFSET_Y, config.offsetY)
            putFloat(KEY_PILL_WIDTH, config.pillWidth)
            putFloat(KEY_PILL_HEIGHT, config.pillHeight)
            putFloat(KEY_PILL_RADIUS, config.pillRadius)
            putFloat(KEY_CUTOUT_GAP, config.cutoutGap)
            putFloat(KEY_SPLIT_SPACING, config.splitSpacing)
            putBoolean(KEY_HAPTICS, config.enableHaptics)
            putBoolean(KEY_AUTO_SPLIT, config.enableAutoSplit)
            putBoolean(KEY_TRIPPLE_STACK, config.enableTrippleStack)
            putBoolean(KEY_SWIPE_DISMISS, config.enableSwipeDismiss)
            putBoolean(KEY_LIQUID_GLOW, config.enableLiquidGlow)
            putBoolean(KEY_ALWAYS_AMBIENT, config.alwaysShowAmbientPill)
            putString(KEY_THEME, config.theme.name)
            putString(KEY_ACTIVE_PRESET_NAME, config.activePresetName)
            putBoolean(KEY_HIDE_IN_FOREGROUND, config.hideInForeground)
            putBoolean(KEY_ENABLE_GLOW_RING, config.enableGlowRing)
            putBoolean(KEY_COLORIZE_ARTWORK, config.colorizeFromArtwork)
            putBoolean(KEY_IGNORE_DND, config.ignoreDnd)
            putBoolean(KEY_IGNORE_ONGOING, config.ignoreOngoing)
            putBoolean(KEY_AUTO_EXPAND_NOTIFICATIONS, config.autoExpandNotifications)
            putBoolean(KEY_TOUCH_OUTSIDE_COLLAPSE, config.touchOutsideCollapse)
            putStringSet(KEY_ALLOWED_PACKAGES, config.allowedPackages)
            putBoolean(KEY_BATTERY_CHARGING, config.batteryShowCharging)
            putBoolean(KEY_BATTERY_WATTAGE, config.batteryShowWattage)
            putBoolean(KEY_BATTERY_LOW, config.batteryShowLow)
            putBoolean(KEY_BATTERY_FULL, config.batteryShowFull)
            putBoolean(KEY_SHOW_ON_LOCKSCREEN, config.showOnLockscreen)
            putBoolean(KEY_HIDE_SENSITIVE_LOCKSCREEN, config.hideSensitiveOnLockscreen)
            putInt(KEY_PILL_TIMEOUT_SECONDS, config.pillTimeoutSeconds)
            putInt(KEY_EXPANDED_TIMEOUT_SECONDS, config.expandedTimeoutSeconds)
            putInt(KEY_SINGLE_TAP_ACTION, config.singleTapAction)
            putInt(KEY_LONG_PRESS_ACTION, config.longPressAction)
            putBoolean(KEY_ENABLE_INLINE_REPLY, config.enableInlineReply)
            putInt(KEY_MAX_NOTIFICATION_LINES, config.maxNotificationLines)
            putBoolean(KEY_SHOW_NOTIFICATION_IMAGES, config.showNotificationImages)
            putBoolean(KEY_BLOCK_NATIVE_HEADS_UP, config.blockNativeHeadsUp)
            putBoolean(KEY_FAST_ANIMATIONS, config.fastAnimations)
            putLong(KEY_GLOW_CHARGING_COLOR, config.glowChargingColor)
            putLong(KEY_GLOW_LOW_BATTERY_COLOR, config.glowLowBatteryColor)
            putLong(KEY_GLOW_FULL_BATTERY_COLOR, config.glowFullBatteryColor)
            putLong(KEY_GLOW_NOTIFICATION_COLOR, config.glowNotificationColor)
            apply()
        }
        _configFlow.value = config
    }

    fun toggleAllowedPackage(pkg: String) {
        val current = _configFlow.value.allowedPackages.toMutableSet()
        if (current.contains(pkg)) {
            current.remove(pkg)
        } else {
            current.add(pkg)
        }
        updateConfig(_configFlow.value.copy(allowedPackages = current))
    }

    fun setAllowedPackages(packages: Set<String>) {
        updateConfig(_configFlow.value.copy(allowedPackages = packages))
    }

    fun setTheme(theme: IslandTheme) {
        val updated = _configFlow.value.copy(theme = theme)
        updateConfig(updated)
    }

    fun saveCurrentAsCustomPreset(name: String): CustomPreset {
        val current = _configFlow.value
        val newPreset = CustomPreset(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Custom Preset ${_savedPresetsFlow.value.size + 1}" },
            offsetX = current.offsetX,
            offsetY = current.offsetY,
            pillWidth = current.pillWidth,
            pillHeight = current.pillHeight,
            pillRadius = current.pillRadius,
            cutoutGap = current.cutoutGap,
            splitSpacing = current.splitSpacing
        )
        val currentList = _savedPresetsFlow.value.toMutableList()
        currentList.add(newPreset)
        savePresetsList(currentList)
        updateConfig(current.copy(activePresetName = newPreset.name))
        return newPreset
    }

    fun applyCustomPreset(preset: CustomPreset) {
        val current = _configFlow.value
        val updated = current.copy(
            position = CutoutPosition.CUSTOM,
            offsetX = preset.offsetX,
            offsetY = preset.offsetY,
            pillWidth = preset.pillWidth,
            pillHeight = preset.pillHeight,
            pillRadius = preset.pillRadius,
            cutoutGap = preset.cutoutGap,
            splitSpacing = preset.splitSpacing,
            activePresetName = preset.name
        )
        updateConfig(updated)
    }

    fun deleteCustomPreset(presetId: String) {
        val currentList = _savedPresetsFlow.value.toMutableList()
        currentList.removeAll { it.id == presetId }
        savePresetsList(currentList)
    }

    private fun savePresetsList(presets: List<CustomPreset>) {
        val array = JSONArray()
        for (p in presets) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("offsetX", p.offsetX.toDouble())
                put("offsetY", p.offsetY.toDouble())
                put("pillWidth", p.pillWidth.toDouble())
                put("pillHeight", p.pillHeight.toDouble())
                put("pillRadius", p.pillRadius.toDouble())
                put("cutoutGap", p.cutoutGap.toDouble())
                put("splitSpacing", p.splitSpacing.toDouble())
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_SAVED_PRESETS, array.toString()).apply()
        _savedPresetsFlow.value = presets
    }

    private fun loadSavedPresets(): List<CustomPreset> {
        val raw = prefs.getString(KEY_SAVED_PRESETS, null) ?: return emptyList()
        val list = mutableListOf<CustomPreset>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CustomPreset(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        offsetX = obj.getDouble("offsetX").toFloat(),
                        offsetY = obj.getDouble("offsetY").toFloat(),
                        pillWidth = obj.getDouble("pillWidth").toFloat(),
                        pillHeight = obj.getDouble("pillHeight").toFloat(),
                        pillRadius = obj.getDouble("pillRadius").toFloat(),
                        cutoutGap = obj.getDouble("cutoutGap").toFloat(),
                        splitSpacing = obj.getDouble("splitSpacing").toFloat()
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun setServiceActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_ACTIVE, active).apply()
        _isServiceActiveFlow.value = active
    }

    fun resetToDefaults() {
        val defaultConfig = CutoutConfig()
        updateConfig(defaultConfig)
    }

    fun applyPreset(position: CutoutPosition) {
        val current = _configFlow.value
        val updated = when (position) {
            CutoutPosition.CENTER_PUNCH_HOLE -> current.copy(
                position = position,
                offsetX = 0f,
                offsetY = 9f,
                pillWidth = 108f,
                pillHeight = 34f,
                pillRadius = 17f,
                cutoutGap = 32f,
                activePresetName = "Center Hole"
            )
            CutoutPosition.LEFT_PUNCH_HOLE -> current.copy(
                position = position,
                offsetX = -120f,
                offsetY = 9f,
                pillWidth = 108f,
                pillHeight = 34f,
                pillRadius = 17f,
                cutoutGap = 32f,
                activePresetName = "Left Hole"
            )
            CutoutPosition.RIGHT_PUNCH_HOLE -> current.copy(
                position = position,
                offsetX = 120f,
                offsetY = 9f,
                pillWidth = 108f,
                pillHeight = 34f,
                pillRadius = 17f,
                cutoutGap = 32f,
                activePresetName = "Right Hole"
            )
            CutoutPosition.PILL_CUTOUT -> current.copy(
                position = position,
                offsetX = 0f,
                offsetY = 9f,
                pillWidth = 135f,
                pillHeight = 36f,
                pillRadius = 18f,
                cutoutGap = 50f,
                activePresetName = "Pill Cutout"
            )
            CutoutPosition.CUSTOM -> current.copy(position = position)
        }
        updateConfig(updated)
    }

    private fun loadConfig(): CutoutConfig {
        val posName = prefs.getString(KEY_POSITION, CutoutPosition.CENTER_PUNCH_HOLE.name)
        val pos = try {
            CutoutPosition.valueOf(posName ?: CutoutPosition.CENTER_PUNCH_HOLE.name)
        } catch (_: Exception) {
            CutoutPosition.CENTER_PUNCH_HOLE
        }

        val themeName = prefs.getString(KEY_THEME, IslandTheme.REGULAR.name)
        val theme = try {
            IslandTheme.valueOf(themeName ?: IslandTheme.REGULAR.name)
        } catch (_: Exception) {
            IslandTheme.REGULAR
        }

        return CutoutConfig(
            position = pos,
            offsetX = prefs.getFloat(KEY_OFFSET_X, 0f),
            offsetY = prefs.getFloat(KEY_OFFSET_Y, 9f),
            pillWidth = prefs.getFloat(KEY_PILL_WIDTH, 108f),
            pillHeight = prefs.getFloat(KEY_PILL_HEIGHT, 34f),
            pillRadius = prefs.getFloat(KEY_PILL_RADIUS, 17f),
            cutoutGap = prefs.getFloat(KEY_CUTOUT_GAP, 32f),
            splitSpacing = prefs.getFloat(KEY_SPLIT_SPACING, 10f),
            enableHaptics = prefs.getBoolean(KEY_HAPTICS, true),
            enableAutoSplit = prefs.getBoolean(KEY_AUTO_SPLIT, true),
            enableTrippleStack = prefs.getBoolean(KEY_TRIPPLE_STACK, true),
            enableSwipeDismiss = prefs.getBoolean(KEY_SWIPE_DISMISS, true),
            enableLiquidGlow = prefs.getBoolean(KEY_LIQUID_GLOW, true),
            alwaysShowAmbientPill = prefs.getBoolean(KEY_ALWAYS_AMBIENT, true),
            theme = theme,
            activePresetName = prefs.getString(KEY_ACTIVE_PRESET_NAME, null),
            hideInForeground = prefs.getBoolean(KEY_HIDE_IN_FOREGROUND, true),
            enableGlowRing = prefs.getBoolean(KEY_ENABLE_GLOW_RING, true),
            colorizeFromArtwork = prefs.getBoolean(KEY_COLORIZE_ARTWORK, true),
            ignoreDnd = prefs.getBoolean(KEY_IGNORE_DND, true),
            ignoreOngoing = prefs.getBoolean(KEY_IGNORE_ONGOING, true),
            autoExpandNotifications = prefs.getBoolean(KEY_AUTO_EXPAND_NOTIFICATIONS, false),
            touchOutsideCollapse = prefs.getBoolean(KEY_TOUCH_OUTSIDE_COLLAPSE, true),
            allowedPackages = prefs.getStringSet(KEY_ALLOWED_PACKAGES, emptySet()) ?: emptySet(),
            batteryShowCharging = prefs.getBoolean(KEY_BATTERY_CHARGING, true),
            batteryShowWattage = prefs.getBoolean(KEY_BATTERY_WATTAGE, true),
            batteryShowLow = prefs.getBoolean(KEY_BATTERY_LOW, true),
            batteryShowFull = prefs.getBoolean(KEY_BATTERY_FULL, true),
            showOnLockscreen = prefs.getBoolean(KEY_SHOW_ON_LOCKSCREEN, true),
            hideSensitiveOnLockscreen = prefs.getBoolean(KEY_HIDE_SENSITIVE_LOCKSCREEN, true),
            pillTimeoutSeconds = prefs.getInt(KEY_PILL_TIMEOUT_SECONDS, 4),
            expandedTimeoutSeconds = prefs.getInt(KEY_EXPANDED_TIMEOUT_SECONDS, 4),
            singleTapAction = prefs.getInt(KEY_SINGLE_TAP_ACTION, 1),
            longPressAction = prefs.getInt(KEY_LONG_PRESS_ACTION, 0),
            enableInlineReply = prefs.getBoolean(KEY_ENABLE_INLINE_REPLY, true),
            maxNotificationLines = prefs.getInt(KEY_MAX_NOTIFICATION_LINES, 6),
            showNotificationImages = prefs.getBoolean(KEY_SHOW_NOTIFICATION_IMAGES, true),
            blockNativeHeadsUp = prefs.getBoolean(KEY_BLOCK_NATIVE_HEADS_UP, false),
            fastAnimations = prefs.getBoolean(KEY_FAST_ANIMATIONS, false),
            glowChargingColor = prefs.getLong(KEY_GLOW_CHARGING_COLOR, 0xFF10B981L),
            glowLowBatteryColor = prefs.getLong(KEY_GLOW_LOW_BATTERY_COLOR, 0xFFEF4444L),
            glowFullBatteryColor = prefs.getLong(KEY_GLOW_FULL_BATTERY_COLOR, 0xFF3B82F6L),
            glowNotificationColor = prefs.getLong(KEY_GLOW_NOTIFICATION_COLOR, 0xFF8B5CF6L)
        )
    }

    companion object {
        private const val PREFS_NAME = "dynamic_island_settings"
        private const val KEY_SERVICE_ACTIVE = "service_active"
        private const val KEY_POSITION = "position"
        private const val KEY_OFFSET_X = "offset_x"
        private const val KEY_OFFSET_Y = "offset_y"
        private const val KEY_PILL_WIDTH = "pill_width"
        private const val KEY_PILL_HEIGHT = "pill_height"
        private const val KEY_PILL_RADIUS = "pill_radius"
        private const val KEY_CUTOUT_GAP = "cutout_gap"
        private const val KEY_SPLIT_SPACING = "split_spacing"
        private const val KEY_HAPTICS = "enable_haptics"
        private const val KEY_AUTO_SPLIT = "enable_auto_split"
        private const val KEY_TRIPPLE_STACK = "enable_tripple_stack"
        private const val KEY_SWIPE_DISMISS = "enable_swipe_dismiss"
        private const val KEY_LIQUID_GLOW = "enable_liquid_glow"
        private const val KEY_ALWAYS_AMBIENT = "always_show_ambient"
        private const val KEY_THEME = "island_theme"
        private const val KEY_SAVED_PRESETS = "saved_custom_presets"
        private const val KEY_ACTIVE_PRESET_NAME = "active_preset_name"
        private const val KEY_HIDE_IN_FOREGROUND = "hide_in_foreground"
        private const val KEY_ENABLE_GLOW_RING = "enable_glow_ring"
        private const val KEY_COLORIZE_ARTWORK = "colorize_artwork"
        private const val KEY_IGNORE_DND = "ignore_dnd"
        private const val KEY_IGNORE_ONGOING = "ignore_ongoing"
        private const val KEY_AUTO_EXPAND_NOTIFICATIONS = "auto_expand_notifications"
        private const val KEY_TOUCH_OUTSIDE_COLLAPSE = "touch_outside_collapse"
        private const val KEY_ALLOWED_PACKAGES = "allowed_packages"
        private const val KEY_BATTERY_CHARGING = "battery_show_charging"
        private const val KEY_BATTERY_WATTAGE = "battery_show_wattage"
        private const val KEY_BATTERY_LOW = "battery_show_low"
        private const val KEY_BATTERY_FULL = "battery_show_full"
        private const val KEY_SHOW_ON_LOCKSCREEN = "show_on_lockscreen"
        private const val KEY_HIDE_SENSITIVE_LOCKSCREEN = "hide_sensitive_lockscreen"
        private const val KEY_PILL_TIMEOUT_SECONDS = "pill_timeout_seconds"
        private const val KEY_EXPANDED_TIMEOUT_SECONDS = "expanded_timeout_seconds"
        private const val KEY_SINGLE_TAP_ACTION = "single_tap_action"
        private const val KEY_LONG_PRESS_ACTION = "long_press_action"
        private const val KEY_ENABLE_INLINE_REPLY = "enable_inline_reply"
        private const val KEY_MAX_NOTIFICATION_LINES = "max_notification_lines"
        private const val KEY_SHOW_NOTIFICATION_IMAGES = "show_notification_images"
        private const val KEY_BLOCK_NATIVE_HEADS_UP = "block_native_heads_up"
        private const val KEY_FAST_ANIMATIONS = "fast_animations"
        private const val KEY_GLOW_CHARGING_COLOR = "glow_charging_color"
        private const val KEY_GLOW_LOW_BATTERY_COLOR = "glow_low_battery_color"
        private const val KEY_GLOW_FULL_BATTERY_COLOR = "glow_full_battery_color"
        private const val KEY_GLOW_NOTIFICATION_COLOR = "glow_notification_color"

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
