# Architecture baseline

โจทย์กำหนด Next.js frontend → REST API → Spring Boot application/domain → JPA → PostgreSQL

- `frontend/src/app`: routing/pages/layout; `features/equipment-requests`: form/list/components/hooks/API mapping; `shared`: reusable UI/utilities
- `backend/.../controller`: HTTP/DTO boundary; `application`: use cases และ transactions; `domain`: state rules; `repository`: persistence; `dto`: request/response; `exception`: error mapping; `configuration`: wiring
- `backend/src/main/resources/db/migration`: migration files หลังเลือก tooling; ห้ามใช้ production ddl-auto=create
- `contracts`: API specification และ fixtures ที่ frontend/backend ใช้ร่วมกัน

## Decisions to capture

1. Mock identity/role, ownership และ error mapping ของ denied access
2. Request/item schema, unique number allocation, indexes ตาม query, transaction และ optimistic locking
3. Form validation, server state, race cancellation, conflict UX และ error mapping
4. Redis สำหรับข้อมูลที่ต้องแชร์หลาย instance; Caffeine สำหรับ reference data ที่เปลี่ยนน้อย — เป็นข้อเสนอ ต้องสรุป keys/TTL/eviction/fallback ใน ADR
5. Cache ต้องไม่ข้าม authorization หรือใช้แทน authoritative version/state checks; ระบุ invalidation หลัง transaction commit และทดสอบ cross-user isolation

Technology versions ถูกกำหนดใน ADR-001 และ contract/domain baseline ถูกกำหนดใน ADR-002 งานแต่ละ slice ต้องมี decision ที่เกี่ยวข้องก่อน implement

## TASK-002 approved design

- [ADR-002](decisions/ADR-002-request-contract-and-domain.md)
- [API behavior matrix](api-behavior.md)
- [Data model](data-model.md)
- [Frontend interaction design](ui-flow.md)
- [OpenAPI contract](../../contracts/openapi.yaml)
- [Design verification plan](../quality/TASK-002-test-design.md)

## TASK-003 proposed design

- [ADR-003 draft vertical slice](decisions/ADR-003-draft-vertical-slice.md)
- [Frontend interaction design](ui-flow.md)
- [TASK-003 verification design](../quality/TASK-003-test-design.md)
