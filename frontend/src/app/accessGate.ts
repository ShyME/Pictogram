import { type Profile, fetchMyProfile } from '@features/profile';
import { redirect } from 'react-router';

export async function requireOnboarded(): Promise<Profile> {
  const me = await fetchMyProfile();
  if (me.status === 'unauthenticated') throw redirect('/login');
  if (me.status === 'not-onboarded') throw redirect('/onboarding');
  return me.profile;
}

// The viewer for a screen that anonymous visitors may also see (the public profile page):
// their profile if signed in and onboarded, otherwise null. Never redirects.
export async function fetchViewerOrNull(): Promise<Profile | null> {
  const me = await fetchMyProfile().catch(() => ({ status: 'unauthenticated' }) as const);
  return me.status === 'onboarded' ? me.profile : null;
}

export async function requireAnonymous(): Promise<null> {
  const me = await fetchMyProfile();
  if (me.status === 'onboarded') throw redirect('/');
  if (me.status === 'not-onboarded') throw redirect('/onboarding');
  return null;
}

export async function requireOnboardedOrAnon(): Promise<null> {
  const me = await fetchMyProfile();
  if (me.status === 'unauthenticated') throw redirect('/login');
  if (me.status === 'onboarded') throw redirect('/');
  return null;
}
