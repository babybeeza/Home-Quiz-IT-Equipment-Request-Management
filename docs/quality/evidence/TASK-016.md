# Evidence: TASK-016 framework error statuses

Date / operator: 2026-09-26 / Claude Code
Revision: branch `codex/task-016-framework-error-status` from `main` `b9f39d0`, working tree with the TASK-016 change (commit follows this evidence)
Environment: Windows 11; PowerShell and Git Bash; Temurin 25.0.3 targeting Java 21; Docker 29.5.2, Compose 5.1.3; Node 24.15.0-alpine container; PostgreSQL 17 / Redis 8 in isolated Compose projects
Requirement: REQ-06, REQ-11; Design D1–D3 approved 2026-09-26 ([approvals](../../governance/approvals.md))

## Changed behavior

`ApiExceptionHandler.unexpected` now checks whether the exception is a Spring `ErrorResponse` with a 4xx status. If so, it returns that status in the shared `ApiError` envelope with `NOT_FOUND`, `METHOD_NOT_ALLOWED`, `NOT_ACCEPTABLE` or `UNSUPPORTED_MEDIA_TYPE`, otherwise `MALFORMED_REQUEST`. It copies the exception's headers (for example `Allow`) and logs at DEBUG. Every other exception still returns 500 `INTERNAL_ERROR` with an ERROR log. Explicit handlers for domain and validation errors are unchanged. OpenAPI documents the codes in `ApiError.code`, and ADR-002 has a TASK-016 amendment.

Deviation from D1 wording, same behavior: D1 named `@ExceptionHandler(ErrorResponse::class)`, but the annotation accepts only `Throwable` types and `ErrorResponse` is an interface. The check therefore sits inside the catch-all handler.

## Checks

| Check | Command / method | Exit | Result | Observation |
| --- | --- | ---: | --- | --- |
| Controller tests | In `backend/`, PowerShell: `.\mvnw.cmd --batch-mode test "-Dtest=EquipmentRequestControllerTest"` | 0 | PASS | 29/29, including 404, 405 + `Allow`, and 415 with no save |
| First full backend attempt | Git Bash `cmd //c "mvnw.cmd ..."` | 1 | NOT RUN | `'mvnw.cmd' is not recognized`; invocation error, no tests ran |
| Backend package | In `backend/`, PowerShell: `.\mvnw.cmd --batch-mode clean package` | 0 | PASS | BUILD SUCCESS; 15 Surefire reports, 132 tests, 0 failures, 0 errors, 0 skipped (Testcontainers ran) |
| Frontend | `docker run ... node:24.15.0-alpine sh -c "npm ci --no-audit --no-fund && npm run lint && npm run typecheck && npm test && npm run build"` | 0 | PASS | Lint/typecheck clean; 32/32 Vitest; build compiled. No frontend change |
| Browser acceptance | Git Bash: `bash tests/e2e/run-e2e.sh` | 0 | PASS | 50/50 Chromium, including AT-33; isolated project removed by runner |
| Live check | Isolated `home-quiz-task016` (ports 55435/56382/58012/53012), `docker compose ... up -d --build --wait`, then `curl` | 0 | PASS except 406 body | Unmatched path 404 `NOT_FOUND` (backend and via frontend proxy); `DELETE /{id}` 405 `METHOD_NOT_ALLOWED`, `Allow: PUT, GET`; `text/plain` POST 415 `UNSUPPORTED_MEDIA_TYPE`; `Accept: text/csv` 406 with empty body; unchanged: missing id 404 `REQUEST_NOT_FOUND`, missing headers 400 `MALFORMED_REQUEST`, health 200. 0 "Unexpected request failure" log lines |
| Cleanup | Verified project volume labels, then `docker compose -p home-quiz-task016 --profile app down -v` | 0 | PASS | Only the task016 containers, network and two volumes removed; `home-quiz-release` and `home-quiz-theme` untouched |
| Hygiene | `git diff --check` | 0 | PASS | |

## Review findings

