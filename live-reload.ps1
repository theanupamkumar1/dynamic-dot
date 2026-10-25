# Dynamic Island - Live Watcher & Auto-Deploy
# Watches app/src/main and auto-installs to your device on save

param(
    [string]$Device = ""
)

$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$adb = "C:\Users\anupam kumar\AppData\Local\Android\Sdk\platform-tools\adb.exe"

if (-not $Device) {
    # Auto-detect connected ADB device
    $deviceLine = & $adb devices | Where-Object { $_ -match "\bdevice$" } | Select-Object -First 1
    if ($deviceLine) {
        $Device = ($deviceLine -split "\s+")[0]
    } else {
        $Device = "192.168.0.104:35797"
    }
}
$projectRoot = $PSScriptRoot
if (-not $projectRoot) { $projectRoot = Get-Location }

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  DYNAMIC ISLAND LIVE AUTO-DEPLOY WATCHER    " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "Target Device : $Device" -ForegroundColor Yellow
Write-Host "Watching path : $projectRoot\app\src\main" -ForegroundColor Yellow
Write-Host "Edit any Kotlin or XML file and save (Ctrl+S)..." -ForegroundColor Green
Write-Host "Press Ctrl+C to stop." -ForegroundColor Gray
Write-Host "---------------------------------------------" -ForegroundColor Gray

$watchPath = Join-Path $projectRoot "app\src\main"
$watcher = New-Object System.IO.FileSystemWatcher
$watcher.Path = $watchPath
$watcher.IncludeSubdirectories = $true
$watcher.EnableRaisingEvents = $true
$watcher.NotifyFilter = [System.IO.NotifyFilters]::LastWrite -bor [System.IO.NotifyFilters]::FileName

$lastRun = [DateTime]::MinValue

while ($true) {
    $change = $watcher.WaitForChanged([System.IO.WatcherChangeTypes]::Changed -bor [System.IO.WatcherChangeTypes]::Created, 1000)
    if ($change.TimedOut) {
        continue
    }

    $ext = [System.IO.Path]::GetExtension($change.Name)
    if ($ext -notin @(".kt", ".xml", ".gradle", ".toml")) {
        continue
    }

    $now = [DateTime]::Now
    if (($now - $lastRun).TotalSeconds -lt 2) {
        continue # debounce
    }
    $lastRun = $now

    Write-Host "`n[$([DateTime]::Now.ToString('HH:mm:ss'))] File changed: $($change.Name)" -ForegroundColor Magenta
    Write-Host "Building and pushing incremental update..." -ForegroundColor Yellow
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

    & "$projectRoot\gradlew.bat" -p "$projectRoot" installDebug --offline 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        # Retry online in case offline cache missed something
        & "$projectRoot\gradlew.bat" -p "$projectRoot" installDebug 2>&1 | Out-Null
    }

    $stopwatch.Stop()
    if ($LASTEXITCODE -eq 0) {
        Write-Host "SUCCESS: Live update deployed to phone in $($stopwatch.Elapsed.TotalSeconds.ToString('F1'))s!" -ForegroundColor Green
    } else {
        Write-Host "BUILD FAILED. Check compile errors." -ForegroundColor Red
    }
}
