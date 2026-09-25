# Backend

Spring Boot 4.1.1 / Kotlin 2.3.21 / Maven application foundation สำหรับ PostgreSQL, Redis และ Caffeine

Package root: `src/main/kotlin/com/example/equipment/`

| Directory | Responsibility |
| --- | --- |
| controller | HTTP routing และ DTO boundary |
| application | use cases, authorization และ transaction boundaries |
| domain | request state และ business rules |
| repository | persistence access |
| dto | request/response models และ mapping |
| exception | consistent error responses |
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
