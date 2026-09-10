import { PostDetailPage } from '@features/post/PostDetailPage';
import { jsonResponse, pathOf, stubFetch } from '@test-support/mockFetch';
import { renderWithProviders } from '@test-support/render';
import { screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

function renderAt(postId: string) {
  return renderWithProviders(
    <MemoryRouter initialEntries={[`/p/${postId}`]}>
      <Routes>
        <Route
          path="/p/:postId"
          element={
            <PostDetailPage
              renderLike={() => <span>like control</span>}
              renderComments={(_postId, authorId) => <span>comments for {authorId}</span>}
            />
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

test('renders the post with its author, image and caption, and the injected controls', async () => {
  stubFetch((request) => {
    if (pathOf(request) === '/api/posts/by-ids') {
      return jsonResponse({
        items: [
          {
            postId: 'p-1',
            authorId: 'u-1',
            mediaId: 'm-1',
            caption: 'a clearing storm',
            publishedAt: '2026-09-04T10:00:00Z',
          },
        ],
      });
    }
    if (pathOf(request) === '/api/profiles') {
      return jsonResponse([{ userId: 'u-1', username: 'ansel', displayName: 'Ansel' }]);
    }
    throw new Error(`unexpected ${pathOf(request)}`);
  });

  renderAt('p-1');

  expect(await screen.findByText('a clearing storm')).toBeInTheDocument();
  expect(screen.getByRole('link', { name: /ansel/i })).toHaveAttribute('href', '/u/ansel');
  expect(screen.getByRole('img')).toHaveAttribute('src', '/api/media/m-1/original');
  expect(screen.getByText('like control')).toBeInTheDocument();
  expect(screen.getByText('comments for u-1')).toBeInTheDocument();
});

test('shows a not-found state when the post is missing', async () => {
  stubFetch((request) => {
    if (pathOf(request) === '/api/posts/by-ids') return jsonResponse({ items: [] });
    return jsonResponse([]);
  });

  renderAt('p-gone');

  expect(await screen.findByText(/doesn.t exist/i)).toBeInTheDocument();
});
