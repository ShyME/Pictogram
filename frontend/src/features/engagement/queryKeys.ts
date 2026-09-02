export function postLikesKey(postId: string) {
  return ['engagement', 'likes', postId] as const;
}
