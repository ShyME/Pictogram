import { readCsrfToken } from '@features/auth/csrf';
import { stubCookieJar } from '@test-support/cookieJar';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.restoreAllMocks();
});

test('reads the XSRF-TOKEN cookie value', () => {
  stubCookieJar('XSRF-TOKEN=a-double-submit-token');

  expect(readCsrfToken()).toBe('a-double-submit-token');
});

test('picks XSRF-TOKEN out from among other cookies', () => {
  stubCookieJar('other=one; XSRF-TOKEN=the-token; another=two');

  expect(readCsrfToken()).toBe('the-token');
});

test('url-decodes the cookie value', () => {
  stubCookieJar(`XSRF-TOKEN=${encodeURIComponent('a/b+c=d')}`);

  expect(readCsrfToken()).toBe('a/b+c=d');
});

test('is null when no XSRF-TOKEN cookie is set', () => {
  stubCookieJar('unrelated=value');

  expect(readCsrfToken()).toBeNull();
});
