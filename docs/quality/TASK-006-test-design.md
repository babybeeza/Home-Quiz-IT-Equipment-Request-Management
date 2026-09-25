# TASK-006 verification design

Status: Approved in TASK-006 Design on 2026-09-25; no results are claimed here
Requirements: REQ-01, REQ-07, REQ-10, REQ-11

| Boundary | Required behavior |
| --- | --- |
| Redis load → hit → expiry | The first detail read misses and stores `…:{id}:{version}` with a TTL of at most the configured value. The second read is a hit that skips the aggregate query (verified by Hibernate statement count). With a short test TTL, the key disappears and the next read misses again |
| Key and payload | The key contains the ID, the version and the `v1` prefix. The JSON round-trip preserves every view field, including items and their order. A corrupt payload counts as an error, is deleted and is served from the database |
| Mutations | After each of update, submit, cancel, approve and reject, the next detail read returns the new status and version. The previous version's key is deleted and the new version's key is present |
| Rollback | A mutation whose transaction rolls back publishes no key for the would-be version, and the next read returns the unchanged committed data |
| Stale fill | A payload planted under an old version key, or a planted different payload under an old key after a commit, is never returned; reads follow the database version |
| Cross-user | With a cached entry present, a non-owner Employee still gets 403 and the cached body is never returned. No cache entry is keyed by or contains actor identity |
| Redis outage | With Redis stopped, a detail read returns 200 from the database within 1 s and the `error` counter increases. A PostgreSQL failure still propagates (not masked by the cache layer) |
| Switch | With `app.cache.request-detail.enabled=false`, no Redis command is issued and reads are correct |
| Two instances | Two backend processes share Compose Redis and PostgreSQL. After a mutation through instance A, a detail read through instance B returns the new version |
| Caffeine load → hit → expiry | With a fake `Ticker`: the first `reference-data` call queries the database, the second does not, and after advancing past 1 h the next call queries again. Statistics show the hits and misses |
| Caffeine is local | Two instances: after a direct database change to `departments`, the instance that has cached reference data returns the old value until expiry, while a freshly started instance returns the new value |
| Reference endpoint / UI | `GET /reference-data` returns active departments in order and equipment options, and missing identity → 400. The form and list filter offer department suggestions; a failed reference request leaves the input usable |
| Metrics | `/actuator/metrics/equipment.cache.request_detail` and `cache.gets` show real counts after the scenarios; no PII appears in cache logs |

## Evidence plan

- Backend unit and MVC tests plus Testcontainers integration tests (PostgreSQL 17 + Redis 8) run through the Maven wrapper. Skipped container tests count as NOT RUN.
- The two-instance checks run as packaged jars on ports 8080/8081 against Compose, with commands and responses recorded.
- The frontend runs lint, typecheck, tests and build in the Node 24.15.0 container.
- Warm and cold procedures (flush `equipment:v1:*`, restart for Caffeine) and the cache switches are documented for TASK-007.
