# Evidence: TASK-006 Design

Date / operator: 2026-09-25 / Claude Code
Status: Approved by project owner acting as technical owner on 2026-09-25
Requirements: REQ-01, REQ-07, REQ-10, REQ-11

## Reviewed inputs

- Assignment caching section (Redis + local Caffeine, with an explanation of data placement), REQ-10 and Discover Q-11 (Redis detail, Caffeine reference metadata, DB authoritative)
- Implementation-plan cache plan, data-model transaction rules (publish after commit), TASK-006 packet
- Final read/write paths from TASK-003–005 (`EquipmentRequestService.get` and the five mutations), and the TASK-005 query-plan baseline (0.04–1.3 ms uncached)

## Design outputs

- [ADR-006](../../architecture/decisions/ADR-006-caching.md) decides:
  - the data placement table
  - version-keyed Redis detail entries behind an authoritative primary-key `owner_id, version` check
  - after-commit publication, and the timeout and fallback policy
  - a Caffeine-cached `GET /reference-data` backed by a seeded `departments` table
  - metrics and the cache switches
- [TASK-006 test design](../TASK-006-test-design.md) lists the load/hit/expiry, mutation, rollback, stale-fill, cross-user, outage, two-instance and local-cache cases.

## Decisions for reviewer attention

1. Contract and schema additions:
   - new `GET /api/v1/reference-data` in OpenAPI (identity headers, all roles)
   - V2 migration that creates and seeds `departments`
   - the department field stays free text; the list only supplies `<datalist>` suggestions
2. Every detail read does one primary-key query on `owner_id, version` before touching Redis. That extra query buys version-exact correctness and database-decided authorization. No speed-up is claimed until TASK-007 measures it.
3. List results are not cached (too many key combinations, invalidated by every mutation, and already under 1.3 ms).
4. Add `spring-boot-starter-actuator` exposing only `health` and `metrics`, for hit/miss/error evidence.
5. Redis timeouts are 250 ms. On a Redis error the read is served from the database; PostgreSQL errors are never masked.
6. Caffeine staleness of up to 1 h after a department migration is accepted.

No code, contract or migration changes are part of this Design step.
