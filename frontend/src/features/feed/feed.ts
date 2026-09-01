import type { components } from '@shared';

export type FeedPost = {
  postId: string;
  authorId: string;
  mediaId: string;
  caption: string | null;
  publishedAt: string;
};

export function toFeedPost(wire: components['schemas']['FeedPost']): FeedPost {
  return {
    postId: wire.postId ?? '',
    authorId: wire.authorId ?? '',
    mediaId: wire.mediaId ?? '',
    caption: wire.caption ?? null,
    publishedAt: wire.publishedAt ?? '',
  };
}

export type FeedAuthor = {
  userId: string;
  username: string;
  displayName: string | null;
};

export type FeedCard = {
  postId: string;
  author: FeedAuthor;
  imageUrl: string;
  caption: string | null;
  publishedAt: string;
};

export function imageUrl(mediaId: string): string {
  return `/api/media/${mediaId}/original`;
}
