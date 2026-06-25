@echo off
setlocal enabledelayedexpansion

echo ====================================================
echo             Kiroku Desktop Build Script            
echo ====================================================
echo.
echo Please select an option:
echo [1] Compile and Run (Default)
echo [2] Package Application (Create JAR in target/)
echo [3] Clean Maven Target
echo [4] Build Standalone Executable (Folder with launcher)
echo [5] Build Windows Installer (.exe Setup)
echo [6] Build Windows Installer (.msi)
echo.
set /p opt="Enter option (1-6) [default=1]: "

if "%opt%"=="" set opt=1

if "%opt%"=="1" (
    echo.
    echo [Kiroku] Compiling and starting application...
    echo ====================================================
    powershell -ExecutionPolicy Bypass -File "%~dp0build_exe.ps1" -Type run
) else if "%opt%"=="2" (
    echo.
    echo [Kiroku] Packaging application into JAR...
    echo ====================================================
    powershell -ExecutionPolicy Bypass -File "%~dp0build_exe.ps1" -Type jar
) else if "%opt%"=="3" (
    echo.
    echo [Kiroku] Cleaning target folder...
    echo ====================================================
    powershell -ExecutionPolicy Bypass -Command "if (Test-Path '%~dp0target\classes') { Remove-Item '%~dp0target\classes' -Recurse -Force }; if (Test-Path '%~dp0target\libs') { Remove-Item '%~dp0target\libs' -Recurse -Force }; if (Test-Path '%~dp0target\dist') { Remove-Item '%~dp0target\dist' -Recurse -Force }; if (Test-Path '%~dp0target\MANIFEST.MF') { Remove-Item '%~dp0target\MANIFEST.MF' -Force }"
    echo [Kiroku] Clean complete.
) else if "%opt%"=="4" (
    echo.
    echo [Kiroku] Building standalone folder with launcher...
    echo ====================================================
    powershell -ExecutionPolicy Bypass -File "%~dp0build_exe.ps1" -Type app-image
) else if "%opt%"=="5" (
    echo.
    echo [Kiroku] Building Windows Installer - exe Setup...
    echo ====================================================
    powershell -ExecutionPolicy Bypass -File "%~dp0build_exe.ps1" -Type exe
) else if "%opt%"=="6" (
    echo.
    echo [Kiroku] Building Windows Installer - msi...
    echo ====================================================
    powershell -ExecutionPolicy Bypass -File "%~dp0build_exe.ps1" -Type msi
) else (
    echo Invalid option selected.
)

echo.
echo Press any key to exit...
pause > nul
