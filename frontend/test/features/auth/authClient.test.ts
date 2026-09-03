import { authMiddleware } from '@features/auth/authClient';
import { getAccessToken, setAccessToken } from '@features/auth/session';
import { api } from '@shared';
import { stubCookieJar } from '@test-support/cookieJar';
import { jsonResponse, pathOf, stubFetch } from '@test-support/mockFetch';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';

beforeEach(() => {
  api.use(authMiddleware);
  setAccessToken(null);
});

afterEach(() => {
  api.eject(authMiddleware);
  setAccessToken(null);
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

test('attaches the current access token as a bearer header', async () => {
  setAccessToken('token-abc');
  const calls = stubFetch(() => jsonResponse({ username: 'ada' }));

  await api.GET('/api/profiles/me');

  expect(calls[0].headers.get('Authorization')).toBe('Bearer token-abc');
});

test('echoes the XSRF-TOKEN cookie as X-XSRF-TOKEN on a mutating call', async () => {
  stubCookieJar('XSRF-TOKEN=csrf-xyz');
  const calls = stubFetch(() => new Response(null, { status: 204 }));

  await api.POST('/api/auth/logout');

  expect(calls[0].headers.get('X-XSRF-TOKEN')).toBe('csrf-xyz');
});

test('does not attach X-XSRF-TOKEN to a GET', async () => {
  stubCookieJar('XSRF-TOKEN=csrf-xyz');
  const calls = stubFetch(() => jsonResponse({ username: 'ada' }));

  await api.GET('/api/profiles/me');

  expect(calls[0].headers.get('X-XSRF-TOKEN')).toBeNull();
});

test('does not attach X-XSRF-TOKEN to a mutating call outside the identity chain', async () => {
  stubCookieJar('XSRF-TOKEN=csrf-xyz');
  const calls = stubFetch(() => new Response(null, { status: 204 }));

  await api.PUT('/api/follows/{userId}', { params: { path: { userId: 'u-1' } } });

  expect(calls[0].headers.get('X-XSRF-TOKEN')).toBeNull();
});

test('retries an identity-chain 403 once with the token the response seeded', async () => {
  const seedCookie = stubCookieJar();
  const calls = stubFetch((_request, hits) => {
    if (hits === 0) {
      seedCookie('XSRF-TOKEN=seeded-by-the-403');
      return new Response(null, { status: 403 });
    }
    return new Response(null, { status: 204 });
  });

  const { response } = await api.POST('/api/auth/logout');

  expect(response.status).toBe(204);
  expect(calls).toHaveLength(2);
  expect(calls[0].headers.get('X-XSRF-TOKEN')).toBeNull();
  expect(calls[1].headers.get('X-XSRF-TOKEN')).toBe('seeded-by-the-403');
});

test('gives up after one 403 retry on the identity chain', async () => {
  stubCookieJar('XSRF-TOKEN=stale');
  const calls = stubFetch(() => new Response(null, { status: 403 }));

  const { response } = await api.POST('/api/auth/logout');

  expect(response.status).toBe(403);
  expect(calls).toHaveLength(2);
});

test('on 401 it refreshes once and retries the request with the new token', async () => {
  setAccessToken('stale');
  const calls = stubFetch((request, hits) => {
    if (pathOf(request) === '/api/auth/refresh') {
      return jsonResponse({ accessToken: 'fresh', expiresInSeconds: 900 });
    }
    return hits === 0 ? jsonResponse({}, 401) : jsonResponse({ username: 'ada' });
  });

  const { data, response } = await api.GET('/api/profiles/me');

  expect(response.status).toBe(200);
  expect(data).toEqual({ username: 'ada' });
  expect(calls.filter((c) => pathOf(c) === '/api/auth/refresh')).toHaveLength(1);
  expect(calls.at(-1)?.headers.get('Authorization')).toBe('Bearer fresh');
  expect(getAccessToken()).toBe('fresh');
});

test('when the refresh fails it returns the 401 and drops the token', async () => {
  setAccessToken('stale');
  const calls = stubFetch(() => jsonResponse({}, 401));

  const { response } = await api.GET('/api/profiles/me');

  expect(response.status).toBe(401);
  expect(getAccessToken()).toBeNull();
  expect(calls.filter((c) => pathOf(c) === '/api/profiles/me')).toHaveLength(1);
});

test('concurrent 401s share a single refresh round-trip', async () => {
  setAccessToken('stale');
  const calls = stubFetch((request, hits) => {
    if (pathOf(request) === '/api/auth/refresh') {
      return jsonResponse({ accessToken: 'fresh', expiresInSeconds: 900 });
    }
    if (hits === 0) return jsonResponse({}, 401);
    return jsonResponse(pathOf(request) === '/api/feed' ? { items: [] } : { username: 'ada' });
  });

  await Promise.all([api.GET('/api/profiles/me'), api.GET('/api/feed')]);

  expect(calls.filter((c) => pathOf(c) === '/api/auth/refresh')).toHaveLength(1);
});
