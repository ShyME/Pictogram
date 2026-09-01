/** The one query key for an author's grid, so the composer can invalidate it after publishing. */
export function postsByAuthorKey(authorId: string) {
  return ["posts", "by-author", authorId] as const;
}
