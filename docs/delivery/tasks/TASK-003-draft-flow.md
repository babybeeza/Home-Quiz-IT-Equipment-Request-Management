# TASK-003: Create, view และ edit draft end-to-end

Status: In progress — Implement
Owner: Developer
Requirement IDs: REQ-01, REQ-03, REQ-04, REQ-06, REQ-07, REQ-08, REQ-09, REQ-11
Dependencies: TASK-002

## Human approvals

| Gate | Approver | Decision | Date / revision |
| --- | --- | --- | --- |
| Requirements | Project owner | Approved | 2026-09-25 / approved requirements and Discover baseline |
| Design | Project owner acting as technical owner | Approved | 2026-09-25 / ADR-003 + TASK-003 test design |
| Implement | Code reviewer | In progress | — |
| Verify | QA / acceptance owner | Pending | — |
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
- [ ] Create ได้ UUID/requestNumber/DRAFT/version/timestamps และ persisted items ถูกต้อง
- [ ] Employee อ่าน/แก้ของคนอื่นไม่ได้; Approver อ่านได้แต่แก้ไม่ได้ตาม contract
- [ ] Edit DRAFT สำเร็จและ version เปลี่ยน รวม item-only edit; non-DRAFT ปฏิเสธ
- [ ] Form validation ครบ limits, dates และ equipment enums; draft items ว่างเป็นไปตาม A-02
- [ ] UI เพิ่ม/ลบ items, disabled loading, error mapping, dirty warning และ success reset ใช้งานได้
- [ ] Missing ID →404; stale version →409 โดยข้อมูลล่าสุดไม่เปลี่ยน
- [ ] บันทึก item ล้มเหลวไม่เหลือ partial request/items

## Verification
FE behavior tests: required fields, dynamic items, duplicate submit, server errors, error preservation/success reset, 409 UX; BE tests: create/ownership/validation/update/404/409; PostgreSQL integration ตรวจ rollback และ item-only optimistic locking

## Handoff
Design approved; implementation follows ADR-003 and the approved test design
