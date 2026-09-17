import { initSentry } from '@app/sentry';
import * as Sentry from '@sentry/react';
import { afterEach, expect, test, vi } from 'vitest';

vi.mock('@sentry/react', () => ({ init: vi.fn() }));

afterEach(() => {
  vi.unstubAllEnvs();
  vi.mocked(Sentry.init).mockClear();
});

test('initializes Sentry with the build-time DSN, tagged production', () => {
  vi.stubEnv('VITE_SENTRY_DSN', 'https://examplePublicKey@o0.ingest.sentry.io/0');

  initSentry();

  expect(Sentry.init).toHaveBeenCalledWith({
    dsn: 'https://examplePublicKey@o0.ingest.sentry.io/0',
    environment: 'production',
  });
});

test('does nothing when no DSN is configured (local dev and CI)', () => {
  vi.stubEnv('VITE_SENTRY_DSN', '');

  initSentry();

  expect(Sentry.init).not.toHaveBeenCalled();
});
