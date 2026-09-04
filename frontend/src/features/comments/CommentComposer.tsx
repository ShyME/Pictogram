import { Button, Textarea, toast } from '@shared';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useId, useState } from 'react';
import { MAX_COMMENT_LENGTH, commentLength } from './comment';
import { postComment } from './commentsApi';
import { commentThreadKey } from './queryKeys';

export function CommentComposer({ postId }: { postId: string }) {
  const queryClient = useQueryClient();
  const [body, setBody] = useState('');
  const [error, setError] = useState<string | null>(null);
  const errorId = useId();

  const add = useMutation({
    mutationFn: () => postComment(postId, body),
    onSuccess: async (outcome) => {
      if (outcome.status === 'too-long') {
        setError(`Keep it under ${MAX_COMMENT_LENGTH} characters.`);
        return;
      }
      if (outcome.status === 'empty') {
        setError('Write something first.');
        return;
      }
      setBody('');
      setError(null);
      await queryClient.invalidateQueries({ queryKey: commentThreadKey(postId) });
    },
    onError: () => {
      toast({
        variant: 'error',
        title: 'Your comment didn’t post',
        description: 'Try again in a moment.',
      });
    },
  });

  const length = commentLength(body);
  const isOverLimit = length > MAX_COMMENT_LENGTH;
  const canSubmit = body.trim().length > 0 && !isOverLimit && !add.isPending;
  const remaining = MAX_COMMENT_LENGTH - length;

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        if (canSubmit) add.mutate();
      }}
      className="flex flex-col gap-2"
    >
      <Textarea
        value={body}
        onChange={(event) => {
          setBody(event.target.value);
          if (error) setError(null);
        }}
        placeholder="Add a comment…"
        rows={2}
        aria-label="Add a comment"
        aria-invalid={isOverLimit || undefined}
        aria-describedby={error ? errorId : undefined}
      />
      <div className="flex items-center justify-between gap-3">
        {error ? (
          <p id={errorId} role="alert" className="text-xs text-danger-text">
            {error}
          </p>
        ) : (
          <span
            className={
              isOverLimit
                ? 'text-xs text-danger-text'
                : 'text-xs text-foreground-subtle tabular-nums'
            }
          >
            {remaining < 100 ? remaining.toString() : ''}
          </span>
        )}
        <Button type="submit" size="sm" loading={add.isPending} disabled={!canSubmit}>
          Post
        </Button>
      </div>
    </form>
  );
}
