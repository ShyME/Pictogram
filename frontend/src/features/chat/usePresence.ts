import { useEffect, useState } from 'react';
import { chatPresence, chatSocketOpen, queryPresence } from './chatConnection';

// 'unknown' until chat answers — the socket may be closed, or the reply not back yet.
export type Presence = 'online' | 'offline' | 'unknown';

// Asks chat whether `userId` is connected (ADR-0014): once when this entry point mounts,
// and again whenever the session's socket (re)opens, since a query sent while it is closed
// is lost. Not a poll and not a server-side subscription — one question, one answer.
export function usePresence(userId: string): Presence {
  const [answer, setAnswer] = useState<{ userId: string; presence: Presence }>({
    userId,
    presence: 'unknown',
  });

  useEffect(() => {
    const answers = chatPresence().subscribe((update) => {
      if (update.userId === userId) {
        setAnswer({ userId, presence: update.online ? 'online' : 'offline' });
      }
    });
    const asks = chatSocketOpen().subscribe((open) => {
      // Closed socket: chat can't be asked, so drop back to 'unknown' rather than leave a
      // stale dot lit through the reconnect window.
      if (open) queryPresence([userId]);
      else setAnswer({ userId, presence: 'unknown' });
    });
    return () => {
      answers.unsubscribe();
      asks.unsubscribe();
    };
  }, [userId]);

  // A stale answer for the previous peer is not this one's presence.
  return answer.userId === userId ? answer.presence : 'unknown';
}
