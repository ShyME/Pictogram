import { ChatRail, ChatRailTrigger } from '@features/chat';
import { chatConnectionStatus } from '@features/chat/chatConnection';
import { closeConversation, openConversationPeer } from '@features/chat/chatStore';
import { closeRailDrawer } from '@features/chat/rail/railStore';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { jsonResponse, pathOf, problemResponse, stubFetch } from '@test-support/mockFetch';
import { FakeWebSocket, stubWebSocket } from '@test-support/stubWebSocket';
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import type { ReactNode } from 'react';
import { BehaviorSubject } from 'rxjs';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';

const PEOPLE: Record<string, { username: string; displayName: string | null }> = {
  'u-vivian': { username: 'vivian', displayName: 'Vivian Maier' },
  'u-dorothea': { username: 'dorothea', displayName: 'Dorothea Lange' },
  'u-saul': { username: 'saul', displayName: 'Saul Leiter' },
};

let connection: { unsubscribe: () => void } | undefined;

// A stateful matchMedia: `min-width` queries answer `isWide`; listeners fire on change.
function installViewport(isWide: boolean) {
  const listeners = new Set<() => void>();
  vi.stubGlobal('matchMedia', (query: string) => ({
    get matches() {
      return isWide && query.includes('min-width');
    },
    media: query,
    onchange: null,
    addListener: (l: () => void) => listeners.add(l),
    removeListener: (l: () => void) => listeners.delete(l),
    addEventListener: (_: string, l: () => void) => listeners.add(l),
    removeEventListener: (_: string, l: () => void) => listeners.delete(l),
    dispatchEvent: () => false,
  }));
}

function stubFollowing(ids: string[]) {
  return stubFetch((request) => {
    const url = new URL(request.url);
    if (pathOf(request) === '/api/follows/u-me/following') {
      return jsonResponse({ items: ids, nextCursor: null });
    }
    if (pathOf(request) === '/api/profiles') {
      const asked = url.searchParams.getAll('ids');
      return jsonResponse(asked.map((id) => ({ userId: id, ...PEOPLE[id] })));
    }
    return problemResponse('not-found', 404);
  });
}

function openSocket(): FakeWebSocket {
  const tokens = new BehaviorSubject<string | null>('a-token');
  connection = chatConnectionStatus(tokens).subscribe();
  const socket = FakeWebSocket.instances[0];
  act(() => {
    socket.open();
  });
  return socket;
}

function renderRail(ui: ReactNode = <ChatRail viewerId="u-me" />) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

function railPanel(): Promise<HTMLElement> {
  return screen.findByRole('complementary', { name: 'Messages' });
}

const filterBox = () => screen.getByRole('searchbox', { name: /filter/i });

beforeEach(() => {
  stubWebSocket();
  installViewport(true);
  localStorage.clear();
});

afterEach(() => {
  connection?.unsubscribe();
  connection = undefined;
  act(() => {
    closeConversation();
    closeRailDrawer();
  });
  vi.unstubAllGlobals();
  localStorage.clear();
});

test('lists followed people, online first, alphabetical within each group', async () => {
  stubFollowing(['u-saul', 'u-vivian', 'u-dorothea']);
  const socket = openSocket();
  renderRail();

  const panel = await railPanel();
  await within(panel).findByText('Vivian Maier');

  act(() => {
    socket.receive('{"type":"presence","userId":"u-vivian","online":true}');
    socket.receive('{"type":"presence","userId":"u-saul","online":false}');
    socket.receive('{"type":"presence","userId":"u-dorothea","online":false}');
  });

  await waitFor(() => {
    const names = within(panel)
      .getAllByRole('listitem')
      .map((li) => within(li).getByText(/Maier|Lange|Leiter/).textContent);
    expect(names).toEqual(['Vivian Maier', 'Dorothea Lange', 'Saul Leiter']);
  });
});

test('the filter box narrows the visible rows', async () => {
  stubFollowing(['u-vivian', 'u-dorothea', 'u-saul']);
  openSocket();
  renderRail();
  await screen.findByText('Vivian Maier');

  fireEvent.change(filterBox(), { target: { value: 'lang' } });

  expect(screen.getByText('Dorothea Lange')).toBeInTheDocument();
  expect(screen.queryByText('Vivian Maier')).not.toBeInTheDocument();
  expect(screen.queryByText('Saul Leiter')).not.toBeInTheDocument();
});

