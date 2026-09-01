import { api } from "@shared";
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
