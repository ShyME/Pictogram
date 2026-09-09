import { useSyncExternalStore } from 'react';

// Two independent pieces of rail UI state (#203):
//
//  - collapsed — the docked desktop rail's expand/collapse choice, remembered in
//    `localStorage` so it survives a reload.
//  - drawer-open — whether the narrow-viewport drawer is showing. Ephemeral: opening it
//    is a per-visit action, triggered from the `AppNav` chat icon.

const COLLAPSED_KEY = 'pictogram.chat-rail.collapsed';

function isCollapsedStored(): boolean {
  try {
    return localStorage.getItem(COLLAPSED_KEY) === '1';
  } catch {
    // Private-mode / disabled storage: fall back to the expanded default.
    return false;
  }
}

function storeCollapsed(isCollapsedNow: boolean): void {
  try {
    localStorage.setItem(COLLAPSED_KEY, isCollapsedNow ? '1' : '0');
  } catch {
    // Nothing to persist to — the in-memory value below still drives this session.
  }
}

let isCollapsed = isCollapsedStored();
let isDrawerOpen = false;
const listeners = new Set<() => void>();

function notify(): void {
  for (const listener of listeners) listener();
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export function toggleRailCollapsed(): void {
  isCollapsed = !isCollapsed;
  storeCollapsed(isCollapsed);
  notify();
}

export function useIsRailCollapsed(): boolean {
  return useSyncExternalStore(
    subscribe,
    () => isCollapsed,
    () => isCollapsed,
  );
}

export function openRailDrawer(): void {
  if (isDrawerOpen) return;
  isDrawerOpen = true;
  notify();
}

export function closeRailDrawer(): void {
  if (!isDrawerOpen) return;
  isDrawerOpen = false;
  notify();
}

export function useIsRailDrawerOpen(): boolean {
  return useSyncExternalStore(
    subscribe,
    () => isDrawerOpen,
    () => false,
  );
}
