import { api, fetchAccounts, throwIfSessionExpired } from '@shared';
import type { ChatPeer } from '../chatStore';

// The backend clamps the following page to 48 (FollowList.MAX_LIMIT); ask for the max so
// a long list is paged to completion in as few round-trips as possible.
const PAGE_LIMIT = 48;

export type FollowingPage = {
  peers: ChatPeer[];
  nextCursor: string | null;
  // How many follows this page covered, resolved or not — the rail's cap counts follows
  // considered, so an unresolvable account can't make it page the whole graph.
  followsSeen: number;
};

// The rail's own read of the monolith following list (ADR-0014: chat resolves display
// data client-side, and the follow graph lives in `social`, not chat). The `follow`
// feature's `fetchFollowListPage` also pulls follow *relationships* the rail has no use
// for — and a feature may not import another feature — so this asks for just the accounts.
export async function fetchFollowingPage(
  viewerId: string,
  cursor?: string,
): Promise<FollowingPage> {
  const { data, response } = await api.GET('/api/follows/{userId}/following', {
    params: { path: { userId: viewerId }, query: { cursor, limit: PAGE_LIMIT } },
  });
  throwIfSessionExpired(response);
  if (!data) throw new Error(`Following list request failed: ${response.status}`);

  const ids = data.items ?? [];
  const accounts = await fetchAccounts(ids);
  const peers = ids.flatMap((id) => {
    const account = accounts.get(id);
    return account
      ? [{ userId: account.userId, username: account.username, displayName: account.displayName }]
      : [];
  });

  return { peers, nextCursor: data.nextCursor ?? null, followsSeen: ids.length };
}
