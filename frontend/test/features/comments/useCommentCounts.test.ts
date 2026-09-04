import { commentCountKey, commentThreadKey } from '@features/comments/queryKeys';
import { prefetchCommentCounts, seedCommentCounts } from '@features/comments/useCommentCounts';
import { createQueryClient } from '@shared';
import { jsonResponse, pathOf, stubFetch } from '@test-support/mockFetch';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

test('the count key nests under the thread key, so invalidating the thread refreshes the count', async () => {
  const client = createQueryClient();
  seedCommentCounts(client, [{ postId: 'p-1', commentCount: 3 }]);

  await client.invalidateQueries({ queryKey: commentThreadKey('p-1') });

  expect(client.getQueryState(commentCountKey('p-1'))?.isInvalidated).toBe(true);
});

test('seedCommentCounts writes each record under its per-post key', () => {
  const client = createQueryClient();

  seedCommentCounts(client, [
    { postId: 'p-1', commentCount: 3 },
    { postId: 'p-2', commentCount: 0 },
  ]);

  expect(client.getQueryData(commentCountKey('p-1'))).toEqual({ commentCount: 3 });
  expect(client.getQueryData(commentCountKey('p-2'))).toEqual({ commentCount: 0 });
});

test('prefetchCommentCounts fetches one batch and seeds every post key', async () => {
  const client = createQueryClient();
  const calls = stubFetch(() =>
    jsonResponse([
      { postId: 'p-1', commentCount: 5 },
      { postId: 'p-2', commentCount: 1 },
    ]),
  );

  await prefetchCommentCounts(client, ['p-1', 'p-2']);

  expect(calls.filter((c) => pathOf(c) === '/api/comments')).toHaveLength(1);
  expect(client.getQueryData(commentCountKey('p-1'))).toEqual({ commentCount: 5 });
  expect(client.getQueryData(commentCountKey('p-2'))).toEqual({ commentCount: 1 });
});

test('prefetchCommentCounts makes no call for an empty page', async () => {
  const client = createQueryClient();
  const calls = stubFetch(() => jsonResponse([]));

  await prefetchCommentCounts(client, []);

  expect(calls).toHaveLength(0);
});
