# TASK-010: Resolve list-row actions (G-1 / AT-33)

Status: Design/Implement/Verify approved 2026-09-26 for merged `main` revision `77b56241`; Delivery approved with stated limitations
Owner: Developer and product owner
Requirement IDs: REQ-05; acceptance case AT-33 (P1)
Dependencies: TASK-005, TASK-008; implementation path selected by the user's "Implement" instruction

## Context

The original assignment §4.2 lists Edit · Submit · Cancel for DRAFT and Approve · Reject · Cancel for PENDING. [ADR-005](../../architecture/decisions/ADR-005-search-list.md) instead puts actions on the detail page. [AT-33](../../quality/acceptance-test-cases.md) records the difference, but its result and the owner decision are blank. A passing Playwright assertion about the current UI is not a Pass against the source criterion. See the [TASK-007 audit](../../quality/evidence/TASK-007.md).

## Scope / non-goals

Resolve AT-33 against the assignment. If the owner accepts the deviation, record that decision and its rationale in the acceptance record and ADR/traceability. Otherwise add role- and status-appropriate row actions, reusing the existing workflow behavior and conflict handling. Do not change backend authorization or state rules.

## Acceptance criteria

- [x] AT-33 has an automated Pass result against assignment §4.2; human Verify approval for merged revision `77b56241` is recorded in [approvals](../../governance/approvals.md).
- [x] Employee sees Edit/Submit/Cancel for owned DRAFT and Cancel for owned PENDING; Approver sees Approve/Reject for PENDING; terminal rows expose no mutation controls. The assignment's PENDING table also says Cancel, but the approved role matrix permits only the Employee owner to cancel; [ADR-005](../../architecture/decisions/ADR-005-search-list.md) records that interpretation.
- [x] Actions use the current `expectedVersion`; stale actions do not change a request. The list refreshes after success and preserves filters/page. Backend authorization still rejects unauthorized actions.
- [x] Playwright/RTL coverage checks role, status, success, conflict and AT-33; [evidence](../../quality/evidence/TASK-010.md) records commands and results.

## Verification and handoff

Frontend lint, typecheck, 32 tests and build passed; Docker Playwright passed 50/50. The later clean-checkout [TASK-013 evidence](../../quality/evidence/TASK-013.md) also records 129 backend tests. ADR-005, acceptance results, traceability and the gate record were reviewed for merged revision `77b56241`; the user approved Design/Implement/Verify and Delivery with disclosed limitations on 2026-09-26.
