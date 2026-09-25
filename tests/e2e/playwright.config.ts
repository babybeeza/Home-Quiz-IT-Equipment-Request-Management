import { defineConfig, devices } from "@playwright/test";

// Acceptance suite for docs/quality/acceptance-test-cases.md (TASK-008).
// Runs against an already started stack; see tests/e2e/README.md.
export default defineConfig({
  testDir: "./specs",
  // One worker: every spec shares one database, and list/pagination totals must not race with other specs.
  workers: 1,
  fullyParallel: false,
  retries: 0,
  timeout: 30_000,
  expect: { timeout: 10_000 },
  reporter: [["list"], ["html", { open: "never", outputFolder: "playwright-report" }], ["json", { outputFile: "results/results.json" }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://host.docker.internal:3200",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    locale: "th-TH",
    timezoneId: "Asia/Bangkok",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
