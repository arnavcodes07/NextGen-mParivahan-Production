# NextGen mParivahan V19 — Staging-first package

## What this package is
This is a staging preparation of the supplied V19 Android + Spring Boot project. It preserves the existing app and demo API. It is **not** a production release and is **not** connected to official VAHAN/SARATHI, DigiLocker, payment, OTP, police, or government systems.

## Phase 1 — run staging locally

### Requirements
- Android Studio with Android SDK Platform 35
- Gradle JDK 17
- JDK 17 and Maven 3.9+ for the backend
- Android emulator, or a physical Android phone on the same Wi-Fi as the development PC

### A. Start staging API
1. Extract this ZIP.
2. Open the extracted root folder in Android Studio (the folder containing `settings.gradle`).
3. In Windows Explorer, double-click `START_STAGING_API.bat`, or run from the backend folder:
   `mvn spring-boot:run -Dspring-boot.run.profiles=staging`
4. Verify in a browser on the PC: `http://localhost:8080/api/health`.
5. Staging data is stored separately in `backend/data/mparivahan-staging.*`.

### B. Build and run Android staging app
1. Set Android Studio Gradle JDK to 17.
2. Sync the project.
3. Double-click `BUILD_STAGING_APK.bat`, or run:
   `gradlew.bat clean assembleDebug -PAPI_BASE_URL=http://10.0.2.2:8080/api`
4. The APK is at `app/build/outputs/apk/debug/app-debug.apk`.
5. Install/run it on an Android Studio emulator.

For a **physical phone**, replace `10.0.2.2` with the development PC's LAN IPv4 address, for example `http://192.168.1.20:8080/api`; the example IP is not your actual address. Allow inbound TCP port 8080 in Windows Firewall only on a trusted private network. Do not use `localhost` on the phone.

### Demo credentials and records
- Mobile `9560559548`, MPIN `8554`
- Mobile `9876543210`, MPIN `1234`
- Demo vehicle `DL01AB1000`
- Demo driving licence `DL-DEMO-2021-014`

All records are fictional. Do not enter real citizen or vehicle data.

## Phase 1 staging test checklist
- [ ] `GET /api/health` returns `status: UP`
- [ ] Android app launches without a black screen
- [ ] Login succeeds with a demo account
- [ ] Invalid mobile / MPIN is rejected
- [ ] Mobile accepts exactly 10 digits and MPIN exactly 4 digits
- [ ] Vehicle and driving-licence demo lookup work
- [ ] Profile read/update works
- [ ] Complaint create/list works
- [ ] Transaction history works
- [ ] Drawer, bottom navigation, back button and every screen control are tested
- [ ] Test on at least one emulator and one physical phone
- [ ] Capture Logcat errors and screenshots for any failed screen

## Important staging findings / known limitations
- The supplied backend is a demo API. Login compares a four-digit MPIN directly in the database; there is no secure password hashing, session/token authorization, role-based access, rate limiting, account lockout, audit trail, or OTP verification. It must not be exposed to the public internet.
- Registration and MPIN reset return the generated MPIN in the API response. This is demo-only and must be redesigned before production.
- CORS is permissive. Android cleartext HTTP is enabled only for the debug build; the release manifest disables cleartext. A release build must be supplied an HTTPS `API_BASE_URL` and a hardened backend.
- Nearby services are static demo categories, not GPS-based live results. Emergency numbers are static. Complaint submission is saved only to this demo database and is not sent to an authority. File attachment upload is not implemented.
- No official government data or integrations are included.
- Existing app screens are implemented in a large Java Activity; a proper production refactor into screen components, repositories, lifecycle-aware state, and automated UI tests remains future work.

## Phase 2 — production preparation (NOT approved/complete)
A production profile template and `.env.production.example` are included. They are placeholders only. Do not activate it or publish the app yet. Before production, implement and test:
1. Secure authentication (hash MPIN/password, issue expiring access/refresh tokens, revoke sessions, rate limit and lock accounts).
2. Authorization on every personal-data endpoint; never trust a mobile number supplied by the client as identity.
3. PostgreSQL schema migrations, backups, restore test, encryption, retention and privacy policy.
4. Remove demo seed credentials/data and rotate all secrets.
5. HTTPS/TLS, restrictive CORS, security headers, request validation, logging/monitoring and vulnerability scanning.
6. Android release signing, secrets management, privacy disclosures, accessibility and device compatibility testing.
7. Independent security, legal and data-protection review; confirm rights/approval for any official government integration or branding.
8. Build a release APK/AAB only with an HTTPS endpoint, e.g. `gradlew.bat bundleRelease -PAPI_BASE_URL=https://YOUR_DOMAIN/api`; configure and protect the signing key outside this ZIP.

## Phase 3 — live deployment (NOT performed)
No cloud provider, domain, database host, credentials, signing key, or deployment approval was supplied. This package does not deploy itself. Once Phase 1 passes and production hardening is complete, deployment can be planned for your chosen hosting provider. Never put server passwords, private keys, keystores, or production secrets in this ZIP or source control.


## V19 staging drawer / physical-phone update
- The drawer panel's internal top padding is now 4dp (previously 20dp) to remove the apparent gap above its profile header. The Android content area still respects the system status bar.
- For a physical phone, run `BUILD_STAGING_APK_PHONE.bat` and enter the development PC's LAN IPv4 address. Do not use `localhost` or `10.0.2.2` on a physical phone.
- Start the staging backend with `START_STAGING_API.bat`, ensure phone and PC share a trusted Wi-Fi network, and allow inbound TCP 8080 in Windows Firewall for Private networks only.
- This is a focused staging update, not a claim that all app functions are production-ready or glitch-free. See `TESTING_STATUS.txt` for verification limits.
