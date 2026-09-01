import { type QueryClient, useQuery } from "@tanstack/react-query";
import { type FollowRelationshipById, fetchFollowRelationship } from "./follow-api";
import { followRelationshipKey } from "./query-keys";

const SEEDED_STALE_TIME_MS = 30_000;

export function useFollowRelationship(userId: string) {
  return useQuery({
    queryKey: followRelationshipKey(userId),
    queryFn: () => fetchFollowRelationship(userId),
    staleTime: SEEDED_STALE_TIME_MS,
  });
}

export function seedFollowRelationships(
  client: QueryClient,
  records: FollowRelationshipById[],
): void {
  for (const { userId, ...relationship } of records) {
    client.setQueryData(followRelationshipKey(userId), relationship);
  }
}
