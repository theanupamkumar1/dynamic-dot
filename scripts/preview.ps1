param (
    [string]$Type = "music",
    [string]$OutputPath = "C:\Users\anupam kumar\.gemini\antigravity-ide\brain\1a3eda46-a1e5-4843-b711-1249f5663271\live_preview.png"
)

$adb = "C:\Users\anupam kumar\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "==> Triggering state: $Type..."
& $adb shell "am broadcast -a com.dynamicisland.TRIGGER_TEST -p com.dynamicisland.app --es type $Type" | Out-Null

Start-Sleep -Milliseconds 600

Write-Host "==> Capturing screenshot..."
& $adb shell "screencap -p /sdcard/live_preview.png"
& $adb pull /sdcard/live_preview.png $OutputPath | Out-Null

$cropPath = $OutputPath -replace "\.png$", "_crop.png"
python -c "
from PIL import Image
try:
    img = Image.open(r'$OutputPath')
    crop = img.crop((0, 0, 1080, 140))
    crop.save(r'$cropPath')
    print('Preview & Crop ready.')
except Exception as e:
    print('Error cropping:', e)
"
