# Evidence: TASK-003 implementation

Date / operator: 2026-09-25 / Codex (implementation), Claude Code (review rounds 1–2); Implement approved by project owner acting as code reviewer on 2026-09-25
Approved Design revision: `76fa110`
Environment: Windows, Temurin Java 25.0.3 targeting Java 21, Node 24.15.0 container, PostgreSQL 17.11 in Docker
Requirements: REQ-01, REQ-03, REQ-04, REQ-06, REQ-07, REQ-08, REQ-09, REQ-11

| Check | Command / method | Result | Observation |
| --- | --- | --- | --- |
| Backend clean package | `backend\\mvnw.cmd --batch-mode clean package` | PASS | 21 tests, 0 failures/errors/skips; executable jar created |
| Frontend quality checks | Node 24.15 container: `npm run lint && npm run typecheck && npm test && npm run build` | PASS | ESLint and TypeScript passed; 6 behavior tests passed; all routes built |
| JPA/Flyway startup | Start packaged jar against Compose PostgreSQL | PASS | Flyway V1 current; Hibernate `ddl-auto=validate` accepted mappings; application started on 8080 |
| Create/read/update | POST, owner GET and item-only PUT with identity headers | PASS | 201 DRAFT `REQ-2026-000001`, version 0; update persisted replacement item and returned version 1 |
| Authorization | Owner GET, non-owner GET, Approver POST | PASS | 200, 403 and 403 respectively; identity came only from headers |
| Error contract | Unknown UUID, missing identity, stale PUT and invalid nested quantity | PASS | 404, 400, 409 and 400; nested path returned as `items[0].quantity` |
| Concurrent update | Two parallel PUTs with the same version against PostgreSQL | PASS | Responses were 200 and 409; database changed from version 1 to 2 exactly once with one item |
| Aggregate rollback | Temporary DB check rejected a valid-at-application HEADSET child during POST | PASS | API returned sanitized 500; persisted request/item counts for the probe remained `0|0`; temporary check removed |
| Frontend form behavior | Vitest + React Testing Library | PASS | Dynamic items, client/server errors, nested mapping, duplicate save, success navigation/reset and 409 value preservation covered |
| Frontend route smoke | Production Next server on alternate port 3100 | PASS | `/requests`, `/requests/new` and detail route returned 200; port 3000 was occupied by an unrelated local container |
| Configurable CORS | Preflight with `FRONTEND_ORIGIN=http://localhost:3100` | PASS | 200 with `Access-Control-Allow-Origin: http://localhost:3100` |

## Review round 1 (2026-09-25, Claude Code)

The original checks were rerun independently before the fixes: backend exit 0 with 23 tests, which differs from the 21 recorded above, and frontend exit 0 with 6 tests. Code review found three defects, each fixed with a regression test:

| Finding | Fix | Regression test |
| --- | --- | --- |
| Saving did not update the detail query cache, so detail showed pre-edit data for up to `staleTime` (15 s) and a following edit sent a stale version, causing a 409 against the user's own save | The form hook writes the saved request into the shared `equipmentRequestQueryKey` cache before navigating | `stores the saved request in the detail cache before navigating` |
| Any new `request` prop, such as a window-focus refetch, reset the form and advanced `expectedVersion`, silently discarding input and bypassing the 409 | The form pins its loaded request as the edit baseline, which only a successful save moves; the edit route disables focus/reconnect refetch | `keeps in-progress edits and the loaded version when newer data arrives` |
| All items shared one `createdAt`, so the response order after reload depended on PostgreSQL row order | Items get increasing timestamps 1 µs apart in submitted order; no migration was needed | `items keep submitted order when persistence returns them unordered` |

