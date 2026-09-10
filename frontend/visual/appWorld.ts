import { test as base, expect, type Page } from '@playwright/test';
import type { paths } from '../src/shared/api/schema';

export { expect } from '@playwright/test';

// A small fixed world used to snapshot the app screens that need a backend. The visual
// server has none, so every `/api/**` read is fulfilled here from static fixtures.
//
// One dispatch table (`TABLE`) driven by a `World` value, so `stubApp` and `stubStress`
// share every handler instead of maintaining two routers. Handlers are typed against the
// generated `paths`. A request no handler claims fails the test (via the `appWorldRoutes`
// fixture) rather than returning a silent 404 that screenshots green on an empty state.

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

type CommentView = {
  commentId: string;
  postId: string;
  authorId: string;
  body: string;
  createdAt: string;
};

type LikeRow = { likeCount: number; likedByViewer: boolean };
type FollowRow = { followerCount: number; followingCount: number; followedByViewer: boolean };

type NotificationView = {
  type: 'post-liked' | 'post-commented' | 'user-followed';
  actorId: string;
  subjectPostId?: string;
  occurredAt: string;
  read: boolean;
};

// The accounts, posts and comments one run sees; every handler reads only from here.
type World = {
  viewer: ProfileView;
  people: ProfileView[];
  posts: PostView[];
  feed: PostView[];
  comments: Record<string, CommentView[]>;
  likes: Map<string, LikeRow>;
  follows: Map<string, FollowRow>;
  followerIds: string[];
  followingIds: string[];
  notifications: NotificationView[];
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

const LIKES = new Map<string, LikeRow>([
  ['p-a1', { likeCount: 34, likedByViewer: false }],
  ['p-a2', { likeCount: 12, likedByViewer: true }],
  ['p-v1', { likeCount: 128, likedByViewer: true }],
  ['p-s1', { likeCount: 76, likedByViewer: false }],
  ['p-d1', { likeCount: 5, likedByViewer: false }],
]);

const FOLLOW = new Map<string, FollowRow>([
  ['u-ansel', { followerCount: 1840, followingCount: 62, followedByViewer: false }],
  ['u-vivian', { followerCount: 900, followingCount: 3, followedByViewer: true }],
  ['u-saul', { followerCount: 540, followingCount: 210, followedByViewer: false }],
  ['u-dorothea', { followerCount: 300, followingCount: 45, followedByViewer: true }],
]);

const COMMENTS: Record<string, CommentView[]> = {
  'p-v1': [
    {
      commentId: 'c-1',
      postId: 'p-v1',
      authorId: 'u-saul',
      body: 'The reflection makes it — you can feel the street around you.',
      createdAt: ago(5 * HOUR),
    },
    {
      commentId: 'c-2',
      postId: 'p-v1',
      authorId: 'u-dorothea',
      body: 'More at https://pictogram.dev/vivian',
      createdAt: ago(2 * HOUR),
    },
  ],
};

const NOTIFICATIONS: NotificationView[] = [
  {
    type: 'post-liked',
    actorId: 'u-vivian',
    subjectPostId: 'p-a1',
    occurredAt: ago(40 * 60_000),
    read: false,
  },
  {
    type: 'post-commented',
    actorId: 'u-saul',
    subjectPostId: 'p-a2',
    occurredAt: ago(3 * HOUR),
    read: false,
  },
  { type: 'user-followed', actorId: 'u-dorothea', occurredAt: ago(2 * DAY), read: true },
];

const NORMAL: World = {
  viewer: VIEWER,
  people: PEOPLE,
  posts: OWN_POSTS,
  feed: FEED_POSTS,
  comments: COMMENTS,
  likes: LIKES,
  follows: FOLLOW,
  followerIds: ['u-vivian', 'u-saul', 'u-dorothea'],
  followingIds: ['u-vivian', 'u-dorothea'],
  notifications: NOTIFICATIONS,
};

// Worst-case content for the horizontal-overflow assertions (#139): a maximum-length
// username, an unusually long display name, and a bio carrying one enormous unbreakable
// token plus a long bare URL — the strings that break a layout if a container forgets to
// wrap or truncate. Kept out of the screenshot world so the committed baselines stay
// readable.
const STRESS_HANDLE = 'aureliano_buendia_08'; // 20 chars — the username cap
const STRESS_NAME = 'Aureliano Buendía de la Fotografía de Montaña y Nieve';
const STRESS_TOKEN = `unbreakable-${'x'.repeat(90)}`;
const STRESS_URL = `https://example.com/${'segment/'.repeat(12)}end`;

const STRESS_PERSON: ProfileView = {
  userId: 'u-stress',
  username: STRESS_HANDLE,
  displayName: STRESS_NAME,
  bio: `${STRESS_TOKEN} and then a bare link ${STRESS_URL} to close it out`,
};

const STRESS_POST: PostView = {
  postId: 'p-stress',
  authorId: 'u-stress',
  mediaId: 'm-a1',
  caption: `${STRESS_TOKEN} ${STRESS_URL}`,
  publishedAt: ago(2 * HOUR),
};

const STRESS_COMMENT: CommentView = {
  commentId: 'c-stress',
  postId: 'p-stress',
  authorId: 'u-stress',
  body: `${STRESS_TOKEN} ${STRESS_URL}`,
  createdAt: ago(1 * HOUR),
};

// One account carrying the worst-case strings above.
const STRESS: World = {
  viewer: STRESS_PERSON,
  people: [STRESS_PERSON],
  posts: [STRESS_POST],
  feed: [STRESS_POST],
  comments: { [STRESS_POST.postId]: [STRESS_COMMENT] },
  likes: new Map(),
  follows: new Map(),
  followerIds: [STRESS_PERSON.userId],
  followingIds: [STRESS_PERSON.userId],
  notifications: [
    {
      type: 'post-liked',
      actorId: STRESS_PERSON.userId,
      subjectPostId: STRESS_POST.postId,
      occurredAt: ago(1 * HOUR),
      read: false,
    },
    { type: 'user-followed', actorId: STRESS_PERSON.userId, occurredAt: ago(2 * HOUR), read: true },
  ],
};

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

// --- dispatch table -------------------------------------------------------------------

// The reads a request carries: path params from the matched template, plus the one place
// query-string filtering is spelled out.
type Request = {
  params: Record<string, string>;
  list: (name: string) => string[];
  value: (name: string) => string | undefined;
};

type Reply = { status: number; contentType?: string; body: string };

// The JSON body a GET on `P` answers 200 with, straight from the generated contract.
type JsonGetPath = {
  [P in keyof paths]: paths[P] extends {
    get: { responses: { 200: { content: { 'application/json': unknown } } } };
  }
    ? P
    : never;
}[keyof paths];

type JsonBody<P extends JsonGetPath> = paths[P] extends {
  get: { responses: { 200: { content: { 'application/json': infer B } } } };
}
  ? B
  : never;

const PROFILE_NOT_FOUND = 'https://pictogram.dev/problems/profile-not-found';
type Problem = { problem: { type: string; status: number } };
const notFound = (type: string): Problem => ({ problem: { type, status: 404 } });
const isProblem = (value: unknown): value is Problem =>
  typeof value === 'object' && value !== null && 'problem' in value;

type Entry = {
  method: string;
  match: (path: string) => Record<string, string> | null;
  reply: (request: Request, world: World) => Reply;
};

// `/api/profiles/{username}` -> `^/api/profiles/([^/]+)$`, capturing `username`.
function matcher(template: string): (path: string) => Record<string, string> | null {
  const names: string[] = [];
  const pattern = template.replaceAll(/\{([^}]+)\}/g, (_full, name: string) => {
    names.push(name);
    return '([^/]+)';
  });
  const regExp = new RegExp(`^${pattern}$`);
  return (path) => {
    const found = regExp.exec(path);
    if (!found) return null;
    const params: Record<string, string> = {};
    for (const [index, name] of names.entries())
      params[name] = decodeURIComponent(found[index + 1]);
    return params;
  };
}

