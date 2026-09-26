# Evidence: TASK-013 REQ-13 published release candidate

Date / operator: 2026-09-26 / Codex
Candidate branch: `codex/req13-candidate-20260926` at `https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management.git` (separate worktree; handoff commits add documentation only)
Application revision tested: `bb888a94584d6e6f95f1b43a2064409021fa249f`
Candidate worktree: `E:\wp\Home-Quiz-IT-Equipment-Request-Management-candidate`
Clean verification checkout: detached worktree at `E:\wp\Home-Quiz-IT-Equipment-Request-Management-verify`, outside the active `main` workspace
Environment: Windows / PowerShell, Docker 29.5.2, Compose 5.1.3, Temurin 25.0.3 targeting Java 21, Node 24.15.0-alpine, PostgreSQL 17 and Redis 8 in isolated containers

## Result and scope

The candidate contains the source, root README, Flyway V1/V2, OpenAPI and automated tests. The README has Environment, Run, Test, API, Decisions, Assumptions and Known limitations sections. Verification used a separate checkout and the Compose project `home-quiz-req13` with host ports 55433/56380/58010/53010; it did not use or remove the existing `home-quiz` volumes.

| Check | Command / method | Exit code | Result | Observation |
| --- | --- | --- | --- | --- |
| Candidate checkout | `git worktree add --detach <verify-path> bb888a9`; `git rev-parse HEAD`; `git status --short` | 0 | PASS | Exact commit checked out with no tracked or untracked changes before checks |
| Mandatory artifact inventory | `Test-Path` for source trees, README, Flyway V1/V2, OpenAPI, E2E specs and Compose; `rg` for README headings | 0 | PASS | All named paths present; required README areas found |
| Config parse | `docker compose -f <verify>/compose.yaml -p home-quiz-req13 --profile app config --quiet` | 0 | PASS | Single Compose app profile parsed |
| SSH remote read | `git ls-remote origin HEAD` with noninteractive SSH | 128 | FAIL | `Permission denied (publickey)` from this machine; this does not establish reviewer access |
| Initial HTTPS remote read | `git ls-remote https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management.git HEAD` | 0 | PASS for remote read | Before publication, remote default HEAD was `8a09c4d`; this was a baseline observation |
| First frontend attempt | `docker run ... node:24.15.0-alpine ...` | 1 | NOT RUN | Docker Desktop engine was stopped; launched it and retried the same checks |
| Backend build and tests | In clean `backend/`: `.\mvnw.cmd --batch-mode clean package` | 0 | PASS | BUILD SUCCESS; 15 Surefire XML reports sum to 129 tests, 0 failures, 0 errors, 0 skipped. Testcontainers used PostgreSQL and Redis |
| Frontend lint/typecheck/tests/build | Node 24.15.0-alpine container: `npm ci --no-audit --no-fund && npm run lint && npm run typecheck && npm test && npm run build` | 0 | PASS | Lint and typecheck passed; 4 Vitest files, 32/32 tests passed; Next.js production build succeeded |
| Browser acceptance | Git Bash: `tests/e2e/run-e2e.sh` from the clean checkout | 0 | PASS | 50/50 Playwright Chromium tests, including two AT-33 cases; isolated E2E project removed by runner |
| Clean Docker app start | `docker compose -p home-quiz-req13 --profile app up -d --build --wait` with host ports 55433/56380/58010/53010 | 0 | PASS | PostgreSQL, Redis, backend and frontend all healthy; Flyway applied schema in a new volume |
| Smoke through frontend proxy | PowerShell HTTP: Employee create → submit; Approver approve → filtered list | 0 | PASS | 201 DRAFT v0, 200 PENDING, 200 APPROVED, 200 list containing the request |
| Isolated cleanup | Verify Compose project labels/volume names, then `docker compose -p home-quiz-req13 --profile app down -v` | 0 | PASS | Removed only the `home-quiz-req13` app containers, network and two test volumes |
| HTTPS branch push dry run | `git push --dry-run https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management.git HEAD:refs/heads/codex/req13-candidate-20260926` | 0 | PASS | New branch would be accepted; publication and independent reviewer fetch are separate checks |
| Branch publication | `git push https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management.git HEAD:refs/heads/codex/req13-candidate-20260926` | 0 | PASS | Created review branch at documentation commit `f389e64e27f0d20115fa744184e3087673bba491` |
| Reviewer access probe | `git -c credential.helper= ls-remote https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management.git refs/heads/codex/req13-candidate-20260926`, with terminal prompts disabled | 0 | PASS | Unauthenticated HTTPS read resolved the subsequent published documentation commit `0cde7b8450ef6916ab3d3dbd6be5122185258386`; actual reviewer clone remains unobserved |
| Independent handoff clone | `git -c credential.helper= clone --branch codex/req13-candidate-20260926 --single-branch https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management.git E:\wp\Home-Quiz-IT-Equipment-Request-Management-reviewer-check`, with prompts disabled; `git rev-parse HEAD`, `git status --short`, artifact `Test-Path` | 0 | PASS | Fresh clone resolved `236a2b3e32cce85f820366105fbe960e752dc789`, was clean and contained README, Compose, OpenAPI, V1 migration, E2E runner and TASK-013 evidence. This is an access rehearsal by Codex, not a reviewer sign-off |
| Manual P2 AT-44/AT-46; application rollback/data restore | Manual cases and runbook operations | N/A | NOT RUN | No new rehearsal in this task; limitations remain explicit |

