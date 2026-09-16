import { ChatOverlay } from '@features/chat/ChatOverlay';
import { chatConnectionStatus } from '@features/chat/chatConnection';
import { closeConversation, openConversation } from '@features/chat/chatStore';
import { FakeWebSocket, stubWebSocket } from '@test-support/stubWebSocket';
import { act, fireEvent, render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { BehaviorSubject } from 'rxjs';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';

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

function noop(): void {
  // the overlay never re-queries `matches` mid-test, so no listener need actually fire
}

// A matchMedia stand-in: `min-width` queries track `isWide`. The default (no call) leaves
// every query unmatched, i.e. the narrow, full-screen overlay layout.
function stubViewport(isWide: boolean) {
  vi.stubGlobal('matchMedia', (query: string) => ({
    matches: isWide && query.includes('min-width'),
    media: query,
    onchange: null,
    addListener: noop,
    removeListener: noop,
    addEventListener: noop,
    removeEventListener: noop,
    dispatchEvent: () => false,
  }));
}

function renderOverlay() {
  return render(
    <MemoryRouter>
      <ChatOverlay />
    </MemoryRouter>,
  );
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
  vi.unstubAllGlobals();
});

test('renders nothing until a conversation is opened', () => {
  const { container } = renderOverlay();
  expect(container).toBeEmptyDOMElement();

  act(() => {
    openConversation(ada);
  });

  expect(screen.getByRole('dialog', { name: 'Chat with Ada Lovelace' })).toBeInTheDocument();
  expect(screen.getByText('@ada')).toBeInTheDocument();
});

test('falls back to the handle when the peer has no display name', () => {
  renderOverlay();
  act(() => {
    openConversation(bob);
  });

  expect(screen.getByRole('dialog', { name: 'Chat with @bob' })).toBeInTheDocument();
});

test('composing and sending echoes the message and writes the protocol frame', () => {
  const socket = withOpenSocket();
  renderOverlay();
  act(() => {
    openConversation(ada);
  });

  typeAndSend('hello Ada');

  expect(screen.getByText('hello Ada')).toBeInTheDocument();
  expect(socket.sent).toContain('{"recipientUserId":"u-ada","text":"hello Ada"}');
  expect(screen.queryByText('Not delivered')).not.toBeInTheDocument();
});

test('a message that cannot be sent is shown as not delivered', () => {
  renderOverlay();
  act(() => {
    openConversation(ada);
  });

  typeAndSend('anyone home?');

  const dialog = screen.getByRole('dialog');
  expect(within(dialog).getByText('anyone home?')).toBeInTheDocument();
  expect(within(dialog).getByText('Not delivered')).toBeInTheDocument();
});

test('opening the overlay asks chat for the peer presence and shows the dot on the answer', () => {
  const socket = withOpenSocket();
  renderOverlay();
  act(() => {
    openConversation(ada);
  });

  expect(socket.sent).toContain('{"type":"presence-query","userIds":["u-ada"]}');

  act(() => {
    socket.receive('{"type":"presence","userId":"u-ada","online":true}');
  });

  const dialog = screen.getByRole('dialog');
  expect(within(dialog).getByRole('img', { name: 'Online' })).toBeInTheDocument();
});

test('the close control discards the overlay', () => {
  renderOverlay();
  act(() => {
    openConversation(ada);
  });

  fireEvent.click(screen.getByRole('button', { name: 'Close chat' }));

  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});

test('opening a second conversation replaces the first, with no carried-over messages', () => {
  renderOverlay();
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

test('opening with a seed message shows it, and replaces a stale same-peer conversation', () => {
  renderOverlay();
  act(() => {
    openConversation(ada);
  });
  typeAndSend('note to Ada');
  expect(screen.getByText('note to Ada')).toBeInTheDocument();

  act(() => {
    openConversation(ada, 'did you see this?');
  });

  const dialog = screen.getByRole('dialog', { name: 'Chat with Ada Lovelace' });
  expect(within(dialog).getByText('did you see this?')).toBeInTheDocument();
  expect(within(dialog).queryByText('note to Ada')).not.toBeInTheDocument();
});

test('an unsent draft does not carry across to the next conversation', () => {
  renderOverlay();
  act(() => {
    openConversation(ada);
  });
  fireEvent.change(screen.getByRole('textbox'), { target: { value: 'half-written' } });

  act(() => {
    openConversation(bob);
  });

  expect(screen.getByRole('textbox')).toHaveValue('');
});

test('the header name links to the peer profile', () => {
  renderOverlay();
  act(() => {
    openConversation(ada);
  });

  expect(screen.getByRole('link', { name: 'Ada Lovelace' })).toHaveAttribute('href', '/u/ada');
});

test('on a narrow viewport, following the profile link closes the full-screen overlay', () => {
  stubViewport(false);
  renderOverlay();
  act(() => {
    openConversation(ada);
  });

  fireEvent.click(screen.getByRole('link', { name: 'Ada Lovelace' }));

  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});

test('on a desktop viewport, following the profile link leaves the floating overlay open', () => {
  stubViewport(true);
  renderOverlay();
  act(() => {
    openConversation(ada);
  });

  fireEvent.click(screen.getByRole('link', { name: 'Ada Lovelace' }));

  expect(screen.getByRole('dialog')).toBeInTheDocument();
});
