# Dynamic Island - Automated Feature Demo & Test Runner
# Triggers full sequence of all 16 Dynamic Island features on your phone screen!

param(
    [string]$Device = ""
)

$adb = if (Get-Command adb -ErrorAction SilentlyContinue) { "adb" } elseif (Test-Path "C:\adb_driver_for_wsa\adb.exe") { "C:\adb_driver_for_wsa\adb.exe" } else { "C:\Users\anupam kumar\AppData\Local\Android\Sdk\platform-tools\adb.exe" }

if (-not $Device) {
    $deviceLine = & $adb devices | Where-Object { $_ -match "\bdevice$" } | Select-Object -First 1
    if (-not $deviceLine) {
        & $adb connect 192.168.0.107:40229 | Out-Null
        $deviceLine = & $adb devices | Where-Object { $_ -match "\bdevice$" } | Select-Object -First 1
    }
    if ($deviceLine) {
        $Device = ($deviceLine -split "\s+")[0]
    }
}

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "   NOTHING OS DYNAMIC ISLAND TEST RUNNER          " -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
if ($Device) {
    Write-Host "Target Device: $Device" -ForegroundColor Yellow
} else {
    Write-Host "Target Device: Default connected ADB device" -ForegroundColor Yellow
}
Write-Host "Starting automated test loop of Nothing OS concept states..." -ForegroundColor Green

$adbArgs = if ($Device) { @("-s", $Device) } else { @() }
& $adb @adbArgs shell am broadcast -a com.dynamicisland.TRIGGER_TEST --es type demo_all

Write-Host "`nTest sequence is running on your phone screen:" -ForegroundColor White
Write-Host "  1. Nothing OS Ambient Notch Mask (Dot-matrix 10:25 clock)" -ForegroundColor Gray
Write-Host "  2. Music Playing (Minimised - Dot Equalizer + Pause)" -ForegroundColor Gray
Write-Host "  3. Music Playing (Expanded - Coldplay Sparks, Dot Scrubber, Controls)" -ForegroundColor Gray
Write-Host "  4. Incoming Call (Minimised - Green/Red Action Pills)" -ForegroundColor Gray
Write-Host "  5. Incoming Call (Expanded - Avatar 'A', Anupam, Accept/Decline)" -ForegroundColor Gray
Write-Host "  6. Timer / Stopwatch (Minimised - 03:27 with Pause)" -ForegroundColor Gray
Write-Host "  7. Timer / Stopwatch (Expanded - Dotted Circular Ring Countdown)" -ForegroundColor Gray
Write-Host "  8. Navigation / Maps (Minimised - Turn Arrow + Maneuver)" -ForegroundColor Gray
Write-Host "  9. Navigation / Maps (Expanded - 200m Turn Right, ETA Bar)" -ForegroundColor Gray
Write-Host " 10. Voice Recording (Minimised - Pulsing Red Dot + Dot Waveform)" -ForegroundColor Gray
Write-Host " 11. Voice Recording (Expanded - Dot Waveform + Stop/Pause)" -ForegroundColor Gray
Write-Host " 12. Charging 62% (Minimised - Dotted Green Ring + Lightning)" -ForegroundColor Gray
Write-Host " 13. Charging 62% (Expanded - Dotted Ring, Big Percentage)" -ForegroundColor Gray
Write-Host " 14. Earbuds Connected (Minimised - Nothing Ear L/R Battery)" -ForegroundColor Gray
Write-Host " 15. Earbuds Connected (Expanded - Transparent Buds, L/R/Case Bars)" -ForegroundColor Gray
Write-Host " 16. Hotspot / Tethering (Minimised - 1 Device Connected)" -ForegroundColor Gray
Write-Host " 17. Hotspot / Tethering (Expanded - Concentric Dot Waves + Turn Off)" -ForegroundColor Gray
Write-Host " 18. Now Playing Video (Minimised - YouTube Red Icon)" -ForegroundColor Gray
Write-Host " 19. Now Playing Video (Expanded - Nothing OS 3.0 Trailer Card)" -ForegroundColor Gray
Write-Host " 20. Silent Mode / Alerts (Red Mute Bell Badge)" -ForegroundColor Gray
Write-Host " 21. Face Unlock (Dot Scanner Reticle Animation)" -ForegroundColor Gray
Write-Host " 22. Do Not Disturb (Purple Crescent Moon Badge)" -ForegroundColor Gray
Write-Host " 23. Screenshot Saved (System Alert with Thumbnail Preview)" -ForegroundColor Gray
Write-Host " 24. Flight Mode (Airplane Orange Badge)" -ForegroundColor Gray
Write-Host " 25. Low Battery (15% Red Accent Alert)" -ForegroundColor Gray
Write-Host " 26. Bluetooth Connected (Blue Bluetooth Pill)" -ForegroundColor Gray
Write-Host " 27. Multiple Activities Stack (Music + Nav + Timer + Rec Icons)" -ForegroundColor Gray
Write-Host "`nTo cancel at any time, run:" -ForegroundColor Yellow
Write-Host "  adb shell am broadcast -a com.dynamicisland.TRIGGER_TEST --es type stop_demo`n" -ForegroundColor Yellow
