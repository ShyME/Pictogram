import { MessageCircle } from 'lucide-react';
import { commentCountLabel } from './comment';
import { usePostCommentCount } from './useCommentCounts';

export function CommentCount({ postId }: { postId: string }) {
  const { data } = usePostCommentCount(postId);
  const count = data?.commentCount ?? 0;

  return (
    <span className="flex items-center gap-2 text-foreground-muted">
      <MessageCircle className="size-6" aria-hidden="true" />
      <span className="text-sm">{commentCountLabel(count)}</span>
    </span>
  );
}
