import { fetchPosts } from '@shared';
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

const view = (postId: string, mediaId: string) => ({
  postId,
  authorId: `author-of-${postId}`,
  mediaId,
  caption: `caption ${postId}`,
  publishedAt: '2026-09-04T10:00:00Z',
});

test('makes no request and returns an empty Map for an empty id set', async () => {
  const requests = stubFetch(() => json({ items: [] }));

  await expect(fetchPosts([])).resolves.toEqual(new Map());
  expect(requests).toHaveLength(0);
});

test('batches every id into one /api/posts/by-ids call keyed by postId', async () => {
  const requests = stubFetch(() => json({ items: [view('p-1', 'm-1'), view('p-2', 'm-2')] }));

  const posts = await fetchPosts(['p-1', 'p-2']);

  expect(requests).toHaveLength(1);
  const url = new URL(requests[0].url);
  expect(url.pathname).toBe('/api/posts/by-ids');
  expect(url.searchParams.getAll('ids')).toEqual(['p-1', 'p-2']);
  expect(posts.get('p-1')?.mediaId).toBe('m-1');
  expect(posts.get('p-2')?.caption).toBe('caption p-2');
});

test('falls back to an anonymous read when the bearer call is unauthorised', async () => {
  const requests = stubFetch((_url, hit) =>
    hit === 0 ? json({}, 401) : json({ items: [view('p-1', 'm-1')] }),
  );

  const posts = await fetchPosts(['p-1']);

  expect(requests).toHaveLength(2);
  expect(posts.get('p-1')?.mediaId).toBe('m-1');
});
