import { useFollowingPeers } from '@features/chat/rail/useFollowingPeers';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { jsonResponse, pathOf, problemResponse, stubFetch } from '@test-support/mockFetch';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, expect, test, vi } from 'vitest';

function wrapper() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

// A following list served in pages of `pageSize`, plus the profile batch read for each id.
function stubFollowing(ids: string[], pageSize: number) {
  return stubFetch((request) => {
    const url = new URL(request.url);
    if (pathOf(request) === '/api/follows/u-me/following') {
      const from = Number(url.searchParams.get('cursor') ?? '0');
      const slice = ids.slice(from, from + pageSize);
      const next = from + pageSize;
      return jsonResponse({
        items: slice,
        nextCursor: next < ids.length ? String(next) : null,
      });
    }
    if (pathOf(request) === '/api/profiles') {
      const asked = url.searchParams.getAll('ids');
      return jsonResponse(
        asked.map((id) => ({ userId: id, username: id.replace('u-', ''), displayName: null })),
      );
    }
    throw new Error(`unexpected ${pathOf(request)}`);
  });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

test('pages the following list to completion', async () => {
  stubFollowing(['u-a', 'u-b', 'u-c', 'u-d', 'u-e'], 2);
  const { result } = renderHook(() => useFollowingPeers('u-me'), { wrapper: wrapper() });

  await waitFor(() => {
    expect(result.current.status).toBe('ready');
  });
  expect(result.current.peers.map((p) => p.userId)).toEqual(['u-a', 'u-b', 'u-c', 'u-d', 'u-e']);
});

test('stops at the ~500 cap even when more pages remain', async () => {
  const ids = Array.from({ length: 640 }, (_, i) => `u-${i}`);
  stubFollowing(ids, 100);
  const { result } = renderHook(() => useFollowingPeers('u-me'), { wrapper: wrapper() });

  await waitFor(() => {
    expect(result.current.status).toBe('ready');
  });
  expect(result.current.peers).toHaveLength(500);
});

test('caps on follows considered, not rows resolved, if profiles do not resolve', async () => {
  const ids = Array.from({ length: 900 }, (_, i) => `u-${i}`);
  const calls = stubFetch((request) => {
    const url = new URL(request.url);
    if (pathOf(request) === '/api/follows/u-me/following') {
      const from = Number(url.searchParams.get('cursor') ?? '0');
      const next = from + 50;
      return jsonResponse({
        items: ids.slice(from, from + 50),
        nextCursor: next < ids.length ? String(next) : null,
      });
    }
    return jsonResponse([]); // the profile service resolves nothing
  });
  const { result } = renderHook(() => useFollowingPeers('u-me'), { wrapper: wrapper() });

  await waitFor(() => {
    expect(result.current.status).toBe('ready');
  });
  expect(result.current.peers).toEqual([]);
  const pagesFetched = calls.filter((c) => pathOf(c) === '/api/follows/u-me/following').length;
  expect(pagesFetched).toBeLessThanOrEqual(10); // ~500 / 50, not all 18
});

test('reports an error status when the list read fails', async () => {
  stubFetch((request) => {
    if (pathOf(request) === '/api/follows/u-me/following')
      return problemResponse('server-error', 500);
    return jsonResponse([]);
  });
  const { result } = renderHook(() => useFollowingPeers('u-me'), { wrapper: wrapper() });

  await waitFor(() => {
    expect(result.current.status).toBe('error');
  });
  expect(result.current.peers).toEqual([]);
});