## Delivery status at candidate verification (before the human decision below)

The application revision passed clean-checkout automation and smoke. Handoff commits add evidence and status documentation without changing application code or manifests. The candidate branch is published and an independent clone succeeded. TASK-010 Design/Implement/Verify gates await human review. For TASK-011/H-1, the source owner confirmed on 2026-09-26 that the token is revoked; a post-confirmation workspace GET of the saved link returned HTTP 403. The URL text remains in Git history. A-08's owner decision and the release owner's Delivery decision are pending. No approval is inferred from passing tests.

REQ-13 mandatory source, README, migration/schema and automated tests are present. The optional whole-system Compose path is present and was rehearsed; an optional Postman/Bruno API collection is absent (OpenAPI is present). No cloud deployment is claimed.

## Deliver and learn audit

2026-09-26: Compared the published application revision, [acceptance cases](../acceptance-test-cases.md), [approval record](../../governance/approvals.md), [runbook](../../operations/runbook.md) and this evidence. All 43 P1 cases have automated coverage in the 50/50 Playwright run, including AT-33; that result does not replace TASK-010 human Design/Implement/Verify review. The historical Verify approval covers TASK-001–009 only. The runbook previously said no candidate revision or target had been named; it now identifies the published branch, tested application revision and the release-owner decision still required. Manual P2 AT-44/AT-46 and application rollback/data restore remain NOT RUN.

Follow-up owners: the TASK-010 technical/code/QA reviewers decide the changed gates; the TASK-011 source owner has supplied the revocation decision, recorded in [TASK-011 evidence](TASK-011.md); the release owner accepts the handoff location and exact final commit under A-08, weighs residual Git history and the unrun manual/recovery checks, and records Delivery in [approvals](../../governance/approvals.md). The [TASK-013 packet](../../delivery/tasks/TASK-013-req13-release-handoff.md) tracks those decisions. No Delivery decision was supplied in this audit.

The repeated delivery check added an independent anonymous clone rehearsal; the earlier `ls-remote` probe alone did not prove that the branch contents could be fetched. The remaining owner decisions are unchanged, and this rehearsal does not approve any gate.

## Post-merge delivery check, 2026-09-26

GitHub [PR #10](https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management/pull/10) merged candidate head `642dc12f04b8b408e24d7104b61e01c119cfd315` into `main` at `96acc6411133ecccd3261c53473dc59f2ba1d1fc` (`2026-09-26T03:06:50Z`; merged by repository owner `babybeeza`). A noninteractive HTTPS `ls-remote` confirmed `main` at that merge commit. The GitHub PR API reported no submitted reviews and an empty status-check rollup; `gh pr checks 10` exited 1 with “no checks reported.” These observations do not undo the local clean-checkout results above, and the merge alone does not record Design/Implement/Verify or Delivery approval under the repository playbook. H-1 source-owner revocation confirmation is recorded in [TASK-011 evidence](TASK-011.md). The release owner still needs to decide the gates and accept the exact merged revision and residual limitations. No new application checks or manual P2/rollback/restore rehearsal were run in this documentation-only audit.

## Human gate decision, 2026-09-26

After [PR #11](https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management/pull/11) merged the post-merge documentation to `main` at `77b56241a9eef817c62e34d4e949bfc6ecfdc9f7`, the user explicitly answered “Approve all listed gates” to a question naming TASK-010 Design, Implement, Verify and Delivery for that revision. The question also disclosed that manual P2 AT-44/AT-46 and application rollback/data restore were NOT RUN and that revoked token text remains in Git history. The [approval record](../../governance/approvals.md) records the user in each owner role. This is a human decision about the verified application and named revision; no additional application tests were run for these documentation-only changes. The unrun checks remain limitations, not Pass results.
