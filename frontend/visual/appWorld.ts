import type { Page, Route } from '@playwright/test';

// A small fixed world used to snapshot the app screens that need a backend. The visual
// server has none, so every `/api/**` call the router and its pages make is fulfilled
// here from static fixtures. Only GET reads are covered — the screenshot suite never
// mutates.

type ProfileView = {
  userId: string;
  username: string;
  displayName?: string;
  bio?: string;
};

type PostView = {
  postId: string;
  authorId: string;
  mediaId: string;
  caption?: string;
  publishedAt: string;
};

const VIEWER: ProfileView = {
  userId: 'u-ansel',
  username: 'ansel',
  displayName: 'Ansel Adams',
  bio: 'Landscape photographer. Chasing light across the Sierra Nevada.',
};

const PEOPLE: ProfileView[] = [
  VIEWER,
  { userId: 'u-vivian', username: 'vivian', displayName: 'Vivian Maier' },
  { userId: 'u-saul', username: 'saul', displayName: 'Saul Leiter', bio: 'Colour, snow, windows.' },
  { userId: 'u-dorothea', username: 'dorothea', displayName: 'Dorothea Lange' },
];

const byId = new Map(PEOPLE.map((person) => [person.userId, person]));
const byUsername = new Map(PEOPLE.map((person) => [person.username, person]));

// Timestamps are relative to the clock the spec pins, so the relative labels are stable.
const NOW = Date.parse('2026-09-04T12:00:00.000Z');
const ago = (ms: number) => new Date(NOW - ms).toISOString();
const HOUR = 3_600_000;
const DAY = 24 * HOUR;

const OWN_POSTS: PostView[] = [
  {
    postId: 'p-a1',
    authorId: 'u-ansel',
    mediaId: 'm-a1',
    caption: 'Clearing storm',
    publishedAt: ago(2 * HOUR),
  },
  {
    postId: 'p-a2',
    authorId: 'u-ansel',
    mediaId: 'm-a2',
    caption: 'Aspens, dawn',
    publishedAt: ago(2 * DAY),
  },
  { postId: 'p-a3', authorId: 'u-ansel', mediaId: 'm-a3', publishedAt: ago(6 * DAY) },
  {
    postId: 'p-a4',
    authorId: 'u-ansel',
    mediaId: 'm-a4',
    caption: 'Half Dome, winter',
    publishedAt: ago(9 * DAY),
  },
  { postId: 'p-a5', authorId: 'u-ansel', mediaId: 'm-a5', publishedAt: ago(12 * DAY) },
  {
    postId: 'p-a6',
    authorId: 'u-ansel',
    mediaId: 'm-a6',
    caption: 'Tenaya Lake',
    publishedAt: ago(15 * DAY),
  },
];

const FEED_POSTS: PostView[] = [
  {
    postId: 'p-v1',
    authorId: 'u-vivian',
    mediaId: 'm-v1',
    caption: 'Self-portrait, shop window',
    publishedAt: ago(3 * HOUR),
  },
  {
    postId: 'p-s1',
    authorId: 'u-saul',
    mediaId: 'm-s1',
    caption: 'Red umbrella in the snow',
    publishedAt: ago(1 * DAY),
  },
  { postId: 'p-d1', authorId: 'u-dorothea', mediaId: 'm-d1', publishedAt: ago(4 * DAY) },
];

const LIKES = new Map<string, { likeCount: number; likedByViewer: boolean }>([
  ['p-a1', { likeCount: 34, likedByViewer: false }],
  ['p-a2', { likeCount: 12, likedByViewer: true }],
  ['p-v1', { likeCount: 128, likedByViewer: true }],
  ['p-s1', { likeCount: 76, likedByViewer: false }],
  ['p-d1', { likeCount: 5, likedByViewer: false }],
]);

const FOLLOW = new Map<
  string,
  { followerCount: number; followingCount: number; followedByViewer: boolean }
>([
  ['u-ansel', { followerCount: 1840, followingCount: 62, followedByViewer: false }],
  ['u-vivian', { followerCount: 900, followingCount: 3, followedByViewer: true }],
  ['u-saul', { followerCount: 540, followingCount: 210, followedByViewer: false }],
  ['u-dorothea', { followerCount: 300, followingCount: 45, followedByViewer: true }],
]);

const FOLLOWERS_OF_ANSEL = ['u-vivian', 'u-saul', 'u-dorothea'];
const ANSEL_FOLLOWS = ['u-vivian', 'u-dorothea'];

function likesFor(postIds: string[]) {
  return postIds.map((postId) => {
    const likes = LIKES.get(postId);
    return {
      postId,
      likeCount: likes?.likeCount ?? 0,
      likedByViewer: likes?.likedByViewer ?? false,
    };
  });
}

