# TASK-005: List, search, filter, pagination และ React state

Status: Planned
Owner: Unassigned (developer)
Requirement IDs: REQ-01, REQ-05, REQ-06, REQ-07, REQ-08, REQ-11
Dependencies: TASK-003

## Context
[แผนหลัก](../implementation-plan.md), [requirements](../../product/requirements.md), pagination/search contract จาก TASK-002

## Implementation plan
1. Backend GET collection: keyword OR number/title/name และ AND status/department/owner scope
2. Validate page/size/status/sort, deterministic createdAt/id sorting, accurate total counts; DTO projection ไม่โหลด items แบบ N+1
3. Query indexes ตาม access pattern; เก็บ query plan ตัวอย่างและแก้ costly query ก่อน caching
4. UI table ครบ columns, query parameters ใน URL, debounce keyword, page reset เมื่อ filter เปลี่ยน
5. แยก form/server/UI state; cancel/ignore stale response; invalidate scoped data เมื่อ role/user เปลี่ยน; memoization เฉพาะมีเหตุผล
6. เชื่อม actions จาก 004 เมื่อพร้อม; list สามารถส่งมอบและทดสอบแยกได้ด้วย links ไป detail

## Acceptance criteria
- [ ] ทุก list column ตามโจทย์แสดงถูกต้องรวม totalItems ที่ตกลงแล้ว
- [ ] Keyword + status + department ใช้ร่วมกัน; Employee ไม่เห็นรายการคนอื่น/ยอดรวมคนอื่น
- [ ] Pagination metadata ถูกต้องทั้ง empty/page สุดท้าย และ sort ทำซ้ำได้
- [ ] URL refresh/back/forward คืน query state; filter ใหม่กลับ page แรก
- [ ] Loading/error/empty/success ชัดเจน; request เก่าไม่ overwrite ผล request ใหม่
- [ ] Hooks dependencies/cleanup และ immutable updates ถูกต้อง; ไม่เก็บ derived state ซ้ำ

## Verification
Repository/API tests ด้วยหลาย owner/status/department และ matching ทั้ง 3 keyword fields; FE search/filter/paging/race tests; ตรวจ SQL count และ query plan พร้อม evidence

## Handoff
Baseline query performance ส่งให้ 006/007; ยัง NOT RUN
