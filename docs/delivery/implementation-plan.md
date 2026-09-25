# แผน implementation ทั้งโครงการ

วันที่จัดทำ: 2026-09-25 · สถานะ: Planned (ยังไม่ได้ implement)

แหล่งอ้างอิง: [โจทย์ฉบับ revise 1](../../Home-Quiz-IT-Equipment-Request-Management_revise_1.html), [requirements](../product/requirements.md), [playbook](../playbook.md)

## เป้าหมายและขอบเขต

ส่ง repository ที่ติดตั้งและรันได้: Employee สร้าง/แก้ draft และส่งคำขอ; Approver ค้นหาและพิจารณา; backend บังคับ role, ownership, validation, state และ version; มี PostgreSQL indexes, Redis, Caffeine, automated tests และผล k6 จริง

**Required:** ฟีเจอร์ทุกข้อในโจทย์, frontend tests ≥4, backend tests ≥6, Redis และ Caffeine ทั้งคู่, k6 report, schema/migration และ README ครบ

**ส่วนเสริมที่แผนเลือกทำเพื่อให้ตรวจสอบง่าย:** OpenAPI, Flyway migration, Docker Compose สำหรับ local DB/Redis, integration tests สำหรับ concurrency/rollback/cache และ E2E smoke ขนาดเล็ก ทั้งนี้ integration tests และ Compose เป็น bonus ในโจทย์; Flyway บังคับเมื่อใช้เกณฑ์ Senior

**เลื่อนได้:** API collection, automated browser suite ขนาดใหญ่, CI automation และ deployment บน cloud จริง ไม่ทำ real authentication, inventory/stock reservation, notification, attachment หรือ AI feature ใน product เพราะโจทย์ไม่ได้กำหนด

## ลำดับและ effort

ประมาณการเป็นเวลาทำงาน ไม่ใช่คำรับประกัน รวมการเขียน tests ต่อ slice แต่ไม่รวมรอ environment หรือแก้ compatibility ที่ไม่คาดคิด

| ลำดับ | Task | ผลลัพธ์ที่ตรวจได้ | Effort |
| --- | --- | --- | --- |
| 1 | [TASK-001](tasks/TASK-001-bootstrap.md) | Toolchains และ local services เริ่มได้ | 1–2 ชม. |
| 2 | [TASK-002](tasks/TASK-002-contract-domain-data.md) | Contract, decisions, schema, domain rules | 2–3 ชม. |
| 3 | [TASK-003](tasks/TASK-003-draft-flow.md) | Create/view/edit draft end-to-end | 3–4 ชม. |
| 4 | [TASK-004](tasks/TASK-004-approval-workflow.md) | Submit/approve/reject/cancel ถูกสิทธิ์และสถานะ | 2–3 ชม. |
| 5 | [TASK-005](tasks/TASK-005-search-list.md) | Search/filter/page/sort และ list UX | 2–3 ชม. |
| 6 | [TASK-006](tasks/TASK-006-caching.md) | Redis/Caffeine พร้อม correctness evidence | 2–3 ชม. |
| 7 | [TASK-007](tasks/TASK-007-verification-delivery.md) | Final checks, k6 report และส่งมอบ | 2–4 ชม. |

รวมแผนเต็มประมาณ **14–22 ชั่วโมง** ขณะที่โจทย์แนะนำ 8–12 ชั่วโมง หากจำกัดเวลาให้ใช้ UI เรียบง่ายและลด bonus/API collection/cloud/large E2E suite ก่อน ห้ามตัด caching, k6, concurrency หรือ minimum tests แล้วอ้างว่าครบโจทย์ ให้บันทึกสิ่งที่ยังไม่ผ่านตามจริง

Dependencies: 001 → 002 → 003 → {004, 005} → 006 → 007 งาน 004/005 เป็นอิสระกันหลัง draft พร้อม แต่แผนเริ่มต้นดำเนินงานตามลำดับโดยผู้รับผิดชอบคนเดียว

## การตัดสินใจที่ใช้วางแผน