function relationshipsFor(userIds: string[]) {
  return userIds.map((userId) => {
    const follow = FOLLOW.get(userId);
    return {
      userId,
      followerCount: follow?.followerCount ?? 0,
      followingCount: follow?.followingCount ?? 0,
      followedByViewer: follow?.followedByViewer ?? false,
    };
  });
}

// A flat, deterministic image so a card or grid cell looks real without pulling a binary
// fixture in. The hue is derived from the media id.
function photo(mediaId: string): string {
  let hash = 0;
  for (const char of mediaId) hash = (Math.imul(hash, 131) + (char.codePointAt(0) ?? 0)) >>> 0;
  // Spread the hue: ids in this world differ only in a trailing digit, so scale the hash
  // before the wrap or adjacent ids land on near-identical colours.
  const hue = (hash * 47) % 360;
  return [
    `<svg xmlns="http://www.w3.org/2000/svg" width="640" height="640">`,
    `<rect width="640" height="640" fill="hsl(${hue} 45% 62%)"/>`,
    `<rect y="420" width="640" height="220" fill="hsl(${(hue + 40) % 360} 40% 40%)"/>`,
    `</svg>`,
  ].join('');
}

function json(route: Route, body: unknown, status = 200): Promise<void> {
  return route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
}

async function handle(route: Route): Promise<void> {
  const url = new URL(route.request().url());
  const path = url.pathname;

  if (path === '/api/auth/refresh') return route.fulfill({ status: 401, body: '' });

  const media = /^\/api\/media\/([^/]+)\//.exec(path);
  if (media) return route.fulfill({ contentType: 'image/svg+xml', body: photo(media[1]) });

  if (path === '/api/profiles/me') return json(route, VIEWER);

  if (path === '/api/profiles') {
    const ids = url.searchParams.getAll('ids');
    const wanted = ids.length > 0 ? ids : PEOPLE.map((p) => p.userId);
    return json(
      route,
      wanted.flatMap((id) => {
        const person = byId.get(id);
        return person ? [person] : [];
      }),
    );
  }

  const profileMatch = /^\/api\/profiles\/([^/]+)$/.exec(path);
  if (profileMatch) {
    const person = byUsername.get(decodeURIComponent(profileMatch[1]));
    return person
      ? json(route, person)
      : json(route, { type: 'https://pictogram.dev/problems/profile-not-found', status: 404 }, 404);
  }

  if (path === '/api/feed') return json(route, { items: FEED_POSTS, nextCursor: null });

  if (path === '/api/posts') {
    const author = url.searchParams.get('author');
    return json(route, {
      items: OWN_POSTS.filter((post) => post.authorId === author),
      nextCursor: null,
    });
  }

  if (path === '/api/likes') return json(route, likesFor(url.searchParams.getAll('postIds')));

  if (path === '/api/follows') return json(route, relationshipsFor(url.searchParams.getAll('ids')));

  const followersMatch = /^\/api\/follows\/([^/]+)\/followers$/.exec(path);
  if (followersMatch) return json(route, { items: FOLLOWERS_OF_ANSEL, nextCursor: null });

  const followingMatch = /^\/api\/follows\/([^/]+)\/following$/.exec(path);
  if (followingMatch) return json(route, { items: ANSEL_FOLLOWS, nextCursor: null });

  const relationshipMatch = /^\/api\/follows\/([^/]+)$/.exec(path);
  if (relationshipMatch) return json(route, relationshipsFor([relationshipMatch[1]])[0]);

  return json(route, { type: 'about:blank', status: 404 }, 404);
}

// Only true API calls, not the `/src/shared/api/*` module requests the dev server serves.
const isApiCall = (url: URL) => url.pathname.startsWith('/api/');

// Serve every `/api/**` read from the fixed world above, signed in as `@ansel`.
export async function stubApp(page: Page): Promise<void> {
  await page.clock.setFixedTime(new Date(NOW));
  await page.route(isApiCall, (route) => void handle(route));
}

// The one screen shown to a visitor who has authenticated but not yet picked a username.
export async function stubNeedsOnboarding(page: Page): Promise<void> {
  await page.clock.setFixedTime(new Date(NOW));
  await page.route(isApiCall, (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path === '/api/auth/refresh') return route.fulfill({ status: 401, body: '' });
    return route.fulfill({
      status: 404,
      contentType: 'application/problem+json',
      body: JSON.stringify({
        type: 'https://pictogram.dev/problems/profile-not-found',
        status: 404,
      }),
    });
  });
}

export const OWN_PROFILE_PATH = '/u/ansel';
