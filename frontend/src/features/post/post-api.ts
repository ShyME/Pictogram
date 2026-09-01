import { api, problemSlug } from "@shared";
import { type Post, toPost } from "./post";

export async function uploadPhoto(image: Blob): Promise<string> {
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

export async function deletePost(postId: string): Promise<void> {
  const { response } = await api.DELETE("/api/posts/{postId}", {
    params: { path: { postId } },
  });
  if (!response.ok) throw new Error(`Deleting the post failed: ${response.status}`);
}

export type PostPage = { posts: Post[]; nextCursor: string | null };

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