const json = (body: unknown, status = 200): Reply => ({
  status,
  contentType: 'application/json',
  body: JSON.stringify(body),
});

function readJson<P extends JsonGetPath>(
  template: P,
  read: (request: Request, world: World) => JsonBody<P> | Problem,
): Entry {
  return {
    method: 'GET',
    match: matcher(template),
    reply: (request, world) => {
      const result: unknown = read(request, world);
      return isProblem(result) ? json(result.problem, result.problem.status) : json(result);
    },
  };
}

function readRaw(
  template: keyof paths,
  method: 'GET' | 'POST',
  reply: (request: Request) => Reply,
): Entry {
  return { method, match: matcher(template), reply: (request) => reply(request) };
}

// The fixture world always fits one page: return every item, never a next cursor.
const pageOf = <T>(items: T[]): { items: T[] } => ({ items });

function relationship(world: World, userId: string): FollowRow {
  const row = world.follows.get(userId);
  return {
    followerCount: row?.followerCount ?? 0,
    followingCount: row?.followingCount ?? 0,
    followedByViewer: row?.followedByViewer ?? false,
  };
}

const TABLE: Entry[] = [
  readRaw('/api/auth/refresh', 'POST', () => ({ status: 401, body: '' })),
  readRaw('/api/media/{mediaId}/original', 'GET', ({ params }) => ({
    status: 200,
    contentType: 'image/svg+xml',
    body: photo(params.mediaId),
  })),
  readRaw('/api/media/{mediaId}/thumbnail', 'GET', ({ params }) => ({
    status: 200,
    contentType: 'image/svg+xml',
    body: photo(params.mediaId),
  })),

  readJson('/api/profiles/me', (_request, world) => world.viewer),
  readJson('/api/profiles', (request, world) => {
    const asked = request.list('ids');
    const ids = asked.length > 0 ? asked : world.people.map((person) => person.userId);
    return ids.flatMap((id) => {
      const person = world.people.find((candidate) => candidate.userId === id);
      return person ? [person] : [];
    });
  }),
  readJson('/api/profiles/{username}', ({ params }, world) => {
    return (
      world.people.find((candidate) => candidate.username === params.username) ??
      notFound(PROFILE_NOT_FOUND)
    );
  }),

  readJson('/api/feed', (_request, world) => pageOf(world.feed)),
  readJson('/api/posts', (request, world) =>
    pageOf(world.posts.filter((post) => post.authorId === request.value('author'))),
  ),
  readJson('/api/posts/by-ids', (request, world) => {
    const asked = new Set(request.list('ids'));
    return pageOf([...world.posts, ...world.feed].filter((post) => asked.has(post.postId)));
  }),

  readJson('/api/notifications', (_request, world) => pageOf(world.notifications)),
  readJson('/api/notifications/unread-count', (_request, world) => ({
    count: world.notifications.filter((notification) => !notification.read).length,
  })),
  readRaw('/api/notifications/mark-read', 'POST', () => ({ status: 204, body: '' })),

  readJson('/api/likes', (request, world) =>
    request.list('postIds').map((postId) => {
      const row = world.likes.get(postId);
      return { postId, likeCount: row?.likeCount ?? 0, likedByViewer: row?.likedByViewer ?? false };
    }),
  ),
  readJson('/api/comments', (request, world) =>
    request
      .list('postIds')
      .map((postId) => ({ postId, commentCount: (world.comments[postId] ?? []).length })),
  ),
  readJson('/api/posts/{postId}/comments', ({ params }, world) =>
    pageOf(world.comments[params.postId] ?? []),
  ),

  readJson('/api/follows/{userId}/followers', (_request, world) => pageOf(world.followerIds)),
  readJson('/api/follows/{userId}/following', (_request, world) => pageOf(world.followingIds)),
  readJson('/api/follows', (request, world) =>
    request.list('ids').map((userId) => ({ userId, ...relationship(world, userId) })),
  ),
  readJson('/api/follows/{userId}', ({ params }, world) => relationship(world, params.userId)),
];

