import type { FeedPost } from '@features/feed/feed';
import { toFeedCards } from '@features/feed/feedCards';
import { SessionExpiredError } from '@shared';
import { jsonResponse, pathOf, stubFetch } from '@test-support/mockFetch';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

function post(overrides: Partial<FeedPost> = {}): FeedPost {
  return {
    postId: 'p-1',
    authorId: 'u-1',
    mediaId: 'm-1',
    caption: null,
    publishedAt: '2026-09-01T10:00:00Z',
    ...overrides,
  };
}

test('resolves every author in a single batched profile call — no N+1', async () => {
  const calls = stubFetch(() =>
    jsonResponse([
      { userId: 'u-1', username: 'ada', displayName: 'Ada' },
      { userId: 'u-2', username: 'bob', displayName: null },
    ]),
  );

  const cards = await toFeedCards([
    post({ postId: 'p-1', authorId: 'u-1', mediaId: 'm-1' }),
    post({ postId: 'p-2', authorId: 'u-2', mediaId: 'm-2' }),
    post({ postId: 'p-3', authorId: 'u-1', mediaId: 'm-3' }),
  ]);

  const profileCalls = calls.filter((c) => pathOf(c) === '/api/profiles');
  expect(profileCalls).toHaveLength(1);
  expect(new URL(profileCalls[0].url).searchParams.getAll('ids')).toEqual(['u-1', 'u-2']);

  expect(cards).toEqual([
    {
      postId: 'p-1',
      author: { userId: 'u-1', username: 'ada', displayName: 'Ada' },
      imageUrl: '/api/media/m-1/original',
      caption: null,
      publishedAt: '2026-09-01T10:00:00Z',
    },
    {
      postId: 'p-2',
      author: { userId: 'u-2', username: 'bob', displayName: null },
      imageUrl: '/api/media/m-2/original',
      caption: null,
      publishedAt: '2026-09-01T10:00:00Z',
    },
    {
      postId: 'p-3',
      author: { userId: 'u-1', username: 'ada', displayName: 'Ada' },
      imageUrl: '/api/media/m-3/original',
      caption: null,
      publishedAt: '2026-09-01T10:00:00Z',
    },
  ]);
});

test('makes no profile call for an empty page', async () => {
  const calls = stubFetch(() => jsonResponse([]));

  await expect(toFeedCards([])).resolves.toEqual([]);
  expect(calls).toHaveLength(0);
});

test('falls back to a placeholder author when a profile is missing from the batch', async () => {
  stubFetch(() => jsonResponse([]));

  const [card] = await toFeedCards([post({ authorId: 'ghost' })]);

  expect(card.author).toEqual({ userId: 'ghost', username: '', displayName: null });
});

test('propagates an expired session from the author batch', async () => {
  stubFetch(() => jsonResponse({}, 401));

  await expect(toFeedCards([post()])).rejects.toBeInstanceOf(SessionExpiredError);
});
