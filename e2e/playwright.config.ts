import { defineConfig, devices } from '@playwright/test'

/**
 * End-to-end tests against the whole app running in Docker
 * (`docker compose --profile app up -d --build`, then `npm test` in this folder).
 *
 * E2E_BASE_URL     where the app is (default http://localhost:3000)
 * E2E_BROWSER_CHANNEL  e.g. "msedge" to use an installed browser instead of downloading Chromium
 */
const isCI = !!process.env.CI

export default defineConfig({
  testDir: './tests',
  // Every test registers its own users, so tests share no data and can run in parallel.
  fullyParallel: true,
  forbidOnly: isCI,
  // One retry in CI absorbs a rare slow container; a test that only passes on retry is reported
  // as "flaky", so it is visible instead of hidden.
  retries: isCI ? 1 : 0,
  workers: isCI ? 2 : undefined,
  reporter: isCI
    ? [['list'], ['html', { open: 'never' }]]
    : [['list'], ['html', { open: 'on-failure' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:3000',
    // On failure: a trace (DOM snapshots, network, console) to replay what happened.
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        channel: process.env.E2E_BROWSER_CHANNEL || undefined,
      },
    },
  ],
})
