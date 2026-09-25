# Discover: requirement analysis

Status: Approved
Date: 2026-09-25
Source: [Home Quiz — IT Equipment Request Management revise 1](../../Home-Quiz-IT-Equipment-Request-Management_revise_1.html)

เอกสารนี้แยกข้อกำหนดที่โจทย์ระบุจริงออกจาก assumption และคำถาม ห้ามถือข้อเสนอในหัวข้อ Open questions เป็น requirement จนกว่าจะได้รับ human approval

## Product outcome

พัฒนา web application ให้พนักงานส่งคำขออุปกรณ์ IT และให้ผู้อนุมัติตรวจสอบ อนุมัติ หรือปฏิเสธ โดย backend เป็นผู้บังคับใช้ validation, สิทธิ์, state transition และ optimistic concurrency เสมอ

## Actors and observable outcomes

| Actor | Facts from assignment | Acceptance criteria |
| --- | --- | --- |
| Employee | สร้างคำขอ, ดูของตนเอง, แก้เฉพาะ DRAFT, submit และ cancel คำขอที่ยังไม่ approved | Given Employee คนหนึ่ง เมื่อ list/view ต้องไม่เห็นข้อมูล Employee คนอื่น; create ได้ DRAFT; edit สำเร็จเฉพาะ DRAFT; submit DRAFT ที่ valid เป็น PENDING; cancel DRAFT/PENDING เป็น CANCELLED |
| Approver | ดูทั้งหมด, search/filter, approve และ reject พร้อมเหตุผล | Given Approver เมื่อ list/view เห็นทุกคำขอ; approve สำเร็จเฉพาะ PENDING; reject PENDING ต้องมี reason; action ที่ state ไม่ถูกต้องถูกปฏิเสธ |

Authentication จริงอยู่นอกขอบเขต โจทย์อนุญาต role selector หรือ request header แต่ยังไม่ระบุวิธีแทน user identity สำหรับ ownership

## Functional requirements and acceptance criteria

### REQ-01 — Roles and ownership

- Employee สร้าง request และเข้าถึง request ของตนเอง
- Employee แก้ได้เฉพาะ DRAFT และ cancel ได้เมื่อยังไม่ approved
- Approver อ่านทั้งหมด, search/filter, approve/reject
- UI ซ่อน action ที่ใช้ไม่ได้ แต่ direct API call ต้องถูก backend ตรวจซ้ำ

Acceptance:

- Given Employee A, when requesting Employee B's data or mutation directly, then backend denies it and data remains unchanged; exact HTTP status is an open decision.
- Given an invalid role/action combination, when calling the API directly, then backend denies it regardless of UI state.

### REQ-02 — State machine

Allowed transitions stated by the assignment:

```text
DRAFT   -> PENDING
DRAFT   -> CANCELLED
PENDING -> APPROVED
PENDING -> REJECTED
PENDING -> CANCELLED
```

APPROVED, REJECTED และ CANCELLED เป็น terminal states แก้ไขหรือ transition ต่อไม่ได้ และ approve ซ้ำไม่ได้

Acceptance:

- ทุก allowed transition ให้สถานะปลายทางถูกต้อง
- ทุก transition อื่นคืน conflict/business error ตาม contract และไม่เปลี่ยนข้อมูล/version โดยไม่ตั้งใจ

### REQ-03 — Request and item validation

| Field | Fact from assignment |
| --- | --- |
| employeeName | required, 2–100 chars |
| employeeEmail | required, valid email |
| department | required; ไม่มี max length ระบุ |
| title | required, 5–150 chars |
| purpose | required, 10–500 chars |
| requiredDate | required, must not be in the past |
| additionalNote | optional, max 500 chars |
| items | at least 1 before submit |
| equipmentType | required; NOTEBOOK, MONITOR, KEYBOARD, MOUSE, HEADSET, OTHER |
| quantity | required, 1–5 per item |
| specification | optional, max 250 chars |
| rejectionReason | required on reject; ไม่มี max length ระบุ |

Acceptance:

