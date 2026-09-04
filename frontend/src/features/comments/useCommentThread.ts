import { useInfiniteQuery } from '@tanstack/react-query';
import { fetchCommentThread } from './commentsApi';
import { commentThreadKey } from './queryKeys';

export function useCommentThread(postId: string) {
  return useInfiniteQuery({
    queryKey: commentThreadKey(postId),
    queryFn: ({ pageParam }) => fetchCommentThread(postId, pageParam),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (page) => page.nextCursor ?? undefined,
  });
}
