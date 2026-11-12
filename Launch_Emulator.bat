@echo off
title Limit Crosses - Android Emulator Launcher
echo ========================================================
echo   LIMIT CROSSES - Android Emulator Launcher
echo   Created by Pankaj Yadav
echo ========================================================
echo.

set "SDK_ROOT=C:\Users\IndiPankaj26\AppData\Local\Android\Sdk"
set "EMULATOR_EXE=%SDK_ROOT%\emulator\emulator.exe"
set "ADB_EXE=%SDK_ROOT%\platform-tools\adb.exe"
set "AVD_NAME=Medium_Phone_API_37.0"
set "APK_PATH=%~dp0LimitCrosses-debug.apk"

if not exist "%EMULATOR_EXE%" (
    echo [ERROR] Android Emulator not found at: %EMULATOR_EXE%
    pause
    exit /b 1
)

echo [1/3] Launching Android Emulator GUI window (%AVD_NAME%)...
start "" "%EMULATOR_EXE%" -avd %AVD_NAME%

echo.
echo [2/3] Waiting for emulator to boot up...
"%ADB_EXE%" wait-for-device
:wait_boot
for /f "tokens=*" %%a in ('"%ADB_EXE%" shell getprop sys.boot_completed 2^>nul') do set BOOT=%%a
if not "%BOOT%"=="1" (
    timeout /t 2 /nobreak >nul
    goto wait_boot
)
echo [OK] Device booted successfully!

echo.
echo [3/3] Installing and starting Limit Crosses...
if exist "%APK_PATH%" (
    "%ADB_EXE%" install -r "%APK_PATH%"
    echo [OK] LimitCrosses-debug.apk installed!
    "%ADB_EXE%" shell am start -n com.limitcrosses.app/.MainActivity
    echo [OK] Application launched!
) else (
    echo [WARNING] APK not found at %APK_PATH%
)

echo.
echo ========================================================
echo   Ready! You can now test Limit Crosses on the emulator.
echo   App created by Pankaj Yadav
echo ========================================================
echo.
pause

