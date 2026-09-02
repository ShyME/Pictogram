import { useMutation, useQueryClient } from '@tanstack/react-query';
import { followUser, unfollowUser } from './followApi';
import { followRelationshipKey } from './queryKeys';
import { useFollowRelationship } from './useFollowRelationship';

export function FollowButton({ userId }: { userId: string }) {
  const queryClient = useQueryClient();
  const key = followRelationshipKey(userId);

  const { data, isPending: loading } = useFollowRelationship(userId);

  const following = data?.followedByViewer ?? false;

  const toggle = useMutation({
    mutationFn: () => (following ? unfollowUser(userId) : followUser(userId)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: key }),
  });

  const busy = loading || toggle.isPending;

  return (
    <div className="flex flex-col items-start gap-1">
      <button
        type="button"
        aria-pressed={following}
        disabled={busy}
        onClick={() => {
          toggle.mutate();
        }}
        className={
          following
            ? 'rounded-lg border border-neutral-300 px-3 py-1.5 text-sm font-medium text-neutral-700 hover:bg-neutral-50 disabled:opacity-50'
            : 'rounded-lg bg-neutral-900 px-3 py-1.5 text-sm font-medium text-white hover:bg-neutral-700 disabled:opacity-50'
        }
      >
        {following ? 'Following' : 'Follow'}
      </button>
      {toggle.isError && (
        <p role="alert" className="text-xs text-red-600">
          That didn&rsquo;t work. Try again in a moment.
        </p>
      )}
    </div>
  );
}
