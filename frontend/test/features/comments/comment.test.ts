import {
  canDeleteComment,
  commentCountLabel,
  commentLength,
  commentSegments,
} from '@features/comments/comment';
import { expect, test } from 'vitest';

test('commentCountLabel is singular for one and plural otherwise', () => {
  expect(commentCountLabel(0)).toBe('0 comments');
  expect(commentCountLabel(1)).toBe('1 comment');
  expect(commentCountLabel(2)).toBe('2 comments');
});

// Mirrors the three permission cases CommentThread.mayDelete pins server-side (#156) — this
// gate must never diverge from it.
const mine = { authorId: 'u-ada' };

test('canDeleteComment allows the comment’s own author', () => {
  expect(canDeleteComment(mine, 'u-ada', 'u-zed')).toBe(true);
});

test('canDeleteComment allows the post’s author', () => {
  expect(canDeleteComment(mine, 'u-zed', 'u-zed')).toBe(true);
});

test('canDeleteComment forbids an unrelated viewer', () => {
  expect(canDeleteComment(mine, 'u-bob', 'u-zed')).toBe(false);
});

test('canDeleteComment forbids when nobody is signed in', () => {
  expect(canDeleteComment(mine, null, 'u-ada')).toBe(false);
});

test('plain text is one segment with no href', () => {
  expect(commentSegments('just a normal comment')).toEqual([{ text: 'just a normal comment' }]);
});

test('a bare URL becomes its own linked segment', () => {
  expect(commentSegments('see https://pictogram.dev/x now')).toEqual([
    { text: 'see ' },
    { text: 'https://pictogram.dev/x', href: 'https://pictogram.dev/x' },
    { text: ' now' },
  ]);
});

test('trailing punctuation stays outside the link', () => {
  const segments = commentSegments('look at https://example.com/page.');
  expect(segments.at(-1)).toEqual({ text: '.' });
  expect(segments.find((segment) => segment.href)?.href).toBe('https://example.com/page');
});

test('http and https are linked; other schemes and @mentions are not', () => {
  expect(commentSegments('mail me at ftp://nope and ask @ansel')).toEqual([
    { text: 'mail me at ftp://nope and ask @ansel' },
  ]);
});

test('counts emoji as a single code point', () => {
  expect(commentLength('a🌄b')).toBe(3);
});
