# Evidence: TASK-014 Spark Deck theme design

Date / operator: 2026-09-26 / Codex
Revision: worktree branch `codex/req14-spark-theme-20260926`, based on `717bbfb` (uncommitted during these checks)
Environment: Windows PowerShell, Docker 29.5.2, `node:24.15.0-alpine`

## Result

[ADR-008](../../architecture/decisions/ADR-008-spark-theme.md) preserves the 12 source colors from the user's table, keeps template `hlink`/`folHlink` outside PowerPoint `THEME`, and maps UI roles and states to semantic tokens. Derived neutrals and surfaces are named separately; no deck file was inspected. This is a proposed Design decision pending technical-owner review.

| Check | Command / method | Exit code | Result |
| --- | --- | --- | --- |
| Current color inventory | `rg -n '#[0-9A-Fa-f]{3,8}|rgb\(|hsl\(' frontend/src --glob '*.css' --glob '*.tsx'` before editing | 0 | Green tokens and literal colors were concentrated in `frontend/src/app/styles.css`; no component-local hex colors found |
| Palette and contrast | `docker run --rm -v "${PWD}:/workspace" -w /workspace node:24.15.0-alpine node tests/accessibility/check-theme-contrast.mjs` | 0 | 12/12 source colors preserved; 20 semantic foreground/background pairs passed specified thresholds; 0 failures |
| Document links | Local Markdown relative-link check across eight edited `.md` files | 0 | All linked local paths resolved |
| Patch whitespace | `git diff --check` | 0 | No whitespace errors |

The contrast script reads CSS tokens and computes sRGB relative luminance using the formula in ADR-008. It checks the declared pairs; rendered hover/focus and responsive UI still require TASK-015 visual inspection. Design approval is **pending**. No approval is inferred from this calculation or from the user's Implement instruction.
