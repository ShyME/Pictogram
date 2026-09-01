import { api, problemSlug } from '@shared';
import { type Profile, toProfile } from './profile';

export type MyProfile =
  | { status: 'onboarded'; profile: Profile }
  | { status: 'not-onboarded' }
  | { status: 'unauthenticated' };

// A route loader calls this and turns "unauthenticated" into a redirect, so it reports a
// dead session as a status rather than throwing SessionExpiredError like the data fetches.
export async function fetchMyProfile(): Promise<MyProfile> {
  const { data, error, response } = await api.GET('/api/profiles/me');
  if (data) return { status: 'onboarded', profile: toProfile(data) };
  if (problemSlug(error) === 'profile-not-found') return { status: 'not-onboarded' };
  if (response.status === 401) return { status: 'unauthenticated' };
  throw new Error(`Unexpected /api/profiles/me response: ${response.status}`);
}

export type ProfileLookup = { status: 'found'; profile: Profile } | { status: 'not-found' };

export async function fetchProfileByUsername(username: string): Promise<ProfileLookup> {
  const { data, error, response } = await api.GET('/api/profiles/{username}', {
    params: { path: { username } },
  });
  if (data) return { status: 'found', profile: toProfile(data) };
  if (problemSlug(error) === 'profile-not-found') return { status: 'not-found' };
  if (response.status === 401) return anonymousProfileLookup(username);
  throw new Error(`Unexpected /api/profiles/${username} response: ${response.status}`);
}

async function anonymousProfileLookup(username: string): Promise<ProfileLookup> {
  const response = await fetch(`/api/profiles/${encodeURIComponent(username)}`, {
    headers: { Accept: 'application/json' },
  });
  if (response.ok) {
    return { status: 'found', profile: toProfile(await response.json()) };
  }
  if (response.status === 404) return { status: 'not-found' };
  throw new Error(`Unexpected /api/profiles/${username} response: ${response.status}`);
}

export type ProfileFields = {
  username: string;
  displayName?: string;
  bio?: string;
};

export type ProfileFieldError = 'username-taken' | 'username-invalid' | 'details-invalid';

export type OnboardingOutcome =
  | { status: 'created'; profile: Profile }
  | { status: 'already-onboarded' }
  | { status: ProfileFieldError };

export async function submitOnboarding(input: ProfileFields): Promise<OnboardingOutcome> {
  const { data, error, response } = await api.POST('/api/profiles', {
    body: profileWriteBody(input),
  });

  if (data) return { status: 'created', profile: toProfile(data) };

  const field = fieldError(error);
  if (field) return { status: field };
  if (problemSlug(error) === 'already-onboarded') return { status: 'already-onboarded' };
  throw new Error(`Onboarding failed: ${response.status}`);
}

export type ProfileEditOutcome =
  | { status: 'updated'; profile: Profile }
  | { status: 'not-onboarded' }
  | { status: ProfileFieldError };

export async function submitProfileEdit(input: ProfileFields): Promise<ProfileEditOutcome> {
  const { data, error, response } = await api.PUT('/api/profiles/me', {
    body: profileWriteBody(input),
  });

  if (data) return { status: 'updated', profile: toProfile(data) };

  const field = fieldError(error);
  if (field) return { status: field };
  if (problemSlug(error) === 'profile-not-found') return { status: 'not-onboarded' };
  throw new Error(`Profile edit failed: ${response.status}`);
}

function profileWriteBody(input: ProfileFields) {
  return {
    username: input.username,
    displayName: emptyToUndefined(input.displayName),
    bio: emptyToUndefined(input.bio),
  };
}

function fieldError(error: unknown): ProfileFieldError | null {
  switch (problemSlug(error)) {
    case 'username-taken':
      return 'username-taken';
    case 'username-invalid':
      return 'username-invalid';
    case 'profile-details-invalid':
      return 'details-invalid';
    default:
      return null;
  }
}

function emptyToUndefined(value: string | undefined): string | undefined {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}
