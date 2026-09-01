import type { components } from "@shared";

export type Post = {
  postId: string;
  authorId: string;
  mediaId: string;
  caption: string | null;
  publishedAt: string;
};

// The generated schema marks every PostView field optional (no `required` block), but a
// real post always has these ids and a timestamp; `caption` is genuinely absent when the
// author left it blank.
export function toPost(view: components["schemas"]["PostView"]): Post {
  return {
    postId: view.postId ?? "",
    authorId: view.authorId ?? "",
    mediaId: view.mediaId ?? "",
    caption: view.caption ?? null,
    publishedAt: view.publishedAt ?? "",
  };
}

/** The full-size rendition URL media serves for a post's image. */
export function originalUrl(mediaId: string): string {
  return `/api/media/${mediaId}/original`;
}

/** The square thumbnail rendition URL — what a grid cell shows. */
export function thumbnailUrl(mediaId: string): string {
  return `/api/media/${mediaId}/thumbnail`;
}
