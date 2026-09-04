import { CommentCount } from '@features/comments/CommentCount';
import { jsonResponse, stubFetch } from '@test-support/mockFetch';
import { renderWithProviders } from '@test-support/render';
import { screen } from '@testing-library/react';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

const counts = (commentCount: number) => jsonResponse([{ postId: 'p-1', commentCount }]);

test('renders the real count as static text with no interactive control', async () => {
  stubFetch(() => counts(4));
  renderWithProviders(<CommentCount postId="p-1" />);

  expect(await screen.findByText('4 comments')).toBeInTheDocument();
  expect(screen.queryByRole('button')).not.toBeInTheDocument();
});

test('renders the singular label for a single comment', async () => {
  stubFetch(() => counts(1));
  renderWithProviders(<CommentCount postId="p-1" />);

  expect(await screen.findByText('1 comment')).toBeInTheDocument();
});
