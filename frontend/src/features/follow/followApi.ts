import { type Account, api, type components, fetchAccounts, orAnonymous } from '@shared';
import { type FollowRelationship, toFollowRelationship } from './follow';

export async function fetchFollowRelationship(userId: string): Promise<FollowRelationship> {
  return toFollowRelationship(
    await orAnonymous<components['schemas']['FollowRelationship']>(
      await api.GET('/api/follows/{userId}', { params: { path: { userId } } }),
      { path: `/api/follows/${encodeURIComponent(userId)}`, label: 'Follow relationship request' },
    ),
  );
}

export async function followUser(userId: string): Promise<void> {
  const { response } = await api.PUT('/api/follows/{userId}', { params: { path: { userId } } });
  if (!response.ok) throw new Error(`Following failed: ${response.status}`);
}

export async function unfollowUser(userId: string): Promise<void> {
  const { response } = await api.DELETE('/api/follows/{userId}', { params: { path: { userId } } });
  if (!response.ok) throw new Error(`Unfollowing failed: ${response.status}`);
}

export type FollowListMode = 'followers' | 'following';

export type FollowRelationshipById = FollowRelationship & { userId: string };

export type AccountListPage = {
  accounts: Account[];
  relationships: FollowRelationshipById[];
  nextCursor: string | null;
};

export async function fetchFollowListPage(
  mode: FollowListMode,
  userId: string,
  cursor?: string,
): Promise<AccountListPage> {
  const { data, response } =
    mode === 'followers'
      ? await api.GET('/api/follows/{userId}/followers', {
          params: { path: { userId }, query: { cursor } },
        })
      : await api.GET('/api/follows/{userId}/following', {
          params: { path: { userId }, query: { cursor } },
        });
  if (!data) throw new Error(`Follow list request failed: ${response.status}`);

  const ids = data.items ?? [];
  const [accountsById, relationships] = await Promise.all([
    fetchAccounts(ids),
    fetchFollowRelationships(ids),
  ]);

  return {
    accounts: ids.flatMap((id) => {
      const account = accountsById.get(id);
      return account ? [account] : [];
    }),
    relationships,
    nextCursor: data.nextCursor ?? null,
  };
}

export async function fetchFollowRelationships(
  userIds: string[],
): Promise<FollowRelationshipById[]> {
  if (userIds.length === 0) return [];
  const { data, response } = await api.GET('/api/follows', {
    params: { query: { ids: userIds } },
  });
  if (!data) throw new Error(`Follow relationship batch request failed: ${response.status}`);
  return data.map((view) => ({ userId: view.userId ?? '', ...toFollowRelationship(view) }));
}
