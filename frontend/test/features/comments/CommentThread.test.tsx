import { CommentThread } from '@features/comments/CommentThread';
import { jsonResponse, pathOf, stubFetch } from '@test-support/mockFetch';
import { renderWithProviders } from '@test-support/render';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

const comment = (id: string, body: string, authorId = 'u-ada') => ({
  commentId: id,
  postId: 'p-1',
  authorId,
  body,
  createdAt: '2026-09-04T10:00:00Z',
});

const profiles = () => jsonResponse([{ userId: 'u-ada', username: 'ada', displayName: 'Ada' }]);

function renderThread(
  canComment: boolean,
  extra: { viewerId?: string | null; postAuthorId?: string | null } = {},
) {
  return renderWithProviders(
    <MemoryRouter>
      <CommentThread postId="p-1" canComment={canComment} {...extra} />
    </MemoryRouter>,
  );
}

test('renders the thread oldest-first with the author handle and linkified URLs', async () => {
  stubFetch((request) => {
    if (pathOf(request) === '/api/profiles') return profiles();
    return jsonResponse({
      items: [comment('c-1', 'first at https://pictogram.dev'), comment('c-2', 'second')],
      nextCursor: null,
    });
  });

  renderThread(false);

  const items = await screen.findAllByRole('listitem');
  expect(items).toHaveLength(2);
  expect(items[0]).toHaveTextContent('first at');
  expect(items[1]).toHaveTextContent('second');
  expect(screen.getByRole('link', { name: 'https://pictogram.dev' })).toHaveAttribute(
    'href',
    'https://pictogram.dev',
  );
});

test('shows the composer only when the viewer can comment', async () => {
  stubFetch((request) => {
    if (pathOf(request) === '/api/profiles') return profiles();
    return jsonResponse({ items: [], nextCursor: null });
  });

  const { rerender } = renderThread(false);
  expect(await screen.findByText(/No comments yet/)).toBeInTheDocument();
  expect(screen.queryByLabelText('Add a comment')).not.toBeInTheDocument();

  rerender(
    <MemoryRouter>
      <CommentThread postId="p-1" canComment />
    </MemoryRouter>,
  );
  expect(await screen.findByLabelText('Add a comment')).toBeInTheDocument();
});

test('"Load more comments" fetches the next page with the cursor', async () => {
  const calls = stubFetch((request) => {
    const url = new URL(request.url);
    if (url.pathname === '/api/profiles') return profiles();
    if (url.searchParams.get('cursor') === 'CURSOR') {
      return jsonResponse({ items: [comment('c-2', 'page two')], nextCursor: null });
    }
    return jsonResponse({ items: [comment('c-1', 'page one')], nextCursor: 'CURSOR' });
  });

  renderThread(false);

  fireEvent.click(await screen.findByRole('button', { name: /load more comments/i }));

  await waitFor(() => {
    expect(screen.getByText('page two')).toBeInTheDocument();
  });
  expect(calls.some((call) => new URL(call.url).searchParams.get('cursor') === 'CURSOR')).toBe(
    true,
  );
});

test('shows a delete control only on comments the viewer may remove and calls DELETE', async () => {
  const calls = stubFetch((request) => {
    if (pathOf(request) === '/api/profiles') return profiles();
    if (request.method === 'DELETE') return new Response(null, { status: 204 });
    return jsonResponse({
      items: [comment('c-mine', 'mine', 'u-ada'), comment('c-theirs', 'theirs', 'u-bob')],
      nextCursor: null,
    });
  });

  renderThread(true, { viewerId: 'u-ada', postAuthorId: 'u-zed' });

  const rows = await screen.findAllByRole('listitem');
  expect(within(rows[0]).getByRole('button', { name: 'Delete' })).toBeInTheDocument();
  expect(within(rows[1]).queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument();

  fireEvent.click(within(rows[0]).getByRole('button', { name: 'Delete' }));

  await waitFor(() => {
    expect(
      calls.some((call) => call.method === 'DELETE' && pathOf(call) === '/api/comments/c-mine'),
    ).toBe(true);
  });
});

test('the post author may delete any comment', async () => {
  stubFetch((request) => {
    if (pathOf(request) === '/api/profiles') return profiles();
    return jsonResponse({ items: [comment('c-1', 'hi', 'u-bob')], nextCursor: null });
  });

  renderThread(false, { viewerId: 'u-zed', postAuthorId: 'u-zed' });

  const rows = await screen.findAllByRole('listitem');
  expect(within(rows[0]).getByRole('button', { name: 'Delete' })).toBeInTheDocument();
});
