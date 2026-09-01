import { signInErrorMessage } from '@features/auth/sign-in-error';

test('no reason -> no message', () => {
  expect(signInErrorMessage(null)).toBeNull();
  expect(signInErrorMessage(undefined)).toBeNull();
  expect(signInErrorMessage('')).toBeNull();
});

test('known reasons get their own copy', () => {
  expect(signInErrorMessage('email-unverified')).toMatch(/verif/i);
  expect(signInErrorMessage('email-missing')).toMatch(/email address/i);
});

test('an unrecognised reason falls back to the generic message', () => {
  expect(signInErrorMessage('something-new')).toBe(signInErrorMessage('sign-in-failed'));
});

test('a prototype key in the reason still yields a plain string, not an object', () => {
  for (const evil of ['__proto__', 'constructor', 'hasOwnProperty', 'toString']) {
    expect(typeof signInErrorMessage(evil)).toBe('string');
  }
});
