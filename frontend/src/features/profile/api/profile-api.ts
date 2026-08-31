import { api, problemSlug } from "@shared";
import { type Profile, toProfile } from "../model/profile";

export type MyProfile =
  | { status: "onboarded"; profile: Profile }
  | { status: "not-onboarded" }
  | { status: "unauthenticated" };

/** Reads the caller's own profile — the root-route guard's single source of truth. */
export async function fetchMyProfile(): Promise<MyProfile> {
  const { data, response } = await api.GET("/api/profiles/me");
  if (response.status === 401) return { status: "unauthenticated" };
  if (response.status === 404) return { status: "not-onboarded" };
  if (data) return { status: "onboarded", profile: toProfile(data) };
  throw new Error(`Unexpected /api/profiles/me response: ${response.status}`);
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
