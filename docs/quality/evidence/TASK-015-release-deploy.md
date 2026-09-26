# Evidence: REQ-14 local release stack redeploy

Date / operator: 2026-09-26 / Claude Code
Revision deployed: `main` `55974760a06db2f038ce7159592830ce36199f69` (PR #13 merge; contains approved theme revision `5a3b2e1`)
Environment: Windows 11, Git Bash, Docker 29.5.2, Compose 5.1.3; working directory `E:\wp\Home-Quiz-IT-Equipment-Request-Management`
Request: user asked “Release stack re deploy” on 2026-09-26; recorded in [approvals](../../governance/approvals.md)

## Before

`home-quiz-release` ran from the `-release-record` worktree, then at `717bbfb`, which predates the theme stylesheet. `git diff 717bbfb origin/main -- compose.yaml backend frontend/Dockerfile frontend/package.json` was empty: only frontend source changed. Neither checkout has a `.env`; the resolved Compose ports for project `home-quiz-release` are the defaults 3000/8080/5432/6379, matching the running stack.

Before the redeploy, local `main` sat at `8a09c4d` and held uncommitted files identical to merged commit `642dc12`. Each file was compared with that commit before `git stash push -u`. After the stash, `git merge --ff-only origin/main` fast-forwarded `main` to `5597476`. The stash is kept.

## Checks

| Check | Command / method | Exit | Result | Observation |
| --- | --- | ---: | --- | --- |
| Redeploy | `docker compose -p home-quiz-release --profile app up -d --build --wait` from the `main` worktree | 0 | PASS | Frontend and backend rebuilt and recreated; postgres and redis unchanged (created 10:39, still running); all four healthy |
| Volumes kept | `docker volume ls --filter label=com.docker.compose.project=home-quiz-release`; postgres container not recreated | 0 | PASS | `home-quiz-release_postgres_data` and `_redis_data` reused; Flyway V1/V2 successful; `equipment_requests` held 0 rows before and after |
| UI route | `curl http://localhost:3000/requests` | 0 | PASS | HTTP 200 |
| Theme served | Fetch the page's CSS chunk and search for tokens | 0 | PASS | Served CSS contains `--spark-dark`, `--spark-light`, `--spark-blue` and the other Spark tokens from `frontend/src/app/styles.css` |
| Backend health | `curl http://localhost:8080/actuator/health` | 0 | PASS | HTTP 200, `UP` |
| Read-only API via proxy | `GET /api/v1/equipment-requests?page=0&size=5` with `X-User-Id: approver-001`, `X-Role: APPROVER` on ports 3000 and 8080 | 0 | PASS | HTTP 200, empty page |
| Write smoke | create → submit → approve | N/A | NOT RUN | Skipped so no test rows land in the release database; TASK-013 ran the write smoke on an isolated project, and backend code is unchanged since then |
| Frontend lint/typecheck/tests/build (rerun at owner request) | `docker run --rm -v <repo>:/workspace -w /workspace/frontend node:24.15.0-alpine sh -c "npm ci --no-audit --no-fund && npm run lint && npm run typecheck && npm test && npm run build"` on `6c5769b` | 0 | PASS | ESLint `--max-warnings=0` and typecheck clean; 4 Vitest files, 32/32 tests; Next.js build compiled |
| Playwright acceptance (rerun at owner request) | Git Bash: `tests/e2e/run-e2e.sh` on `6c5769b` | 0 | PASS | 50/50 Chromium tests including both AT-33 cases; runner removed its isolated project, `home-quiz-release` untouched |

## Finding

An unknown API path (for example `GET /api/v1/nonexistent-xyz`) returns HTTP 500 `INTERNAL_ERROR` instead of 404. The catch-all handler in `backend/src/main/kotlin/com/example/equipment/api/ApiExceptionHandler.kt` handles unmatched routes. Backend code is unchanged since the approved REQ-13 revision, so this predates REQ-14 and is outside the contract paths. It was not fixed here; [TASK-016](../../delivery/tasks/TASK-016-framework-error-status.md) plans the fix.

## Limitations

This is a local Docker redeploy, not a cloud deployment. The previous frontend/backend images were not tagged for rollback; rolling back means running the same command from a checkout of the earlier revision. Manual AT-44/AT-46 and data restore remain NOT RUN as before.
