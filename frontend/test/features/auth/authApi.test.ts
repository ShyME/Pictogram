import { refreshAccessToken, signOut } from '@features/auth/authApi';
import { getAccessToken, setAccessToken } from '@features/auth/session';
import { stubCookieJar } from '@test-support/cookieJar';
import { jsonResponse, pathOf, stubFetch } from '@test-support/mockFetch';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  setAccessToken(null);
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

test('refreshAccessToken stores and returns the new token', async () => {
  stubFetch(() => jsonResponse({ accessToken: 'fresh', expiresInSeconds: 900 }));

  await expect(refreshAccessToken()).resolves.toBe('fresh');
  expect(getAccessToken()).toBe('fresh');
});

test('refreshAccessToken echoes the XSRF-TOKEN cookie as X-XSRF-TOKEN', async () => {
  stubCookieJar('XSRF-TOKEN=csrf-abc');
  const calls = stubFetch(() => jsonResponse({ accessToken: 'fresh', expiresInSeconds: 900 }));

  await refreshAccessToken();

  expect(calls).toHaveLength(1);
  expect(calls[0].headers.get('X-XSRF-TOKEN')).toBe('csrf-abc');
});

test('refreshAccessToken retries once on 403, carrying the token the first call seeded', async () => {
  const seedCookie = stubCookieJar();
  const calls = stubFetch((_request, hits) => {
    if (hits === 0) {
      seedCookie('XSRF-TOKEN=seeded-by-the-403');
      return new Response(null, { status: 403 });
    }
    return jsonResponse({ accessToken: 'fresh', expiresInSeconds: 900 });
  });

  await expect(refreshAccessToken()).resolves.toBe('fresh');
  expect(calls).toHaveLength(2);
  expect(calls[0].headers.get('X-XSRF-TOKEN')).toBeNull();
  expect(calls[1].headers.get('X-XSRF-TOKEN')).toBe('seeded-by-the-403');
});

test('refreshAccessToken gives up after one retry if 403 persists', async () => {
  setAccessToken('stale');
  const calls = stubFetch(() => new Response(null, { status: 403 }));

  await expect(refreshAccessToken()).resolves.toBeNull();
  expect(calls).toHaveLength(2);
  expect(getAccessToken()).toBeNull();
});

test('refreshAccessToken yields null and clears the token when the session is gone', async () => {
  setAccessToken('stale');
  stubFetch(() => jsonResponse({}, 401));

  await expect(refreshAccessToken()).resolves.toBeNull();
  expect(getAccessToken()).toBeNull();
});

test('signOut posts to the logout endpoint and drops the local token', async () => {
  setAccessToken('live');
  const calls = stubFetch(() => new Response(null, { status: 204 }));

  await signOut();

  expect(calls.map((request) => pathOf(request))).toContain('/api/auth/logout');
  expect(calls[0].method).toBe('POST');
  expect(getAccessToken()).toBeNull();
});

test('signOut still clears the token if the logout call fails', async () => {
  setAccessToken('live');
  stubFetch(() => Promise.reject(new Error('offline')));

  await signOut();

  expect(getAccessToken()).toBeNull();
});
