# Implementation backlog

ทุกงานมีสถานะ Planned และมี task packet แล้ว อ่าน [แผน implementation ทั้งโครงการ](implementation-plan.md) สำหรับ scope, effort, decisions, risks และ completion gates

| Task | Slice / deliverable | Depends on |
| --- | --- | --- |
| [TASK-001](tasks/TASK-001-bootstrap.md) | Bootstrap frontend/backend, pin runtime versions, build/test tooling, run instructions | — |
| [TASK-002](tasks/TASK-002-contract-domain-data.md) | Domain rules, API contract, schema/index design, decisions A-01 ถึง A-05 และ migration baseline | TASK-001 |
| [TASK-003](tasks/TASK-003-draft-flow.md) | Create/view/edit draft จาก UI ถึง DB พร้อม validation, ownership และ version conflict | TASK-002 |
| [TASK-004](tasks/TASK-004-approval-workflow.md) | Submit/approve/reject/cancel พร้อม transition, role, reason, concurrency และ rollback tests | TASK-003 |
| [TASK-005](tasks/TASK-005-search-list.md) | List/search/filter/pagination/sort, URL state, loading/error/empty และ race handling | TASK-003 |
| [TASK-006](tasks/TASK-006-caching.md) | Redis/Caffeine implementation, invalidation, failure handling และ cache correctness tests | TASK-004, TASK-005 |
| [TASK-007](tasks/TASK-007-verification-delivery.md) | Required test coverage, k6 workload/report, README, release/rollback rehearsal | TASK-004, TASK-005, TASK-006 |

TASK-007 เป็น final verification; แต่ละ slice ต้องเขียนและรัน tests ของตัวเอง ไม่เลื่อน testing ทั้งหมดไปท้ายโครงการ
