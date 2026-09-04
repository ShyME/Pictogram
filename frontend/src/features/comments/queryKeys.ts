export function commentThreadKey(postId: string) {
  return ['comments', postId] as const;
}
