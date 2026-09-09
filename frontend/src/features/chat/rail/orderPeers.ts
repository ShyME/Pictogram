import type { ChatPeer } from '../chatStore';
import type { Presence } from '../usePresence';

export function railName(peer: ChatPeer): string {
  return peer.displayName ?? `@${peer.username}`;
}

// Rail ordering (#203): online first, then everyone else, alphabetical by display name
// within each group. A peer whose presence is `offline` or `unknown` sorts the same —
// the rail only promotes people it knows are reachable. (#204 adds an unread group above
// online.)
export function orderPeers(peers: ChatPeer[], presences: Map<string, Presence>): ChatPeer[] {
  const onlineFirst = (peer: ChatPeer) => (presences.get(peer.userId) === 'online' ? 0 : 1);
  const ordered = [...peers];
  ordered.sort(
    (a, b) =>
      onlineFirst(a) - onlineFirst(b) ||
      railName(a).localeCompare(railName(b), undefined, { sensitivity: 'base' }),
  );
  return ordered;
}
