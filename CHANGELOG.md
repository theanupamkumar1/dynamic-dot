# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-10-31

### Added
- **Full dynamicSpot Feature Parity (All Pro / Premium Features 100% Unlocked & Free)**:
  - **App Notification Filter**: Whitelist/blacklist apps with an interactive package picker dialog including search, Select All, and Clear All.
  - **Hardware Battery Notification Events**: Real-time alerts for charger connect, SuperVOOC / USB-PD fast charging wattage, low battery warning (<15%), and full charge (100%).
  - **Lockscreen & Always-On Display (AOD)**: Display dynamic island popups on the lockscreen with optional sensitive content masking.
  - **Banner Suppression**: Snoozes native Android heads-up floating banners to avoid double notifications.
  - **Interactive Inline Quick Reply**: `RemoteInput` pending intent text field inside expanded notification cards for instant replies without opening messaging apps.
  - **Camera Cutout Glow Ring & Halo**: Pulsing illuminated halo around punch-hole cutout with event-specific colors (charging emerald, low battery crimson, full battery blue, notification accent).
  - **Adaptive Dynamic Artwork Color Tinting**: Real-time HSV saturation & luminance extraction from media artwork and notification icons.
  - **Dual-Island Split Mode**: Apple-style detached secondary bubble when two background activities run concurrently.
  - **Honor Magic Capsule Tripple-Stack**: Cycle through up to 3 background tasks via horizontal swipe gestures.
  - **Configurable Gestures**: Single-tap, long-press, swipe down to expand, swipe up to collapse, and swipe horizontally to dismiss.
  - **Configurable Timeouts**: Independent sliders for Compact Pill Auto-Dismiss (0–15s) and Expanded Card Auto-Collapse (0–15s).
- **Authentic Apple iOS Dynamic Island Music Player**:
  - Apple squircle album artwork thumbnail with glass rim.
  - Rhythm-reactive live animated audio visualizer equalizer bars.
  - Continuous seekbar scrubber with elapsed and countdown remaining timestamps.
  - True iOS playback controls (AirPlay, previous, large circular play/pause, next, and audio routing).
- **Cutout Calibration Studio**:
  - Live interactive sliders for notch alignment: Horizontal Shift, Vertical Margin, Pill Width, Pill Height, Corner Radius, Cutout Gap, and Split Spacing.
  - One-click presets for Center Hole, Left Hole, and Right Hole cutouts.
  - One-click 16-state automated live feature demo test.

### Changed
- Refactored `DynamicIslandOverlay` to use dynamic sizing for notifications, calls, earbuds, and screenshots to eliminate text clipping.
- Re-architected settings screen into dedicated sections: Notifications & Apps, Behavior & Gestures, and Appearance & Visuals.
- Optimized battery consumption to < 1% daily through 100% event-driven listeners without background polling.

### Fixed
- Fixed notification compact pill text wrapping and bounds clipping.
- Fixed lockscreen visibility to respect keyguard unlock state.
- Fixed expanded card outside-tap detection to prevent accidental dismissal during card interaction.
