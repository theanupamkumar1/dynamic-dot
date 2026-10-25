package com.dynamicisland.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dynamicisland.app.manager.IslandStateManager
import com.dynamicisland.app.manager.SettingsRepository
import com.dynamicisland.app.service.IslandOverlayService
import com.dynamicisland.app.ui.settings.SettingsScreen

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepo: SettingsRepository
    private val stateManager = IslandStateManager.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepo = SettingsRepository.getInstance(this)

        if (settingsRepo.isServiceActiveFlow.value && Settings.canDrawOverlays(this)) {
            startOverlayService()
        }

        setContent {
            SettingsScreen(
                settingsRepo = settingsRepo,
                stateManager = stateManager,
                onStartService = { startOverlayService() },
                onStopService = { stopOverlayService() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val component = android.content.ComponentName(this, com.dynamicisland.app.service.IslandNotificationListener::class.java)
                android.service.notification.NotificationListenerService.requestRebind(component)
            }
        } catch (_: Exception) {}

        if (settingsRepo.isServiceActiveFlow.value && Settings.canDrawOverlays(this)) {
            startOverlayService()
        }
    }

    override fun onPause() {
        super.onPause()
        stateManager.removeTask("calibration_task")
        stateManager.removeTask("secondary_calibration_task")
    }

    private fun startOverlayService() {
        if (!Settings.canDrawOverlays(this)) return

        val intent = Intent(this, IslandOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopOverlayService() {
        val intent = Intent(this, IslandOverlayService::class.java)
        stopService(intent)
    }
}