- Frontend แสดง field error ใกล้ field และ backend ตรวจ rules เดียวกันซ้ำ
- Submit ที่ไม่มี item, requiredDate เป็นอดีต, item quantity นอก 1–5 หรือ reject ไม่มี reason ต้องล้มเหลวโดยไม่เปลี่ยน state
- Boundary lengths ที่ min/max ต้องผ่าน; ต่ำ/สูงกว่าหนึ่งหน่วยต้องไม่ผ่าน

### REQ-04 — Request form behavior

- Dynamic add/remove item, create/edit mode, field and cross-field validation
- Loading state และป้องกัน submit ซ้ำ
- API error ต้องเก็บค่าที่ผู้ใช้กรอก; success จึง reset ตาม flow
- Dirty state และ warning ก่อนออกจาก form ที่ยังไม่บันทึก
- Map server `fieldErrors` กลับไปยัง field
- Version conflict แสดงข้อความและไม่ overwrite ข้อมูลใหม่อัตโนมัติ

Acceptance ใช้พฤติกรรมที่ผู้ใช้สังเกตได้ ครอบคลุม loading, error และ success ไม่ผูกกับ implementation detail

### REQ-05 — List, search and pagination

- Columns: requestNumber, title, employeeName, department, requiredDate, totalItems, status, createdAt
- Keyword ค้น request number/title/employee name; ใช้ร่วมกับ status และ department filters
- Pagination มี content/page/size/totalElements/totalPages, empty state และ sort ตาม created date
- โจทย์แนะนำเก็บ query state ใน URL แต่ไม่ได้บังคับถ้อยคำว่า must

Acceptance:

- Search + filters ใช้พร้อมกันและคืนผลเฉพาะ actor scope
- Empty result มี metadata ถูกต้อง; เปลี่ยน query ไม่ถูก stale response เก่าเขียนทับ
- จำนวนและความหมายของ `totalItems`, default page size และ allowed sort เป็น open decisions

### REQ-06 — REST API and errors

Base path `/api/v1/equipment-requests` และ operations ขั้นต่ำ 8 รายการ:

| Method | Path | Outcome |
| --- | --- | --- |
| POST | `/` | create DRAFT |
| GET | `/` | search/list |
| GET | `/{id}` | detail |
| PUT | `/{id}` | edit DRAFT with current version |
| POST | `/{id}/submit` | DRAFT → PENDING |
| POST | `/{id}/approve` | PENDING → APPROVED |
| POST | `/{id}/reject` | PENDING → REJECTED with reason |
| POST | `/{id}/cancel` | DRAFT/PENDING → CANCELLED |

Error envelope ต้องสม่ำเสมอ มี timestamp/status/code/message/path/fieldErrors โจทย์กำหนด 400 malformed/field validation, 404 missing, 409 version/state conflict, 422 business rule และ 500 unexpected แต่ไม่ได้กำหนด authorization status

### REQ-07 — Persistence, transaction and concurrency

- PostgreSQL, UUID primary keys, unique request number, item foreign key และ indexes สำหรับ field ที่ค้นบ่อย
- Optimistic locking ผ่าน version; stale update คืน 409 และไม่ overwrite latest data
- Request/items อยู่ใน transaction เดียว ถ้า item save fail ต้อง rollback ทั้งชุด
- Production migration ห้ามใช้ `ddl-auto=create`; migration script/schema เป็น deliverable ส่วน Flyway/Liquibase เป็น bonus หรือ Senior requirement

Acceptance ต้องตรวจทั้ง HTTP response และ persisted state หลัง failure รวม concurrent user scenario จาก version 1 → version 2

### REQ-08 / REQ-09 — Technology and separation

- Frontend: Next.js, React, TypeScript, functional components/hooks, form validation library, Vitest หรือ Jest + React Testing Library
- React: custom hook อย่างน้อยหนึ่งตัว, แยก server/UI/form/derived state, immutable update, dependencies/cleanup/stale-response/race handling
- Backend: Spring Boot 4.x, Kotlin, Maven, Spring Web, Data JPA, Bean Validation
- แยก controller/application/domain/repository/DTO/exception/configuration; controller ไม่มี business logic; ห้ามส่ง entity เป็น API response
- Mapping responsibility, transaction boundary และ exception handling ต้องชัด

### REQ-10 — Caching

ต้อง implement ทั้ง Redis และ local Caffeine พร้อมอธิบายว่า data ใดอยู่ cache ใด โจทย์ไม่ได้กำหนด keys, TTL, invalidation, failure mode หรือ target hit rate จึงต้องตัดสินใจใน Design

