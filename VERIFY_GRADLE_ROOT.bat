@echo off
setlocal
if exist "%~dp0settings.gradle" if exist "%~dp0build.gradle" if exist "%~dp0app" (
  echo.
  echo [OK] This is the correct Android Studio / Gradle project root.
  echo.
  echo settings.gradle : FOUND
  echo build.gradle    : FOUND
  echo app folder      : FOUND
  echo.
  echo Open THIS folder in Android Studio.
  pause
  exit /b 0
)
echo.
echo [ERROR] This is NOT the Gradle project root.
echo.
echo Android Studio must be opened at the folder containing settings.gradle and build.gradle.
echo.
for /d %%D in ("%~dp0*") do (
  if exist "%%~fD\settings.gradle" if exist "%%~fD\build.gradle" echo Correct project folder: %%~fD
)
pause
exit /b 1
