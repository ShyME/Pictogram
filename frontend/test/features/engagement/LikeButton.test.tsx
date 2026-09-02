import { LikeButton } from '@features/engagement/LikeButton';
import { jsonResponse, pathOf, stubFetch } from '@test-support/mockFetch';
import { renderWithProviders } from '@test-support/render';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

const likes = (isLiked: boolean, likeCount: number) =>
  jsonResponse([{ postId: 'p-1', likeCount, likedByViewer: isLiked }]);

async function findEnabled(name: string) {
  const button = await screen.findByRole('button', { name });
  await waitFor(() => {
    expect(button).toBeEnabled();
  });
  return button;
}

test('shows the count and an unpressed heart when the viewer has not liked', async () => {
  stubFetch(() => likes(false, 3));
  renderWithProviders(<LikeButton postId="p-1" />);

  expect(await screen.findByText('3 likes')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Like' })).toHaveAttribute('aria-pressed', 'false');
});

test('shows a pressed heart when the viewer has liked', async () => {
  stubFetch(() => likes(true, 1));
  renderWithProviders(<LikeButton postId="p-1" />);

  expect(await screen.findByRole('button', { name: 'Unlike' })).toHaveAttribute(
    'aria-pressed',
    'true',
  );
  expect(screen.getByText('1 like')).toBeInTheDocument();
});

test('clicking an unpressed heart PUTs the like and flips once the count reloads', async () => {
  const calls = stubFetch((request) => {
    if (request.method === 'PUT') return new Response(null, { status: 204 });
    const isLiked = calls.some((c) => c.method === 'PUT');
    return likes(isLiked, isLiked ? 4 : 3);
  });
  renderWithProviders(<LikeButton postId="p-1" />);

  fireEvent.click(await findEnabled('Like'));

  expect(await screen.findByRole('button', { name: 'Unlike' })).toBeInTheDocument();
  expect(await screen.findByText('4 likes')).toBeInTheDocument();
  const put = calls.find((c) => c.method === 'PUT');
  if (!put) throw new Error('expected a PUT request');
  expect(pathOf(put)).toBe('/api/engagement/likes/p-1');
});

test('clicking a pressed heart DELETEs the like', async () => {
  const calls = stubFetch((request) => {
    if (request.method === 'DELETE') return new Response(null, { status: 204 });
    const isUnliked = calls.some((c) => c.method === 'DELETE');
    return likes(!isUnliked, isUnliked ? 0 : 1);
  });
  renderWithProviders(<LikeButton postId="p-1" />);

  fireEvent.click(await findEnabled('Unlike'));

  expect(await screen.findByRole('button', { name: 'Like' })).toBeInTheDocument();
  expect(
    calls.some((c) => c.method === 'DELETE' && pathOf(c) === '/api/engagement/likes/p-1'),
  ).toBe(true);
});

test('surfaces an error when the like write fails', async () => {
  stubFetch((request) => {
    if (request.method === 'PUT') return new Response(null, { status: 500 });
    return likes(false, 0);
  });
  renderWithProviders(<LikeButton postId="p-1" />);

  fireEvent.click(await findEnabled('Like'));

  await waitFor(() => {
    expect(screen.getByRole('alert')).toHaveTextContent(/didn.t work/i);
  });
});
