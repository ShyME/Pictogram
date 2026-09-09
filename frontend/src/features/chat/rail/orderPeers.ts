import type { ChatPeer } from '../chatStore';
import type { Presence } from '../usePresence';

export function railName(peer: ChatPeer): string {
  return peer.displayName ?? `@${peer.username}`;
}

// Rail ordering (#203, #204): unread first, then online, then everyone else, alphabetical
// by display name within each group. A peer whose presence is `offline` or `unknown` sorts
// the same — the rail only promotes people it knows are reachable — and an unread peer
// sorts in the unread group whether or not they are online.
export function orderPeers(
  peers: ChatPeer[],
  presences: Map<string, Presence>,
  unread: ReadonlySet<string>,
): ChatPeer[] {
  const rank = (peer: ChatPeer) => {
    if (unread.has(peer.userId)) return 0;
    return presences.get(peer.userId) === 'online' ? 1 : 2;
  };
  const ordered = [...peers];
  ordered.sort(
    (a, b) =>
      rank(a) - rank(b) ||
      railName(a).localeCompare(railName(b), undefined, { sensitivity: 'base' }),
  );
  return ordered;
}
