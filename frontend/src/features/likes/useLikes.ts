import { type QueryClient, useQuery } from '@tanstack/react-query';
import type { PostLikes, PostLikesById } from './likes';
import { fetchPostLikesBatch } from './likesApi';
import { postLikesKey } from './queryKeys';

const SEEDED_STALE_TIME_MS = 30_000;

const NO_LIKES: PostLikes = { likeCount: 0, likedByViewer: false };

export function usePostLikes(postId: string) {
  return useQuery({
    queryKey: postLikesKey(postId),
    queryFn: async (): Promise<PostLikes> => {
      const batch = await fetchPostLikesBatch([postId]);
      const likes = batch.find((record) => record.postId === postId);
      return likes ? { likeCount: likes.likeCount, likedByViewer: likes.likedByViewer } : NO_LIKES;
    },
    staleTime: SEEDED_STALE_TIME_MS,
  });
}

export function seedPostLikes(client: QueryClient, records: PostLikesById[]): void {
  for (const { postId, likeCount, likedByViewer } of records) {
    client.setQueryData<PostLikes>(postLikesKey(postId), { likeCount, likedByViewer });
  }
}

// The feed loads a page of cards then seeds every post's like state from one batch call,
// so the per-card LikeButtons read from cache instead of firing a request each (ADR-0005).
export async function prefetchPostLikes(client: QueryClient, postIds: string[]): Promise<void> {
  if (postIds.length === 0) return;
  seedPostLikes(client, await fetchPostLikesBatch(postIds));
}
