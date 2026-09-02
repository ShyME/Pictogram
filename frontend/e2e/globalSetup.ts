const BASE_URL = process.env.PICTOGRAM_BASE_URL ?? 'http://localhost:8080';
const WARMUP_BUDGET_MS = 90_000;
const POLL_INTERVAL_MS = 500;

type Probe = {
  path: string;
  accept: string;
  expected: number;
  bodyIncludes?: string;
};

const probes: readonly Probe[] = [
  { path: '/', accept: 'text/html', expected: 200 },
  {
    path: '/actuator/health',
    accept: 'application/json',
    expected: 200,
    bodyIncludes: '"status":"UP"',
  },
];

async function waitForWarmProbe(probe: Probe): Promise<void> {
  const { path, accept, expected, bodyIncludes } = probe;
  const deadline = Date.now() + WARMUP_BUDGET_MS;
  let lastReason = 'no response yet';

  while (Date.now() < deadline) {
    try {
      const response = await fetch(new URL(path, BASE_URL), { headers: { Accept: accept } });
      const body = await response.text();
      if (response.status !== expected) {
        lastReason = `status ${response.status}, expected ${expected}`;
      } else if (bodyIncludes !== undefined && !body.includes(bodyIncludes)) {
        lastReason = `body did not contain ${bodyIncludes}`;
      } else {
        return;
      }
    } catch (error) {
      lastReason = error instanceof Error ? error.message : String(error);
    }
    await new Promise((resolve) => setTimeout(resolve, POLL_INTERVAL_MS));
  }

  throw new Error(
    `App at ${BASE_URL} did not warm up within ${WARMUP_BUDGET_MS}ms (${path}: ${lastReason})`,
  );
}

export default async function globalSetup(): Promise<void> {
  for (const probe of probes) {
    await waitForWarmProbe(probe);
  }
}
