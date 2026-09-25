# ADR-006: Redis and Caffeine caching

Status: Accepted
Date: 2026-09-25
Owner: Technical owner
Requirements/tasks: REQ-01, REQ-07, REQ-10, REQ-11 / TASK-006

## Context

The assignment requires Redis caching and local Caffeine caching, and an explanation of which data belongs in each. Discover Q-11 approved the direction:
- a Redis request-detail cache
- Caffeine for reference metadata
- the database stays authoritative for authorization, status and version

The data model says cache changes publish only after commit. TASK-005 measured the uncached list at 0.04–1.3 ms per query on 1,207 rows, so caching is not needed to fix a slow query. It exists to meet REQ-10 correctly, and any speed-up must be measured in TASK-007 rather than assumed.

Two facts shape the design:
1. **The detail read already exists.** `GET /{id}` loads the aggregate (request + items) and checks access. Every mutation changes `version`.
2. **Nothing reads reference data today.** Department is free text and equipment types are a frontend constant. A Caffeine cache needs a real read path the app calls. TASK-006 plan item 4 asks for "a metadata endpoint the form actually calls".

## Options

### What goes where

| Data | Redis (shared) | Caffeine (per instance) |
| --- | --- | --- |
| Request detail (mutable, per request, written by any instance) | ✅ All instances must agree after a mutation, so a shared store is needed | ❌ Each instance would keep its own stale copy after another instance's write, and cross-instance invalidation would be needed |
| Reference data (departments, equipment options; the same for every user, changed only by migration) | Possible but pointless: a network hop to save a tiny, rarely changing read | ✅ A tiny, read-mostly value that tolerates bounded staleness, with no network hop |
| Paged list results | ❌ Too many key combinations (actor × 6 parameters), invalidated by every mutation, and the list is already fast | ❌ Same reason |

### Redis consistency approach

1. **Version-keyed entries, checked against the database:** each detail read first fetches `owner_id, version` by primary key from PostgreSQL, authorizes, then reads `detail:{id}:{version}`. An entry can never be served for the wrong version, whatever races happen, and access is always decided from the database.
2. **Unversioned `detail:{id}` with evict-on-write:** simpler keys, but a reader that loaded old data before a commit can write it back after the eviction (the late-fill race). Any rollback-then-reuse or missed eviction serves stale data until the TTL expires.
3. **Spring `@Cacheable` on `get`:** it caches the whole method result, including the authorization outcome, keyed without version, and publishes inside the transaction.

### Caffeine read path

1. **A new `GET /api/v1/reference-data` backed by a seeded `departments` table (V2 migration) plus the equipment-type options.** The form and the list filter use it for department suggestions via `<datalist>`, so free text stays valid and existing data and validation are unchanged.
2. **Cache the `EquipmentType` enum:** there is no data source to save work on, so it would be caching for show.

## Decision

### Redis: request detail (Option 1)

- **Key:** `equipment:v1:request-detail:{id}:{version}`. `v1` is the payload-format version, so a format change can never read old-shaped payloads.
- **Value:** JSON of the application-layer `EquipmentRequestView`, written with the application's Jackson `JsonMapper` through `StringRedisTemplate`. The value holds no per-viewer data, and authorization is never cached.
- **TTL:** 10 minutes (`app.cache.request-detail.ttl`). TTL is the fallback for old versions, not the correctness mechanism.
- **Read path (`EquipmentRequestService.get`):**
  1. `findAccessHeaderById` (a projection of `id, owner_id, version`) → 404/403 exactly as today.
  2. Redis `GET` for the key with that version → return on a hit.
  3. On a miss, load the aggregate. If its version equals the header version, `SET` it with the TTL. If it doesn't (a concurrent commit happened in between), return the freshly loaded aggregate and cache it under its own version, which is still correct because the key contains the version.
- **Write path:** after `update`, `submit`, `cancel`, `approve` or `reject`, a `TransactionSynchronization.afterCommit` callback stores the returned view under the new version and deletes the previous version's key. A rollback never runs `afterCommit`, so nothing is published. This matters because a version number that rolled back can be reused by a later commit.
- **Late fill:** a reader that loaded version N may write `…:{N}` after version N+1 commits. That entry is never read again, because readers look up the version the database reports, and the TTL removes it.
- **Failure policy:** Redis uses `timeout=250ms` and `connect-timeout=250ms`. The Redis adapter catches Spring `DataAccessException`/`RedisConnectionFailureException` on GET, SET and DEL, counts them as `error` and continues on the database. PostgreSQL exceptions are never caught by the cache layer. A corrupt or undeserializable payload counts as `error`, is deleted, and is read from the database instead.
- **Cross-instance behavior:** all instances share Redis and read the authoritative version from PostgreSQL, so instance B never serves instance A's superseded entry. Nothing needs a pub/sub invalidation channel.
- **Switch:** `app.cache.request-detail.enabled` (default `true`). When false, the service skips Redis completely, for k6 cold/disabled runs.

### Caffeine: reference data (Option 1)

- **V2 migration:** a `departments` table (`code` PK, `name` unique, `sort_order`, `active`) seeded with the 6 departments the TASK-005 seed dataset already uses.
- **`ReferenceDataService.get()`:** annotated `@Cacheable("reference-data")`. It returns active departments ordered by `sort_order`, plus equipment types with Thai labels and the 1–5 quantity limits.
- **Endpoint:** `GET /api/v1/reference-data` requires the usual identity headers and is available to every role. It is added to the OpenAPI contract as `ReferenceData`.
- **Cache manager:** `CaffeineCacheManager` is the only Spring `CacheManager` (`spring.cache.type` is explicit), so Redis is never used for annotation caching.
  - Settings: `maximumSize=16`, `expireAfterWrite=1h`, `recordStats`.
  - The builder takes an injectable `Ticker` bean, so expiry is testable deterministically.
  - Staleness of up to 1 hour after a migration changes departments is acceptable and documented.
- **Switch:** `app.cache.reference-data.enabled` (default `true`) selects a no-op cache manager when false.

### Observability

- Add `spring-boot-starter-actuator`, exposing only `health` and `metrics`.
- Redis detail cache counter: `equipment.cache.request_detail{result=hit|miss|error}`.
- Caffeine statistics: bound through Micrometer as `cache.gets{cache=reference-data,result=hit|miss}`.
- Log lines carry the request ID and the error class only; no names, emails or payloads.

### Frontend

- `useReferenceData()` wraps a TanStack query (`staleTime: Infinity`, keyed by actor).
- It feeds a `<datalist>` for the department input in the request form and in the list filter. A failure just leaves the input without suggestions.

## Consequences and verification

- **Extra primary-key query:** every detail read costs one primary-key lookup even on a hit. The saving is the aggregate plus the item join. Whether that is faster at k6 load is for TASK-007 to measure. This ADR claims only correctness.
- **Reused version numbers:** if a transaction rolled back after a version was published, a later commit could reuse that number with different content. After-commit publication rules this out.
- **Configuration docs:** cache settings and warm/cold procedures go into the backend README and are handed to TASK-007.

Verification follows the [TASK-006 test design](../../quality/TASK-006-test-design.md).
