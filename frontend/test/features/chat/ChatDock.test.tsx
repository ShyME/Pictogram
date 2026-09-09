import { ChatDock } from '@features/chat';
import { closeConversation, openConversation } from '@features/chat/chatStore';
import { clearPeerUnread, unreadPeerIds } from '@features/chat/unreadStore';
import { dismissToast, publishAccessToken, Toaster, useToast } from '@shared';
import { jsonResponse, pathOf, stubFetch } from '@test-support/mockFetch';
import { FakeWebSocket, stubWebSocket } from '@test-support/stubWebSocket';
import { act, fireEvent, render, renderHook, screen, within } from '@testing-library/react';
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
  const { result } = renderHook(() => useToast());
  act(() => {
    publishAccessToken(null);
    closeConversation();
    for (const id of unreadPeerIds()) clearPeerUnread(id);
    for (const queued of result.current.toasts) dismissToast(queued.id);
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
  const adaDialog = screen.getByRole('dialog', { name: 'Chat with Ada Lovelace' });
  expect(within(adaDialog).queryByText('knock knock')).not.toBeInTheDocument();
});

test('a message from a non-open conversation marks that peer unread', async () => {
  mountDock();
  const socket = connectedSocket();
  act(() => {
    openConversation(ada);
  });

  act(() => {
    socket.receive('{"type":"message","senderUserId":"u-zoe","text":"knock knock"}');
  });

  expect([...unreadPeerIds()]).toEqual(['u-zoe']);
  // Let the toast's async sender lookup settle so it doesn't leak into the next test.
  await screen.findByText('New message from Zoe');
});

test('a message marks the sender unread when no conversation is open at all', async () => {
  mountDock();
  const socket = connectedSocket();

  act(() => {
    socket.receive('{"type":"message","senderUserId":"u-zoe","text":"anyone home?"}');
  });

  expect([...unreadPeerIds()]).toEqual(['u-zoe']);
  await screen.findByText('New message from Zoe');
});

test('a message for the currently-open conversation never marks unread', () => {
  mountDock();
  const socket = connectedSocket();
  act(() => {
    openConversation(ada);
  });

  act(() => {
    socket.receive('{"type":"message","senderUserId":"u-ada","text":"on my way"}');
  });

  expect(unreadPeerIds().has('u-ada')).toBe(false);
});

test('opening the marked conversation from the toast clears its unread marker', async () => {
  mountDock();
  const socket = connectedSocket();
  act(() => {
    openConversation(ada);
  });
  act(() => {
    socket.receive('{"type":"message","senderUserId":"u-zoe","text":"knock knock"}');
  });
  expect(unreadPeerIds().has('u-zoe')).toBe(true);

  fireEvent.click(await screen.findByRole('button', { name: 'Open' }));

  expect(unreadPeerIds().has('u-zoe')).toBe(false);
});

test("the message toast opens the sender's conversation, carrying the message across", async () => {
  mountDock();
  const socket = connectedSocket();
  act(() => {
    openConversation(ada);
  });
  act(() => {
    socket.receive('{"type":"message","senderUserId":"u-zoe","text":"knock knock"}');
  });

  fireEvent.click(await screen.findByRole('button', { name: 'Open' }));

  const zoeDialog = screen.getByRole('dialog', { name: 'Chat with Zoe' });
  expect(within(zoeDialog).getByText('knock knock')).toBeInTheDocument();
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
