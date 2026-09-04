import { FeedPage } from '@features/feed/FeedPage';
import { jsonResponse, pathOf, stubFetch } from '@test-support/mockFetch';
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
    jsonResponse(pathOf(request) === '/api/feed' ? { items: [], nextCursor: null } : []),
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
    jsonResponse(
      pathOf(request) === '/api/feed'
        ? { items: [card({ postId: 'p-1' }), card({ postId: 'p-2' })], nextCursor: null }
        : [{ userId: 'u-1', username: 'ada', displayName: 'Ada Lovelace' }],
    ),
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

test('renders the injected like control per card and preloads every post id in one pass', async () => {
  stubFetch((request) =>
    jsonResponse(
      pathOf(request) === '/api/feed'
        ? { items: [card({ postId: 'p-1' }), card({ postId: 'p-2' })], nextCursor: null }
        : [{ userId: 'u-1', username: 'ada', displayName: 'Ada' }],
    ),
  );
  const preloaded: string[][] = [];
  renderWithProviders(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route
          path="/"
          element={
            <FeedPage
              renderLike={(postId) => <button type="button">heart {postId}</button>}
              preloadLikes={(_client, postIds) => {
                preloaded.push(postIds);
                return Promise.resolve();
              }}
            />
          }
        />
      </Routes>
    </MemoryRouter>,
  );

  expect(await screen.findByRole('button', { name: 'heart p-1' })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'heart p-2' })).toBeInTheDocument();
  expect(preloaded).toEqual([['p-1', 'p-2']]);
});

test('loads the next page on demand', async () => {
  stubFetch((request) => {
    if (pathOf(request) === '/api/profiles') {
      return jsonResponse([{ userId: 'u-1', username: 'ada', displayName: 'Ada' }]);
    }
    const cursor = new URL(request.url).searchParams.get('cursor');
    return jsonResponse(
      cursor === 'PAGE2'
        ? { items: [card({ postId: 'p-2', caption: 'second page' })], nextCursor: null }
        : { items: [card({ postId: 'p-1', caption: 'first page' })], nextCursor: 'PAGE2' },
    );
  });
  renderFeed();

  expect(await screen.findByText('first page')).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: /load more/i }));

  expect(await screen.findByText('second page', undefined, { timeout: 3000 })).toBeInTheDocument();
  expect(screen.getAllByRole('article')).toHaveLength(2);
  expect(screen.queryByRole('button', { name: /load more/i })).not.toBeInTheDocument();
});

test('renders the injected comment count and opens the post detail from the card', async () => {
  stubFetch((request) =>
    jsonResponse(
      pathOf(request) === '/api/feed'
        ? { items: [card({ postId: 'p-1' })], nextCursor: null }
        : [{ userId: 'u-1', username: 'ada', displayName: 'Ada' }],
    ),
  );

  renderWithProviders(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route
          path="/"
          element={
            <FeedPage
              renderCommentCount={(postId) => <span>{postId} chatter</span>}
              renderPostDetail={(detail, onClose) => (
                <div role="dialog">
                  detail for {detail.postId} by {detail.authorId}
                  <button type="button" onClick={onClose}>
                    x
                  </button>
                </div>
              )}
            />
          }
        />
      </Routes>
    </MemoryRouter>,
  );

  expect(await screen.findByText('p-1 chatter')).toBeInTheDocument();

  fireEvent.click(screen.getByRole('button', { name: 'Open post' }));
  expect(screen.getByRole('dialog')).toHaveTextContent('detail for p-1 by u-1');
});
