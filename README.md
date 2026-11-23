# NextGen mParivahan V19 — staging-first Android project

Start with **STAGING_FIRST_README.md**. It contains extraction/import steps, staging API startup, staging APK build, demo credentials, device networking notes, a test checklist, and explicit production/live deployment gates.

Open the extracted project root in Android Studio — the folder containing `settings.gradle`, `build.gradle`, `gradlew`, `app/`, and `backend/`. Use Gradle JDK 17.

This is a fictional local demo/portfolio application. It is not an official government app and does not connect to VAHAN, SARATHI, DigiLocker, payments, OTP, police, or government databases.

Staging API: `mvn spring-boot:run -Dspring-boot.run.profiles=staging` from `backend/`.
Staging Android build: `BUILD_STAGING_APK.bat` from the project root.
