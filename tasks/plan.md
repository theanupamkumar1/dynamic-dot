# Dynamic Island Android - Architecture & Feature Specification

## 1. Project Overview & Philosophy
The objective is to build a high-performance, battery-efficient, fully native Android application mimicking and expanding upon Apple's Dynamic Island, enriched with best-in-class features from Android OEM adaptations (Honor Magic Capsule, Xiaomi HyperOS Dynamic Notch, Oppo/Realme Mini Capsule & Fluid Cloud).

### Core Pillars
1. **True Native 24/7 Experience**: Runs seamlessly in the background as an overlay window without needing to be re-opened. Automatically boots on device startup (`BOOT_COMPLETED`).
2. **Near-Zero Battery Drain (< 1% daily)**: 100% event-driven architecture using Android's native `BroadcastReceiver`, `NotificationListenerService`, and `MediaSessionManager`. Zero polling loops. Complete animation halt and CPU idle when the screen is turned off (`ACTION_SCREEN_OFF`).
3. **Dynamic Touch Hitbox**: When collapsed, the overlay window bounds strictly match the small island pill (`FLAG_NOT_FOCUSABLE`). All other screen touches pass through unimpeded to underlying apps and the status bar. When expanded, touch bounds dynamically enlarge to receive interactive gestures.
4. **OLED Pure-Black Blending & Spring Physics**: Rendered in `#000000` pitch black to invisibly merge with the physical camera punch hole. Smooth spring animations with natural rubber-band damping.

---

## 2. Comprehensive Feature Matrix

### A. System Alerts & Hardware Indicators (Transient Auto-Collapsing Pills)
- **Charging & Battery**:
  - Plug-in connection animation with animated green battery icon and percentage.
  - Fast-charging / SuperVOOC / SuperCharge wattages / status indicator.
  - Battery full notification & low battery warnings (20%, 10%).
  - Cable disconnect animation.
- **Ringer & Sound Modes**:
  - Live transition between Ring, Vibrate, and Silent mode with animated bell and strike-through icon.
- **Device Connections**:
  - Bluetooth device connection/disconnection (TWS earbuds, headphones, smartwatch, car audio) with battery level pill.
  - USB accessory and OTG drive connected indicator.
- **Hotspot & Tethering**:
  - Active hotspot indicator with connected devices count.
- **Flashlight**:
  - Pill indicator when flashlight is turned on, with one-tap toggle to turn off.
- **Privacy Indicators**:
  - Subtle mic, camera, and location access indicators matching hardware safety alerts.
- **QuickShare / Nearby Share / Downloads**:
  - Circular / linear transfer progress ring and completed toast.

### B. Media & Audio Experience
- **Now Playing Integration**:
  - Compatible with Spotify, YouTube Music, Apple Music, SoundCloud, Audible, podcast apps, and local players via `MediaSessionManager`.
  - Album artwork thumbnail with smooth rounded borders.
  - Animated live audio visualizer / equalizer waveform bars (rhythm-reactive).
  - Track title and artist scrolling marquee.
- **Expanded Media Widget (Long-Press / Swipe Down)**:
  - Interactive playback controls: Play, Pause, Previous, Next, Seek backward/forward.
  - Interactive seekbar / track scrubber with elapsed and total time.
  - Volume slider and output device indicator.

### C. Live Activities & Ongoing Tasks
- **Phone & VoIP Calls**:
  - Incoming call pill: Caller name, avatar, pulsing green accept button and red decline button.
  - Ongoing call pill: Call duration timer, animated audio wave, quick mute, speaker toggle, and end call button.
  - Full support for cellular calls and VoIP (WhatsApp, Telegram, Google Meet, Truecaller).
- **Turn-by-Turn Navigation**:
  - Google Maps & Waze integration via active navigation notifications.
  - Compact view: Direction arrow icon (Turn left, turn right, roundabout, straight) and distance to next maneuver (e.g. "200m").
  - Expanded view: Full street name, ETA, remaining trip distance.
- **Timer & Stopwatch**:
  - Real-time countdown timer display.
  - Expanded view: Pause, resume, +1 min, and reset/cancel controls.
  - High-precision stopwatch mode.
- **Food Delivery & Ride Tracking**:
  - Real-time status pills from delivery apps (Uber, Lyft, Zomato, Swiggy, DoorDash) showing driver arriving, ETA, order picked up.
- **Messaging & Notifications**:
  - Dynamic notification banner morph: Incoming SMS, WhatsApp, or Telegram messages morph smoothly into the pill.
  - Expanded quick view with inline reply action or mark-as-read.

### D. Android OEM Innovations & Multitasking
- **Honor Magic Capsule Multi-App Stacking**:
  - Supports up to 3 simultaneous active background tasks (e.g. Media Playback + Navigation + Timer).
  - Tripple-app stack indicator dots.
  - Horizontal swipe gesture on the island to effortlessly cycle between stacked background tasks without opening any app!
