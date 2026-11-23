@echo off
cd /d "%~dp0"
echo Checking Gradle project root...
if exist settings.gradle (echo PASS: settings.gradle found) else (echo FAIL: settings.gradle missing)
if exist build.gradle (echo PASS: build.gradle found) else (echo FAIL: build.gradle missing)
if exist app\build.gradle (echo PASS: app\build.gradle found) else (echo FAIL: app\build.gradle missing)
pause
