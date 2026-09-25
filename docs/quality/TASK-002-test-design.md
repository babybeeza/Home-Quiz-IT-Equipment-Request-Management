# TASK-002 design verification plan

Status: Proposed; these are executable checks for the Implement/Verify phases, not test results

| Area | Required cases |
| --- | --- |
| Contract | OpenAPI parses; 8 operations exist; identity headers, versions, schemas, pagination and common errors are present |
| State machine | every allowed transition; every invalid/terminal transition; approve twice |
| Validation | min/max boundaries, blank fields, email, today/yesterday, item enum/quantity/specification, empty submit, blank reject reason |
| Authorization | Employee own/non-own and Approver matrix for list/detail/edit/actions |
| Persistence | fresh migration, constraints/indexes/FK, sequence allocation, item replacement bumps parent version |
| Transaction | child save failure rolls back parent/items; error leaves version/state unchanged |
| Concurrency | two real transactions with same version: exactly one commits, other returns 409 |
| Error contract | 400/403/404/409/422/500 envelope and stable field paths |
| Search design | actor scope AND filters; keyword OR fields; deterministic pagination and total quantity without N+1 |

Implementation evidence must record exact commands, PostgreSQL version and persisted before/after state. Mock-only tests cannot prove optimistic locking or rollback behavior.
