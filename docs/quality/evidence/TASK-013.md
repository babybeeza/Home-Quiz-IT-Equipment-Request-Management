# Evidence: TASK-013 REQ-13 local release candidate

Date / operator: 2026-09-26 / Codex
Candidate branch: `codex/req13-candidate-20260926` at `https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management.git` (separate worktree; handoff commits add documentation only)
Application revision tested: `bb888a94584d6e6f95f1b43a2064409021fa249f`
Candidate worktree: `E:\wp\Home-Quiz-IT-Equipment-Request-Management-candidate`
Clean verification checkout: detached worktree at `E:\wp\Home-Quiz-IT-Equipment-Request-Management-verify`, outside the active `main` workspace
Environment: Windows / PowerShell, Docker 29.5.2, Compose 5.1.3, Temurin 25.0.3 targeting Java 21, Node 24.15.0-alpine, PostgreSQL 17 and Redis 8 in isolated containers

## Result and scope

The candidate contains the source, root README, Flyway V1/V2, OpenAPI and automated tests. The README has Environment, Run, Test, API, Decisions, Assumptions and Known limitations sections. Verification used a separate checkout and the Compose project `home-quiz-req13` with host ports 55433/56380/58010/53010; it did not use or remove the existing `home-quiz` volumes.

| Check | Command / method | Exit code | Result | Observation |
| --- | --- | --- | --- | --- |
| Candidate checkout | `git worktree add --detach <verify-path> bb888a9`; `git rev-parse HEAD`; `git status --short` | 0 | PASS | Exact commit checked out with no tracked or untracked changes before checks |
| Mandatory artifact inventory | `Test-Path` for source trees, README, Flyway V1/V2, OpenAPI, E2E specs and Compose; `rg` for README headings | 0 | PASS | All named paths present; required README areas found |
| Config parse | `docker compose -f <verify>/compose.yaml -p home-quiz-req13 --profile app config --quiet` | 0 | PASS | Single Compose app profile parsed |
| SSH remote read | `git ls-remote origin HEAD` with noninteractive SSH | 128 | FAIL | `Permission denied (publickey)` from this machine; this does not establish reviewer access |
| HTTPS remote read | `git ls-remote https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management.git HEAD` | 0 | PASS for remote read | Remote HEAD was `8a09c4d`, older than the local candidate; candidate has not been published |
| First frontend attempt | `docker run ... node:24.15.0-alpine ...` | 1 | NOT RUN | Docker Desktop engine was stopped; launched it and retried the same checks |
| Backend build and tests | In clean `backend/`: `.\mvnw.cmd --batch-mode clean package` | 0 | PASS | BUILD SUCCESS; 15 Surefire XML reports sum to 129 tests, 0 failures, 0 errors, 0 skipped. Testcontainers used PostgreSQL and Redis |
| Frontend lint/typecheck/tests/build | Node 24.15.0-alpine container: `npm ci --no-audit --no-fund && npm run lint && npm run typecheck && npm test && npm run build` | 0 | PASS | Lint and typecheck passed; 4 Vitest files, 32/32 tests passed; Next.js production build succeeded |
| Browser acceptance | Git Bash: `tests/e2e/run-e2e.sh` from the clean checkout | 0 | PASS | 50/50 Playwright Chromium tests, including two AT-33 cases; isolated E2E project removed by runner |
| Clean Docker app start | `docker compose -p home-quiz-req13 --profile app up -d --build --wait` with host ports 55433/56380/58010/53010 | 0 | PASS | PostgreSQL, Redis, backend and frontend all healthy; Flyway applied schema in a new volume |
| Smoke through frontend proxy | PowerShell HTTP: Employee create → submit; Approver approve → filtered list | 0 | PASS | 201 DRAFT v0, 200 PENDING, 200 APPROVED, 200 list containing the request |
| Isolated cleanup | Verify Compose project labels/volume names, then `docker compose -p home-quiz-req13 --profile app down -v` | 0 | PASS | Removed only the `home-quiz-req13` app containers, network and two test volumes |
| HTTPS branch push dry run | `git push --dry-run https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management.git HEAD:refs/heads/codex/req13-candidate-20260926` | 0 | PASS | New branch would be accepted; publication and independent reviewer fetch are separate checks |
| Branch publication | `git push https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management.git HEAD:refs/heads/codex/req13-candidate-20260926` | 0 | PASS | Created review branch at documentation commit `f389e64e27f0d20115fa744184e3087673bba491` |
| Reviewer access probe | `git -c credential.helper= ls-remote https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management.git refs/heads/codex/req13-candidate-20260926`, with terminal prompts disabled | 0 | PASS | Unauthenticated HTTPS read resolved `f389e64e27f0d20115fa744184e3087673bba491`; actual reviewer clone remains unobserved |
| Manual P2 AT-44/AT-46; application rollback/data restore | Manual cases and runbook operations | N/A | NOT RUN | No new rehearsal in this task; limitations remain explicit |

## Delivery status

The application revision passed clean-checkout automation and smoke. Handoff commits add evidence and status documentation without changing application code or manifests. The candidate branch is published and an unauthenticated HTTPS remote read succeeded. TASK-010 Design/Implement/Verify gates await human review. TASK-011/H-1 awaits the source-storage owner's decision; two workspace GETs returned 403 but do not prove revocation. A-08's owner decision and the release owner's Delivery decision are pending. No approval is inferred from passing tests.