ต่อไปนี้เป็น **proposed defaults** ไม่ใช่ข้อกำหนดเพิ่มเติมจากโจทย์ TASK-002 ต้องบันทึกข้อสรุปก่อน implement; exact runtime/library versions ตรวจใน TASK-001

| ประเด็น | Default สำหรับวางแผน | เหตุผล / ข้อจำกัด |
| --- | --- | --- |
| Mock identity | X-User-Id + X-Role, users ตัวอย่างคงที่; owner_id เก็บแยกจาก email | เปลี่ยน email ไม่เปลี่ยนเจ้าของ; ใช้เฉพาะ demo ไม่ใช่ authentication จริง |
| Role | Employee ทำได้เฉพาะของตน; Approver อ่านทั้งหมดและ approve/reject เท่านั้น | ไม่เพิ่มสิทธิ์ edit/cancel ให้ Approver จากตาราง status เพียงอย่างเดียว |
| Draft validation | required fields และ item ที่มีต้อง valid; items ว่างได้จนถึง submit | เคารพ required field table และเงื่อนไข ≥1 item เมื่อ submit |
| วันที่ | business timezone Asia/Bangkok, LocalDate + injectable clock; revalidate date ตอน submit | ป้องกัน draft ที่เก็บไว้จนวันที่กลายเป็นอดีต |
| Version | PUT และทุก action รับ expected version; create เริ่ม version 0 | ตรวจ stale writes ทั้ง edit และ workflow |
| totalItems | ผลรวม quantity; หากต้องแสดงจำนวนบรรทัดใช้ชื่อ itemCount แยก | ลดความคลุมเครือของข้อมูลบน list |
| Error | 400 field/format; 403 role/ownership; 404 missing; 409 version/state; 422 business; 500 sanitized unexpected | 403 เป็น design addition; ใช้ error envelope เดียวกัน |
| Request number | unique sequence-based number พร้อมปีจาก business timezone; ยอมให้มีเลขข้าม | หลีกเลี่ยง count+1 ที่ชนเมื่อ concurrent create |
| Search | keyword case-insensitive OR ระหว่าง number/title/name แล้ว AND status/department/owner | sort createdAt + id เพื่อผล pagination คงที่; validate page/size/sort |

## State และ permission matrix

| Action | Actor | From → To | Preconditions |
| --- | --- | --- | --- |
| Create | Employee | — → DRAFT | valid fields; owner จาก mock identity |
| Edit | Employee เจ้าของ | DRAFT → DRAFT | valid fields + expected version |
| Submit | Employee เจ้าของ | DRAFT → PENDING | ≥1 item, valid date/fields + version |
| Cancel | Employee เจ้าของ | DRAFT/PENDING → CANCELLED | expected version |
| Approve | Approver | PENDING → APPROVED | expected version |
| Reject | Approver | PENDING → REJECTED | nonblank reason + expected version |
| View/list | Employee เจ้าของ / Approver | ไม่เปลี่ยนสถานะ | scope ตาม identity |

APPROVED, REJECTED, CANCELLED เป็น terminal ทุก mutation ที่เหลือปฏิเสธ รวม approve ซ้ำ Client ส่ง status มาเปลี่ยน workflow เองไม่ได้

## Data และ contract deliverables

- OpenAPI สำหรับ 8 operations ตามโจทย์: POST/GET collection, GET/PUT detail, POST submit/approve/reject/cancel พร้อม request/response/errors/examples
- equipment_requests: fields ครบตามโจทย์ + owner_id ตาม identity decision; equipment_request_items มี FK, type, quantity, specification และ timestamps
- UUID PK, unique request_number, quantity constraints, @Version ที่ parent; แก้เฉพาะ items ต้องทำให้ parent version เปลี่ยนด้วย
- Request/items อยู่ transaction เดียว; DTO mapping ภายในขอบเขตที่เหมาะสม; optimistic lock failure map เป็น 409 แม้เกิดตอน flush/commit
- Index FK, owner/status/department/createdAt ตาม query; วิเคราะห์ keyword contains-search ด้วย query plan ไม่ถือว่า B-tree ธรรมดารองรับทุก pattern
- Frontend routes ที่วางแผน: /requests, /requests/new, /requests/[id], /requests/[id]/edit; role switcher สำหรับ demo

