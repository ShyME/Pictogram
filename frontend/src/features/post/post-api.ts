import { api, problemSlug } from "@shared";
import { type Post, toPost } from "./post";

/**
 * Uploads one image and returns its `MediaId`. The bytes are re-encoded server-side to the
 * canonical square (media pipeline, #13); this is step one of publishing — {@link publishPost}
 * is step two. A 401 here means the auth middleware's silent refresh already failed, so it
 * throws like any other failure (the profile write flows do the same).
 */
export async function uploadPhoto(image: Blob): Promise<string> {
  // openapi-fetch multipart: the schema types the `file` part as a string, so `body` is a
  // placeholder and `bodySerializer` builds the real FormData (openapi-fetch then lets the
  // browser set the multipart Content-Type + boundary). Going through `api` keeps the auth
  // middleware's bearer token and silent refresh.
  const { data, response } = await api.POST("/api/media", {
    body: { file: image as unknown as string },
    bodySerializer: () => {
      const form = new FormData();
      form.append("file", image, "photo.jpg");
      return form;
    },
  });
  if (!data) throw new Error(`Photo upload failed: ${response.status}`);
  return data.mediaId;
}

export type PublishOutcome =
  | { status: "published"; post: Post }
  | { status: "media-unusable" }
  | { status: "caption-too-long" };

/**
 * Publishes an uploaded photo with an optional caption. `media-unusable` (422) means the
 * `MediaId` no longer resolves to media the caller owns — rare, but possible if the upload
 * and the publish are far apart; `caption-too-long` (400) is a backstop for the composer's
 * own length check.
 */
export async function publishPost(input: { mediaId: string; caption: string }): Promise<PublishOutcome> {
  const caption = input.caption.trim();
  const { data, error, response } = await api.POST("/api/posts", {
    body: { mediaId: input.mediaId, caption: caption || undefined },
  });

  if (data) return { status: "published", post: toPost(data) };

  switch (problemSlug(error)) {
    case "post-media-unusable":
      return { status: "media-unusable" };
    case "post-caption-too-long":
      return { status: "caption-too-long" };
    default:
      throw new Error(`Publishing failed: ${response.status}`);
  }
}

/**
 * Permanently deletes one of the caller's own posts — hard delete, no undo (#15). The UI
 * only offers this on the author's own grid, so a 403 (someone else's post) or 404 (already
 * gone) is an unexpected state and throws like any other failure.
 */
export async function deletePost(postId: string): Promise<void> {
  const { response } = await api.DELETE("/api/posts/{postId}", {
    params: { path: { postId } },
  });
  if (!response.ok) throw new Error(`Deleting the post failed: ${response.status}`);
}

export type PostPage = { posts: Post[]; nextCursor: string | null };

/**
 * A page of an author's posts, newest first. Public — a profile grid is shareable by link.
 * Pass the previous page's `nextCursor` to page on; a `null` cursor means the last page.
 */
export async function fetchPostsByAuthor(authorId: string, cursor?: string): Promise<PostPage> {
  const { data, response } = await api.GET("/api/posts", {
    params: { query: { author: authorId, cursor } },
  });
  if (!data) throw new Error(`Post grid request failed: ${response.status}`);

  return {
    posts: (data.items ?? []).map(toPost),
    nextCursor: data.nextCursor ?? null,
  };
}
