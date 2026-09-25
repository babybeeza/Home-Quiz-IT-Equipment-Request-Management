# Performance report — TASK-007

Status: **PASS**. All five measured runs met every threshold fixed in advance.
Date / revision / operator: 2026-09-25 / backend jar built from `fc8378c` (identical application code to `main` at `b90c778`); k6 script on branch `task-007-verification-delivery` / Claude Code
Environment:
- Windows 11 host with an Intel Core i5-10400 @ 2.90 GHz (6 cores / 12 threads) and 31.9 GB RAM
- Docker 29.5.2 (VM with 12 CPUs / 15.6 GB), running PostgreSQL 17.11 and Redis 8 in Compose
- the backend jar on the host (Temurin 25.0.3, Spring Boot 4.1.1)
- k6 v2.3.0 (`grafana/k6:2.3.0`) calling `host.docker.internal:8080`

**Everything shares one machine.**

Dataset / setup / cleanup:
- [`seed-search-dataset.sql`](../seed-search-dataset.sql) runs before every run. It restores exactly 1,200 seeded requests (10 owners × 120, 6 departments, 5 statuses, 1,800 items) and deletes all rows owned by `seed-%`, including drafts created by earlier runs.
- The request-detail Redis keys are flushed, and the backend is restarted before every run.

Workload / VUs / duration / cache state:
- The mix and runs follow [ADR-007](../../../docs/architecture/decisions/ADR-007-performance-and-delivery.md): 25% Employee list, 15% Approver filtered list, 40% detail over 200 seeded IDs, and 20% workflow (create → edit → submit → approve/reject → re-read). Each iteration then waits 0.1–0.5 s of think time.
- Every tenth workflow sends a deliberately stale approve (an expected 409).

Exact command: `tests/performance/run-k6.sh <run> <vus> <duration> <disabled|cold|warm>`. The script wraps `docker run grafana/k6:2.3.0 run -e VUS -e DURATION --summary-export … /perf/workload.js`.

Thresholds and rationale: fixed and approved before any run, and never changed ([ADR-007](../../../docs/architecture/decisions/ADR-007-performance-and-delivery.md), assumption A-06).
- `http_req_duration` p95 < 500 ms and p99 < 1,000 ms
- `unexpected_errors` < 1%
- `checks` = 100%

## Results

| Run | VUs / time | Cache | p50 | p90 | p95 | p99 | max | Requests (req/s) | Unexpected errors | Checks | Expected 409 | Thresholds |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 00 smoke | 1 / 30 s | warm | — | — | 16.53 ms | 80.13 ms | — | 189 | 0% | 309 / 309 | 4 | PASS (validity only) |
| 01 baseline | 10 / 2 m | disabled | 6.06 ms | 9.55 ms | **11.12 ms** | 16.48 ms | 302 ms | 6,883 (57.0/s) | 0% | 11,965 / 11,965 | 79 | **PASS** |
| 02 baseline | 10 / 2 m | cold | 7.15 ms | 10.65 ms | **12.19 ms** | 18.36 ms | 705 ms | 6,998 (57.8/s) | 0% | 12,168 / 12,168 | 84 | **PASS** |
| 03 baseline | 10 / 2 m | warm | 6.93 ms | 10.30 ms | **11.41 ms** | 14.87 ms | 201 ms | 6,817 (56.6/s) | 0% | 11,972 / 11,972 | 75 | **PASS** |
| 04 stress | 25 / 2 m | disabled | 5.53 ms | 9.25 ms | **10.73 ms** | 16.88 ms | 305 ms | 17,467 (144.7/s) | 0% | 30,404 / 30,404 | 201 | **PASS** |
| 05 stress | 25 / 2 m | warm | 6.29 ms | 9.67 ms | **11.18 ms** | 16.31 ms | 354 ms | 17,498 (145.3/s) | 0% | 30,378 / 30,378 | 206 | **PASS** |

The correctness checks cover:
- Employee lists contain exactly their own 120 rows
- Approver filters hold for status, department and keyword, and page metadata is consistent
- detail reads return the correct ID and seeded version
- workflow versions go 0 → 1 → 2 → 3 with the correct final status and reason
- stale approves return 409 `REQUEST_VERSION_CONFLICT`

Every check passed in every run.

## Cache observations (actuator counters after each run)

| Run | Redis detail hit / miss / error | Hit rate | Redis detail keys after run | Caffeine reference-data |
| --- | --- | --- | --- | --- |
| 01, 04 disabled | n/a (NoOp cache) | — | **0** (confirms the switch) | n/a |
| 02 cold | 2,086 / 201 / 0 | 91.2% | 971 | 0 / 0 (not in workload) |
| 03 warm | 2,283 / 200 / 0 | 91.9% | 932 | 0 / 0 |
| 05 warm | 5,824 / 200 / 0 | 96.7% | 2,123 | 0 / 0 |

- The 200 misses in the warm runs come from the unmeasured warm-up pass: the counters span the process lifetime. Measured detail reads in runs 03 and 05 were therefore effectively all hits.
- Keys left after a run include the entries published after commit for workflow-created requests.

## Findings

1. **All thresholds passed with a large margin:** p95 of 10.7–12.2 ms against 500 ms, and p99 of 14.9–18.4 ms against 1,000 ms.
2. **Caching gave no measurable latency benefit at this scale.**
   - At 10 VUs, warm p95 was 11.41 ms vs 11.12 ms disabled. At 25 VUs it was 11.18 ms vs 10.73 ms.
   - This matches the ADR-006 design: every detail read still makes one primary-key lookup (owner and version), and the Redis round trip on a hit costs about the same as the saved aggregate load, which TASK-005 measured at ≤ 1.3 ms.
   - The cache is justified by correctness under multiple instances and by offloading PostgreSQL, not by latency on this dataset. **No speed-up is claimed.**
3. **The load did not reach capacity.** Throughput scaled with VUs (57 → 145 req/s) while latency stayed flat, so the server was not saturated. That is expected with 0.1–0.5 s think time. These runs demonstrate behavior at 10 and 25 concurrent users, not a maximum capacity.
4. **Latency spikes have two confounders.**
   - The cold run's max of 705 ms is one outlier; p99 stayed at 18.4 ms.
   - The backend is restarted before every run, so the "cold" and "disabled" runs also start with a cold JIT, while warm runs benefit from their warm-up pass.
5. **The deliberate stale approves** (645 in total) always returned 409 and never counted as errors.

## Limitations and follow-up

- The single-host setup means k6, the JVM, PostgreSQL and Redis compete for CPU. Absolute numbers describe this machine only, and network latency is not represented.
- The dataset is small (1,200 requests), so it doesn't stress the trigram and B-tree plans. A larger seed (for example 100k rows) and an open-model arrival rate would be needed for capacity planning.
- Caffeine is not exercised by this workload. Its load, hit and expiry behavior is evidenced in [TASK-006](../../../docs/quality/evidence/TASK-006.md).
- k6 summary export has no per-endpoint percentiles because no tag-scoped thresholds were defined. The raw per-run output is kept alongside this report.

Evidence artifacts: [`TASK-007/`](TASK-007/)
- per run: `<run>.json` (summary export), `<run>.txt` (k6 output), `<run>-cache.txt` (actuator counters and Redis key count) and `<run>-backend.log`
- per warm run: `<run>-warmup.txt`
