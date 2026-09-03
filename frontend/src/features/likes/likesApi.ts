import { api, type components } from '@shared';
import { type PostLikesById, toPostLikes } from './likes';

export async function fetchPostLikesBatch(postIds: string[]): Promise<PostLikesById[]> {
  if (postIds.length === 0) return [];
  const { data, response } = await api.GET('/api/likes', {
    params: { query: { postIds } },
  });
  if (data) return data.map((view) => toRecord(view));
  if (response.status === 401) return anonymousPostLikesBatch(postIds);
  throw new Error(`Post likes batch request failed: ${response.status}`);
}

async function anonymousPostLikesBatch(postIds: string[]): Promise<PostLikesById[]> {
  const query = postIds.map((id) => `postIds=${encodeURIComponent(id)}`).join('&');
  const response = await fetch(`/api/likes?${query}`, {
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) {
    throw new Error(`Post likes batch request failed: ${response.status}`);
  }
  const views = (await response.json()) as components['schemas']['PostLikesView'][];
  return views.map((view) => toRecord(view));
}

function toRecord(view: components['schemas']['PostLikesView']): PostLikesById {
  return { postId: view.postId ?? '', ...toPostLikes(view) };
}

export async function likePost(postId: string): Promise<void> {
  const { response } = await api.PUT('/api/likes/{postId}', {
    params: { path: { postId } },
  });
  if (!response.ok) throw new Error(`Liking failed: ${response.status}`);
}

export async function unlikePost(postId: string): Promise<void> {
  const { response } = await api.DELETE('/api/likes/{postId}', {
    params: { path: { postId } },
  });
  if (!response.ok) throw new Error(`Unliking failed: ${response.status}`);
}
