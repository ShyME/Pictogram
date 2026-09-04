import { type QueryClient, useQuery } from '@tanstack/react-query';
import type { PostCommentCount, PostCommentCountById } from './comment';
import { fetchCommentCountsBatch } from './commentsApi';
import { commentCountKey } from './queryKeys';

const SEEDED_STALE_TIME_MS = 30_000;

const NO_COMMENTS: PostCommentCount = { commentCount: 0 };

export function usePostCommentCount(postId: string) {
  return useQuery({
    queryKey: commentCountKey(postId),
    queryFn: async (): Promise<PostCommentCount> => {
      const batch = await fetchCommentCountsBatch([postId]);
      const found = batch.find((record) => record.postId === postId);
      return found ? { commentCount: found.commentCount } : NO_COMMENTS;
    },
    staleTime: SEEDED_STALE_TIME_MS,
  });
}

export function seedCommentCounts(client: QueryClient, records: PostCommentCountById[]): void {
  for (const { postId, commentCount } of records) {
    client.setQueryData<PostCommentCount>(commentCountKey(postId), { commentCount });
  }
}

// The feed and the grid load a page then seed every post's comment count from one batch
// call, so the per-card counts read from cache instead of firing a request each (ADR-0005).
export async function prefetchCommentCounts(client: QueryClient, postIds: string[]): Promise<void> {
  if (postIds.length === 0) return;
  seedCommentCounts(client, await fetchCommentCountsBatch(postIds));
}
