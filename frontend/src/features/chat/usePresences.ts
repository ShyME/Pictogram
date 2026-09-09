import { useEffect, useMemo, useState } from 'react';
import { chatPresence, chatSocketOpen, queryPresence } from './chatConnection';
import type { Presence } from './usePresence';

// Chat answers a presence query once; the chat sidebar's follow list (#202, #203) needs it
// to stay current as people come and go, so the batch query is re-asked on a slow poll
// while the socket is open. Still client-driven — chat never pushes a feed (ADR-0014).
const PRESENCE_POLL_MS = 30_000;

// A key for the *set* of ids: a caller re-ordering the same ids (the sidebar orders by
// presence) must not re-run the effect and flash every dot back to 'unknown'.
function presenceKey(userIds: string[]): string {
  // eslint-disable-next-line unicorn/no-array-sort -- toSorted needs the ES2023 lib; the app targets ES2022 and this is a throwaway copy
  return JSON.stringify([...new Set(userIds)].sort((a, b) => a.localeCompare(b)));
}

// Presence for many users at once, for the chat sidebar. Asks chat on mount, on every
// socket (re)open, and on a 30s poll while the socket is open. Every requested id resolves
// to 'unknown' until answered, and all of them fall back to 'unknown' while the socket is
// closed, since a query sent then is lost.
export function usePresences(userIds: string[]): Map<string, Presence> {
  // Memoised so a caller passing a stable id array only rebuilds the key (a sort + a
  // stringify over the whole list) when the set actually changes, not every render.
  const key = useMemo(() => presenceKey(userIds), [userIds]);
  const [presences, setPresences] = useState<Map<string, Presence>>(() => new Map());

  useEffect(() => {
    const ids = JSON.parse(key) as string[];
    const wanted = new Set(ids);
    const allUnknown = () => new Map<string, Presence>(ids.map((id) => [id, 'unknown']));

    const answers = chatPresence().subscribe((update) => {
      if (!wanted.has(update.userId)) return;
      setPresences((prev) =>
        new Map(prev).set(update.userId, update.online ? 'online' : 'offline'),
      );
    });

    let poll: ReturnType<typeof setInterval> | undefined;
    const asks = chatSocketOpen().subscribe((open) => {
      clearInterval(poll);
      poll = undefined;
      setPresences(allUnknown());
      if (!open || ids.length === 0) return;
      queryPresence(ids);
      poll = setInterval(() => {
        queryPresence(ids);
      }, PRESENCE_POLL_MS);
    });

    return () => {
      answers.unsubscribe();
      asks.unsubscribe();
      clearInterval(poll);
    };
  }, [key]);

  return presences;
}
