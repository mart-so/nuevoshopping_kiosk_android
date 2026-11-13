$ErrorActionPreference = 'Stop'
$Repo = Split-Path -Parent $PSScriptRoot
if (-not $env:JAVA_HOME -and (Test-Path 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe')) { $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr' }
if (-not $env:ANDROID_HOME -and (Test-Path "$env:LOCALAPPDATA\Android\Sdk")) { $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk" }
Push-Location $Repo
try {
    .\gradlew.bat --no-daemon testDebugUnitTest assembleDebug assembleRelease
    if ($LASTEXITCODE -ne 0) { throw "Gradle terminó con código $LASTEXITCODE" }
    $RepoRoot = [System.IO.Path]::GetFullPath($Repo).TrimEnd('\')
    $Dist = [System.IO.Path]::GetFullPath((Join-Path $RepoRoot 'dist'))
    if (-not $Dist.StartsWith($RepoRoot + '\', [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "La carpeta de APKs está fuera del repositorio: $Dist"
    }
    New-Item -ItemType Directory -Force -Path $Dist | Out-Null
    $Date = Get-Date -Format 'yyyy-MM-dd'
    $PreviousDatedApks = Get-ChildItem -LiteralPath $Dist -File -Filter 'generic-kiosk-browser-*.apk' |
        Where-Object { $_.Name -match '^generic-kiosk-browser-(debug|release-unsigned)-\d{4}-\d{2}-\d{2}\.apk$' }
    foreach ($OldApk in $PreviousDatedApks) {
        $OldPath = [System.IO.Path]::GetFullPath($OldApk.FullName)
        if (-not $OldPath.StartsWith($Dist.TrimEnd('\') + '\', [System.StringComparison]::OrdinalIgnoreCase) -or
            ($OldApk.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
            throw "Se rechazó eliminar un APK fuera de dist o un enlace: $OldPath"
        }
        Remove-Item -LiteralPath $OldPath -Force
    }
    $DebugApk = Join-Path $Repo 'app\build\outputs\apk\debug\app-debug.apk'
    $ReleaseApk = Join-Path $Repo 'app\build\outputs\apk\release\app-release-unsigned.apk'
    Copy-Item -LiteralPath $DebugApk -Destination (Join-Path $Dist 'generic-kiosk-browser-debug.apk') -Force
    Copy-Item -LiteralPath $DebugApk -Destination (Join-Path $Dist "generic-kiosk-browser-debug-$Date.apk") -Force
    Copy-Item -LiteralPath $ReleaseApk -Destination (Join-Path $Dist 'generic-kiosk-browser-release-unsigned.apk') -Force
    Copy-Item -LiteralPath $ReleaseApk -Destination (Join-Path $Dist "generic-kiosk-browser-release-unsigned-$Date.apk") -Force
} finally { Pop-Location }
