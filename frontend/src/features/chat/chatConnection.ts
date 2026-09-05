import { Observable, of, timer, type Observer } from 'rxjs';
import { retry, switchMap } from 'rxjs/operators';

export type ChatConnectionStatus = 'closed' | 'connecting' | 'open';

const BASE_BACKOFF_MS = 1000;
const MAX_BACKOFF_MS = 30_000;

export function backoffDelayMs(retryCount: number): number {
  return Math.min(BASE_BACKOFF_MS * 2 ** (retryCount - 1), MAX_BACKOFF_MS);
}

class ChatConnectionDroppedError extends Error {
  constructor() {
    super('The chat connection dropped.');
    this.name = 'ChatConnectionDroppedError';
  }
}

function chatSocketUrl(): string {
  const wsProtocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
  // Temporary: chat is its own origin (a separate port, fronted by its own Caddy —
  // compose.yaml/Caddyfile) until #169 folds it behind app's origin. VITE_CHAT_PORT
  // overrides the default for host-based `task chat` dev, which runs chat bare on 8081
  // rather than behind the compose-only Caddy route on 8082.
  const port: string = import.meta.env.VITE_CHAT_PORT ?? '8082';
  return `${wsProtocol}//${location.hostname}:${port}/ws`;
}

// The token travels as the WebSocket subprotocol, not a query parameter (ADR-0014), so it
// never lands in a proxy's access logs.
function connect(accessToken: string): Observable<ChatConnectionStatus> {
  return new Observable<ChatConnectionStatus>((subscriber: Observer<ChatConnectionStatus>) => {
    subscriber.next('connecting');
    const socket = new WebSocket(chatSocketUrl(), [accessToken]);
    socket.addEventListener('open', () => {
      subscriber.next('open');
    });
    socket.addEventListener('close', () => {
      subscriber.error(new ChatConnectionDroppedError());
    });
    return () => {
      socket.close();
    };
  });
}

// One WebSocket per signed-in session (ADR-0014): switchMap tears down the previous
// connection the instant the access token changes, including to null on sign-out.
export function chatConnectionStatus(
  accessTokens: Observable<string | null>,
): Observable<ChatConnectionStatus> {
  return accessTokens.pipe(
    switchMap((token) => {
      if (!token) return of<ChatConnectionStatus>('closed');
      // No `resetOnSuccess`: `connect` emits 'connecting' synchronously on every
      // subscribe, which RxJS counts as a qualifying success and would reset the
      // backoff before a real 'open' ever happens — so backoff only grows, capped at
      // MAX_BACKOFF_MS, rather than resetting after a healthy stretch.
      return connect(token).pipe(
        retry({ delay: (_error, retryCount) => timer(backoffDelayMs(retryCount)) }),
      );
    }),
  );
}
