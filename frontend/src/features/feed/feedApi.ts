import { api, type components, orAnonymous, throwIfSessionExpired } from '@shared';
import { type FeedPost, toFeedPost } from './feed';
import { exploreKey, feedKey } from './queryKeys';

export type FeedPostPage = { posts: FeedPost[]; nextCursor: string | null };

export async function fetchFeedPage(cursor?: string): Promise<FeedPostPage> {
  const { data, response } = await api.GET('/api/feed', {
    params: { query: { cursor } },
  });
  throwIfSessionExpired(response);
  if (!data) throw new Error(`Feed request failed: ${response.status}`);

  return {
    posts: (data.items ?? []).map((item) => toFeedPost(item)),
    nextCursor: data.nextCursor ?? null,
  };
}

export async function fetchExplorePage(cursor?: string): Promise<FeedPostPage> {
  const data = await orAnonymous<components['schemas']['ApiPageFeedPost']>(
    await api.GET('/api/explore', { params: { query: { cursor } } }),
    { path: '/api/explore', query: { cursor }, label: 'Explore request' },
  );

  return {
    posts: (data.items ?? []).map((item) => toFeedPost(item)),
    nextCursor: data.nextCursor ?? null,
  };
}

export type FeedSource = {
  queryKey: readonly unknown[];
  fetchPage: (cursor?: string) => Promise<FeedPostPage>;
  loadingLabel: string;
  emptyTitle: string;
  emptyDescription: string;
  loadErrorMessage: string;
};

export const followingSource: FeedSource = {
  queryKey: feedKey(),
  fetchPage: fetchFeedPage,
  loadingLabel: 'Loading your feed',
  emptyTitle: 'Your feed is quiet',
  emptyDescription: 'Find people to follow and their posts will show up here.',
  loadErrorMessage: 'We couldn’t load your feed. Try again in a moment.',
};

export const exploreSource: FeedSource = {
  queryKey: exploreKey(),
  fetchPage: fetchExplorePage,
  loadingLabel: 'Loading Explore',
  emptyTitle: 'No posts yet',
  emptyDescription: 'Once people start posting, their photos will show up here.',
  loadErrorMessage: 'We couldn’t load Explore. Try again in a moment.',
};
