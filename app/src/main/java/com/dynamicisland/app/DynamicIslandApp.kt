package com.dynamicisland.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log

class DynamicIslandApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Crash Shield: Prevent any unexpected UI or background thread exception from terminating the app
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("DynamicIslandApp", "Caught unhandled exception in thread ${thread.name}: ${throwable.message}", throwable)
            // Log without terminating the whole app process if possible
            if (thread.name == "main") {
                // Main thread exception - pass to system handler or restart service
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        // 2. Pre-create foreground service notification channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "dynamic_island_core_channel",
                "Dynamic Island Core Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Dynamic Island running 24/7 seamlessly in background"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    companion object {
        lateinit var instance: DynamicIslandApp
            private set
    }
}

