import { api, throwIfSessionExpired } from '@shared';
import { type FeedAuthor, type FeedCard, type FeedPost, imageUrl } from './feed';

export async function toFeedCards(posts: FeedPost[]): Promise<FeedCard[]> {
  const authors = await fetchAuthors(unique(posts.map((post) => post.authorId)));

  return posts.map((post) => ({
    postId: post.postId,
    author: authors.get(post.authorId) ?? {
      userId: post.authorId,
      username: '',
      displayName: null,
    },
    imageUrl: imageUrl(post.mediaId),
    caption: post.caption,
    publishedAt: post.publishedAt,
  }));
}

async function fetchAuthors(ids: string[]): Promise<Map<string, FeedAuthor>> {
  if (ids.length === 0) return new Map();

  const { data, response } = await api.GET('/api/profiles', {
    params: { query: { ids } },
  });
  throwIfSessionExpired(response);
  if (!data) throw new Error(`Feed author batch request failed: ${response.status}`);

  return new Map(
    data.map((view) => [
      view.userId ?? '',
      {
        userId: view.userId ?? '',
        username: view.username ?? '',
        displayName: view.displayName ?? null,
      },
    ]),
  );
}

function unique(values: string[]): string[] {
  return [...new Set(values)];
}
