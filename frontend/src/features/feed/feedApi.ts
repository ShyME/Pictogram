import { api, throwIfSessionExpired } from '@shared';
import { type FeedPost, toFeedPost } from './feed';

export type FeedPostPage = { posts: FeedPost[]; nextCursor: string | null };

export async function fetchFeedPage(cursor?: string): Promise<FeedPostPage> {
  const { data, response } = await api.GET('/api/feed', {
    params: { query: { cursor } },
  });
  throwIfSessionExpired(response);
  if (!data) throw new Error(`Feed request failed: ${response.status}`);

  return {
    posts: (data.items ?? []).map(toFeedPost),
    nextCursor: data.nextCursor ?? null,
  };
}
