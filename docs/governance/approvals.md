# Human approval gates

ทุก phase ใช้สถานะ Draft → In review → Changes requested หรือ Approved งานเปลี่ยน phase ได้เมื่อคนที่รับผิดชอบอนุมัติ artifact revision ที่ตรวจแล้ว AI สร้างหลักฐานและช่วย review ได้ แต่ไม่เป็นผู้อนุมัติงานของตนเอง

| Gate | Required approver | Artifact | Current status |
| --- | --- | --- | --- |
| Requirements | Product owner | requirements, scope, acceptance criteria, assumptions | Approved to proceed |
| Design | Technical owner | architecture, ADRs, contract, data/test plan | Approved to proceed |
| Implement | Code reviewer | code diff, implementation tests and task evidence | Approved for TASK-001 |
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

หากแก้ artifact ที่ approved แล้วอย่างมีนัยสำคัญ ให้เปลี่ยน gate นั้นกลับเป็น In review และบันทึก approval ใหม่พร้อม revision/date ผู้อนุมัติคนเดียวรับหลายบทบาทได้ใน take-home project แต่ต้องระบุบทบาททุกครั้ง
