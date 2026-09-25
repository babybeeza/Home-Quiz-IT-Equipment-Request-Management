# TASK-007: Final verification, performance และส่งมอบ

Status: Implement approved — Verify pending (G-1, H-1 open)
Owner: Developer
Requirement IDs: REQ-01 ถึง REQ-13 (final audit), โดยเฉพาะ REQ-11, REQ-12, REQ-13
Dependencies: TASK-004, TASK-005, TASK-006 (all Implement approved and merged)

## Human approvals

| Gate | Approver | Decision | Date / revision |
| --- | --- | --- | --- |
| Requirements | Project owner | Approved | 2026-09-25 / approved requirements and Discover baseline (Q-12) |
| Design | Project owner acting as technical owner | Approved | 2026-09-25 / ADR-007 + TASK-007 test design |
| Implement | Project owner acting as code reviewer | Approved | 2026-09-25 / TASK-007 evidence + performance report; G-1 and H-1 not yet decided |
| Verify | QA / acceptance owner | Pending | — |
| Delivery | Release owner | Pending | — |

## Context
[ADR-007](../../architecture/decisions/ADR-007-performance-and-delivery.md), [test design](../../quality/TASK-007-test-design.md), [design evidence](../../quality/evidence/TASK-007-design.md), [แผนหลัก](../implementation-plan.md), [test strategy](../../quality/test-strategy.md), [traceability](../../product/traceability.md), [runbook](../../operations/runbook.md), [performance template](../../../tests/performance/results/TEMPLATE.md)

## Implementation plan
1. Audit requirement→implementation→test→evidence ทุก ID; ตรวจ FE ≥4, BE ≥6 พร้อม behavioral assertions ไม่ใช่เพียงไฟล์ test
2. รัน lint/typecheck/build/unit และ integration/contract checks ของ final revision; smoke user journeys ครบ roles พร้อม conflict/error path
3. Resolve A-06: finalize k6 thresholds/dataset/workload/hardware ก่อนรัน สร้าง seed/cleanup ที่รันซ้ำได้ ใช้ dates อิง clock
4. k6 smoke → baseline → stress; เก็บ disabled/cold/warm cache แยกกัน, expected conflicts แยก metric, ใช้ IDs/versions ไม่ชนกันจาก test setup
5. รายงาน p50/p95/p99, throughput, unexpected errors, correctness, cache/DB observations พร้อม artifact และข้อจำกัด
6. README prerequisites/versions/install/run/test/API/mock identity/state/cache/decisions/assumptions; migration/schema และ env example ครบ
7. Clean-start rehearsal บน local environment, migration/restore หรือ forward-fix rehearsal ตามที่เลือก; บันทึก repository/revision และ final handoff

## Acceptance criteria
- [x] Mandatory requirements ทุกข้อมี evidence หรือรายงาน gap ที่ทำให้ยังไม่ผ่านอย่างชัดเจน (G-1 list row actions รอตัดสินใจ)
- [x] FE/BE minimum tests ผ่านจริง และ concurrency/transaction/cache tests ของแผนผ่าน
- [x] k6 รันจริงและมี report พร้อม workload/environment/threshold results; threshold fail ต้องรายงาน ไม่ปรับย้อนหลังเพื่อให้ผ่าน
- [x] ผู้อื่นทำตาม README จาก clean checkout แล้ว start frontend/backend/DB/Redis และ demo workflow ได้
- [x] Source, tests, API docs, migration/schema, lockfiles/wrapper และ limitations ครบ; ไม่มี secrets ใหม่ (H-1: token เดิมใน assignment HTML รายงานแล้ว)
- [x] Runbook ระบุ health/smoke/recovery steps ที่ทดสอบแล้ว; ไม่อ้างว่า deploy cloud แล้ว

## Verification
ใช้ commands จาก manifests/wrappers จริง เก็บ output ที่ docs/quality/evidence และ tests/performance/results; หาก environment ทำให้ตรวจไม่ได้ ระบุ NOT RUN และยังไม่ mark task Done

## Handoff
Implement gate อนุมัติแล้ว 2026-09-25 (G-1, H-1 ยังเปิดอยู่) ดู [TASK-007 evidence](../../quality/evidence/TASK-007.md) และ [performance report](../../../tests/performance/results/TASK-007-report.md); release summary อยู่ใน README และ evidence; Verify/Delivery gates ยังรอ QA และ release owner
