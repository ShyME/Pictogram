import { relativeTime } from '@shared/lib/relativeTime';
import { expect, test } from 'vitest';

const now = new Date('2026-09-01T12:00:00Z');

test("calls anything under ~45s 'just now'", () => {
  expect(relativeTime('2026-09-01T11:59:30Z', now)).toBe('just now');
});

test('counts minutes, then hours, then days, then weeks', () => {
  expect(relativeTime('2026-09-01T11:40:00Z', now)).toBe('20m');
  expect(relativeTime('2026-09-01T09:00:00Z', now)).toBe('3h');
  expect(relativeTime('2026-08-29T12:00:00Z', now)).toBe('3d');
  expect(relativeTime('2026-08-11T12:00:00Z', now)).toBe('3w');
});

test('falls back to a calendar date once it is more than a month old', () => {
  expect(relativeTime('2026-06-15T12:00:00Z', now)).toMatch(/Jun/);
});

test('returns an empty string for an unparseable timestamp', () => {
  expect(relativeTime('not-a-date', now)).toBe('');
});
