# Local REST API (Spring Boot / H2)

Base URL on the host PC: `http://localhost:8080/api`
Android Studio emulator: `http://10.0.2.2:8080/api`

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/health` | API health |
| POST | `/auth/login` | Demo login `{mobile, mpin}` |
| POST | `/auth/register` | Demo registration `{name, mobile, email}` |
| POST | `/auth/generate-mpin` | Demo MPIN reset `{mobile}` |
| GET | `/services` | Service catalog |
| GET | `/vehicles/{registration}` | Fictional vehicle lookup |
| GET | `/licences/{number}` | Fictional licence lookup |
| GET / PUT | `/profile/{mobile}` | Read/update demo profile |
| POST / GET | `/complaints` / `/complaints/{mobile}` | Create/list complaints |
| POST / GET | `/transactions` / `/transactions/{mobile}` | Create/list activity |
| GET | `/people` | Fictional people/vehicle directory |
| GET | `/nearby` | Nearby service categories |
| GET | `/emergency` | Emergency dial numbers |

This API persists records in local H2. Seeded records are fictional. It is not connected to VAHAN, SARATHI, DigiLocker, police, payment, OTP, or other government systems.
