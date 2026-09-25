# Human approval gates

ทุก phase ใช้สถานะ Draft → In review → Changes requested หรือ Approved งานเปลี่ยน phase ได้เมื่อคนที่รับผิดชอบอนุมัติ artifact revision ที่ตรวจแล้ว AI สร้างหลักฐานและช่วย review ได้ แต่ไม่เป็นผู้อนุมัติงานของตนเอง

| Gate | Required approver | Artifact | Current status |
| --- | --- | --- | --- |
| Requirements | Product owner | requirements, scope, acceptance criteria, assumptions | Approved |
| Design | Technical owner | architecture, ADRs, contract, data/test plan | Approved through TASK-005 |
| Implement | Code reviewer | code diff, implementation tests and task evidence | Approved through TASK-005 |
| Verify | QA / acceptance owner | traceability, functional/reliability/performance evidence | Pending |
| Delivery | Release owner | release revision, runbook, rollback and limitations | Pending |

## Approval record

| Phase | Decision | Approver | Date | Revision / evidence | Notes |
| --- | --- | --- | --- | --- | --- |
| Requirements | Approved to proceed | Project owner (user) | 2026-09-25 | User request: “เริ่ม implement ตาม plan” | Approves implementation against the repository requirements and stated plan; unresolved assumptions must still be resolved in TASK-002 |
| Design | Approved to proceed | Project owner acting as technical owner | 2026-09-25 | [implementation plan](../delivery/implementation-plan.md) | TASK-level ADRs and contracts still require review when produced |
| Implement | Approved | Project owner acting as code reviewer | 2026-09-25 | [TASK-001 evidence](../quality/evidence/TASK-001.md) | User explicitly approved TASK-001; later implementation tasks still require review |
| Verify | Pending | QA / acceptance owner | — | — | Cannot approve before implementation review |
| Delivery | Pending | Release owner | — | — | Cannot approve before verification |
| Requirements | Reopened for review | Project owner (user) | 2026-09-25 | [Discover analysis](../product/discovery.md) | User explicitly selected Discover; Q-01 through Q-12 require approval before Design resumes |
| Requirements | Approved | Project owner (user) | 2026-09-25 | [Discover analysis](../product/discovery.md) | User approved requirement baseline and proposed defaults Q-01 through Q-12 |
| Design | Approved | Project owner acting as technical owner | 2026-09-25 | [ADR-002](../architecture/decisions/ADR-002-request-contract-and-domain.md), [OpenAPI](../../contracts/openapi.yaml), [data model](../architecture/data-model.md), [UI flow](../architecture/ui-flow.md) | User explicitly approved the TASK-002 Design package |
| Implement | Approved | Project owner acting as code reviewer | 2026-09-25 | [TASK-002 evidence](../quality/evidence/TASK-002.md) | User explicitly approved the TASK-002 domain, validation, tests and migration baseline |
| Design | Approved | Project owner acting as technical owner | 2026-09-25 | [ADR-003](../architecture/decisions/ADR-003-draft-vertical-slice.md), [TASK-003 test design](../quality/TASK-003-test-design.md) | User explicitly approved the TASK-003 draft vertical-slice design |
| Implement | Approved | Project owner acting as code reviewer | 2026-09-25 | [TASK-003 evidence](../quality/evidence/TASK-003.md) | User explicitly approved TASK-003 after review rounds 1–2, including the ADR-003 layer-wording amendment (no behavior or contract change) |
| Design | Approved | Project owner acting as technical owner | 2026-09-25 | [ADR-004](../architecture/decisions/ADR-004-approval-workflow.md), [TASK-004 test design](../quality/TASK-004-test-design.md) | User explicitly approved the TASK-004 approval-workflow design including decisions 1–5 in [design evidence](../quality/evidence/TASK-004-design.md) |
| Implement | Approved | Project owner acting as code reviewer | 2026-09-25 | [TASK-004 evidence](../quality/evidence/TASK-004.md) | User explicitly approved the TASK-004 approval-workflow implementation, tests and evidence |
| Design | Approved | Project owner acting as technical owner | 2026-09-25 | [ADR-005](../architecture/decisions/ADR-005-search-list.md), [TASK-005 test design](../quality/TASK-005-test-design.md) | User explicitly approved the TASK-005 search/list design including decisions 1–5 in [design evidence](../quality/evidence/TASK-005-design.md) |
| Implement | Approved | Project owner acting as code reviewer | 2026-09-25 | [TASK-005 evidence](../quality/evidence/TASK-005.md) | User explicitly approved the TASK-005 search/list implementation, tests, query-plan evidence and the documented debounce deviation |

หากแก้ artifact ที่ approved แล้วอย่างมีนัยสำคัญ ให้เปลี่ยน gate นั้นกลับเป็น In review และบันทึก approval ใหม่พร้อม revision/date ผู้อนุมัติคนเดียวรับหลายบทบาทได้ใน take-home project แต่ต้องระบุบทบาททุกครั้ง
