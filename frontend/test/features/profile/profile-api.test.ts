import {
  fetchMyProfile,
  fetchProfileByUsername,
  submitOnboarding,
  submitProfileEdit,
} from '@features/profile/profile-api';
import { jsonResponse, problemResponse, stubFetch } from '@test-support/mock-fetch';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

test('fetchProfileByUsername returns the public profile on 200', async () => {
  stubFetch(() =>
    jsonResponse({ userId: 'u-1', username: 'ada_lovelace', displayName: 'Ada', bio: 'hi' }),
  );

  await expect(fetchProfileByUsername('ada_lovelace')).resolves.toEqual({
    status: 'found',
    profile: { userId: 'u-1', username: 'ada_lovelace', displayName: 'Ada', bio: 'hi' },
  });
});

test('fetchProfileByUsername maps a profile-not-found 404 to not-found', async () => {
  stubFetch(() => problemResponse('profile-not-found', 404));

  await expect(fetchProfileByUsername('ghost_user')).resolves.toEqual({ status: 'not-found' });
});

test('fetchProfileByUsername retries anonymously when a stale token makes the public read 401', async () => {
  stubFetch((request, hits) => {
    if (new URL(request.url).pathname === '/api/auth/refresh') {
      return problemResponse('unauthorized', 401);
    }
    return hits === 0
      ? problemResponse('unauthorized', 401)
      : jsonResponse({ userId: 'u-1', username: 'ada_lovelace' });
  });

  await expect(fetchProfileByUsername('ada_lovelace')).resolves.toEqual({
    status: 'found',
    profile: { userId: 'u-1', username: 'ada_lovelace', displayName: null, bio: null },
  });
});

test("fetchProfileByUsername maps the anonymous retry's 404 to not-found", async () => {
  stubFetch((request, hits) => {
    if (new URL(request.url).pathname === '/api/auth/refresh') {
      return problemResponse('unauthorized', 401);
    }
    return hits === 0
      ? problemResponse('unauthorized', 401)
      : problemResponse('profile-not-found', 404);
  });

  await expect(fetchProfileByUsername('ghost_user')).resolves.toEqual({ status: 'not-found' });
});

test('fetchProfileByUsername requests the username as a path parameter', async () => {
  const calls = stubFetch(() => jsonResponse({ userId: 'u-1', username: 'ada_lovelace' }));

  await fetchProfileByUsername('ada_lovelace');

  expect(new URL(calls[0].url).pathname).toBe('/api/profiles/ada_lovelace');
});

test('fetchMyProfile maps 200 to an onboarded profile', async () => {
  stubFetch(() => jsonResponse({ userId: 'u-1', username: 'ada', displayName: 'Ada', bio: null }));

  await expect(fetchMyProfile()).resolves.toEqual({
    status: 'onboarded',
    profile: { userId: 'u-1', username: 'ada', displayName: 'Ada', bio: null },
  });
});

test('fetchMyProfile maps 404 to not-onboarded', async () => {
  stubFetch(() => problemResponse('profile-not-found', 404));

  await expect(fetchMyProfile()).resolves.toEqual({ status: 'not-onboarded' });
});

test('fetchMyProfile maps 401 to unauthenticated', async () => {
  stubFetch(() => problemResponse('unauthorized', 401));

  await expect(fetchMyProfile()).resolves.toEqual({ status: 'unauthenticated' });
});

test('submitOnboarding returns the created profile on 201', async () => {
  stubFetch(() =>
    jsonResponse({ userId: 'u-1', username: 'ada_lovelace', displayName: null, bio: null }, 201),
  );

  await expect(submitOnboarding({ username: 'ada_lovelace' })).resolves.toEqual({
    status: 'created',
    profile: { userId: 'u-1', username: 'ada_lovelace', displayName: null, bio: null },
  });
});

test('submitOnboarding distinguishes a taken username from a malformed one', async () => {
  stubFetch(() => problemResponse('username-taken', 409));
  await expect(submitOnboarding({ username: 'ada' })).resolves.toEqual({
    status: 'username-taken',
  });

  stubFetch(() => problemResponse('username-invalid', 400));
  await expect(submitOnboarding({ username: 'ada' })).resolves.toEqual({
    status: 'username-invalid',
  });
});

test('submitOnboarding surfaces an over-long display name or bio', async () => {
  stubFetch(() => problemResponse('profile-details-invalid', 400));

  await expect(submitOnboarding({ username: 'ada', bio: 'x' })).resolves.toEqual({
    status: 'details-invalid',
  });
});

test('submitOnboarding treats an existing profile as already-onboarded, not an error', async () => {
  stubFetch(() => problemResponse('already-onboarded', 409));

  await expect(submitOnboarding({ username: 'ada' })).resolves.toEqual({
    status: 'already-onboarded',
  });
});

test('submitOnboarding omits blank optional fields from the request body', async () => {
  let sentBody: unknown;
  stubFetch(async (request) => {
    sentBody = await request.clone().json();
    return jsonResponse({ userId: 'u-1', username: 'ada_lovelace' }, 201);
  });

  await submitOnboarding({ username: 'ada_lovelace', displayName: '  ', bio: '' });

  expect(sentBody).toEqual({ username: 'ada_lovelace' });
});

test('submitProfileEdit PUTs to /api/profiles/me and returns the updated profile', async () => {
  const calls = stubFetch(() =>
    jsonResponse({ userId: 'u-1', username: 'ada_lovelace', displayName: 'Ada L.', bio: 'hi' }),
  );

  await expect(
    submitProfileEdit({ username: 'ada_lovelace', displayName: 'Ada L.', bio: 'hi' }),
  ).resolves.toEqual({
    status: 'updated',
    profile: { userId: 'u-1', username: 'ada_lovelace', displayName: 'Ada L.', bio: 'hi' },
  });
  expect(calls[0].method).toBe('PUT');
  expect(new URL(calls[0].url).pathname).toBe('/api/profiles/me');
});

test('submitProfileEdit distinguishes a taken username, a malformed one, and bad details', async () => {
  stubFetch(() => problemResponse('username-taken', 409));
  await expect(submitProfileEdit({ username: 'grace' })).resolves.toEqual({
    status: 'username-taken',
  });

  stubFetch(() => problemResponse('username-invalid', 400));
  await expect(submitProfileEdit({ username: 'No Good' })).resolves.toEqual({
    status: 'username-invalid',
  });

  stubFetch(() => problemResponse('profile-details-invalid', 400));
  await expect(submitProfileEdit({ username: 'ada', bio: 'x' })).resolves.toEqual({
    status: 'details-invalid',
  });
});

test('submitProfileEdit maps a missing profile to not-onboarded', async () => {
  stubFetch(() => problemResponse('profile-not-found', 404));

  await expect(submitProfileEdit({ username: 'ada' })).resolves.toEqual({
    status: 'not-onboarded',
  });
});
