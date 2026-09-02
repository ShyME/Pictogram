import { Link } from 'react-router';
import { useFollowRelationship } from './useFollowRelationship';

export function FollowCounts({ userId, username }: { userId: string; username: string }) {
  const { data } = useFollowRelationship(userId);

  const followers = data?.followerCount ?? 0;
  const following = data?.followingCount ?? 0;

  return (
    <dl className="mt-3 flex gap-6 text-sm text-neutral-700">
      <div>
        <dt className="sr-only">Followers</dt>
        <dd>
          <Link to={`/u/${username}/followers`} className="hover:underline">
            <span className="font-semibold">{followers}</span>{' '}
            <span className="text-neutral-500">followers</span>
          </Link>
        </dd>
      </div>
      <div>
        <dt className="sr-only">Following</dt>
        <dd>
          <Link to={`/u/${username}/following`} className="hover:underline">
            <span className="font-semibold">{following}</span>{' '}
            <span className="text-neutral-500">following</span>
          </Link>
        </dd>
      </div>
    </dl>
  );
}
