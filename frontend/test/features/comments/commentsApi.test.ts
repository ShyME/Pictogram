import { fetchCommentThread, postComment } from '@features/comments/commentsApi';
import { jsonResponse, pathOf, problemResponse, stubFetch } from '@test-support/mockFetch';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

type CommentViewFixture = {
  commentId: string;
  postId: string;
  authorId: string;
  body: string;
  createdAt: string;
};

const commentView = (over: Partial<CommentViewFixture> = {}): CommentViewFixture => ({
  commentId: 'c-1',
  postId: 'p-1',
  authorId: 'u-ada',
  body: 'nice',
  createdAt: '2026-09-04T10:00:00Z',
  ...over,
});

test('postComment returns the created comment', async () => {
  const calls = stubFetch(() => jsonResponse(commentView(), 201));

  const outcome = await postComment('p-1', 'nice');

  expect(outcome).toEqual({
    status: 'created',
    comment: {
      commentId: 'c-1',
      postId: 'p-1',
      authorId: 'u-ada',
      body: 'nice',
      createdAt: '2026-09-04T10:00:00Z',
    },
  });
  expect(pathOf(calls[0])).toBe('/api/posts/p-1/comments');
  expect(calls[0].method).toBe('POST');
});

test('postComment maps the empty and too-long problem responses', async () => {
  stubFetch(() => problemResponse('comment-empty', 400));
  expect(await postComment('p-1', '  ')).toEqual({ status: 'empty' });

  stubFetch(() => problemResponse('comment-too-long', 400));
  expect(await postComment('p-1', 'x'.repeat(2000))).toEqual({ status: 'too-long' });
});

test('fetchCommentThread enriches every comment with one batched author lookup', async () => {
  const calls = stubFetch((request) => {
    const path = pathOf(request);
    if (path === '/api/posts/p-1/comments') {
      return jsonResponse({
        items: [
          commentView({ commentId: 'c-1', authorId: 'u-ada' }),
          commentView({ commentId: 'c-2', authorId: 'u-ada' }),
        ],
        nextCursor: 'CURSOR',
      });
    }
    if (path === '/api/profiles') {
      return jsonResponse([{ userId: 'u-ada', username: 'ada', displayName: 'Ada' }]);
    }
    throw new Error(`unexpected ${path}`);
  });

  const page = await fetchCommentThread('p-1');

  expect(page.nextCursor).toBe('CURSOR');
  expect(page.comments.map((comment) => comment.author?.username)).toEqual(['ada', 'ada']);
  expect(calls.filter((call) => pathOf(call) === '/api/profiles')).toHaveLength(1);
});
