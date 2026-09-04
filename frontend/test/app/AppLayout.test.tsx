import { AppLayout } from '@app/AppLayout';
import { renderWithProviders } from '@test-support/render';
import { fireEvent, screen, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useOutletContext } from 'react-router';
import { expect, test, vi } from 'vitest';

vi.mock('react-router', async (importOriginal) => ({
  ...(await importOriginal<typeof import('react-router')>()),
  useLoaderData: () => ({
    profile: { userId: 'u-1', username: 'ada', displayName: 'Ada', bio: null },
  }),
}));

// jsdom's matchMedia stub reports the narrow layout, so the nav actions sit in the menu.
test('mounts the app nav for the signed-in viewer and renders the routed page', () => {
  renderWithProviders(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route element={<AppLayout />}>
          <Route index element={<p>feed content</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );

  expect(screen.getByText('feed content')).toBeInTheDocument();

  fireEvent.keyDown(screen.getByRole('button', { name: /open menu/i }), { key: 'Enter' });
  const menu = screen.getByRole('menu');

  expect(within(menu).getByRole('menuitem', { name: /your profile/i })).toHaveAttribute(
    'href',
    '/u/ada',
  );
  expect(within(menu).getByRole('menuitem', { name: /new post/i })).toHaveAttribute('href', '/new');
  expect(within(menu).getByRole('menuitem', { name: /sign out/i })).toBeInTheDocument();
});

function ShowsHandle() {
  const { profile } = useOutletContext<{ profile: { username: string } }>();
  return <p>context: {profile.username}</p>;
}

test('hands the signed-in profile to nested routes through the outlet context', () => {
  renderWithProviders(
    <MemoryRouter initialEntries={['/new']}>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="new" element={<ShowsHandle />} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );

  expect(screen.getByText('context: ada')).toBeInTheDocument();
});
