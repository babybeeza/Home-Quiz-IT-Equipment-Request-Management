# TASK-004 verification design

Status: Approved in TASK-004 Design on 2026-09-25; no results are claimed here
Requirements: REQ-01, REQ-02, REQ-03, REQ-04, REQ-06, REQ-07, REQ-11

| Boundary | Required behavior |
| --- | --- |
| Transition matrix | Parameterized over 5 states × 4 actions. The five allowed rows (DRAFT→PENDING, DRAFT→CANCELLED, PENDING→CANCELLED/APPROVED/REJECTED) succeed with exactly one version increment. Every other cell returns 409 `REQUEST_STATE_CONFLICT` and saves nothing |
| Role and ownership | Parameterized over action × actor (owner Employee, non-owner Employee, Approver). Submit/cancel succeed only for the owner; approve/reject only for an Approver; everyone else gets 403 `ACCESS_DENIED` and nothing is saved |
| Precedence | For each action, a request that is wrong on every axis returns 403, then 409 version, then 409 state, then 422 as each earlier fault is removed |
| Submit rules | Empty items → 422 `ITEMS_REQUIRED`. A stored date before today (fixed clock) → 422 `BUSINESS_RULE_VIOLATION` with `fieldErrors.requiredDate`. A date equal to today is accepted. Nothing changes on failure |
| Reject reason | Missing, null, empty or whitespace → 422 `REJECTION_REASON_REQUIRED`. More than 500 characters → 400 `VALIDATION_ERROR`. The reason is stored trimmed. Only reject writes `rejectionReason` |
| Terminal immutability | APPROVED, REJECTED and CANCELLED reject every action and edit with 409. A second approve with the new version → 409 state; with the old version → 409 version. Stored status, reason and version are unchanged |
| Envelope / MVC | For all four endpoints: missing body or non-JSON → 400 `MALFORMED_REQUEST`; bad UUID or headers → 400; unknown ID → 404; 422 envelope carries `code` and `fieldErrors`; a flush-time optimistic lock → 409 |
| Real concurrency | Against Compose PostgreSQL, parallel approve and cancel on one PENDING version yield exactly one 200 and one 409. Two parallel approves also yield one 200 and one 409. Final status and version are recorded before and after |
| UI action visibility | For each role/ownership × status combination, only the actions in the ui-flow table render |
| UI action lifecycle | The API receives the displayed version. While pending, all action controls are disabled and a second click sends nothing. Success updates the displayed status from the response |
| UI errors | A 409 shows the conflict panel and a reload action, with no automatic retry. A 422 shows the server message |
| Reject dialog | An empty or whitespace reason is blocked client-side. A server failure keeps the typed reason. Success closes the dialog. The reason input is labelled |

## Evidence plan

- Backend service/domain/MVC tests run with the Maven wrapper; parameterized cases are counted individually.
- The PostgreSQL concurrency script runs against the Compose database. Evidence records requests, responses and before/after rows.
- Frontend tests run with Vitest and React Testing Library, together with lint, typecheck and production build in the Node 24.15.0 container.
- Evidence records exact commands, versions and counts, and marks any NOT RUN case with a reason.
