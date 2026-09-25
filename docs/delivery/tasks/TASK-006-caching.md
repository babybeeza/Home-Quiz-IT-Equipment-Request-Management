# TASK-006: Redis และ Caffeine caching

Status: Planned
Owner: Unassigned (developer)
Requirement IDs: REQ-01, REQ-07, REQ-10, REQ-11
Dependencies: TASK-004, TASK-005

## Context
[Cache plan](../implementation-plan.md), [architecture](../../architecture/README.md), final read/write paths จาก 003–005

## Scope / non-goals
ทำทั้ง shared Redis cache และ in-process Caffeine โดยมี read paths ใช้งานจริง ไม่เพิ่ม inventory management หรือ cache ทุก query โดยไม่มีเหตุผล

## Implementation plan
1. ADR ระบุ data placement: Redis request detail, Caffeine reference metadata; keys/version/TTL/max size/serialization และ measured benefit
2. Detail read ตรวจ authoritative owner/version แล้วอ่าน versioned cache; miss โหลด snapshot ที่ตรงกับ version หรือ retry/read DB ตาม policy
3. อัปเดตหลัง commit เท่านั้น; evict obsolete keys พร้อม TTL fallback; handle reader late-fill และ concurrent write โดยไม่คืน old-version payload
4. Caffeine cache สำหรับ metadata endpoint ที่ form เรียกจริง; lifecycle/expiry ไม่ขึ้นกับ distributed mutable request data
5. Redis timeout/outage fallback DB, metrics hit/miss/error ไม่เผย PII; ไม่ cache error/authorization result

## Acceptance criteria
- [ ] พิสูจน์ load→hit→expiry ได้ทั้ง Redis และ Caffeine; configuration ถูกบันทึกใน README
- [ ] Edit/submit/approve/reject/cancel แล้วอ่านได้สถานะ/version ที่ถูกต้องตาม consistency contract
- [ ] Rollback ไม่ publish cached mutation; stale reader ไม่ทำให้ latest read คืนข้อมูลเก่า
- [ ] Employee อีกคนอ่าน cached request ไม่ได้; role change ไม่มีข้อมูลค้างข้าม identity
- [ ] Redis unavailable ยังอ่านผ่าน DB ได้ภายใน timeout policy; DB errors ไม่ถูกกลบ
- [ ] มีหลักฐาน invalidation ข้าม backend instances และ Caffeine เป็น local จริง

## Verification
Unit tests ของ keys/eviction; integration กับ Redis และ PostgreSQL สำหรับ stale-fill race/rollback/cross-user; สอง app instances ตรวจ shared invalidation; deterministic expiry tests และบันทึก metrics จริง

## Handoff
ส่ง cache switches/config และ warm/cold procedure ให้ k6 task; ไม่มีการอ้าง speedup จนกว่าจะวัด; ยัง NOT RUN
