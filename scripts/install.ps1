param([string]$Apk = "app\build\outputs\apk\debug\app-debug.apk")
$ErrorActionPreference = 'Stop'
$Repo = Split-Path -Parent $PSScriptRoot
$Adb = if ($env:ANDROID_HOME) { Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe' } else { 'adb' }
Push-Location $Repo
try {
    if (-not (Test-Path $Apk)) { throw "No existe $Apk. Ejecutá scripts\build.ps1 primero." }
    & $Adb install -r $Apk
    if ($LASTEXITCODE -ne 0) { throw "adb install terminó con código $LASTEXITCODE" }
    & $Adb shell monkey -p com.generic.kiosk.debug 1
} finally { Pop-Location }
