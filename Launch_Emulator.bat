@echo off
title Limit Crosses - Android Emulator
echo ========================================================
echo   LIMIT CROSSES - Android Emulator Launcher
echo   Created by Pankaj Yadav
echo ========================================================
echo.

set "SDK_ROOT=C:\Users\IndiPankaj26\AppData\Local\Android\Sdk"
set "EMULATOR_EXE=%SDK_ROOT%\emulator\emulator.exe"
set "ADB_EXE=%SDK_ROOT%\platform-tools\adb.exe"
set "AVD_NAME=Medium_Phone_API_37.0"
set "AVD_DIR=%USERPROFILE%\.android\avd\Medium_Phone.avd"
set "APK_PATH=%~dp0LimitCrosses-debug.apk"

if not exist "%EMULATOR_EXE%" (
    echo [ERROR] Android Emulator not found at: %EMULATOR_EXE%
    pause
    exit /b 1
)

:: Clear stale locks if emulator crashed or was closed
if exist "%AVD_DIR%\hardware-qemu.ini.lock" rmdir /s /q "%AVD_DIR%\hardware-qemu.ini.lock" >nul 2>&1
if exist "%AVD_DIR%\multiinstance.lock" del /f /q "%AVD_DIR%\multiinstance.lock" >nul 2>&1

echo [1/3] Launching Android Emulator GUI Phone Window (%AVD_NAME%)...
echo Please wait for the phone window to appear on your desktop...
start "Android Emulator Phone" "%EMULATOR_EXE%" -avd %AVD_NAME%

echo.
echo [2/3] Connecting and waiting for emulator to boot up...
"%ADB_EXE%" wait-for-device

:wait_boot
for /f "tokens=*" %%a in ('"%ADB_EXE%" shell getprop sys.boot_completed 2^>nul') do set BOOT=%%a
if not "%BOOT%"=="1" (
    timeout /t 3 /nobreak >nul
    goto wait_boot
)
echo [OK] Phone window is now booted on your desktop!

echo.
echo [3/3] Installing and starting Limit Crosses inside the emulator...
if exist "%APK_PATH%" (
    "%ADB_EXE%" install -r "%APK_PATH%"
    echo [OK] LimitCrosses-debug.apk installed successfully!
    "%ADB_EXE%" shell am start -n com.limitcrosses.app/.MainActivity
    echo [OK] Limit Crosses launched on the phone screen!
) else (
    echo [WARNING] APK not found at %APK_PATH%
)

echo.
echo ========================================================
echo   SUCCESS! The Android Phone GUI is running on your screen.
echo   You can click and interact with the phone directly!
echo   Created by Pankaj Yadav
echo ========================================================
echo.
echo Leave this window open while testing, or press any key to close this console.
pause
