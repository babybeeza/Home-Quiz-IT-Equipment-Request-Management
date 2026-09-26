# Evidence: TASK-015 Spark Deck UI theme

Date / operator: 2026-09-26 / Codex
Revision: `codex/req14-spark-theme-20260926`, based on `717bbfb`; review the branch commit named in the handoff
Environment: Windows PowerShell, Docker 29.5.2, Node 24.15.0 Alpine container, Chrome browser. Host Node/npm were unavailable. The existing `home-quiz-release` Compose stack was left running; theme visual QA used isolated `home-quiz-theme` on frontend port 53011 and backend port 58011, with separate database and Redis volumes.

## Change and checks

[ADR-008](../../architecture/decisions/ADR-008-spark-theme.md) proposes the mapping. The shared [stylesheet](../../../frontend/src/app/styles.css) now declares all 12 exact source swatches, semantic tokens and named support colors. It applies navy headings/table headers, blue primary actions and focus, status colors with text labels, and readable error/conflict styling. Component logic, backend, API and schema were not changed.

| Check | Command / method | Exit | Result |
| --- | --- | ---: | --- |
| Source palette and contrast | `docker run --rm -v "${PWD}:/workspace" -w /workspace node:24.15.0-alpine node tests/accessibility/check-theme-contrast.mjs` | 0 | 12/12 source hex values matched; 20 declared foreground/background pairs met 4.5:1 text or 3:1 essential UI thresholds; 0 failures |
| Frontend quality | `docker run --rm -v "${PWD}:/workspace" -w /workspace/frontend node:24.15.0-alpine sh -c "npm ci --no-audit --no-fund && npm run lint && npm run typecheck && npm test && npm run build"` | 0 | Dependency install, lint, typecheck, 32/32 unit tests and Next.js production build passed |
| Browser acceptance | `& 'C:\Program Files\Git\bin\bash.exe' tests/e2e/run-e2e.sh` | 0 | 50/50 Playwright Chromium tests passed, including AT-33; runner's isolated `home-quiz-e2e` stack cleaned itself |
| Theme stack smoke | `docker compose -p home-quiz-theme --profile app up -d --build --wait` with ports 55434/56381/58011/53011 | 0 | Four services healthy; used only for visual QA |
| CSS inventory | `rg -n '#[0-9A-Fa-f]{3,8}|rgb\(|hsl\(' frontend/src --glob '*.css' --glob '*.tsx'` | 0 | Literal hex values confined to the source/support token declarations; two navy RGB alpha values are shadows; no component-local color literals |
| Patch whitespace and Markdown links | `git diff --check`; local Markdown relative-link check across eight edited `.md` files | 0 / 0 | No whitespace errors; all local links resolved |

## Browser visual review

Chrome was inspected at 1440×900 and 390×844. The list, new form, draft detail, pending detail, approver reject dialog and validation errors retained readable Thai labels and visible blue keyboard focus. The populated list showed DRAFT, PENDING, APPROVED, REJECTED and CANCELLED with text inside each pill. Empty and loading list states were observed. The browser's computed styles confirmed navy `rgb(0, 44, 99)` header, white header text, template hlink `rgb(101, 178, 232)`, pending yellow `rgb(254, 200, 0)` with black text, and red danger button `rgb(218, 32, 16)` with white text. All declared pairs were calculated by the linked contrast script; this browser sampling corroborated the main visible pairs.

The list table retains its pre-existing horizontal scroll at 1440 px and mobile width; no columns or controls were clipped inside the scroll region. Theme QA created five sample requests only in the isolated theme database. No visual change was made to the approved release stack.

## Limits and review

Backend unit/integration suites and manual AT-44/AT-46 were **NOT RUN** for this CSS-only change; the E2E runner rebuilt the backend and exercised its workflows. A scripted, exhaustive rendered-pixel audit of every hover/disabled/loading/conflict combination was **NOT RUN**; the token checker covered declared pairs and targeted browser review covered main routes and states. The original Spark Deck file was not supplied; the user-provided hex table is the source. The user subsequently approved Design, Implement, Verify and candidate Delivery for application revision `5a3b2e1` with these limitations; see the [approval record](../../governance/approvals.md).