Acceptance อย่างน้อยต้องพิสูจน์ว่าทั้งสอง cache มี read path ใช้งานจริง และ mutation/authorization ไม่คืนข้อมูลผิดหรือข้อมูลข้ามผู้ใช้

### REQ-11 / REQ-12 — Tests and performance

- Frontend tests อย่างน้อย 4 cases ด้วย RTL เน้น user behavior
- Backend tests อย่างน้อย 6 cases ด้วย JUnit 5 + MockK/Mockito เน้น business/state/error/data integrity
- Integration tests เป็น bonus แต่ concurrency/rollback ต้องมีหลักฐานที่พิสูจน์ผล persisted state
- k6 ต้องรันและส่งผล โจทย์ไม่ได้กำหนด workload, dataset, VUs, duration หรือ thresholds

### REQ-13 — Delivery

Required: Git repository, frontend/backend source, README, migration/schema และ automated tests README ต้องมี prerequisites/runtime versions, run frontend/backend/database, tests/API docs, architecture/state decisions, assumptions และ known limitations

Bonus: API collection และ Docker Compose

## Explicit non-goals

โจทย์ไม่ได้ขอ real authentication, equipment inventory/stock reservation, procurement, notification, attachment, audit UI, cloud deployment หรือ AI feature สิ่งเหล่านี้ไม่อยู่ใน scope เว้นแต่เจ้าของงานเพิ่ม requirement

## Open questions requiring human decision

| ID | Question | Why it matters | Proposed default (not approved) |
| --- | --- | --- | --- |
| Q-01 | จำลอง user identity อย่างไร นอกเหนือจาก role? | ownership/list isolation | **Approved:** `X-User-Id` + `X-Role`; owner ID แยกจาก email |
| Q-02 | Create DRAFT ต้องกรอก scalar required fields ครบหรือบันทึก draft ไม่ครบได้? | DTO/validation/form UX | **Approved:** scalar fields ต้อง valid; items ว่างได้จน submit |
| Q-03 | “วันที่ไม่เป็นอดีต” ใช้ timezone ใด และ revalidate ตอน submit หรือไม่? | midnight boundary/stored drafts | **Approved:** Asia/Bangkok และ revalidate create/update/submit |
| Q-04 | Action submit/approve/reject/cancel ต้องส่ง expected version หรือเฉพาะ PUT? | concurrent workflow safety | **Approved:** ทุก mutation ส่ง expectedVersion |
| Q-05 | `totalItems` คือจำนวนแถวหรือผลรวม quantity? | list/API consistency | **Approved:** ผลรวม quantity |
| Q-06 | Employee เข้าถึงของคนอื่นควรเป็น 403 หรือ 404? | error contract/resource disclosure | **Approved:** 403 สำหรับ take-home demo |
| Q-07 | Approver cancel ได้หรือ Employee owner เท่านั้น? | permission matrix; action table ไม่ระบุ role | **Approved:** Employee owner เท่านั้น |
| Q-08 | department/rejection reason มี max length เท่าใด? | API/schema constraints | **Approved:** department 100, reason 500 |
| Q-09 | Request number ต้อง reset sequence ทุกปีหรือไม่? | concurrency/format/migration | **Approved:** ไม่ reset; format ด้วยปีปัจจุบันและ global sequence |
| Q-10 | Default/max page size และ sort whitelist คืออะไร? | API safety and deterministic paging | **Approved:** default 10, max 100, createdAt only |
| Q-11 | Cache correctness/fallback contract คืออะไร? | stale/cross-user/outage behavior | **Approved:** Redis detail cache; Caffeine reference metadata; DB authoritative |
| Q-12 | k6 success thresholds/workload คืออะไร? | pass/fail cannot be inferred after results | **Approved:** finalize before TASK-007 run |

## Discover exit criteria

- [x] Source requirements have stable IDs and observable acceptance criteria
- [x] Mandatory, recommended and bonus items are separated
- [x] Assumptions are not presented as facts
- [x] Open questions identify downstream impact and proposed defaults
- [x] Product owner approved the requirement baseline and defaults for Q-01 through Q-12 on 2026-09-25
