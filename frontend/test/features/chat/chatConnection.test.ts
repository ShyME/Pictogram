import {
  backoffDelayMs,
  chatConnectionStatus,
  type ChatConnectionStatus,
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

test('opens exactly one socket, carrying the token as the subprotocol', () => {
  record('a-token');

  expect(FakeWebSocket.instances).toHaveLength(1);
  expect(FakeWebSocket.instances[0]?.protocols).toEqual(['a-token']);
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
