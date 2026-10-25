# Dynamic Island Implementation Checklist

## Phase 1: Project Scaffolding & Core Foundation
- [x] Initialize Android project with Gradle Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`, `libs.versions.toml`) targeting SDK 34/35.
- [x] Configure Jetpack Compose BOM, Material 3, and Icons Extended.
- [x] Setup permissions in `AndroidManifest.xml` (`SYSTEM_ALERT_WINDOW`, `BIND_NOTIFICATION_LISTENER_SERVICE`, `FOREGROUND_SERVICE`, `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`, `BLUETOOTH_CONNECT`).
- [x] Implement `BootReceiver` for seamless start on device reboot.

## Phase 2: Overlay Window & Morphing Engine
- [x] Build `IslandOverlayService` utilizing `WindowManager` with `TYPE_APPLICATION_OVERLAY`.
- [x] Implement Dynamic Touch Hitbox (compact bounds vs expanded bounds to prevent blocking underlying screen interactions).
- [x] Implement Compose-based Island container with spring physics, rubber-band damping, and true `#000000` OLED black blending.
- [x] Implement Gesture Recognition: Tap, Long-press, Swipe Down, Swipe Up, Swipe Left, Swipe Right.

## Phase 3: Hardware & System Event Collectors (Zero-Battery Drain)
- [x] Implement `BatteryChargingReceiver` (plug/unplug animations, percentage, fast charging).
- [x] Implement `RingerModeReceiver` (silent/vibrate/ring toggle animations).
- [x] Implement `BluetoothReceiver` (headphone/watch connection pill with battery stats).
- [x] Implement `ScreenStateReceiver` (`ACTION_SCREEN_OFF` to suspend all animations, `ACTION_SCREEN_ON` to resume).

## Phase 4: Media, Audio & Live Waveforms
- [x] Implement `MediaSessionObserver` to capture Spotify, YouTube Music, and other media players.
- [x] Extract title, artist, playback state, and album art bitmap.
- [x] Implement animated audio visualizer waveform canvas in compact pill.
- [x] Implement Expanded Media Card with seekbar scrubber, play/pause/prev/next, and volume slider.

## Phase 5: Notifications & Live Activities (Calls, Navigation, Timers)
- [x] Implement `IslandNotificationListenerService` (`NLService`) for zero-polling notification event ingestion.
- [x] Support Incoming & Ongoing Phone / VoIP Calls with answer/reject/hangup and duration timer.
- [x] Support Google Maps Turn-by-Turn Navigation (direction arrow, distance, street name).
- [x] Support Timers & Stopwatch with live ticking and interactive pause/reset.
- [x] Support Delivery / Ride tracking progress pill.

## Phase 6: Multitasking, Dual-Island Split & Honor Tripple-Stack
- [x] Build `IslandStateManager` with priority arbitration (Alerts > Calls > Nav > Timers > Media).
- [x] Implement iOS-style Dual-Island Split (Main pill + Detached auxiliary bubble when 2 tasks run).
- [x] Implement Honor Magic Capsule Tripple-Stack with pagination dots and horizontal swipe switching.

## Phase 7: Cutout Calibration Studio & Settings UI
- [x] Implement `DisplayCutout` auto-detection for punch-hole position.
- [x] Build modern Jetpack Compose Settings UI with live interactive calibration preview (X-offset, Y-offset, Width, Height, Radius).
- [x] Battery Optimization guidance & Permission Wizard (Overlay, Notification Access).
- [x] Built and verified debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Phase 8: Live Physical Device Validation (Nothing Phone)
- [x] Connected via ADB Wireless Debugging (`192.168.1.68:35923`).
- [x] Validated overlay window rendering directly above the hardware punch-hole camera.
- [x] Validated Now Playing Media pill with real-time rhythm-reactive green equalizer waveform.
- [x] Validated Full Interactive Expanded Media Card with seekbar scrubber and playback controls.
- [x] Validated Incoming Call pill with caller info and interactive Accept/Reject buttons.
- [x] Validated Google Maps Turn-by-Turn navigation compact pill & expanded card with ETA.
- [x] Validated Apple iOS Dual-Island Split with detached secondary bubble.
- [x] Validated Honor Magic Capsule Tripple-Stack with pagination dots and horizontal swipe switching.
- [x] Validated Charging fluid green edge-glow animation and battery indicator.
- [x] Validated Ambient notch cover seamlessly blending into the status bar on the launcher and inside apps.

## Phase 9: Nothing News Filter, Static Pause Visualizer, App Redirection & Lockscreen Fix
- [x] Filter out ghost Nothing OS News Reporter (`com.nothing.hearthstone` / "News Reporter") when idle or paused so Island remains clean.
- [x] Freeze music rhythm bars completely (`isPlaying == false` static resting heights with 0 pixel motion diff) when paused or not playing.
- [x] Enable tap redirection from expanded island card headers directly to originating applications (YouTube Music, WhatsApp, Phone, Maps, etc.) with Android 14/15 BAL exemption.
- [x] Hide overlay and apply `FLAG_NOT_TOUCHABLE` when screen is off or locked by single pressing power button (`KeyguardManager.isKeyguardLocked`), restoring gracefully upon unlock (`ACTION_USER_PRESENT`).
