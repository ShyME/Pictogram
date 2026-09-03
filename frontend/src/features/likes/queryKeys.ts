export function postLikesKey(postId: string) {
  return ['likes', postId] as const;
}
