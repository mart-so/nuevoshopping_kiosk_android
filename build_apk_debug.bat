@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0build_apk_debug.ps1" %*
exit /b %ERRORLEVEL%
