import { chatConnectionStatus } from '@features/chat/chatConnection';
import { usePresence } from '@features/chat/usePresence';
import { FakeWebSocket, stubWebSocket } from '@test-support/stubWebSocket';
import { act, render, screen } from '@testing-library/react';
import { BehaviorSubject } from 'rxjs';
import { afterEach, beforeEach, expect, test } from 'vitest';

let connection: { unsubscribe: () => void } | undefined;

function openSocket(): FakeWebSocket {
  const tokens = new BehaviorSubject<string | null>('a-token');
  connection = chatConnectionStatus(tokens).subscribe();
  const socket = FakeWebSocket.instances[0];
  act(() => {
    socket.open();
  });
  return socket;
}

function PresenceProbe({ userId }: { userId: string }) {
  return <output>{usePresence(userId)}</output>;
}

beforeEach(() => {
  stubWebSocket();
});

afterEach(() => {
  connection?.unsubscribe();
  connection = undefined;
});

test('queries chat for the user once the socket is open', () => {
  const socket = openSocket();

  render(<PresenceProbe userId="u-ada" />);

  expect(socket.sent).toEqual(['{"type":"presence-query","userId":"u-ada"}']);
  expect(screen.getByRole('status')).toHaveTextContent('unknown');
});

test('reflects the answer for that user, ignoring answers for anyone else', () => {
  const socket = openSocket();
  render(<PresenceProbe userId="u-ada" />);

  act(() => {
    socket.receive('{"type":"presence","userId":"u-zoe","online":true}');
  });
  expect(screen.getByRole('status')).toHaveTextContent('unknown');

  act(() => {
    socket.receive('{"type":"presence","userId":"u-ada","online":true}');
  });
  expect(screen.getByRole('status')).toHaveTextContent('online');

  act(() => {
    socket.receive('{"type":"presence","userId":"u-ada","online":false}');
  });
  expect(screen.getByRole('status')).toHaveTextContent('offline');
});

test('drops back to unknown when the socket closes', () => {
  const socket = openSocket();
  render(<PresenceProbe userId="u-ada" />);
  act(() => {
    socket.receive('{"type":"presence","userId":"u-ada","online":true}');
  });
  expect(screen.getByRole('status')).toHaveTextContent('online');

  act(() => {
    socket.simulateDrop();
  });

  expect(screen.getByRole('status')).toHaveTextContent('unknown');
});

test('stays unknown and sends nothing while the socket is closed', () => {
  render(<PresenceProbe userId="u-ada" />);

  expect(FakeWebSocket.instances).toHaveLength(0);
  expect(screen.getByRole('status')).toHaveTextContent('unknown');
});
