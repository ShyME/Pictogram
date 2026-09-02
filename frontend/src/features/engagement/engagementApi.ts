import { api } from '@shared';
import { type PostLikesById, toPostLikes } from './engagement';

export async function fetchPostLikesBatch(postIds: string[]): Promise<PostLikesById[]> {
  if (postIds.length === 0) return [];
  const { data, response } = await api.GET('/api/engagement/likes', {
    params: { query: { postIds } },
  });
  if (!data) throw new Error(`Post likes batch request failed: ${response.status}`);
  return data.map((view) => ({ postId: view.postId ?? '', ...toPostLikes(view) }));
}

export async function likePost(postId: string): Promise<void> {
  const { response } = await api.PUT('/api/engagement/likes/{postId}', {
    params: { path: { postId } },
  });
  if (!response.ok) throw new Error(`Liking failed: ${response.status}`);
}

export async function unlikePost(postId: string): Promise<void> {
  const { response } = await api.DELETE('/api/engagement/likes/{postId}', {
    params: { path: { postId } },
  });
  if (!response.ok) throw new Error(`Unliking failed: ${response.status}`);
}
