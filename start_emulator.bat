@echo off
title Android Virtual Device - Pixel 6
echo ========================================================
echo  Starting Google Pixel 6 Android Virtual Device (AVD)
echo  Hardware GPU acceleration enabled
echo ========================================================
"C:\Users\anupam kumar\AppData\Local\Android\Sdk\emulator\emulator.exe" -avd Pixel_6_API_31 -gpu host
pause
