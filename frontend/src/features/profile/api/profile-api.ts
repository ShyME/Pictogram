import { api, problemSlug } from "@shared";
import { type Profile, toProfile } from "../model/profile";

export type MyProfile =
  | { status: "onboarded"; profile: Profile }
  | { status: "not-onboarded" }
  | { status: "unauthenticated" };

/** Reads the caller's own profile — the root-route guard's single source of truth. */
export async function fetchMyProfile(): Promise<MyProfile> {
  const { data, error, response } = await api.GET("/api/profiles/me");
  if (data) return { status: "onboarded", profile: toProfile(data) };
  if (problemSlug(error) === "profile-not-found") return { status: "not-onboarded" };
  // A 401 is the shared auth boundary (the middleware's silent refresh has already failed),
  // not a documented response of this endpoint.
  if (response.status === 401) return { status: "unauthenticated" };
  throw new Error(`Unexpected /api/profiles/me response: ${response.status}`);
}

export type ProfileLookup =
  | { status: "found"; profile: Profile }
  | { status: "not-found" };

/**
 * Reads a public profile by username — the `/u/<username>` page's data. A username nobody
 * holds comes back as `not-found` (a `profile-not-found` Problem Detail) so the page can
 * show a clear "no such account" screen rather than an error.
 *
 * A stale in-memory access token that can't be refreshed makes the resource server reject
 * even this public call with a 401 (the bearer filter runs before the permit rule). Since
 * the profile is public, we retry once with no credentials rather than fail the page.
 */
export async function fetchProfileByUsername(username: string): Promise<ProfileLookup> {
  const { data, error, response } = await api.GET("/api/profiles/{username}", {
    params: { path: { username } },
  });
  if (data) return { status: "found", profile: toProfile(data) };
  if (problemSlug(error) === "profile-not-found") return { status: "not-found" };
  if (response.status === 401) return anonymousProfileLookup(username);
  throw new Error(`Unexpected /api/profiles/${username} response: ${response.status}`);
}

async function anonymousProfileLookup(username: string): Promise<ProfileLookup> {
  const response = await fetch(`/api/profiles/${encodeURIComponent(username)}`, {
    headers: { Accept: "application/json" },
  });
  if (response.ok) {
    return { status: "found", profile: toProfile(await response.json()) };
  }
  if (response.status === 404) return { status: "not-found" };
  throw new Error(`Unexpected /api/profiles/${username} response: ${response.status}`);
}

export type OnboardingInput = {
  username: string;
  displayName?: string;
  bio?: string;
};

export type OnboardingOutcome =
  | { status: "created"; profile: Profile }
  | { status: "already-onboarded" }
  | { status: "username-taken" }
  | { status: "username-invalid" }
  | { status: "details-invalid" };

/**
 * Creates the profile from a chosen username (plus optional display name and bio). The
 * two username failures are kept distinct so the form can tell "wrong shape" from
 * "taken"; an existing profile resolves as `already-onboarded` rather than an error,
 * since the caller just wants to move on to the feed.
 */
export async function submitOnboarding(input: OnboardingInput): Promise<OnboardingOutcome> {
  const { data, error, response } = await api.POST("/api/profiles", {
    body: {
      username: input.username,
      displayName: emptyToUndefined(input.displayName),
      bio: emptyToUndefined(input.bio),
    },
  });

  if (data) return { status: "created", profile: toProfile(data) };

  switch (problemSlug(error)) {
    case "username-taken":
      return { status: "username-taken" };
    case "username-invalid":
      return { status: "username-invalid" };
    case "profile-details-invalid":
      return { status: "details-invalid" };
    case "already-onboarded":
      return { status: "already-onboarded" };
    default:
      throw new Error(`Onboarding failed: ${response.status}`);
  }
}

function emptyToUndefined(value: string | undefined): string | undefined {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}
