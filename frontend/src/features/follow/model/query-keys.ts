/**
 * The one query key for a user's follow standing, so the follow button and the count row
 * (separate components, one request) share a cache entry and a mutation can invalidate it.
 */
export function followRelationshipKey(userId: string) {
  return ["follow", "relationship", userId] as const;
}
