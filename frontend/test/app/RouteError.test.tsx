import { RouteError } from '@app/RouteError';
import * as Sentry from '@sentry/react';
import { render, screen } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { afterEach, expect, test, vi } from 'vitest';

vi.mock('@sentry/react', () => ({ captureException: vi.fn() }));

afterEach(() => {
  vi.mocked(Sentry.captureException).mockClear();
});

function ThrowingRoute(): never {
  throw new Error('boom');
}

function renderAtABrokenRoute() {
  const router = createMemoryRouter(
    [{ path: '/broken', element: <ThrowingRoute />, errorElement: <RouteError /> }],
    { initialEntries: ['/broken'] },
  );
  return render(<RouterProvider router={router} />);
}

test('reports the route error to Sentry', () => {
  renderAtABrokenRoute();

  expect(Sentry.captureException).toHaveBeenCalledTimes(1);
  expect(Sentry.captureException).toHaveBeenCalledWith(
    expect.objectContaining({ message: 'boom' }),
  );
});

test('still renders the fallback message', () => {
  renderAtABrokenRoute();

  expect(screen.getByText('Something went wrong')).toBeInTheDocument();
});
