import { AccountList } from '@features/follow/lists/AccountList';
import { render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';
import { expect, test, vi } from 'vitest';

vi.mock('react-router', () => ({
  Link: ({ to, children }: { to: string; children: ReactNode }) => <a href={to}>{children}</a>,
}));

vi.mock('@features/follow/FollowButton', () => ({
  FollowButton: ({ userId }: { userId: string }) => <button type="button">follow {userId}</button>,
}));

const accounts = [
  { userId: 'u-9', username: 'ada', displayName: 'Ada Lovelace' },
  { userId: 'u-3', username: 'carol', displayName: null },
];

test('renders each account with its name, handle and a link to the profile', () => {
  render(<AccountList accounts={accounts} viewerId="viewer" />);

  expect(screen.getByText('Ada Lovelace')).toBeInTheDocument();
  expect(screen.getByRole('link', { name: /Ada Lovelace/ })).toHaveAttribute('href', '/u/ada');
  expect(screen.getByRole('link', { name: /@carol/ })).toHaveAttribute('href', '/u/carol');
});

test("every row but the viewer's own carries a follow control", () => {
  render(<AccountList accounts={accounts} viewerId="u-3" />);

  expect(screen.getByRole('button', { name: 'follow u-9' })).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'follow u-3' })).not.toBeInTheDocument();
});
