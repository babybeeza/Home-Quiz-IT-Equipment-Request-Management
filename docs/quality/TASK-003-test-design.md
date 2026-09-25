# TASK-003 verification design

Status: Approved in TASK-003 Design on 2026-09-25; no results are claimed here
Requirements: REQ-01, REQ-03, REQ-04, REQ-06, REQ-07, REQ-08, REQ-09, REQ-11

| Boundary | Required behavior |
| --- | --- |
| Create API | Employee creates DRAFT with server owner, UUID, sequence number, version 0, timestamps and persisted items; Approver gets 403 |
| Detail API | Employee owner and Approver can read; Employee non-owner gets 403; unknown UUID gets 404 |
| Update API | Owner updates DRAFT with matching version; scalar and item-only updates increment version; non-owner/Approver get 403; non-DRAFT gets 409 |
| Validation | Exact and over boundaries, malformed JSON/UUID/headers, past business date and nested item paths return the approved envelope |
| Conflict | Stale `expectedVersion` returns 409 and leaves the latest scalar fields/items/version unchanged |
| Transaction | A forced child constraint failure leaves no partial create and does not partially replace items on update |
| Real concurrency | Two PostgreSQL transactions using one version produce exactly one successful update and one optimistic conflict |
| Request number | Sequence-backed values match the approved format, remain unique and tolerate a rollback gap |
| Form basics | Required and boundary messages render; item rows add/remove immutably; an empty item list is valid for a draft |
| Save lifecycle | Double submit calls the API once; pending controls are disabled; API failure preserves values; success resets dirty baseline |
| Server errors | Scalar and `items[index].field` errors map to inputs; unknown paths render in the form alert |
| Conflict UX | 409 keeps user input, shows reload-latest action and never overwrites automatically |
| Dirty protection | Clean forms do not warn; dirty forms register navigation/unload protection; cleanup removes it after reset/unmount |
| Detail states | Loading, accessible error, forbidden/not-found and success content render from user-visible behavior |
| Stale response | Changing actor or request cancels/isolates earlier detail queries; old data cannot render for the new actor |

## Evidence plan

- Backend unit/MVC tests run with the Maven wrapper.
- PostgreSQL integration checks run against the Compose database and record before/after rows and versions.
- Frontend tests run with Vitest and React Testing Library; lint, typecheck and production build also run.
- Evidence records exact commands, runtime/database versions, counts and any NOT RUN case with its reason.

