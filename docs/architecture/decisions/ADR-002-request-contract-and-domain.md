# ADR-002: Request contract, identity and domain semantics

Status: Accepted
Date: 2026-09-25
Owner: Project owner / developer
Requirements/tasks: REQ-01, REQ-02, REQ-03, REQ-06, REQ-07 / TASK-002

## Context
The assignment permits simulated roles but still requires backend authorization, ownership, state validation and optimistic concurrency. It leaves identity headers, draft validation timing, timezone, action versions, total-items semantics and access-denial status unspecified.

## Decision

### Identity and permissions
- Every endpoint requires `X-User-Id` and `X-Role` (`EMPLOYEE` or `APPROVER`). This is a demo identity contract, not authentication.
- `owner_id` comes only from `X-User-Id`; request email may change without transferring ownership.
- Employees create, list, view, edit, submit and cancel only their own requests. Approvers list/view all requests and may approve or reject PENDING requests. Approvers do not edit or cancel requests.
- Known resources outside the actor's scope return `403 ACCESS_DENIED`; missing IDs return `404 REQUEST_NOT_FOUND`. This makes demo authorization observable. A production security design may choose 404 to reduce resource enumeration.

### Validation and time
- Create/update draft requires all scalar fields from the assignment to be valid. `items` may be empty while DRAFT; any supplied item must be valid. Submit revalidates the whole request and requires at least one item.
- The business timezone is `Asia/Bangkok`. Required date is represented as ISO `LocalDate` and must be today or later on create/update and again on submit. Domain validation receives a `Clock` so boundary tests are deterministic.
- Blank optional strings are normalized to null. Reject reason is trimmed, required and limited to 500 characters.

### Concurrency and list semantics
- PUT and every workflow action carry `expectedVersion` in the JSON body. A stale value returns `409 REQUEST_VERSION_CONFLICT`; an invalid transition returns `409 REQUEST_STATE_CONFLICT`.
- Editing child items must increment the parent request version. No mutation retries automatically after a version conflict.
- `totalItems` is the sum of item quantities. `itemCount` is reserved for the number of item rows if later needed.
- Search uses case-insensitive contains over request number, title and employee name, OR within keyword and AND with status, department and actor scope. Default sort is `createdAt,desc` with `id,desc` as a deterministic tie-breaker. `page` starts at 0, default size is 10 and maximum size is 100.

### Request number and persistence
- The application allocates the next PostgreSQL sequence value and formats `REQ-{year}-{sequence padded to 6}` using the business year. Gaps are allowed; `request_number` remains unique. This avoids concurrent `count + 1` allocation.
- UUIDs are application-generated. Request and items persist in one transaction. Database constraints defend enum values, lengths, quantities and foreign-key integrity; application/domain validation provides stable error codes.
- PostgreSQL `pg_trgm` GIN indexes support contains search. Deployment therefore requires permission to install the extension during migration.

## Consequences and verification
- Clients must send identity headers and expected versions; OpenAPI documents both.
- The API never accepts status, owner ID, rejection reason or audit/version fields through create/update DTOs.
- Tests cover every transition, date boundaries and validation limits. Flyway is run against a fresh PostgreSQL database and rerun to prove idempotent startup behavior.
- Design approved by the project owner acting as technical owner on 2026-09-25. Material contract changes require a new ADR or renewed Design approval.
- TASK-016 amendment, 2026-09-26 (Design approved by the project owner acting as technical owner): Spring MVC client errors that carry their own 4xx status use the shared `ApiError` envelope instead of `500 INTERNAL_ERROR`. An unmatched route returns `404 NOT_FOUND`, which is distinct from `404 REQUEST_NOT_FOUND` for a missing request. An unsupported method returns `405 METHOD_NOT_ALLOWED` with `Allow`, `406 NOT_ACCEPTABLE` or `415 UNSUPPORTED_MEDIA_TYPE` covers media-type mismatches, and any other framework 4xx uses `MALFORMED_REQUEST`. These are logged at DEBUG; only genuine failures return `500 INTERNAL_ERROR` with an ERROR log.
