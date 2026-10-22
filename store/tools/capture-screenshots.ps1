# Takes the Play Store phone screenshots from the Turnfall app running on the Android emulator.
#
# 1. Start the emulator (F5 in VS Code, or: emulator -avd Turnfall_API_36).
# 2. Install the app: .\gradlew.bat :app:installDebug
# 3. Run this from the project root:
#      powershell -ExecutionPolicy Bypass -File store\tools\capture-screenshots.ps1
#
# The screens are saved as 1080x1920 (9:16) 24-bit PNG files in store\screenshots\phone\.
# Play asks for an aspect ratio of at most 2:1, so the emulator's screen size is set to 1080x1920 while
# the screenshots are taken, and reset afterwards.

$ErrorActionPreference = 'Stop'

$root = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$package = 'com.turnfall.game'
$out = Join-Path $root 'store\screenshots\phone'
$temp = Join-Path $env:TEMP 'turnfall-store'

New-Item -ItemType Directory -Force -Path $out, $temp | Out-Null
Add-Type -AssemblyName System.Drawing

function Adb {
    & $adb @args
    if ($LASTEXITCODE -ne 0) { throw "adb failed: $args" }
}

# Finds an element by its text on the current screen and taps the middle of it.
function Tap-Text([string]$text) {
    Adb shell uiautomator dump /sdcard/turnfall-ui.xml | Out-Null
    Adb pull /sdcard/turnfall-ui.xml (Join-Path $temp 'ui.xml') | Out-Null
    $xml = Get-Content -Raw -Path (Join-Path $temp 'ui.xml')
    $pattern = 'text="' + [regex]::Escape($text) + '"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    $match = [regex]::Match($xml, $pattern)
    if (-not $match.Success) { throw "no element with the text '$text' on the screen" }
    $x = [int](([int]$match.Groups[1].Value + [int]$match.Groups[3].Value) / 2)
    $y = [int](([int]$match.Groups[2].Value + [int]$match.Groups[4].Value) / 2)
    Adb shell input tap $x $y
}

# Takes a screenshot and saves it as a 24-bit PNG (no alpha channel), which the Play Console accepts.
function Capture([string]$name) {
    Adb shell screencap -p /sdcard/turnfall-shot.png
    Adb pull /sdcard/turnfall-shot.png (Join-Path $temp 'shot.png') | Out-Null
    $source = [System.Drawing.Image]::FromFile((Join-Path $temp 'shot.png'))
    $bitmap = New-Object System.Drawing.Bitmap $source.Width, $source.Height, ([System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.DrawImage($source, 0, 0, $source.Width, $source.Height)
    $graphics.Dispose()
    $source.Dispose()
    $bitmap.Save((Join-Path $out $name), [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
    "saved $name"
}

Adb shell wm size 1080x1920
try {
    Adb shell am force-stop $package
    Adb shell monkey -p $package -c android.intent.category.LAUNCHER 1 | Out-Null
    Start-Sleep -Seconds 4
    Capture '01-menu.png'

    Tap-Text 'Play'
    Start-Sleep -Seconds 3
    Capture '02-play.png'

    Adb shell input keyevent KEYCODE_BACK
    Start-Sleep -Seconds 2
    Tap-Text 'Levels'
    Start-Sleep -Seconds 2
    Capture '03-levels.png'

    Adb shell input keyevent KEYCODE_BACK
    Start-Sleep -Seconds 2
    Tap-Text 'Settings'
    Start-Sleep -Seconds 2
    Capture '04-settings.png'
}
finally {
    Adb shell wm size reset | Out-Null
}
