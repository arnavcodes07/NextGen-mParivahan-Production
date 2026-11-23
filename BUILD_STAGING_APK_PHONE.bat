@echo off
setlocal
cd /d "%~dp0"
echo ================================================
echo NextGen mParivahan - PHYSICAL PHONE STAGING BUILD
echo ================================================
echo.
echo Find your PC IPv4 address by running IPCONFIG in another CMD window.
echo Use the IPv4 address of the Wi-Fi/Ethernet adapter on the same network
set /p API_HOST=Enter your PC IPv4 address (example 192.168.1.20): 
if "%API_HOST%"=="" (
  echo No IP address entered. Build cancelled.
  pause
  exit /b 1
)
echo.
echo Building with API URL http://%API_HOST%:8080/api
call gradlew.bat clean assembleDebug "-PAPI_BASE_URL=http://%API_HOST%:8080/api"
if errorlevel 1 (
  echo.
  echo BUILD FAILED. Check the error above.
  pause
  exit /b 1
)
echo.
echo STAGING APK:
echo app\build\outputs\apk\debug\app-debug.apk
echo.
echo Keep the staging backend running and allow TCP port 8080 through
 echo Windows Firewall on a trusted PRIVATE network only.
pause
