import {
  readFeedScopePreference,
  writeFeedScopePreference,
} from '@features/feed/feedScopePreference';
import { afterEach, beforeEach, expect, test } from 'vitest';

beforeEach(() => {
  localStorage.clear();
});

afterEach(() => {
  localStorage.clear();
});

test('defaults to following when nothing is stored', () => {
  expect(readFeedScopePreference()).toBe('following');
});

test('remembers an explore choice across reads', () => {
  writeFeedScopePreference('explore');

  expect(readFeedScopePreference()).toBe('explore');
});

test('switching back to following overwrites the stored choice', () => {
  writeFeedScopePreference('explore');
  writeFeedScopePreference('following');

  expect(readFeedScopePreference()).toBe('following');
});
