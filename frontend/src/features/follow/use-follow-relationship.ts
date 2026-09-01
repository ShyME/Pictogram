import { type QueryClient, useQuery } from "@tanstack/react-query";
import { type FollowRelationshipById, fetchFollowRelationship } from "./follow-api";
import { followRelationshipKey } from "./query-keys";

/**
 * How long a seeded relationship stays fresh before a mounting {@link useFollowRelationship}
 * will refetch it. Pinned here rather than left to the app's default `staleTime` (#59): the
 * "list page fetches this once" guarantee that {@link seedFollowRelationships} exists for
 * would silently break if that unrelated default ever changed.
 */
const SEEDED_STALE_TIME_MS = 30_000;

/**
 * The one follow-relationship query. {@link FollowButton} and {@link FollowCounts} are
 * separate components but call this so they share a single cache entry: a follow mutation
 * invalidates the key and both re-render with the new counts and flag together.
 */
export function useFollowRelationship(userId: string) {
  return useQuery({
    queryKey: followRelationshipKey(userId),
    queryFn: () => fetchFollowRelationship(userId),
    staleTime: SEEDED_STALE_TIME_MS,
  });
}

/**
 * Primes {@link useFollowRelationship}'s cache from a batch read (#59) so the follow buttons
 * on a list page render from this instead of each firing `GET /api/follows/{id}`.
 */
export function seedFollowRelationships(
  client: QueryClient,
  records: FollowRelationshipById[],
): void {
  for (const { userId, ...relationship } of records) {
    client.setQueryData(followRelationshipKey(userId), relationship);
  }
}
