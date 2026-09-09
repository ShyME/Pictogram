import type { ChatPeer } from '@features/chat';
import { orderPeers, railName } from '@features/chat/rail/orderPeers';
import type { Presence } from '@features/chat/usePresence';
import { expect, test } from 'vitest';

const peer = (userId: string, displayName: string | null, username = userId): ChatPeer => ({
  userId,
  username,
  displayName,
});

function ordered(peers: ChatPeer[], presence: Record<string, Presence>): string[] {
  return orderPeers(peers, new Map(Object.entries(presence))).map((p) => p.userId);
}

test('online peers sort above everyone else', () => {
  const peers = [peer('a', 'Ada'), peer('b', 'Bo'), peer('c', 'Cal')];

  expect(ordered(peers, { a: 'offline', b: 'online', c: 'unknown' })).toEqual(['b', 'a', 'c']);
});

test('within a group, orders alphabetically by display name, case-insensitively', () => {
  const peers = [peer('z', 'zara'), peer('a', 'Aaron'), peer('m', 'Mabel')];

  expect(ordered(peers, {})).toEqual(['a', 'm', 'z']);
});

test('sorts each presence group independently', () => {
  const peers = [
    peer('cal-on', 'Cal'),
    peer('ada-off', 'Ada'),
    peer('bo-on', 'Bo'),
    peer('dot-off', 'Dot'),
  ];

  expect(ordered(peers, { 'cal-on': 'online', 'bo-on': 'online' })).toEqual([
    'bo-on',
    'cal-on',
    'ada-off',
    'dot-off',
  ]);
});

test('treats offline and unknown presence identically', () => {
  const peers = [peer('a', 'Ada'), peer('b', 'Bo')];

  expect(ordered(peers, { a: 'unknown', b: 'offline' })).toEqual(['a', 'b']);
});

test('does not mutate the input array', () => {
  const peers = [peer('b', 'Bo'), peer('a', 'Ada')];
  orderPeers(peers, new Map());

  expect(peers.map((p) => p.userId)).toEqual(['b', 'a']);
});

test('railName falls back to the @handle when there is no display name', () => {
  expect(railName(peer('u', null, 'ada'))).toBe('@ada');
  expect(railName(peer('u', 'Ada Lovelace', 'ada'))).toBe('Ada Lovelace');
});
