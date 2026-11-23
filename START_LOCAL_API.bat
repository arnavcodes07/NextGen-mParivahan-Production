@echo off
setlocal
cd /d "%~dp0backend"
echo Starting NextGen mParivahan local API on port 8080...
echo Keep this window open while using the Android app.
where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven was not found. Install Maven 3.9+ and configure JAVA_HOME to JDK 17.
  pause
  exit /b 1
)
mvn spring-boot:run
pause