| # | Severity | File / line | Finding | Evidence | Disposition |
| --- | --- | --- | --- | --- | --- |
| R1 | Low | `ApiExceptionHandler.kt` `unexpected` | For 406, the client refuses JSON, so the `ApiError` body cannot be written. Spring logs WARN "Failure in @ExceptionHandler", falls back to `DefaultHandlerExceptionResolver`, and returns 406 with an empty body and a duplicated `Accept` header. The status is correct, but the log is noisy. | Live check and backend log above | Fixed in the review-fix revision below |
| R2 | Low | `EquipmentRequestControllerTest.kt` | The fallback branch (other 4xx → `MALFORMED_REQUEST`) and a 5xx `ErrorResponse` staying `INTERNAL_ERROR` have no automated test. | Code review | Fixed in the review-fix revision below |
| R3 | Info, pre-existing, unverified | Controller methods without `produces` | A mutating POST/PUT sent with an `Accept` header that excludes JSON may commit before response writing fails with 406. Not introduced by TASK-016. | Reasoning from the Spring return-value flow; not reproduced | Candidate follow-up; verify before acting |

No finding affects ownership, state transitions, version checks or cache invalidation: no domain, persistence or cache code changed.

## Review fixes (R1, R2)

PR #15 merged at `6e4efc0` before these fixes were ready. On 2026-09-26 the owner asked for R1 and R2 to be fixed, so the fixes went on branch `codex/task-016-review-fixes` from that merge.

- R1: a dedicated `HttpMediaTypeNotAcceptableException` handler returns 406 with no body and logs at DEBUG. `NOT_ACCEPTABLE` was removed from the code map because it is now unreachable, and OpenAPI and ADR-002 were updated to match. This refines D2: a 406 has no `ApiError` body, since the client accepts no JSON.
- R2: `ApiExceptionHandlerTest` covers a 4xx `ErrorResponse` outside the map (`MissingServletRequestParameterException` → 400 `MALFORMED_REQUEST`) and a 5xx `ErrorResponse` (`ResponseStatusException` 503 → 500 `INTERNAL_ERROR`). The controller test adds `Accept: text/csv` → 406 with an empty body.

| Check | Command / method | Exit | Result | Observation |
| --- | --- | ---: | --- | --- |
| Targeted tests | PowerShell: `.\mvnw.cmd --batch-mode test "-Dtest=EquipmentRequestControllerTest,ApiExceptionHandlerTest"` | 0 | PASS | 32/32 (30 + 2) |
| Backend package | PowerShell: `.\mvnw.cmd --batch-mode clean package` | 0 | PASS | 16 Surefire reports, 135 tests, 0 failures, 0 errors, 0 skipped |
| Live check | Isolated `home-quiz-task016`, same ports, rebuilt; `curl` | 0 | PASS | 406 `Content-Length: 0`, no duplicated header; 404/405/415 unchanged; 0 "Failure in @ExceptionHandler" or "Unexpected request failure" log lines. Stack removed with `down -v` afterwards |
| Browser acceptance | Git Bash: `bash tests/e2e/run-e2e.sh` | 0 | PASS | 50/50 Chromium, including AT-33; isolated project removed by runner |
| Frontend | — | N/A | NOT RUN | No frontend change since the 32/32 run above |

## Deliver and learn

Readiness: all TASK-016 acceptance checks have evidence. Implement, Verify and Delivery gates await the project owner. R1 and R2 are fixed. R3 remains a pre-existing, unverified candidate follow-up. The local `home-quiz-release` stack does not include TASK-016 until redeployed on request.

Lessons:
- PR #14 was merged before the second commit (`179a1a2`, rerun evidence plus the TASK-016 plan) was pushed, so that commit missed `main`. It was cherry-picked onto the TASK-016 branch. PR #15 then merged before R1/R2 were fixed, and a state check caught it this time, so the fixes went on a new branch. Before adding commits to an open PR, check `gh pr view <n> --json state`; after a merge, check that the expected commits are ancestors of `main`.
- In Git Bash, `cmd //c "mvnw.cmd ..."` does not resolve the wrapper. Use PowerShell `.\mvnw.cmd`, as the README states.
