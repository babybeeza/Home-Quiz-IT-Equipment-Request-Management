# ADR-003: Draft vertical slice architecture

Status: Accepted
Date: 2026-09-25
Owner: Technical owner
Requirements/tasks: REQ-01, REQ-03, REQ-04, REQ-06, REQ-07, REQ-08, REQ-09, REQ-11 / TASK-003

## Context

TASK-003 must implement create, detail and edit for an equipment-request draft from Next.js through Spring Boot to PostgreSQL. The backend must own identity, authorization, validation, state and version checks. Request and item writes must be atomic, and an item-only edit must increment the parent version. The UI must retain failed input, map nested errors, prevent duplicate saves, warn about dirty forms and avoid overwriting a newer version after HTTP 409.

The OpenAPI contract and ADR-002 already fix the wire format and domain rules. This ADR decides how the vertical slice is divided and where transaction, form and server state live.

## Options considered

### Backend persistence

1. Model the request and items as one JPA aggregate. Replace its child collection inside one transaction and deliberately touch the parent on every edit.
2. Persist request and items through separate repositories and coordinate updates manually.
3. Use SQL/JDBC for explicit versioned statements and child replacement.

Option 1 follows the project stack, keeps the transaction boundary visible and uses JPA optimistic locking. Option 2 makes partial writes and version handling easier to get wrong. Option 3 gives more SQL control but adds a second persistence style before it is needed.

### Frontend state

1. React Hook Form with Zod for editable form state and TanStack Query for remote request state.
2. Hand-written `useState` validation and `fetch` effects.
3. Keep fetched request data and editable form data in one shared store.

Option 1 separates server and form concerns and supports dynamic arrays and nested error paths. Option 2 would require custom validation, dirty tracking and race handling. Option 3 risks mutating cached server data while the user edits.

## Decision

### Backend layers

- `api`: controllers, header identity parsing, request/response DTOs, DTO ↔ draft/view mapping and global exception translation.
- `application`: transactional create/get/update use cases over domain drafts, request-number allocation, authorization order and entity → view mapping. It does not depend on `api` types. (Amended 2026-09-25 in TASK-003 review round 2; no behavior or contract change.)
- `domain`: approved status, access and validation policies. The application layer calls these policies rather than duplicating rules.
- `persistence`: JPA entities and repositories. API responses never expose entities.

`EquipmentRequestEntity` is the aggregate root. It owns `EquipmentRequestItemEntity` with cascade and orphan removal and has JPA `@Version`. Create and update run in one `@Transactional` method. Update performs these checks in order: load existing record, authorize actor, compare `expectedVersion`, require DRAFT, validate input, replace items, update `updatedAt`, flush, then map the response. Updating `updatedAt` makes an item-only edit dirty at the aggregate root, so the parent version increments.

The explicit version comparison produces a stable conflict before mutation. A database optimistic-lock exception during flush remains a second collision defense and maps to the same `REQUEST_VERSION_CONFLICT`. A failed child insert rolls the transaction back. Integration tests use PostgreSQL because an in-memory database cannot prove the chosen locking and constraint behavior.

Request IDs and item IDs are application-generated UUIDs. The application obtains the next value from `equipment_request_number_seq` and formats `REQ-{Asia/Bangkok year}-{sequence padded to at least six digits}`. Sequence gaps after rollback remain valid. Blank optional text is normalized to null at the application boundary.

The identity adapter accepts required `X-User-Id` and `X-Role` headers, trims the user ID and maps the role case-sensitively to the approved enum. Missing, blank or unknown values return `400 MALFORMED_REQUEST`. Identity is never accepted in a request body.

Exception advice returns the common OpenAPI error envelope. Field validation returns `400 VALIDATION_ERROR`; an existing request outside the actor's permissions returns 403; missing UUID data returns 404; stale version or non-DRAFT update returns 409; domain business failures reserved by the contract return 422; unexpected details are logged server-side and sanitized in the response.

### Frontend structure

- Install React Hook Form, Zod, the Zod resolver and TanStack Query during implementation, with exact compatible versions recorded in the lockfile.
- A client-side identity provider exposes fixed demo actors and persists only the selected actor key in browser storage. Every API query key includes role and user ID. Changing actor cancels/invalidates actor-scoped queries and navigates to `/requests`.
- A typed API client owns base URL, identity headers, JSON parsing, abort signals and `ApiError` decoding.
- `/requests/new` creates a draft. `/requests/{id}` renders detail. `/requests/{id}/edit` loads a request and permits the owner to edit DRAFT data. The full searchable list remains TASK-005.
- `useEquipmentRequestForm` is the required custom hook. It owns schema validation, dynamic item fields, pending submission guard, API field-error mapping, conflict state and successful baseline reset.
- React Hook Form owns editable values, dirty/touched state and field errors. TanStack Query owns fetched detail. Status, permissions and totals are derived values.
- A failed save keeps current values. Known server paths such as `items[0].quantity` map to fields; unknown paths appear in a form-level alert. A 409 displays an explicit conflict panel with “reload latest”; it never automatically resets or resubmits dirty input.
- Pending saves disable the submit control and the submission handler rejects a second invocation. Successful create/update resets the form to the returned payload before navigation or refetch.
- Dirty protection registers `beforeunload` and guards application-owned navigation controls. The browser's native prompt text is not customized. Cleanup removes listeners when the form becomes clean or unmounts.

## Consequences and verification

The aggregate replacement approach is simple for a small item collection but issues delete/insert work on update. TASK-003 verifies correctness first; query tuning belongs to later evidence if volume makes it necessary. Updating the parent timestamp for every accepted edit is intentional and observable.

Verification must include unit tests for use-case ordering and errors, MVC tests for headers/envelopes, frontend behavior tests and PostgreSQL integration tests for rollback and optimistic locking. Two transactions starting from one version must yield one successful commit and one conflict, and an item-only edit must increment the parent version. The approved cases are listed in [TASK-003 test design](../../quality/TASK-003-test-design.md).

