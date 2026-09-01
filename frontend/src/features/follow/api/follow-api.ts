import { api } from "@shared";
import { type Account, toAccount } from "../model/account";
import { type FollowRelationship, toFollowRelationship } from "../model/follow";

/**
 * Reads a user's follower / following counts and whether the caller follows them — the data
 * behind a profile page's follow button and count row. Public: the counts show on a
 * shareable profile even when signed out, in which case `followedByViewer` is `false`.
 *
 * A stale in-memory access token that can't be refreshed makes the resource server reject
 * even this public call with a 401 (the bearer filter runs before the permit rule). Since
 * the counts are public, we retry once with no credentials rather than fail — the same
 * fallback the public profile lookup uses.
 */
export async function fetchFollowRelationship(userId: string): Promise<FollowRelationship> {
  const { data, response } = await api.GET("/api/follows/{userId}", {
    params: { path: { userId } },
  });
  if (data) return toFollowRelationship(data);
  if (response.status === 401) return anonymousFollowRelationship(userId);
  throw new Error(`Unexpected /api/follows/${userId} response: ${response.status}`);
}

async function anonymousFollowRelationship(userId: string): Promise<FollowRelationship> {
  const response = await fetch(`/api/follows/${encodeURIComponent(userId)}`, {
    headers: { Accept: "application/json" },
  });
  if (!response.ok) {
    throw new Error(`Unexpected /api/follows/${userId} response: ${response.status}`);
  }
  return toFollowRelationship(await response.json());
}

/**
 * Follows a user. Idempotent server-side (204 whether or not the edge was already there),
 * so the UI treats it as "now following". A 401 means the auth middleware's silent refresh
 * already failed — it throws like the other write flows.
 */
export async function followUser(userId: string): Promise<void> {
  const { response } = await api.PUT("/api/follows/{userId}", { params: { path: { userId } } });
  if (!response.ok) throw new Error(`Following failed: ${response.status}`);
}

/** Unfollows a user. Idempotent server-side, so the UI treats it as "not following". */
export async function unfollowUser(userId: string): Promise<void> {
  const { response } = await api.DELETE("/api/follows/{userId}", { params: { path: { userId } } });
  if (!response.ok) throw new Error(`Unfollowing failed: ${response.status}`);
}

export type FollowListMode = "followers" | "following";

export type FollowRelationshipById = FollowRelationship & { userId: string };

export type AccountListPage = {
  accounts: Account[];
  relationships: FollowRelationshipById[];
  nextCursor: string | null;
};

/**
 * One page of a user's follower or following list, resolved to renderable rows (#57). The
 * follow list (a page of ids + a cursor), then two batches on that id set, per ADR-0005:
 * `GET /api/profiles?ids=` for handles and names, and `GET /api/follows?ids=` for each
 * user's follow standing — the latter seeded into the per-user cache (#59) so the reused
 * follow buttons don't each fetch `GET /api/follows/{id}`. Rows keep the follow-list order —
 * newest relationship first — and any id the profile batch doesn't return (a deleted
 * profile) is dropped. All three endpoints are authenticated.
 */
export async function fetchFollowListPage(
  mode: FollowListMode,
  userId: string,
  cursor?: string,
): Promise<AccountListPage> {
  const { data, response } =
    mode === "followers"
      ? await api.GET("/api/follows/{userId}/followers", {
          params: { path: { userId }, query: { cursor } },
        })
      : await api.GET("/api/follows/{userId}/following", {
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
  const { data, response } = await api.GET("/api/profiles", {
    params: { query: { ids: userIds } },
  });
  if (!data) throw new Error(`Profile batch request failed: ${response.status}`);
  return data.map(toAccount);
}

/**
 * The viewer's follow standing with each of `userIds` in one call (#59). Each record is the
 * same shape `fetchFollowRelationship` returns, so a caller can seed `followRelationshipKey`
 * with it. Authenticated — every caller (the list screens) already holds a session.
 */
export async function fetchFollowRelationships(
  userIds: string[],
): Promise<FollowRelationshipById[]> {
  if (userIds.length === 0) return [];
  const { data, response } = await api.GET("/api/follows", {
    params: { query: { ids: userIds } },
  });
  if (!data) throw new Error(`Follow relationship batch request failed: ${response.status}`);
  return data.map((view) => ({ userId: view.userId ?? "", ...toFollowRelationship(view) }));
}
