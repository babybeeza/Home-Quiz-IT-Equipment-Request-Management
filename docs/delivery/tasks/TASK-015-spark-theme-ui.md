# TASK-015: Apply approved Spark Deck theme to the UI

Status: Implemented; Design, Implement, Verify and candidate Delivery approved for `5a3b2e1`
Owner: Frontend developer; code reviewer and QA / acceptance owner review the result
Requirement IDs: REQ-14; regression coverage for REQ-01–REQ-08 and AT-33
Dependencies: [TASK-014](TASK-014-spark-theme-design.md) approved ADR-008/token mapping; [REQ-14 Discover](../../product/req-14-theme-discovery.md)

## Human approvals

| Gate | Approver | Decision | Date / revision |
| --- | --- | --- | --- |
| Requirements | Project owner | Approved REQ-14 baseline | 2026-09-26 / `main` `5597476` |
| Design | Project owner acting as technical owner | Approved | 2026-09-26 / `5a3b2e1` |
| Implement | Project owner acting as code reviewer | Approved | 2026-09-26 / `5a3b2e1` |
| Verify | Project owner acting as QA / acceptance owner | Approved with stated limitations | 2026-09-26 / `5a3b2e1` |
| Delivery | Project owner acting as release owner | Approved for reviewed candidate with stated limitations | 2026-09-26 / `5a3b2e1` |

## Context

Read the [REQ-14 Discover](../../product/req-14-theme-discovery.md), TASK-014 design/evidence once approved, [current CSS](../../../frontend/src/app/styles.css), [UI flow](../../architecture/ui-flow.md), [acceptance cases](../../quality/acceptance-test-cases.md) and frontend manifests before editing. Work on a revision isolated from the already-running approved release stack.

## Scope / non-goals

Apply the approved semantic tokens across the shared header, list/search/actions, create/edit forms, request detail, dialogs and all loading/empty/error/conflict/status states. Preserve the user-provided source palette and record any approved derived shade. Keep responsive behavior and accessible status text/focus indicators.

No backend, API, schema, role/state workflow, logo, typography, layout redesign or dark mode changes. Do not add tests that only compare CSS literals; verify behavior and visible outcomes.

## Acceptance criteria

- [ ] On `/requests`, `/requests/new`, `/requests/{id}` and `/requests/{id}/edit`, desktop and mobile views visibly use the approved Spark Deck token mapping. No prior green theme color remains in active UI unless ADR-008 explicitly retains it.
- [ ] Header, table headers, actions, links, filters, form fields, dialogs, status pills and feedback states retain readable text and visible keyboard focus; actual foreground/background pairs meet ADR-008's WCAG 2.2 AA thresholds.
- [ ] Employee and Approver workflows, route navigation, list-row actions, validation messages, stale-conflict handling and status text remain unchanged in behavior. Backend role/ownership/state/version enforcement remains authoritative.
- [ ] No critical content is clipped at desktop and mobile widths; action controls and long Thai labels remain usable. Empty/loading/error/conflict states have visual evidence, not just the happy path.
- [ ] Frontend lint, typecheck, tests and build pass; the Playwright acceptance suite passes, including AT-33. Evidence records command, environment, exit code, counts and any NOT RUN checks.
- [ ] Implement/Verify reviewers record decisions for the exact revision. Runbook/README receive only changes needed to explain the theme or its limitations.

## Implementation plan

1. From approved ADR-008, define source and semantic CSS custom properties in `frontend/src/app/styles.css`; replace hardcoded colors systematically without changing component logic.
2. Check all route/state combinations and responsive widths; adjust focus, hover, disabled, alert and dialog pairings to the approved design. Keep the already running release stack untouched; use an isolated Compose project or the E2E runner.
3. Capture and inspect desktop/mobile screenshots for list, form, detail, dialog and feedback states. Record contrast calculations for the actual rendered pairs, then fix any failures.
4. Run the frontend checks and Playwright acceptance below; update traceability and evidence. Present the exact revision for human Implement/Verify and Delivery decisions.

## Verification

| Check | Command / method | Expected evidence |
| --- | --- | --- |
| Frontend quality | In `frontend/` with Node 24.15.0: `npm ci --no-audit --no-fund`, `npm run lint`, `npm run typecheck`, `npm test`, `npm run build` (host or the README's Node container) | Exit codes and test counts; no style-only snapshot assertions required |
| Browser regression | From repo root with Docker/Git Bash: `& 'C:\Program Files\Git\bin\bash.exe' tests/e2e/run-e2e.sh` | Playwright result/count, including AT-33; isolated stack cleaned by runner |
| Visual and accessibility | Inspect screenshots at about 1440×900 and 390×844 for routes/states above; keyboard Tab/focus; calculate contrast for rendered text/controls | Screenshots or review notes and contrast table, with failures fixed or stated |
| Scope and hygiene | `git diff --check`; `rg -n '#[0-9A-Fa-f]{3,8}|rgb\(|hsl\(' frontend/src --glob '*.css' --glob '*.tsx'`; review `git diff` | Remaining raw colors explained by ADR-008; no backend/contract edits |

Execution results are recorded in [TASK-015 evidence](../../quality/evidence/TASK-015.md). The user's subsequent “approved” reply approved the four gates explicitly listed in the handoff for application revision `5a3b2e1`; see the [approval record](../../governance/approvals.md).

## Handoff

- Changes: approved theme mapping applied to the shared UI stylesheet at application revision `5a3b2e1`
- Evidence: [TASK-015 evidence](../../quality/evidence/TASK-015.md)
- Decisions / open issues: the four listed gates approved for the candidate with stated limitations; REQ-14 Requirements baseline approved 2026-09-26
- Next action: [PR #12](https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management/pull/12) merged at `a69a950`; see [post-merge evidence](../../quality/evidence/TASK-015-postmerge.md). At the owner's request the local default-port `home-quiz-release` stack was redeployed from `main` `5597476`; see [release deploy evidence](../../quality/evidence/TASK-015-release-deploy.md).
