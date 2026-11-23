@echo off
setlocal
cd /d "%~dp0backend"
echo Starting NextGen mParivahan STAGING API on port 8080...
echo Keep this window open while testing the Android app.
call mvn spring-boot:run "-Dspring-boot.run.profiles=staging"
pause