| Check | Command / method | Result | Observation |
| --- | --- | --- | --- |
| Backend clean package after fixes | `backend\\mvnw.cmd --batch-mode -q clean package` | PASS (exit 0) | 24 tests, 0 failures/errors |
| Frontend quality checks after fixes | Node 24.15.0-alpine container, isolated `node_modules`/`.next` volumes: `npm ci && npm run lint && npm run typecheck && npm test && npm run build` | PASS (exit 0) | Lint and typecheck passed; 8 tests passed; all routes built |
| PostgreSQL / API smoke after fixes | — | Superseded | Run in round 2 below |

## Review round 2 (2026-09-25, Claude Code)

| Note from round 1 | Outcome |
| --- | --- |
| PUT has no `@Valid`, unlike POST | No change needed. [api-behavior](../architecture/api-behavior.md) requires ownership → version → state → validation precedence, and `@Valid` would return 400 before 403/409. A controller comment explains this, and a new MVC test freezes the order. Adding `@Valid` temporarily made that test fail (mutation check). |
| The test design lists MVC tests, but none exist | Added `EquipmentRequestControllerTest` (`@WebMvcTest` with the real service and mocked repositories): 201 payload without `ownerId`, missing/unknown/blank identity headers, malformed JSON, enum and UUID, nested `items[0].quantity`, Approver create 403, 404 envelope, update error precedence, DB optimistic-lock → 409, sanitized 500, and CORS preflight |
| The application service imports API DTOs | The service now takes domain `EquipmentRequestDraft` and returns `EquipmentRequestView`, and `api/EquipmentRequestMapping.kt` maps DTO ↔ draft/view. ADR-003's layer wording was amended to match, with no behavior or contract change |

| Check | Command / method | Result | Observation |
| --- | --- | --- | --- |
| Backend clean package | `backend\\mvnw.cmd --batch-mode clean package` | PASS (exit 0) | 34 tests (10 MVC, 11 service, 13 domain/foundation), 0 failures/errors/skips. The expected `Unexpected request failure` log comes from the sanitized-500 test |
| Error-precedence mutation check | Temporarily add `@Valid` to PUT, then run `-Dtest=EquipmentRequestControllerTest` | FAIL as intended | The precedence test failed at the validation step; the source was restored |
| Frontend | — | Not rerun | No frontend or contract change since round 1 (8 tests PASS) |
| PostgreSQL smoke | Packaged jar against Compose PostgreSQL 17.11 on Temurin 25.0.3 | PASS | Flyway validated V1 and the app started. A 5-item create returned the same submitted order on 3 GETs. A reordered 6-item item-only PUT returned version 1 and kept its order on 2 GETs, with DB `created_at` 1 µs apart in order. Stale PUT → 409 with version unchanged at 1. With a complete but invalid body: non-owner → `ACCESS_DENIED`, owner with stale version → `REQUEST_VERSION_CONFLICT`, owner with current version → `VALIDATION_ERROR` including `items[0].quantity` |

Observation: a PUT body with missing required JSON properties fails deserialization and returns 400 `MALFORMED_REQUEST` before the ownership check. This is syntax-level rejection, so no request content is revealed. Smoke rows (for example `REQ-2026-000003`) remain in the local development database, and the Compose services were stopped afterwards.

## Implemented boundaries

- Backend exposes POST collection plus GET/PUT detail with controller, DTO, application, domain, persistence and exception boundaries.
- Request/items are one JPA aggregate with orphan removal and parent `@Version`; every accepted edit touches the parent.
- Frontend provides demo identity selection, typed API access and new/detail/edit routes using React Hook Form 7.88.0, Zod 4.6.5 and TanStack Query 5.103.2.
- `useEquipmentRequestForm` owns editable state and error/conflict handling; query state is actor-scoped and changing actor clears it.

## Scope notes

- Submit/cancel/approve/reject remain TASK-004.
- Searchable request list remains TASK-005; `/requests` is currently an entry page.
- Redis/Caffeine behavior remains TASK-006.
- Browser-specific native unload prompt wording is controlled by the browser; automated coverage verifies listener behavior through the hook/form contract while manual route smoke verifies rendered pages.

## Approval needed

Code reviewer must approve the TASK-003 code, tests and this evidence before TASK-004 begins.

