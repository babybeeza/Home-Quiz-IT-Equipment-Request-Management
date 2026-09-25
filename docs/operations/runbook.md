# Delivery and operations runbook

Status: local environment only. The steps below were executed in the TASK-007 clean-start rehearsal on 2026-09-25 ([evidence](../quality/evidence/TASK-007.md)). No cloud deployment exists or is claimed.

## Before delivery
- [x] Revision, environment and runtime versions: see the README Environment section and the TASK-007 evidence
- [x] README install/run/test works from a clean checkout (rehearsed; environment port conflicts noted)
- [x] Required checks have evidence; API, schema and limitations match the code
- [x] Configuration variables documented without secret values (`.env.example` holds local-only defaults)
- [x] Migrations applied to an empty database (Flyway V1 + V2); backup/restore approach below
- [x] Smoke: create draft → submit → decision → list (rehearsal HTTP journey)
- [ ] Release owner and target: to be named at the Delivery gate

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
| Backend crash | Restart the jar or `mvnw spring-boot:run`. Flyway validates the schema on start | The backend started cleanly on an empty and on an existing database |
| Stale detail cache suspected | Delete the `equipment:v1:request-detail:*` keys (see the backend README). Reads refill from PostgreSQL | Correctness never depends on the cache, because reads use the database version |

## Rollback
- **Application:** redeploy the previous jar or revision. The V2 migration only adds the `departments` table. An older jar (TASK-005) is expected to still start against the V2 schema, for two reasons: Flyway's default `ignoreMigrationPatterns=*:future` skips the unknown applied V2, and `ddl-auto=validate` ignores extra tables. **This was not rehearsed.**
- **Schema:** Flyway migrations are forward-only. Fix a bad migration with a new forward migration (V3…), never by editing an applied one.
- **Local data:** `docker compose exec postgres pg_dump -U equipment_app equipment_requests > backup.sql` before risky changes, and restore with `psql`. This was not rehearsed; it is the documented approach.

## Feedback
Record each incident or defect as requirement → regression test → backlog task. Update the prompt or context when a mistake repeats.
