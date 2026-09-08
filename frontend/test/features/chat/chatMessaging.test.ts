import {
  chatConnectionStatus,
  chatEvents,
  sendChatMessage,
  type ChatEvent,
} from '@features/chat/chatConnection';
import { FakeWebSocket, stubWebSocket } from '@test-support/stubWebSocket';
import { BehaviorSubject } from 'rxjs';
import { afterEach, beforeEach, expect, test } from 'vitest';

let subscription: { unsubscribe: () => void };

function openConnection(): FakeWebSocket {
  const tokens = new BehaviorSubject<string | null>('a-token');
  subscription = chatConnectionStatus(tokens).subscribe();
  const socket = FakeWebSocket.instances[0];
  socket.open();
  return socket;
}

function collectEvents(): ChatEvent[] {
  const events: ChatEvent[] = [];
  chatEvents().subscribe((event) => {
    events.push(event);
  });
  return events;
}

beforeEach(() => {
  stubWebSocket();
});

afterEach(() => {
  subscription.unsubscribe();
});

test('an inbound message frame surfaces as a chat event', () => {
  const socket = openConnection();
  const events = collectEvents();

  socket.receive('{"type":"message","senderUserId":"u-bob","text":"hi there"}');

  expect(events).toEqual([{ type: 'message', senderUserId: 'u-bob', text: 'hi there' }]);
});

test('an inbound undelivered frame surfaces as a chat event', () => {
  const socket = openConnection();
  const events = collectEvents();

  socket.receive('{"type":"undelivered","recipientUserId":"u-bob","text":"anyone?"}');

  expect(events).toEqual([{ type: 'undelivered', recipientUserId: 'u-bob', text: 'anyone?' }]);
});

test('a malformed or unrecognised frame is dropped, not surfaced', () => {
  const socket = openConnection();
  const events = collectEvents();

  socket.receive('not json');
  socket.receive('{"type":"typing"}');
  socket.receive('{"type":"message","senderUserId":"u-bob"}');

  expect(events).toEqual([]);
});

test('sendChatMessage writes the send request frame to the open socket', () => {
  const socket = openConnection();

  expect(sendChatMessage('u-bob', 'hello bob')).toBe('sent');

  expect(socket.sent).toEqual(['{"recipientUserId":"u-bob","text":"hello bob"}']);
});

test('sendChatMessage reports no-connection when there is no open socket', () => {
  const tokens = new BehaviorSubject<string | null>(null);
  subscription = chatConnectionStatus(tokens).subscribe();

  expect(sendChatMessage('u-bob', 'hello?')).toBe('no-connection');
});
