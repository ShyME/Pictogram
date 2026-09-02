import { useMutation, useQueryClient } from '@tanstack/react-query';
import { likeLabel } from './engagement';
import { likePost, unlikePost } from './engagementApi';
import { HeartGlyph } from './HeartGlyph';
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
            ? 'text-red-600 transition hover:text-red-500 disabled:opacity-50'
            : 'text-neutral-400 transition hover:text-neutral-600 disabled:opacity-50'
        }
      >
        <HeartGlyph filled={isLiked} />
      </button>
      <span className="text-sm text-neutral-500">{likeLabel(count)}</span>
      {toggle.isError && (
        <p role="alert" className="text-xs text-red-600">
          That didn&rsquo;t work. Try again in a moment.
        </p>
      )}
    </div>
  );
}
