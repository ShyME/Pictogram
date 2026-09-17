import { FeedRoute } from '@app/FeedRoute';
import { jsonResponse, pathOf, stubFetch } from '@test-support/mockFetch';
import { renderWithProviders } from '@test-support/render';
import { fireEvent, screen } from '@testing-library/react';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router';
import { afterEach, expect, test, vi } from 'vitest';

const profile = { userId: 'u-1', username: 'ada', displayName: 'Ada', bio: null };

function StubAppLayoutContext() {
  return <Outlet context={{ profile }} />;
}

function renderFeedRoute() {
  return renderWithProviders(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route element={<StubAppLayoutContext />}>
          <Route index element={<FeedRoute />} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

afterEach(() => {
  localStorage.clear();
  vi.unstubAllGlobals();
});

test('defaults to Following and calls /api/feed', async () => {
  const requests = stubFetch(() => jsonResponse({ items: [], nextCursor: null }));
  renderFeedRoute();

  expect(await screen.findByText(/find people to follow/i)).toBeInTheDocument();
  expect(screen.getByRole('tab', { name: 'Following' })).toHaveAttribute('aria-selected', 'true');
  expect(requests.some((r) => pathOf(r) === '/api/feed')).toBe(true);
  expect(requests.some((r) => pathOf(r) === '/api/explore')).toBe(false);
});

test('switching to Explore calls /api/explore and remembers the choice', async () => {
  const requests = stubFetch(() => jsonResponse({ items: [], nextCursor: null }));
  renderFeedRoute();
  await screen.findByText(/find people to follow/i);

  fireEvent.click(screen.getByRole('tab', { name: 'Explore' }));

  await screen.findByText(/no posts yet/i);
  expect(requests.some((r) => pathOf(r) === '/api/explore')).toBe(true);
  expect(localStorage.getItem('pictogram.feed-scope')).toBe('explore');
});

test('an earlier Explore choice is honoured on the next visit', async () => {
  localStorage.setItem('pictogram.feed-scope', 'explore');
  const requests = stubFetch(() => jsonResponse({ items: [], nextCursor: null }));
  renderFeedRoute();

  await screen.findByText(/no posts yet/i);
  expect(screen.getByRole('tab', { name: 'Explore' })).toHaveAttribute('aria-selected', 'true');
  expect(requests.some((r) => pathOf(r) === '/api/feed')).toBe(false);
});
