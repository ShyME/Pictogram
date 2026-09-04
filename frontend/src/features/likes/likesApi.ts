import { api, type components, orAnonymous } from '@shared';
import { type PostLikesById, toPostLikes } from './likes';

export async function fetchPostLikesBatch(postIds: string[]): Promise<PostLikesById[]> {
  if (postIds.length === 0) return [];
  const views = await orAnonymous<components['schemas']['PostLikesView'][]>(
    await api.GET('/api/likes', { params: { query: { postIds } } }),
    { path: '/api/likes', query: { postIds }, label: 'Post likes batch request' },
  );
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
