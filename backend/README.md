# Backend

Spring Boot 4.1.1 / Kotlin 2.3.21 / Maven API สำหรับ PostgreSQL พร้อม Redis และ Caffeine dependencies สำหรับ task ถัดไป

Package root: `src/main/kotlin/com/example/equipment/`

| Directory | Responsibility |
| --- | --- |
| api | HTTP routing, DTO boundary และ exception responses |
| application | use cases, authorization และ transaction boundaries |
| domain | request state และ business rules |
| persistence | JPA aggregate และ repository access |
| configuration | application/cache wiring |

Tests: `src/test/kotlin/com/example/equipment/`; resources และ Flyway migrations: `src/main/resources/db/migration/`

## Commands

ต้องใช้ JDK 21+; Maven Wrapper รวมอยู่ใน repository

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
.\mvnw.cmd spring-boot:run
```

บน macOS/Linux ใช้ `./mvnw` แทน หลังเริ่ม PostgreSQL และ Redis จาก repository root ด้วย `docker compose up -d --wait` backend จะอ่านค่า connection จาก environment variables ใน `.env.example` หรือใช้ local defaults

TASK-003 endpoints: `POST /api/v1/equipment-requests` และ `GET/PUT /api/v1/equipment-requests/{id}` ทุก request ต้องส่ง `X-User-Id` และ `X-Role` ตั้ง `FRONTEND_ORIGIN` เพื่อเปลี่ยน allowed CORS origin จากค่าเริ่มต้น `http://localhost:3000`
