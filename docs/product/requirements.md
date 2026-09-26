# Requirements baseline

Source: [assignment](../../Home-Quiz-IT-Equipment-Request-Management_revise_1.html) เป็น authoritative detail; ตารางนี้เป็นดัชนีสำหรับ implement และ trace tests ดู facts, acceptance criteria และคำถามฉบับเต็มใน [Discover analysis](discovery.md)

| ID | Requirement / acceptance criteria |
| --- | --- |
| REQ-01 | Employee สร้าง/ดูคำขอตนเอง, แก้ไขเฉพาะ DRAFT, submit และ cancel DRAFT/PENDING; Approver ดูทั้งหมด, search/filter, approve/reject; backend ตรวจ role และ ownership |
| REQ-02 | อนุญาต DRAFT→PENDING/CANCELLED และ PENDING→APPROVED/REJECTED/CANCELLED เท่านั้น; terminal states เปลี่ยนหรือแก้ไขไม่ได้ |
| REQ-03 | name 2–100, email ถูก format, department required, title 5–150, purpose 10–500, required date ไม่เป็นอดีต, note ≤500; item type required, quantity 1–5, specification ≤250; submit มี ≥1 item; reject มี reason |
| REQ-04 | Form เพิ่ม/ลบ item, field/server errors, loading และป้องกัน submit ซ้ำ, dirty warning, edit, เก็บข้อมูลเมื่อ error, reset เมื่อ success, conflict ไม่ overwrite อัตโนมัติ |
| REQ-05 | List มี request number/title/employee/department/required date/total items/status/created at; search number/title/name ร่วมกับ status/department, pagination metadata, created-date sort และ empty state |
| REQ-06 | REST /api/v1/equipment-requests: POST/GET collection, GET/PUT /{id}, POST /{id}/submit, /approve, /reject, /cancel; error envelope สม่ำเสมอ; 400/404/409/422/500 ตามโจทย์ |
| REQ-07 | PostgreSQL request/items, UUID PK, unique request number, FK/indexes, version optimistic locking; stale write คืน 409 ไม่ทับข้อมูลใหม่; request/items บันทึกหรือ rollback ทั้งชุด |
| REQ-08 | Next.js/React/TypeScript, functional components/hooks, form validation library, custom hook ≥1; แยก server/UI/derived state, จัดการ stale response และ cleanup |
| REQ-09 | Spring Boot 4.x/Kotlin/Maven, Web/Data JPA/Bean Validation; แยก controller/application/domain/repository/DTO/exception/configuration |
| REQ-10 | Implement Redis และ local Caffeine caching; อธิบาย data placement, TTL, invalidation และความถูกต้องเมื่อ cache ล้าสมัย |
| REQ-11 | Frontend tests ≥4 ด้วย Vitest หรือ Jest + React Testing Library; backend tests ≥6 ด้วย JUnit 5 + MockK หรือ Mockito; เน้น behavior, errors และ data integrity |
| REQ-12 | รัน k6 performance tests และส่งผลพร้อม workload/environment; ไม่อ้างผลจาก script ที่ยังไม่ได้รัน |
| REQ-13 | ส่ง Git repository ที่ผู้อื่นติดตั้ง ทดสอบ และ run ได้ พร้อม frontend/backend source, README (environment/run/test & API/decisions/scope), migration หรือ schema และ automated tests; ดู [Discover detail](req-13-discovery.md) |
| REQ-14 (new request; Design pending) | ปรับสีธีม UI โดยอ้าง palette Spark Deck 12 สีที่ผู้ใช้ให้; semantic mapping และ contrast ต้องออกแบบก่อนแก้ UI ดู [Discover detail](req-14-theme-discovery.md) |

Equipment types: NOTEBOOK, MONITOR, KEYBOARD, MOUSE, HEADSET, OTHER

Bonus ตามโจทย์: integration tests, API collection, Docker Compose; Flyway/Liquibase เป็น bonus หรือ requirement สำหรับระดับ Senior ให้ระบุขอบเขตที่เลือกใน ADR
