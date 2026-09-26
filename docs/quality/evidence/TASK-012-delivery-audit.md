# TASK-012: Delivery and learn audit

Date / operator: 2026-09-25 / Codex
Revision reviewed: `8a09c4d`; working tree was clean before this documentation update
Environment: Windows / PowerShell; Docker Compose v5.1.3; existing `home-quiz` stack on host ports 3300 and 8090
Scope: TASK-001–009 delivery evidence, acceptance sign-off, runbook and original assignment §4.2

## Readiness

Delivery is **pending**. The project owner approved Verify for TASK-001–009, but [AT-33](../acceptance-test-cases.md) is a P1 criterion whose current implementation differs from assignment §4.2; neither a Pass result nor an explicit decision accepting a Fail is recorded. The same acceptance document says Verify can be approved only after each P1 passes or a failure has a decision. The existing approval statement is preserved as a historical fact; it does not itself resolve G-1. TASK-010 captures the decision or implementation path.

H-1 is a separately disclosed delivery issue: a token appears in the authoritative assignment HTML's saved-from URL comment. Its validity is unknown. TASK-011 assigns the source-side check and possible revocation to the storage owner. The assignment file was not modified.

No release owner, target or final revision has been recorded. Application rollback and data restore are documented but not rehearsed. These points must be visible to the release owner; they are not claimed as tested.

## Checks this audit performed

| Check | Command / method | Exit code | Result | Observation |
| --- | --- | --- | --- | --- |
| Clean starting state | `git status --short` | 0 | PASS | No tracked changes before this audit |
| Revision | `git rev-parse --short HEAD` | 0 | PASS | `8a09c4d` |
| Compose syntax | `docker compose --profile app config --quiet` | 0 | PASS | Current single-Compose app profile parses |
| Docker UI response | PowerShell `Invoke-WebRequest http://localhost:3300/requests` | 0 | PASS | HTTP 200 from the already running stack; no new clean start claimed |
| Backend health response | PowerShell `Invoke-WebRequest http://localhost:8090/actuator/health` | 0 | PASS | HTTP 200 from the already running stack |
| Assignment comparison | Read original HTML §4.2 and AT-33 | N/A | FAIL for criterion alignment | Source lists actions on request list; AT-33 documents actions on detail instead |
| Backend/frontend full checks | See TASK-009 evidence | N/A | NOT RUN in this audit | Existing recorded checks are 129 backend tests, 31 frontend tests and build |
| Playwright | See TASK-009 evidence | N/A | NOT RUN in this audit | Existing recorded run is 49/49; this audit changed only docs |
| Rollback and data restore | Runbook review | N/A | NOT RUN | No rehearsal evidence exists for the Docker rollback or restore path |
| Edited Markdown links | PowerShell regex extraction and `Test-Path` for each local target | 0 | PASS | All edited local links resolve |
| Patch whitespace | `git diff --check` | 0 | PASS | No whitespace errors; Git noted line-ending normalization for the prompt file |

## Learn and next actions

An approval record can conflict with its own exit criterion when an automated test asserts known current behavior rather than the source requirement. The [Deliver and learn prompt](../../../ai/prompts/README.md) now calls for checking every P1 result and any deviation decision before stating readiness. The [runbook](../../operations/runbook.md) now names the Docker start/stop path, its untested recovery/rollback limits and the open actions. TASK-010 and TASK-011 are bounded follow-ups; the release owner must decide the final revision and gate after reviewing their outcomes.
