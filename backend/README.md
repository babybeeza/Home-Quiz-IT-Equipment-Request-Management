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

Endpoints: `GET/POST /api/v1/equipment-requests` (list/search และ create), `GET/PUT /api/v1/equipment-requests/{id}` และ `POST /api/v1/equipment-requests/{id}/submit|cancel|approve|reject` ทุก request ต้องส่ง `X-User-Id` และ `X-Role` ตั้ง `FRONTEND_ORIGIN` (คั่นหลาย origin ด้วย comma ได้) เพื่อเปลี่ยน allowed CORS origin จากค่าเริ่มต้น `http://localhost:3000`

`EquipmentRequestSearchIntegrationTest` ใช้ Testcontainers (`postgres:17-alpine`) จึงต้องเปิด Docker ระหว่าง `test`/`package` ถ้าไม่มี Docker test ชุดนี้จะถูก skip (รายงานใน `Skipped`) ซึ่งต้องถือเป็น NOT RUN ใน evidence

## Caching (ADR-006)

| Cache | Data | Key / name | TTL | Switch |
| --- | --- | --- | --- | --- |
| Redis (shared) | Request detail | `equipment:v1:request-detail:{id}:{version}` | `APP_CACHE_REQUEST_DETAIL_TTL` (10m) | `APP_CACHE_REQUEST_DETAIL_ENABLED` |
| Caffeine (per instance) | `GET /api/v1/reference-data` (departments + equipment options) | `reference-data`, max 16 | `APP_CACHE_REFERENCE_DATA_TTL` (1h) | `APP_CACHE_REFERENCE_DATA_ENABLED` |

- **Detail reads:** before touching Redis, every detail read looks up `owner_id` and `version` in PostgreSQL. Permission is always decided by the database, and only the entry for the current version is used.
- **Mutations:** each successful mutation writes the new version and deletes the previous one only after commit.
- **Redis timeouts:** `SPRING_DATA_REDIS_TIMEOUT` and `SPRING_DATA_REDIS_CONNECT_TIMEOUT` both default to 250ms. If Redis fails, reads fall back to PostgreSQL.
- **Metrics:** `GET /actuator/metrics/equipment.cache.request_detail?tag=result:hit|miss|error` and `GET /actuator/metrics/cache.gets?tag=cache:reference-data&tag=result:hit|miss`.
- **Cold start (for k6):**
  - Redis: `docker compose exec redis redis-cli --scan --pattern 'equipment:v1:request-detail:*' | xargs -r docker compose exec -T redis redis-cli del`, or `FLUSHDB` on the local instance.
  - Caffeine: restart the backend.
- **Warm start:** read the target details or reference data once before measuring.
- **Disabled run:** set both `*_ENABLED=false`.
- **Health:** overall `/actuator/health` reports DOWN while Redis is unavailable, even though requests keep being served from PostgreSQL.
