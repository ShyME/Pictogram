import { defineConfig, devices } from '@playwright/test';

const baseURL = process.env.PICTOGRAM_BASE_URL ?? 'http://localhost:8080';

export default defineConfig({
  testDir: './e2e',
  globalSetup: './e2e/globalSetup.ts',
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: 0,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL,
    trace: 'retain-on-failure',
    navigationTimeout: 15_000,
    actionTimeout: 10_000,
  },
  expect: { timeout: 10_000 },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
