# Limit Crosses - Android Emulator Launcher
# Created by Pankaj Yadav

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  LIMIT CROSSES - Android Emulator Launcher" -ForegroundColor Yellow
Write-Host "  Created by Pankaj Yadav" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

$SdkRoot = "C:\Users\IndiPankaj26\AppData\Local\Android\Sdk"
$EmulatorExe = Join-Path $SdkRoot "emulator\emulator.exe"
$AdbExe = Join-Path $SdkRoot "platform-tools\adb.exe"
$AvdName = "Medium_Phone_API_37.0"
$ApkPath = Join-Path $PSScriptRoot "LimitCrosses-debug.apk"

if (-not (Test-Path $EmulatorExe)) {
    Write-Host "[ERROR] Emulator executable not found at: $EmulatorExe" -ForegroundColor Red
    pause
    exit 1
}

Write-Host "[1/3] Launching Android Emulator window ($AvdName)..." -ForegroundColor Yellow
Start-Process -FilePath $EmulatorExe -ArgumentList "-avd", $AvdName

Write-Host "[2/3] Waiting for device to finish booting..." -ForegroundColor Yellow
& $AdbExe wait-for-device

$booted = $false
while (-not $booted) {
    $res = (& $AdbExe shell getprop sys.boot_completed).Trim()
    if ($res -eq "1") {
        $booted = $true
        break
    }
    Start-Sleep -Seconds 2
}
Write-Host "[OK] Emulator booted successfully!" -ForegroundColor Green

if (Test-Path $ApkPath) {
    Write-Host "[3/3] Installing latest Limit Crosses build..." -ForegroundColor Yellow
    & $AdbExe install -r $ApkPath
    Write-Host "[OK] Limit Crosses installed!" -ForegroundColor Green
    Write-Host "Launching Limit Crosses on emulator screen..." -ForegroundColor Cyan
    & $AdbExe shell am start -n com.limitcrosses.app/.MainActivity
}

Write-Host ""
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  Ready! You can now test the app directly on emulator." -ForegroundColor Green
Write-Host "  App created by Pankaj Yadav" -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor Cyan