// --- installation -------------------------------------------------------------------

// Only true API calls, not the `/src/shared/api/*` module requests the dev server serves.
const isApiCall = (url: URL) => url.pathname.startsWith('/api/');

// Per-page sink for requests no handler claimed. The `appWorldRoutes` fixture owns the
// array (creates it, asserts it empty in teardown); `install` only appends.
const unmatched = new WeakMap<Page, string[]>();

async function install(page: Page, world: World): Promise<void> {
  await page.clock.setFixedTime(new Date(NOW));
  const misses = unmatched.get(page);
  if (!misses) {
    throw new Error(
      'appWorld stub installed without the appWorldRoutes fixture — import { test } from "./appWorld"',
    );
  }

  await page.route(isApiCall, (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method();

    for (const entry of TABLE) {
      if (entry.method !== method) continue;
      const params = entry.match(url.pathname);
      if (!params) continue;
      const reply = entry.reply(
        {
          params,
          list: (name) => url.searchParams.getAll(name),
          value: (name) => url.searchParams.get(name) ?? undefined,
        },
        world,
      );
      return route.fulfill({
        status: reply.status,
        contentType: reply.contentType,
        body: reply.body,
      });
    }

    misses.push(`${method} ${url.pathname}`);
    return route.fulfill({
      status: 599,
      contentType: 'text/plain',
      body: `appWorld has no handler for ${method} ${url.pathname}`,
    });
  });
}

// Serve every `/api/**` read from the fixed world above, signed in as `@ansel`.
export const stubApp = (page: Page): Promise<void> => install(page, NORMAL);

