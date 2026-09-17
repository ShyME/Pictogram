import { loginLoader, onboardingLoader, rootLoader, viewerLoader } from '@app/guards';
import { jsonResponse, problemResponse, stubFetch } from '@test-support/mockFetch';
import type { LoaderFunctionArgs } from 'react-router';
import { afterEach, expect, test, vi } from 'vitest';

function profileEndpoint(status: number, body: unknown = {}) {
  const slug = status === 404 ? 'profile-not-found' : 'unauthorized';
  stubFetch(() => (status === 200 ? jsonResponse(body) : problemResponse(slug, status)));
}

function redirectTarget(result: unknown): string | null {
  return result instanceof Response ? result.headers.get('Location') : null;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

function loaderArgs(pathname: string): LoaderFunctionArgs {
  return { request: new Request(`http://localhost${pathname}`) } as LoaderFunctionArgs;
}

test('rootLoader on the home route: 401 -> /explore, 404 -> /onboarding, 200 -> the profile', async () => {
  const home = loaderArgs('/');

  profileEndpoint(401);
  expect(redirectTarget(await rootLoader(home))).toBe('/explore');

  profileEndpoint(404);
  expect(redirectTarget(await rootLoader(home))).toBe('/onboarding');

  profileEndpoint(200, { userId: 'u-1', username: 'ada' });
  expect(await rootLoader(home)).toEqual({
    profile: { userId: 'u-1', username: 'ada', displayName: null, bio: null },
  });
});

test('rootLoader on any other AppLayout route sends a signed-out visitor to /login, not /explore', async () => {
  const settings = loaderArgs('/settings/profile');

  profileEndpoint(401);
  expect(redirectTarget(await rootLoader(settings))).toBe('/login');

  profileEndpoint(404);
  expect(redirectTarget(await rootLoader(settings))).toBe('/onboarding');

  profileEndpoint(200, { userId: 'u-1', username: 'ada' });
  expect(await rootLoader(settings)).toEqual({
    profile: { userId: 'u-1', username: 'ada', displayName: null, bio: null },
  });
});

test('loginLoader bounces an onboarded user to the feed and a half-onboarded one to onboarding', async () => {
  profileEndpoint(200, { userId: 'u-1', username: 'ada' });
  expect(redirectTarget(await loginLoader())).toBe('/');

  profileEndpoint(404);
  expect(redirectTarget(await loginLoader())).toBe('/onboarding');

  profileEndpoint(401);
  expect(await loginLoader()).toBeNull();
});

test('onboardingLoader requires a session and is skipped once a profile exists', async () => {
  profileEndpoint(401);
  expect(redirectTarget(await onboardingLoader())).toBe('/login');

  profileEndpoint(200, { userId: 'u-1', username: 'ada' });
  expect(redirectTarget(await onboardingLoader())).toBe('/');

  profileEndpoint(404);
  expect(await onboardingLoader()).toBeNull();
});

test('viewerLoader: the onboarded profile, or null for an anonymous or half-onboarded visitor', async () => {
  profileEndpoint(200, { userId: 'u-1', username: 'ada' });
  expect(await viewerLoader()).toEqual({
    viewer: { userId: 'u-1', username: 'ada', displayName: null, bio: null },
  });

  profileEndpoint(401);
  expect(await viewerLoader()).toEqual({ viewer: null });

  profileEndpoint(404);
  expect(await viewerLoader()).toEqual({ viewer: null });
});
