# Evidence: TASK-009 whole system in Docker for UI testing

Date / operator: 2026-09-25 / Claude Code; Design and Implement approved by project owner on 2026-09-25
Environment:
- Windows 11, Docker 29.5.2 (BuildKit)
- images `home-quiz-backend:local` (597 MB, Temurin 21 JRE) and `home-quiz-frontend:local` (941 MB, Node 24.15 alpine)
- `mcr.microsoft.com/playwright:v1.63.0-noble`

| Check | Command | Result | Observation |
| --- | --- | --- | --- |
| Playwright on the first Docker layout | `run-e2e.sh` (separate UI-test compose file) | 36 passed / **13 failed** | Every browser POST/PUT failed with "ไม่สามารถดำเนินการได้". Cause below |
| MVC tests after the CORS fix | `mvnw test -Dtest=EquipmentRequestControllerTest` | PASS | New test: a configured origin (`http://frontend:3000`) may POST; an unlisted origin gets 403 |
| Playwright on the single `compose.yaml` | `bash tests/e2e/run-e2e.sh` (project `home-quiz-e2e`) | **PASS: 49/49** in 42.0 s, exit 0 | Same suite as TASK-008, now fully containerized |
| Manual QA path | `FRONTEND_PORT=3300 BACKEND_PORT=8090 docker compose --profile app up -d --build --wait` | PASS | All 4 services healthy. From the host: page 200; proxied GET 200; proxied POST with `Origin: http://localhost:3300` → 201 DRAFT; unlisted origin → 403; reference data 6 departments; direct backend 200 |
| Backend release build | `mvnw --batch-mode clean package` | PASS (exit 0) | 129 tests, 0 skipped |
| Frontend checks | Node 24.15 container: lint, typecheck, test, build | PASS (exit 0) | 31 tests; build compiles with the conditional rewrite |
| Compose validity | `docker compose config` (default and `--profile e2e`) | PASS | Services: postgres, redis, backend, frontend, seed, e2e |

## Root cause of the 13 failures

- Chrome sends an `Origin` header on POST/PUT even to the same origin. It does not send one on GET, which is why list and detail cases passed.
- The Next.js proxy forwards that header, and the backend allowed only its single default origin (`http://localhost:3000`).
- Spring therefore answered 403 "Invalid CORS request" as plain text, which the UI displays as its generic "ไม่สามารถดำเนินการได้".
- This was a configuration gap in the new Docker layout, not an application defect.
- **Fix:** `app.cors.allowed-origin` accepts a comma-separated list, and Compose passes the host and in-network origins. Anything else is still rejected, as the new MVC test and the host smoke check show.

## Replaced files

`backend/Dockerfile` and `frontend/Dockerfile` already existed from the bootstrap commit `8c6de53`. They were overwritten without being read first; that was noticed and reviewed afterwards. Nothing referenced them: TASK-001 evidence records that they were never built.

| File | Original | Now |
| --- | --- | --- |
| `backend/Dockerfile` | Alpine JDK build that ran the test suite inside the image, then an Alpine JRE | JDK build with `-DskipTests` (tests need Docker for Testcontainers and run separately), a Maven cache mount, a CRLF-safe wrapper, JRE runtime, a non-root user and a liveness healthcheck |
| `frontend/Dockerfile` | Development image (`npm run dev`) | Production build served by `next start`, non-root, with a healthcheck and the `/api/v1` proxy target |

## Project rename (`home-quiz`) and environment notes

- **The rename leaves the old project behind.** The Compose project was previously named after the directory, `home-quiz-it-equipment-request-management`, so its stopped containers and volumes (`…_postgres_data`, `…_redis_data`, which hold the earlier smoke and seed data) still exist.
  - They were **not deleted**, because deleting user data needs the owner's decision.
  - Their containers were stopped because they held ports 5432/6379.
  - To remove them: `docker compose -p home-quiz-it-equipment-request-management down -v`.
- **Stuck networking on the first start.** The first `up` attempt hit that port conflict and left the postgres container with half-configured networking, so the backend could not connect. `docker compose --profile app down`, then `up` again, fixed it (volumes kept).
- **Ports in this environment.** Host port 3000 belongs to an unrelated project here, so the manual check used `FRONTEND_PORT=3300` and `BACKEND_PORT=8090`. The defaults remain 3000/8080.
- **Stack left running.** The `home-quiz` app stack was left running on :3300/:8090 for the owner to try (`docker compose --profile app down` stops it).

## Follow-ups (not done)

- **Frontend image size:** 941 MB, because it carries the full production `node_modules`. Next.js `output: "standalone"` would cut it substantially.
- **Browser coverage:** Chromium only, as in TASK-008.
