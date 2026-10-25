package com.dynamicisland.app.receiver

import android.app.KeyguardManager
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import com.dynamicisland.app.manager.IslandStateManager
import com.dynamicisland.app.model.BluetoothAlert
import com.dynamicisland.app.model.ChargingAlert
import com.dynamicisland.app.model.RingerAlert
import com.dynamicisland.app.model.RingerState

class HardwareEventReceiver(
    private val stateManager: IslandStateManager,
    private val onScreenLockStateChanged: (isInteractiveAndUnlocked: Boolean) -> Unit
) : BroadcastReceiver() {

    private var lastBatteryLevel = -1
    private var lastChargingState = false

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                handleBatteryChange(context, isPluggedEvent = true)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                handleBatteryChange(context, isUnpluggedEvent = true)
            }
            Intent.ACTION_BATTERY_CHANGED -> {
                handleBatteryChange(context, isPluggedEvent = false)
            }
            AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                handleRingerChange(context)
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                handleBluetoothEvent(intent, isConnected = true)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                handleBluetoothEvent(intent, isConnected = false)
            }
            Intent.ACTION_SCREEN_OFF -> {
                onScreenLockStateChanged(false)
            }
            Intent.ACTION_SCREEN_ON -> {
                val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                val isLocked = km?.isKeyguardLocked ?: false
                onScreenLockStateChanged(!isLocked)
            }
            Intent.ACTION_USER_PRESENT -> {
                onScreenLockStateChanged(true)
            }
        }
    }

    private fun handleBatteryChange(context: Context, isPluggedEvent: Boolean = false, isUnpluggedEvent: Boolean = false) {
        val batteryIntent = try {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: Exception) {
            null
        }
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0

        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 50
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL || plugged > 0

        val isFast = plugged == BatteryManager.BATTERY_PLUGGED_AC && pct < 90

        val config = try {
            com.dynamicisland.app.manager.SettingsRepository.getInstance(context).configFlow.value
        } catch (_: Exception) {
            com.dynamicisland.app.model.CutoutConfig()
        }

        if (isPluggedEvent || (isCharging && !lastChargingState) || (isUnpluggedEvent && lastChargingState)) {
            if (config.batteryShowCharging) {
                stateManager.postTask(
                    ChargingAlert(
                        batteryPercent = pct,
                        isCharging = isCharging,
                        isFastCharging = isFast,
                        wattage = if (config.batteryShowWattage && isFast) "SuperVOOC / Fast Charge" else if (isCharging) "Charging" else "Unplugged"
                    )
                )
            }
        } else if (pct == 100 && lastBatteryLevel in 0..99 && isCharging) {
            if (config.batteryShowFull) {
                stateManager.postTask(
                    ChargingAlert(
                        batteryPercent = 100,
                        isCharging = true,
                        isFullyCharged = true,
                        wattage = "Fully Charged (100%)"
                    )
                )
            }
        } else if (pct in 1..14 && lastBatteryLevel >= 15 && !isCharging) {
            if (config.batteryShowLow) {
                stateManager.postTask(
                    ChargingAlert(
                        batteryPercent = pct,
                        isCharging = false,
                        isLowBattery = true,
                        wattage = "Low Battery ($pct%)"
                    )
                )
            }
        }
        lastBatteryLevel = pct
        lastChargingState = isCharging
    }

    private fun handleRingerChange(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val ringerState = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> RingerState.SILENT
            AudioManager.RINGER_MODE_VIBRATE -> RingerState.VIBRATE
            else -> RingerState.RING
        }
        stateManager.postTask(RingerAlert(state = ringerState))
    }

    private fun handleBluetoothEvent(intent: Intent, isConnected: Boolean) {
        val device: BluetoothDevice? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

        val name = try {
            device?.name ?: "Bluetooth Device"
        } catch (_: SecurityException) {
            "Bluetooth Device"
        }

        stateManager.postTask(
            BluetoothAlert(
                deviceName = name,
                isConnected = isConnected
            )
        )
    }

    companion object {
        fun createIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
        }
    }
}
