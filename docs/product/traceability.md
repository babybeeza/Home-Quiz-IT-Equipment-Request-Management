# Requirement traceability

อัปเดตช่อง implementation, tests และ evidence เป็นลิงก์ไฟล์จริงเมื่อมี ห้ามใช้ Planned แทนผล PASS

| Requirements | Task | Implementation / tests | Evidence | Status |
| --- | --- | --- | --- | --- |
| REQ-08, REQ-09, REQ-13 | [TASK-001](../delivery/tasks/TASK-001-bootstrap.md) | [frontend](../../frontend/README.md), [backend](../../backend/README.md), [Compose](../../compose.yaml), [ADR-001](../architecture/decisions/ADR-001-toolchains.md) | [TASK-001 evidence](../quality/evidence/TASK-001.md) | Approved |
| REQ-01, REQ-02, REQ-03, REQ-04, REQ-05, REQ-06, REQ-07, REQ-08, REQ-09 | [TASK-002](../delivery/tasks/TASK-002-contract-domain-data.md) | [domain](../../backend/src/main/kotlin/com/example/equipment/domain/RequestStatus.kt), [migration](../../backend/src/main/resources/db/migration/V1__create_equipment_request_schema.sql), [OpenAPI](../../contracts/openapi.yaml), [UI flow](../architecture/ui-flow.md) | [Design evidence](../quality/evidence/TASK-002-design.md), [implementation evidence](../quality/evidence/TASK-002.md) | Implement approved |
| REQ-01, REQ-03, REQ-04, REQ-06, REQ-07, REQ-08, REQ-09, REQ-11 | [TASK-003](../delivery/tasks/TASK-003-draft-flow.md) | [ADR-003](../architecture/decisions/ADR-003-draft-vertical-slice.md), [OpenAPI](../../contracts/openapi.yaml), [UI flow](../architecture/ui-flow.md) | [Design evidence](../quality/evidence/TASK-003-design.md), [test design](../quality/TASK-003-test-design.md) | Design approved |
| REQ-01, REQ-02, REQ-03, REQ-04, REQ-06, REQ-07, REQ-11 | [TASK-004](../delivery/tasks/TASK-004-approval-workflow.md) | — | — | Planned |
| REQ-01, REQ-05, REQ-06, REQ-07, REQ-08, REQ-11 | [TASK-005](../delivery/tasks/TASK-005-search-list.md) | — | — | Planned |
| REQ-01, REQ-07, REQ-10, REQ-11 | [TASK-006](../delivery/tasks/TASK-006-caching.md) | — | — | Planned |
| REQ-01 ถึง REQ-13: final audit; REQ-11, REQ-12, REQ-13: deliverables | [TASK-007](../delivery/tasks/TASK-007-verification-delivery.md) | — | — | Planned |
