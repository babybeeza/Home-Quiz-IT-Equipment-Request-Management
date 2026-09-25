# ADR-005: Request list, search and pagination

Status: Accepted
Date: 2026-09-25
Owner: Technical owner
Requirements/tasks: REQ-01, REQ-05, REQ-06, REQ-07, REQ-08, REQ-11 / TASK-005

## Context

The contract (`GET /api/v1/equipment-requests`) is already approved:
- `keyword`, `status`, `department`, `page` (≥0, default 0), `size` (1–100, default 10) and `sort` (`createdAt,asc|desc`, default desc)
- it returns `EquipmentRequestPage` of `EquipmentRequestSummary`
- Employees see only their own requests; Approvers see all

The data model already has the trigram GIN indexes on `lower(request_number|title|employee_name)` and B-tree indexes on `(owner_id|status|department, created_at DESC, id DESC)`. `totalItems` is the sum of item quantities (A-05) and must not load items one request at a time (N+1). The [ui-flow](../ui-flow.md) makes the URL the source of list state, debounces the keyword, resets the page on filter change and forbids stale responses from replacing newer ones.

This ADR decides how the query is built, how the parameters are validated, how the test runs against PostgreSQL, and where list state lives in the UI. It does not change the wire format.

## Options

### Query construction

1. **Spring Data `JpaSpecificationExecutor`:** predicates are added only for the parameters actually given, and `findAll(spec, pageable)` returns the page plus a count query. One grouped query then sums quantities for the page's IDs. That is 3 SQL statements per page no matter how many rows it has, and it stays in the JPA style chosen by ADR-003.
2. **One JPQL query with `(:p IS NULL OR …)` for every filter:** a single static query, but PostgreSQL may plan it generically and skip the indexes the data model planned.
3. **Hand-built native SQL through JdbcTemplate:** full control, but a second persistence style and hand-maintained mapping.

### PostgreSQL test harness

1. **Testcontainers `postgres:17-alpine` with `@ServiceConnection`:** automated, isolated from local data and runs the real Flyway V1. It needs Docker at test time.
2. **Tests against the Compose database, recorded as scripted evidence:** no new dependency, but search correctness has many combinations that are poorly suited to a manual script.

### Frontend list state

1. **URL as the only store for list parameters, with a custom `useRequestSearch` hook that parses and validates them. TanStack Query keys on actor + parameters.** Only the keyword and department inputs keep a local draft value while they are debounced.
2. **Component state mirrored into the URL:** two sources of truth, which is the duplicated derived state that REQ-08 asks to avoid.

## Decision

### Backend

Query option 1.
- A new `EquipmentRequestQueryService.search(actor, criteria)` runs read-only. The Employee scope adds `ownerId = actor.userId` unconditionally; Approvers add no scope predicate. No request parameter can change the scope.
- **Keyword:** trimmed. Blank means no keyword filter. Otherwise it matches `lower(request_number) LIKE p OR lower(title) LIKE p OR lower(employee_name) LIKE p`, where `p = '%' + escape(lower(keyword)) + '%'` with `\`, `%` and `_` escaped. This keeps the trigram expression indexes usable and makes `%` and `_` in user input match literally.
- **Status:** equality. **Department:** trimmed, case-insensitive equality (`lower(department) = lower(:department)`), and blank means no department filter. The existing plain `department` B-tree cannot serve that predicate. The TASK-005 evidence records `EXPLAIN ANALYZE` on a seeded dataset of at least 1,000 rows. A V2 expression-index migration is added only if that plan shows the filter is costly. This design does not change the schema.
- **Order:** `created_at <dir>, id <dir>`, so equal timestamps page deterministically. `totalPages = ceil(totalElements / size)`. A page past the end returns empty `content` with correct totals.
- **Totals:** `totalItems` comes from `select request_id, sum(quantity) … where request_id in (:pageIds) group by request_id`, and requests without items get 0.
- **Validation:** the controller reads the query parameters as strings and validates them itself, so every failure returns `400 VALIDATION_ERROR` with a field-named entry, for example `fieldErrors.size`. That covers `page` < 0 or non-integer, `size` outside 1–100 or non-integer, `sort` outside the enum, `status` outside the enum, `keyword` over 150 characters and `department` over 100. Missing or invalid identity headers keep returning `400 MALFORMED_REQUEST`, checked first. The list has no 403 case, because every valid role may list within its scope.

Test harness option 1. `org.testcontainers:postgresql` and `junit-jupiter` are added at test scope, with versions from the Spring Boot BOM. Search integration tests use `@Testcontainers(disabledWithoutDocker = true)`, so a machine without Docker reports them as skipped instead of failing. Evidence must show they executed, and a skip counts as NOT RUN.

### Frontend

State option 1.
- `/requests` becomes a client list inside a `Suspense` boundary, because `useSearchParams` needs one.
- `useRequestSearch()` parses `keyword`, `status`, `department`, `page` and `sort` from the URL and replaces invalid values with the defaults. `size` stays at the contract default of 10. It exposes `update(patch)`, which pushes a new URL entry and resets `page` to 0 whenever anything other than `page` changes, so back/forward and refresh restore the list.
- Keyword and department inputs keep a local draft value, and a 300 ms `useDebouncedValue` commits it to the URL. The debounce clears its timer on unmount and on each keystroke.
- `useQuery` keys on `["equipment-requests", userId, role, params]` and passes the `AbortSignal` to `fetch`. Old requests are cancelled or ignored because their key differs. `placeholderData` keeps the previous page visible only for the same actor, marked with a loading indicator. Actor changes already clear the query client (TASK-003).
- The table has these columns: request number (a link to the detail page), title, employee, department, required date, total items, status (as text) and created at. The page also has loading, error, empty (with a "clear filters" action) and success states, plus "page N of M" with totals and previous/next buttons.
- Rows link to the detail page, where the TASK-004 actions live. Inline row actions are not added. The contract summary lacks the item data that submit needs, and duplicating the action UI would add risk without meeting a requirement.

## Consequences and verification

Option 1 costs three SQL statements per page. That is predictable and O(1) in the page size, and the summing query only touches the page's IDs through the existing `request_id` index. Testcontainers adds a Docker requirement to the full backend test run. Unit and MVC tests still run without Docker.

Verification follows the [TASK-005 test design](../../quality/TASK-005-test-design.md). The query-plan evidence and the seed dataset are handed to TASK-006/007 as the pre-cache baseline.
