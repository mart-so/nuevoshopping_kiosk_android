$ErrorActionPreference = 'Stop'

# Determinar el directorio raíz del repositorio
$Repo = $PSScriptRoot
if (-not $Repo) {
    $Repo = (Get-Location).Path
}

# Configuración automática de variables de entorno para Android Studio y SDK si no están definidas
if (-not $env:JAVA_HOME -and (Test-Path 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe')) {
    $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
}
if (-not $env:ANDROID_HOME -and (Test-Path "$env:LOCALAPPDATA\Android\Sdk")) {
    $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
}

$Dist = Join-Path $Repo 'dist'
if (-not (Test-Path -LiteralPath $Dist)) {
    New-Item -ItemType Directory -Force -Path $Dist | Out-Null
}

# 1. Borrar todas las APKs viejas existentes en dist
Write-Host "Limpiando APKs anteriores en dist..." -ForegroundColor Cyan
$OldApks = Get-ChildItem -LiteralPath $Dist -File -Filter '*.apk'
foreach ($old in $OldApks) {
    Remove-Item -LiteralPath $old.FullName -Force
}

# 2. Compilar APK en modo Debug con Gradle
Write-Host "Compilando APK Debug con Gradle..." -ForegroundColor Cyan
Push-Location $Repo
try {
    .\gradlew.bat assembleDebug
    if ($LASTEXITCODE -ne 0) {
        throw "La compilacion con Gradle fallo con codigo de salida $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

# 3. Asegurar que solo quede la nueva APK con fecha, hora y minutos en el nombre
# Si se genero el nombre sin fecha (generic-kiosk-browser-debug.apk), se elimina
$StableDebugApk = Join-Path $Dist 'generic-kiosk-browser-debug.apk'
if (Test-Path -LiteralPath $StableDebugApk) {
    Remove-Item -LiteralPath $StableDebugApk -Force
}

# Si por alguna razon Gradle no copio el archivo fechado a dist, se copia desde la salida del build
$DatedApks = Get-ChildItem -LiteralPath $Dist -File -Filter 'generic-kiosk-browser-debug-*.apk'
if ($DatedApks.Count -eq 0) {
    $GradleOutputApk = Join-Path $Repo 'app\build\outputs\apk\debug\app-debug.apk'
    if (Test-Path -LiteralPath $GradleOutputApk) {
        $BuildStamp = Get-Date -Format 'yyyy-MM-dd-HHmm'
        $TargetName = "generic-kiosk-browser-debug-$BuildStamp.apk"
        Copy-Item -LiteralPath $GradleOutputApk -Destination (Join-Path $Dist $TargetName) -Force
    } else {
        throw "No se encontro el APK generado en $GradleOutputApk"
    }
}

# Si hubiera mas de un APK, conservar unicamente el mas reciente
$FinalApks = Get-ChildItem -LiteralPath $Dist -File -Filter '*.apk' | Sort-Object LastWriteTime -Descending
if ($FinalApks.Count -gt 1) {
    foreach ($Extra in $FinalApks[1..($FinalApks.Count - 1)]) {
        Remove-Item -LiteralPath $Extra.FullName -Force
    }
}

$ResultApk = Get-ChildItem -LiteralPath $Dist -File -Filter '*.apk' | Select-Object -First 1

if ($ResultApk) {
    $SizeMB = [math]::Round($ResultApk.Length / 1MB, 2)
    Write-Host ""
    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host " [OK] APK Debug compilado con exito" -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host " Archivo : $($ResultApk.Name)" -ForegroundColor White
    Write-Host " Carpeta : $Dist" -ForegroundColor White
    Write-Host " Tamano  : $SizeMB MB" -ForegroundColor White
    Write-Host " Fecha   : $($ResultApk.LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss'))" -ForegroundColor White
    Write-Host "==========================================================" -ForegroundColor Green
} else {
    throw "Error: No se encontro el archivo APK en $Dist despues del build."
}
