# TASK-017: Check whether a non-JSON Accept header lets a mutation commit before 406 (R3)

Status: Planned — investigation; Design review not yet needed
Owner: Backend developer investigates; project owner decides whether a fix is needed
Requirement IDs: REQ-06 (error envelope / status), REQ-07 (data integrity), REQ-02 (state transitions)
Dependencies: [TASK-016](TASK-016-framework-error-status.md) (bodiless 406 handler), finding R3 in [TASK-016 evidence](../../quality/evidence/TASK-016.md)

## Human approvals

| Gate | Approver | Decision | Date / revision |
| --- | --- | --- | --- |
| Requirements | Project owner | Investigation requested 2026-09-26 (“R3 เปิดเป็น task ตรวจสอบ”) | 2026-09-26 |
| Design | Project owner acting as technical owner | Pending — only if the investigation confirms R3 | — |
| Implement | Project owner acting as code reviewer | Pending | — |
| Verify | Project owner acting as QA / acceptance owner | Pending | — |
| Delivery | Project owner acting as release owner | Pending | — |

## Context

[`EquipmentRequestController`](../../../backend/src/main/kotlin/com/example/equipment/api/EquipmentRequestController.kt) declares no `produces`, so Spring does not check `Accept` while choosing a handler. The hypothesis: for `POST /api/v1/equipment-requests`, `PUT /{id}` and `POST /{id}/submit|approve|reject|cancel`, Spring runs the service method and commits the transaction first. Only then does writing the JSON response fail with `HttpMediaTypeNotAcceptableException`, which TASK-016 turns into a bodiless 406.

The client would then see a failure for a change that actually happened. A retry could create a duplicate DRAFT, because create is not idempotent. A retried transition would get 409 even though the first attempt had already applied the change. Cache eviction on the changed request may also be affected.

This is unverified. The frontend [`api.ts`](../../../frontend/src/features/equipment-requests/api.ts) sets no `Accept` header, so browsers send `*/*` and the UI is not exposed. Only direct API clients that send an `Accept` without JSON are affected. The behavior existed before TASK-016; TASK-016 changed only the 406 response shape.

## Scope / non-goals

Establish with evidence whether R3 happens, for which endpoints, and what state changes: rows, version, status, request-number sequence and Redis detail cache. Record the result and a recommended disposition.

Out of scope until the owner decides: any change to controllers, the handler, the contract or the frontend.

## Acceptance criteria

- [ ] For create, update and each action (submit, approve, reject, cancel), a reproduction with `Accept: text/csv` records the HTTP status and body, plus whether the database row, `version`, `status` and item rows changed.
- [ ] For create, the result records whether a request number was consumed and whether a retry with a valid `Accept` creates a second DRAFT.
- [ ] Controls: `Accept: application/json`, `*/*` and a missing `Accept` behave as today (201/200 with body).
- [ ] The detail cache is checked after a confirmed silent change: whether a later `GET /{id}` returns the new or stale state.
- [ ] Evidence records environment, commands, exit codes and observations. It ends with one of three recommendations: no action, document the limitation, or a fix option for Design review.

## Investigation plan

1. Add a disposable reproduction: `@SpringBootTest` + Testcontainers (PostgreSQL and Redis, as the existing integration tests use). For each endpoint, send the request with `Accept: text/csv`, then read the database and cache directly. Keep the test only if R3 is confirmed and a fix follows; otherwise record the result and drop it.
2. Confirm on an isolated Compose project (`home-quiz-task017`, separate ports and volumes) with `curl` and `psql`. Never use `home-quiz-release`, because R3 would write rows.
3. Compare against the controls in the acceptance criteria.
4. If confirmed, write fix options for Design review. The candidate is `produces = [MediaType.APPLICATION_JSON_VALUE]` on the controller's class-level `@RequestMapping`: mapping then rejects the request with 406 before the handler runs. Its effect on `*/*`, missing `Accept`, error responses and CORS preflight must be tested.

## Verification

| Check | Command | Expected evidence |
| --- | --- | --- |
| Reproduction test | In `backend/`, PowerShell: `.\mvnw.cmd --batch-mode test "-Dtest=<reproduction class>"` | Per-endpoint status and state observations; Testcontainers executed, not skipped |
| Live confirmation | `POSTGRES_PORT=55436 REDIS_PORT=56383 BACKEND_PORT=58013 FRONTEND_PORT=53013 docker compose -p home-quiz-task017 --profile app up -d --build --wait`; `curl`; `docker exec ... psql`; then `down -v` for that project only | Same observations as the test; only the task017 project's volumes removed |
| Hygiene | `git diff --check` | Exit 0 |

## Handoff

- Changes: pending
- Evidence: pending (`docs/quality/evidence/TASK-017.md`)
- Decisions / open issues: whether R3 is real; if so, the fix option goes to Design review
- Next action: run the investigation plan
