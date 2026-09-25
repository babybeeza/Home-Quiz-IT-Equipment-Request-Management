# ADR-004: Approval workflow actions

Status: Accepted
Date: 2026-09-25
Owner: Technical owner
Requirements/tasks: REQ-01, REQ-02, REQ-03, REQ-04, REQ-06, REQ-07, REQ-11 / TASK-004

## Context

TASK-004 adds submit, cancel, approve and reject on top of the TASK-003 aggregate. The following are already approved:
- the contract (`POST /{id}/submit|approve|reject|cancel` with `VersionAction` or `RejectAction`, returning the updated request)
- the permission and transition matrices in [api-behavior](../api-behavior.md)
- the error precedence (identity/role/ownership → expected version → state → business rule)
- the domain `RequestStatus.transition`
- the UI action table in [ui-flow](../ui-flow.md)

This ADR decides how actions are executed, which error code each failure returns, and where action state lives in the UI. It does not change the wire format.

## Options

### Backend action execution

1. **One application method per action, sharing a private `mutate` template:** load, authorize, compare version, transition, apply business rules, touch the parent, then flush. Each action's rules stay explicit while the ordering is written once.
2. **A generic `perform(action, payload)` endpoint and service:** fewer methods, but reject's reason and submit's revalidation become conditional branches, and the contract already has four endpoints.
3. **Conditional SQL updates (`UPDATE … WHERE id = ? AND version = ? AND status = ?`):** atomic in one statement, but it bypasses the aggregate and domain transition and adds a second persistence style.

### Frontend action state

1. **A `useRequestAction` hook built on TanStack Query mutations, with a reject dialog that owns its reason.** This is consistent with ADR-003, and the server response replaces the cached detail.
2. **Optimistic updates of status in the cache:** faster feedback, but a 409 or 422 would require rollback and could briefly show a state the server never accepted.

## Decision

### Backend

Option 1. `EquipmentRequestService` gains `submit`, `cancel`, `approve` and `reject`. Each runs in one `@Transactional` method with this order:

1. `findAggregateById`; missing → 404 `REQUEST_NOT_FOUND`.
2. `RequestAccessPolicy` for the operation. Submit and cancel require the owner Employee; approve and reject require an Approver. Failure → 403 `ACCESS_DENIED`.
3. `expectedVersion` must be ≥ 0 and equal the current version. A negative value → 400 `VALIDATION_ERROR`; a mismatch → 409 `REQUEST_VERSION_CONFLICT`.
4. `status.transition(action)`. An invalid source state → 409 `REQUEST_STATE_CONFLICT`, which covers every terminal-state mutation and a repeated approve.
5. Business rules:
   - Submit runs `validateForSubmit` on the stored data with today's Asia/Bangkok date. If the only failure is empty items → 422 `ITEMS_REQUIRED`. Any other failure, such as a required date now in the past → 422 `BUSINESS_RULE_VIOLATION`. Both carry field paths in `fieldErrors`.
   - Reject trims `reason`. Missing, null or blank → 422 `REJECTION_REASON_REQUIRED` (`fieldErrors.reason`). More than 500 characters → 400 `VALIDATION_ERROR` (`fieldErrors.reason`).
6. Set the new status, store the trimmed `rejectionReason` for reject only, update `updatedAt`, then `saveAndFlush`. The version increments once per successful action. A flush-time optimistic lock failure maps to the same 409 `REQUEST_VERSION_CONFLICT`, as in ADR-003.

Action bodies are parsed without `@Valid`, so validation cannot preempt the precedence above. A body that is missing entirely, is not JSON or has a non-integer version → 400 `MALFORMED_REQUEST`. `reason` is nullable in the Kotlin DTO so that a missing reason reaches step 5 and returns 422 rather than 400. Unknown JSON properties are ignored, as elsewhere in the API. The `additionalProperties: false` in the contract documents what clients send; it is not enforced, which matches the existing endpoints.

`InvalidRequestTransition` and `RequestNotEditable` both map to `REQUEST_STATE_CONFLICT`. A new `EquipmentRequestBusinessRuleViolation(code, fieldErrors)` maps to 422 with its code. No automatic retry happens on 409.

Out of scope, recorded here so it is not assumed: decision actor and timestamp columns, notifications and inventory reservation. Deciding who approved would need a migration and a contract change. The assignment does not ask for it.

### Frontend

Option 1.
- The detail page derives the available actions from role, ownership and status using the [ui-flow](../ui-flow.md) table. For example, an Employee owner sees Submit and Cancel on a DRAFT, and an Approver sees Approve and Reject on a PENDING request. Hidden actions are only a convenience.
- `useRequestAction(request)`:
  - sends the displayed `version` as `expectedVersion`
  - lets one action be pending at a time, disabling every action control while it runs
  - on success, writes the response into the detail query cache and announces the result
  - on 409, shows the conflict panel with "reload latest", which refetches the detail; nothing retries
  - on 422/400, shows the message and any field errors
- Cancel asks for confirmation because it is irreversible. Submit and approve do not, because their buttons are explicit.
- The reject dialog is a native `<dialog>` with a labelled textarea and client-side trim/required/500 checks. The server stays authoritative. The dialog keeps the typed reason on any failure and clears it only on success. Focus moves into the dialog on open and returns to the Reject button on close.

## Consequences and verification

Option 1 adds four similar service methods, but the shared template keeps precedence in one place. The ordering is frozen by tests at two levels: service tests for state × action × role, and MVC tests for envelopes and precedence. Submit revalidation reads stored data, so a draft saved yesterday with today's date remains submittable, while one whose date has passed does not.

Verification follows the [TASK-004 test design](../../quality/TASK-004-test-design.md). Real-PostgreSQL concurrency (approve vs cancel on one version, and double approve) is recorded as scripted evidence against Compose, the same method TASK-003 used.
