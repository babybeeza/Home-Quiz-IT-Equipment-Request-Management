# TASK-005 verification design

Status: Approved in TASK-005 Design on 2026-09-25; no results are claimed here
Requirements: REQ-01, REQ-05, REQ-06, REQ-07, REQ-08, REQ-11

| Boundary | Required behavior |
| --- | --- |
| Scope (PostgreSQL) | The fixture has 2 Employees and 3 departments across all 5 statuses. An Employee sees only their own rows, and their `totalElements` counts only their own rows. An Approver sees every row |
| Keyword (PostgreSQL) | Case-insensitive contains-match on the request number, title and employee name, each proven by a row that matches through that field only. Blank or whitespace applies no filter. `%` and `_` in the keyword match literally. Thai text matches |
| Combined filters (PostgreSQL) | Keyword AND status AND department narrow together. Department matching is trimmed and case-insensitive |
| Pagination (PostgreSQL) | Correct `totalElements`/`totalPages` for an empty result, a full page, the last partial page and a page past the end. Paging through with both sorts returns every row exactly once, including rows with identical `created_at` |
| totalItems (PostgreSQL) | Equals the sum of quantities, is 0 for a request without items, and uses a bounded number of statements per page, asserted with the Hibernate statement count |
| Validation (MVC) | `page` = -1 or `x`, `size` = 0 or 101 or `x`, an unknown `sort` or `status`, keyword over 150 and department over 100 each give 400 `VALIDATION_ERROR` naming that field. Missing identity gives 400 `MALFORMED_REQUEST`. Defaults apply when parameters are absent |
| Query plan | `EXPLAIN ANALYZE` on a ≥1,000-row seed for: Employee default page, Approver status filter, keyword search, and keyword + status + department. Evidence records plans, timings and any index decision |
| URL state (UI) | Reading `?keyword=&status=&department=&page=&sort=` renders the matching query. Changing a filter pushes a URL with `page` 0. Paging keeps the filters. Invalid URL values fall back to defaults |
| Debounce (UI) | Typing several characters quickly causes one list request with the final keyword after the delay, and pending timers are cleared on unmount |
| Stale response (UI) | A slow response for an old keyword that arrives after the new keyword's response does not replace the newer rows |
| Rendering states (UI) | Loading, accessible error, empty with a "clear filters" action, and success showing all 8 columns with status as text and a link to the detail page. Pagination controls are disabled at the bounds |

## Evidence plan

- Backend unit, MVC and Testcontainers integration tests run with the Maven wrapper. Evidence states how many integration tests executed; a skipped container test counts as NOT RUN.
- The seed script and `EXPLAIN ANALYZE` output are run against Compose PostgreSQL 17.
- Frontend tests run with Vitest and React Testing Library, together with lint, typecheck and production build in the Node 24.15.0 container.
