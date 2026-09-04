import { defineConfig, devices } from '@playwright/test';

// Visual-regression suite (ADR-0013). Separate from the end-to-end journeys: it drives
// a bare `vite` dev server with no backend — only the token-only screens (`/ui`, `/login`)
// are snapshotted, and `/api/**` is stubbed per-test. Baselines are generated in the
// Playwright CI image (see .github/workflows/ci.yml) so font/AA rendering is stable;
// `retries: 0` holds (ADR-0007).

const PORT = 4174;

export default defineConfig({
  testDir: './visual',
  testMatch: /.*\.visual\.ts$/,
  snapshotDir: './visual/__screenshots__',
  workers: 1,
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: 0,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',
  webServer: {
    command: `pnpm exec vite --port ${PORT.toString()} --strictPort`,
    port: PORT,
    reuseExistingServer: !process.env.CI,
    stdout: 'ignore',
    stderr: 'pipe',
  },
  use: {
    baseURL: `http://localhost:${PORT.toString()}`,
    trace: 'retain-on-failure',
  },
  expect: {
    toHaveScreenshot: {
      animations: 'disabled',
      // AA rounding varies slightly even within one platform; the CI image is the source
      // of truth, this only absorbs sub-pixel noise on a re-run there.
      maxDiffPixelRatio: 0.01,
    },
  },
  projects: [
    {
      name: 'mobile',
      use: { ...devices['Desktop Chrome'], viewport: { width: 375, height: 812 } },
    },
    {
      name: 'desktop',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } },
    },
  ],
});
