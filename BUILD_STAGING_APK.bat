@echo off
setlocal
cd /d "%~dp0"
echo Building staging APK with Android emulator API URL...
call gradlew.bat clean assembleDebug -PAPI_BASE_URL=http://10.0.2.2:8080/api
if errorlevel 1 (
  echo BUILD FAILED. Check the error above.
  pause
  exit /b 1
)
echo.
echo STAGING APK:
echo app\build\outputs\apk\debug\app-debug.apk
pause
