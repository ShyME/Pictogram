import { NotificationsBell } from '@features/notifications/NotificationsBell';
import { jsonResponse, stubFetch } from '@test-support/mockFetch';
import { renderWithProviders } from '@test-support/render';
import { screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

function renderBell() {
  return renderWithProviders(
    <MemoryRouter>
      <NotificationsBell />
    </MemoryRouter>,
  );
}

test('links to the notifications screen', async () => {
  stubFetch(() => jsonResponse({ count: 0 }));
  renderBell();

  expect(await screen.findByRole('link', { name: /notifications/i })).toHaveAttribute(
    'href',
    '/notifications',
  );
});

test('shows no badge at zero', async () => {
  stubFetch(() => jsonResponse({ count: 0 }));
  renderBell();

  await screen.findByRole('link', { name: 'Notifications' });
  expect(screen.queryByText(/^\d/)).not.toBeInTheDocument();
});

test('shows the exact count up to nine', async () => {
  stubFetch(() => jsonResponse({ count: 3 }));
  renderBell();

  expect(await screen.findByText('3')).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Notifications, 3 unread' })).toBeInTheDocument();
});

test('caps the badge at 9+', async () => {
  stubFetch(() => jsonResponse({ count: 42 }));
  renderBell();

  expect(await screen.findByText('9+')).toBeInTheDocument();
});
