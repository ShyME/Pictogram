import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Heart } from 'lucide-react';
import { likeLabel } from './likes';
import { likePost, unlikePost } from './likesApi';
import { postLikesKey } from './queryKeys';
import { usePostLikes } from './useLikes';

export function LikeButton({ postId }: { postId: string }) {
  const queryClient = useQueryClient();
  const key = postLikesKey(postId);

  const { data, isPending: loading } = usePostLikes(postId);

  const isLiked = data?.likedByViewer ?? false;
  const count = data?.likeCount ?? 0;

  const toggle = useMutation({
    mutationFn: () => (isLiked ? unlikePost(postId) : likePost(postId)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: key }),
  });

  const isBusy = loading || toggle.isPending;

  return (
    <div className="flex items-center gap-2">
      <button
        type="button"
        aria-pressed={isLiked}
        aria-label={isLiked ? 'Unlike' : 'Like'}
        disabled={isBusy}
        onClick={() => {
          toggle.mutate();
        }}
        className={
          isLiked
            ? 'text-like transition-colors disabled:opacity-50'
            : 'text-foreground-subtle transition-colors hover:text-foreground-muted disabled:opacity-50'
        }
      >
        <Heart className={isLiked ? 'size-6 fill-current' : 'size-6'} aria-hidden="true" />
      </button>
      <span className="text-sm text-foreground-muted">{likeLabel(count)}</span>
      {toggle.isError && (
        <p role="alert" className="text-xs text-danger-text">
          That didn&rsquo;t work. Try again in a moment.
        </p>
      )}
    </div>
  );
}
