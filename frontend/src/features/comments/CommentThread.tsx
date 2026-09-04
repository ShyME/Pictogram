import { Button, Spinner, relativeTime, toast } from '@shared';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router';
import { type ThreadComment, canDeleteComment } from './comment';
import { CommentComposer } from './CommentComposer';
import { deleteComment } from './commentsApi';
import { CommentText } from './CommentText';
import { commentThreadKey } from './queryKeys';
import { useCommentThread } from './useCommentThread';

export function CommentThread({
  postId,
  canComment,
  viewerId = null,
  postAuthorId = null,
}: {
  postId: string;
  canComment: boolean;
  viewerId?: string | null;
  postAuthorId?: string | null;
}) {
  const thread = useCommentThread(postId);

  const comments = thread.data?.pages.flatMap((page) => page.comments) ?? [];

  return (
    <div className="flex flex-col gap-3">
      <h3 className="text-xs font-semibold uppercase tracking-[0.08em] text-foreground-subtle">
        Comments
      </h3>

      {thread.isPending ? (
        <div className="flex justify-center py-4">
          <Spinner label="Loading comments" />
        </div>
      ) : thread.isError ? (
        <p className="py-2 text-sm text-danger-text">
          We couldn&rsquo;t load the comments. Try again in a moment.
        </p>
      ) : comments.length === 0 ? (
        <p className="py-2 text-sm text-foreground-subtle">
          No comments yet.{canComment ? ' Be the first.' : ''}
        </p>
      ) : (
        <ul className="flex flex-col gap-3">
          {comments.map((comment) => (
            <CommentRow
              key={comment.commentId}
              comment={comment}
              postId={postId}
              canDelete={canDeleteComment(comment, viewerId, postAuthorId)}
            />
          ))}
        </ul>
      )}

      {thread.hasNextPage && (
        <div className="text-center">
          <Button
            variant="secondary"
            size="sm"
            onClick={() => void thread.fetchNextPage()}
            loading={thread.isFetchingNextPage}
            disabled={thread.isFetchingNextPage}
          >
            {thread.isFetchingNextPage ? 'Loading…' : 'Load more comments'}
          </Button>
        </div>
      )}

      {canComment && <CommentComposer postId={postId} />}
    </div>
  );
}

function CommentRow({
  comment,
  postId,
  canDelete,
}: {
  comment: ThreadComment;
  postId: string;
  canDelete: boolean;
}) {
  const queryClient = useQueryClient();
  const handle = comment.author?.username;
  const name = comment.author?.displayName;

  const remove = useMutation({
    mutationFn: () => deleteComment(comment.commentId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: commentThreadKey(postId) }),
    onError: () => {
      toast({
        variant: 'error',
        title: 'That comment didn’t delete',
        description: 'Try again in a moment.',
      });
    },
  });

  return (
    <li className="text-sm">
      <span className="flex items-baseline gap-2">
        {handle ? (
          <Link to={`/u/${handle}`} className="font-semibold text-foreground hover:underline">
            {name ?? `@${handle}`}
          </Link>
        ) : (
          <span className="font-semibold text-foreground">Someone</span>
        )}
        <time
          dateTime={comment.createdAt}
          className="text-xs text-foreground-subtle"
          title={new Date(comment.createdAt).toLocaleString()}
        >
          {relativeTime(comment.createdAt)}
        </time>
        {canDelete && (
          <button
            type="button"
            onClick={() => {
              remove.mutate();
            }}
            disabled={remove.isPending}
            className="ml-auto text-xs text-foreground-subtle transition-colors hover:text-danger-text disabled:opacity-50"
          >
            {remove.isPending ? 'Deleting…' : 'Delete'}
          </button>
        )}
      </span>
      <p className="mt-0.5 whitespace-pre-wrap break-words text-foreground">
        <CommentText body={comment.body} />
      </p>
    </li>
  );
}
