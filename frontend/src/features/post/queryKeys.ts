export function postsByAuthorKey(authorId: string) {
  return ['posts', 'by-author', authorId] as const;
}

export function postDetailKey(postId: string) {
  return ['posts', 'detail', postId] as const;
}
