import type { components } from '@shared';

export type PostLikes = {
  likeCount: number;
  likedByViewer: boolean;
};

export type PostLikesById = PostLikes & { postId: string };

export function toPostLikes(view: components['schemas']['PostLikesView']): PostLikes {
  return {
    likeCount: view.likeCount ?? 0,
    likedByViewer: view.likedByViewer ?? false,
  };
}
