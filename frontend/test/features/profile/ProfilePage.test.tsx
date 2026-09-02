import { ProfilePage } from '@features/profile/ProfilePage';
import type { ProfilePageData } from '@features/profile/profileLoader';
import { renderWithProviders } from '@test-support/render';
import { screen } from '@testing-library/react';
import type { ReactNode } from 'react';
import { expect, test, vi } from 'vitest';

let loaderData: ProfilePageData;

vi.mock('react-router', () => ({
  useLoaderData: () => loaderData,
  Link: ({ to, children }: { to: string; children: ReactNode }) => <a href={to}>{children}</a>,
}));

const found = (
  over: Partial<Extract<ProfilePageData, { status: 'found' }>> = {},
): ProfilePageData => ({
  status: 'found',
  profile: {
    userId: 'u-1',
    username: 'ada_lovelace',
    displayName: 'Ada Lovelace',
    bio: 'Countess of Lovelace',
  },
  isOwnProfile: false,
  viewerCanFollow: true,
  ...over,
});

test('shows the display name, handle, bio and follower/following slots', () => {
  loaderData = found();
  renderWithProviders(<ProfilePage />);

  expect(screen.getByRole('heading', { name: 'Ada Lovelace' })).toBeInTheDocument();
  expect(screen.getByText('@ada_lovelace')).toBeInTheDocument();
  expect(screen.getByText('Countess of Lovelace')).toBeInTheDocument();
  expect(screen.getByText('followers')).toBeInTheDocument();
  expect(screen.getByText('following')).toBeInTheDocument();
});

test('falls back to the handle as the heading when there is no display name', () => {
  loaderData = found({
    profile: { userId: 'u-1', username: 'ada_lovelace', displayName: null, bio: null },
  });
  renderWithProviders(<ProfilePage />);

  expect(screen.getByRole('heading', { name: '@ada_lovelace' })).toBeInTheDocument();
});

test("another user's profile shows a follow slot, not an edit slot", () => {
  loaderData = found({ isOwnProfile: false });
  renderWithProviders(<ProfilePage />);

  expect(screen.getByRole('button', { name: /follow/i })).toBeInTheDocument();
  expect(screen.queryByRole('link', { name: /edit profile/i })).not.toBeInTheDocument();
});

test('a signed-out visitor sees a sign-in link where the follow button would be', () => {
  loaderData = found({ isOwnProfile: false, viewerCanFollow: false });
  renderWithProviders(
    <ProfilePage renderFollowButton={(userId) => <button type="button">follow {userId}</button>} />,
  );

  expect(screen.getByRole('link', { name: /follow/i })).toHaveAttribute('href', '/login');
  expect(screen.queryByRole('button', { name: /follow/i })).not.toBeInTheDocument();
});

test("the viewer's own profile shows an edit link to the settings page, not a follow slot", () => {
  loaderData = found({ isOwnProfile: true });
  renderWithProviders(<ProfilePage />);

  expect(screen.getByRole('link', { name: /edit profile/i })).toHaveAttribute(
    'href',
    '/settings/profile',
  );
  expect(screen.queryByRole('button', { name: /follow/i })).not.toBeInTheDocument();
});

test('renders the app-supplied grid on every profile, telling it whether the viewer owns it', () => {
  const grid = (authorId: string, isOwn: boolean) => (
    <div>
      grid for {authorId} ({isOwn ? 'own' : 'visitor'})
    </div>
  );

  loaderData = found({ isOwnProfile: true });
  const { rerender } = renderWithProviders(<ProfilePage renderGrid={grid} />);
  expect(screen.getByText('grid for u-1 (own)')).toBeInTheDocument();

  loaderData = found({ isOwnProfile: false });
  rerender(<ProfilePage renderGrid={grid} />);
  expect(screen.getByText('grid for u-1 (visitor)')).toBeInTheDocument();
});

test("renders the app-supplied follow button on another user's profile, not on your own", () => {
  loaderData = found({ isOwnProfile: false });
  const { rerender } = renderWithProviders(
    <ProfilePage renderFollowButton={(userId) => <button type="button">follow {userId}</button>} />,
  );
  expect(screen.getByRole('button', { name: 'follow u-1' })).toBeInTheDocument();

  loaderData = found({ isOwnProfile: true });
  rerender(
    <ProfilePage renderFollowButton={(userId) => <button type="button">follow {userId}</button>} />,
  );
  expect(screen.queryByRole('button', { name: /follow/i })).not.toBeInTheDocument();
  expect(screen.getByRole('link', { name: /edit profile/i })).toBeInTheDocument();
});

test('renders the app-supplied count row on every profile', () => {
  loaderData = found({ isOwnProfile: true });
  renderWithProviders(<ProfilePage renderFollowCounts={(userId) => <p>counts for {userId}</p>} />);
  expect(screen.getByText('counts for u-1')).toBeInTheDocument();
});

test('an unknown username renders a clear not-found page', () => {
  loaderData = { status: 'not-found', username: 'ghost_user' };
  renderWithProviders(<ProfilePage />);

  expect(screen.getByRole('heading', { name: /doesn.t exist/i })).toBeInTheDocument();
  expect(screen.getByText('@ghost_user')).toBeInTheDocument();
});
