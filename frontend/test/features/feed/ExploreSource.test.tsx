import { FeedPage, exploreSource } from '@features/feed';
import { jsonResponse, pathOf, stubFetch } from '@test-support/mockFetch';
import { renderWithProviders } from '@test-support/render';
import { screen, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, expect, test, vi } from 'vitest';

function renderExplore() {
  renderWithProviders(
    <MemoryRouter initialEntries={['/explore']}>
      <Routes>
        <Route path="/explore" element={<FeedPage source={exploreSource} />} />
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

test('falls back to an anonymous read on a stale-token 401 rather than redirecting to /login', async () => {
  const requests = stubFetch((request, hits) =>
    hits === 0 && pathOf(request) === '/api/explore'
      ? jsonResponse({}, 401)
      : jsonResponse({ items: [], nextCursor: null }),
  );
  renderExplore();

  expect(await screen.findByText(/no posts yet/i)).toBeInTheDocument();
  expect(requests.filter((r) => pathOf(r) === '/api/explore')).toHaveLength(2);
  expect(requests.some((r) => pathOf(r) === '/api/feed')).toBe(false);
});

test('renders every post it is handed, regardless of who published it', async () => {
  stubFetch((request) =>
    jsonResponse(
      pathOf(request) === '/api/explore'
        ? {
            items: [card({ postId: 'p-1' }), card({ postId: 'p-2', authorId: 'u-2' })],
            nextCursor: null,
          }
        : [
            { userId: 'u-1', username: 'ada', displayName: 'Ada' },
            { userId: 'u-2', username: 'bob', displayName: 'Bob' },
          ],
    ),
  );
  renderExplore();

  const articles = await screen.findAllByRole('article');
  expect(articles).toHaveLength(2);
  expect(within(articles[1]).getByRole('link', { name: /Bob/ })).toHaveAttribute('href', '/u/bob');
});
