import {
  fetchNotificationsPage,
  fetchUnreadCount,
  markAllNotificationsRead,
} from '@features/notifications/notificationsApi';
import { SessionExpiredError } from '@shared';
import { jsonResponse, pathOf, stubFetch } from '@test-support/mockFetch';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

const notification = (over: Record<string, unknown> = {}) => ({
  type: 'post-liked',
  actorId: 'u-actor',
  subjectPostId: 'p-1',
  occurredAt: '2026-09-04T10:00:00Z',
  read: false,
  ...over,
});

test('composes one page with the actors and posts resolved in one batch each', async () => {
  const calls = stubFetch((request) => {
    if (pathOf(request) === '/api/notifications') {
      return jsonResponse({
        items: [
          notification({ actorId: 'u-a', subjectPostId: 'p-1' }),
          notification({ type: 'user-followed', actorId: 'u-b', subjectPostId: null }),
        ],
        nextCursor: 'CURSOR',
      });
    }
    if (pathOf(request) === '/api/profiles') {
      return jsonResponse([
        { userId: 'u-a', username: 'ada', displayName: 'Ada' },
        { userId: 'u-b', username: 'bob', displayName: 'Bob' },
      ]);
    }
    if (pathOf(request) === '/api/posts/by-ids') {
      return jsonResponse({ items: [{ postId: 'p-1', mediaId: 'm-1' }] });
    }
    throw new Error(`unexpected ${pathOf(request)}`);
  });

  const page = await fetchNotificationsPage();

  expect(calls.filter((c) => pathOf(c) === '/api/profiles')).toHaveLength(1);
  expect(calls.filter((c) => pathOf(c) === '/api/posts/by-ids')).toHaveLength(1);
  expect(page.nextCursor).toBe('CURSOR');
  expect(page.cards[0]).toMatchObject({
    type: 'post-liked',
    actor: { username: 'ada' },
    postMediaId: 'm-1',
  });
  expect(page.cards[1]).toMatchObject({ type: 'user-followed', actor: { username: 'bob' } });
  expect(page.cards[1].postMediaId).toBeNull();
});

test('makes no post-batch call when the page holds only follows', async () => {
  const calls = stubFetch((request) => {
    if (pathOf(request) === '/api/notifications') {
      return jsonResponse({
        items: [notification({ type: 'user-followed', subjectPostId: null })],
      });
    }
    if (pathOf(request) === '/api/profiles') return jsonResponse([]);
    throw new Error(`unexpected ${pathOf(request)}`);
  });

  await fetchNotificationsPage();

  expect(calls.filter((c) => pathOf(c) === '/api/posts/by-ids')).toHaveLength(0);
});

test('forwards the cursor as a query param', async () => {
  const calls = stubFetch((request) => {
    if (pathOf(request) === '/api/notifications') return jsonResponse({ items: [] });
    return jsonResponse([]);
  });

  await fetchNotificationsPage('NEXT');

  const list = calls.find((c) => pathOf(c) === '/api/notifications');
  expect(list).toBeDefined();
  expect(new URL(list?.url ?? '').searchParams.get('cursor')).toBe('NEXT');
});

test('an expired session propagates from the list read', async () => {
  stubFetch(() => jsonResponse({}, 401));
  await expect(fetchNotificationsPage()).rejects.toBeInstanceOf(SessionExpiredError);
});

test('fetchUnreadCount reads the count', async () => {
  stubFetch(() => jsonResponse({ count: 7 }));
  await expect(fetchUnreadCount()).resolves.toBe(7);
});

test('markAllNotificationsRead POSTs and resolves on 204', async () => {
  const calls = stubFetch(() => new Response(null, { status: 204 }));
  await expect(markAllNotificationsRead()).resolves.toBeUndefined();
  expect(calls[0].method).toBe('POST');
  expect(pathOf(calls[0])).toBe('/api/notifications/mark-read');
});