// The same world with the viewer following nobody — the chat rail's empty state (#203).
export const stubNoFollows = (page: Page): Promise<void> =>
  install(page, { ...NORMAL, followingIds: [] });

// The chat rail with live presence and session-local unread markers (#204). The visual
// server has no chat backend, so this also stands up a fake session WebSocket in the page:
// it answers presence queries from `online` and pushes a `message` frame from each id in
// `unreadFrom` once it opens, which is exactly what marks a rail row unread. A token is
// needed before the app opens the socket at all, so the first `/api/profiles/me` 401s to
// drive one refresh.
export async function stubChatRail(
  page: Page,
  { online = [], unreadFrom = [] }: { online?: string[]; unreadFrom?: string[] } = {},
): Promise<void> {
  await install(page, NORMAL);

  let profileProbes = 0;
  await page.route('**/api/profiles/me', (route) => {
    profileProbes += 1;
    return profileProbes === 1
      ? route.fulfill({
          status: 401,
          contentType: 'application/problem+json',
          body: JSON.stringify({ type: 'about:blank', status: 401 }),
        })
      : route.fallback();
  });
  await page.route('**/api/auth/refresh', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ accessToken: 'visual-token', expiresInSeconds: 3600 }),
    }),
  );

  await page.addInitScript(
    ({ online: onlineIds, unreadFrom: unreadIds }) => {
      class FakeChatSocket {
        url: string;
        protocols: string | string[];
        readyState = 0;
        private readonly listeners: Record<string, ((event: unknown) => void)[]> = {};

        constructor(url: string, protocols: string | string[]) {
          this.url = url;
          this.protocols = protocols;
          setTimeout(() => {
            this.readyState = 1;
            this.emit('open', {});
            for (const senderUserId of unreadIds) {
              this.emit('message', {
                data: JSON.stringify({
                  type: 'message',
                  senderUserId,
                  text: 'Sent while you were away',
                }),
              });
            }
          }, 0);
        }

        addEventListener(type: string, fn: (event: unknown) => void): void {
          (this.listeners[type] ??= []).push(fn);
        }

        removeEventListener(type: string, fn: (event: unknown) => void): void {
          this.listeners[type] = (this.listeners[type] ?? []).filter((l) => l !== fn);
        }

        send(raw: string): void {
          let frame: { type?: string; userId?: string; userIds?: string[] };
          try {
            frame = JSON.parse(raw) as { type?: string; userId?: string; userIds?: string[] };
          } catch {
            return;
          }
          if (frame.type !== 'presence-query') return;
          const ids = frame.userIds ?? (frame.userId === undefined ? [] : [frame.userId]);
          for (const userId of ids) {
            this.emit('message', {
              data: JSON.stringify({
                type: 'presence',
                userId,
                online: onlineIds.includes(userId),
              }),
            });
          }
        }

        close(): void {
          this.readyState = 3;
          this.emit('close', {});
        }

        private emit(type: string, event: unknown): void {
          const forType = this.listeners[type] ?? [];
          for (const fn of forType) fn(event);
        }
      }

      Object.defineProperty(globalThis, 'WebSocket', { configurable: true, value: FakeChatSocket });
    },
    { online, unreadFrom },
  );
}

// The same handlers over a one-account world carrying the worst-case strings (#139).
export const stubStress = (page: Page): Promise<void> => install(page, STRESS);

export const STRESS_PROFILE_PATH = `/u/${STRESS_HANDLE}`;

// The screen shown to a visitor who authenticated but hasn't picked a username: every
// profile read 404s, so the app routes to onboarding. A deliberate all-404 world, so it
// doesn't go through `TABLE` or the unmatched-route check.
export async function stubNeedsOnboarding(page: Page): Promise<void> {
  await page.clock.setFixedTime(new Date(NOW));
  await page.route(isApiCall, (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path === '/api/auth/refresh') return route.fulfill({ status: 401, body: '' });
    return route.fulfill({
      status: 404,
      contentType: 'application/problem+json',
      body: JSON.stringify({ type: PROFILE_NOT_FOUND, status: 404 }),
    });
  });
}

type Fixtures = { appWorldRoutes: string[] };

// Auto fixture: fails any test whose stubbed screen hit an `/api/**` route no handler
// claimed, so a missing arm turns a screenshot red rather than green on an empty state.
export const test = base.extend<Fixtures>({
  appWorldRoutes: [
    async ({ page }, use) => {
      const misses: string[] = [];
      unmatched.set(page, misses);
      await use(misses);
      expect(misses, 'appWorld received /api/ requests with no handler').toEqual([]);
    },
    { auto: true },
  ],
});
