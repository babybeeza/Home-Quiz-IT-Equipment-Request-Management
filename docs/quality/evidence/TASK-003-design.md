# Evidence: TASK-003 Design

Date / operator: 2026-09-25 / Codex
Status: Approved by project owner acting as technical owner on 2026-09-25
Requirements: REQ-01, REQ-03, REQ-04, REQ-06, REQ-07, REQ-08, REQ-09, REQ-11

## Reviewed inputs

- Original assignment and approved requirements/discovery baseline
- Accepted ADR-002, OpenAPI contract, API behavior, data model and UI flow
- TASK-003 scope and acceptance criteria
- Existing backend/frontend manifests and TASK-002 domain/migration implementation

## Design outputs

- [ADR-003](../../architecture/decisions/ADR-003-draft-vertical-slice.md) defines backend layers, the JPA aggregate and transaction/version strategy, request-number allocation, error mapping and frontend state ownership.
- [TASK-003 test design](../TASK-003-test-design.md) defines observable API, persistence, concurrency and UI cases before implementation.
- [TASK-003 packet](../../delivery/tasks/TASK-003-draft-flow.md) records the gate, concrete file boundaries and handoff condition.

## Review focus

The project owner acting as technical owner approved the JPA aggregate replacement approach, deliberate parent touch for item-only edits, PostgreSQL concurrency evidence, React Hook Form/Zod/TanStack Query choice, fixed demo identity provider and 409/dirty-form UX.

