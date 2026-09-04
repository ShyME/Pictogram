import { PublicLayout } from '@app/PublicLayout';
import { renderWithProviders } from '@test-support/render';
import { screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { expect, test, vi } from 'vitest';

const viewer = vi.hoisted(() => ({ current: null as { username: string } | null }));

vi.mock('react-router', async (importOriginal) => ({
  ...(await importOriginal<typeof import('react-router')>()),
  useLoaderData: () => ({ viewer: viewer.current }),
}));

function renderAt() {
  return renderWithProviders(
    <MemoryRouter initialEntries={['/u/grace']}>
      <Routes>
        <Route element={<PublicLayout />}>
          <Route path="u/:username" element={<p>profile content</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

test('shows the signed-in nav when there is a viewer', () => {
  viewer.current = { username: 'ada' };
  renderAt();

  expect(screen.getByText('profile content')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /open menu/i })).toBeInTheDocument();
  expect(screen.queryByRole('link', { name: /log in/i })).not.toBeInTheDocument();
});

test('shows the logged-out nav for an anonymous visitor', () => {
  viewer.current = null;
  renderAt();

  expect(screen.getByText('profile content')).toBeInTheDocument();
  expect(screen.getByRole('link', { name: /log in/i })).toHaveAttribute('href', '/login');
  expect(screen.queryByRole('button', { name: /open menu/i })).not.toBeInTheDocument();
});
