# Evidence: TASK-005 Design

Date / operator: 2026-09-25 / Claude Code
Status: Approved by project owner acting as technical owner on 2026-09-25
Requirements: REQ-01, REQ-05, REQ-06, REQ-07, REQ-08, REQ-11

## Reviewed inputs

- REQ-05/REQ-08, assumption A-05 (`totalItems` = sum of quantities) and the implementation plan's search default
- The OpenAPI `searchEquipmentRequests` operation and the `EquipmentRequestPage`/`EquipmentRequestSummary` schemas
- The [data model](../../architecture/data-model.md) index plan (trigram GIN on the lowered keyword fields, scoped B-trees), and the list and race rules in [ui-flow](../../architecture/ui-flow.md)
- The TASK-003/004 implementation: the JPA aggregate, `ApiExceptionHandler`, identity switching that clears the query client, and the detail page actions

## Design outputs

- [ADR-005](../../architecture/decisions/ADR-005-search-list.md) covers Specification-based dynamic predicates with a grouped totals query, keyword escaping, parameter validation, the Testcontainers harness and URL-driven list state with a debounce.
- [TASK-005 test design](../TASK-005-test-design.md) covers the PostgreSQL search matrix, pagination, validation, query plan and UI state/race cases.

## Decisions for reviewer attention

1. Department filtering is trimmed, case-insensitive equality. The plain `department` index can't serve that predicate, so a V2 expression index is added only if the recorded `EXPLAIN ANALYZE` shows a need.
2. Invalid query parameters return 400 `VALIDATION_ERROR` with the parameter name, including non-integer `page`/`size`, not `MALFORMED_REQUEST`.
3. Add Testcontainers PostgreSQL at test scope. Container tests are skipped on machines without Docker, and evidence must show they ran.
4. The list links to the detail page for actions; there are no inline row actions.
5. Page size stays at the contract default of 10 in the UI. The API still accepts 1–100.

No code, contract or migration changes are part of this Design step.
