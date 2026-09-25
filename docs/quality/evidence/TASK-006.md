# Evidence: TASK-006 implementation

Date / operator: 2026-09-25 / Claude Code; Implement approved by project owner acting as code reviewer on 2026-09-25
Approved Design: [ADR-006](../../architecture/decisions/ADR-006-caching.md), [TASK-006 test design](../TASK-006-test-design.md)
Environment: Windows, Temurin Java 25.0.3 targeting Java 21, Docker Desktop (Testcontainers 2.0.5: `postgres:17-alpine`, `redis:8-alpine`), Compose PostgreSQL 17.11 + Redis 8, Node 24.15.0-alpine container
Requirements: REQ-01, REQ-07, REQ-10, REQ-11

| Check | Command / method | Result | Observation |
| --- | --- | --- | --- |
| Backend clean package | `backend\\mvnw.cmd --batch-mode clean package` | PASS (exit 0) | 126 tests, 0 failures/errors/**0 skipped**. There are 25 new tests, including a unit test that a PostgreSQL failure propagates and Redis is never consulted; the real PostgreSQL + Redis ones are `RequestDetailCacheIntegrationTest` (6) and `RedisOutageIntegrationTest` (1) |
| Mutation check: publish before commit | Replace `afterCommit {` with `run {` in `saveAndPublish`, run `-Dtest=RequestDetailCacheIntegrationTest` | FAIL as intended | Only the rollback test failed; the source was restored |
| Frontend quality checks | Node 24.15.0-alpine container: `npm ci && npm run lint && npm run typecheck && npm test && npm run build` | PASS (exit 0) | 31 tests (2 new department-suggestion tests); existing tests now route reference-data calls through `stubApi` |
| OpenAPI | `mikefarah/yq:4` parses `contracts/openapi.yaml` | PASS | New `/api/v1/reference-data`, `ReferenceData` schema and `Reference Data` tag present |
| Two instances: shared Redis | Packaged jars on :8080 (A) and :8081 (B) against Compose | PASS | See scenario 1 below |
| Two instances: Caffeine is local | Same two jars | PASS | See scenario 2 below |
| Redis outage on a running jar | `docker compose stop redis`, three detail reads on A | PASS with a noted cost | HTTP 200 in 0.514–0.517 s each; error counter 6; after restart, 0.009 s |
| Metrics | `/actuator/metrics/equipment.cache.request_detail` and `cache.gets` on both instances | Recorded | A: detail hit 2 / miss 0 / error 0, reference hit 1 / miss 1. B: detail hit 3 / miss 1 / error 0, reference hit 0 / miss 1 |

## Integration coverage (`RequestDetailCacheIntegrationTest`)

- **Load → hit → expiry:**
  - Creating a request does not pre-fill the cache. The first read stores the key with a TTL of at most 2 s (test TTL).
  - A hit runs exactly **1** SQL statement (the owner/version lookup) instead of the aggregate load. The hit counter increases.
  - The key expires, and the next read is a miss that refills it.
- **Mutations:** after update, submit, approve, reject and cancel, only the key for the new version exists and reads return the new status and version. Reject stores the trimmed reason.
- **Rollback:** an update inside a transaction marked rollback-only publishes no `…:1` key, and reads return the committed title at version 0.
- **Late fill:** a stale payload planted under version 0 after version 1 committed is never served.
- **Corrupt payload:** counted as an error, then replaced from PostgreSQL and refilled with valid JSON.
- **Cross-user:** with the entry cached, `employee-002` still gets `EquipmentRequestAccessDenied`. The cached payload contains no owner ID.
- **Outage (`RedisOutageIntegrationTest`):** after Redis stops, the detail read returns from PostgreSQL in under 1 s, the error counter increases, and an update still commits (version 1) and is readable.
- **Local Caffeine (`ReferenceDataCacheTest`, real `@Cacheable` proxy, fake `Ticker`):**
  - A second call hits without a repository call (stats: 1 hit / 1 miss).
  - At +59 min the entry is still cached; at +61 min it reloads.
  - The disabled switch yields a `NoOpCacheManager`.
- **Switches (`CacheConfigurationTest`):** both switches off gives the NoOp detail cache and the NoOp cache manager without any Redis bean. A partial override keeps each cache's own default TTL. This test exposed a binding NPE in the first shared-`Settings` version, which was fixed before commit.

## Scenario 1: shared Redis across instances

| Step | Result |
| --- | --- |
| Create via A; read via B | `Original title`, version 0; Redis holds version `0` |
| Update via A | version 1; Redis holds only `1` (0 evicted after commit) |
| Read via B | `Edited via A`, version 1 |
| Submit via A, approve via B | `PENDING`, then `APPROVED` |
| Read via A | `APPROVED`, version 3; Redis holds only `3` |
| Non-owner `employee-002` via B while cached | HTTP 403 |

## Scenario 2: Caffeine is local

| Step | Result |
| --- | --- |
| A reads reference data | 6 seeded departments (fills A's Caffeine) |
| Insert `Quality Assurance` directly in PostgreSQL | — |
| A reads again | 6 departments: A's cached copy, stale by design until the 1 h TTL |
| B reads for the first time | 7 departments, including `Quality Assurance`: B has its own cache |
| Redis keys matching `*reference*` | 0 |

The row was deleted afterwards.

## Implemented boundaries

- **Read path:** `EquipmentRequestService.get` → `findAccessHeaderById` (primary key) → 404/403 → `RequestDetailCache.get(id, version)` → on a miss, load the aggregate and `put` it under its own version.
- **Write path:** `saveAndPublish` registers `afterCommit { put(new); evict(previous) }` for update and all four actions. `create` does not pre-fill.
- **`RedisRequestDetailCache`:**
  - uses the Boot `JsonMapper` (Jackson 3) with keys `equipment:v1:request-detail:{id}:{version}`
  - absorbs only Redis `DataAccessException` and Jackson failures; PostgreSQL errors are never caught
  - logs only the request ID and the exception type
- **Caffeine:**
  - `CaffeineCacheManager` is the only `CacheManager` (`maximumSize=16`, `expireAfterWrite=1h`, `recordStats`, injectable `Ticker`)
  - it serves `@Cacheable ReferenceDataService`, which reads the V2 `departments` table plus the equipment options, using quantity limits from the domain constants
- **Configuration:** `app.cache.*` is bound through typed `CacheProperties`. Actuator exposes only `health` and `metrics`.
- **Refactor:** `parseActor` was extracted from the controller so both controllers share the identity contract.
- **Frontend:** `useDepartmentSuggestions` feeds a `<datalist>` in the request form and the list filter. The field stays free text, and a failed reference request leaves it usable.

## Limitations and notes

- **Outage latency:** during a Redis outage each detail miss costs up to two timeouts (read, then refill) of about 0.51 s. It stays within the 1 s policy. A short-circuit after a read error, or a circuit breaker, would halve that. It is left for TASK-007 to decide with k6 data.
- **Health status:** overall `/actuator/health` reports DOWN while Redis is down, although requests are still served. Any deployment probe should target liveness/readiness, not overall health.
- **Speed-up not claimed:** each detail read, hit or miss, still makes one primary-key query. Performance is measured in TASK-007.
- **Local data left behind:** the Compose database now includes V2 (`departments`), and the smoke requests remain.

## Approval needed

The code reviewer must approve the TASK-006 code, tests and this evidence before the Implement gate closes.
