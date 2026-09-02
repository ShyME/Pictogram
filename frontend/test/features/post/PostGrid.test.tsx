import { PostGrid } from '@features/post/PostGrid';
import { jsonResponse, pathOf, problemResponse, stubFetch } from '@test-support/mockFetch';
import { renderWithProviders } from '@test-support/render';
import { fireEvent, screen, within } from '@testing-library/react';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

test('shows an empty state when the author has no posts', async () => {
  stubFetch(() => jsonResponse({ items: [], nextCursor: null }));
  renderWithProviders(<PostGrid authorId="u-1" />);

  expect(await screen.findByText('No posts yet')).toBeInTheDocument();
});

test("renders each post's thumbnail, newest first as the server sends them", async () => {
  stubFetch(() =>
    jsonResponse({
      items: [
        { postId: 'p-2', mediaId: 'm-2', caption: 'later', publishedAt: 't2' },
        { postId: 'p-1', mediaId: 'm-1', caption: null, publishedAt: 't1' },
      ],
      nextCursor: null,
    }),
  );
  renderWithProviders(<PostGrid authorId="u-1" />);

  const images = await screen.findAllByRole('img');
  expect(images.map((img) => img.getAttribute('src'))).toEqual([
    '/api/media/m-2/thumbnail',
    '/api/media/m-1/thumbnail',
  ]);
});

test('a plain viewer sees the posts but no delete controls', async () => {
  stubFetch(() =>
    jsonResponse({
      items: [{ postId: 'p-1', mediaId: 'm-1', caption: 'hi', publishedAt: 't1' }],
      nextCursor: null,
    }),
  );
  renderWithProviders(<PostGrid authorId="u-1" />);

  expect(await screen.findByAltText('hi')).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument();
});

test('the owner deletes a post after confirming, then the grid refetches', async () => {
  const calls = stubFetch((request) => {
    if (request.method === 'DELETE') return new Response(null, { status: 204 });
    const seenDelete = calls.some((c) => c.method === 'DELETE');
    return jsonResponse({
      items: seenDelete
        ? []
        : [{ postId: 'p-1', mediaId: 'm-1', caption: 'bye', publishedAt: 't1' }],
      nextCursor: null,
    });
  });
  renderWithProviders(<PostGrid authorId="u-1" manageable />);

  fireEvent.click(await screen.findByRole('button', { name: 'Delete' }));

  const dialog = screen.getByRole('alertdialog');
  expect(within(dialog).getByText(/can.t be undone/i)).toBeInTheDocument();
  fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }));

  expect(await screen.findByText('No posts yet')).toBeInTheDocument();
  const del = calls.find((c) => c.method === 'DELETE');
  if (!del) throw new Error('expected a DELETE request');
  expect(pathOf(del)).toBe('/api/posts/p-1');
});

test('cancelling the confirmation leaves the post and sends nothing', async () => {
  const calls = stubFetch(() =>
    jsonResponse({
      items: [{ postId: 'p-1', mediaId: 'm-1', caption: 'stay', publishedAt: 't1' }],
      nextCursor: null,
    }),
  );
  renderWithProviders(<PostGrid authorId="u-1" manageable />);

  fireEvent.click(await screen.findByRole('button', { name: 'Delete' }));
  fireEvent.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: /cancel/i }));

  expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
  expect(await screen.findByAltText('stay')).toBeInTheDocument();
  expect(calls.some((c) => c.method === 'DELETE')).toBe(false);
});

test('a failed delete keeps the dialog open with an error', async () => {
  stubFetch((request) =>
    request.method === 'DELETE'
      ? problemResponse('forbidden', 403)
      : jsonResponse({
          items: [{ postId: 'p-1', mediaId: 'm-1', publishedAt: 't1' }],
          nextCursor: null,
        }),
  );
  renderWithProviders(<PostGrid authorId="u-1" manageable />);

  fireEvent.click(await screen.findByRole('button', { name: 'Delete' }));
  fireEvent.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Delete' }));

  expect(await within(screen.getByRole('alertdialog')).findByRole('alert')).toHaveTextContent(
    /didn.t work/i,
  );
});

test("pages on the keyset cursor when 'Load more' is clicked", async () => {
  const calls = stubFetch((_request, hits) =>
    hits === 0
      ? jsonResponse({
          items: [{ postId: 'p-2', mediaId: 'm-2', publishedAt: 't2' }],
          nextCursor: 'CURSOR',
        })
      : jsonResponse({
          items: [{ postId: 'p-1', mediaId: 'm-1', publishedAt: 't1' }],
          nextCursor: null,
        }),
  );
  renderWithProviders(<PostGrid authorId="u-1" />);

  fireEvent.click(await screen.findByRole('button', { name: /load more/i }));

  expect(await screen.findByAltText('A post')).toBeInTheDocument();
  expect(screen.getAllByRole('img')).toHaveLength(2);
  expect(new URL(calls[1].url).searchParams.get('cursor')).toBe('CURSOR');
  expect(screen.queryByRole('button', { name: /load more/i })).not.toBeInTheDocument();
});
