# TASK-008: Playwright acceptance automation

Status: Implement approved — results handed to QA / acceptance owner
Owner: Developer
Requirement IDs: REQ-01–REQ-08, REQ-10, REQ-11 (verification only; no product change)
Dependencies: TASK-007 (merged); [acceptance test cases](../../quality/acceptance-test-cases.md)

## Human approvals

| Gate | Approver | Decision | Date / revision |
| --- | --- | --- | --- |
| Requirements | Project owner | Approved | 2026-09-25 / user request "test ด้วย playwright" |
| Design | Project owner acting as technical owner | Approved | 2026-09-25 / decisions 1–6 below (reviewed together with the implementation) |
| Implement | Project owner acting as code reviewer | Approved | 2026-09-25 / [TASK-008 evidence](../../quality/evidence/TASK-008.md) |

## Scope / non-goals
Automate the QA acceptance cases in a real browser. Non-goals: product changes, changing the QA sign-off process (automation results are evidence; the QA / acceptance owner still decides the Verify gate), cross-browser coverage and CI.

## Design decisions
1. **Location and tool:** `tests/e2e`, a separate npm package with `@playwright/test` 1.63.0 pinned by lockfile. This is the test strategy's planned E2E layer, and it keeps browser tooling out of the frontend app's dependencies.
2. **Execution:**
   - Docker image `mcr.microsoft.com/playwright:v1.63.0-noble`. The host has no Node.
   - The browser reaches the app through `host.docker.internal`.
   - The frontend is a production build with `NEXT_PUBLIC_API_BASE_URL` pointing there, and the backend's `FRONTEND_ORIGIN` is set to match.
   - Using the real bundle also avoids dev-server cross-origin restrictions.
3. **Data:** each test arranges its own requests through the public API with unique titles. Section F uses the TASK-005 seed dataset and skips with a reason when it is missing.
4. **Browsers and parallelism:** Chromium only, with 1 worker (shared database, exact list totals), `th-TH` locale and Asia/Bangkok timezone.
5. **Coverage:** 53 of 55 AT cases are automated. AT-44 and AT-46 need service outages mid-run and stay manual; the TASK-006/007 evidence already covers the Redis-outage behavior.
6. **Traceability:** test titles carry `AT-xx` IDs. AT-33 records the current behavior for open item G-1 and is annotated as such; it does not decide G-1.

## Acceptance criteria
- [x] Every automatable AT case has a test titled with its ID
- [x] One command starts the stack, runs the suite and cleans up
- [x] Two consecutive full runs pass with no flaky results
- [x] Failures found during development are classified as test or app defects, with the cause recorded

## Handoff
Automation results are input for the QA / acceptance owner. The Verify gate still needs a human decision, including G-1 (AT-33) and the manual cases AT-44 and AT-46.
