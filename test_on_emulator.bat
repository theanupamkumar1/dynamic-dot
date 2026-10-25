@echo off
title Dynamic Island - Deploy to Emulator
echo Waiting for Android Virtual Device...
"C:\Users\anupam kumar\AppData\Local\Android\Sdk\platform-tools\adb.exe" -e wait-for-device
echo Emulator connected! Installing Dynamic Island APK...
"C:\Users\anupam kumar\AppData\Local\Android\Sdk\platform-tools\adb.exe" -e install -r "%~dp0app\build\outputs\apk\debug\app-debug.apk"
echo Granting overlay and accessibility permissions...
"C:\Users\anupam kumar\AppData\Local\Android\Sdk\platform-tools\adb.exe" -e shell appops set com.dynamicisland.app SYSTEM_ALERT_WINDOW allow
"C:\Users\anupam kumar\AppData\Local\Android\Sdk\platform-tools\adb.exe" -e shell settings put secure enabled_accessibility_services com.dynamicisland.app/com.dynamicisland.app.service.IslandAccessibilityService
"C:\Users\anupam kumar\AppData\Local\Android\Sdk\platform-tools\adb.exe" -e shell settings put secure accessibility_enabled 1
echo Launching Dynamic Island...
"C:\Users\anupam kumar\AppData\Local\Android\Sdk\platform-tools\adb.exe" -e shell am start -n com.dynamicisland.app/.MainActivity
echo ========================================================
echo App is now running on your virtual Google Pixel 6!
echo ========================================================
pause