test('the filter also matches the @username', async () => {
  stubFollowing(['u-vivian', 'u-dorothea']);
  openSocket();
  renderRail();
  await screen.findByText('Vivian Maier');

  fireEvent.change(filterBox(), { target: { value: 'vivian' } });

  expect(screen.getByText('Vivian Maier')).toBeInTheDocument();
  expect(screen.queryByText('Dorothea Lange')).not.toBeInTheDocument();
});

test('clicking a row opens the conversation for that peer', async () => {
  stubFollowing(['u-vivian', 'u-dorothea']);
  openSocket();
  renderRail();

  fireEvent.click(await screen.findByRole('button', { name: /Vivian Maier/ }));

  expect(openConversationPeer()).toEqual({
    userId: 'u-vivian',
    username: 'vivian',
    displayName: 'Vivian Maier',
  });
});

test('renders an empty state when the viewer follows nobody', async () => {
  stubFollowing([]);
  openSocket();
  renderRail();

  expect(await screen.findByText(/no one to message yet/i)).toBeInTheDocument();
  expect(screen.queryByRole('searchbox')).not.toBeInTheDocument();
});

test('shows a loading state before the list resolves', async () => {
  stubFollowing(['u-vivian']);
  openSocket();
  renderRail();

  // The first render is before react-query has settled the following query.
  expect(screen.getByRole('status', { name: /loading conversations/i })).toBeInTheDocument();
  expect(await screen.findByText('Vivian Maier')).toBeInTheDocument();
});

test('with the socket down the list still renders, without presence dots', async () => {
  stubFollowing(['u-vivian', 'u-dorothea']);
  renderRail();

  await screen.findByText('Vivian Maier');
  expect(screen.queryByRole('img', { name: /online|offline/i })).not.toBeInTheDocument();
});

test('the collapse toggle hides the list, writes localStorage, and expands back', async () => {
  stubFollowing(['u-vivian']);
  openSocket();
  renderRail();
  await screen.findByText('Vivian Maier');

  fireEvent.click(screen.getByRole('button', { name: /collapse messages/i }));
  expect(screen.queryByText('Vivian Maier')).not.toBeInTheDocument();
  expect(screen.queryByRole('searchbox')).not.toBeInTheDocument();
  expect(localStorage.getItem('pictogram.chat-rail.collapsed')).toBe('1');

  fireEvent.click(screen.getByRole('button', { name: /expand messages/i }));
  expect(await screen.findByText('Vivian Maier')).toBeInTheDocument();
  expect(localStorage.getItem('pictogram.chat-rail.collapsed')).toBe('0');
});

test('on a narrow viewport the rail is a drawer opened from the trigger', async () => {
  installViewport(false);
  stubFollowing(['u-vivian']);
  openSocket();
  renderRail(
    <>
      <ChatRailTrigger />
      <ChatRail viewerId="u-me" />
    </>,
  );

  expect(screen.queryByRole('complementary', { name: 'Messages' })).not.toBeInTheDocument();

  fireEvent.click(screen.getByRole('button', { name: /open messages/i }));
  const drawer = await railPanel();
  await within(drawer).findByText('Vivian Maier');

  fireEvent.click(within(drawer).getByRole('button', { name: /close messages/i }));
  await waitFor(() => {
    expect(screen.queryByRole('complementary', { name: 'Messages' })).not.toBeInTheDocument();
  });
});

test('the open drawer locks body scroll and restores it on close', async () => {
  installViewport(false);
  stubFollowing(['u-vivian']);
  openSocket();
  renderRail(
    <>
      <ChatRailTrigger />
      <ChatRail viewerId="u-me" />
    </>,
  );
  expect(document.body.style.overflow).toBe('');

  fireEvent.click(screen.getByRole('button', { name: /open messages/i }));
  await railPanel();
  expect(document.body.style.overflow).toBe('hidden');

  fireEvent.keyDown(document, { key: 'Escape' });
  await waitFor(() => {
    expect(screen.queryByRole('complementary', { name: 'Messages' })).not.toBeInTheDocument();
  });
  expect(document.body.style.overflow).toBe('');
});

test('opening a conversation from the drawer closes it', async () => {
  installViewport(false);
  stubFollowing(['u-vivian']);
  openSocket();
  renderRail(
    <>
      <ChatRailTrigger />
      <ChatRail viewerId="u-me" />
    </>,
  );

  fireEvent.click(screen.getByRole('button', { name: /open messages/i }));
  const drawer = await railPanel();
  fireEvent.click(await within(drawer).findByRole('button', { name: /Vivian Maier/ }));

  expect(openConversationPeer()?.userId).toBe('u-vivian');
  await waitFor(() => {
    expect(screen.queryByRole('complementary', { name: 'Messages' })).not.toBeInTheDocument();
  });
});
