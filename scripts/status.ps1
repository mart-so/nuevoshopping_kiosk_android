$ErrorActionPreference = 'Stop'
$Adb = if ($env:ANDROID_HOME) { Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe' } else { 'adb' }
& $Adb devices -l
& $Adb shell dumpsys device_policy | Select-String 'Device Owner|device owner|com.generic.kiosk'
& $Adb shell pidof com.generic.kiosk.debug
