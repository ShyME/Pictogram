import { redirect } from "react-router";
import { fetchMyProfile } from "../api/profile-api";
import type { Profile } from "../model/profile";

export type EditProfileData = { profile: Profile };

/**
 * Loads the `/settings/profile` edit form with the caller's current profile to pre-fill it.
 * You can only edit a profile you have: no session → `/login`, a session but no profile →
 * `/onboarding` (the same fork the root guard makes).
 */
export async function editProfileLoader(): Promise<EditProfileData | Response> {
  const me = await fetchMyProfile();
  if (me.status === "unauthenticated") return redirect("/login");
  if (me.status === "not-onboarded") return redirect("/onboarding");
  return { profile: me.profile };
}
