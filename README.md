# IT Equipment Request Management

ระบบให้ Employee สร้างและติดตามคำขออุปกรณ์ IT และให้ Approver ค้นหา อนุมัติ หรือปฏิเสธคำขอ ตาม[โจทย์ต้นฉบับ](Home-Quiz-IT-Equipment-Request-Management_revise_1.html)
พัฒนาแบบ AI-native SDLC: ทุก task มี design, human approval gate และหลักฐานทดสอบจริงใน repository (ดู [playbook](docs/playbook.md) และ [approvals](docs/governance/approvals.md))

**สถานะ:** TASK-001 ถึง TASK-009 มี Verify approval (2026-09-25). [TASK-010](docs/delivery/tasks/TASK-010-list-row-actions.md) เพิ่ม list-row actions ตาม P1 AT-33 และ automated acceptance ผ่านแล้ว; Design/Implement/Verify ของ revision นี้รอ human review. [PR #10](https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management/pull/10) merge candidate เข้า `main` ที่ `96acc641`; application revision `bb888a9` ผ่าน clean-checkout checks และ smoke ([evidence](docs/quality/evidence/TASK-013.md)); Delivery ยัง pending. [TASK-011](docs/delivery/tasks/TASK-011-assignment-token.md): source owner ยืนยัน 2026-09-26 ว่า token ถูก revoke แล้ว และ URL เดิมคืน HTTP 403 อีกครั้งจาก workspace; URL text ยังอยู่ใน Git history ให้ release owner ประเมิน.

## Environment

| Tool | Version | หมายเหตุ |
| --- | --- | --- |
| JDK | 21+ (ทดสอบบน Temurin 25.0.3, target Java 21) | ใช้ Maven Wrapper ใน `backend/` ไม่ต้องติดตั้ง Maven |
| Node.js | 24.15.0, npm 11+ | หรือรันผ่าน container `node:24.15.0-alpine` ไม่ต้องติดตั้ง Node |
| Docker | Docker Desktop / Compose v2 | PostgreSQL 17, Redis 8; Testcontainers ใน backend tests |
| k6 (optional) | 2.3.0 ผ่าน `grafana/k6:2.3.0` | ใช้เฉพาะ performance test |

Stack: Next.js 16.3.6, React 19.3.0, TypeScript 5.9.3; Spring Boot 4.1.1, Kotlin 2.3.21, Spring Web/Data JPA/Validation, Flyway; PostgreSQL 17, Redis 8, Caffeine 3.2 — เหตุผลใน [ADR-001](docs/architecture/decisions/ADR-001-toolchains.md)

## Run

### ทางลัด: ทั้งระบบใน Docker (สำหรับ test UI / QA)

ต้องมีแค่ Docker — build backend และ frontend เป็น image แล้วรันพร้อม PostgreSQL และ Redis จาก `compose.yaml` ไฟล์เดียว (project `home-quiz`):

```bash
docker compose --profile app up -d --build --wait        # http://localhost:3000/requests , API http://localhost:8080
docker compose --profile app --profile seed run --rm seed # (optional) 1,200 คำขอสำหรับทดสอบ search/pagination
docker compose --profile app down                         # หยุด (ใส่ -v เพื่อลบข้อมูล)
```

ถ้า port ชน ให้ตั้ง `FRONTEND_PORT` / `BACKEND_PORT` เช่น `FRONTEND_PORT=3300 BACKEND_PORT=8090 docker compose --profile app up -d --build --wait` Browser เรียก API ผ่าน frontend เดียวกัน (`/api/v1` → backend) จึงไม่ต้องตั้ง CORS เอง

### Development (backend/frontend บน host)

จาก repository root (PowerShell; บน macOS/Linux ใช้ `cp` และ `./mvnw`):

```powershell
Copy-Item .env.example .env          # ค่า local-only ทั้งหมด ไม่มี secret จริง
docker compose up -d --wait          # PostgreSQL :5432 + Redis :6379
cd backend
.\mvnw.cmd spring-boot:run           # http://localhost:8080 — Flyway สร้าง schema อัตโนมัติ (V1, V2)
```

อีก terminal จาก repository root:

```powershell
docker run --rm -v "${PWD}:/workspace" -w /workspace/frontend node:24.15.0-alpine npm ci
docker run --rm -it -p 3000:3000 -v "${PWD}:/workspace" -w /workspace/frontend node:24.15.0-alpine npm run dev -- --hostname 0.0.0.0
```

`.env` ถูกอ่านโดย Docker Compose เท่านั้น (ports, DB credentials) — backend และ frontend ใช้ environment variables ของ shell/container หรือค่า default ใน `application.properties` / `NEXT_PUBLIC_API_BASE_URL` ที่ชี้ `http://localhost:8080/api/v1` อยู่แล้ว ถ้า port 3000 ถูกใช้อยู่ ให้ map เป็น port อื่น (เช่น `-p 3200:3000`) และรัน backend ด้วย `FRONTEND_ORIGIN=http://localhost:3200` เพื่อให้ CORS อนุญาต

เปิด http://localhost:3000/requests แล้วเลือกผู้ใช้จำลองที่มุมขวาบน:

| ผู้ใช้ | `X-User-Id` / `X-Role` | ทำได้ |
| --- | --- | --- |
| สมชาย / สมหญิง | `employee-001`, `employee-002` / `EMPLOYEE` | สร้าง ดู แก้ DRAFT, submit, cancel (เฉพาะคำขอของตัวเอง) |
| หัวหน้าฝ่าย | `approver-001` / `APPROVER` | ดูและค้นหาทุกคำขอ, approve / reject พร้อมเหตุผล |

ไม่มี authentication จริง ตามที่โจทย์อนุญาต: identity มาจาก header เท่านั้นและ backend ตรวจ role/ownership ทุก request หยุด services ด้วย `docker compose down` (ข้อมูลยังอยู่ใน volume; ใส่ `-v` เพื่อล้าง)

Database: migration อยู่ที่ [`backend/src/main/resources/db/migration/`](backend/src/main/resources/db/migration/) (Flyway; `ddl-auto=validate` เท่านั้น) และ [data model](docs/architecture/data-model.md)

## Test

```powershell
cd backend; .\mvnw.cmd clean package      # unit + MVC + Testcontainers (ต้องเปิด Docker; ไม่มี Docker = integration tests ถูก skip)
docker run --rm -v "${PWD}:/workspace" -w /workspace/frontend node:24.15.0-alpine sh -c "npm ci && npm run lint && npm run typecheck && npm test && npm run build"
```

- Backend: JUnit 5 + MockK; MVC tests สำหรับ error envelope/precedence; Testcontainers PostgreSQL + Redis สำหรับ search, cache, rollback และ outage
- Frontend: Vitest + React Testing Library เน้นพฤติกรรมที่ผู้ใช้เห็น (form, actions, list/URL state, stale response)
- E2E / acceptance: Playwright ใน [`tests/e2e`](tests/e2e/README.md) — `bash tests/e2e/run-e2e.sh` (ต้องมีแค่ Docker) build ทั้งระบบเป็น container แล้วรัน 53 จาก 55 [acceptance test cases](docs/quality/acceptance-test-cases.md) ผ่าน browser จริง
- Performance: [k6 README](tests/performance/README.md) และผลใน [`tests/performance/results/`](tests/performance/results/)

## API

- Contract: [`contracts/openapi.yaml`](contracts/openapi.yaml) (OpenAPI 3.0.3) และ [API behavior matrix](docs/architecture/api-behavior.md)
- ทุก request ต้องมี `X-User-Id` และ `X-Role`

| Method | Path | Use |
| --- | --- | --- |
| POST | `/api/v1/equipment-requests` | สร้าง DRAFT (Employee) |
| GET | `/api/v1/equipment-requests?keyword=&status=&department=&page=0&size=10&sort=createdAt,desc` | ค้นหา/รายการ (Employee เห็นเฉพาะของตัวเอง) |
| GET / PUT | `/api/v1/equipment-requests/{id}` | ดู / แก้ DRAFT ด้วย `expectedVersion` |
| POST | `/api/v1/equipment-requests/{id}/submit\|cancel\|approve\|reject` | workflow ด้วย `expectedVersion` (reject ต้องมี `reason`) |
| GET | `/api/v1/reference-data` | department suggestions + equipment options (Caffeine) |
| GET | `/actuator/health`, `/actuator/metrics` | health และ cache metrics |

Error ทุกกรณีใช้ envelope `{timestamp, status, code, message, path, fieldErrors}`: 400 `VALIDATION_ERROR`/`MALFORMED_REQUEST`, 403 `ACCESS_DENIED`, 404 `REQUEST_NOT_FOUND`, 409 `REQUEST_VERSION_CONFLICT`/`REQUEST_STATE_CONFLICT`, 422 `ITEMS_REQUIRED`/`REJECTION_REASON_REQUIRED`/`BUSINESS_RULE_VIOLATION`, 500 `INTERNAL_ERROR` (ไม่เผย stack trace) ลำดับการตรวจ: identity → role/ownership → version → state → business rule

## Decisions

| หัวข้อ | การตัดสินใจ | ADR |
| --- | --- | --- |
| Contract / domain | Status machine ใน domain, error envelope เดียว, `expectedVersion` ทุก mutation, owner แยกจาก email | [ADR-002](docs/architecture/decisions/ADR-002-request-contract-and-domain.md) |
| Backend layers | `api` (controller/DTO/error) → `application` (use case, transaction) → `domain` (rules) → `persistence` (JPA); ไม่ส่ง entity ออก API | [ADR-003](docs/architecture/decisions/ADR-003-draft-vertical-slice.md) |
| Concurrency | JPA aggregate + parent `@Version`; แก้เฉพาะ item ก็ bump version; stale write → 409 ไม่เขียนทับ; request/items อยู่ transaction เดียว | ADR-003 |
| Workflow | action endpoint แยก 4 ตัว ใช้ template เดียว ตรวจ access → version → state → rule | [ADR-004](docs/architecture/decisions/ADR-004-approval-workflow.md) |
| Search | Specification สร้าง predicate เฉพาะ filter ที่ส่งมา, trigram index สำหรับ keyword, sort `created_at,id`, 3 SQL ต่อหน้า | [ADR-005](docs/architecture/decisions/ADR-005-search-list.md) |
| Caching | ดูหัวข้อ Caching ด้านล่าง | [ADR-006](docs/architecture/decisions/ADR-006-caching.md) |
| Performance | workload/thresholds กำหนดก่อนรัน | [ADR-007](docs/architecture/decisions/ADR-007-performance-and-delivery.md) |

### State management (frontend)

- **Server state:** TanStack Query 5 — cache ต่อ actor (`queryKey` มี user/role), ยกเลิก request เก่าด้วย `AbortSignal`, เขียน response ลง cache หลัง mutation สำเร็จ; เลือกเพราะจัดการ stale response/race ได้ในตัว
- **Form state:** React Hook Form 7 + Zod 4 ผ่าน custom hook `useEquipmentRequestForm` (dynamic items, map server `fieldErrors` รวม `items[0].quantity`, กัน submit ซ้ำ, reset เมื่อสำเร็จ, 409 เก็บค่าที่กรอกและให้ผู้ใช้เลือก reload)
- **URL state:** list parameters อยู่ใน URL เท่านั้น (`useRequestSearch`), keyword debounce 300 ms, เปลี่ยน filter กลับหน้าแรก
- **UI state:** `useState` เฉพาะ dialog/alert; derived values (actions ที่แสดง, totals) คำนวณจาก data ไม่เก็บซ้ำ
- Custom hooks อื่น: `useRequestAction`, `useDirtyWarning`, `useDebouncedUrlField`, `useDepartmentSuggestions`

### Caching

| Data | อยู่ที่ | เหตุผล |
| --- | --- | --- |
| Request detail | **Redis** `equipment:v1:request-detail:{id}:{version}`, TTL 10 นาที | เปลี่ยนบ่อยและทุก instance ต้องเห็นตรงกัน; key มี version และทุก read ตรวจ owner/version จาก PostgreSQL ก่อน จึงไม่คืน version เก่าและไม่ cache สิทธิ์; publish หลัง commit เท่านั้น |
| Reference data (departments, equipment options) | **Caffeine** local, TTL 1 ชม., max 16 | เหมือนกันทุกผู้ใช้และเปลี่ยนน้อย ยอมรับความล้าสมัยจำกัดได้ ไม่ต้องข้าม network |
| Search results | ไม่ cache | key combination มากเกินไป invalidate ทุก mutation และ query เร็วอยู่แล้ว (≤1.3 ms) |

Redis ล่ม: timeout 250 ms แล้วอ่านจาก PostgreSQL ต่อ; ปิด cache ได้ด้วย `APP_CACHE_REQUEST_DETAIL_ENABLED` / `APP_CACHE_REFERENCE_DATA_ENABLED` ดู [backend README](backend/README.md)

## Performance

k6 ตาม [ADR-007](docs/architecture/decisions/ADR-007-performance-and-delivery.md): thresholds p95 < 500 ms, p99 < 1 s, unexpected errors < 1%, checks 100% — ผลอยู่ใน [performance report](tests/performance/results/TASK-007-report.md)

## Assumptions

Assumption ที่ยืนยันแล้วอยู่ใน [assumptions](docs/product/assumptions.md) และ [Discover](docs/product/discovery.md) โดยสรุป:

- Identity จำลองผ่าน `X-User-Id` + `X-Role`; ownership ใช้ user ID ไม่ใช่ email
- Draft เก็บได้โดยยังไม่มี item; ต้องมี ≥1 item ตอน submit
- วันที่ใช้ timezone Asia/Bangkok และตรวจซ้ำตอน create/update/submit
- `totalItems` = ผลรวม quantity
- Employee cancel ได้ทั้ง DRAFT และ PENDING; Approver ไม่แก้หรือ cancel

## Known limitations

- ไม่มี authentication จริง; header identity ใช้เพื่อ demo เท่านั้น
- ไม่ได้ deploy cloud; ทดสอบบน local เท่านั้น
- ไม่เก็บผู้อนุมัติ/เวลาอนุมัติ, ไม่มี notification หรือ inventory reservation
- Department เป็น free text (มี suggestions) ไม่บังคับให้อยู่ในรายการ
- ระหว่าง Redis ล่ม detail read ช้าลงเป็น ~0.5 s และ `/actuator/health` แสดง DOWN แม้ยังให้บริการได้
- ผล k6 วัดบนเครื่องเดียว (k6, backend, DB, Redis ใช้ CPU ร่วมกัน)
- ไม่มี API collection (Postman/Bruno); ใช้ OpenAPI แทน

## Repository

```text
frontend/     Next.js app (src/app routes, src/features/equipment-requests)
backend/      Spring Boot service (api, application, domain, persistence, cache, configuration)
contracts/    OpenAPI contract
tests/        k6 workload, seed และผล performance
docs/         requirements, ADRs, task packets, test evidence, approvals
compose.yaml  PostgreSQL + Redis (default), ทั้งระบบ (profile app), seed และ Playwright (profile e2e)
```
