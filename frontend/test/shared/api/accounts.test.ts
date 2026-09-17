import { fetchAccounts } from '@shared';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

function stubFetch(respond: (url: URL, hit: number) => Response): Request[] {
  const requests: Request[] = [];
  vi.stubGlobal('fetch', (input: RequestInfo | URL, init?: RequestInit) => {
    const request =
      input instanceof Request
        ? input
        : new Request(new URL(String(input), 'http://localhost'), init);
    const url = new URL(request.url);
    const hit = requests.filter((r) => new URL(r.url).pathname === url.pathname).length;
    requests.push(request);
    return Promise.resolve(respond(url, hit));
  });
  return requests;
}

const json = (body: unknown, status = 200): Response =>
  Response.json(body, { status, headers: { 'Content-Type': 'application/json' } });

test('makes no request and returns an empty Map for an empty id set', async () => {
  const requests = stubFetch(() => json([]));

  await expect(fetchAccounts([])).resolves.toEqual(new Map());
  expect(requests).toHaveLength(0);
});

test('batches every id into one /api/profiles?ids= call keyed by userId', async () => {
  const requests = stubFetch(() =>
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
  stubFetch(() => json([{}]));

  const accounts = await fetchAccounts(['u-1']);

  expect(accounts.get('')).toEqual({ userId: '', username: '', displayName: null });
});

test('falls back to an anonymous read when the bearer call is unauthorised', async () => {
  const requests = stubFetch((_url, hit) =>
    hit === 0 ? json({}, 401) : json([{ userId: 'u-1', username: 'ada', displayName: 'Ada' }]),
  );

  const accounts = await fetchAccounts(['u-1']);

  expect(requests).toHaveLength(2);
  expect(accounts.get('u-1')?.username).toBe('ada');
});

test('throws when the batch response carries no data', async () => {
  stubFetch(() => new Response(null, { status: 500 }));

  await expect(fetchAccounts(['u-1'])).rejects.toThrow(/500/);
});
