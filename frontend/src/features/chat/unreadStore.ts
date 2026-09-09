import { useSyncExternalStore } from 'react';

// Session-local unread markers for the chat rail (#204, ADR-0014): peers who have sent a
// message the viewer hasn't opened yet. Held only in memory — a reload clears every
// marker, by design. Chat keeps no history, so there is nothing to reconcile a persisted
// marker against.

let unread: ReadonlySet<string> = new Set();
const listeners = new Set<() => void>();

function notify(): void {
  for (const listener of listeners) listener();
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export function markPeerUnread(peerUserId: string): void {
  if (unread.has(peerUserId)) return;
  unread = new Set(unread).add(peerUserId);
  notify();
}

export function clearPeerUnread(peerUserId: string): void {
  if (!unread.has(peerUserId)) return;
  unread = new Set([...unread].filter((id) => id !== peerUserId));
  notify();
}

// The non-reactive read, mirroring chatStore's `openConversationPeer()` beside its hook.
export function unreadPeerIds(): ReadonlySet<string> {
  return unread;
}

export function useUnreadPeerIds(): ReadonlySet<string> {
  return useSyncExternalStore(
    subscribe,
    () => unread,
    () => unread,
  );
}

export function useIsAnyPeerUnread(): boolean {
  return useSyncExternalStore(
    subscribe,
    () => unread.size > 0,
    () => false,
  );
}
