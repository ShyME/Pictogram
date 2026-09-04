import { fetchAccounts, SessionExpiredError } from '@shared';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

function stubProfiles(respond: () => Response): Request[] {
  const requests: Request[] = [];
  vi.stubGlobal('fetch', (input: RequestInfo | URL, init?: RequestInit) => {
    requests.push(input instanceof Request ? input : new Request(String(input), init));
    return Promise.resolve(respond());
  });
  return requests;
}

const json = (body: unknown, status = 200): Response =>
  Response.json(body, { status, headers: { 'Content-Type': 'application/json' } });

test('makes no request and returns an empty Map for an empty id set', async () => {
  const requests = stubProfiles(() => json([]));

  await expect(fetchAccounts([])).resolves.toEqual(new Map());
  expect(requests).toHaveLength(0);
});

test('batches every id into one /api/profiles?ids= call keyed by userId', async () => {
  const requests = stubProfiles(() =>
    json([
      { userId: 'u-1', username: 'ada', displayName: 'Ada' },
      { userId: 'u-2', username: 'bob', displayName: null },
    ]),
  );

  const accounts = await fetchAccounts(['u-1', 'u-2']);

  expect(requests).toHaveLength(1);
  const url = new URL(requests[0].url);
  expect(url.pathname).toBe('/api/profiles');
  expect(url.searchParams.getAll('ids')).toEqual(['u-1', 'u-2']);
  expect(accounts.get('u-1')).toEqual({ userId: 'u-1', username: 'ada', displayName: 'Ada' });
  expect(accounts.get('u-2')).toEqual({ userId: 'u-2', username: 'bob', displayName: null });
});

test('normalises missing wire fields to empty strings and a null display name', async () => {
  stubProfiles(() => json([{}]));

  const accounts = await fetchAccounts(['u-1']);

  expect(accounts.get('')).toEqual({ userId: '', username: '', displayName: null });
});

test('propagates an expired session', async () => {
  stubProfiles(() => json({}, 401));

  await expect(fetchAccounts(['u-1'])).rejects.toBeInstanceOf(SessionExpiredError);
});

test('throws when the batch response carries no data', async () => {
  stubProfiles(() => new Response(null, { status: 500 }));

  await expect(fetchAccounts(['u-1'])).rejects.toThrow(/500/);
});
