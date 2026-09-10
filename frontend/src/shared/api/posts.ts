import { api } from './client';
import { orAnonymous } from './publicRead';
import type { components } from './schema';

export type PostSummary = {
  postId: string;
  authorId: string;
  mediaId: string;
  caption: string | null;
  publishedAt: string;
};

export function toPostSummary(view: components['schemas']['PostView']): PostSummary {
  return {
    postId: view.postId ?? '',
    authorId: view.authorId ?? '',
    mediaId: view.mediaId ?? '',
    caption: view.caption ?? null,
    publishedAt: view.publishedAt ?? '',
  };
}

// One batched lookup of posts by id (ADR-0005) — the notifications screen and the
// post-detail page both hold a bare post id and want its media / caption without a
// request per row. Tolerates a signed-out caller, like the rest of the post read model.
export async function fetchPosts(ids: string[]): Promise<Map<string, PostSummary>> {
  if (ids.length === 0) return new Map();

  const page = await orAnonymous<components['schemas']['ApiPagePostView']>(
    await api.GET('/api/posts/by-ids', { params: { query: { ids } } }),
    { path: '/api/posts/by-ids', query: { ids }, label: 'Post batch request' },
  );
  return new Map((page.items ?? []).map((view) => [view.postId ?? '', toPostSummary(view)]));
}
