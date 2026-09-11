import { requestAccessTokenRefresh } from '@shared';
import { BehaviorSubject, Observable, of, Subject, timer, type Observer } from 'rxjs';
import { distinctUntilChanged, map, retry, switchMap } from 'rxjs/operators';

export type ChatConnectionStatus = 'closed' | 'connecting' | 'open';

// The two frames chat pushes down a connection (ADR-0014's Delivery language): someone
// else's message, or an "undelivered" outcome for one this connection's caller just sent.
export type IncomingMessage = { type: 'message'; senderUserId: string; text: string };
export type UndeliveredNotice = { type: 'undelivered'; recipientUserId: string; text: string };
export type ChatEvent = IncomingMessage | UndeliveredNotice;

// The answer to a presence query this connection asked (ADR-0014): one user, right now —
// never pushed unsolicited, never a feed of everyone's status.
export type PresenceUpdate = { userId: string; online: boolean };

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
  // Chat shares the app's origin, routed to the chat service by path at the edge
  // (#169, ADR-0014) — one predictable URL, no second port or TLS certificate.
  // VITE_CHAT_PORT points host-based `task chat` dev (chat runs bare on 8081, no
  // Caddy) at that port instead.
  const devPort = import.meta.env.VITE_CHAT_PORT;
  const host = devPort ? `${location.hostname}:${devPort}` : location.host;
  return `${wsProtocol}//${host}/ws`;
}

// One parse per inbound frame, tagged by which stream it belongs on: the chat-event
// stream (message / undelivered) or the presence-answer stream.
type InboundFrame =
  { stream: 'event'; event: ChatEvent } | { stream: 'presence'; update: PresenceUpdate };

function parseInboundFrame(raw: unknown): InboundFrame | null {
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
    return {
      stream: 'event',
      event: { type: 'message', senderUserId: frame.senderUserId, text: frame.text },
    };
  if (
    frame.type === 'undelivered' &&
    typeof frame.recipientUserId === 'string' &&
    typeof frame.text === 'string'
  )
    return {
      stream: 'event',
      event: { type: 'undelivered', recipientUserId: frame.recipientUserId, text: frame.text },
    };
  if (
    frame.type === 'presence' &&
    typeof frame.userId === 'string' &&
    typeof frame.online === 'boolean'
  )
    return { stream: 'presence', update: { userId: frame.userId, online: frame.online } };
  return null;
}

// The live socket, published so `sendChatMessage` can write to it without threading it
// through the RxJS status pipeline. Only ever holds a socket that has reached 'open'.
const openSocket$ = new BehaviorSubject<WebSocket | null>(null);
const inboundEvents$ = new Subject<ChatEvent>();
const presenceUpdates$ = new Subject<PresenceUpdate>();

export function chatEvents(): Observable<ChatEvent> {
  return inboundEvents$.asObservable();
}

// The stream of presence-query answers (ADR-0014). A consumer asks with `queryPresence`
// and filters this to the user it asked about — chat never emits here unprompted.
export function chatPresence(): Observable<PresenceUpdate> {
  return presenceUpdates$.asObservable();
}

// Whether the session's one WebSocket is currently open. Driven by the single connection
// opened at the app shell — subscribing here never opens one.
export function chatSocketOpen(): Observable<boolean> {
  return openSocket$.pipe(
    map((socket) => socket !== null),
    distinctUntilChanged(),
  );
}

// Chat emits every answer to a presence query synchronously into its bounded per-connection
// outbound buffer (chat ChatWebSocketHandler), so one frame naming hundreds of users would
// overflow it and drop the socket. Split a long list across frames, each well under chat's
// own MAX_PRESENCE_QUERY_SUBJECTS.
const PRESENCE_QUERY_CHUNK = 100;

// Asks chat which of these users are connected, right now (ADR-0014) — on-demand only,
// never a poll or a subscription on chat's side. Chat answers one `PresenceUpdate` per id
// on the presence stream. A no-op with no open socket, or an empty list: the answers would
// be lost, and the caller shows nothing rather than a stale state.
export function queryPresence(userIds: string[]): void {
  const socket = openSocket$.value;
  if (!socket) return;
  for (let from = 0; from < userIds.length; from += PRESENCE_QUERY_CHUNK) {
    const chunk = userIds.slice(from, from + PRESENCE_QUERY_CHUNK);
    socket.send(JSON.stringify({ type: 'presence-query', userIds: chunk }));
  }
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

// A fixed subprotocol offered alongside the token: the browser fails the connection unless
// the server echoes back one of the offered subprotocols (WHATWG "establish a WebSocket
// connection"), and the server cannot echo the opaque token. chat selects this constant and
// reads the token from the other offered value — ChatWebSocketHandler#getSubProtocols.
const CHAT_SUBPROTOCOL = 'pictogram-chat';

// A dedicated close code (RFC 6455 private-use range) chat sends when it closes a
// connection because the access token it was handshaked with expired (#192) — distinct
// from an ordinary drop, so the reconnect below refreshes first rather than retrying the
// same, now-invalid token. Must match ChatWebSocketHandler.ACCESS_TOKEN_EXPIRED_CLOSE_CODE.
const ACCESS_TOKEN_EXPIRED_CLOSE_CODE = 4401;

// The token travels as a WebSocket subprotocol, not a query parameter (ADR-0014), so it
// never lands in a proxy's access logs.
function connect(accessToken: string): Observable<ChatConnectionStatus> {
  return new Observable<ChatConnectionStatus>((subscriber: Observer<ChatConnectionStatus>) => {
    subscriber.next('connecting');
    const socket = new WebSocket(chatSocketUrl(), [CHAT_SUBPROTOCOL, accessToken]);
    socket.addEventListener('open', () => {
      openSocket$.next(socket);
      subscriber.next('open');
    });
    socket.addEventListener('message', (event: MessageEvent) => {
      const frame = parseInboundFrame(event.data);
      if (!frame) return;
      if (frame.stream === 'presence') presenceUpdates$.next(frame.update);
      else inboundEvents$.next(frame.event);
    });
    socket.addEventListener('close', (event: CloseEvent) => {
      if (openSocket$.value === socket) openSocket$.next(null);
      // The switchMap in chatConnectionStatus only picks up a refreshed token once
      // accessTokenChanges emits one; retrying below with the same, still-expired token in
      // the meantime is expected — it will keep failing until the refresh lands.
      if (event.code === ACCESS_TOKEN_EXPIRED_CLOSE_CODE) requestAccessTokenRefresh();
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
