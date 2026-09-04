import type { Account, components } from '@shared';

export type Comment = {
  commentId: string;
  postId: string;
  authorId: string;
  body: string;
  createdAt: string;
};

export function toComment(view: components['schemas']['CommentView']): Comment {
  return {
    commentId: view.commentId ?? '',
    postId: view.postId ?? '',
    authorId: view.authorId ?? '',
    body: view.body ?? '',
    createdAt: view.createdAt ?? '',
  };
}

export type ThreadComment = Comment & { author: Account | null };

export type PostCommentCount = { commentCount: number };

export type PostCommentCountById = PostCommentCount & { postId: string };

export function toCommentCount(view: components['schemas']['PostCommentsView']): PostCommentCount {
  return { commentCount: view.commentCount ?? 0 };
}

export function commentCountLabel(count: number): string {
  return count === 1 ? '1 comment' : `${count} comments`;
}

// A comment may be removed by its own author or by the post's author (#138); the backend
// enforces the same rule, this only decides whether to show the control.
export function canDeleteComment(
  comment: { authorId: string },
  viewerId: string | null,
  postAuthorId: string | null,
): boolean {
  if (!viewerId) return false;
  return comment.authorId === viewerId || postAuthorId === viewerId;
}

export const MAX_COMMENT_LENGTH = 1000;

export function commentLength(body: string): number {
  // Count Unicode code points, not UTF-16 units, so an emoji counts as one — the backend
  // enforces the same limit the same way (`CommentBody`).
  // eslint-disable-next-line @typescript-eslint/no-misused-spread
  return [...body].length;
}

// http / https only; `@mention`s are deliberately left alone (#137).
const URL_PATTERN = /\bhttps?:\/\/[^\s<]+[^\s<.,!?;:)\]}'"]/gi;

export type CommentSegment = { text: string; href?: string };

export function commentSegments(body: string): CommentSegment[] {
  const segments: CommentSegment[] = [];
  let cursor = 0;
  for (const match of body.matchAll(URL_PATTERN)) {
    const start = match.index;
    if (start > cursor) segments.push({ text: body.slice(cursor, start) });
    segments.push({ text: match[0], href: match[0] });
    cursor = start + match[0].length;
  }
  if (cursor < body.length) segments.push({ text: body.slice(cursor) });
  return segments;
}
