# TASK-004: Submit, approve, reject และ cancel

Status: Planned
Owner: Unassigned (developer)
Requirement IDs: REQ-01, REQ-02, REQ-03, REQ-04, REQ-06, REQ-07, REQ-11
Dependencies: TASK-003

## Context
[แผนหลักและ state matrix](../implementation-plan.md), [requirements](../../product/requirements.md), contract/domain จาก TASK-002

## Implementation plan
1. เพิ่ม action endpoints 4 ตัว รับ version และ reject reason ตาม contract
2. ใช้ application transactions ตรวจ role/owner → current version/state → business rules → persist; กำหนด error precedence ไว้ใน tests
3. Revalidate date/items ตอน submit; reject reason trim แล้วห้ามว่าง; ปฏิเสธ terminal mutations
4. UI action controls ตาม role + state, reject dialog, loading/errors, refetch/invalidate หลัง success
5. ตรวจ optimistic conflict ตอน flush/commit และ concurrent approve/cancel โดยไม่ทำ automatic retry ที่เปลี่ยน intent ผู้ใช้

## Acceptance criteria
- [ ] Allowed transitions ทุกแถวใน matrix สำเร็จและเพิ่ม version
- [ ] Empty-item submit และ stale date submit ไม่ผ่าน; reject reason ว่างไม่ผ่าน
- [ ] Wrong role/owner และทุก invalid source state ปฏิเสธตาม contract
- [ ] Approve ซ้ำ/แก้ terminal state ทำไม่ได้ ไม่มีข้อมูลเปลี่ยนเมื่อ error
- [ ] Concurrent mutations ด้วย version เดียวกันสำเร็จเพียงหนึ่งคำสั่ง อีกคำสั่งเป็น conflict
- [ ] UI ซ่อน action ที่ใช้ไม่ได้ แต่ direct API call ยังถูกตรวจ; failure ไม่ล้าง input reason

## Verification
Parameterized domain/service tests สำหรับ state × action × role; endpoint tests สำหรับ error envelope; real DB transactions ทดสอบ approve-vs-cancel และ double-approve; FE tests สำหรับ reason/loading/conflict

## Handoff
Evidence พร้อมผลข้อมูลก่อน/หลัง failure; ไม่รวม notification หรือ inventory reservation; ยัง NOT RUN
