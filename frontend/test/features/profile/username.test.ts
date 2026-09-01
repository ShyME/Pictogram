import { isUsernameShapeValid, usernameError } from '@features/profile/username';
import { expect, test } from 'vitest';

test.each(['ada', 'ada_lovelace', 'a1_', 'abc', 'a'.repeat(20)])(
  'accepts well-formed username %j',
  (value) => {
    expect(isUsernameShapeValid(value)).toBe(true);
  },
);

test.each(['', 'ab', 'a'.repeat(21), 'Ada', 'ada lovelace', 'adá', 'ada-lovelace', 'ada!'])(
  'rejects malformed username %j',
  (value) => {
    expect(isUsernameShapeValid(value)).toBe(false);
  },
);

test('usernameError prefers a live shape check over the server verdict', () => {
  expect(usernameError('No Good', 'username-taken')).toBe('username-shape');
});

test('usernameError falls back to the server verdict when the shape is fine', () => {
  expect(usernameError('ada_lovelace', 'username-taken')).toBe('username-taken');
  expect(usernameError('ada_lovelace', 'username-shape')).toBe('username-shape');
});

test('usernameError ignores a clear field and non-username server errors', () => {
  expect(usernameError('ada_lovelace', 'details')).toBeNull();
  expect(usernameError('ada_lovelace', null)).toBeNull();
  expect(usernameError('', null)).toBeNull();
});
