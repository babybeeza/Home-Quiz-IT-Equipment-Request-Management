# Evidence: TASK-004 implementation

Date / operator: 2026-09-25 / Claude Code; Implement approved by project owner acting as code reviewer on 2026-09-25
Approved Design: [ADR-004](../../architecture/decisions/ADR-004-approval-workflow.md), [TASK-004 test design](../TASK-004-test-design.md)
Environment: Windows, Temurin Java 25.0.3 targeting Java 21, Node 24.15.0-alpine container, PostgreSQL 17.11 in Docker Compose
Requirements: REQ-01, REQ-02, REQ-03, REQ-04, REQ-06, REQ-07, REQ-11

| Check | Command / method | Result | Observation |
| --- | --- | --- | --- |
| Backend clean package | `backend\\mvnw.cmd --batch-mode clean package` | PASS (exit 0) | 85 tests, 0 failures/errors/skips. New: `EquipmentRequestWorkflowTest` with 47 cases (20 status × action, 12 action × actor, 4 precedence, 11 rule/terminal/404 cases) and 4 new MVC tests (14 MVC total) |
| Frontend quality checks | Node 24.15.0-alpine container, isolated `node_modules`/`.next` volumes: `npm ci && npm run lint && npm run typecheck && npm test && npm run build` | PASS (exit 0) | Lint and typecheck passed; 23 tests passed (15 new in `request-actions.test.tsx`); all routes built |
| Approve vs cancel | Packaged jar against Compose PostgreSQL; parallel `curl` approve (Approver) and cancel (owner), both with `expectedVersion` 1 | PASS | Before `PENDING\|1`; approve 200, cancel 409 `REQUEST_VERSION_CONFLICT`; after `APPROVED\|2` |
| Double approve | Two parallel approves with `expectedVersion` 1 | PASS | Before `PENDING\|1`; 200 and 409 `REQUEST_VERSION_CONFLICT`; after `APPROVED\|2`. A third approve at version 2 → 409 `REQUEST_STATE_CONFLICT`, still `APPROVED\|2` |
| Failure leaves data unchanged | Empty-item submit, blank reject reason, Employee approve | PASS | `ITEMS_REQUIRED`, `REJECTION_REASON_REQUIRED` and `ACCESS_DENIED`; DB rows (status\|version\|reason\|items) identical before and after |
| Reject and terminal immutability | Reject with `"  Over budget  "`, then cancel and PUT at the current version | PASS | 200 `REJECTED\|2\|Over budget` (trimmed); cancel and edit → 409 `REQUEST_STATE_CONFLICT`; row unchanged |
| DRAFT cancel | Cancel an empty DRAFT at version 0 | PASS | 200 `CANCELLED`, version 1 |
| Past-date submit on PostgreSQL | — | NOT RUN | Needs a stored date that has since passed. Covered by the fixed-clock service test and the MVC envelope test; the application clock is not injectable at runtime |
| Browser walkthrough of the action UI | — | NOT RUN | Covered by React Testing Library behavior tests and the production build; no manual browser session was performed |

For the concurrency checks, the evidence shows the outcome (exactly one success, one 409, one version increment). It does not show which defence caught the loser, because either the explicit version comparison or the flush-time `@Version` check can reject it depending on timing. Both map to `REQUEST_VERSION_CONFLICT` (ADR-003/ADR-004).

## Implemented boundaries

- `EquipmentRequestService.submit/cancel/approve/reject` share one `transition` template (load → access → version → state → rules → persist). `update` reuses the same `loadForMutation`.
- Submit revalidates stored data through `validateForSubmit`. Reject uses `validateRejectionReason`: a blank reason → 422, too long → 400. Only reject writes `rejectionReason`.
- `InvalidRequestTransition` → 409 `REQUEST_STATE_CONFLICT`. The new `EquipmentRequestBusinessRuleViolation` → 422 with a stable code and `fieldErrors`.
- Action bodies are not `@Valid`. A missing body, non-JSON body, non-integer version or missing `expectedVersion` (`{}`) → 400 `MALFORMED_REQUEST`; there is no silent default to 0.
- Frontend:
  - `availableActions` derives buttons from role and status (an Employee who can load a detail is its owner).
  - `useRequestAction` sends the displayed version, allows one pending action at a time, writes the response into the detail cache, shows the conflict panel on 409 without retrying, and maps 422/403 codes to Thai messages.
  - Cancel asks for confirmation.
  - The reject dialog focuses its labelled textarea, blocks a blank reason client-side, keeps the typed reason on failure and returns focus on close.
  - The detail page shows the rejection reason.

## Scope notes

- Decision actor and timestamp, notifications and inventory reservation are out of scope (ADR-004).
- The searchable list and contextual list actions remain TASK-005; actions are on the detail page.
- Smoke rows remain in the local development database; the Compose services were stopped afterwards.

## Approval needed

The code reviewer must approve the TASK-004 code, tests and this evidence before the Implement gate closes.
