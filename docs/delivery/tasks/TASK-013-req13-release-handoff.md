# TASK-013: Verify and hand off a REQ-13 release revision

Status: Candidate verified; reviewer-access check and human gates pending; Delivery pending
Owner: Developer prepares evidence; project owner reviews changed gates; release owner decides handoff
Requirement IDs: REQ-13 (required delivery package), REQ-05/AT-33 as a changed dependency
Dependencies: [TASK-010](TASK-010-list-row-actions.md) review, [TASK-011](TASK-011-assignment-token.md) source-owner disposition or explicit limitation, [A-08](../../product/assumptions.md) handoff choice, TASK-007–009 evidence

## Human approvals

| Gate | Approver | Decision | Date / revision |
| --- | --- | --- | --- |
| Requirements | Project owner | Existing REQ-13 baseline approved; 2026-09-26 [clarification](../../product/req-13-discovery.md) prepared for review | Earlier approval 2026-09-25 |
| Design | Technical owner | TASK-010 ADR-005 amendment in review; no new application design in this handoff task | — |
| Implement | Code reviewer | Pending for TASK-010 changes and final candidate | — |
| Verify | QA / acceptance owner | Pending for changed revision | — |
| Delivery | Release owner | Pending | — |

## Context

Read the [original assignment](../../../Home-Quiz-IT-Equipment-Request-Management_revise_1.html) §10, [REQ-13 Discover](../../product/req-13-discovery.md), [traceability](../../product/traceability.md), [approval record](../../governance/approvals.md), [runbook](../../operations/runbook.md), [acceptance cases](../../quality/acceptance-test-cases.md), [TASK-010 evidence](../../quality/evidence/TASK-010.md) and [TASK-011 evidence](../../quality/evidence/TASK-011.md).

## Scope / non-goals

Prepare one identified repository revision for someone else to obtain, install, test and run. Verify that revision from a clean checkout and assemble a release handoff with actual commands, results, limitations and gate decisions. Include the mandatory source, README, migration/schema and automated tests; report the API-collection and Compose bonuses separately. This task does not add cloud deployment, authentication or an API collection, and does not edit the authoritative assignment HTML.

## Acceptance criteria

- [ ] The release owner identifies the handoff Git remote/access method and immutable revision. A reviewer can fetch that revision; a configured `origin` alone does not establish reviewer access. If access fails, handoff remains pending.
- [x] The application revision contains frontend/backend source, README, database migration or schema and automated tests. README covers environment, run, test/API, decisions and scope as listed in REQ-13 Discover.
- [x] From an isolated clean checkout of the application revision, the documented setup starts the database and both applications, and a role-based create → submit → decision → list smoke succeeds. Port changes are recorded in [TASK-013 evidence](../../quality/evidence/TASK-013.md).
- [x] Frontend lint, typecheck, tests and build; backend package/tests including the Testcontainers executed/skipped count; Compose validation; and Playwright acceptance are recorded with commands, exit codes and output in the evidence.
- [ ] TASK-010 Design/Implement/Verify review and AT-33 disposition are recorded for the release revision. TASK-011/H-1 has a source-owner decision or remains an explicit limitation for the release owner to assess. Untested rollback/restore and the two manual P2 cases remain labelled as such unless actually checked.
- [ ] README, runbook, traceability and release evidence all name the same revision and limitations; the release owner records the Delivery decision. If a required check fails or is missing, status remains pending with a concrete next action.

## Implementation plan

1. Audit tracked files and the pending worktree changes. Complete reviews of TASK-010 and the REQ-13 clarification; record the source-owner response for TASK-011 without copying token values. Choose a candidate revision and confirm remote access with the reviewer-access method.
2. Use a separate clean checkout of that revision. Follow the root README to build/start the Compose app and perform the smoke. Keep the existing workspace and its database volumes untouched.
3. Run the required checks on the candidate. Commands from the present wrappers/manifests are listed below; re-read them on the candidate before execution in case they changed.
4. Write `docs/quality/evidence/TASK-013.md` with environment, revision, command, exit code, counts and observed behavior. Update the README/runbook/traceability only for discrepancies found; if any changes alter the candidate, choose a new revision and repeat affected checks.
5. Present the exact revision, evidence, bonus/limitation list and outstanding decisions to the release owner. Record their Delivery decision in `docs/governance/approvals.md`; do not infer approval from passing automation.

## Verification

| Check | Command from current tooling | Expected evidence / failure path |
| --- | --- | --- |
| Repository and access | `git status --short`, `git rev-parse HEAD`, `git remote -v`, then reviewer-access fetch/clone check | Exact revision and access result; a failed fetch blocks handoff |
| Backend | In `backend/`: `.\mvnw.cmd --batch-mode clean package` on Windows, or `./mvnw --batch-mode clean package` on Linux | Exit code, test/failure/skip counts; skipped Testcontainers are NOT RUN |
| Frontend | In `frontend/` with Node 24.15.0: `npm ci`, `npm run lint`, `npm run typecheck`, `npm test`, `npm run build` | Separate exit codes and counts; stop on failure |
| Docker app | `docker compose --profile app config --quiet`; `docker compose --profile app up -d --build --wait` | Exit codes, health and smoke; choose free host ports without touching unrelated services |
| Browser acceptance | Git Bash on this Windows host: `& 'C:\Program Files\Git\bin\bash.exe' tests/e2e/run-e2e.sh` | Exit code, Playwright pass/fail count and AT-33 result; isolated project removed afterwards |
| Documentation and hygiene | Check named README sections, OpenAPI, V1/V2 migrations, test files and `git diff --check` | Missing required artifact blocks handoff; H-1 outcome recorded without token value |

If the reviewer cannot fetch the revision, a required check fails, a test is skipped, or the clean checkout cannot start, record the exact failure and leave Delivery pending. Do not delete existing project volumes while testing or roll back a shared environment as part of this task.

Plan validation on 2026-09-26 (Windows PowerShell): `git diff --check` exit 0, local Markdown link check exit 0, and `git remote -v` showed a configured GitHub `origin`. Execution results are recorded in [TASK-013 evidence](../../quality/evidence/TASK-013.md).

## Handoff

- Changes: verified application revision `bb888a94584d6e6f95f1b43a2064409021fa249f` on branch `codex/req13-candidate-20260926`, followed by a documentation-only handoff commit
- Evidence: [TASK-013 evidence](../../quality/evidence/TASK-013.md)
- Decisions / open issues: A-08 location/access/revision; TASK-010 gates; TASK-011/H-1; release owner Delivery decision
- Next action: confirm published branch access, then obtain TASK-010 review, H-1 source-owner disposition and the release owner's Delivery decision
