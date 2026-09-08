import { BehaviorSubject, Observable, of, Subject, timer, type Observer } from 'rxjs';
import { retry, switchMap } from 'rxjs/operators';

export type ChatConnectionStatus = 'closed' | 'connecting' | 'open';

// The two frames chat pushes down a connection (ADR-0014's Delivery language): someone
// else's message, or an "undelivered" outcome for one this connection's caller just sent.
export type IncomingMessage = { type: 'message'; senderUserId: string; text: string };
export type UndeliveredNotice = { type: 'undelivered'; recipientUserId: string; text: string };
export type ChatEvent = IncomingMessage | UndeliveredNotice;

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

function parseChatEvent(raw: unknown): ChatEvent | null {
  if (typeof raw !== 'string') return null;
  let value: unknown;
  try {
    value = JSON.parse(raw);
  } catch {
    return null;
  }
  if (typeof value !== 'object' || value === null) return null;
  const frame = value as Record<string, unknown>;
  if (
    frame.type === 'message' &&
    typeof frame.senderUserId === 'string' &&
    typeof frame.text === 'string'
  )
    return { type: 'message', senderUserId: frame.senderUserId, text: frame.text };
  if (
    frame.type === 'undelivered' &&
    typeof frame.recipientUserId === 'string' &&
    typeof frame.text === 'string'
  )
    return { type: 'undelivered', recipientUserId: frame.recipientUserId, text: frame.text };
  return null;
}

// The live socket, published so `sendChatMessage` can write to it without threading it
// through the RxJS status pipeline. Only ever holds a socket that has reached 'open'.
const openSocket$ = new BehaviorSubject<WebSocket | null>(null);
const inboundEvents$ = new Subject<ChatEvent>();

export function chatEvents(): Observable<ChatEvent> {
  return inboundEvents$.asObservable();
}

export type MessageDelivery = 'sent' | 'no-connection';

// Best-effort by design (ADR-0014): a message is relayed live or not at all. 'no-connection'
// means there was no open socket to write to — the caller reports that undelivered itself,
// since chat will never send a notice back for a send it never received.
export function sendChatMessage(recipientUserId: string, text: string): MessageDelivery {
  const socket = openSocket$.value;
  if (!socket) return 'no-connection';
  socket.send(JSON.stringify({ recipientUserId, text }));
  return 'sent';
}

// The token travels as the WebSocket subprotocol, not a query parameter (ADR-0014), so it
// never lands in a proxy's access logs.
function connect(accessToken: string): Observable<ChatConnectionStatus> {
  return new Observable<ChatConnectionStatus>((subscriber: Observer<ChatConnectionStatus>) => {
    subscriber.next('connecting');
    const socket = new WebSocket(chatSocketUrl(), [accessToken]);
    socket.addEventListener('open', () => {
      openSocket$.next(socket);
      subscriber.next('open');
    });
    socket.addEventListener('message', (event: MessageEvent) => {
      const chatEvent = parseChatEvent(event.data);
      if (chatEvent) inboundEvents$.next(chatEvent);
    });
    socket.addEventListener('close', () => {
      if (openSocket$.value === socket) openSocket$.next(null);
      subscriber.error(new ChatConnectionDroppedError());
    });
    return () => {
      if (openSocket$.value === socket) openSocket$.next(null);
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
