import { ExploreRoute } from '@app/ExploreRoute';
import { jsonResponse, pathOf, stubFetch } from '@test-support/mockFetch';
import { renderWithProviders } from '@test-support/render';
import { screen } from '@testing-library/react';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router';
import { afterEach, expect, test, vi } from 'vitest';

function renderExploreRoute(viewer: { userId: string; username: string } | null) {
  return renderWithProviders(
    <MemoryRouter initialEntries={['/explore']}>
      <Routes>
        <Route element={<Outlet context={{ viewer }} />}>
          <Route path="/explore" element={<ExploreRoute />} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

const post = {
  postId: 'p-1',
  authorId: 'u-2',
  mediaId: 'm-1',
  caption: 'a caption',
  publishedAt: '2026-09-01T10:00:00Z',
};

afterEach(() => {
  vi.unstubAllGlobals();
});

function stubExploreResponses() {
  return stubFetch((request) => {
    switch (pathOf(request)) {
      case '/api/explore':
        return jsonResponse({ items: [post], nextCursor: null });
      case '/api/comments':
        return jsonResponse([{ postId: 'p-1', commentCount: 0 }]);
      case '/api/likes':
        return jsonResponse([{ postId: 'p-1', likeCount: 0, likedByViewer: false }]);
      default:
        return jsonResponse([{ userId: 'u-2', username: 'bob', displayName: 'Bob' }]);
    }
  });
}

test('an anonymous visitor sees a like count, not a like button', async () => {
  stubExploreResponses();
  renderExploreRoute(null);

  expect(await screen.findAllByRole('article')).toHaveLength(1);
  expect(screen.queryByRole('button', { name: /^like$/i })).not.toBeInTheDocument();
});

test('a signed-in visitor sees an interactive like button', async () => {
  stubExploreResponses();
  renderExploreRoute({ userId: 'u-1', username: 'ada' });

  expect(await screen.findByRole('button', { name: /^like$/i })).toBeInTheDocument();
});