## Cache plan

Redis: เริ่มจาก shared request detail cache; ไม่ cache paginated list ในรอบแรกเพื่อลด invalidation complexity อ่าน ownership/version จาก authoritative storage ก่อนคืน cached detail และใช้ key ที่รวม id/version เพื่อไม่คืนข้อมูลเวอร์ชันเก่า ตรวจสิทธิ์ก่อนทุกครั้ง Cache miss/outage อ่าน DB ได้โดยใช้ timeout จำกัดและไม่ซ่อน DB errors

Caffeine: reference metadata ที่เปลี่ยนน้อย เช่นรายการ department ที่ seed ไว้และ equipment options; ต้องมี path ที่ application เรียกจริง, TTL/max size และ test load/hit/expiry ไม่เพิ่ม stock/catalog CRUD นอก scope

TASK-006 ตัดสินใจ key format, TTL, serialization, after-commit invalidation และ late cache-fill race ใน ADR ทดสอบว่าการ rollback ไม่ publish ข้อมูลและผู้ใช้ต่างคนไม่เห็นข้อมูลกัน การตรวจ DB ก่อนคืน detail cache มี overhead ต้องวัดผล ไม่อ้างว่าทำให้เร็วขึ้นโดยไม่มีหลักฐาน

## Verification และ completion gates

| Gate | หลักฐานก่อนผ่าน |
| --- | --- |
| Foundation | clean install, frontend/backend build/test setup, local DB/Redis connectivity |
| Contract/domain | 8 operations, role/state/error matrix, validation boundaries, migration จากฐานว่าง |
| Functional | draft/workflow/list criteria ผ่าน; FE ≥4 และ BE ≥6; stale writes ไม่ทำลายข้อมูล |
| Reliability | real DB concurrency/rollback, cache isolation/invalidation/fallback, UI errors/races |
| Delivery | k6 ผลจริง, README clean-start rehearsal, schema, source, tests, known limitations |

k6 workload ที่เสนอ: seed 1,000 requests จากหลาย user/status, read-heavy 80% list/detail + 20% mutations โดยใช้ข้อมูลแยกต่อ iteration, smoke 1 VU ก่อน baseline 10 VUs/2 นาทีและ stress 25 VUs/2 นาที; แยก disabled/cold/warm cache ใช้ environment เดียวกัน Proposed baseline thresholds: p95 <500ms, unexpected error rate <1%, correctness checks 100% ต้อง finalize ก่อนรันและระบุ hardware/dataset ข้อผิดพลาดที่ตั้งใจทดสอบ 409 แยกจาก unexpected failures

ทุก task แนบ commands, exit codes, environment และผลจริงที่ docs/quality/evidence/ ใช้ NOT RUN หากยังไม่รัน ไม่สร้างผลทดสอบจากการคาดเดา

## ความเสี่ยงและการจัดการ

- Boot/Kotlin/JDK/test library compatibility: ตรวจและ pin ใน 001 ก่อนเริ่ม feature
- Identity/validation ambiguity: resolve A-01 ถึง A-05 ก่อน contract freeze
- Child-only updates ไม่ bump parent version: เพิ่ม targeted integration test ใน 003
- Approve/cancel พร้อมกัน: ใช้ real concurrent transactions ใน 004 ให้สำเร็จเพียงรายการเดียว
- Search ช้า/N+1: ตรวจ SQL/query plan และ page loading ใน 005 ก่อนใช้ cache กลบปัญหา
- Cache stale/cross-user leak: version-aware reads, authorization และ race tests ใน 006
- เวลาไม่พอ: ลด bonus ตาม scope ด้านบน รายงาน mandatory gaps ใน 007

## วิธีใช้แผนกับ AI

หยิบ task packet ทีละงาน → อ่าน source/contract/ADR ที่เกี่ยวข้อง → implement พร้อม tests → review diff เทียบ criteria → เก็บ evidence → อัปเดต traceability/status ก่อนเริ่มงานถัดไป การเสร็จของแผนครั้งนี้ไม่เปลี่ยน implementation tasks เป็น Done
