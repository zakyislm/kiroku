@echo off
echo ====================================================
echo             Kiroku Desktop Uninstaller            
echo ====================================================
echo.
echo Stopping Kiroku background services...
taskkill /f /im Kiroku.exe >nul 2>&1

echo Removing startup shortcuts...
set STARTUP_BAT="%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup\Kiroku.bat"
if exist %STARTUP_BAT% (
    del /f /q %STARTUP_BAT%
)

echo Removing application log files...
set LOG_DIR="%USERPROFILE%\.kiroku"
if exist %LOG_DIR% (
    rmdir /s /q %LOG_DIR%
)

echo Cleaning installation files...
rmdir /s /q "%~dp0target\dist\Kiroku" >nul 2>&1

echo.
echo Kiroku Desktop has been successfully uninstalled.
echo ====================================================
pause
