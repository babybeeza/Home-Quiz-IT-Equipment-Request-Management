# Evidence: TASK-017 non-JSON Accept on mutations (R3)

Date / operator: 2026-09-26 / Claude Code
Revision: `main` `96fc860` (application content equal to TASK-016 `c7cadf3`), branch `codex/task-017-accept-mutation-check`
Environment: Windows 11, Git Bash, Docker 29.5.2, Compose 5.1.3; isolated project `home-quiz-task017` (PostgreSQL 17, Redis 8, backend :58013, frontend :53013)
Request: user “เริ่ม” on 2026-09-26 for the [TASK-017](../../delivery/tasks/TASK-017-accept-before-mutation.md) investigation

## Result

**R3 is confirmed on all six mutating endpoints.** With `Accept: text/csv`, each request returns 406 with an empty body, yet the change is committed. The client is told the request failed when it succeeded.

| Endpoint | Response | Database afterwards | Retry with `Accept: application/json` |
| --- | --- | --- | --- |
| `POST /` create | 406, empty | New DRAFT v0 `REQ-2026-000001` | 201; a **second DRAFT** `REQ-2026-000002` with the same title |
| `PUT /{id}` (v0) | 406, empty | DRAFT v1, new title | 409 `REQUEST_VERSION_CONFLICT` |
| `POST /{id}/submit` (v1) | 406, empty | PENDING v2 | 409 `REQUEST_VERSION_CONFLICT` |
| `POST /{id}/approve` (v2) | 406, empty | APPROVED v3 | — |
| `POST /{id}/reject` (v1) | 406, empty | REJECTED v2 | — |
| `POST /{id}/cancel` (v0) | 406, empty | CANCELLED v1 | — |

Controls: create with `Accept: */*` and with no `Accept` returned 201 with a body. Cache: after warming `GET /{id}` and then the silent PUT, a new `GET` returned the new title and version 1. Eviction happens on commit, so the cache is not stale. Logs had no "Failure in @ExceptionHandler" or "Unexpected request failure" lines.

Impact: a direct API client that sends a non-JSON `Accept` and retries on error creates duplicate requests and consumes request numbers. It also gets 409 for transitions it has in fact already applied. Ownership, role and state rules are still enforced, and the data stays consistent: every change is a legal one the actor was allowed to make. The UI is not exposed because it sends no `Accept` header.

## Checks

| Check | Command / method | Exit | Result | Observation |
| --- | --- | ---: | --- | --- |
| Isolated stack | `POSTGRES_PORT=55436 REDIS_PORT=56383 BACKEND_PORT=58013 FRONTEND_PORT=53013 docker compose -p home-quiz-task017 --profile app up -d --build --wait` | 0 | PASS | Four services healthy |
| Live reproduction | Scratchpad script: `curl` per endpoint with `Accept: text/csv`, JSON retry, controls; `docker exec ... psql` for row state and request numbers | 0 | CONFIRMED | Table above |
| Cleanup | Verified project volume labels, then `docker compose -p home-quiz-task017 --profile app down -v` | 0 | PASS | Only task017 containers, network and two volumes removed; `home-quiz-release` untouched |
| Testcontainers reproduction test | Plan step 1 | N/A | NOT RUN | The live run already used real HTTP, PostgreSQL and Redis and was conclusive. The automated check belongs with the fix as a regression test, asserting 406 **and** no state change; a test asserting the defect would be deleted once fixed |

## Recommendation

Fix it, pending Design review. Candidate: `produces = [MediaType.APPLICATION_JSON_VALUE]` on the class-level `@RequestMapping` of `EquipmentRequestController`, and likewise on `ReferenceDataController` for consistency. Spring then rejects the request with 406 during handler mapping, before the handler runs, so nothing is committed. Design must confirm:
- `*/*`, no `Accept`, and `application/json` stay 2xx
- `application/*+json` behaves as intended
- error envelopes still render
- CORS preflight still works
- the TASK-016 bodiless-406 handler still applies

Regression tests should assert 406 with no row, version or status change for create and one transition, at both MockMvc and Testcontainers level.
