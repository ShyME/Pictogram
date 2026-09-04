import { api, problemSlug, throwIfSessionExpired } from '@shared';
import type { Comment, CommentAuthor, ThreadComment } from './comment';
import { toComment } from './comment';

export type CommentThreadPage = { comments: ThreadComment[]; nextCursor: string | null };

// One page of the thread plus the one batched author lookup it needs, so the rows never
// fan out a request each (ADR-0005).
export async function fetchCommentThread(
  postId: string,
  cursor?: string,
): Promise<CommentThreadPage> {
  const { data, response } = await api.GET('/api/posts/{postId}/comments', {
    params: { path: { postId }, query: { cursor } },
  });
  throwIfSessionExpired(response);
  if (!data) throw new Error(`Comment thread request failed: ${response.status}`);

  const comments = (data.items ?? []).map((item) => toComment(item));
  const authors = await fetchAuthors([...new Set(comments.map((comment) => comment.authorId))]);

  return {
    comments: comments.map((comment) => ({
      ...comment,
      author: authors.get(comment.authorId) ?? null,
    })),
    nextCursor: data.nextCursor ?? null,
  };
}

async function fetchAuthors(ids: string[]): Promise<Map<string, CommentAuthor>> {
  if (ids.length === 0) return new Map();

  const { data, response } = await api.GET('/api/profiles', { params: { query: { ids } } });
  throwIfSessionExpired(response);
  if (!data) throw new Error(`Comment author batch request failed: ${response.status}`);

  return new Map(
    data.map((view) => [
      view.userId ?? '',
      {
        userId: view.userId ?? '',
        username: view.username ?? '',
        displayName: view.displayName ?? null,
      },
    ]),
  );
}

export type PostCommentOutcome =
  { status: 'created'; comment: Comment } | { status: 'empty' } | { status: 'too-long' };

export async function postComment(postId: string, body: string): Promise<PostCommentOutcome> {
  const { data, error, response } = await api.POST('/api/posts/{postId}/comments', {
    params: { path: { postId } },
    body: { body },
  });

  if (data) return { status: 'created', comment: toComment(data) };

  throwIfSessionExpired(response);
  switch (problemSlug(error)) {
    case 'comment-empty':
      return { status: 'empty' };
    case 'comment-too-long':
      return { status: 'too-long' };
    default:
      throw new Error(`Posting a comment failed: ${response.status}`);
  }
}
