# TASK-001: Bootstrap application toolchains

Status: Done
Owner: Developer
Requirement IDs: REQ-08, REQ-09, REQ-13
Dependencies: None

## Context
- [Requirements](../../product/requirements.md)
- [Architecture](../../architecture/README.md)
- [Version decision A-07](../../product/assumptions.md)

## Human approvals
| Gate | Approver | Decision | Date / revision |
| --- | --- | --- | --- |
| Requirements | Project owner | Approved to proceed | 2026-09-25 / implementation request |
| Design | Project owner acting as technical owner | Approved to proceed | 2026-09-25 / implementation plan + ADR-001 |
| Implement | Project owner acting as code reviewer | Approved | 2026-09-25 / TASK-001 working tree and evidence |
| Verify | QA / acceptance owner | Pending | — |
| Delivery | Release owner | Pending | — |

## Scope
สร้าง Next.js/TypeScript frontend และ Spring Boot 4.x/Kotlin/Maven backend ภายในโฟลเดอร์ที่เตรียมไว้ เลือก compatible Node.js/JDK/dependency versions และเพิ่ม lockfile/Maven wrapper ตาม tooling ที่เลือก

Non-goals: business features, external deployment และผล performance

## Acceptance criteria
- [x] มี dependency manifests, pinned runtime versions และข้อสรุป A-07 ใน ADR-001
- [x] frontend แสดง starter page และ backend เริ่มทำงานโดยเชื่อม PostgreSQL ได้
- [x] มี Vitest/React Testing Library และ JUnit 5/MockK พร้อม tests ที่ถูก discover และผ่าน
- [x] README มี prerequisites, install/run/build/test commands ที่รันตรวจแล้ว
- [x] บันทึก build/test outputจริงพร้อม environment ใน evidence

## Verification
ผลคำสั่งจริงอยู่ใน [TASK-001 evidence](../../quality/evidence/TASK-001.md)

## Implementation plan
1. ตรวจ installed tools และ compatibility จาก official documentation ตอนลงมือ; pin Node.js/JDK/Next.js/Spring Boot 4.x/Kotlin/Maven และ test dependencies ที่เข้ากันได้
2. Bootstrap ในโฟลเดอร์เดิมโดยไม่ทับ README/context; เพิ่ม frontend lockfile และ Maven wrapper
3. ตั้ง lint/typecheck/build/frontend test runner และ JUnit 5 + MockK หรือ Mockito; ตรวจ test discovery จริง
4. เพิ่ม local PostgreSQL/Redis configuration, .env.example และ health checks; แผนเลือก Docker Compose เป็น bonus เพื่อรันซ้ำได้
5. เพิ่ม config profiles/DB connection ที่ไม่ใช้ production ddl-auto=create และ startup instructions
6. บันทึก toolchain ADR, actual commands และ evidence; migration baseline จะทำใน TASK-002

## Additional acceptance criteria
- [x] PostgreSQL และ Redis เริ่มและผ่าน health checks; backend เชื่อม PostgreSQL/Flyway ได้
- [x] Install/build ใช้ lockfile/wrapper; `.env.example` มีเฉพาะ local example values
- [x] Frontend และ backend test runner discover/execute อย่างละ 1 foundation test

## Handoff
Foundation ได้รับ human approval แล้ว TASK-002 สามารถเริ่มได้ ส่วน Verify/Delivery เป็น project-level gates หลัง implementation ทุก slice เสร็จ
