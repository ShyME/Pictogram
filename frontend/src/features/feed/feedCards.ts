import { fetchAccounts } from '@shared';
import { type FeedCard, type FeedPost, imageUrl } from './feed';

export async function toFeedCards(posts: FeedPost[]): Promise<FeedCard[]> {
  const authors = await fetchAccounts([...new Set(posts.map((post) => post.authorId))]);

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
