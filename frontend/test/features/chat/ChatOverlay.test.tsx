import { ChatOverlay } from '@features/chat/ChatOverlay';
import { chatConnectionStatus } from '@features/chat/chatConnection';
import { closeConversation, openConversation } from '@features/chat/chatStore';
import { FakeWebSocket, stubWebSocket } from '@test-support/stubWebSocket';
import { act, fireEvent, render, screen, within } from '@testing-library/react';
import { BehaviorSubject } from 'rxjs';
import { afterEach, beforeEach, expect, test } from 'vitest';

const ada = { userId: 'u-ada', username: 'ada', displayName: 'Ada Lovelace' };
const bob = { userId: 'u-bob', username: 'bob', displayName: null };

let connection: { unsubscribe: () => void } | undefined;

function withOpenSocket(): FakeWebSocket {
  const tokens = new BehaviorSubject<string | null>('a-token');
  connection = chatConnectionStatus(tokens).subscribe();
  const socket = FakeWebSocket.instances[0];
  socket.open();
  return socket;
}

function typeAndSend(text: string) {
  fireEvent.change(screen.getByRole('textbox'), { target: { value: text } });
  fireEvent.click(screen.getByRole('button', { name: 'Send' }));
}

beforeEach(() => {
  stubWebSocket();
});

afterEach(() => {
  connection?.unsubscribe();
  connection = undefined;
  act(() => {
    closeConversation();
  });
});

test('renders nothing until a conversation is opened', () => {
  const { container } = render(<ChatOverlay />);
  expect(container).toBeEmptyDOMElement();

  act(() => {
    openConversation(ada);
  });

  expect(screen.getByRole('dialog', { name: 'Chat with Ada Lovelace' })).toBeInTheDocument();
  expect(screen.getByText('@ada')).toBeInTheDocument();
});

test('falls back to the handle when the peer has no display name', () => {
  render(<ChatOverlay />);
  act(() => {
    openConversation(bob);
  });

  expect(screen.getByRole('dialog', { name: 'Chat with @bob' })).toBeInTheDocument();
});

test('composing and sending echoes the message and writes the protocol frame', () => {
  const socket = withOpenSocket();
  render(<ChatOverlay />);
  act(() => {
    openConversation(ada);
  });

  typeAndSend('hello Ada');

  expect(screen.getByText('hello Ada')).toBeInTheDocument();
  expect(socket.sent).toEqual(['{"recipientUserId":"u-ada","text":"hello Ada"}']);
  expect(screen.queryByText('Not delivered')).not.toBeInTheDocument();
});

test('a message that cannot be sent is shown as not delivered', () => {
  render(<ChatOverlay />);
  act(() => {
    openConversation(ada);
  });

  typeAndSend('anyone home?');

  const dialog = screen.getByRole('dialog');
  expect(within(dialog).getByText('anyone home?')).toBeInTheDocument();
  expect(within(dialog).getByText('Not delivered')).toBeInTheDocument();
});

test('the close control discards the overlay', () => {
  render(<ChatOverlay />);
  act(() => {
    openConversation(ada);
  });

  fireEvent.click(screen.getByRole('button', { name: 'Close chat' }));

  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});

test('opening a second conversation replaces the first, with no carried-over messages', () => {
  render(<ChatOverlay />);
  act(() => {
    openConversation(ada);
  });
  typeAndSend('note to Ada');

  act(() => {
    openConversation(bob);
  });

  expect(screen.getByRole('dialog', { name: 'Chat with @bob' })).toBeInTheDocument();
  expect(screen.queryByText('note to Ada')).not.toBeInTheDocument();
});

test('an unsent draft does not carry across to the next conversation', () => {
  render(<ChatOverlay />);
  act(() => {
    openConversation(ada);
  });
  fireEvent.change(screen.getByRole('textbox'), { target: { value: 'half-written' } });

  act(() => {
    openConversation(bob);
  });

  expect(screen.getByRole('textbox')).toHaveValue('');
});
