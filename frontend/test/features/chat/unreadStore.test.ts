import { act, renderHook } from '@testing-library/react';
import { afterEach, expect, test, vi } from 'vitest';

type UnreadStore = typeof import('@features/chat/unreadStore');

async function loadStore(): Promise<UnreadStore> {
  vi.resetModules();
  return import('@features/chat/unreadStore');
}

afterEach(() => {
  vi.resetModules();
});

test('no peers are unread to start with', async () => {
  const { useUnreadPeerIds, useIsAnyPeerUnread } = await loadStore();

  expect(renderHook(() => useUnreadPeerIds()).result.current.size).toBe(0);
  expect(renderHook(() => useIsAnyPeerUnread()).result.current).toBe(false);
});

test('marking a peer adds it to the set and flips the any-unread flag', async () => {
  const store = await loadStore();
  const set = renderHook(() => store.useUnreadPeerIds());
  const any = renderHook(() => store.useIsAnyPeerUnread());

  act(() => {
    store.markPeerUnread('u-ada');
  });

  expect([...set.result.current]).toEqual(['u-ada']);
  expect(any.result.current).toBe(true);
});

test('clearing a peer removes it; the flag drops once the set is empty', async () => {
  const store = await loadStore();
  const set = renderHook(() => store.useUnreadPeerIds());
  const any = renderHook(() => store.useIsAnyPeerUnread());

  act(() => {
    store.markPeerUnread('u-ada');
    store.markPeerUnread('u-bo');
    store.clearPeerUnread('u-ada');
  });
  expect([...set.result.current]).toEqual(['u-bo']);
  expect(any.result.current).toBe(true);

  act(() => {
    store.clearPeerUnread('u-bo');
  });
  expect(set.result.current.size).toBe(0);
  expect(any.result.current).toBe(false);
});

test('marking the same peer twice does not notify a second time', async () => {
  const store = await loadStore();
  let renders = 0;
  renderHook(() => {
    renders += 1;
    return store.useUnreadPeerIds();
  });
  const rendersAfterMount = renders;

  act(() => {
    store.markPeerUnread('u-ada');
  });
  const rendersAfterFirstMark = renders;

  act(() => {
    store.markPeerUnread('u-ada');
  });

  expect(rendersAfterFirstMark).toBe(rendersAfterMount + 1);
  expect(renders).toBe(rendersAfterFirstMark);
});

test('clearing an unmarked peer is a no-op', async () => {
  const store = await loadStore();
  const any = renderHook(() => store.useIsAnyPeerUnread());

  act(() => {
    store.clearPeerUnread('u-nobody');
  });

  expect(any.result.current).toBe(false);
});

test('unread markers do not survive a reload', async () => {
  const first = await loadStore();
  act(() => {
    first.markPeerUnread('u-ada');
  });

  // A fresh module load stands in for a page reload — nothing is persisted.
  const reloaded = await loadStore();

  expect(renderHook(() => reloaded.useUnreadPeerIds()).result.current.size).toBe(0);
});
