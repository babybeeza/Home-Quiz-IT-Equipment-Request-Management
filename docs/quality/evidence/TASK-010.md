# Evidence: TASK-010 list-row actions / AT-33

Date / operator: 2026-09-25 / Codex
Revision: working tree based on `8a09c4d`; includes the preceding TASK-012 delivery documentation, with no commit claimed
Environment: Windows / PowerShell, Docker Compose v5.1.3, Git Bash, Node 24.15.0-alpine for frontend checks, PostgreSQL 17 / Redis 8 in isolated Compose E2E project
Requirement: REQ-05, original assignment §4.2, AT-33 (P1)

## Changed behavior

The list has an action column. The Employee owner sees Edit/Submit/Cancel on DRAFT and Cancel on PENDING. The Approver sees Approve/Reject on PENDING. Terminal rows have no mutations. The shared action component sends the row's `expectedVersion`; a successful action refreshes actor-scoped lists, and a conflict offers an explicit reload. The URL filters and page remain unchanged by a row action. Backend role, ownership, transition and version enforcement are unchanged.

The assignment §4.2 PENDING table includes Cancel without naming an actor. The accepted role matrix in `ui-flow.md` and backend contract permits only the Employee owner to cancel; Approvers do not receive a Cancel control. ADR-005 records this interpretation for current review.

## Checks

| Check | Exact command / method | Exit code | Result | Observation |
| --- | --- | --- | --- | --- |
| Frontend lint, typecheck, tests, production build | `docker run --rm -v "${PWD}:/workspace" -w /workspace/frontend node:24.15.0-alpine sh -c "npm ci --no-audit --no-fund && npm run lint && npm run typecheck && npm test && npm run build"` | 0 | PASS | 32 Vitest tests passed, including 7 list tests; Next compiled and generated all routes |
| Docker Playwright acceptance | `& 'C:\Program Files\Git\bin\bash.exe' tests/e2e/run-e2e.sh` | 0 | PASS | 50/50 browser tests; AT-33 row actions and stale conflict both passed. Runner removed its isolated project and volumes |
| Default `bash` invocation | `bash tests/e2e/run-e2e.sh` | 1 | NOT RUN | This Windows `bash` points to WSL, whose `/bin/bash` is unavailable; the same script ran through Git Bash above |
| Backend build/tests | — | N/A | NOT RUN | No backend, contract or migration changed; the Docker build used the existing backend image recipe |

## Review and handoff

AT-33 now passes against the source list-row criterion in Chromium. AT-44 and AT-46 remain manual P2 cases as before. ADR-005, the acceptance document and traceability changed with the UI; Design, Implement and Verify for this revision are In review under the repository gate rule. No human approval is inferred from these automated results. H-1 and the Delivery gate remain open.
