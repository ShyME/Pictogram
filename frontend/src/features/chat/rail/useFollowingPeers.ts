import { useQuery } from '@tanstack/react-query';
import type { ChatPeer } from '../chatStore';
import { fetchFollowingPage } from './followingApi';

// Beyond this many follows the rail stops paging — the filter box is the way to reach a
// row past the cap (#201). A viewer following thousands of people is not the rail's case.
// The cap counts follows *considered*, not rows resolved, so a page of unresolvable
// accounts can't send the loop through the whole follow graph.
const RAIL_CAP = 500;

export const railFollowingKey = (viewerId: string) =>
  ['chat', 'rail', 'following', viewerId] as const;

export type FollowingPeers = {
  peers: ChatPeer[];
  status: 'loading' | 'error' | 'ready';
};

async function loadFollowing(viewerId: string): Promise<ChatPeer[]> {
  const peers: ChatPeer[] = [];
  let followsSeen = 0;
  let cursor: string | undefined;
  do {
    const page = await fetchFollowingPage(viewerId, cursor);
    peers.push(...page.peers);
    followsSeen += page.followsSeen;
    if (followsSeen >= RAIL_CAP) return peers.slice(0, RAIL_CAP);
    cursor = page.nextCursor ?? undefined;
  } while (cursor);
  return peers;
}

// Everyone the viewer follows, as chat peers, paged to completion (capped). One query for
// the whole rail; presence and unread are layered on by the caller.
export function useFollowingPeers(viewerId: string): FollowingPeers {
  const query = useQuery({
    queryKey: railFollowingKey(viewerId),
    queryFn: () => loadFollowing(viewerId),
    staleTime: 60_000,
  });

  return {
    peers: query.data ?? [],
    status: query.isPending ? 'loading' : query.isError ? 'error' : 'ready',
  };
}
