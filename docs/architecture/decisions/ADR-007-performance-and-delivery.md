# ADR-007: k6 performance workload, thresholds and delivery verification

Status: Accepted
Date: 2026-09-25
Owner: Technical owner
Requirements/tasks: REQ-01–REQ-13 (final audit), especially REQ-11, REQ-12, REQ-13 / TASK-007

## Context

REQ-12 requires k6 runs whose results are reported together with the workload and environment. The assignment fixes no workload, VUs, duration or thresholds. Discover Q-12 and assumption A-06 require them to be **finalized before running**, and a failed threshold must be reported, not tuned afterwards.

The implementation plan proposed:
- about 1,000 seeded requests
- 80% reads (list and detail) and 20% mutations, using data isolated per iteration
- runs of smoke 1 VU, then baseline 10 VUs for 2 minutes, then stress 25 VUs for 2 minutes
- disabled, cold and warm cache measured separately
- p95 < 500 ms, unexpected errors < 1%, correctness 100%

TASK-005 and TASK-006 provide:
- the 1,200-request seed script
- the `APP_CACHE_*_ENABLED` switches
- the cold, warm and disabled procedures

Environment known now (recorded again in each report):
- Windows 11 host with an Intel i5-10400 (6 cores / 12 threads) and 31.9 GB RAM
- Docker 29.5.2 (VM with 12 CPUs / 15.6 GB)
- k6 v2.3.0 as the `grafana/k6` image
- PostgreSQL 17 and Redis 8 in Compose, and the packaged backend jar on the host (Temurin 25)

Everything runs on one machine.

## Options

### Where k6 runs
1. **The `grafana/k6:2.3.0` container calling the host backend through `host.docker.internal`.** Nothing to install, and the version is pinned. The load generator shares the CPU with the system under test, which is recorded as a limitation.
2. **A native k6 install:** it has the same single-host contention and adds an unpinned tool to the prerequisites.

### Workload shape
1. **One scenario with a weighted random action per iteration.** The request mix matches the stated 80/20 split, and each mutation iteration uses a draft it creates itself, so there are no accidental conflicts.
2. **Separate read and write scenarios, split by VU count:** simpler, but the mix follows VU share rather than request share, so the 80/20 split is not guaranteed.

## Decision

### Workload (Option 1, one scenario), `tests/performance/workload.js`

| Weight | Action | Actor | Checks (correctness) |
| --- | --- | --- | --- |
| 25% | `GET /equipment-requests` (default page) | random `seed-employee-01…10` | 200; `page`=0; every row's `employeeName` belongs to that seed owner; `totalElements` = 120 |
| 15% | `GET /equipment-requests?status=PENDING&keyword=monitor&department=…` rotating | Approver | 200; every row matches the status; metadata is consistent (`totalPages` = ceil(total/size)) |
| 40% | `GET /equipment-requests/{id}` for one of 200 fixed seeded IDs | Approver | 200; `id` and `version` match the seeded row |
| 20% | Workflow on its own data: create → edit → submit → approve **or** reject (alternating) → detail read | random `seed-writer-01…10`, then Approver | each step 200/201; version increases 0 → 1 → 2 → 3; final status and reason correct |

- **Expected conflicts:** 1 in 10 workflow iterations deliberately repeats the approve with the old version. The resulting 409 `REQUEST_VERSION_CONFLICT` is counted in its own `expected_conflicts` metric and is **excluded** from the unexpected-error rate. Any other non-2xx response counts in `unexpected_errors`.
- **Dates:** `requiredDate` is derived from today in Asia/Bangkok plus 30 days.

### Data, setup and cleanup
- Before each measured run, [`seed-search-dataset.sql`](../../../tests/performance/seed-search-dataset.sql) restores exactly 1,200 seeded rows. It deletes only `seed-%` owners, so it also removes the drafts earlier runs created.
- The workflow uses separate owners `seed-writer-01…10`. Read-scope checks on `seed-employee-*` therefore stay exact (120 each), and reseeding still removes the drafts because they match `seed-%`.
- The 200 detail IDs are the deterministic `md5('seed-request-N')` UUIDs for N = 1…200.

### Runs (fixed order, fresh seed before each measured run)

| # | Run | VUs / duration | Cache state | Purpose |
| --- | --- | --- | --- | --- |
| 0 | Smoke | 1 VU / 30 s | enabled | The script and checks are valid; not a performance claim |
| 1 | Baseline, disabled | 10 / 2 m | `APP_CACHE_REQUEST_DETAIL_ENABLED=false`, `APP_CACHE_REFERENCE_DATA_ENABLED=false` | Database-only baseline |
| 2 | Baseline, cold | 10 / 2 m | enabled; `equipment:v1:*` flushed and the backend restarted immediately before | Measures from empty caches |
| 3 | Baseline, warm | 10 / 2 m | enabled; a 1-VU pre-pass reads all 200 detail IDs, and that pass is not measured | Steady-state cache |
| 4 | Stress, disabled | 25 / 2 m | disabled | Database-only under higher load |
| 5 | Stress, warm | 25 / 2 m | enabled, warmed | Cache under higher load |

- Every iteration ends with 0.1–0.5 s of random think time so runs are comparable. `discardResponseBodies` stays off because the checks need the bodies.

### Thresholds (fixed now, applied to every run 1–5, never changed after results)

| Metric | Threshold | Rationale |
| --- | --- | --- |
| `http_req_duration` p95 | < 500 ms | A plan default. Interactive list/detail should feel instant; TASK-005 measured queries at ≤ 1.3 ms, leaving room for HTTP, JSON and contention |
| `http_req_duration` p99 | < 1,000 ms | Bounds tail latency to the ADR-006 outage policy scale |
| `unexpected_errors` rate | < 1% | Excludes only the deliberate `expected_conflicts` |
| `checks` rate | = 100% (`rate==1.0`) | Correctness: scope, filters, versions, statuses |

- p50, p90, throughput (`http_reqs`/s) and per-action latency (k6 tags) are reported but have no pass/fail threshold.
- Cache observations come from the actuator metrics (hit/miss/error) read after each run.
- No speed-up is claimed unless the numbers show one. If the warm run is not faster than the disabled one, the report says so.

### Delivery verification (the rest of TASK-007)
- **Final audit:** every REQ-01…13 is traced to implementation, tests and evidence in `traceability.md`, with gaps stated. The FE ≥ 4 and BE ≥ 6 minimums are confirmed by named behavioral tests.
- **Final checks on the release revision:** backend `clean package` (with the Testcontainers count), frontend lint, typecheck, test and build, and an OpenAPI parse.
- **Clean-start rehearsal:**
  - `git clone` the pushed branch into a new temporary directory and follow only the README to start DB/Redis, backend and frontend.
  - Run an HTTP smoke of the full journey (create → edit → submit → approve/reject → list), plus one conflict and one forbidden path.
  - Record any README step that failed and fix it.
- **Runbook:** health (`/actuator/health`), the smoke steps and recovery steps (a Redis outage is served from the DB; restart; Flyway forward-fix policy) are tested locally and recorded. No cloud deployment is claimed.
- **Hygiene:** a secret scan with `git grep` for password, token and key patterns, beyond the documented local-only defaults.

## Consequences and verification

- The single-host setup inflates latency and hides network cost. Results describe this machine only and are labelled as such.
- A threshold failure is a finding to report, not a reason to change the thresholds.
- Warm and cold comparisons include the per-read primary-key query ADR-006 chose, so they measure the real design, not an idealized cache.

Verification follows the [TASK-007 test design](../../quality/TASK-007-test-design.md).