- **Dual-Island Split Presentation (Apple iOS Style)**:
  - When two tasks run simultaneously, the island splits into two components:
    1. **Primary Pill**: Main task (e.g. music album art + visualizer).
    2. **Secondary Detached Bubble**: Secondary task (e.g. timer icon or navigation turn arrow).
- **Oppo / Realme Mini Capsule Fluid Glow**:
  - Fluid particle edge-glow around the camera punch hole during charging and high-priority alerts.
- **Xiaomi HyperOS Dynamic Notch Blending**:
  - Pixel-perfect camera cutout masking.

### E. Complete Gesture System
- **Single Tap**: Launches the originating app (e.g. tap music pill -> opens Spotify; tap navigation -> opens Google Maps).
- **Long Press**: Smooth spring morph into the rich **Expanded Interactive Card**.
- **Swipe Down**: Direct expand gesture into the rich interactive card.
- **Swipe Up**: Instantly collapse expanded card back into the compact pill.
- **Swipe Left / Right**: 
  - On single pill: Dismiss temporary alert or hide pill.
  - On stacked multi-app pills: Cycle through stacked activities (Honor style).
- **Tap Outside**: Dismisses expanded card and restores compact pill.

### F. Hardware Cutout Adaptation & Calibration Engine
- Automatic punch hole detection using Android `DisplayCutout` API.
- Position presets:
  - Center punch hole (Samsung S series, Pixel, Xiaomi, Redmi, Poco).
  - Left punch hole (OnePlus, Realme, older Samsungs).
  - Right punch hole.
  - Pill / Dual punch hole cutout.
  - Waterdrop / Notch.
- Custom Calibration Studio in app settings:
  - Sliders for X-offset, Y-offset, Pill Width, Pill Height, Corner Radius, and Island Spacing with live interactive test overlays.

---

## 3. Technical Architecture & Battery Optimization

```
┌─────────────────────────────────────────────────────────────┐
│                       ANDROID SYSTEM                        │
│  BroadcastReceivers   NotificationListener   MediaSession   │
│  (Battery, BT, Ring)    (Calls, Nav, Apps)    (Now Playing) │
└──────────────┬──────────────────┬──────────────────┬────────┘
               │                  │                  │
               ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────┐
│                    ISLAND EVENT BUS                         │
│  - Event deduplication & priority queue                     │
│  - Screen state listener (Sleeps when Screen OFF)           │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 ISLAND STATE & STACK MANAGER                │
│  - Priority Arbiter (Alerts > Calls > Nav > Timer > Media)  │
│  - Multi-Task Stack (Up to 3 concurrent active tasks)       │
│  - Dual-Island Split Engine (Main Pill + Detached Circle)   │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 NATIVE OVERLAY WINDOW SERVICE               │
│  - WindowManager (TYPE_APPLICATION_OVERLAY)                 │
│  - Dynamic Window Bounds (Pill size vs Expanded size)       │
│  - Jetpack Compose Overlay UI + Canvas Waveforms            │
│  - Spring Physics Morphing Engine (Rubber-band physics)     │
│  - Full Gesture Handler (Tap, Long-press, Swipes)           │
└─────────────────────────────────────────────────────────────┘
```

### Battery Consumption Safeguards
1. **Zero Polling**: No background while-loops or continuous timer tickers. Listeners only trigger when an OS event occurs.
2. **Display Power-State Awareness**:
   - Registers `ACTION_SCREEN_OFF`. When the screen goes dark, all animation tickers and coroutines immediately halt.
   - Resumes cleanly on `ACTION_SCREEN_ON`.
3. **Foreground Service Safety**:
   - `IslandOverlayService` runs with `START_STICKY` and a silent, minimal-importance notification.
   - Battery optimization whitelist guide included in the setup flow.

---

## 4. OEM & System Specific Handlers
1. **Nothing OS News Filter**:
   - Filters out persistent background audio sessions created by Nothing OS's news widget (`com.nothing.hearthstone` / "News Reporter") whenever paused or idle, keeping the island clean for real media.
2. **Audio Visualizer Static Freeze on Pause**:
   - Audio visualizer automatically halts infinite transition animations when `isPlaying == false`, displaying subtle, static resting bars (diff = 0) with zero CPU/GPU overhead.
3. **Instant App Redirection**:
   - Tapping an expanded card action or header launches the source application directly via session PendingIntent or PackageManager launch intent with Android 14/15 Background Activity Launch (BAL) exemption.
4. **Lockscreen & Power Button Invisibility**:
   - Broadcast listeners for `ACTION_SCREEN_OFF`, `ACTION_SCREEN_ON`, and `ACTION_USER_PRESENT` check `KeyguardManager.isKeyguardLocked`.
   - On screen lock: island immediately hides (`View.GONE`) and sets `FLAG_NOT_TOUCHABLE`, preventing any interference with the lock screen or power button.
   - On unlock: island restores smoothly to active status.

