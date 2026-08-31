import type { LoaderFunctionArgs } from "react-router";
import { fetchMyProfile, fetchProfileByUsername } from "../api/profile-api";
import type { Profile } from "../model/profile";

export type ProfilePageData =
  | { status: "found"; profile: Profile; isOwnProfile: boolean }
  | { status: "not-found"; username: string };

/**
 * Loads the `/u/<username>` page. The profile itself is public, so this works signed out;
 * the extra `GET /api/profiles/me` only decides whether the viewer is looking at their own
 * profile (edit affordance) or someone else's (follow affordance). That check is cosmetic,
 * so if it fails for any reason the page still renders — as someone else's.
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
  return { status: "found", profile: lookup.profile, isOwnProfile };
}
