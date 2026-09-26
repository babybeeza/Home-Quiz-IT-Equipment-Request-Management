# TASK-011: Resolve committed assignment download token (H-1)

Status: Workspace check done (HTTP 403) — source-storage owner decision pending
Owner: Project owner / source-storage owner
Requirement IDs: REQ-13 (delivery hygiene)
Dependencies: Access to the original file's storage controls

## Context

[TASK-007 evidence](../../quality/evidence/TASK-007.md) reports a download URL with a token in line 2 of the original assignment HTML. The file and its history must remain untouched by implementation work. Two workspace GET requests returned HTTP 403 on 2026-09-25; source-side validity and intended access remain unknown. See [TASK-011 evidence](../../quality/evidence/TASK-011.md).

## Scope / non-goals

Determine whether the token still grants access to the original file. If it does and access is unwanted, revoke or rotate it at the source. Do not copy the token into evidence, edit the authoritative assignment HTML, or claim that a repository edit removes the value from existing Git history.

## Acceptance criteria

- [x] Workspace GET result is recorded without storing the token: HTTP 403 twice.
- [ ] Source-storage owner confirms whether the token is active in any intended context and whether that access is intended.
- [ ] If access is unintended, the storage owner revokes or rotates it and verifies that the old link no longer works.
- [ ] Delivery handoff records the decision, date and a safe evidence reference; if source access is unavailable, retain H-1 as an explicit limitation.

## Verification and handoff

The workspace HTTP check is recorded in [evidence](../../quality/evidence/TASK-011.md). The storage owner must perform and record the source-side check. No local automated test can establish revocation. Any test not performed is NOT RUN with a reason.
