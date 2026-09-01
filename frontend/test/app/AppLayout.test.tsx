import { AppLayout } from '@app/AppLayout';
import { renderWithProviders } from '@test-support/render';
import { screen } from '@testing-library/react';
import type { ReactNode } from 'react';
import { expect, test, vi } from 'vitest';

vi.mock('react-router', () => ({
  useLoaderData: () => ({
    profile: { userId: 'u-1', username: 'ada', displayName: 'Ada', bio: null },
  }),
  useNavigate: () => vi.fn(),
  Link: ({ to, children }: { to: string; children: ReactNode }) => <a href={to}>{children}</a>,
  Outlet: () => <p>feed content</p>,
}));

test('shows the signed-in username, a sign-out control, and the routed page', () => {
  renderWithProviders(<AppLayout />);

  expect(screen.getByText('@ada')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /sign out/i })).toBeInTheDocument();
  expect(screen.getByText('feed content')).toBeInTheDocument();
});

test("the signed-in handle links to the viewer's own profile page", () => {
  renderWithProviders(<AppLayout />);

  expect(screen.getByRole('link', { name: '@ada' })).toHaveAttribute('href', '/u/ada');
});

test('offers a link to the post composer', () => {
  renderWithProviders(<AppLayout />);

  expect(screen.getByRole('link', { name: /new post/i })).toHaveAttribute('href', '/new');
});
