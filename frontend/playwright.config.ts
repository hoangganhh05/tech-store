import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  timeout: 30_000,
  expect: { timeout: 7_000 },
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: process.env.CI
    ? [
        ["list"],
        ["junit", { outputFile: "test-results/e2e-junit.xml" }],
        ["html", { outputFolder: "playwright-report", open: "never" }],
      ]
    : "list",
  use: {
    baseURL: "http://127.0.0.1:4173",
    // CI uses the installed Playwright Chromium. A locally installed Chrome/Edge
    // channel can be selected explicitly when browser download is unavailable.
    channel: process.env.PLAYWRIGHT_BROWSER_CHANNEL as
      | "chrome"
      | "msedge"
      | undefined,
    trace: "on-first-retry",
    screenshot: "only-on-failure",
  },
  webServer: {
    command: "npm run dev -- --host 127.0.0.1 --port 4173",
    url: "http://127.0.0.1:4173",
    reuseExistingServer: !process.env.CI,
    timeout: 30_000,
    env: { VITE_API_BASE_URL: "/api/v1" },
  },
});
