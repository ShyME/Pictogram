import { FollowCounts } from '@features/follow/FollowCounts';
import { jsonResponse, stubFetch } from '@test-support/mockFetch';
import { renderWithProviders } from '@test-support/render';
import { screen } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, expect, test, vi } from 'vitest';

vi.mock('react-router', () => ({
  Link: ({ to, children }: { to: string; children: ReactNode }) => <a href={to}>{children}</a>,
}));

afterEach(() => {
  vi.unstubAllGlobals();
});

test('renders the follower and following counts from the relationship query', async () => {
  stubFetch(() => jsonResponse({ followerCount: 42, followingCount: 7, followedByViewer: false }));
  renderWithProviders(<FollowCounts userId="u-1" username="ada" />);

  const followers = await screen.findByText('42');
  expect(followers).toBeInTheDocument();
  expect(screen.getByText('7')).toBeInTheDocument();
  expect(screen.getByText('followers')).toBeInTheDocument();
  expect(screen.getByText('following')).toBeInTheDocument();
});

test('each count links to the matching list screen', async () => {
  stubFetch(() => jsonResponse({ followerCount: 1, followingCount: 2, followedByViewer: false }));
  renderWithProviders(<FollowCounts userId="u-1" username="ada" />);

  await screen.findByText('1');
  expect(screen.getByRole('link', { name: /followers/i })).toHaveAttribute(
    'href',
    '/u/ada/followers',
  );
  expect(screen.getByRole('link', { name: /following/i })).toHaveAttribute(
    'href',
    '/u/ada/following',
  );
});

test('shows zeros before the counts have loaded', () => {
  stubFetch(
    () =>
      new Promise(() => {
        // never resolves: the counts stay in their loading state
      }),
  );
  renderWithProviders(<FollowCounts userId="u-1" username="ada" />);

  expect(screen.getAllByText('0')).toHaveLength(2);
});
