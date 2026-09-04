import { commentLength, commentSegments } from '@features/comments/comment';
import { expect, test } from 'vitest';

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
