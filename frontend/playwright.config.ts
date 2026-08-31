import { defineConfig, devices } from "@playwright/test";

// Blackbox journeys run against the whole app in containers (`compose.yaml`, port 8080) —
// CI on `main` only, see .github/workflows/ci.yml. Point at a deployed stack with
// PICTOGRAM_BASE_URL.
const baseURL = process.env.PICTOGRAM_BASE_URL ?? "http://localhost:8080";

export default defineConfig({
  testDir: "./e2e",
  // Blocks until the app answers a real request, not just the container healthcheck —
  // the first request after `compose up --build` is slow (JIT/JPA warm-up).
  globalSetup: "./e2e/global-setup.ts",
  // One worker: the journeys share one stateful mock-oauth2-server, whose interactive
  // login keeps a single "who is signed in" state — two concurrent auth-code flows race
  // and one browser comes back with the other's identity.
  workers: 1,
  forbidOnly: !!process.env.CI,
  // No retries — ADR-0007: flakiness is a defect, not a retry target.
  retries: 0,
  reporter: process.env.CI ? [["github"], ["html", { open: "never" }]] : "list",
  use: {
    baseURL,
    trace: "retain-on-failure",
    // Generous but bounded: a containerised full-stack round-trip is legitimately slower
    // than an in-process one. Warm-up is handled by globalSetup; this is headroom, not a retry.
    navigationTimeout: 15_000,
    actionTimeout: 10_000,
  },
  expect: { timeout: 10_000 },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
