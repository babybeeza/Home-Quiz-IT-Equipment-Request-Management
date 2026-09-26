# TASK-016: Return correct status for unmatched routes and framework HTTP errors

Status: Done — all gates approved 2026-09-26 for `main` `c7cadf3` with stated limitations
Owner: Backend developer; project owner reviews as technical owner, code reviewer and QA owner
Requirement IDs: REQ-06 (consistent error envelope, 400/404/409/422/500), REQ-11 (backend error tests)
Dependencies: none; found during the [release redeploy](../../quality/evidence/TASK-015-release-deploy.md)

## Human approvals

| Gate | Approver | Decision | Date / revision |
| --- | --- | --- | --- |
| Requirements | Project owner | Covered by the approved REQ-06 baseline | 2026-09-25 |
| Design | Project owner acting as technical owner | Approved D1–D3 | 2026-09-26 |
| Implement | Project owner acting as code reviewer | Approved | 2026-09-26 / `c7cadf3` |
| Verify | Project owner acting as QA / acceptance owner | Approved with stated limitations | 2026-09-26 / `c7cadf3` |
| Delivery | Project owner acting as release owner | Approved with stated limitations | 2026-09-26 / `c7cadf3` |

## Context

[`ApiExceptionHandler`](../../../backend/src/main/kotlin/com/example/equipment/api/ApiExceptionHandler.kt) ends with `@ExceptionHandler(Exception::class)`, which returns 500 `INTERNAL_ERROR` and logs at ERROR level. Spring Boot 4.1 raises framework exceptions for client mistakes: `NoResourceFoundException` for unmatched paths, `HttpRequestMethodNotSupportedException`, `HttpMediaTypeNotSupportedException`, `HttpMediaTypeNotAcceptableException` and `MissingServletRequestParameterException`. Each implements `org.springframework.web.ErrorResponse` and carries its own status, but the catch-all handles them first. Observed on 2026-09-26: `GET /api/v1/nonexistent-xyz` returned HTTP 500 `INTERNAL_ERROR`.

Effects: clients get a server-error status for their own mistakes; ERROR logs and 5xx metrics are inflated; REQ-06 asks for 404 on not-found resources. Contract paths are not affected. `REQUEST_NOT_FOUND`, 409 and 422 responses from the domain stay as they are. Read the [OpenAPI `ApiError`](../../../contracts/openapi.yaml) and [ADR-002](../../architecture/decisions/ADR-002-request-contract-and-domain.md) before editing.

## Scope / non-goals

Add handling so these framework exceptions return their proper 4xx status in the existing `ApiError` envelope, logged below ERROR level. Genuine unexpected exceptions keep 500 `INTERNAL_ERROR` with an ERROR log.

Non-goals: no change to domain exceptions, existing codes, controllers, security/identity headers, frontend, Next.js proxy or actuator.

## Design decisions for review

- **D1 — mechanism (recommended: one handler for `ErrorResponse`).** Add `@ExceptionHandler(ErrorResponse::class)`, which reads `exception.statusCode`, so every current and future Spring MVC client error is covered. The alternative is one explicit handler per exception type, which is narrower but leaves gaps when Spring adds types.
- **D2 — codes (recommended).** 404 → `NOT_FOUND`, 405 → `METHOD_NOT_ALLOWED`, 406 → `NOT_ACCEPTABLE`, 415 → `UNSUPPORTED_MEDIA_TYPE`, missing query parameter 400 → existing `MALFORMED_REQUEST`, other 4xx → `MALFORMED_REQUEST`. A 5xx `ErrorResponse` keeps `INTERNAL_ERROR` and ERROR logging. `NOT_FOUND` is distinct from `REQUEST_NOT_FOUND` so clients can tell "no such route" from "no such request". All match the contract's code pattern.
- **D3 — headers and contract.** Keep Spring's `Allow` header on 405 by copying `exception.headers`. Add the new codes to the OpenAPI `ApiError.code` description. No path or schema shape changes.

## Acceptance criteria

- [x] Given any role, when calling an unmatched `/api/v1/...` path, then the response is 404 with `code: NOT_FOUND`, the request `path`, an empty `fieldErrors` and no ERROR log.
- [x] Given a valid path with an unsupported method (e.g. `DELETE /api/v1/equipment-requests/{id}`), then 405 `METHOD_NOT_ALLOWED` with an `Allow` header.
- [x] Given `POST /api/v1/equipment-requests` with `Content-Type: text/plain`, then 415 `UNSUPPORTED_MEDIA_TYPE`, and no request is created.
- [x] Existing responses are unchanged: `REQUEST_NOT_FOUND` 404, `MALFORMED_REQUEST` 400, `ACCESS_DENIED` 403, 409 conflicts, 422 rules, and 500 `INTERNAL_ERROR` for an unexpected exception (the existing `INTERNAL_ERROR` test in `EquipmentRequestControllerTest.kt`).
- [x] OpenAPI documents the added codes. The backend package passes with Testcontainers tests executed, and evidence records commands and counts.

## Implementation plan

1. Add the `ErrorResponse` handler (D1/D2) above the catch-all in `ApiExceptionHandler.kt`: map `statusCode` to a code, pass `exception.headers`, and log at DEBUG/WARN. Extend the private `error()` helper to accept headers.
2. Add `@WebMvcTest` cases in `EquipmentRequestControllerTest.kt` for 404 unmatched path, 405 with `Allow`, 415, and keep the 500 case. Add one proxy-level check if the Next.js route forwards backend status unchanged.
3. Update `contracts/openapi.yaml` (`ApiError.code` description) and add a TASK-016 amendment note to ADR-002.
4. Record evidence in `docs/quality/evidence/TASK-016.md`; update traceability REQ-06.

## Verification

| Check | Command | Expected |
| --- | --- | --- |
| Backend | In `backend/`: `.\mvnw.cmd --batch-mode clean package` | Exit 0; new tests pass; Testcontainers not skipped (skips are NOT RUN) |
| Frontend | Node 24.15.0 container: `npm ci && npm run lint && npm run typecheck && npm test && npm run build` | Exit 0; unchanged count (no frontend change) |
| Browser regression | Git Bash: `tests/e2e/run-e2e.sh` | 50/50 including AT-33 |
| Live check | After redeploy on an isolated project: `curl` unmatched path, wrong method, wrong media type | 404 / 405 / 415 with `ApiError` bodies |
| Hygiene | `git diff --check`; Markdown link check | Exit 0 |

## Handoff

- Changes: `ErrorResponse` 4xx mapping in `ApiExceptionHandler`, 3 controller tests, OpenAPI `ApiError.code` description, ADR-002 amendment
- Evidence: [TASK-016 evidence](../../quality/evidence/TASK-016.md) — backend 135/135 after review fixes, frontend 32/32, Playwright 50/50, live 404/405/406/415
- Decisions / open issues: Design D1–D3 approved; R1 (bodiless 406) and R2 (fallback tests) fixed on `codex/task-016-review-fixes`; R3 pre-existing candidate follow-up
- Next action: none required; local `home-quiz-release` redeployed from `5392a55` ([evidence](../../quality/evidence/TASK-016.md)); R3 is a candidate follow-up
