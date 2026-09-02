import {
  fetchFollowListPage,
  fetchFollowRelationship,
  fetchFollowRelationships,
  followUser,
  unfollowUser,
} from '@features/follow/followApi';
import { jsonResponse, pathOf, problemResponse, stubFetch } from '@test-support/mockFetch';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

test('fetchFollowRelationship maps the counts and viewer flag', async () => {
  stubFetch(() => jsonResponse({ followerCount: 3, followingCount: 1, followedByViewer: true }));

  await expect(fetchFollowRelationship('u-1')).resolves.toEqual({
    followerCount: 3,
    followingCount: 1,
    followedByViewer: true,
  });
});

test('fetchFollowRelationship retries anonymously when a stale token makes the public read 401', async () => {
  stubFetch((request, hits) => {
    if (new URL(request.url).pathname === '/api/auth/refresh') {
      return problemResponse('unauthorized', 401);
    }
    return hits === 0
      ? problemResponse('unauthorized', 401)
      : jsonResponse({ followerCount: 2, followingCount: 0, followedByViewer: false });
  });

  await expect(fetchFollowRelationship('u-1')).resolves.toEqual({
    followerCount: 2,
    followingCount: 0,
    followedByViewer: false,
  });
});

test('followUser PUTs the relationship resource', async () => {
  const calls = stubFetch(() => new Response(null, { status: 204 }));

  await expect(followUser('u-9')).resolves.toBeUndefined();

  expect(calls[0].method).toBe('PUT');
  expect(pathOf(calls[0])).toBe('/api/follows/u-9');
});

test('unfollowUser DELETEs the relationship resource', async () => {
  const calls = stubFetch(() => new Response(null, { status: 204 }));

  await expect(unfollowUser('u-9')).resolves.toBeUndefined();

  expect(calls[0].method).toBe('DELETE');
  expect(pathOf(calls[0])).toBe('/api/follows/u-9');
});

test('fetchFollowRelationships maps the batch to seedable records', async () => {
  const calls = stubFetch(() =>
    jsonResponse([
      { userId: 'u-9', followerCount: 4, followingCount: 2, followedByViewer: true },
      { userId: 'u-3', followerCount: 0, followingCount: 1, followedByViewer: false },
    ]),
  );

  await expect(fetchFollowRelationships(['u-9', 'u-3'])).resolves.toEqual([
    { userId: 'u-9', followerCount: 4, followingCount: 2, followedByViewer: true },
    { userId: 'u-3', followerCount: 0, followingCount: 1, followedByViewer: false },
  ]);
  expect(pathOf(calls[0])).toBe('/api/follows');
  expect(new URL(calls[0].url).searchParams.getAll('ids')).toEqual(['u-9', 'u-3']);
});

test('fetchFollowRelationships makes no call for an empty id set', async () => {
  const calls = stubFetch(() => jsonResponse([]));

  await expect(fetchFollowRelationships([])).resolves.toEqual([]);
  expect(calls).toHaveLength(0);
});

test('fetchFollowListPage composes the list with a profile batch and a relationship batch, keeping order', async () => {
  const calls = stubFetch((request) => {
    const url = new URL(request.url);
    if (url.pathname === '/api/follows/u-1/followers') {
      return jsonResponse({ items: ['u-9', 'u-3'], nextCursor: 'CURSOR' });
    }
    if (url.pathname === '/api/profiles') {
      return jsonResponse([
        { userId: 'u-3', username: 'carol', displayName: 'Carol' },
        { userId: 'u-9', username: 'ada', displayName: null },
      ]);
    }
    if (url.pathname === '/api/follows') {
      return jsonResponse([
        { userId: 'u-9', followerCount: 1, followingCount: 0, followedByViewer: true },
        { userId: 'u-3', followerCount: 2, followingCount: 2, followedByViewer: false },
      ]);
    }
    throw new Error(`unexpected ${url.pathname}`);
  });

  await expect(fetchFollowListPage('followers', 'u-1', undefined)).resolves.toEqual({
    accounts: [
      { userId: 'u-9', username: 'ada', displayName: null },
      { userId: 'u-3', username: 'carol', displayName: 'Carol' },
    ],
    relationships: [
      { userId: 'u-9', followerCount: 1, followingCount: 0, followedByViewer: true },
      { userId: 'u-3', followerCount: 2, followingCount: 2, followedByViewer: false },
    ],
    nextCursor: 'CURSOR',
  });

  expect(calls.map(pathOf).sort()).toEqual(
    ['/api/follows', '/api/follows/u-1/followers', '/api/profiles'].sort(),
  );
});

test("fetchFollowListPage skips a listed id the profile batch doesn't return", async () => {
  stubFetch((request) => {
    const url = new URL(request.url);
    if (url.pathname === '/api/follows/u-1/following') {
      return jsonResponse({ items: ['u-9', 'u-gone'], nextCursor: null });
    }
    if (url.pathname === '/api/follows') {
      return jsonResponse([
        { userId: 'u-9', followerCount: 0, followingCount: 0, followedByViewer: false },
        { userId: 'u-gone', followerCount: 0, followingCount: 0, followedByViewer: false },
      ]);
    }
    return jsonResponse([{ userId: 'u-9', username: 'ada', displayName: 'Ada' }]);
  });

  const page = await fetchFollowListPage('following', 'u-1');
  expect(page.accounts).toEqual([{ userId: 'u-9', username: 'ada', displayName: 'Ada' }]);
  expect(page.nextCursor).toBeNull();
});

test('fetchFollowListPage makes no batch calls for an empty page', async () => {
  const calls = stubFetch(() => jsonResponse({ items: [], nextCursor: null }));

  const page = await fetchFollowListPage('followers', 'u-1');
  expect(page).toEqual({ accounts: [], relationships: [], nextCursor: null });
  expect(calls.map(pathOf)).toEqual(['/api/follows/u-1/followers']);
});

test('followUser throws when the write fails', async () => {
  stubFetch((request) => {
    if (new URL(request.url).pathname === '/api/auth/refresh')
      return problemResponse('unauthorized', 401);
    return problemResponse('unauthorized', 401);
  });

  await expect(followUser('u-9')).rejects.toThrow(/Following failed/);
});
