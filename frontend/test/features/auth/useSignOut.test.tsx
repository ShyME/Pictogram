import { setAccessToken } from '@features/auth/session';
import { useSignOut } from '@features/auth/useSignOut';
import { pathOf, stubFetch } from '@test-support/mockFetch';
import { renderWithProviders } from '@test-support/render';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { afterEach, expect, test, vi } from 'vitest';

const navigate = vi.fn();
vi.mock('react-router', () => ({ useNavigate: () => navigate }));

function SignOutProbe() {
  const { signOut, signingOut } = useSignOut();
  return (
    <button type="button" onClick={signOut} disabled={signingOut}>
      Sign out
    </button>
  );
}

afterEach(() => {
  navigate.mockClear();
  setAccessToken(null);
  vi.unstubAllGlobals();
});

test('ends the session and returns to /login', async () => {
  setAccessToken('live');
  const calls = stubFetch(() => new Response(null, { status: 204 }));
  renderWithProviders(<SignOutProbe />);

  fireEvent.click(screen.getByRole('button', { name: /sign out/i }));

  await waitFor(() => {
    expect(navigate).toHaveBeenCalledWith('/login', { replace: true });
  });
  expect(calls.map((request) => pathOf(request))).toContain('/api/auth/logout');
});

test('disables the control while the sign-out is in flight', async () => {
  const calls = stubFetch(() => new Response(null, { status: 204 }));
  renderWithProviders(<SignOutProbe />);

  fireEvent.click(screen.getByRole('button', { name: /sign out/i }));

  await waitFor(() => {
    expect(screen.getByRole('button', { name: /sign out/i })).toBeDisabled();
  });
  expect(calls).not.toHaveLength(0);
});
