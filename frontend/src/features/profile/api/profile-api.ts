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

/** The username and optional details onboarding and the edit form both submit. */
export type ProfileFields = {
  username: string;
  displayName?: string;
  bio?: string;
};

/** The three field-level failures onboarding and edit share, by their form-facing name. */
export type ProfileFieldError = "username-taken" | "username-invalid" | "details-invalid";

export type OnboardingOutcome =
  | { status: "created"; profile: Profile }
  | { status: "already-onboarded" }
  | { status: ProfileFieldError };

/**
 * Creates the profile from a chosen username (plus optional display name and bio). The
 * two username failures are kept distinct so the form can tell "wrong shape" from
 * "taken"; an existing profile resolves as `already-onboarded` rather than an error,
 * since the caller just wants to move on to the feed.
 */
export async function submitOnboarding(input: ProfileFields): Promise<OnboardingOutcome> {
  const { data, error, response } = await api.POST("/api/profiles", { body: profileWriteBody(input) });

  if (data) return { status: "created", profile: toProfile(data) };

  const field = fieldError(error);
  if (field) return { status: field };
  if (problemSlug(error) === "already-onboarded") return { status: "already-onboarded" };
  throw new Error(`Onboarding failed: ${response.status}`);
}

export type ProfileEditOutcome =
  | { status: "updated"; profile: Profile }
  | { status: "not-onboarded" }
  | { status: ProfileFieldError };

/**
 * Edits the caller's own profile — display name, bio, and username. `username` is always
 * sent (the edit form pre-fills it); passing back the current handle just leaves it be,
 * while a different one renames and frees the old handle. A caller who is not onboarded
 * yet (no profile to edit) resolves as `not-onboarded` rather than an error.
 */
export async function submitProfileEdit(input: ProfileFields): Promise<ProfileEditOutcome> {
  const { data, error, response } = await api.PUT("/api/profiles/me", { body: profileWriteBody(input) });

  if (data) return { status: "updated", profile: toProfile(data) };

  const field = fieldError(error);
  if (field) return { status: field };
  if (problemSlug(error) === "profile-not-found") return { status: "not-onboarded" };
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
    case "username-taken":
      return "username-taken";
    case "username-invalid":
      return "username-invalid";
    case "profile-details-invalid":
      return "details-invalid";
    default:
      return null;
  }
}

function emptyToUndefined(value: string | undefined): string | undefined {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}
