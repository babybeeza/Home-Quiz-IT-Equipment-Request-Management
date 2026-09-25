# TASK-006: Redis และ Caffeine caching

Status: Verify approved — Delivery pending
Owner: Developer
Requirement IDs: REQ-01, REQ-07, REQ-10, REQ-11
Dependencies: TASK-004, TASK-005 (both Implement approved and merged)

## Human approvals

| Gate | Approver | Decision | Date / revision |
| --- | --- | --- | --- |
| Requirements | Project owner | Approved | 2026-09-25 / approved requirements and Discover baseline (Q-11) |
| Design | Project owner acting as technical owner | Approved | 2026-09-25 / ADR-006 + TASK-006 test design |
| Implement | Project owner acting as code reviewer | Approved | 2026-09-25 / TASK-006 evidence |
| Verify | Project owner acting as QA / acceptance owner | Approved | 2026-09-25 / user statement "QA Approved"; sign-off details in [acceptance test cases](../../quality/acceptance-test-cases.md) |
| Delivery | Release owner | Pending | — |

## Context
[Cache plan](../implementation-plan.md), [architecture](../../architecture/README.md), final read/write paths จาก 003–005, [ADR-006](../../architecture/decisions/ADR-006-caching.md), [test design](../../quality/TASK-006-test-design.md) และ [design evidence](../../quality/evidence/TASK-006-design.md)

## Scope / non-goals
ทำทั้ง shared Redis cache และ in-process Caffeine โดยมี read paths ใช้งานจริง ไม่เพิ่ม inventory management หรือ cache ทุก query โดยไม่มีเหตุผล

## Implementation plan
1. ADR ระบุ data placement: Redis request detail, Caffeine reference metadata; keys/version/TTL/max size/serialization และ measured benefit
2. Detail read ตรวจ authoritative owner/version แล้วอ่าน versioned cache; miss โหลด snapshot ที่ตรงกับ version หรือ retry/read DB ตาม policy
3. อัปเดตหลัง commit เท่านั้น; evict obsolete keys พร้อม TTL fallback; handle reader late-fill และ concurrent write โดยไม่คืน old-version payload
4. Caffeine cache สำหรับ metadata endpoint ที่ form เรียกจริง; lifecycle/expiry ไม่ขึ้นกับ distributed mutable request data
5. Redis timeout/outage fallback DB, metrics hit/miss/error ไม่เผย PII; ไม่ cache error/authorization result

## Acceptance criteria
- [x] พิสูจน์ load→hit→expiry ได้ทั้ง Redis และ Caffeine; configuration ถูกบันทึกใน README
- [x] Edit/submit/approve/reject/cancel แล้วอ่านได้สถานะ/version ที่ถูกต้องตาม consistency contract
- [x] Rollback ไม่ publish cached mutation; stale reader ไม่ทำให้ latest read คืนข้อมูลเก่า
- [x] Employee อีกคนอ่าน cached request ไม่ได้; role change ไม่มีข้อมูลค้างข้าม identity
- [x] Redis unavailable ยังอ่านผ่าน DB ได้ภายใน timeout policy; DB errors ไม่ถูกกลบ
- [x] มีหลักฐาน invalidation ข้าม backend instances และ Caffeine เป็น local จริง

## Verification
Unit tests ของ keys/eviction; integration กับ Redis และ PostgreSQL สำหรับ stale-fill race/rollback/cross-user; สอง app instances ตรวจ shared invalidation; deterministic expiry tests และบันทึก metrics จริง

## Handoff
Implement gate อนุมัติแล้ว 2026-09-25 ดู [TASK-006 evidence](../../quality/evidence/TASK-006.md); cache switches/config และ warm/cold procedure อยู่ใน [backend README](../../../backend/README.md) สำหรับ TASK-007; ไม่มีการอ้าง speedup จนกว่าจะวัด
