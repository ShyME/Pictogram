import { AppLayout } from '@app/AppLayout';
import { jsonResponse, pathOf, stubFetch } from '@test-support/mockFetch';
import { renderWithProviders } from '@test-support/render';
import { stubWebSocket } from '@test-support/stubWebSocket';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useOutletContext } from 'react-router';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';

vi.mock('react-router', async (importOriginal) => ({
  ...(await importOriginal<typeof import('react-router')>()),
  useLoaderData: () => ({
    profile: { userId: 'u-1', username: 'ada', displayName: 'Ada', bio: null },
  }),
}));

beforeEach(() => {
  stubWebSocket();
  stubFetch((request) => {
    if (pathOf(request) === '/api/follows/u-1/following')
      return jsonResponse({ items: ['u-2'], nextCursor: null });
    if (pathOf(request) === '/api/profiles')
      return jsonResponse([{ userId: 'u-2', username: 'grace', displayName: 'Grace Hopper' }]);
    return jsonResponse({});
  });
});

afterEach(() => {
  localStorage.clear();
  vi.unstubAllGlobals();
});

function renderLayout(entry = '/') {
  return renderWithProviders(
    <MemoryRouter initialEntries={[entry]}>
      <Routes>
        <Route element={<AppLayout />}>
          <Route index element={<p>feed content</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

// jsdom's matchMedia stub reports the narrow layout, so the nav actions sit in the menu
// and the chat rail is a drawer behind the nav's chat icon.
test('mounts the app nav for the signed-in viewer and renders the routed page', () => {
  renderLayout();

  expect(screen.getByText('feed content')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /open messages/i })).toBeInTheDocument();

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

test('the nav chat icon opens the rail drawer, listing who the viewer follows', async () => {
  renderLayout();

  expect(screen.queryByRole('complementary', { name: 'Messages' })).not.toBeInTheDocument();

  fireEvent.click(screen.getByRole('button', { name: /open messages/i }));

  const drawer = await screen.findByRole('complementary', { name: 'Messages' });
  expect(await within(drawer).findByText('Grace Hopper')).toBeInTheDocument();

  fireEvent.click(within(drawer).getByRole('button', { name: /close messages/i }));
  await waitFor(() => {
    expect(screen.queryByRole('complementary', { name: 'Messages' })).not.toBeInTheDocument();
  });
});
