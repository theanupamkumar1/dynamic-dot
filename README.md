# Dynamic Dot - Native Dynamic Island for Android

A native, battery-efficient, 24/7 background Dynamic Island for Android, combining the authentic **Apple iOS Dynamic Island** aesthetic with the complete feature set of **dynamicSpot** (all Pro/Premium features 100% unlocked and free), alongside leading OEM innovations (**Honor Magic Capsule**, **Xiaomi HyperOS Dynamic Notch**, and **Oppo/Realme Mini Capsule & Fluid Cloud**).

---

## Key Highlights

1. **True Native 24/7 Experience**:
   - Runs in the background as an overlay window without needing to keep an app open.
   - Automatically starts when your phone boots (`BOOT_COMPLETED`).
2. **Ultra-Low Battery Consumption (< 1% Daily)**:
   - 100% event-driven (`NotificationListenerService`, `MediaSessionManager`, and `BroadcastReceiver`).
   - Zero polling loops.
   - Automatically pauses all visualizer tickers and animations when the screen turns off (`ACTION_SCREEN_OFF`).
3. **Dynamic Touch Hitbox**:
   - When collapsed, the window overlay occupies *only* the pill's bounding box (`FLAG_NOT_FOCUSABLE`). The status bar and other app touches pass through unimpeded.
   - When expanded, the overlay window smoothly expands to accept interactive widget touches.
4. **OLED Pure `#000000` Black & Spring Physics**:
   - Seamlessly blends into the physical camera punch hole.
   - Fluid rubber-band spring animations (`Spring.DampingRatioMediumBouncy`) or optional Snappy Fast Animations.
5. **Authentic Apple iOS Media Experience**:
   - Pink Apple Music / album squircle with glass rim.
   - Live rhythm-reactive animated audio visualizer equalizer bars.
   - Continuous seekbar scrubber with elapsed and countdown remaining timestamps.
   - True iOS playback controls (AirPlay, previous, large circular play/pause, next, and audio routing).

---

## 100% dynamicSpot Feature Parity (All Free & Pro Features Unlocked)

- **App Notification Filter**: Whitelist/blacklist which apps appear on the island with a fast searchable package picker, "Select All", and "Clear All".
- **Comprehensive Battery Events**:
  - Connect to charger alert.
  - USB-PD / SuperVOOC fast charging wattage indicator.
  - Low battery warning (<15%) with crimson halo ring.
  - Battery fully charged (100%) with electric blue halo ring.
- **Lockscreen & AOD Support**:
  - Display island alerts on the lockscreen via `FLAG_SHOW_WHEN_LOCKED`.
  - Sensitive content masking (hide message body while the device is locked).
- **Native Floating Banner Suppression**:
  - Blocks/snoozes native Android heads-up notification popups so they don't double up with the island.
- **Interactive Inline Quick Reply**:
  - Direct `RemoteInput` reply box inside expanded notification cards for messaging apps (WhatsApp, Telegram, Signal, SMS).
- **Gesture Customization**:
  - Single tap action: *Open App*, *Expand Island*, or *None*.
  - Long press action: *Open App*, *Expand Island*, or *Dismiss*.
  - Horizontal swipe to dismiss active tasks.
  - Touch outside expanded card to collapse.
- **Configurable Timeouts**:
  - Independent sliders for Compact Pill Auto-Dismiss (0–15s) and Expanded Card Auto-Collapse (0–15s).
- **Camera Cutout Glow Ring & Halo**:
  - Pulsing illuminated border around the punch hole cutout for notifications, charging, and alerts.
- **Adaptive Artwork Color Tinting**:
  - Automatic HSV saturation & luminance extraction from media artwork and notification icons to tint the visualizer and borders.
- **Apple Dual-Island Split Mode**:
  - Detached auxiliary bubble when two background activities run concurrently.
- **Honor Magic Capsule Tripple-Stack**:
  - Stack up to 3 background tasks with horizontal swipe rotation.

---

## Features Overview

### 1. System Alerts & Hardware Indicators
- **Charging Animation**:
  - Animated lightning bolt, live battery percentage, and wattage display.
  - Pulsing halo glow rings with custom battery event colors.
- **Ringer & Sound Mode Switch**:
  - Ring, Vibrate, and Silent transitions with animated bell and strike-through icons.
- **Bluetooth Device Connection**:
  - Instant pill popup for AirPods, Galaxy Buds, Bluetooth headphones, smartwatches, and car audio.
- **Earbuds Battery Monitor**:
  - Displays L / R earbud battery percentages in the pill and expanded view.
- **Flashlight & Hotspot**:
  - Tethering device counter and flashlight toggle.

### 2. Media & Audio Experience
- **Now Playing Integration**:
  - Works natively with Spotify, YouTube Music, Apple Music, SoundCloud, Audible, podcasts, etc.
  - **Animated Live Audio Equalizer**: Real-time rhythmic audio bars in the compact pill.
  - Interactive expanded player with continuous scrubber, play/pause/prev/next, elapsed/remaining time, and AirPlay.

### 3. Live Activities & Ongoing Tasks
- **Phone & VoIP Calls**:
  - Incoming call pill with caller name and pulsing Accept / Decline buttons (WhatsApp, Telegram, Phone).
  - Ongoing call pill with live duration timer, voice waveform, mute toggle, speakerphone toggle, and hang-up button.
- **Turn-by-Turn Navigation**:
  - Google Maps & Waze integration: Direction arrow icon, distance to next maneuver (`200 m`), street name, and ETA.
- **Timers & Stopwatch**:
  - Real-time countdown timer with pause, resume, +1 min, and cancel controls.

---

## Gesture Guide

| Gesture | Action |
| :--- | :--- |
| **Single Tap** | Configurable: Open App, Expand Island, or None. |
| **Long Press** | Configurable: Open App, Expand Island, or Dismiss. |
| **Swipe Down** | Pull down to expand into the interactive card. |
| **Swipe Up** | Collapse expanded card back into the compact pill. |
| **Swipe Left / Right** | Cycle between stacked apps (Honor style) or dismiss alert. |
| **Tap Outside** | Instantly dismisses expanded card. |

---

## Cutout Calibration Studio

Open the app to access the built-in Calibration Studio:
- **Presets**: Center Hole, Left Hole, Right Hole.
- **Fine-Tuning Sliders**: Horizontal (X) offset, Vertical (Y) margin, Pill Width, Pill Height, Corner Radius, Cutout Gap, and Split Spacing.
- **Interactive Live Simulator**: One-click automated feature demo testing all 16 dynamic island states on your screen.

---

## Building from Source

```bash
# Set your Java 21 JDK
export JAVA_HOME="C:/Program Files/Java/jdk-21"

# Run Unit Tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug
```

---

## License

This project is licensed under the Apache License 2.0.
