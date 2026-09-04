import { createBatchAside } from '@shared';
import type { QueryClient } from '@tanstack/react-query';
import type { PostLikes, PostLikesById } from './likes';
import { fetchPostLikesBatch } from './likesApi';
import { postLikesKey } from './queryKeys';

const NO_LIKES: PostLikes = { likeCount: 0, likedByViewer: false };

const cache = createBatchAside<PostLikes>({
  key: postLikesKey,
  readOne: async (postId) => {
    const batch = await fetchPostLikesBatch([postId]);
    const found = batch.find((record) => record.postId === postId);
    return found ? { likeCount: found.likeCount, likedByViewer: found.likedByViewer } : NO_LIKES;
  },
  readBatch: async (postIds) => {
    const batch = await fetchPostLikesBatch(postIds);
    return batch.map(({ postId, ...likes }) => [postId, likes]);
  },
});

export function usePostLikes(postId: string) {
  return cache.useValue(postId);
}

export function seedPostLikes(client: QueryClient, records: PostLikesById[]): void {
  cache.seed(
    client,
    records.map(({ postId, ...likes }) => [postId, likes]),
  );
}

// The feed loads a page of cards then seeds every post's like state from one batch call,
// so the per-card LikeButtons read from cache instead of firing a request each (ADR-0005).
export const prefetchPostLikes = cache.prime;
