# Evidence: TASK-007 Design

Date / operator: 2026-09-25 / Claude Code
Status: Approved by project owner acting as technical owner on 2026-09-25
Requirements: REQ-01–REQ-13

## Reviewed inputs

- REQ-11/12/13, Discover Q-12 and assumption A-06 (thresholds before running), and the implementation-plan k6 proposal
- TASK-005 seed and query baseline, and TASK-006 cache switches and procedures
- Host environment: Intel i5-10400 (6C/12T), 31.9 GB RAM, Windows 11; Docker 29.5.2 (12 CPUs, 15.6 GB); `grafana/k6` v2.3.0

## Design outputs

- [ADR-007](../../architecture/decisions/ADR-007-performance-and-delivery.md) fixes:
  - the k6 location
  - the weighted workload and its checks
  - expected-conflict accounting
  - the data isolation (`seed-writer-*`)
  - runs 0–5 with their cache states
  - the thresholds
  - the delivery verification steps
- [TASK-007 test design](../TASK-007-test-design.md) lists the checks and the evidence locations.

## Decisions approved (resolve A-06)

1. Thresholds for every measured run: p95 < 500 ms, p99 < 1,000 ms, unexpected errors < 1%, checks = 100%. They will not change after results.
2. Workload: 40% list (25% Employee scope, 15% Approver filters), 40% detail on 200 fixed seeded IDs, 20% full workflow on its own data. One in ten workflows deliberately triggers a 409 that is counted separately.
3. Runs: smoke 1 VU/30 s; baseline 10 VUs/2 min in disabled, cold and warm cache states; stress 25 VUs/2 min in disabled and warm states. Reseed before each.
4. k6, the backend, PostgreSQL and Redis share one machine. The results describe this machine only.
5. The clean-start rehearsal uses a fresh clone of the pushed branch and follows only the README. No cloud deployment is claimed.

No scripts have been run and no results exist yet.
