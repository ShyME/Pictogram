import { fetchPostLikesBatch, likePost, unlikePost } from '@features/engagement/engagementApi';
import { jsonResponse, pathOf, problemResponse, stubFetch } from '@test-support/mockFetch';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

test('fetchPostLikesBatch maps the batch to seedable records, one GET with repeated postIds', async () => {
  const calls = stubFetch(() =>
    jsonResponse([
      { postId: 'p-1', likeCount: 4, likedByViewer: true },
      { postId: 'p-2', likeCount: 0, likedByViewer: false },
    ]),
  );

  await expect(fetchPostLikesBatch(['p-1', 'p-2'])).resolves.toEqual([
    { postId: 'p-1', likeCount: 4, likedByViewer: true },
    { postId: 'p-2', likeCount: 0, likedByViewer: false },
  ]);
  expect(calls).toHaveLength(1);
  expect(pathOf(calls[0])).toBe('/api/engagement/likes');
  expect(new URL(calls[0].url).searchParams.getAll('postIds')).toEqual(['p-1', 'p-2']);
});

test('fetchPostLikesBatch retries anonymously when a stale token makes the public read 401', async () => {
  stubFetch((request, hits) => {
    if (new URL(request.url).pathname === '/api/auth/refresh') {
      return problemResponse('unauthorized', 401);
    }
    return hits === 0
      ? problemResponse('unauthorized', 401)
      : jsonResponse([{ postId: 'p-1', likeCount: 7, likedByViewer: false }]);
  });

  await expect(fetchPostLikesBatch(['p-1'])).resolves.toEqual([
    { postId: 'p-1', likeCount: 7, likedByViewer: false },
  ]);
});

test('fetchPostLikesBatch makes no call for an empty id set', async () => {
  const calls = stubFetch(() => jsonResponse([]));

  await expect(fetchPostLikesBatch([])).resolves.toEqual([]);
  expect(calls).toHaveLength(0);
});

test('likePost PUTs the like resource', async () => {
  const calls = stubFetch(() => new Response(null, { status: 204 }));

  await expect(likePost('p-9')).resolves.toBeUndefined();
  expect(calls[0].method).toBe('PUT');
  expect(pathOf(calls[0])).toBe('/api/engagement/likes/p-9');
});

test('unlikePost DELETEs the like resource', async () => {
  const calls = stubFetch(() => new Response(null, { status: 204 }));

  await expect(unlikePost('p-9')).resolves.toBeUndefined();
  expect(calls[0].method).toBe('DELETE');
  expect(pathOf(calls[0])).toBe('/api/engagement/likes/p-9');
});

test('likePost throws when the write fails', async () => {
  stubFetch(() => new Response(null, { status: 500 }));

  await expect(likePost('p-9')).rejects.toThrow(/Liking failed/);
});
