import { orAnonymous } from '@shared';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

function stubFetch(respond: () => Response): Request[] {
  const requests: Request[] = [];
  vi.stubGlobal('fetch', (input: RequestInfo | URL, init?: RequestInit) => {
    requests.push(input instanceof Request ? input : new Request(String(input), init));
    return Promise.resolve(respond());
  });
  return requests;
}

const json = (body: unknown, status = 200): Response =>
  Response.json(body, { status, headers: { 'Content-Type': 'application/json' } });

const bearer = (data: unknown, status = 200) => ({
  data: status === 200 ? data : undefined,
  response: new Response(null, { status }),
});

test('settles the read from the bearer data without a second request', async () => {
  const requests = stubFetch(() => json([]));

  await expect(
    orAnonymous(bearer([{ n: 1 }]), { path: '/api/likes', label: 'Likes' }),
  ).resolves.toEqual([{ n: 1 }]);
  expect(requests).toHaveLength(0);
});

test('on a 401 re-fetches the same URL bare and returns the public projection', async () => {
  const requests = stubFetch(() => json([{ n: 9 }]));

  const result = await orAnonymous(bearer(undefined, 401), {
    path: '/api/likes',
    query: { postIds: ['p-1', 'p-2'] },
    label: 'Likes',
  });

  expect(result).toEqual([{ n: 9 }]);
  const url = new URL(requests[0].url);
  expect(url.pathname).toBe('/api/likes');
  expect(url.searchParams.getAll('postIds')).toEqual(['p-1', 'p-2']);
  expect(requests[0].headers.get('Accept')).toBe('application/json');
});

test('a non-401 bearer failure throws with the label and status, no retry', async () => {
  const requests = stubFetch(() => json([]));

  await expect(
    orAnonymous(bearer(undefined, 503), { path: '/api/comments', label: 'Comment counts' }),
  ).rejects.toThrow('Comment counts failed: 503');
  expect(requests).toHaveLength(0);
});

test('a failing anonymous retry throws with the label and the retry status', async () => {
  stubFetch(() => new Response(null, { status: 500 }));

  await expect(
    orAnonymous(bearer(undefined, 401), { path: '/api/comments', label: 'Comment counts' }),
  ).rejects.toThrow('Comment counts failed: 500');
});
