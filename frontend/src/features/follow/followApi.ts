import { api, type components } from '@shared';
import { type Account, toAccount } from './account';
import { type FollowRelationship, toFollowRelationship } from './follow';

export async function fetchFollowRelationship(userId: string): Promise<FollowRelationship> {
  const { data, response } = await api.GET('/api/follows/{userId}', {
    params: { path: { userId } },
  });
  if (data) return toFollowRelationship(data);
  if (response.status === 401) return anonymousFollowRelationship(userId);
  throw new Error(`Unexpected /api/follows/${userId} response: ${response.status}`);
}

async function anonymousFollowRelationship(userId: string): Promise<FollowRelationship> {
  const response = await fetch(`/api/follows/${encodeURIComponent(userId)}`, {
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) {
    throw new Error(`Unexpected /api/follows/${userId} response: ${response.status}`);
  }
  return toFollowRelationship(
    (await response.json()) as components['schemas']['FollowRelationship'],
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
  const [accounts, relationships] = await Promise.all([
    fetchAccounts(ids),
    fetchFollowRelationships(ids),
  ]);
  const byId = new Map(accounts.map((account) => [account.userId, account]));

  return {
    accounts: ids.flatMap((id) => {
      const account = byId.get(id);
      return account ? [account] : [];
    }),
    relationships,
    nextCursor: data.nextCursor ?? null,
  };
}

async function fetchAccounts(userIds: string[]): Promise<Account[]> {
  if (userIds.length === 0) return [];
  const { data, response } = await api.GET('/api/profiles', {
    params: { query: { ids: userIds } },
  });
  if (!data) throw new Error(`Profile batch request failed: ${response.status}`);
  return data.map((view) => toAccount(view));
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
