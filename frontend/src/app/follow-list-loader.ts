import type { FollowListData } from '@features/follow';
import { fetchProfileByUsername } from '@features/profile';
import type { LoaderFunctionArgs } from 'react-router';
import { requireOnboarded } from './access-gate';

export async function followListLoader({
  params,
}: LoaderFunctionArgs): Promise<FollowListData | Response> {
  const username = params.username ?? '';
  try {
    const [viewer, lookup] = await Promise.all([
      requireOnboarded(),
      fetchProfileByUsername(username),
    ]);

    if (lookup.status === 'not-found') return { status: 'not-found', username };

    return {
      status: 'found',
      target: { userId: lookup.profile.userId, username: lookup.profile.username },
      viewerId: viewer.userId,
    };
  } catch (thrown) {
    if (thrown instanceof Response) return thrown;
    throw thrown;
  }
}
