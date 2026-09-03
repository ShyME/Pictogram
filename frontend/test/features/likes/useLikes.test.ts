import { postLikesKey } from '@features/likes/queryKeys';
import { prefetchPostLikes, seedPostLikes } from '@features/likes/useLikes';
import { createQueryClient } from '@shared';
import { jsonResponse, pathOf, stubFetch } from '@test-support/mockFetch';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

test('seedPostLikes writes each record under its per-post key', () => {
  const client = createQueryClient();

  seedPostLikes(client, [
    { postId: 'p-1', likeCount: 2, likedByViewer: true },
    { postId: 'p-2', likeCount: 0, likedByViewer: false },
  ]);

  expect(client.getQueryData(postLikesKey('p-1'))).toEqual({ likeCount: 2, likedByViewer: true });
  expect(client.getQueryData(postLikesKey('p-2'))).toEqual({ likeCount: 0, likedByViewer: false });
});

test('prefetchPostLikes fetches one batch and seeds every post key', async () => {
  const client = createQueryClient();
  const calls = stubFetch(() =>
    jsonResponse([
      { postId: 'p-1', likeCount: 5, likedByViewer: false },
      { postId: 'p-2', likeCount: 1, likedByViewer: true },
    ]),
  );

  await prefetchPostLikes(client, ['p-1', 'p-2']);

  expect(calls.filter((c) => pathOf(c) === '/api/likes')).toHaveLength(1);
  expect(client.getQueryData(postLikesKey('p-1'))).toEqual({ likeCount: 5, likedByViewer: false });
  expect(client.getQueryData(postLikesKey('p-2'))).toEqual({ likeCount: 1, likedByViewer: true });
});

test('prefetchPostLikes makes no call for an empty page', async () => {
  const client = createQueryClient();
  const calls = stubFetch(() => jsonResponse([]));

  await prefetchPostLikes(client, []);

  expect(calls).toHaveLength(0);
});
