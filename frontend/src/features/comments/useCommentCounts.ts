import { createBatchAside } from '@shared';
import type { QueryClient } from '@tanstack/react-query';
import type { PostCommentCount, PostCommentCountById } from './comment';
import { fetchCommentCountsBatch } from './commentsApi';
import { commentCountKey } from './queryKeys';

const NO_COMMENTS: PostCommentCount = { commentCount: 0 };

const cache = createBatchAside<PostCommentCount>({
  key: commentCountKey,
  readOne: async (postId) => {
    const batch = await fetchCommentCountsBatch([postId]);
    const found = batch.find((record) => record.postId === postId);
    return found ? { commentCount: found.commentCount } : NO_COMMENTS;
  },
  readBatch: async (postIds) => {
    const batch = await fetchCommentCountsBatch(postIds);
    return batch.map(({ postId, commentCount }) => [postId, { commentCount }]);
  },
});

export function usePostCommentCount(postId: string) {
  return cache.useValue(postId);
}

export function seedCommentCounts(client: QueryClient, records: PostCommentCountById[]): void {
  cache.seed(
    client,
    records.map(({ postId, commentCount }) => [postId, { commentCount }]),
  );
}

export const prefetchCommentCounts = cache.prime;
