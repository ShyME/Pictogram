import type { LoaderFunctionArgs } from 'react-router';
import type { Profile } from './profile';
import { fetchMyProfile, fetchProfileByUsername } from './profileApi';

export type ProfilePageData =
  | {
      status: 'found';
      profile: Profile;
      isOwnProfile: boolean;
      viewerCanFollow: boolean;
      viewerIsAuthenticated: boolean;
      viewerId: string | null;
    }
  | { status: 'not-found'; username: string };

export async function profileLoader({ params }: LoaderFunctionArgs): Promise<ProfilePageData> {
  const username = params.username ?? '';
  const [lookup, me] = await Promise.all([
    fetchProfileByUsername(username),
    fetchMyProfile().catch(() => ({ status: 'unauthenticated' }) as const),
  ]);

  if (lookup.status === 'not-found') return { status: 'not-found', username };

  const isOwnProfile = me.status === 'onboarded' && me.profile.username === lookup.profile.username;
  return {
    status: 'found',
    profile: lookup.profile,
    isOwnProfile,
    viewerCanFollow: me.status === 'onboarded' && !isOwnProfile,
    viewerIsAuthenticated: me.status !== 'unauthenticated',
    viewerId: me.status === 'onboarded' ? me.profile.userId : null,
  };
}
