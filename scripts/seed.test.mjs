// Guards the deterministic shape of the demo dataset in seed.mjs. Zero deps: `node --test`.
import { test } from 'node:test';
import assert from 'node:assert/strict';

import { PERSONAS, CAPTIONS, FOLLOWS, engagementFor, postCount, expectedFeedCount, expectedGridCount } from './seed.mjs';

const USERNAME_SHAPE = /^[a-z0-9_]{3,20}$/;
const usernames = PERSONAS.map((p) => p.username);

test('five personas, each with a valid username, display name and bio', () => {
  assert.equal(PERSONAS.length, 5);
  for (const persona of PERSONAS) {
    assert.match(persona.username, USERNAME_SHAPE);
    assert.ok(persona.displayName.length > 0 && persona.displayName.length <= 50);
    assert.ok(persona.bio.length > 0 && persona.bio.length <= 160);
  }
});

test('twenty posts: four captions per persona', () => {
  assert.equal(postCount(), 20);
  assert.equal(CAPTIONS.length, PERSONAS.length);
  for (const captions of CAPTIONS) {
    assert.equal(captions.length, 4);
    for (const caption of captions) assert.ok(caption.length > 0 && caption.length <= 2200);
  }
});

test('follow graph: known usernames, no self-follow, alice both ways with everyone', () => {
  for (const [follower, followees] of Object.entries(FOLLOWS)) {
    assert.ok(usernames.includes(follower));
    assert.ok(!followees.includes(follower), `${follower} follows themselves`);
    assert.equal(new Set(followees).size, followees.length, `${follower} has a duplicate follow`);
    for (const followee of followees) assert.ok(usernames.includes(followee));
  }
  for (const other of usernames.filter((u) => u !== 'alice')) {
    assert.ok(FOLLOWS.alice.includes(other), `alice does not follow ${other}`);
    assert.ok(FOLLOWS[other].includes('alice'), `${other} does not follow alice`);
  }
});

test('no profile is a dead end — everyone has a follower and someone they follow', () => {
  for (const username of usernames) {
    assert.ok(FOLLOWS[username].length > 0, `${username} follows nobody`);
    assert.ok(
      usernames.some((other) => FOLLOWS[other].includes(username)),
      `${username} has no followers`,
    );
  }
});

test('engagement is deterministic and excludes the post author', () => {
  for (let i = 0; i < postCount(); i++) {
    const authorIndex = Math.floor(i / 4);
    const a = engagementFor(i);
    const b = engagementFor(i);
    assert.deepEqual(a, b);

    assert.ok(a.likers.length === 2 || a.likers.length === 3);
    assert.equal(new Set(a.likers).size, a.likers.length);
    assert.ok(!a.likers.includes(authorIndex));

    assert.ok(a.comments.length === 1 || a.comments.length === 2);
    assert.equal(new Set(a.comments.map((c) => c.personaIndex)).size, a.comments.length);
    for (const comment of a.comments) {
      assert.notEqual(comment.personaIndex, authorIndex);
      assert.ok(comment.body.length > 0 && comment.body.length <= 1000);
    }
  }
});

test('engagement totals are stable', () => {
  let likes = 0;
  let comments = 0;
  for (let i = 0; i < postCount(); i++) {
    const e = engagementFor(i);
    likes += e.likers.length;
    comments += e.comments.length;
  }
  assert.equal(likes, 50);
  assert.equal(comments, 27);
});

test("alice's self-check targets", () => {
  assert.equal(expectedFeedCount(), 16);
  assert.equal(expectedGridCount('alice'), 4);
});
