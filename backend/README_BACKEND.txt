NextGen mParivahan local Spring Boot API

Requirements: JDK 17 and Maven 3.9+.
From this folder run: mvn spring-boot:run
The server listens on http://localhost:8080. Health: GET /api/health.
H2 file database is stored under backend/data/ (ignored by Git).

Android emulator base URL: http://10.0.2.2:8080/api
Physical phone: change ApiClient.BASE_URL to the development PC's LAN IPv4 address.

All seed users, vehicles and licences are fictional test data. This API is a local portfolio demo, not a government service or production authentication system. Do not use real citizen data.
