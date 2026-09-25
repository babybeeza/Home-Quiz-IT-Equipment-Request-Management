# Evidence: TASK-002 Design

Date / operator: 2026-09-25 / Codex; Design approved by project owner
Revision: working tree after approved Requirements commit `6c957fb`
Requirement IDs: REQ-01, REQ-02, REQ-03, REQ-05, REQ-06, REQ-07, REQ-09

| Check | Command / review method | Result | Observation |
| --- | --- | --- | --- |
| OpenAPI parse and reference validation | `docker run --rm -v "${PWD}:/local" openapitools/openapi-generator-cli:v7.16.0 validate -i /local/contracts/openapi.yaml` | PASS | 8 operations parsed; no validation issues detected |
| Requirements mapping | Manual review against approved [Discover analysis](../../product/discovery.md) | PASS | Q-01 through Q-10 reflected in ADR/API/data/UI design; cache and k6 decisions remain assigned to their planned tasks |
| Role/state mapping | Cross-check [API behavior](../../architecture/api-behavior.md) against REQ-01/REQ-02 | PASS | All allowed transitions and denied actor combinations are represented |
| Validation mapping | Cross-check OpenAPI schemas and data model against REQ-03 | PASS | Assignment limits plus approved department/reason limits represented; date/item/reject rules remain domain checks |
| Concurrency/transaction design | Review ADR-002 and data model against REQ-07 | PASS | expectedVersion on all mutations, parent version, rollback and post-commit cache boundary specified |
| Executable code/migration tests | Not applicable in Design | NOT RUN | Domain code and Flyway migration begin only after Design approval |

## Review notes

- OpenAPI 3.0 `allOf` and `additionalProperties: false` can reject fields introduced by another branch. Input base schemas therefore do not use that flag; server DTO deserialization policy will be decided during implementation and tested explicitly.
- `pg_trgm` improves required contains search but needs extension-install privileges; this deployment prerequisite is called out in the data model.
- Error precedence and 403 behavior intentionally expose authorization outcome for this take-home demo, as approved in Discover Q-06.

## Approval

Project owner acting as technical owner approved ADR-002, OpenAPI, API behavior, data model, UI flow and test design on 2026-09-25.
