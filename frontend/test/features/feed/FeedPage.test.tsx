import { FeedPage } from '@features/feed/FeedPage';
import { jsonResponse, pathOf, stubFetch } from '@test-support/mock-fetch';
import { renderWithProviders } from '@test-support/render';
import { fireEvent, screen, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, expect, test, vi } from 'vitest';

function renderFeed() {
  renderWithProviders(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route path="/" element={<FeedPage />} />
        <Route path="/login" element={<p>Login screen</p>} />
        <Route path="/u/:username" element={<p>Profile screen</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

const card = (over: Record<string, unknown>) => ({
  postId: 'p-1',
  authorId: 'u-1',
  mediaId: 'm-1',
  caption: 'a caption',
  publishedAt: '2026-09-01T10:00:00Z',
  ...over,
});

afterEach(() => {
  vi.unstubAllGlobals();
});

test("shows a friendly 'find people to follow' empty state", async () => {
  stubFetch((request) =>
    pathOf(request) === '/api/feed'
      ? jsonResponse({ items: [], nextCursor: null })
      : jsonResponse([]),
  );
  renderFeed();

  expect(await screen.findByText(/find people to follow/i)).toBeInTheDocument();
});

test('redirects to /login when the session has expired', async () => {
  stubFetch(() => jsonResponse({}, 401));
  renderFeed();

  expect(await screen.findByText('Login screen', undefined, { timeout: 3000 })).toBeInTheDocument();
});

test('surfaces a load failure without crashing', async () => {
  stubFetch(() => new Response(null, { status: 500 }));
  renderFeed();

  expect(
    await screen.findByText(/couldn.t load your feed/i, undefined, { timeout: 3000 }),
  ).toBeInTheDocument();
});

test('renders a card per post with the author and a relative timestamp', async () => {
  stubFetch((request) =>
    pathOf(request) === '/api/feed'
      ? jsonResponse({
          items: [card({ postId: 'p-1' }), card({ postId: 'p-2' })],
          nextCursor: null,
        })
      : jsonResponse([{ userId: 'u-1', username: 'ada', displayName: 'Ada Lovelace' }]),
  );
  renderFeed();

  const articles = await screen.findAllByRole('article');
  expect(articles).toHaveLength(2);
  expect(within(articles[0]).getByRole('link', { name: /Ada Lovelace/ })).toHaveAttribute(
    'href',
    '/u/ada',
  );
  expect(within(articles[0]).getByText(/a caption/)).toBeInTheDocument();
});

test('loads the next page on demand', async () => {
  stubFetch((request) => {
    if (pathOf(request) === '/api/profiles') {
      return jsonResponse([{ userId: 'u-1', username: 'ada', displayName: 'Ada' }]);
    }
    const cursor = new URL(request.url).searchParams.get('cursor');
    return cursor === 'PAGE2'
      ? jsonResponse({ items: [card({ postId: 'p-2', caption: 'second page' })], nextCursor: null })
      : jsonResponse({
          items: [card({ postId: 'p-1', caption: 'first page' })],
          nextCursor: 'PAGE2',
        });
  });
  renderFeed();

  expect(await screen.findByText('first page')).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: /load more/i }));

  expect(await screen.findByText('second page', undefined, { timeout: 3000 })).toBeInTheDocument();
  expect(screen.getAllByRole('article')).toHaveLength(2);
  expect(screen.queryByRole('button', { name: /load more/i })).not.toBeInTheDocument();
});
