# Evidence: TASK-004 Design

Date / operator: 2026-09-25 / Claude Code
Status: Approved by project owner acting as technical owner on 2026-09-25
Requirements: REQ-01, REQ-02, REQ-03, REQ-04, REQ-06, REQ-07, REQ-11

## Reviewed inputs

- Approved requirements, assumptions A-02 to A-04 and the implementation plan's state/permission matrix
- Accepted ADR-002/ADR-003, the OpenAPI action endpoints (`VersionAction`, `RejectAction`, 422 responses), [api-behavior](../../architecture/api-behavior.md) and [ui-flow](../../architecture/ui-flow.md)
- The TASK-003 implementation: `EquipmentRequestService`, `RequestStatus.transition`, `RequestAccessPolicy`, `EquipmentRequestValidator.validateForSubmit/validateRejectionReason`, `ApiExceptionHandler`, and the detail page and query cache

## Design outputs

- [ADR-004](../../architecture/decisions/ADR-004-approval-workflow.md) decides the per-action service template and check order, the 400/409/422 code assignment for submit and reject failures, body parsing that preserves precedence, and frontend action state with the reject dialog.
- [TASK-004 test design](../TASK-004-test-design.md) lists the parameterized matrix, precedence, business-rule, MVC, concurrency and UI cases before implementation.

## Decisions for reviewer attention

1. Submit failure codes: empty items only → `ITEMS_REQUIRED`; any other stored-data failure, such as a past date → `BUSINESS_RULE_VIOLATION`. Both are 422 with `fieldErrors`.
2. Reject reason: missing or blank → 422 `REJECTION_REASON_REQUIRED`; over 500 characters → 400 `VALIDATION_ERROR`. Both are evaluated after the version and state checks.
3. Cancel asks for browser confirmation; submit and approve do not.
4. Decision actor and timestamp are not stored. That would need a migration and contract change the assignment does not ask for.
5. Real-DB concurrency uses scripted Compose evidence, as in TASK-003, rather than an automated container test.

No code, contract or migration changes are part of this Design step.
