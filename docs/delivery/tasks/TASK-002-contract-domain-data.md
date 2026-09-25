# TASK-002: Contract, domain และ data baseline

Status: In progress — Implement
Owner: Developer
Requirement IDs: REQ-01, REQ-02, REQ-03, REQ-06, REQ-07, REQ-09
Dependencies: TASK-001

## Human approvals
| Gate | Approver | Decision | Date / revision |
| --- | --- | --- | --- |
| Requirements | Project owner | Approved | 2026-09-25 / Discover analysis Q-01 through Q-12 |
| Design | Project owner acting as technical owner | Approved | 2026-09-25 / ADR-002 + OpenAPI + data/UI/test design |
| Implement | Code reviewer | In progress | domain + migration baseline |
| Verify | QA / acceptance owner | Pending | — |
| Delivery | Release owner | Pending | — |

## Context
[แผนหลัก](../implementation-plan.md), [requirements](../../product/requirements.md), [assumptions](../../product/assumptions.md), [architecture](../../architecture/README.md)

## Scope / non-goals
ทำ contract และกฎกลางพร้อม migration; ไม่ implement UI หรือ workflow controllers ครบทั้งหมดในงานนี้

## Implementation plan
1. Resolve A-01 ถึง A-05 เป็น ADR: mock identity/role, draft validation, timezone, mutation versions, totalItems และ denied-access errors
2. สร้าง contracts/openapi.yaml ครบ 8 operations; list parameters/defaults/bounds, DTOs และ error examples รวม nested items fieldErrors
3. สร้าง domain status/type enums และ transition rules; validation ทั้ง field boundaries และ business rules ใช้ clock ที่ควบคุมใน tests ได้
4. Flyway schema: requests/items, owner, UUID, unique number, constraints/FK/indexes; number generation ไม่ใช้ count+1
5. ออกแบบ application transaction, entity/DTO mapping, optimistic locking และ exception translation

## Acceptance criteria
- [ ] Contract ครบ endpoints และ pagination metadata; version/reason/identity ระบุชัด
- [ ] State/role matrix มีทั้ง allowed และ denied cases; client status ไม่กำหนด state ของ entity
- [ ] Validation boundaries ของ name/email/department/title/purpose/date/note/type/quantity/specification ครบ
- [ ] Fresh DB migration สำเร็จและรันซ้ำไม่สร้าง schema ซ้ำ; ไม่มี production ddl-auto=create
- [ ] Domain tests ครอบคลุม transitions และ terminal states; assumptions/ADR ตรงกับ contract

## Verification
ตรวจ OpenAPI ด้วย validator ที่เลือกจริง; รัน domain tests และ migration บน local PostgreSQL ตรวจ constraints, indexes และ unique number behavior บันทึก commands/output ใน evidence

## Handoff
Design approved; domain code, Flyway migration and implementation tests are in progress
