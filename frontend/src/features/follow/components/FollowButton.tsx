import { useMutation, useQueryClient } from "@tanstack/react-query";
import { followUser, unfollowUser } from "../api/follow-api";
import { followRelationshipKey } from "../model/query-keys";
import { useFollowRelationship } from "../model/use-follow-relationship";

/**
 * The follow / unfollow control on another user's profile (spec story 20). The label
 * reflects the current relationship — "Follow" when not following, "Following" when already
 * following (clicking it unfollows). Both server calls are idempotent, so a double-click is
 * harmless; on success the shared relationship query is invalidated and the count row
 * ({@link FollowCounts}) moves with it.
 *
 * The app layer only renders this for a signed-in viewer looking at someone else's profile,
 * so there is no "follow yourself" or "log in first" state to handle here.
 */
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
        onClick={() => toggle.mutate()}
        className={
          following
            ? "rounded-lg border border-neutral-300 px-3 py-1.5 text-sm font-medium text-neutral-700 hover:bg-neutral-50 disabled:opacity-50"
            : "rounded-lg bg-neutral-900 px-3 py-1.5 text-sm font-medium text-white hover:bg-neutral-700 disabled:opacity-50"
        }
      >
        {following ? "Following" : "Follow"}
      </button>
      {toggle.isError && (
        <p role="alert" className="text-xs text-red-600">
          That didn&rsquo;t work. Try again in a moment.
        </p>
      )}
    </div>
  );
}
