import { NotificationsPage } from '@features/notifications/NotificationsPage';
import { jsonResponse, pathOf, stubFetch } from '@test-support/mockFetch';
import { renderWithProviders } from '@test-support/render';
import { screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

const notification = (over: Record<string, unknown> = {}) => ({
  type: 'post-liked',
  actorId: 'u-ada',
  subjectPostId: 'p-1',
  occurredAt: '2026-09-04T10:00:00Z',
  read: false,
  ...over,
});

function render() {
  return renderWithProviders(
    <MemoryRouter>
      <NotificationsPage />
    </MemoryRouter>,
  );
}

function stubWorld(items: Record<string, unknown>[]) {
  return stubFetch((request) => {
    const path = pathOf(request);
    if (path === '/api/notifications') return jsonResponse({ items, nextCursor: null });
    if (path === '/api/notifications/mark-read') return new Response(null, { status: 204 });
    if (path === '/api/notifications/unread-count') return jsonResponse({ count: 0 });
    if (path === '/api/profiles')
      return jsonResponse([{ userId: 'u-ada', username: 'ada', displayName: 'Ada' }]);
    if (path === '/api/posts/by-ids')
      return jsonResponse({ items: [{ postId: 'p-1', mediaId: 'm-1' }] });
    throw new Error(`unexpected ${path}`);
  });
}

test('lists the notifications with the actor and the sentence, newest kept in order', async () => {
  stubWorld([
    notification({ type: 'post-liked', subjectPostId: 'p-1' }),
    notification({ type: 'user-followed', subjectPostId: null }),
  ]);

  render();

  const rows = await screen.findAllByRole('listitem');
  expect(rows).toHaveLength(2);
  expect(rows[0]).toHaveTextContent('Ada liked your post');
  expect(rows[1]).toHaveTextContent('Ada started following you');
  expect(within(rows[0]).getByRole('link')).toHaveAttribute('href', '/p/p-1');
  expect(within(rows[1]).getByRole('link')).toHaveAttribute('href', '/u/ada');
});

test('a like row shows the post thumbnail; a follow row does not', async () => {
  stubWorld([
    notification({ type: 'post-liked', subjectPostId: 'p-1' }),
    notification({ type: 'user-followed', subjectPostId: null }),
  ]);

  render();

  const rows = await screen.findAllByRole('listitem');
  expect(rows[0].querySelector('img')).toHaveAttribute('src', '/api/media/m-1/thumbnail');
  expect(rows[1].querySelector('img')).toBeNull();
});

test('marks everything read on mount', async () => {
  const calls = stubWorld([notification()]);

  render();

  await screen.findAllByRole('listitem');
  await waitFor(() => {
    const markRead = calls.find((c) => pathOf(c) === '/api/notifications/mark-read');
    expect(markRead?.method).toBe('POST');
  });
});

test('shows the empty state when there is nothing', async () => {
  stubWorld([]);
  render();

  expect(await screen.findByText(/nothing yet/i)).toBeInTheDocument();
});

test('shows an error branch when the list read fails', async () => {
  stubFetch((request) => {
    if (pathOf(request) === '/api/notifications') return new Response(null, { status: 500 });
    if (pathOf(request) === '/api/notifications/mark-read')
      return new Response(null, { status: 204 });
    return jsonResponse({ count: 0 });
  });

  render();

  expect(
    await screen.findByText(/couldn.t load your notifications/i, undefined, { timeout: 3000 }),
  ).toBeInTheDocument();
});
