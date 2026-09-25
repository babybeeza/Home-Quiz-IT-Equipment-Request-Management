# Playwright acceptance suite (TASK-008)

Automates [docs/quality/acceptance-test-cases.md](../../docs/quality/acceptance-test-cases.md) end to end through a real browser. Every test title starts with its `AT-xx` ID. 53 of the 55 cases are automated. AT-44 (backend down) and AT-46 (Redis down) stay manual because they require stopping services mid-test.

Pinned: `@playwright/test` 1.63.0 (lockfile) running in `mcr.microsoft.com/playwright:v1.63.0-noble`, Chromium, locale `th-TH`, timezone Asia/Bangkok. No Node or browser install is needed on the host.

## Run

From the repository root. **Only Docker is required**: the backend and frontend are built as images from `compose.yaml` (profile `e2e`, TASK-009).

```bash
bash tests/e2e/run-e2e.sh                  # all tests
bash tests/e2e/run-e2e.sh -g "AT-2"        # filter by title
KEEP_STACK=1 bash tests/e2e/run-e2e.sh     # keep the stack running afterwards for inspection
```

The script:
1. builds the images
2. runs the `e2e` service as the separate Compose project `home-quiz-e2e`, on ports 55432/56379/58080/53000 (override with `E2E_*_PORT`), so it never touches the development database
3. Compose starts PostgreSQL, Redis, backend, frontend and the one-shot `seed` in dependency order
4. Playwright runs inside the same Docker network against `http://frontend:3000`; the API cases hit `http://backend:8080` directly
5. the whole project and its volumes are removed afterwards

Outputs, all git-ignored: `playwright-report/index.html` (HTML report with traces and screenshots of failures) and `results/results.json`.

## Conventions

- **Isolation:** each test creates its own requests through the API, with a unique `tag()` in the title, and asserts through the UI. Tests run with one worker because they share a database.
- **Identity:** `signInAs` sets the same localStorage key the header selector uses, then reloads, which avoids racing React hydration. AT-04 tests the selector itself.
- **List reads** go through `listUpdate`, which waits for the matching response and for the placeholder rows to disappear. The app deliberately keeps the previous page visible while it loads the next.
- **Missing seed data:** if the seed is missing, section F is **skipped with a reason**. Report a skipped case as NOT RUN, never as passed.
