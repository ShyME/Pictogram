import { Button } from '@shared';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { followUser, unfollowUser } from './followApi';
import { followRelationshipKey } from './queryKeys';
import { useFollowRelationship } from './useFollowRelationship';

export function FollowButton({ userId }: { userId: string }) {
  const queryClient = useQueryClient();
  const key = followRelationshipKey(userId);

  const { data, isPending: loading } = useFollowRelationship(userId);

  const isFollowing = data?.followedByViewer ?? false;

  const toggle = useMutation({
    mutationFn: () => (isFollowing ? unfollowUser(userId) : followUser(userId)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: key }),
  });

  const isBusy = loading || toggle.isPending;

  return (
    <div className="flex flex-col items-start gap-1">
      <Button
        variant={isFollowing ? 'secondary' : 'primary'}
        size="sm"
        aria-pressed={isFollowing}
        disabled={isBusy}
        onClick={() => {
          toggle.mutate();
        }}
      >
        {isFollowing ? 'Following' : 'Follow'}
      </Button>
      {toggle.isError && (
        <p role="alert" className="text-xs text-danger-text">
          That didn&rsquo;t work. Try again in a moment.
        </p>
      )}
    </div>
  );
}
