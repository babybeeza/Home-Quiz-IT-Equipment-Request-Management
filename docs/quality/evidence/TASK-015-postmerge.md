# Evidence: REQ-14 post-merge handoff

Date / operator: 2026-09-26 / Codex
Environment: Windows PowerShell, GitHub CLI, Docker 29.5.2, `node:24.15.0-alpine`

[PR #12](https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management/pull/12) merged into GitHub `main` on 2026-09-26 at `a69a9506f3b42472752e135e6c375ab971a1ffd7`. Its head was `90566576a67db7f6cdd3dbe82f71a5e3bbda2f58`, containing the approved application revision `5a3b2e107e30ba90f1b36e5ea1b2f587c955c4c9` plus a documentation-only approval record. The user's “merge แล้ว” message reported the merge; GitHub PR metadata and fetched `origin/main` independently confirmed it.

| Check | Command / method | Exit | Result |
| --- | --- | ---: | --- |
| Merge status | `gh pr view 12 --json state,mergedAt,mergeCommit,headRefOid` | 0 | `MERGED`; merge `a69a950`; head `9056657` |
| Revision ancestry | `git merge-base --is-ancestor 5a3b2e1 origin/main`; same for `9056657` | 0 / 0 | Approved UI and approval record are in merged `main` |
| Application source | `git diff 5a3b2e1 a69a950 -- frontend/src/app/styles.css tests/accessibility/check-theme-contrast.mjs` | 0 | No difference in theme stylesheet or checker after the approved UI revision |
| Merged contrast check | `docker run --rm -v "${PWD}:/workspace" -w /workspace node:24.15.0-alpine node tests/accessibility/check-theme-contrast.mjs` | 0 | 12 source colors and 20 contrast pairs; 0 failures |
| Running services | `docker compose -p home-quiz-release --profile app ps --format json`; same for `home-quiz-theme` | 0 / 0 | Both isolated stacks have four healthy services |

The `home-quiz-release` stack at default ports 3000/8080 was started from the earlier release checkout and has **not** been rebuilt or deployed from merged `main`. The theme preview remains available in isolated `home-quiz-theme` on frontend port 53011. These checks establish GitHub integration and source identity, not deployment of the merged theme on default ports. Frontend and Playwright results for the approved application revision remain in [TASK-015 evidence](TASK-015.md); they were **NOT RERUN** after merge because the theme source is unchanged. The original unrun checks and formal REQ-14 Requirements baseline status remain as recorded there and in [approvals](../../governance/approvals.md).
