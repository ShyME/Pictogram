import { useQuery } from "@tanstack/react-query";
import { fetchFollowRelationship } from "../api/follow-api";
import { followRelationshipKey } from "./query-keys";

/**
 * The one follow-relationship query. {@link FollowButton} and {@link FollowCounts} are
 * separate components but call this so they share a single cache entry: a follow mutation
 * invalidates the key and both re-render with the new counts and flag together.
 */
export function useFollowRelationship(userId: string) {
  return useQuery({
    queryKey: followRelationshipKey(userId),
    queryFn: () => fetchFollowRelationship(userId),
  });
}
