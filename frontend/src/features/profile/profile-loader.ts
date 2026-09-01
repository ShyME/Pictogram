import type { LoaderFunctionArgs } from "react-router";
import { fetchMyProfile, fetchProfileByUsername } from "./profile-api";
import type { Profile } from "./profile";

export type ProfilePageData =
  | { status: "found"; profile: Profile; isOwnProfile: boolean; viewerCanFollow: boolean }
  | { status: "not-found"; username: string };

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
