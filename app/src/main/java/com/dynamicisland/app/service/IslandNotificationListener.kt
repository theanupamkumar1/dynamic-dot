package com.dynamicisland.app.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.dynamicisland.app.manager.IslandStateManager
import com.dynamicisland.app.model.*
import kotlinx.coroutines.*

class IslandNotificationListener : NotificationListenerService() {

    private val stateManager = IslandStateManager.instance
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaSessionManager: MediaSessionManager? = null
    private var activeMediaController: MediaController? = null
    private var mediaProgressJob: Job? = null
    private var activeCallNotification: StatusBarNotification? = null
    private var activeTimerNotification: StatusBarNotification? = null

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaState()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMediaState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        setupStateCallbacks()
        try {
            initMediaSessionManager()
        } catch (_: Exception) {}
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        initMediaSessionManager()
        try {
            activeNotifications?.forEach { sbn ->
                onNotificationPosted(sbn)
            }
        } catch (e: Exception) {
            Log.w("DynamicIsland", "Error reading active notifications on connect", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        mediaProgressJob?.cancel()
        activeMediaController?.unregisterCallback(mediaCallback)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        val pkg = sbn.packageName

        // Ignore our own notifications
        if (pkg == packageName) return

        val config = try {
            com.dynamicisland.app.manager.SettingsRepository.getInstance(applicationContext).configFlow.value
        } catch (_: Exception) {
            com.dynamicisland.app.model.CutoutConfig()
        }

        // dynamicSpot prefAppSelect: Filter by allowed applications if list is specified
        if (config.allowedPackages.isNotEmpty() && !config.allowedPackages.contains(pkg)) {
            return
        }

        // dynamicSpot prefPopupLockscreen & prefIgnoreLocked: Check device lock state
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
        val isLocked = km?.isKeyguardLocked ?: false
        if (isLocked && !config.showOnLockscreen) {
            return
        }

        // dynamicSpot prefBlockPopupNew: Suppress system floating heads-up notifications
        if (config.blockNativeHeadsUp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                snoozeNotification(sbn.key, 1500)
            } catch (_: Exception) {}
        }

        val category = notification.category
        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        // dynamicSpot prefIgnoreSensitive: Mask sensitive message contents on lockscreen
        val title = if (isLocked && config.hideSensitiveOnLockscreen && rawTitle.isNotEmpty()) "New Notification" else rawTitle
        val text = if (isLocked && config.hideSensitiveOnLockscreen && rawText.isNotEmpty()) "Unlock to view message content" else rawText

        // dynamicSpot prefIgnoreDnd: Ignore non-critical notifications when DND is enabled
        if (config.ignoreDnd) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            val filter = nm?.currentInterruptionFilter ?: android.app.NotificationManager.INTERRUPTION_FILTER_ALL
            if (filter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL &&
                filter != android.app.NotificationManager.INTERRUPTION_FILTER_UNKNOWN
            ) {
                // Ignore general notifications during DND (except call/alarm)
                if (category != Notification.CATEGORY_CALL && category != Notification.CATEGORY_ALARM) {
                    return
                }
            }
        }

        // dynamicSpot prefIgnoreOngoing: Ignore persistent / non-removable notifications
        if (config.ignoreOngoing) {
            val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 || !sbn.isClearable
            if (isOngoing &&
                category != Notification.CATEGORY_CALL &&
                category != Notification.CATEGORY_NAVIGATION &&
                category != Notification.CATEGORY_ALARM &&
                category != Notification.CATEGORY_TRANSPORT
            ) {
                return
            }
        }

