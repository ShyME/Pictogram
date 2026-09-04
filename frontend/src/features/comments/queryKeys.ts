export function commentThreadKey(postId: string) {
  return ['comments', postId] as const;
}

// Nested under the thread key on purpose (pinned in useCommentCounts.test.ts): invalidating
// the thread after a post or a delete refreshes the count in the same pass.
export function commentCountKey(postId: string) {
  return ['comments', postId, 'count'] as const;
}
