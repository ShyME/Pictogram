import { backoffDelayMs, chatConnectionStatus } from '@features/chat/chatConnection';
import { usePresences } from '@features/chat/usePresences';
import { FakeWebSocket, stubWebSocket } from '@test-support/stubWebSocket';
import { act, render, screen } from '@testing-library/react';
import { BehaviorSubject } from 'rxjs';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';

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

function PresencesProbe({ userIds }: { userIds: string[] }) {
  const presences = usePresences(userIds);
  return (
    <ul>
      {userIds.map((id) => (
        <li key={id} data-testid={id}>
          {presences.get(id) ?? 'missing'}
        </li>
      ))}
    </ul>
  );
}

beforeEach(() => {
  stubWebSocket();
  vi.useFakeTimers();
});

afterEach(() => {
  connection?.unsubscribe();
  connection = undefined;
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

test('batch-queries chat for every id once the socket is open', () => {
  const socket = openSocket();

  render(<PresencesProbe userIds={['u-ada', 'u-bob']} />);

  expect(socket.sent).toEqual(['{"type":"presence-query","userIds":["u-ada","u-bob"]}']);
  expect(screen.getByTestId('u-ada')).toHaveTextContent('unknown');
  expect(screen.getByTestId('u-bob')).toHaveTextContent('unknown');
});

test('resolves each id from its own presence answer and ignores unrequested users', () => {
  const socket = openSocket();
  render(<PresencesProbe userIds={['u-ada', 'u-bob']} />);

  act(() => {
    socket.receive('{"type":"presence","userId":"u-ada","online":true}');
    socket.receive('{"type":"presence","userId":"u-bob","online":false}');
    socket.receive('{"type":"presence","userId":"u-zoe","online":true}');
  });

  expect(screen.getByTestId('u-ada')).toHaveTextContent('online');
  expect(screen.getByTestId('u-bob')).toHaveTextContent('offline');
});

test('re-queries on socket reopen and drops every id to unknown while closed', async () => {
  const socket = openSocket();
  render(<PresencesProbe userIds={['u-ada']} />);
  act(() => {
    socket.receive('{"type":"presence","userId":"u-ada","online":true}');
  });
  expect(screen.getByTestId('u-ada')).toHaveTextContent('online');

  act(() => {
    socket.simulateDrop();
  });
  expect(screen.getByTestId('u-ada')).toHaveTextContent('unknown');

  await act(async () => {
    await vi.advanceTimersByTimeAsync(backoffDelayMs(1));
  });
  const reconnected = FakeWebSocket.instances[1];
  act(() => {
    reconnected.open();
  });

  expect(reconnected.sent).toEqual(['{"type":"presence-query","userIds":["u-ada"]}']);
});

test('re-queries on the 30s poll while the socket stays open', async () => {
  const socket = openSocket();
  render(<PresencesProbe userIds={['u-ada']} />);
  expect(socket.sent).toHaveLength(1);

  await act(async () => {
    await vi.advanceTimersByTimeAsync(30_000);
  });
  expect(socket.sent).toHaveLength(2);

  await act(async () => {
    await vi.advanceTimersByTimeAsync(30_000);
  });
  const batchFrame = '{"type":"presence-query","userIds":["u-ada"]}';
  expect(socket.sent).toEqual([batchFrame, batchFrame, batchFrame]);
});

test('keeps a known answer when the same ids are re-passed in a different order', () => {
  const socket = openSocket();
  const { rerender } = render(<PresencesProbe userIds={['u-ada', 'u-bob']} />);
  act(() => {
    socket.receive('{"type":"presence","userId":"u-ada","online":true}');
  });
  expect(screen.getByTestId('u-ada')).toHaveTextContent('online');

  rerender(<PresencesProbe userIds={['u-bob', 'u-ada']} />);

  expect(screen.getByTestId('u-ada')).toHaveTextContent('online');
});

test('sets up no poll and sends nothing for an empty id list', async () => {
  const socket = openSocket();
  render(<PresencesProbe userIds={[]} />);

  await act(async () => {
    await vi.advanceTimersByTimeAsync(30_000);
  });

  expect(socket.sent).toEqual([]);
});

test('leaves every id unknown and sends nothing while the socket is closed', () => {
  render(<PresencesProbe userIds={['u-ada', 'u-bob']} />);

  expect(FakeWebSocket.instances).toHaveLength(0);
  expect(screen.getByTestId('u-ada')).toHaveTextContent('unknown');
  expect(screen.getByTestId('u-bob')).toHaveTextContent('unknown');
});
