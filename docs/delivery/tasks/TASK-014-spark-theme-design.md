# TASK-014: Design Spark Deck color theme

Status: Design approved for application revision `5a3b2e1`
Owner: Frontend developer prepares design; project owner acting as technical owner reviews it
Requirement IDs: REQ-14
Dependencies: [REQ-14 Discover](../../product/req-14-theme-discovery.md), [A-09](../../product/assumptions.md), current [UI flow](../../architecture/ui-flow.md)

## Human approvals

| Gate | Approver | Decision | Date / revision |
| --- | --- | --- | --- |
| Requirements | Project owner | Approved REQ-14 baseline | 2026-09-26 / `main` `5597476` |
| Design | Project owner acting as technical owner | Approved | 2026-09-26 / `5a3b2e1` |
| Implement | Project owner acting as code reviewer | Approved for TASK-015 | 2026-09-26 / `5a3b2e1` |
| Verify | Project owner acting as QA / acceptance owner | Approved with stated limitations for TASK-015 | 2026-09-26 / `5a3b2e1` |
| Delivery | Project owner acting as release owner | Approved for reviewed candidate with stated limitations | 2026-09-26 / `5a3b2e1` |

## Context

Read the [user-supplied palette and acceptance criteria](../../product/req-14-theme-discovery.md), [requirements](../../product/requirements.md), [current CSS](../../../frontend/src/app/styles.css), [UI flow](../../architecture/ui-flow.md) and [approval record](../../governance/approvals.md). The existing app is running from a separate approved checkout; design work does not alter its behavior.

## Scope / non-goals

Create `docs/architecture/decisions/ADR-008-spark-theme.md` with the source palette, semantic tokens, usage matrix and explicit contrast decisions for light-theme UI. Cover primary/secondary/danger actions, headings, body/secondary text, links, borders, focus, table headers, status pills, error/conflict/success, disabled and loading states. Treat `hlink` and `folHlink` as template colors outside `THEME`. Decide whether low-contrast source colors need a different background or a named derived shade, and document every deviation.

Do not change CSS, layout, typography, logo, application behavior, API or backend in this task. Do not claim that an actual deck file was inspected; the user's hex table is the source.

## Acceptance criteria

- [ ] ADR-008 preserves all 12 user-provided hex values and their original slot/name mapping; `blue` is the primary brand color, `navy` is used for headings/table headers, and `dark`/`light` remain main text/background unless the design records a reasoned exception.
- [ ] Every UI role listed in scope maps to a semantic token and foreground/background pair. Hover, focus, disabled, error and conflict states are included so implementation does not choose colors ad hoc.
- [ ] A reproducible contrast table records the exact color pairs and ratios. Normal text ≥4.5:1, large text ≥3:1, and essential UI indicators ≥3:1 under WCAG 2.2 AA; failures are changed in the design before approval.
- [ ] Use of source hex versus derived shade is marked explicitly. Orange/yellow and template link colors are not assigned as normal text on white without an accessible pair.
- [ ] Technical owner records Design approval or requested changes with the ADR revision. Unresolved product choices remain in [assumptions](../../product/assumptions.md).

## Implementation plan

1. Inventory every existing color declaration in `frontend/src/app/styles.css` and group by semantic role, including mobile and action/dialog states.
2. Draft ADR-008 with palette, token naming, interaction-state matrix and accessible pairings. Keep the unmodified source hex in one section and any derived values separately.
3. Calculate contrast ratios from sRGB hex values with a repeatable script or documented formula; review proposed pairs against the actual UI surfaces.
4. Record Design evidence and obtain the technical owner's decision before TASK-015 changes CSS.

## Verification

| Check | Command / method | Expected evidence |
| --- | --- | --- |
| Palette completeness | Compare ADR-008 with 12 rows in [REQ-14 Discover](../../product/req-14-theme-discovery.md) | 12 exact source values; `hlink`/`folHlink` provenance retained |
| Color inventory | `rg -n '#[0-9A-Fa-f]{3,8}|rgb\(|hsl\(' frontend/src --glob '*.css' --glob '*.tsx'` | Every existing color has a proposed semantic role or an explicit exception |
| Contrast | Reproducible sRGB relative-luminance calculation for each proposed foreground/background pair | Ratio and threshold recorded; failures corrected before Design approval |
| Documentation | Local Markdown link check and `git diff --check` | Exit 0, with command/environment recorded in `docs/quality/evidence/TASK-014-design.md` |

Design check results are recorded in [TASK-014 evidence](../../quality/evidence/TASK-014-design.md). This task does not claim application readiness or a human Design decision.

## Handoff

- Changes: [ADR-008](../../architecture/decisions/ADR-008-spark-theme.md) proposes semantic mapping and derived shades
- Evidence: [TASK-014 design evidence](../../quality/evidence/TASK-014-design.md)
- Decisions / open issues: Design approval recorded in [governance](../../governance/approvals.md); REQ-14 Requirements baseline approved 2026-09-26
- Next action: preserve the approved application revision during PR review and integration
