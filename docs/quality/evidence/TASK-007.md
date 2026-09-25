# Evidence: TASK-007 final verification, performance and delivery

Date / operator: 2026-09-25 / Claude Code; Implement approved by project owner acting as code reviewer on 2026-09-25 — G-1 and H-1 remain open (no decision recorded)
Approved Design: [ADR-007](../../architecture/decisions/ADR-007-performance-and-delivery.md), [TASK-007 test design](../TASK-007-test-design.md)
Release candidate: `main` at `b90c778` (TASK-003–006 merged) plus this branch's changes, which are test, script and doc changes only. The application code is unchanged.
Environment:
- Windows 11, Intel i5-10400 (6C/12T), 31.9 GB RAM
- Docker 29.5.2
- Temurin 25.0.3 (Java 21 target)
- Node 24.15.0-alpine container
- PostgreSQL 17 / Redis 8 (Compose and Testcontainers)
- k6 v2.3.0

| Check | Command / method | Result | Observation |
| --- | --- | --- | --- |
| Backend release build | `backend\\mvnw.cmd --batch-mode clean package` | PASS (exit 0) | 128 tests, 0 failures/errors/**0 skipped**. The 2 new tests are `EquipmentRequestTransactionIntegrationTest` |
| Frontend release checks | Node 24.15.0-alpine: `npm ci && npm run lint && npm run typecheck && npm test && npm run build` | PASS (exit 0) | 31 tests; all routes built |
| OpenAPI | `mikefarah/yq:4` parse | PASS | 7 paths |
| k6 smoke + runs 1–5 | `tests/performance/run-k6.sh` per ADR-007 | **PASS** all thresholds | See the [performance report](../../../tests/performance/results/TASK-007-report.md). p95 10.7–12.2 ms, p99 14.9–18.4 ms, 0% unexpected errors, 96,887 of 96,887 checks in runs 1–5, 645 expected 409s counted separately |
| Clean-start rehearsal | Export of `git ls-files -co --exclude-standard` (what a checkout contains: no `node_modules`, `target` or `.env`) into an empty directory, then only the README steps | PASS with fixes | See the section below |
| HTTP journey on the rehearsal stack | curl: create → edit → stale edit → other user → submit → approve → re-approve → empty submit → reject without/with reason → cancel → filtered list → other Employee list → unknown ID → reference data | PASS | 201, 200, 409 `REQUEST_VERSION_CONFLICT` (title unchanged), 403, 200 PENDING, 200 APPROVED, 409 `REQUEST_STATE_CONFLICT`, 422 `ITEMS_REQUIRED`, 422 `REJECTION_REASON_REQUIRED`, 200 REJECTED with the reason, 200 CANCELLED, filtered total 1, other Employee 0, 404, 6 departments |
| Runbook recovery | Rehearsal stack: stop/start Redis, restart PostgreSQL | PASS | Redis down: detail 200 in 0.51 s, health DOWN, liveness UP. Redis back: 0.008 s, health UP. After the PostgreSQL restart: 3 × 200 and data persisted |
| Secret scan | `git grep` for password/secret/token/key assignments, AWS, GitHub and PEM patterns, over tracked and new files | 1 finding reported, not changed | See the hygiene section below |

## Clean-start rehearsal

| Step (README) | Result |
| --- | --- |
| `Copy-Item .env.example .env`; `docker compose up -d --wait` | PostgreSQL and Redis healthy on new, empty volumes |
| `cd backend; .\mvnw.cmd spring-boot:run` | Flyway applied **2** migrations (V1, V2) to the empty database; started; `/actuator/health` UP |
| `docker run … node:24.15.0-alpine npm ci` | 444 packages installed, exit 0 |
| `docker run … -p 3000:3000 … npm run dev` | **Environment deviation:** host ports 3000 and 3100 were held by unrelated pre-existing containers (`se-hr-backend`, `eager_kapitsa`), which were left untouched. The same command on `-p 3200:3000` reported Ready. `/requests`, `/requests/new`, detail and edit returned 200, and `/requests` contained the heading |
| Teardown | The rehearsal Compose project, its volumes, the frontend container, the backend process and the export directory were removed |

README fixes found by the rehearsal and audit:
- `frontend/README.md` referred to a missing `.nvmrc`. It is now added (`24.15.0`).
- The root README did not say that `.env` is read only by Compose. It now does, and it explains how to use another frontend port together with `FRONTEND_ORIGIN`.
- The root README was rewritten to cover what the assignment requires: environment and versions, run, test, API, decisions, state management and library rationale, caching placement, performance, assumptions and limitations.

A first attempt used port 3100. Its container failed to start ("port already allocated"), and the 200 responses seen at that moment came from the unrelated container. That attempt is **not** counted as evidence; the recorded result is the verified container on port 3200.

## Requirements audit (REQ-01…13)

| Requirement | Implementation | Automated tests | Other evidence | Status |
| --- | --- | --- | --- | --- |
| REQ-01 roles and ownership | `RequestAccessPolicy`, service access checks, Employee list scope | `EquipmentRequestWorkflowTest` (12 action × actor cases), MVC precedence, search scope integration | Rehearsal steps 4, 12, 13 | Met |
| REQ-02 transitions and terminal states | `RequestStatus.transition`, `requireEditable` | Workflow 20-case matrix, second approve, terminal edit | TASK-004 PostgreSQL smoke, rehearsal step 7 | Met |
| REQ-03 validation | Zod schema + `EquipmentRequestValidator`, submit and reject rules | Validator tests, workflow submit/reject, MVC nested paths, form tests | Rehearsal steps 8 and 9 | Met |
| REQ-04 form behavior | `useEquipmentRequestForm`, `useDirtyWarning`, reject dialog | 9 form tests + action tests (duplicate submit, server errors, reset, 409, dirty) | — | Met |
| REQ-05 list, search, pagination | Specification search, `RequestList`, URL state | 5 PostgreSQL search tests, 11 MVC list tests, 6 list UI tests | TASK-005 query plans | **Partially met; see gap G-1** |
| REQ-06 REST API and error contract | Controllers, `ApiExceptionHandler`, OpenAPI | MVC tests (25 + 2) | Rehearsal status codes | Met |
| REQ-07 PostgreSQL, locking, transactions | V1/V2 migrations, `@Version` aggregate | Transaction rollback integration (new), stale-update tests, cache rollback | TASK-003/004 concurrency, k6 expected 409s | Met |
| REQ-08 frontend tech and state | Next.js 16 / React 19 / TS; RHF + Zod; TanStack Query; 6 custom hooks | 31 Vitest/RTL tests (stale response, debounce cleanup) | README state management | Met |
| REQ-09 backend tech and layering | Spring Boot 4.1 / Kotlin / Maven; `api`/`application`/`domain`/`persistence`/`cache`/`configuration` | Compile and MVC slice tests | ADR-003 | Met |
| REQ-10 Redis + Caffeine | ADR-006 detail cache and reference-data cache | 7 container + 10 unit/slice cache tests | TASK-006 two-instance evidence, k6 hit rates | Met |
| REQ-11 tests (FE ≥ 4, BE ≥ 6) | — | FE 31, BE 128. Named cases below | — | Met |
| REQ-12 k6 with results | `workload.js`, `run-k6.sh` | — | [Performance report](../../../tests/performance/results/TASK-007-report.md) | Met |
| REQ-13 delivery | README, migrations, OpenAPI, Compose, lockfiles, wrapper | — | Clean-start rehearsal | Met (the API collection bonus was not done; OpenAPI is provided) |

Backend cases the assignment names, each mapped to a test:
- create draft → `create derives owner and request metadata…`
- submit without items → `submit without items is rejected with ITEMS_REQUIRED…`
- DRAFT → PENDING and approve from a non-PENDING state → `transition matrix allows only the approved rows`
- reject without a reason → `reject requires a nonblank reason`
- edit of a non-DRAFT request → `non-draft update is rejected`
- 404 → `unknown request returns not found`
- 409 without overwrite → `stale update is rejected without a save` plus the MVC precedence test
- keyword + status search → `keyword, status and department narrow together`
- item rollback → `create leaves no request when an item insert fails`

Frontend cases the assignment names:
- required-field errors, item add/remove, duplicate submit, server field errors, success reset with list update, and 409 → `request-form.test.tsx`
- search + filter together → `request-list.test.tsx`

### Gaps and findings

- **G-1: list row actions (open).** Assignment §4.2 pairs the request list with actions per status (DRAFT: Edit · Submit · Cancel; PENDING: Approve · Reject · Cancel). The REQ-05 summary did not capture that table, and TASK-005 decision 4 (approved) put all actions on the detail page. The list links each row there. The behavior exists but is one click away from the list. It needs a decision: accept as a documented deviation, or add row actions reusing `RequestActions`, since the list summary already carries `status` and `version`.
- **G-2: automated item-rollback test (closed).** The assignment's backend case "Transaction Rollback when saving an item fails" had only manual evidence from TASK-003. It is now covered by `EquipmentRequestTransactionIntegrationTest` (create and update).
- **H-1: token in the assignment HTML (open, not modified).** Line 2 of `Home-Quiz-IT-Equipment-Request-Management_revise_1.html` is a browser "saved from url" comment containing a Firebase Storage download URL with `token=…`. It has been committed since `8c6de53` and pushed. AGENTS.md forbids modifying the assignment file, so it is unchanged. The owner should revoke or regenerate the download token in Firebase if the file should not be publicly readable. Removing the comment would not remove it from git history.

## Limitations carried forward

See the README "Known limitations" section:
- mock identity only; no cloud deployment
- no approver or decision timestamp
- about 0.5 s detail reads during a Redis outage, and overall health DOWN during one
- single-host k6 with no capacity test
- no API collection

## Approval needed

The code reviewer must approve the TASK-007 changes and this evidence, and decide G-1 and H-1. The Verify and Delivery gates remain with the QA / acceptance owner and the release owner.
