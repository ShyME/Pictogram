import { LikeCount } from '@features/engagement/LikeCount';
import { jsonResponse, stubFetch } from '@test-support/mockFetch';
import { renderWithProviders } from '@test-support/render';
import { screen } from '@testing-library/react';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

const likes = (likeCount: number) =>
  jsonResponse([{ postId: 'p-1', likeCount, likedByViewer: false }]);

test('renders the real count as static text with no interactive control', async () => {
  stubFetch(() => likes(4));
  renderWithProviders(<LikeCount postId="p-1" />);

  expect(await screen.findByText('4 likes')).toBeInTheDocument();
  expect(screen.queryByRole('button')).not.toBeInTheDocument();
});

test('renders the singular label for a single like', async () => {
  stubFetch(() => likes(1));
  renderWithProviders(<LikeCount postId="p-1" />);

  expect(await screen.findByText('1 like')).toBeInTheDocument();
});
