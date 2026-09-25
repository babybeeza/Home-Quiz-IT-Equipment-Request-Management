# Evidence: TASK-008 Playwright acceptance automation

Date / operator: 2026-09-25 / Claude Code; Design and Implement approved by project owner on 2026-09-25
Revision under test: `main` at `d3ca981` (application code unchanged by this task)
Environment:
- Windows 11, i5-10400, Docker 29.5.2
- `mcr.microsoft.com/playwright:v1.63.0-noble` (Chromium 1243, Node 24.20.0)
- the frontend production build in `node:24.15.0-alpine` on :3200
- the backend jar on :8080 (Temurin 25.0.3)
- Compose PostgreSQL 17 / Redis 8 with the 1,200-row seed dataset

| Check | Command | Result | Observation |
| --- | --- | --- | --- |
| First full run | `bash tests/e2e/run-e2e.sh` | 44 passed / 5 failed | All 5 were test-code defects (below); no app defect was found |
| Full run after the test fixes | Same | **PASS**: 49/49 in 42.9 s, exit 0 | 53 AT IDs covered |
| Stability rerun | Same | **PASS**: 49/49 in 45.3 s, exit 0 | No flaky or skipped tests |

## Failures in the first run, and their causes

| Test | Symptom | Classification | Fix |
| --- | --- | --- | --- |
| AT-03, AT-45 | Strict mode: `getByRole('alert')` matched 2 elements | Test defect. The app showed the correct message; Next.js adds an empty `role=alert` route announcer | Scope to `getByRole('main')` |
| AT-17 | 30 s timeout | Test defect. The click waited on a `window.confirm` that the test only handled after the click | Register the dialog handler before clicking |
| AT-39 | 20 unique rows instead of 30 | Test defect. The rows were read while the previous page was still shown as the "กำลังโหลด…" placeholder (intended behavior, ADR-005) | `listUpdate` waits for the matching response and the placeholder to clear |
| AT-41 | First row differed between tabs | Test defect. Same placeholder timing when capturing the first row | Same helper |

## Coverage against the acceptance document

| Section | AT IDs automated | Not automated |
| --- | --- | --- |
| A Identity | 01–04 | — |
| B Create and validation | 05–14 | — |
| C Edit and dirty state | 15–20 | — |
| D Workflow | 21–30 | — |
| E List | 31–33 (AT-33 records G-1's current behavior) | — |
| F Search and pagination | 34–43 | — |
| G States and errors | 45 | **AT-44** (backend down) and **AT-46** (Redis down) need service outages mid-run; they stay manual. Redis-outage behavior is evidenced in TASK-006/007 |
| H Backend rules via API | 47–55 | — |

## Notes

- **Outputs:** the HTML report, the JSON results and the backend log are generated under `tests/e2e/` and git-ignored. The run summary above is the committed record.
- **Scope:** this suite exercises Chromium only. Firefox and WebKit are available in the same image but were not run.
- **Not a QA sign-off:** passing automation is evidence for the QA / acceptance owner. It does not approve the Verify gate, and G-1 still needs a decision.