        // 0. Live Media Session Notification (YouTube Music, Spotify, Apple Music, Gaana, etc.)
        val mediaSessionToken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(Notification.EXTRA_MEDIA_SESSION) as? MediaSession.Token
        }

        if (mediaSessionToken != null) {
            try {
                val controller = MediaController(this, mediaSessionToken)
                if (isIgnoredNewsController(controller)) {
                    Log.d("DynamicIsland", "Ignored Nothing News notification session token: $pkg")
                    return
                }
                Log.d("DynamicIsland", "Found media session token in notification from: $pkg")
                updateActiveController(controller)
                return
            } catch (e: Exception) {
                Log.w("DynamicIsland", "Failed to create MediaController from token for $pkg", e)
            }
        }

        // Any media / transport notification or session change: refresh media sessions immediately
        if (category == Notification.CATEGORY_TRANSPORT ||
            mediaSessionToken != null ||
            notification.actions?.any { it.title?.toString()?.matches(Regex("(?i)play|pause|next|prev|skip")) == true } == true ||
            pkg.contains("music") || pkg.contains("spotify") || pkg.contains("audio") || pkg.contains("youtube") || pkg.contains("sound")
        ) {
            refreshActiveSessions()
        }

        // 1. Phone / VoIP Call Notification
        if (category == Notification.CATEGORY_CALL || category == "call" ||
            pkg.contains("dialer") || pkg.contains("telecom") || pkg.contains("whatsapp") && text.contains("call", ignoreCase = true)
        ) {
            activeCallNotification = sbn
            val isIncoming = notification.actions?.size ?: 0 >= 2 &&
                    (text.contains("incoming", ignoreCase = true) || title.contains("incoming", ignoreCase = true))
            stateManager.postTask(
                CallTask(
                    id = "call_${sbn.id}",
                    callerName = title.ifEmpty { "Incoming Call" },
                    callerNumber = text,
                    isIncoming = isIncoming,
                    isVoip = pkg.contains("whatsapp") || pkg.contains("telegram"),
                    appName = getAppName(pkg)
                )
            )
            return
        }

        // 2. Navigation Notification (Google Maps, Waze)
        if (category == Notification.CATEGORY_NAVIGATION || pkg == "com.google.android.apps.maps") {
            val direction = detectNavDirection(text, title)
            stateManager.postTask(
                NavigationTask(
                    id = "nav_${sbn.id}",
                    direction = direction,
                    distanceText = parseDistance(text, title),
                    instruction = text.ifEmpty { title },
                    appName = getAppName(pkg)
                )
            )
            return
        }

        // 3. Timer Notification (Google Clock, Samsung Clock)
        if (category == Notification.CATEGORY_ALARM || pkg.contains("clock") || pkg.contains("deskclock")) {
            if (title.contains("timer", ignoreCase = true) || text.contains("timer", ignoreCase = true) || text.contains(":")) {
                activeTimerNotification = sbn
                stateManager.postTask(
                    TimerTask(
                        id = "timer_${sbn.id}",
                        label = title.ifEmpty { "Timer" },
                        remainingSeconds = parseRemainingSeconds(text)
                    )
                )
                return
            }
        }

        // 4. Progress Notification (Downloads, QuickShare, Uber/Swiggy delivery)
        val maxProgress = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val currentProgress = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        if (maxProgress > 0) {
            val pct = (currentProgress * 100) / maxProgress
            stateManager.postTask(
                ProgressTask(
                    id = "progress_${sbn.id}",
                    title = title.ifEmpty { getAppName(pkg) },
                    progressPercent = pct,
                    speedText = text
                )
            )
            return
        }

        // 5. Notifications (Messaging, Chat, Mail, Social)
        val isMessageNotification = category == Notification.CATEGORY_MESSAGE ||
                category == Notification.CATEGORY_EMAIL ||
                category == Notification.CATEGORY_SOCIAL ||
                category == Notification.CATEGORY_REMINDER ||
                category == Notification.CATEGORY_EVENT ||
                pkg.contains("messaging") || pkg.contains("whatsapp") || pkg.contains("telegram") ||
                pkg.contains("signal") || pkg.contains("slack") || pkg.contains("gmail")

        if (isMessageNotification && title.isNotEmpty() && text.isNotEmpty()) {
            var replyIntent: android.app.PendingIntent? = null
            var replyResultKey: String? = null
            var replyLabel: String? = null
            val actionItems = mutableListOf<NotificationActionItem>()

            notification.actions?.forEach { action ->
                var hasRemoteInput = false
                action.remoteInputs?.forEach { ri ->
                    if (ri.resultKey != null) {
                        replyIntent = action.actionIntent
                        replyResultKey = ri.resultKey
                        replyLabel = ri.label?.toString()
                        hasRemoteInput = true
                    }
                }
                if (!hasRemoteInput && action.title != null) {
                    actionItems.add(NotificationActionItem(action.title.toString(), action.actionIntent))
                }
            }

            val largeIcon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_LARGE_ICON, Bitmap::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_LARGE_ICON) as? Bitmap
            }

            val bigPicture = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_PICTURE, Bitmap::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_PICTURE) as? Bitmap
            }

            val timeoutSec = if (config.pillTimeoutSeconds > 0) config.pillTimeoutSeconds.toLong() else null

            val task = NotificationTask(
                id = "msg_${sbn.id}",
                title = title,
                message = text,
                packageName = pkg,
                appName = getAppName(pkg),
                largeIcon = largeIcon,
                bigPicture = bigPicture,
                contentIntent = notification.contentIntent,
                replyActionIntent = replyIntent,
                replyResultKey = replyResultKey,
                replyLabel = replyLabel,
                actions = actionItems,
                autoDismissSeconds = timeoutSec
            )
            stateManager.postTask(task)
            if (config.autoExpandNotifications) {
                stateManager.expandIsland(autoCollapseSeconds = if (config.expandedTimeoutSeconds > 0) config.expandedTimeoutSeconds else null)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        val id = sbn.id
        if (sbn.id == activeCallNotification?.id) {
            activeCallNotification = null
        }
        if (sbn.id == activeTimerNotification?.id) {
            activeTimerNotification = null
        }
        stateManager.removeTask("call_$id")
        stateManager.removeTask("nav_$id")
        stateManager.removeTask("timer_$id")
        stateManager.removeTask("progress_$id")
        stateManager.removeTask("msg_$id")
    }

    private fun initMediaSessionManager() {
        try {
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val component = ComponentName(this, IslandNotificationListener::class.java)
            mediaSessionManager?.addOnActiveSessionsChangedListener({ controllers ->
                Log.d("DynamicIsland", "Active media sessions changed: count = ${controllers?.size}")
                updateActiveController(findBestController(controllers))
            }, component)

            // Initial controller check
            val controllers = mediaSessionManager?.getActiveSessions(component)
            Log.d("DynamicIsland", "Initial media controllers found: count = ${controllers?.size}")
            updateActiveController(findBestController(controllers))
        } catch (e: SecurityException) {
            Log.w("DynamicIsland", "SecurityException initializing MediaSessionManager", e)
        }
    }

    private fun isNothingNews(controller: MediaController?): Boolean {
        if (controller == null) return false
        val pkg = controller.packageName?.lowercase() ?: ""
        val title = controller.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.lowercase() ?: ""
        return pkg.contains("hearthstone") || pkg.contains("nothing.news") || title.contains("news reporter")
    }

    private fun isIgnoredNewsController(controller: MediaController?): Boolean {
        if (controller == null) return false
        if (!isNothingNews(controller)) return false
        val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
        // Nothing News should ONLY be shown if actively playing.
        // When paused/stopped/idle, ignore it completely so island is idle!
        return !isPlaying
    }

    private fun findBestController(controllers: List<MediaController>?): MediaController? {
        if (controllers.isNullOrEmpty()) return null

        // Filter out Nothing News if not actively playing
        val validControllers = controllers.filterNot { isIgnoredNewsController(it) }
        if (validControllers.isEmpty()) return null

        // 1. Prioritize actively playing controller
        val playing = validControllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        if (playing != null) {
            Log.d("DynamicIsland", "Selected actively PLAYING media controller: ${playing.packageName}")
            return playing
        }

        // 2. Controller with metadata and non-zero duration (paused or buffering)
        val withMeta = validControllers.firstOrNull {
            val s = it.playbackState?.state
            (s == PlaybackState.STATE_PAUSED || s == PlaybackState.STATE_BUFFERING) && it.metadata != null
        }
        if (withMeta != null) {
            Log.d("DynamicIsland", "Selected PAUSED controller with metadata: ${withMeta.packageName}")
            return withMeta
        }

        // 3. Any controller with metadata
        return validControllers.firstOrNull { it.metadata != null } ?: validControllers.firstOrNull()
    }

    private fun refreshActiveSessions() {
        try {
            val component = ComponentName(this, IslandNotificationListener::class.java)
            val controllers = mediaSessionManager?.getActiveSessions(component)
            val best = findBestController(controllers)
            if (best != null) {
                updateActiveController(best)
            } else {
                // No active or valid media session
                if (activeMediaController != null) {
                    activeMediaController?.unregisterCallback(mediaCallback)
                    activeMediaController = null
                    stateManager.removeTask("media_task")
                    mediaProgressJob?.cancel()
                }
            }
        } catch (e: Exception) {
            Log.w("DynamicIsland", "Could not refresh active media sessions", e)
        }
    }

    private fun updateActiveController(controller: MediaController?) {
        if (controller == null || isIgnoredNewsController(controller)) return
        if (activeMediaController?.sessionToken == controller.sessionToken) {
            updateMediaState()
            return
        }
        activeMediaController?.unregisterCallback(mediaCallback)
        activeMediaController = controller
        activeMediaController?.registerCallback(mediaCallback)
        Log.i("DynamicIsland", "Bound active MediaController: ${controller.packageName}")
        updateMediaState()
    }

    private fun updateMediaState() {
        val controller = activeMediaController
        if (controller == null || isIgnoredNewsController(controller)) {
            stateManager.removeTask("media_task")
            mediaProgressJob?.cancel()
            if (controller != null && isIgnoredNewsController(controller)) {
                activeMediaController?.unregisterCallback(mediaCallback)
                activeMediaController = null
                refreshActiveSessions()
            }
            return
        }

        val metadata = controller.metadata
        val playbackState = controller.playbackState
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        val state = playbackState?.state ?: PlaybackState.STATE_NONE

        if ((state == PlaybackState.STATE_NONE || state == PlaybackState.STATE_STOPPED) && !isPlaying) {
            stateManager.removeTask("media_task")
            mediaProgressJob?.cancel()
            return
        }

        // Fallback extraction from notification if metadata is not yet populated
        var fallbackTitle: String? = null
        var fallbackArtist: String? = null
        var fallbackBitmap: Bitmap? = null
        if (metadata == null) {
            try {
                val matchingSbn = activeNotifications?.firstOrNull { it.packageName == controller.packageName }
                matchingSbn?.notification?.let { notif ->
                    fallbackTitle = notif.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                    fallbackArtist = notif.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                    fallbackBitmap = notif.getLargeIcon()?.loadDrawable(this)?.let { drawable ->
                        if (drawable is android.graphics.drawable.BitmapDrawable) drawable.bitmap else null
                    }
                }
            } catch (_: Exception) {}
        }

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: fallbackTitle
            ?: "Now Playing"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: fallbackArtist
            ?: "Playing"
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = playbackState?.position ?: 0L
        val artBitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            ?: fallbackBitmap

        val accentColor = extractAccentColor(artBitmap)

        val task = MediaTask(
            packageName = controller.packageName ?: "",
            title = title,
            artist = artist,
            isPlaying = isPlaying,
            playbackPositionMs = position,
            trackDurationMs = duration,
            albumArt = artBitmap,
            accentColor = accentColor
        )
        stateManager.postTask(task)

        // Keep position updated while playing
        if (isPlaying) {
            startMediaProgressTracking(controller)
        } else {
            mediaProgressJob?.cancel()
        }
    }

    private fun extractAccentColor(bitmap: Bitmap?): Long? {
        if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) return null
        return try {
            val w = bitmap.width
            val h = bitmap.height
            val samples = intArrayOf(
                bitmap.getPixel(w / 2, h / 2),
                bitmap.getPixel(w / 4, h / 4),
                bitmap.getPixel((w * 3) / 4, h / 4),
                bitmap.getPixel(w / 4, (h * 3) / 4),
                bitmap.getPixel((w * 3) / 4, (h * 3) / 4)
            )
            var bestColor = samples[0]
            var maxChroma = -1f
            val hsv = FloatArray(3)
            for (color in samples) {
                android.graphics.Color.colorToHSV(color, hsv)
                val saturation = hsv[1]
                val value = hsv[2]
                if (value in 0.20f..0.98f && saturation > maxChroma) {
                    maxChroma = saturation
                    bestColor = color
                }
            }
            (bestColor.toLong() and 0xFFFFFFFFL) or 0xFF000000L
        } catch (_: Exception) {
            null
        }
    }

    private fun startMediaProgressTracking(controller: MediaController) {
        mediaProgressJob?.cancel()
        mediaProgressJob = scope.launch {
            while (isActive) {
                delay(1000)
                val current = stateManager.primaryTask.value as? MediaTask ?: break
                if (!current.isPlaying) break
                val pos = controller.playbackState?.position ?: (current.playbackPositionMs + 1000)
                stateManager.postTask(current.copy(playbackPositionMs = pos))
            }
        }
    }

    private fun setupStateCallbacks() {
        stateManager.onMediaPlayPauseToggle = {
            activeMediaController?.let { controller ->
                val state = controller.playbackState?.state
                if (state == PlaybackState.STATE_PLAYING) {
                    controller.transportControls?.pause()
                } else {
                    controller.transportControls?.play()
                }
            }
        }

        stateManager.onMediaNext = {
            activeMediaController?.transportControls?.skipToNext()
        }

        stateManager.onMediaPrevious = {
            activeMediaController?.transportControls?.skipToPrevious()
        }

        stateManager.onMediaSeek = { positionMs ->
            activeMediaController?.transportControls?.seekTo(positionMs)
        }

        stateManager.onMediaStop = {
            activeMediaController?.let { controller ->
                try {
                    controller.transportControls?.stop()
                    controller.transportControls?.pause()
                } catch (_: Exception) {}
            }
            mediaProgressJob?.cancel()
        }

        stateManager.onCallHangup = {
            activeCallNotification?.let { sbn ->
                val actions = sbn.notification?.actions
                val declineAction = actions?.firstOrNull { action ->
                    val title = action.title?.toString()?.lowercase() ?: ""
                    title.contains("decline") || title.contains("hang") || title.contains("reject") || title.contains("end")
                } ?: actions?.firstOrNull()
                try {
                    declineAction?.actionIntent?.send()
                } catch (_: Exception) {}
                try {
                    cancelNotification(sbn.key)
                } catch (_: Exception) {}
            }
        }

        stateManager.onCallAnswer = {
            activeCallNotification?.let { sbn ->
                val actions = sbn.notification?.actions
                val answerAction = actions?.firstOrNull { action ->
                    val title = action.title?.toString()?.lowercase() ?: ""
                    title.contains("answer") || title.contains("accept")
                }
                try {
                    answerAction?.actionIntent?.send()
                } catch (_: Exception) {}
            }
        }

        stateManager.onTimerCancel = {
            activeTimerNotification?.let { sbn ->
                val actions = sbn.notification?.actions
                val cancelAction = actions?.firstOrNull { action ->
                    val title = action.title?.toString()?.lowercase() ?: ""
                    title.contains("cancel") || title.contains("stop") || title.contains("dismiss") || title.contains("delete")
                }
                try {
                    cancelAction?.actionIntent?.send()
                } catch (_: Exception) {}
                try {
                    cancelNotification(sbn.key)
                } catch (_: Exception) {}
            }
        }

        stateManager.onAppLaunchRequest = { pkg ->
            Log.d("DynamicIsland", "onAppLaunchRequest: $pkg")
            try {
                val optionsBundle = if (Build.VERSION.SDK_INT >= 34) {
                    android.app.ActivityOptions.makeBasic().apply {
                        setPendingIntentBackgroundActivityStartMode(android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                    }.toBundle()
                } else null

                // If active media controller matches this package and has sessionActivity, use it!
                val sessionIntent = if (activeMediaController != null && (pkg.isEmpty() || activeMediaController?.packageName == pkg)) {
                    activeMediaController?.sessionActivity
                } else null

                if (sessionIntent != null) {
                    try {
                        if (optionsBundle != null) {
                            sessionIntent.send(this, 0, null, null, null, null, optionsBundle)
                        } else {
                            sessionIntent.send()
                        }
                        Log.d("DynamicIsland", "Launched app via sessionActivity PendingIntent")
                    } catch (e: Exception) {
                        launchAppPackage(pkg)
                    }
                } else {
                    launchAppPackage(pkg)
                }
            } catch (e: Exception) {
                Log.e("DynamicIsland", "Failed to launch app for pkg: $pkg", e)
            }
        }
    }

    private fun launchAppPackage(pkg: String) {
        val optionsBundle = if (Build.VERSION.SDK_INT >= 34) {
            android.app.ActivityOptions.makeBasic().apply {
                setPendingIntentBackgroundActivityStartMode(android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            }.toBundle()
        } else null

        if (pkg.isNotEmpty()) {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                startActivity(launchIntent, optionsBundle)
                return
            }
        }
        try {
            val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(dialIntent, optionsBundle)
        } catch (_: Exception) {}
    }

    private fun getAppName(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    private fun detectNavDirection(text: String, title: String): NavDirection {
        val combined = "$text $title".lowercase()
        return when {
            combined.contains("slight left") -> NavDirection.TURN_SLIGHT_LEFT
            combined.contains("slight right") -> NavDirection.TURN_SLIGHT_RIGHT
            combined.contains("left") -> NavDirection.TURN_LEFT
            combined.contains("right") -> NavDirection.TURN_RIGHT
            combined.contains("roundabout") -> NavDirection.ROUNDABOUT
            combined.contains("u-turn") -> NavDirection.U_TURN
            combined.contains("arrive") || combined.contains("destination") -> NavDirection.DESTINATION
            else -> NavDirection.STRAIGHT
        }
    }

    private fun parseDistance(text: String, title: String): String {
        val regex = Regex("""\b(\d+(\.\d+)?\s*(m|km|ft|mi))\b""", RegexOption.IGNORE_CASE)
        val match = regex.find("$title $text")
        return match?.value ?: "200 m"
    }

    private fun parseRemainingSeconds(text: String): Long {
        val parts = text.split(":")
        return when (parts.size) {
            2 -> {
                val min = parts[0].trim().toLongOrNull() ?: 0L
                val sec = parts[1].trim().toLongOrNull() ?: 0L
                min * 60 + sec
            }
            3 -> {
                val hr = parts[0].trim().toLongOrNull() ?: 0L
                val min = parts[1].trim().toLongOrNull() ?: 0L
                val sec = parts[2].trim().toLongOrNull() ?: 0L
                hr * 3600 + min * 60 + sec
            }
            else -> 120L
        }
    }
}
