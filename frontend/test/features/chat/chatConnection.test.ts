import {
  backoffDelayMs,
  chatConnectionStatus,
  chatPresence,
  queryPresence,
  type ChatConnectionStatus,
  type PresenceUpdate,
} from '@features/chat/chatConnection';
import { FakeWebSocket, stubWebSocket } from '@test-support/stubWebSocket';
import { BehaviorSubject } from 'rxjs';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';

beforeEach(() => {
  stubWebSocket();
  vi.useFakeTimers();
});

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

function record(accessToken: string | null): {
  tokens: BehaviorSubject<string | null>;
  statuses: ChatConnectionStatus[];
} {
  const tokens = new BehaviorSubject<string | null>(accessToken);
  const statuses: ChatConnectionStatus[] = [];
  chatConnectionStatus(tokens).subscribe((status) => {
    statuses.push(status);
  });
  return { tokens, statuses };
}

test('opens exactly one socket, offering the fixed subprotocol then the token', () => {
  record('a-token');

  expect(FakeWebSocket.instances).toHaveLength(1);
  // The server can only echo a subprotocol it was offered, and it can't echo the opaque
  // token — so a fixed value goes first for it to select, or the browser drops the socket.
  expect(FakeWebSocket.instances[0]?.protocols).toEqual(['pictogram-chat', 'a-token']);
});

test('connects to /ws on the page origin (chat shares app’s Caddy origin, #169)', () => {
  record('a-token');

  expect(FakeWebSocket.instances[0]?.url).toBe(`ws://${location.host}/ws`);
});

test('reports connecting then open as the handshake completes', () => {
  const { statuses } = record('a-token');
  expect(statuses).toEqual(['connecting']);

  FakeWebSocket.instances[0]?.open();

  expect(statuses).toEqual(['connecting', 'open']);
});

test('closes the socket and reports closed once the token clears (sign-out)', () => {
  const { tokens, statuses } = record('a-token');
  FakeWebSocket.instances[0]?.open();

  tokens.next(null);

  expect(FakeWebSocket.instances[0]?.closed).toBe(true);
  expect(statuses).toEqual(['connecting', 'open', 'closed']);
});

test('a dropped connection is retried after a backoff delay', async () => {
  const { statuses } = record('a-token');
  FakeWebSocket.instances[0]?.open();

  FakeWebSocket.instances[0]?.simulateDrop();
  expect(FakeWebSocket.instances).toHaveLength(1);

  await vi.advanceTimersByTimeAsync(backoffDelayMs(1) - 1);
  expect(FakeWebSocket.instances).toHaveLength(1);

  await vi.advanceTimersByTimeAsync(1);
  expect(FakeWebSocket.instances).toHaveLength(2);

  FakeWebSocket.instances[1]?.open();
  expect(statuses).toEqual(['connecting', 'open', 'connecting', 'open']);
});

test('queryPresence writes an on-demand presence-query frame to the open socket', () => {
  const { statuses } = record('a-token');
  FakeWebSocket.instances[0]?.open();
  expect(statuses).toContain('open');

  queryPresence('u-ada');

  expect(FakeWebSocket.instances[0]?.sent).toEqual(['{"type":"presence-query","userId":"u-ada"}']);
});

test('queryPresence is a no-op with no open socket', () => {
  queryPresence('u-ada');

  expect(FakeWebSocket.instances).toHaveLength(0);
});

test('chatPresence emits the parsed answer to a presence query', () => {
  record('a-token');
  FakeWebSocket.instances[0]?.open();
  const updates: PresenceUpdate[] = [];
  chatPresence().subscribe((update) => {
    updates.push(update);
  });

  FakeWebSocket.instances[0]?.receive('{"type":"presence","userId":"u-ada","online":true}');
  FakeWebSocket.instances[0]?.receive('{"type":"presence","userId":"u-zoe","online":false}');
  FakeWebSocket.instances[0]?.receive('{"type":"message","senderUserId":"u-ada","text":"hi"}');

  expect(updates).toEqual([
    { userId: 'u-ada', online: true },
    { userId: 'u-zoe', online: false },
  ]);
});

test('backoff grows across consecutive drops before a success', async () => {
  record('a-token');

  FakeWebSocket.instances[0]?.simulateDrop();
  await vi.advanceTimersByTimeAsync(backoffDelayMs(1));
  expect(FakeWebSocket.instances).toHaveLength(2);

  FakeWebSocket.instances[1]?.simulateDrop();
  await vi.advanceTimersByTimeAsync(backoffDelayMs(2) - 1);
  expect(FakeWebSocket.instances).toHaveLength(2);

  await vi.advanceTimersByTimeAsync(1);
  expect(FakeWebSocket.instances).toHaveLength(3);
});
