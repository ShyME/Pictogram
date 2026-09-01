import { useFollowRelationship } from "../model/use-follow-relationship";

/**
 * The follower / following count row on a profile page (spec story 41). Reads the same
 * public query as {@link FollowButton}, so following someone and seeing the count move is
 * one cache invalidation. Counts show for everyone, signed in or not.
 */
export function FollowCounts({ userId }: { userId: string }) {
  const { data } = useFollowRelationship(userId);

  const followers = data?.followerCount ?? 0;
  const following = data?.followingCount ?? 0;

  return (
    <dl className="mt-3 flex gap-6 text-sm text-neutral-700">
      <div>
        <dt className="sr-only">Followers</dt>
        <dd>
          <span className="font-semibold">{followers}</span>{" "}
          <span className="text-neutral-500">followers</span>
        </dd>
      </div>
      <div>
        <dt className="sr-only">Following</dt>
        <dd>
          <span className="font-semibold">{following}</span>{" "}
          <span className="text-neutral-500">following</span>
        </dd>
      </div>
    </dl>
  );
}
