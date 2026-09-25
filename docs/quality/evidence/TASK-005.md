# Evidence: TASK-005 implementation

Date / operator: 2026-09-25 / Claude Code; Implement approved by project owner acting as code reviewer on 2026-09-25
Approved Design: [ADR-005](../../architecture/decisions/ADR-005-search-list.md), [TASK-005 test design](../TASK-005-test-design.md)
Environment: Windows, Temurin Java 25.0.3 targeting Java 21, Docker Desktop (Testcontainers 2.0.5 with `postgres:17-alpine`), Node 24.15.0-alpine container, Compose PostgreSQL 17.11
Requirements: REQ-01, REQ-05, REQ-06, REQ-07, REQ-08, REQ-11

| Check | Command / method | Result | Observation |
| --- | --- | --- | --- |
| Backend clean package | `backend\\mvnw.cmd --batch-mode clean package` | PASS (exit 0) | 101 tests, 0 failures/errors/**0 skipped**. The 5 PostgreSQL container tests in `EquipmentRequestSearchIntegrationTest` executed. There are 11 new MVC list tests (25 MVC total) |
| Mutation check: LIKE escape | Temporarily remove `escapeLike`, then `-Dtest=EquipmentRequestSearchIntegrationTest` | FAIL as intended | The keyword test failed on the `%`/`_` literal cases; the source was restored |
| Frontend quality checks | Node 24.15.0-alpine container, isolated `node_modules`/`.next` volumes: `npm ci && npm run lint && npm run typecheck && npm test && npm run build` | PASS (exit 0) | Lint and typecheck passed; 29 tests passed (6 new list tests plus the updated page test); `/requests` prerendered as static |
| API against the seeded Compose DB | Packaged jar with `spring.jpa.show-sql=true`, after [seed-search-dataset.sql](../../../tests/performance/seed-search-dataset.sql) (1,207 requests / 1,810 items) | PASS | Employee `seed-employee-03` → 120 rows / 12 pages (own only). Approver: `status=PENDING` → 240; `keyword=monitor` → 172; keyword + PENDING + `department=software engineering` → 6; Thai keyword `จอภาพ` → 171 |
| Statements per page | Captured Hibernate SQL, plus the `prepareStatementCount ≤ 3` assertion in the integration test | PASS | Page select, count and one `sum(quantity) … where request_id in (…) group by`. No per-row item loading |
| Query plans | [explain-search.sql](../../../tests/performance/explain-search.sql) → [TASK-005-explain.txt](../../../tests/performance/results/TASK-005-explain.txt) | Recorded | See the analysis below |

## Query-plan analysis (1,207 rows, PostgreSQL 17.11)

| Query | Plan | Execution |
| --- | --- | --- |
| Employee default page | Index Scan `idx_equipment_requests_owner_created` (no sort) | 0.065 ms |
| Employee count | Bitmap Index Scan on the owner index | 0.177 ms |
| Approver `status=PENDING` | Index Scan `idx_equipment_requests_status_created` (no sort) | 0.040 ms |
| Keyword `monitor` (172 matches) | Seq Scan + top-N sort | 1.275 ms |
| Keyword + status + department | Bitmap Index Scan on the status index, then filter | 0.220 ms |
| Department only (`finance`) | Seq Scan + top-N sort | 0.667 ms |
| Page totals (exact `IN` list of 10 IDs) | Bitmap Index Scan `idx_equipment_request_items_request` | 0.145 ms |

- **Trigram indexes are usable:** with `enable_seqscan = off`, the keyword query uses a BitmapOr over all three `*_trgm` expression indexes (0.771 ms). At this table size (about 39 heap pages) the planner correctly prefers a sequential scan, even for a single-row match (1.227 ms). The generated `lower(col) LIKE ? ESCAPE '\'` predicate is index-compatible.
- **Department decision (ADR-005 item 1):** there is no V2 expression index. The case-insensitive department filter takes 0.67 ms at 1,207 rows, and combined with status it uses the status index. The data is handed to TASK-007 k6 to re-check at load volume.

## Implemented boundaries

- **Backend:**
  - `EquipmentRequestSpecifications.search` adds a predicate only for present parameters. The Employee owner scope is forced by `EquipmentRequestQueryService`.
  - Sorting is `created_at, id` in one direction.
  - `sumItemQuantities` fills `totalItems`, with 0 for requests without items.
  - `parseSearchCriteria` turns every invalid parameter into a field-named `VALIDATION_ERROR` after the identity checks. Blank text means absent.
- **Frontend:**
  - `useRequestSearch` treats the URL as the only store. Invalid values fall back to defaults, and a non-page change resets to page 0.
  - `useDebouncedUrlField` debounces the commit to the URL (300 ms), not the typed value, cancels its timer on unmount, and never overwrites input typed after its own commit.
  - The query key is actor + parameters, so an old response cannot replace newer data. `placeholderData` keeps the previous page only for the same actor.
  - The list has 8 columns, a detail link, and loading, error-with-retry, empty (clear filters or back to first page) and success states. Pagination buttons are disabled at the bounds.
- **Tooling:** `vitest.config.ts` gains the `@/` alias from `tsconfig.json`. It was missing, and no test had imported through it before.

## Deviation from ADR-005 wording

ADR-005 names a `useDebouncedValue` hook. The implementation debounces the URL commit inside `useDebouncedUrlField` instead. Debouncing the value would have required a state update inside an effect, which the React hooks lint rule rejects, and could overwrite characters typed after a commit. Delay, cleanup and observable behavior are as designed and covered by the debounce test.

## Scope notes

- Inline row actions are not implemented (ADR-005 item 4); rows link to the detail page.
- Seed rows (`seed-%` owners) remain in the local development database for TASK-006/007. The Compose services were stopped afterwards.
- Browser walkthrough: NOT RUN. Behavior is covered by React Testing Library tests and the production build.

## Approval needed

The code reviewer must approve the TASK-005 code, tests and this evidence before the Implement gate closes.
