# TASK-005: List, search, filter, pagination และ React state

Status: Verify approved — Delivery pending
Owner: Developer
Requirement IDs: REQ-01, REQ-05, REQ-06, REQ-07, REQ-08, REQ-11
Dependencies: TASK-003 (Implement approved); branch stacked on TASK-004

## Human approvals

| Gate | Approver | Decision | Date / revision |
| --- | --- | --- | --- |
| Requirements | Project owner | Approved | 2026-09-25 / approved requirements and Discover baseline |
| Design | Project owner acting as technical owner | Approved | 2026-09-25 / ADR-005 + TASK-005 test design |
| Implement | Project owner acting as code reviewer | Approved | 2026-09-25 / TASK-005 evidence |
| Verify | Project owner acting as QA / acceptance owner | Approved | 2026-09-25 / user statement "QA Approved"; sign-off details in [acceptance test cases](../../quality/acceptance-test-cases.md) |
| Delivery | Release owner | Pending | — |

## Context
[แผนหลัก](../implementation-plan.md), [requirements](../../product/requirements.md), pagination/search contract จาก TASK-002, [ADR-005](../../architecture/decisions/ADR-005-search-list.md), [test design](../../quality/TASK-005-test-design.md) และ [design evidence](../../quality/evidence/TASK-005-design.md)

## Implementation plan
1. Backend GET collection: keyword OR number/title/name และ AND status/department/owner scope
2. Validate page/size/status/sort, deterministic createdAt/id sorting, accurate total counts; DTO projection ไม่โหลด items แบบ N+1
3. Query indexes ตาม access pattern; เก็บ query plan ตัวอย่างและแก้ costly query ก่อน caching
4. UI table ครบ columns, query parameters ใน URL, debounce keyword, page reset เมื่อ filter เปลี่ยน
5. แยก form/server/UI state; cancel/ignore stale response; invalidate scoped data เมื่อ role/user เปลี่ยน; memoization เฉพาะมีเหตุผล
6. เชื่อม actions จาก 004 เมื่อพร้อม; list สามารถส่งมอบและทดสอบแยกได้ด้วย links ไป detail

## Acceptance criteria
- [x] ทุก list column ตามโจทย์แสดงถูกต้องรวม totalItems ที่ตกลงแล้ว
- [x] Keyword + status + department ใช้ร่วมกัน; Employee ไม่เห็นรายการคนอื่น/ยอดรวมคนอื่น
- [x] Pagination metadata ถูกต้องทั้ง empty/page สุดท้าย และ sort ทำซ้ำได้
- [x] URL refresh/back/forward คืน query state; filter ใหม่กลับ page แรก
- [x] Loading/error/empty/success ชัดเจน; request เก่าไม่ overwrite ผล request ใหม่
- [x] Hooks dependencies/cleanup และ immutable updates ถูกต้อง; ไม่เก็บ derived state ซ้ำ

## Verification
Repository/API tests ด้วยหลาย owner/status/department และ matching ทั้ง 3 keyword fields; FE search/filter/paging/race tests; ตรวจ SQL count และ query plan พร้อม evidence

## Handoff
Implement gate อนุมัติแล้ว 2026-09-25 ดู [TASK-005 evidence](../../quality/evidence/TASK-005.md); baseline query performance: [seed](../../../tests/performance/seed-search-dataset.sql) + [EXPLAIN ANALYZE](../../../tests/performance/results/TASK-005-explain.txt) ส่งต่อให้ 006/007
