$ErrorActionPreference = 'Stop'
$Repo = Split-Path -Parent $PSScriptRoot
if (-not $env:JAVA_HOME -and (Test-Path 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe')) { $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr' }
if (-not $env:ANDROID_HOME -and (Test-Path "$env:LOCALAPPDATA\Android\Sdk")) { $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk" }
Push-Location $Repo
try {
    .\gradlew.bat --no-daemon testDebugUnitTest assembleDebug assembleRelease
    if ($LASTEXITCODE -ne 0) { throw "Gradle terminó con código $LASTEXITCODE" }
    $Dist = Join-Path $Repo 'dist'
    New-Item -ItemType Directory -Force -Path $Dist | Out-Null
    Copy-Item -LiteralPath (Join-Path $Repo 'app\build\outputs\apk\debug\app-debug.apk') -Destination (Join-Path $Dist 'generic-kiosk-browser-debug.apk') -Force
    Copy-Item -LiteralPath (Join-Path $Repo 'app\build\outputs\apk\release\app-release-unsigned.apk') -Destination (Join-Path $Dist 'generic-kiosk-browser-release-unsigned.apk') -Force
} finally { Pop-Location }
