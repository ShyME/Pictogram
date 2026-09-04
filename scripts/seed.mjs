#!/usr/bin/env node
// Populate a running `task up` stack with a fixed demo graph so you can sign in as
// `alice` and land on a full app — a non-empty feed and a non-empty profile grid.
//
// Pure Node, zero npm dependencies. Assumes the stack is already up (`task up`).
// See GitHub issue #158.
//
//   node scripts/seed.mjs         # or: task seed
//
// Config (defaults match the `task up` topology):
//   PICTOGRAM_BASE_URL   the app            (default http://localhost:8080)
//   PICTOGRAM_OAUTH_URL  the mock OIDC issuer as reachable from this machine
//                                           (default http://localhost:8095)

import { readFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';

const APP = (process.env.PICTOGRAM_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '');
const OAUTH = (process.env.PICTOGRAM_OAUTH_URL ?? 'http://localhost:8095').replace(/\/$/, '');

const REFRESH_COOKIE = 'pictogram_refresh';

// ---------------------------------------------------------------------------
// The dataset — fully deterministic, no randomness.
// ---------------------------------------------------------------------------

export const PERSONAS = [
  { username: 'alice', displayName: 'Alice Nakamura', bio: 'Ceramics, film photography, slow mornings in Kyoto.' },
  { username: 'bob', displayName: 'Bob Okonkwo', bio: 'Trail runner chasing sunrises and the next ridgeline.' },
  { username: 'carol', displayName: 'Carol Whitfield', bio: 'Architect by day, watercolours by night.' },
  { username: 'dave', displayName: 'Dave Ramirez', bio: 'Dad, cyclist, amateur bread scientist.' },
  { username: 'erin', displayName: 'Erin Lindqvist', bio: 'Marine biologist. Mostly here to photograph kelp.' },
];

// 4 captions per persona, parallel to PERSONAS. 20 posts total.
export const CAPTIONS = [
  [
    'First firing out of the new kiln.',
    'Tea bowls, take four.',
    'Morning light through the workshop window.',
    'Glaze tests — the blue finally behaved.',
  ],
  [
    '5am on the ridge. Worth it.',
    'New personal best on the loop trail.',
    'Fog rolling through the valley.',
    'These shoes have seen better days.',
  ],
  [
    'Site visit sketch, twenty minutes.',
    'Concrete and afternoon shadows.',
    'Trying to paint moving water again.',
    'The stairwell I keep coming back to.',
  ],
  [
    'Seventy-two hour sourdough. Big crumb.',
    'Century ride done. Legs gone.',
    'The repair stand finally earns its keep.',
    'Breakfast of champions.',
  ],
  [
    'Kelp forest at twelve metres.',
    'Tide pool regulars.',
    'Research boat at dawn.',
    'This sea star is missing an arm and doing just fine.',
  ],
];

// follower username -> usernames they follow. alice <-> everyone both ways; the rest
// form a connected sub-graph so no profile is a dead end.
export const FOLLOWS = {
  alice: ['bob', 'carol', 'dave', 'erin'],
  bob: ['alice', 'carol', 'dave'],
  carol: ['alice', 'dave', 'erin'],
  dave: ['alice', 'erin'],
  erin: ['alice', 'bob'],
};

const COMMENT_POOL = [
  'Love this.',
  'Incredible shot.',
  'The light here is unreal.',
  'Been waiting for a new post from you.',
  'This made my day.',
  'Okay this is stunning.',
  'How have you been?',
  'Saving this one.',
];

const ASSET_COUNT = 8;
const POSTS_PER_PERSONA = 4;

const rotate = (xs, n) => {
  const k = ((n % xs.length) + xs.length) % xs.length;
  return [...xs.slice(k), ...xs.slice(0, k)];
};

// Likes and comments for the post at global index `i` (0..19), by persona index.
export function engagementFor(i) {
  const authorIndex = Math.floor(i / POSTS_PER_PERSONA);
  const others = [0, 1, 2, 3, 4].filter((x) => x !== authorIndex);
  const likeCount = 2 + (i % 2); // 2 or 3
  const commentCount = i % 3 === 0 ? 2 : 1; // 1 or 2
  const likers = rotate(others, i).slice(0, likeCount);
  const comments = rotate(others, i * 2 + 1)
    .slice(0, commentCount)
    .map((personaIndex, k) => ({ personaIndex, body: COMMENT_POOL[(i * 3 + k) % COMMENT_POOL.length] }));
  return { likers, comments };
}

export const postCount = () => PERSONAS.length * POSTS_PER_PERSONA;

// alice's feed is every post by someone she follows.
export function expectedFeedCount() {
  return FOLLOWS.alice.reduce((sum, username) => {
    const idx = PERSONAS.findIndex((p) => p.username === username);
    return sum + CAPTIONS[idx].length;
  }, 0);
}

export const expectedGridCount = (username) =>
  CAPTIONS[PERSONAS.findIndex((p) => p.username === username)].length;

// ---------------------------------------------------------------------------
// HTTP plumbing — the Google/OIDC sign-in dance, reimplemented (issue #158).
// ---------------------------------------------------------------------------

// Cookies are host-scoped, not port-scoped: the app (:8080) and the mock issuer
// (:8095) share the `localhost` hostname, so one jar carries the OAuth session
// cookie across both and back to the app's callback.
class CookieJar {
  #byName = new Map();

  absorb(response) {
    for (const header of response.headers.getSetCookie()) {
      const pair = header.split(';', 1)[0].trim();
      const eq = pair.indexOf('=');
      if (eq > 0) this.#byName.set(pair.slice(0, eq), pair.slice(eq + 1));
    }
  }

  get(name) {
    return this.#byName.get(name);
  }

  header() {
    return [...this.#byName].map(([name, value]) => `${name}=${value}`).join('; ');
  }
}

async function walk(startUrl, init, jar) {
  let url = startUrl;
  let request = init;
  for (let hop = 0; hop < 12; hop++) {
    const headers = { ...request.headers };
    const cookies = jar.header();
    if (cookies) headers.cookie = cookies;
    const response = await fetch(url, { ...request, headers, redirect: 'manual' });
    jar.absorb(response);
    const location = response.headers.get('location');
    if (![301, 302, 303, 307, 308].includes(response.status) || !location) {
      return { response, url };
    }
    let next = new URL(location, url);
    // Only two origins take part: the app and the OIDC issuer. Anything that
    // isn't the app is the issuer — swap its origin for PICTOGRAM_OAUTH_URL's so
    // the override can point the issuer at a non-default host/port. The issuer's
    // own path (e.g. /google/authorize) is already absolute in the redirect and
    // is kept as-is.
    if (next.origin !== new URL(APP).origin) {
      next = new URL(next.pathname + next.search + next.hash, new URL(OAUTH).origin);
    }
    url = next.toString();
    request = response.status === 307 || response.status === 308 ? request : { method: 'GET' };
  }
  throw new Error(`too many redirects starting from ${startUrl}`);
}

async function signIn(subject) {
  const jar = new CookieJar();
  const landed = await walk(`${APP}/oauth2/authorization/google`, { method: 'GET' }, jar);

  if (!jar.get(REFRESH_COOKIE)) {
    if (landed.response.status !== 200) {
      throw new Error(`expected the mock login form, got ${landed.response.status} at ${landed.url}`);
    }
    await walk(
      landed.url,
      {
        method: 'POST',
        headers: { 'content-type': 'application/x-www-form-urlencoded' },
        body: `username=${encodeURIComponent(subject)}`,
      },
      jar,
    );
  }

  const refresh = jar.get(REFRESH_COOKIE);
  if (!refresh) throw new Error(`no ${REFRESH_COOKIE} cookie after signing in as "${subject}"`);
  return refresh;
}

function xsrfFrom(response) {
  for (const header of response.headers.getSetCookie()) {
    if (header.startsWith('XSRF-TOKEN=')) return header.slice('XSRF-TOKEN='.length).split(';', 1)[0];
  }
  throw new Error('the refresh 403 seeded no XSRF-TOKEN cookie');
}

async function redeem(refresh) {
  const post = (cookie, token) =>
    fetch(`${APP}/api/auth/refresh`, {
      method: 'POST',
      headers: token ? { cookie, 'x-xsrf-token': token } : { cookie },
    });

  let response = await post(`${REFRESH_COOKIE}=${refresh}`);
  if (response.status === 403) {
    // The identity chain double-submits a CSRF token: a tokenless POST is rejected
    // with a fresh XSRF-TOKEN cookie; the retry echoes it back, as the SPA does.
    const token = xsrfFrom(response);
    response = await post(`${REFRESH_COOKIE}=${refresh}; XSRF-TOKEN=${token}`, token);
  }
  if (response.status !== 200) {
    throw new Error(`redeeming the refresh cookie failed: ${response.status} ${await response.text()}`);
  }
  return (await response.json()).accessToken;
}

class Api {
  constructor(accessToken) {
    this.accessToken = accessToken;
  }

  async call(method, path, { json, form, expect } = {}) {
    const headers = { authorization: `Bearer ${this.accessToken}` };
    let body;
    if (json !== undefined) {
      headers['content-type'] = 'application/json';
      body = JSON.stringify(json);
    } else if (form !== undefined) {
      body = form;
    }
    const response = await fetch(`${APP}${path}`, { method, headers, body });
    if (expect !== undefined && response.status !== expect) {
      throw new Error(`${method} ${path} -> ${response.status} (wanted ${expect}): ${await response.text()}`);
    }
    return response;
  }

  async onboard(username, displayName, bio) {
    const response = await this.call('POST', '/api/profiles', { json: { username, displayName, bio }, expect: 201 });
    return (await response.json()).userId;
  }

  async uploadPhoto(bytes) {
    const form = new FormData();
    form.append('file', new Blob([bytes], { type: 'image/jpeg' }), 'photo.jpg');
    const response = await this.call('POST', '/api/media', { form, expect: 201 });
    return (await response.json()).mediaId;
  }

  async publishPost(mediaId, caption) {
    const response = await this.call('POST', '/api/posts', { json: { mediaId, caption }, expect: 201 });
    return (await response.json()).postId;
  }

  follow(userId) {
    return this.call('PUT', `/api/follows/${userId}`, { expect: 204 });
  }

  like(postId) {
    return this.call('PUT', `/api/likes/${postId}`, { expect: 204 });
  }

  comment(postId, body) {
    return this.call('POST', `/api/posts/${postId}/comments`, { json: { body }, expect: 201 });
  }

  async countPage(path) {
    let total = 0;
    let cursor = null;
    do {
      const sep = path.includes('?') ? '&' : '?';
      const url = `${path}${sep}limit=30${cursor ? `&cursor=${encodeURIComponent(cursor)}` : ''}`;
      const response = await this.call('GET', url, { expect: 200 });
      const page = await response.json();
      total += page.items.length;
      cursor = page.nextCursor;
    } while (cursor);
    return total;
  }
}

// ---------------------------------------------------------------------------
// Seeding.
// ---------------------------------------------------------------------------

const log = (message) => console.log(message);

async function resolveAlice() {
  try {
    const response = await fetch(`${APP}/api/profiles/alice`);
    return response.status === 200 ? (await response.json()).userId : null;
  } catch {
    return null;
  }
}

async function preflight() {
  try {
    const response = await fetch(`${APP}/actuator/health`);
    if (response.ok) return;
  } catch {
    /* falls through to the message below */
  }
  console.error('start the stack first: task up');
  process.exit(1);
}

function loadAssets() {
  return Array.from({ length: ASSET_COUNT }, (_, i) => {
    const name = String(i + 1).padStart(2, '0');
    return readFileSync(new URL(`./seed-assets/${name}.jpg`, import.meta.url));
  });
}

async function seed() {
  const assets = loadAssets();

  const accounts = [];
  for (const persona of PERSONAS) {
    const api = new Api(await redeem(await signIn(persona.username)));
    const userId = await api.onboard(persona.username, persona.displayName, persona.bio);
    accounts.push({ ...persona, api, userId });
    log(`  onboarded ${persona.username}`);
  }

  const byUsername = Object.fromEntries(accounts.map((a) => [a.username, a]));
  const postIds = [];
  for (let i = 0; i < postCount(); i++) {
    const author = accounts[Math.floor(i / POSTS_PER_PERSONA)];
    const caption = CAPTIONS[Math.floor(i / POSTS_PER_PERSONA)][i % POSTS_PER_PERSONA];
    const mediaId = await author.api.uploadPhoto(assets[i % ASSET_COUNT]);
    postIds.push(await author.api.publishPost(mediaId, caption));
  }
  log(`  published ${postIds.length} posts`);

  for (const [follower, followees] of Object.entries(FOLLOWS)) {
    for (const followee of followees) await byUsername[follower].api.follow(byUsername[followee].userId);
  }
  log('  wired the follow graph');

  let likes = 0;
  let comments = 0;
  for (let i = 0; i < postIds.length; i++) {
    const { likers, comments: threadComments } = engagementFor(i);
    for (const personaIndex of likers) {
      await accounts[personaIndex].api.like(postIds[i]);
      likes++;
    }
    for (const { personaIndex, body } of threadComments) {
      await accounts[personaIndex].api.comment(postIds[i], body);
      comments++;
    }
  }
  log(`  added ${likes} likes and ${comments} comments`);

  return byUsername.alice;
}

async function feedAndProfileMismatches(alice) {
  const mismatches = [];
  const check = (label, actual, expected) => {
    if (actual !== expected) mismatches.push(`${label}: got ${actual}, expected ${expected}`);
  };

  check("alice's feed", await alice.api.countPage('/api/feed'), expectedFeedCount());
  check("alice's profile grid", await alice.api.countPage(`/api/posts?author=${alice.userId}`), expectedGridCount('alice'));

  const relationship = await (await fetch(`${APP}/api/follows/${alice.userId}`)).json();
  check("alice's followers", relationship.followerCount, FOLLOWS.alice.length);
  check("alice's following", relationship.followingCount, FOLLOWS.alice.length);
  return mismatches;
}

async function aliceApi(userId) {
  return { username: 'alice', userId, api: new Api(await redeem(await signIn('alice'))) };
}

async function main() {
  await preflight();

  const existingAliceId = await resolveAlice();
  if (existingAliceId) {
    // The dataset's guard is alice's existence (issue #158). If she is here but the
    // graph is incomplete a previous run died partway — re-seeding would double up,
    // so stop and point at the one reset path (`task clean`).
    const mismatches = await feedAndProfileMismatches(await aliceApi(existingAliceId));
    if (mismatches.length === 0) {
      log('already seeded');
      return;
    }
    console.error('partially seeded — run `task clean`, then `task seed` again:');
    for (const line of mismatches) console.error(`  ${line}`);
    process.exit(1);
  }

  log('seeding the demo dataset...');
  const alice = await seed();
  const mismatches = await feedAndProfileMismatches(alice);
  if (mismatches.length > 0) {
    console.error('self-check failed:');
    for (const line of mismatches) console.error(`  ${line}`);
    process.exit(1);
  }
  log('  self-check passed');
  log(`done. Sign in as 'alice' at ${APP}`);
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  await main();
}
