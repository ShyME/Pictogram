const BASE_URL = process.env.PICTOGRAM_BASE_URL ?? "http://localhost:8080";
const WARMUP_BUDGET_MS = 90_000;
const POLL_INTERVAL_MS = 500;

const probes: ReadonlyArray<{ path: string; accept: string; expected: number }> = [
  { path: "/", accept: "text/html", expected: 200 },
  { path: "/api/profiles/warmup_probe", accept: "application/json", expected: 404 },
];

async function waitForWarmProbe(path: string, accept: string, expected: number): Promise<void> {
  const deadline = Date.now() + WARMUP_BUDGET_MS;
  let lastReason = "no response yet";

  while (Date.now() < deadline) {
    try {
      const response = await fetch(new URL(path, BASE_URL), { headers: { Accept: accept } });
      if (response.status === expected) return;
      lastReason = `status ${response.status}, expected ${expected}`;
    } catch (error) {
      lastReason = error instanceof Error ? error.message : String(error);
    }
    await new Promise((resolve) => setTimeout(resolve, POLL_INTERVAL_MS));
  }

  throw new Error(`App at ${BASE_URL} did not warm up within ${WARMUP_BUDGET_MS}ms (${path}: ${lastReason})`);
}

export default async function globalSetup(): Promise<void> {
  for (const probe of probes) {
    await waitForWarmProbe(probe.path, probe.accept, probe.expected);
  }
}
