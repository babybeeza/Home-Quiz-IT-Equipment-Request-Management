# ADR-008: Spark Deck color theme for the light UI

Status: Proposed for technical-owner review
Date: 2026-09-26
Owner: Technical owner
Requirements/tasks: REQ-14 / TASK-014, TASK-015

## Context

The user supplied the Spark Deck color table in [REQ-14 Discover](../../product/req-14-theme-discovery.md). The app currently mixes green CSS custom properties with literal colors in the shared stylesheet. The request names colors, not a new logo, typography, layout or dark mode. The actual deck file was not supplied; the user's table is the source of truth for these hex values. Existing role, state, validation and accessibility behavior must remain unchanged.

## Options

1. Substitute each old literal with the nearest raw deck color. This is quick but makes orange, yellow, grey and template link colors hard to read on white and scatters decisions across selectors.
2. Preserve every source swatch, then map semantic roles to accessible foreground/background pairs and document the few derived neutrals and surfaces. This keeps the supplied identity visible while avoiding low-contrast combinations.
3. Change layout, typography and component structure with the colors. This exceeds the user's color request and raises regression risk.

## Proposed decision

Use option 2. Keep the 12 source colors as immutable `--spark-*` CSS variables. The `hlink` and `folHlink` values are template colors outside PowerPoint `THEME`; they remain separate variables. Map UI roles to semantic variables rather than use raw slots directly in selectors. No workflow or component logic changes.

| Source slot | Name | Hex | Intended UI role |
| --- | --- | --- | --- |
| dk1 | dark | `#000000` | Main text |
| lt1 | light | `#FFFFFF` | Page and card backgrounds |
| dk2 | grey_dark | `#797979` | Neutral source; not normal text on white |
| lt2 | grey_light | `#A9A9A9` | Decorative separators and cancelled status background |
| accent1 | blue | `#0050F0` | Primary action, links on light surfaces, focus |
| accent2 | orange | `#F68B1F` | Highlight with black text |
| accent3 | navy | `#002C63` | Headings, app header and table headers |
| accent4 | red_orange | `#F95922` | Accent source; avoid white normal text on it |
| accent5 | red | `#DA2010` | Danger action and error text |
| accent6 | yellow | `#FEC800` | Pending/warning background with black text |
| template hlink | — | `#65B2E8` | Header link on navy; outside `THEME` |
| template folHlink | — | `#1EB950` | Approved status background with black text; outside `THEME` |

Derived colors are limited to `#707070` (secondary text; the supplied `#797979` misses normal-text contrast on white), `#8F8F8F` (essential input/card border), `#F3F7FF` (subtle blue surface), `#FFF3F1` (error surface), `#FFF4E8` (conflict surface), `#8A4500` (conflict text), `#F2F2F2` (neutral pill), and `#A51B10` (danger hover). These are UI support colors, never claimed as raw deck swatches. The source red on pale error surface and the source blue on pale blue surface are readable without changing their hue. Do not render the supplied `hlink`/`folHlink` as normal text on white; ordinary content links use blue/navy, while the header can use hlink on navy.

| Semantic role / state | Foreground | Background or adjacent surface | CSS token intent |
| --- | --- | --- | --- |
| Body/card text | dark | light | `--text`, `--surface` |
| Secondary text | derived `#707070` | light | `--muted` |
| Heading / table header | navy / light | light / navy | `--heading`, `--table-head`, `--table-head-text` |
| Header brand link | template hlink | navy | `--header-link` |
| Header visited link | template folHlink | navy | `--header-link-visited` |
| Primary action / hover | light / light | blue / navy | `--primary`, `--primary-hover`, `--on-primary` |
| Secondary action | blue | light | `--secondary-ink`, `--surface` |
| Danger action / hover | light / light | red / derived `#A51B10` | `--danger`, `--danger-hover`, `--on-danger` |
| Content link / visited | blue / navy | light | `--link`, `--link-visited` |
| Input/card border, focus | derived `#8F8F8F` / blue | light | `--border`, `--focus` |
| Draft/cancelled pill | dark | derived `#F2F2F2` / grey_light | `--status-draft-bg`, `--status-cancelled-bg` |
| Pending pill | dark | yellow | `--status-pending-bg` |
| Approved pill | dark | template folHlink | `--status-approved-bg` |
| Rejected pill | light | red | `--status-rejected-bg` |
| Error message | red | derived `#FFF3F1` | `--error-ink`, `--error-bg` |
| Conflict message | derived `#8A4500` | derived `#FFF4E8` | `--conflict-ink`, `--conflict-bg` |
| Disabled/loading | Existing opacity/busy state plus text/status | Existing surface | Do not communicate state by color alone |

## Contrast verification

Use WCAG 2.2 AA thresholds: normal text 4.5:1, large text 3:1, and visual information needed to identify controls/states 3:1 ([text](https://www.w3.org/TR/wcag/#contrast-minimum), [non-text](https://www.w3.org/TR/wcag/#non-text-contrast)). For each sRGB channel `c` normalized to 0–1, linearize as `c/12.92` if `c ≤ 0.04045`, otherwise `((c+0.055)/1.055)^2.4`; luminance is `0.2126R + 0.7152G + 0.0722B`; contrast is `(Llighter+0.05)/(Ldarker+0.05)`. Rounded ratios below were calculated from the listed hex values. TASK-015 must recheck actual rendered pairs.

| Foreground / background | Ratio | Use / threshold |
| --- | ---: | --- |
| dark / light | 21.00:1 | Body text ≥4.5 |
| navy / light | 13.63:1 | Headings, visited links ≥4.5 |
| light / navy | 13.63:1 | Table/header text ≥4.5 |
| blue / light | 6.17:1 | Content link/secondary action ≥4.5 |
| light / blue | 6.17:1 | Primary action text ≥4.5 |
| light / red | 5.01:1 | Danger/rejected text ≥4.5 |
| light / `#A51B10` | 7.60:1 | Danger hover text ≥4.5 |
| red / `#FFF3F1` | 4.61:1 | Error text ≥4.5 |
| `#8A4500` / `#FFF4E8` | 6.60:1 | Conflict text ≥4.5 |
| `#707070` / light | 4.95:1 | Secondary text ≥4.5 |
| `#8F8F8F` / light | 3.23:1 | Essential border ≥3 |
| template hlink / navy | 5.90:1 | Header link ≥4.5 |
| template folHlink / navy | 5.27:1 | Header visited link ≥4.5 |
| dark / orange | 8.63:1 | Highlight text ≥4.5 |
| dark / yellow | 13.48:1 | Pending status text ≥4.5 |
| dark / template folHlink | 8.11:1 | Approved status text ≥4.5 |

The supplied grey_dark / white is 4.35:1, orange / white 2.43:1, yellow / white 1.56:1, hlink / white 2.31:1 and folHlink / white 2.59:1. They are not used as normal text on white. Button outlines and focus rings need to be checked against their actual adjacent color, not merely against the page.

## Consequences and verification

The app gains a coherent light theme with palette provenance and explicit support colors. A single stylesheet remains the owner of the theme. There is no backend, contract or data migration. TASK-015 must inspect desktop/mobile routes and loading/error/conflict/dialog states, verify rendered contrast, and run frontend and browser acceptance checks. The user/technical owner must review this proposed design; implementation evidence cannot stand in for that approval.
