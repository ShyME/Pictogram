import type { components } from '@shared';

export type Post = {
  postId: string;
  authorId: string;
  mediaId: string;
  caption: string | null;
  publishedAt: string;
};

export function toPost(view: components['schemas']['PostView']): Post {
  return {
    postId: view.postId ?? '',
    authorId: view.authorId ?? '',
    mediaId: view.mediaId ?? '',
    caption: view.caption ?? null,
    publishedAt: view.publishedAt ?? '',
  };
}

export function originalUrl(mediaId: string): string {
  return `/api/media/${mediaId}/original`;
}

export function thumbnailUrl(mediaId: string): string {
  return `/api/media/${mediaId}/thumbnail`;
}
