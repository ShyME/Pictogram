import { SessionExpiredError, throwIfSessionExpired } from '@shared';
import { expect, test } from 'vitest';

test('throwIfSessionExpired throws SessionExpiredError on a 401', () => {
  expect(() => throwIfSessionExpired(new Response(null, { status: 401 }))).toThrow(
    SessionExpiredError,
  );
});

test('throwIfSessionExpired is a no-op for any other status', () => {
  for (const status of [200, 403, 404, 500]) {
    expect(() => throwIfSessionExpired(new Response(null, { status }))).not.toThrow();
  }
});
