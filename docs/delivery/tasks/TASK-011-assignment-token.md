# TASK-011: Resolve committed assignment download token (H-1)

Status: Source owner confirmed token revoked 2026-09-26; saved link returned HTTP 403 again; release owner accepted residual Git history for `77b56241`
Owner: Project owner / source-storage owner
Requirement IDs: REQ-13 (delivery hygiene)
Dependencies: Access to the original file's storage controls

## Context

[TASK-007 evidence](../../quality/evidence/TASK-007.md) reports a download URL with a token in line 2 of the original assignment HTML. The file and its history must remain untouched by implementation work. Two workspace GET requests returned HTTP 403 on 2026-09-25; source-side validity and intended access remain unknown. See [TASK-011 evidence](../../quality/evidence/TASK-011.md).

## Scope / non-goals

Determine whether the token still grants access to the original file. If it does and access is unwanted, revoke or rotate it at the source. Do not copy the token into evidence, edit the authoritative assignment HTML, or claim that a repository edit removes the value from existing Git history.

## Acceptance criteria

- [x] Workspace GET result is recorded without storing the token: HTTP 403 twice.
- [x] Source-storage owner (user) confirmed on 2026-09-26 that the token is revoked. The owner did not separately state the prior intended access policy.
- [x] The saved link returned HTTP 403 again from this workspace after that confirmation; this is an external reachability observation, not independent access to storage administration.
- [x] The delivery handoff records the owner statement, date and [safe evidence](../../quality/evidence/TASK-011.md) without copying the value. The release owner still decides the Delivery gate.

## Verification and handoff

The workspace HTTP checks and the user's source-owner confirmation are recorded in [evidence](../../quality/evidence/TASK-011.md). The owner stated that the token is revoked; this task has no direct storage-admin inspection. Published Git history still contains the original HTML and saved URL text. No local HTTP test alone can establish global revocation.
