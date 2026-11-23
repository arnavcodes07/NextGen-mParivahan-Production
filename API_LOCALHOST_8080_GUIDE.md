# NextGen mParivahan V19 — local API setup

This build uses the bundled Spring Boot + H2 REST API as the application data source. Android login, registration, MPIN reset, vehicle search, driving-licence demo search, profile, profile update, complaints, complaint history, transaction history, people directory, nearby-service categories, emergency contacts and API health checks call the API. The app no longer silently falls back to the old phone-only SQLite demo data.

All seeded records are fictional portfolio/demo data. This is not an official VAHAN/SARATHI, DigiLocker, police, payment or government integration.

## Requirements
- JDK 17
- Maven 3.9 or newer
- Android Studio with Android SDK 35
- Project Gradle Wrapper: Gradle 8.9; Android Gradle Plugin 8.7.3; Java source/target 17

## Start the API on Windows
1. Extract the project ZIP.
2. Open PowerShell in the extracted project root.
3. Run:

```powershell
cd backend
mvn spring-boot:run
```

Keep this terminal running. Wait until Spring Boot reports that Tomcat started on port 8080. H2 database files are stored in `backend/data/`.

You can also double-click `START_LOCAL_API.bat` from the project root; it starts Maven in a terminal window.

## Verify the API
Open another PowerShell window and run:

```powershell
Invoke-RestMethod http://localhost:8080/api/health

$body = @{ mobile = '9560559548'; mpin = '8554' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login -ContentType 'application/json' -Body $body

Invoke-RestMethod http://localhost:8080/api/vehicles/DL01AB1000
```

Demo login:
- Mobile: `9560559548`
- MPIN: `8554`
- Alternative: `9876543210` / `1234`

Demo vehicle: `DL01AB1000`
Demo driving licence: `DL-DEMO-2021-014`

## Android emulator connection
`app/src/main/java/com/nextgen/mparivahan/ApiClient.java` uses:

```java
public static final String BASE_URL = "http://10.0.2.2:8080/api";
```

This is correct for the standard Android Studio emulator. The backend must be running on the same computer. Do not use `localhost` or `127.0.0.1` in the Android emulator URL.

## Physical Android phone connection
Connect the phone and computer to the same Wi-Fi. Run `ipconfig` on Windows and find the computer's IPv4 address. Change `BASE_URL` to `http://YOUR_PC_IPV4:8080/api`. Allow Java/Maven through Windows Defender Firewall on Private networks. Do not expose this demo server to public networks.

## Android Studio import
1. Extract the ZIP.
2. Open the project root containing `settings.gradle`, `build.gradle`, `gradlew`, `app/` and `backend/`.
3. Set **Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK** to **JDK 17**.
4. Select the Gradle Wrapper and Sync.
5. Keep the API terminal running, start an emulator, and run the app.

## API endpoints
- `GET /api/health`
- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/generate-mpin`
- `GET /api/services`
- `GET /api/vehicles/{registration}`
- `GET /api/licences/{number}`
- `GET /api/profile/{mobile}` / `PUT /api/profile/{mobile}`
- `POST /api/complaints` / `GET /api/complaints/{mobile}`
- `POST /api/transactions` / `GET /api/transactions/{mobile}`
- `GET /api/people`
- `GET /api/nearby`
- `GET /api/emergency`

## Troubleshooting
- If `localhost:8080/api/health` fails on the PC, the Spring Boot API is not running or port 8080 is unavailable.
- If PC health works but Android cannot connect, verify the emulator URL is `10.0.2.2`, keep the backend terminal open, and check Windows Firewall.
- `HTTP 401` means the API was reached but the login credentials were rejected.
- `HTTP 404` for a vehicle/licence means the API has no matching fictional demo record.
- Check Android Studio Logcat for `AndroidRuntime` and the first exception if the app itself crashes.

## Scope and security
This API uses a local H2 database and seeded demonstration credentials. MPIN reset returns a demo MPIN to the client and is not production-secure. Do not use real citizen data. Before any real deployment, use HTTPS, secure password hashing, proper authentication/session tokens, authorization, audit logging, and approved government/provider integrations. The app's emergency contact and exported session metadata remain on the device; business records handled by the listed flows are stored in the local API database. File attachments are selected in the UI but are not uploaded as binary files by this demo API.
