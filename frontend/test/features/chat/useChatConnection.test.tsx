import { useChatConnection } from '@features/chat';
import { publishAccessToken } from '@shared';
import { FakeWebSocket, stubWebSocket } from '@test-support/stubWebSocket';
import { act, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, expect, test } from 'vitest';

function ConnectionProbe() {
  const status = useChatConnection();
  return <output>{status}</output>;
}

beforeEach(() => {
  stubWebSocket();
});

afterEach(() => {
  publishAccessToken(null);
});

test('stays closed with no signed-in session', () => {
  render(<ConnectionProbe />);

  expect(screen.getByRole('status')).toHaveTextContent('closed');
  expect(FakeWebSocket.instances).toHaveLength(0);
});

test('opens one connection once a session signs in, and reflects the handshake completing', () => {
  render(<ConnectionProbe />);

  act(() => {
    publishAccessToken('a-token');
  });
  expect(screen.getByRole('status')).toHaveTextContent('connecting');
  expect(FakeWebSocket.instances).toHaveLength(1);

  act(() => {
    FakeWebSocket.instances[0]?.open();
  });
  expect(screen.getByRole('status')).toHaveTextContent('open');
});

test('closes on sign-out', () => {
  render(<ConnectionProbe />);
  act(() => {
    publishAccessToken('a-token');
    FakeWebSocket.instances[0]?.open();
  });

  act(() => {
    publishAccessToken(null);
  });

  expect(screen.getByRole('status')).toHaveTextContent('closed');
  expect(FakeWebSocket.instances[0]?.closed).toBe(true);
});
