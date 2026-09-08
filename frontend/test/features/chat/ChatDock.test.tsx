import { ChatDock } from '@features/chat';
import { closeConversation, openConversation } from '@features/chat/chatStore';
import { publishAccessToken, Toaster } from '@shared';
import { jsonResponse, pathOf, stubFetch } from '@test-support/mockFetch';
import { FakeWebSocket, stubWebSocket } from '@test-support/stubWebSocket';
import { act, fireEvent, render, screen, within } from '@testing-library/react';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';

const ada = { userId: 'u-ada', username: 'ada', displayName: 'Ada Lovelace' };

function mountDock() {
  return render(
    <>
      <ChatDock />
      <Toaster />
    </>,
  );
}

function connectedSocket(): FakeWebSocket {
  act(() => {
    publishAccessToken('a-token');
  });
  const socket = FakeWebSocket.instances[0];
  act(() => {
    socket.open();
  });
  return socket;
}

beforeEach(() => {
  stubWebSocket();
  stubFetch((request) => {
    if (pathOf(request) === '/api/profiles')
      return jsonResponse([{ userId: 'u-zoe', username: 'zoe', displayName: 'Zoe' }]);
    return jsonResponse({});
  });
});

afterEach(() => {
  act(() => {
    publishAccessToken(null);
    closeConversation();
  });
  vi.unstubAllGlobals();
});

test('a message for the open conversation renders live in the overlay, with no toast', async () => {
  mountDock();
  const socket = connectedSocket();
  act(() => {
    openConversation(ada);
  });

  act(() => {
    socket.receive('{"type":"message","senderUserId":"u-ada","text":"on my way"}');
  });

  const dialog = screen.getByRole('dialog', { name: 'Chat with Ada Lovelace' });
  expect(await within(dialog).findByText('on my way')).toBeInTheDocument();
  expect(screen.queryByText(/new message from/i)).not.toBeInTheDocument();
});

test('a message for another conversation surfaces as a toast naming the sender', async () => {
  mountDock();
  const socket = connectedSocket();
  act(() => {
    openConversation(ada);
  });

  act(() => {
    socket.receive('{"type":"message","senderUserId":"u-zoe","text":"knock knock"}');
  });

  expect(await screen.findByText('New message from Zoe')).toBeInTheDocument();
  expect(await screen.findByText('knock knock')).toBeInTheDocument();
  const dialog = screen.getByRole('dialog', { name: 'Chat with Ada Lovelace' });
  expect(within(dialog).queryByText('knock knock')).not.toBeInTheDocument();
});

test('an undelivered notice for the open conversation flips the sent message', () => {
  mountDock();
  const socket = connectedSocket();
  act(() => {
    openConversation(ada);
  });

  fireEvent.change(screen.getByRole('textbox'), { target: { value: 'you there?' } });
  fireEvent.click(screen.getByRole('button', { name: 'Send' }));
  const dialog = screen.getByRole('dialog');
  expect(within(dialog).queryByText('Not delivered')).not.toBeInTheDocument();

  act(() => {
    socket.receive('{"type":"undelivered","recipientUserId":"u-ada","text":"you there?"}');
  });

  expect(within(dialog).getByText('Not delivered')).toBeInTheDocument();
});
