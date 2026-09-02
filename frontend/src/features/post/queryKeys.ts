export function postsByAuthorKey(authorId: string) {
  return ['posts', 'by-author', authorId] as const;
}
