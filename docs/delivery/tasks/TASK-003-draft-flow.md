# TASK-003: Create, view และ edit draft end-to-end

Status: Verify approved — Delivery pending
Owner: Developer
Requirement IDs: REQ-01, REQ-03, REQ-04, REQ-06, REQ-07, REQ-08, REQ-09, REQ-11
Dependencies: TASK-002

## Human approvals

| Gate | Approver | Decision | Date / revision |
| --- | --- | --- | --- |
| Requirements | Project owner | Approved | 2026-09-25 / approved requirements and Discover baseline |
| Design | Project owner acting as technical owner | Approved | 2026-09-25 / ADR-003 + TASK-003 test design |
| Implement | Project owner acting as code reviewer | Approved | 2026-09-25 / TASK-003 evidence review rounds 1–2 |
| Verify | Project owner acting as QA / acceptance owner | Approved | 2026-09-25 / user statement "QA Approved"; sign-off details in [acceptance test cases](../../quality/acceptance-test-cases.md) |
| Delivery | Release owner | Pending | — |

## Context
[แผนหลัก](../implementation-plan.md), [requirements](../../product/requirements.md), [OpenAPI](../../../contracts/openapi.yaml), [ADR-002](../../architecture/decisions/ADR-002-request-contract-and-domain.md), [ADR-003 proposal](../../architecture/decisions/ADR-003-draft-vertical-slice.md) และ [test design](../../quality/TASK-003-test-design.md)

## Scope / non-goals
สร้าง/อ่าน/แก้ draft ตั้งแต่ UI ถึง PostgreSQL; ยังไม่รวม full search และ approval workflow

## Implementation plan
1. Backend POST/GET detail/PUT: DTO validation, identity/ownership, application service, repository, transaction และ error advice
2. Owner มาจาก identity ไม่ใช่ form; update ตรวจ expected version และ DRAFT; collection item changes ต้อง bump parent version
3. Frontend role/user demo selector, new/detail/edit routes, API client; เลือก form/state libraries พร้อมเหตุผลใน ADR
4. Form dynamic items, inline/nested server errors, loading/double-submit guard, dirty warning ทั้ง navigation และ unload ที่รองรับ
5. สร้าง custom form hook; reset baseline เมื่อ save สำเร็จ เก็บค่าที่กรอกเมื่อ failure; 409 ให้เลือก reload โดยไม่ auto-overwrite

## Acceptance criteria
- [x] Create ได้ UUID/requestNumber/DRAFT/version/timestamps และ persisted items ถูกต้อง
- [x] Employee อ่าน/แก้ของคนอื่นไม่ได้; Approver อ่านได้แต่แก้ไม่ได้ตาม contract
- [x] Edit DRAFT สำเร็จและ version เปลี่ยน รวม item-only edit; non-DRAFT ปฏิเสธ
- [x] Form validation ครบ limits, dates และ equipment enums; draft items ว่างเป็นไปตาม A-02
- [x] UI เพิ่ม/ลบ items, disabled loading, error mapping, dirty warning และ success reset ใช้งานได้
- [x] Missing ID →404; stale version →409 โดยข้อมูลล่าสุดไม่เปลี่ยน
- [x] บันทึก item ล้มเหลวไม่เหลือ partial request/items

## Verification
FE behavior tests: required fields, dynamic items, duplicate submit, server errors, error preservation/success reset, 409 UX; BE tests: create/ownership/validation/update/404/409; PostgreSQL integration ตรวจ rollback และ item-only optimistic locking

## Handoff
Implement gate อนุมัติแล้ว 2026-09-25 ดู [TASK-003 evidence](../../quality/evidence/TASK-003.md); TASK-004 เริ่มได้ ส่วน Verify gate ของ TASK-003 ยังรอ QA / acceptance owner
