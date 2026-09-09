import { railFollowingKey } from '@features/chat/rail/useFollowingPeers';
import { FollowButton } from '@features/follow/FollowButton';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { jsonResponse, stubFetch } from '@test-support/mockFetch';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, expect, test, vi } from 'vitest';

// The chat rail (#203) reads the viewer's follow list from its own query; a follow write
// in the `follow` feature must invalidate it so the rail doesn't show a stale membership
// until a reload.

afterEach(() => {
  vi.unstubAllGlobals();
});

test('a successful follow invalidates the chat rail following query', async () => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  queryClient.setQueryData(railFollowingKey('u-me'), []);

  const calls: string[] = [];
  stubFetch((request) => {
    calls.push(request.method);
    if (request.method === 'PUT') return new Response(null, { status: 204 });
    return jsonResponse({
      followerCount: calls.includes('PUT') ? 1 : 0,
      followingCount: 0,
      followedByViewer: calls.includes('PUT'),
    });
  });

  render(
    <QueryClientProvider client={queryClient}>
      <FollowButton userId="u-1" />
    </QueryClientProvider>,
  );

  const button = await screen.findByRole('button', { name: 'Follow' });
  await waitFor(() => {
    expect(button).toBeEnabled();
  });
  fireEvent.click(button);

  await waitFor(() => {
    expect(queryClient.getQueryState(railFollowingKey('u-me'))?.isInvalidated).toBe(true);
  });
});
