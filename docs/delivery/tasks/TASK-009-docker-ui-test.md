# TASK-009: Whole system in Docker for UI testing

Status: Verify approved — Delivery pending
Owner: Developer
Requirement IDs: REQ-13 (delivery; the Docker Compose bonus), supporting the TASK-008 acceptance run
Dependencies: TASK-008

## Human approvals

| Gate | Approver | Decision | Date / revision |
| --- | --- | --- | --- |
| Requirements | Project owner | Approved | 2026-09-25 / user requests: "build เป็น docker สำหรับ test ui", project name `home-quiz`, "compose ให้เหลือ file เดียว" |
| Design | Project owner acting as technical owner | Approved | 2026-09-25 / decisions 1–5 below |
| Implement | Project owner acting as code reviewer | Approved | 2026-09-25 / [TASK-009 evidence](../../quality/evidence/TASK-009.md) |
| Verify | Project owner acting as QA / acceptance owner | Approved | 2026-09-25 / user statement "QA Approved" |

## Design decisions
1. **One `compose.yaml` (project `home-quiz`) with profiles:**
   - default: `postgres` and `redis`, unchanged for host development
   - `app`: adds the backend and frontend images
   - `seed`: the one-shot seed dataset
   - `e2e`: app, seed and Playwright
2. **Images:**
   - `backend/Dockerfile`: multi-stage Temurin 21 JDK → JRE, a non-root user, a liveness healthcheck, and tests skipped in the image build (they need Docker for Testcontainers)
   - `frontend/Dockerfile`: multi-stage Node 24.15 production build (`next start`), non-root
3. **Same-origin API:** in Docker the browser calls `/api/v1`, and Next rewrites it to `API_PROXY_TARGET` (`http://backend:8080`). One URL then works for host browsers and for Playwright inside the network. The rewrite exists only when `API_PROXY_TARGET` is set, so local development is unchanged.
4. **CORS:** `app.cors.allowed-origin` now accepts a comma-separated list. Browsers send `Origin` on same-origin POST/PUT and the proxy forwards it, so Compose allows `http://localhost:${FRONTEND_PORT}` and `http://frontend:3000`. Unlisted origins are still rejected.
5. **Isolated e2e:** `tests/e2e/run-e2e.sh` runs the same file as the project `home-quiz-e2e` on high ports and removes it afterwards. It replaces the TASK-008 host-based runner (host JDK, host jar, `host.docker.internal`).

## Acceptance criteria
- [x] `docker compose --profile app up -d --build --wait` starts a working UI from Docker alone
- [x] The Playwright suite passes against the containerized stack
- [x] One Compose file; the project is named `home-quiz`
- [x] Backend and frontend checks still pass after the CORS and `next.config.ts` changes

## Handoff
QA can run the UI from Docker with one command. Moving from the old default project name leaves the old `home-quiz-it-equipment-request-management_*` volumes and stopped containers in place (see the evidence).
