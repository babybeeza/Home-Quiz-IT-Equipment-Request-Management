# Playwright acceptance suite (TASK-008)

Automates [docs/quality/acceptance-test-cases.md](../../docs/quality/acceptance-test-cases.md) end to end through a real browser. Every test title starts with its `AT-xx` ID. 53 of the 55 cases are automated. AT-44 (backend down) and AT-46 (Redis down) stay manual because they require stopping services mid-test.

Pinned: `@playwright/test` 1.63.0 (lockfile) running in `mcr.microsoft.com/playwright:v1.63.0-noble`, Chromium, locale `th-TH`, timezone Asia/Bangkok. No Node or browser install is needed on the host.

## Run

From the repository root, with Docker running and the backend jar built (`cd backend; .\mvnw.cmd package`):

```bash
bash tests/e2e/run-e2e.sh                  # all tests
bash tests/e2e/run-e2e.sh -g "AT-2"        # filter by title
```

The script:
1. starts Compose PostgreSQL/Redis and loads the [seed dataset](../performance/seed-search-dataset.sql) that section F needs
2. starts the backend jar on `:8080`, allowing CORS from `http://host.docker.internal:3200`
3. builds and starts the frontend (`next build && next start`) on `:3200`, with `NEXT_PUBLIC_API_BASE_URL=http://host.docker.internal:8080/api/v1`
4. runs Playwright in its container, where the browser reaches the host through `host.docker.internal`
5. stops the backend and the frontend container, and exits with the Playwright status

Change the frontend port with `E2E_FRONTEND_PORT`. Outputs, all git-ignored:
- `playwright-report/index.html`: HTML report with traces and screenshots of failures
- `results/results.json`
- `results/backend.log`

## Conventions

- **Isolation:** each test creates its own requests through the API, with a unique `tag()` in the title, and asserts through the UI. Tests run with one worker because they share a database.
- **Identity:** `signInAs` sets the same localStorage key the header selector uses, then reloads, which avoids racing React hydration. AT-04 tests the selector itself.
- **List reads** go through `listUpdate`, which waits for the matching response and for the placeholder rows to disappear. The app deliberately keeps the previous page visible while it loads the next.
- **Missing seed data:** if the seed is missing, section F is **skipped with a reason**. Report a skipped case as NOT RUN, never as passed.
