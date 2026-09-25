# TASK-004: Submit, approve, reject และ cancel

Status: Verify approved — Delivery pending
Owner: Developer
Requirement IDs: REQ-01, REQ-02, REQ-03, REQ-04, REQ-06, REQ-07, REQ-11
Dependencies: TASK-003 (Implement approved)

## Human approvals

| Gate | Approver | Decision | Date / revision |
| --- | --- | --- | --- |
| Requirements | Project owner | Approved | 2026-09-25 / approved requirements and Discover baseline |
| Design | Project owner acting as technical owner | Approved | 2026-09-25 / ADR-004 + TASK-004 test design |
| Implement | Project owner acting as code reviewer | Approved | 2026-09-25 / TASK-004 evidence |
| Verify | Project owner acting as QA / acceptance owner | Approved | 2026-09-25 / user statement "QA Approved"; sign-off details in [acceptance test cases](../../quality/acceptance-test-cases.md) |
| Delivery | Release owner | Pending | — |

## Context
[แผนหลักและ state matrix](../implementation-plan.md), [requirements](../../product/requirements.md), contract/domain จาก TASK-002, [ADR-004](../../architecture/decisions/ADR-004-approval-workflow.md), [test design](../../quality/TASK-004-test-design.md) และ [design evidence](../../quality/evidence/TASK-004-design.md)

## Implementation plan
1. เพิ่ม action endpoints 4 ตัว รับ version และ reject reason ตาม contract
2. ใช้ application transactions ตรวจ role/owner → current version/state → business rules → persist; กำหนด error precedence ไว้ใน tests
3. Revalidate date/items ตอน submit; reject reason trim แล้วห้ามว่าง; ปฏิเสธ terminal mutations
4. UI action controls ตาม role + state, reject dialog, loading/errors, refetch/invalidate หลัง success
5. ตรวจ optimistic conflict ตอน flush/commit และ concurrent approve/cancel โดยไม่ทำ automatic retry ที่เปลี่ยน intent ผู้ใช้

## Acceptance criteria
- [x] Allowed transitions ทุกแถวใน matrix สำเร็จและเพิ่ม version
- [x] Empty-item submit และ stale date submit ไม่ผ่าน; reject reason ว่างไม่ผ่าน
- [x] Wrong role/owner และทุก invalid source state ปฏิเสธตาม contract
- [x] Approve ซ้ำ/แก้ terminal state ทำไม่ได้ ไม่มีข้อมูลเปลี่ยนเมื่อ error
- [x] Concurrent mutations ด้วย version เดียวกันสำเร็จเพียงหนึ่งคำสั่ง อีกคำสั่งเป็น conflict
- [x] UI ซ่อน action ที่ใช้ไม่ได้ แต่ direct API call ยังถูกตรวจ; failure ไม่ล้าง input reason

## Verification
Parameterized domain/service tests สำหรับ state × action × role; endpoint tests สำหรับ error envelope; real DB transactions ทดสอบ approve-vs-cancel และ double-approve; FE tests สำหรับ reason/loading/conflict

## Handoff
Implementation พร้อม code review ดู [TASK-004 evidence](../../quality/evidence/TASK-004.md) (รวมผลข้อมูลก่อน/หลัง failure); ไม่รวม notification, inventory reservation หรือการเก็บผู้อนุมัติ/เวลาอนุมัติ (ADR-004); Implement gate อนุมัติแล้ว 2026-09-25; TASK-005 เริ่มได้ ส่วน Verify gate ของ TASK-004 ยังรอ QA / acceptance owner
