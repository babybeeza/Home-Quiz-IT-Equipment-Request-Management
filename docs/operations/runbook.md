# Delivery and operations runbook

Status: local environment only; Delivery gate pending. The published candidate branch is `codex/req13-candidate-20260926` at `https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management.git`; its application revision `bb888a94584d6e6f95f1b43a2064409021fa249f` passed clean-checkout build, automated acceptance and smoke ([TASK-013 evidence](../quality/evidence/TASK-013.md)). The host-based recovery steps were executed in [TASK-007](../quality/evidence/TASK-007.md). No cloud deployment exists or is claimed.

For the current Docker UI path, run `docker compose --profile app up -d --build --wait`, open `http://localhost:3000/requests`, and stop with `docker compose --profile app down`. Set `FRONTEND_PORT` and `BACKEND_PORT` before starting if the defaults are occupied; use the same values for `down`. `down -v` deletes the project database and Redis volumes. The isolated acceptance runner is `bash tests/e2e/run-e2e.sh`; it removes its own project and volumes unless `KEEP_STACK=1` is set.

## Before delivery
- [x] Candidate branch, tested application revision, environment and runtime versions: see the README Environment section and TASK-013 evidence; final handoff revision still requires release-owner acceptance
- [x] README install/run/test works from a clean checkout (rehearsed; environment port conflicts noted)
- [x] Build, automated acceptance and performance checks have recorded evidence; API, schema and limitations were audited in TASK-007–009
- [x] Configuration variables documented without secret values (`.env.example` holds local-only defaults)
- [x] Migrations applied to an empty database (Flyway V1 + V2); backup/restore approach below
- [x] Smoke: create draft → submit → decision → list (rehearsal HTTP journey)
- [ ] AT-33 / G-1: list-row actions are implemented and automated AT-33 passes; obtain Design/Implement/Verify review for the changed revision ([TASK-010](../delivery/tasks/TASK-010-list-row-actions.md))
- [x] H-1 source-owner disposition: user confirmed the token revoked on 2026-09-26; the saved link returned HTTP 403 again from this workspace. The original URL text remains in published Git history; release owner assesses that residual limitation ([TASK-011 evidence](../quality/evidence/TASK-011.md))
- [ ] Release owner to accept the published GitHub branch/access method and final immutable commit at the Delivery gate; an independent clone with credential helpers disabled succeeded, but no reviewer sign-off is recorded
- [ ] Application rollback and data restore rehearsal; documented below but NOT RUN
- [ ] Manual P2 acceptance AT-44 and AT-46; NOT RUN on this candidate

## Health
- Liveness: `GET /actuator/health/liveness`. Use it for restart decisions; it stays UP while Redis is down.
- Overall: `GET /actuator/health`. It reports DOWN when Redis or PostgreSQL is unavailable. Redis being down degrades the service but does not make it unavailable.
- Cache metrics: `GET /actuator/metrics/equipment.cache.request_detail?tag=result:hit|miss|error`.

## Smoke (after start or recovery)
Send headers `X-User-Id: employee-001` / `X-Role: EMPLOYEE`, then `X-User-Id: approver-001` / `X-Role: APPROVER`:
1. `POST /api/v1/equipment-requests` → 201 DRAFT v0
2. `POST /{id}/submit {"expectedVersion":0}` → 200 PENDING
3. As the Approver, `POST /{id}/approve {"expectedVersion":1}` → 200 APPROVED
4. `GET /api/v1/equipment-requests?status=APPROVED` → the request is listed
5. Repeat the approve → 409 `REQUEST_STATE_CONFLICT`

## Recovery (tested)

| Incident | Action | Observed |
| --- | --- | --- |
| Redis unavailable | No action is required to keep serving. Restore with `docker compose start redis` | Detail reads were 200 from PostgreSQL in about 0.51 s; after the restore, 0.008 s and health UP |
| PostgreSQL restart | `docker compose restart postgres`; the backend reconnects through its pool | 3 × 200 immediately after, and data persisted |
| Backend crash | In the host-based setup, restart the jar or `mvnw spring-boot:run`. In the Docker UI setup, `docker compose --profile app restart backend` is the corresponding command | Host-based restart was rehearsed; Docker restart was NOT RUN as a recovery test |
| Stale detail cache suspected | Delete the `equipment:v1:request-detail:*` keys (see the backend README). Reads refill from PostgreSQL | Correctness never depends on the cache, because reads use the database version |

## Rollback
- **Application:** for the Docker UI stack, rebuild a chosen earlier revision in an isolated checkout, then recreate backend and frontend with `docker compose --profile app up -d --build --wait`. Keep the current database volumes. For the host-based setup, redeploy the previous jar or revision. The V2 migration only adds the `departments` table. An older jar (TASK-005) is expected to start against the V2 schema because Flyway ignores future migrations by default and `ddl-auto=validate` ignores extra tables. **Neither application rollback path was rehearsed.**
- **Schema:** Flyway migrations are forward-only. Fix a bad migration with a new forward migration (V3…), never by editing an applied one.
- **Local data:** `docker compose exec postgres pg_dump -U equipment_app equipment_requests > backup.sql` before risky changes, and restore with `psql`. This was not rehearsed; it is the documented approach.

## Feedback
Record each incident or defect as requirement → regression test → backlog task. Update the prompt or context when a mistake repeats.
