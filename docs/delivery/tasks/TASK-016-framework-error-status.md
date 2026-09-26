# TASK-016: Return correct status for unmatched routes and framework HTTP errors

Status: Planned — Design review pending
Owner: Backend developer; project owner reviews as technical owner, code reviewer and QA owner
Requirement IDs: REQ-06 (consistent error envelope, 400/404/409/422/500), REQ-11 (backend error tests)
Dependencies: none; found during the [release redeploy](../../quality/evidence/TASK-015-release-deploy.md)

## Human approvals

| Gate | Approver | Decision | Date / revision |
| --- | --- | --- | --- |
| Requirements | Project owner | Covered by the approved REQ-06 baseline | 2026-09-25 |
| Design | Project owner acting as technical owner | Pending — decisions D1–D3 below | — |
| Implement | Project owner acting as code reviewer | Pending | — |
| Verify | Project owner acting as QA / acceptance owner | Pending | — |
| Delivery | Project owner acting as release owner | Pending | — |

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

- [ ] Given any role, when calling an unmatched `/api/v1/...` path, then the response is 404 with `code: NOT_FOUND`, the request `path`, an empty `fieldErrors` and no ERROR log.
- [ ] Given a valid path with an unsupported method (e.g. `DELETE /api/v1/equipment-requests/{id}`), then 405 `METHOD_NOT_ALLOWED` with an `Allow` header.
- [ ] Given `POST /api/v1/equipment-requests` with `Content-Type: text/plain`, then 415 `UNSUPPORTED_MEDIA_TYPE`, and no request is created.
- [ ] Existing responses are unchanged: `REQUEST_NOT_FOUND` 404, `MALFORMED_REQUEST` 400, `ACCESS_DENIED` 403, 409 conflicts, 422 rules, and 500 `INTERNAL_ERROR` for an unexpected exception (the existing `INTERNAL_ERROR` test in `EquipmentRequestControllerTest.kt`).
- [ ] OpenAPI documents the added codes. The backend package passes with Testcontainers tests executed, and evidence records commands and counts.

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

- Changes: pending
- Evidence: pending
- Decisions / open issues: D1–D3 need technical-owner approval before Implement
- Next action: owner reviews D1–D3; then implement on a `codex/task-016-*` branch
