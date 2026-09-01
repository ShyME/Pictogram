import { profileLoader } from '@features/profile/profile-loader';
import { jsonResponse, problemResponse, stubFetch } from '@test-support/mock-fetch';
import type { LoaderFunctionArgs } from 'react-router';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

function route(handlers: { profile: () => Response; me: () => Response }) {
  stubFetch((request) => {
    const { pathname } = new URL(request.url);
    if (pathname === '/api/profiles/me') return handlers.me();
    return handlers.profile();
  });
}

const load = (username: string) =>
  profileLoader({ params: { username } } as unknown as LoaderFunctionArgs);

test("marks the profile as the viewer's own when the usernames match", async () => {
  route({
    profile: () => jsonResponse({ userId: 'u-1', username: 'ada', displayName: 'Ada', bio: null }),
    me: () => jsonResponse({ userId: 'u-1', username: 'ada', displayName: 'Ada', bio: null }),
  });

  await expect(load('ada')).resolves.toEqual({
    status: 'found',
    profile: { userId: 'u-1', username: 'ada', displayName: 'Ada', bio: null },
    isOwnProfile: true,
    viewerCanFollow: false,
  });
});

test('a signed-in viewer looking at someone else can follow them', async () => {
  route({
    profile: () => jsonResponse({ userId: 'u-2', username: 'grace' }),
    me: () => jsonResponse({ userId: 'u-1', username: 'ada' }),
  });

  await expect(load('grace')).resolves.toMatchObject({
    status: 'found',
    isOwnProfile: false,
    viewerCanFollow: true,
  });
});

test('an unauthenticated visitor sees the public profile but cannot follow', async () => {
  route({
    profile: () => jsonResponse({ userId: 'u-2', username: 'grace' }),
    me: () => problemResponse('unauthorized', 401),
  });

  await expect(load('grace')).resolves.toMatchObject({
    status: 'found',
    isOwnProfile: false,
    viewerCanFollow: false,
  });
});

test("a failing own-profile check does not sink the page — it renders as a visitor's view", async () => {
  route({
    profile: () => jsonResponse({ userId: 'u-2', username: 'grace' }),
    me: () => new Response(null, { status: 500 }),
  });

  await expect(load('grace')).resolves.toMatchObject({
    status: 'found',
    isOwnProfile: false,
    viewerCanFollow: false,
  });
});

test('an unknown username resolves to not-found with the username echoed back', async () => {
  route({
    profile: () => problemResponse('profile-not-found', 404),
    me: () => problemResponse('unauthorized', 401),
  });

  await expect(load('ghost_user')).resolves.toEqual({
    status: 'not-found',
    username: 'ghost_user',
  });
});
