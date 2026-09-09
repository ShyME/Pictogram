import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';

type RailStore = typeof import('@features/chat/rail/railStore');

async function loadStore(): Promise<RailStore> {
  vi.resetModules();
  return import('@features/chat/rail/railStore');
}

beforeEach(() => {
  localStorage.clear();
});

afterEach(() => {
  localStorage.clear();
});

test('the rail is expanded by default', async () => {
  const { useIsRailCollapsed } = await loadStore();
  const { result } = renderHook(() => useIsRailCollapsed());

  expect(result.current).toBe(false);
});

test('toggling collapse flips the flag and persists it', async () => {
  const store = await loadStore();
  const { result } = renderHook(() => store.useIsRailCollapsed());

  act(() => {
    store.toggleRailCollapsed();
  });

  expect(result.current).toBe(true);
  expect(localStorage.getItem('pictogram.chat-rail.collapsed')).toBe('1');
});

test('a collapsed rail stays collapsed across a reload', async () => {
  const first = await loadStore();
  act(() => {
    first.toggleRailCollapsed();
  });

  // A fresh module load stands in for a page reload.
  const reloaded = await loadStore();
  const { result } = renderHook(() => reloaded.useIsRailCollapsed());

  expect(result.current).toBe(true);
});

test('the drawer starts closed and opens and closes', async () => {
  const store = await loadStore();
  const { result } = renderHook(() => store.useIsRailDrawerOpen());
  expect(result.current).toBe(false);

  act(() => {
    store.openRailDrawer();
  });
  expect(result.current).toBe(true);

  act(() => {
    store.closeRailDrawer();
  });
  expect(result.current).toBe(false);
});

test('the drawer open state is not persisted', async () => {
  const store = await loadStore();
  act(() => {
    store.openRailDrawer();
  });

  const reloaded = await loadStore();
  const { result } = renderHook(() => reloaded.useIsRailDrawerOpen());

  expect(result.current).toBe(false);
});
