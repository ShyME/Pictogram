import { useMutation, useQueryClient } from '@tanstack/react-query';
import { likePost, unlikePost } from './engagementApi';
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
        <svg
          viewBox="0 0 24 24"
          className="size-6"
          fill={isLiked ? 'currentColor' : 'none'}
          stroke="currentColor"
          strokeWidth="2"
          aria-hidden="true"
        >
          <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 1 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8Z" />
        </svg>
      </button>
      <span className="text-sm text-neutral-500">{count === 1 ? '1 like' : `${count} likes`}</span>
      {toggle.isError && (
        <p role="alert" className="text-xs text-red-600">
          That didn&rsquo;t work. Try again in a moment.
        </p>
      )}
    </div>
  );
}
