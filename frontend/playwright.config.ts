import { defineConfig, devices } from "@playwright/test";

// Blackbox journeys run against the whole app in containers (`compose.yaml`, port 8080) —
// CI on `main` only, see .github/workflows/ci.yml. Point at a deployed stack with
// PICTOGRAM_BASE_URL.
const baseURL = process.env.PICTOGRAM_BASE_URL ?? "http://localhost:8080";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  // No retries — ADR-0007: flakiness is a defect, not a retry target.
  retries: 0,
  reporter: process.env.CI ? [["github"], ["html", { open: "never" }]] : "list",
  use: {
    baseURL,
    trace: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
