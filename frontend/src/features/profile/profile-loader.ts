import type { LoaderFunctionArgs } from "react-router";
import { fetchMyProfile, fetchProfileByUsername } from "./profile-api";
import type { Profile } from "./profile";

export type ProfilePageData =
  | { status: "found"; profile: Profile; isOwnProfile: boolean; viewerCanFollow: boolean }
  | { status: "not-found"; username: string };

/**
 * Loads the `/u/<username>` page. The profile itself is public, so this works signed out;
 * the extra `GET /api/profiles/me` decides whether the viewer sees the edit affordance
 * (their own profile), an interactive follow button (a signed-in someone else), or a
 * sign-in prompt in its place (a signed-out visitor — following needs an account). That
 * check is cosmetic, so if it fails for any reason the page still renders — as a
 * signed-out visitor's view.
 */
export async function profileLoader({ params }: LoaderFunctionArgs): Promise<ProfilePageData> {
  const username = params.username ?? "";
  const [lookup, me] = await Promise.all([
    fetchProfileByUsername(username),
    fetchMyProfile().catch(() => ({ status: "unauthenticated" }) as const),
  ]);

  if (lookup.status === "not-found") return { status: "not-found", username };

  const isOwnProfile =
    me.status === "onboarded" && me.profile.username === lookup.profile.username;
  return {
    status: "found",
    profile: lookup.profile,
    isOwnProfile,
    viewerCanFollow: me.status === "onboarded" && !isOwnProfile,
  };
}
