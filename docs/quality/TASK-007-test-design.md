# TASK-007 verification design

Status: Approved in TASK-007 Design on 2026-09-25; no results are claimed here
Requirements: REQ-01–REQ-13, especially REQ-11, REQ-12, REQ-13

| Boundary | Required behavior |
| --- | --- |
| k6 script validity | The 1-VU smoke run passes every check. Each action and step is tagged so latency can be reported per action |
| k6 runs 1–5 | Run exactly as listed in [ADR-007](../architecture/decisions/ADR-007-performance-and-delivery.md), with a fresh seed before each and the cache state set as specified. Report p50/p90/p95/p99, throughput, `checks`, `unexpected_errors` and `expected_conflicts`, and PASS/FAIL against the fixed thresholds |
| Cache observation | After runs 2, 3 and 5, read the actuator `equipment.cache.request_detail` hit/miss/error counters and Caffeine `cache.gets`. After run 1 (disabled), no Redis keys matching `equipment:v1:*` exist |
| Isolation | Workflow iterations use `seed-writer-*` owners; `seed-employee-*` list totals stay 120 during every run |
| Final checks | On the release revision: backend `clean package` (0 skipped container tests); frontend lint, typecheck, test and build; OpenAPI parse |
| Requirements audit | Each of REQ-01…13 has implementation, test and evidence links, or an explicit gap. The FE ≥ 4 and BE ≥ 6 minimums are backed by named behavioral tests |
| Clean-start rehearsal | A fresh `git clone` into a temporary directory, following only the README, starts DB/Redis, backend and frontend. The HTTP journey smoke passes: create, edit, submit, approve, reject, list, one 409 and one 403. Every README deviation is recorded and fixed |
| Runbook | Health, smoke and recovery steps (Redis outage, restart, migration forward-fix policy) are executed locally and recorded |
| Hygiene | The secret scan finds only documented local-development defaults; no personal data appears in fixtures or logs |

## Evidence plan

- k6 JSON summaries go to `tests/performance/results/`, and one report per run set follows the results template.
- The final audit, rehearsal and runbook results go to `docs/quality/evidence/TASK-007.md`, with commands, exit codes and environment.
- Any failed threshold or unexecuted check is reported as FAIL or NOT RUN. The task is not marked Done until gaps are resolved or accepted.
