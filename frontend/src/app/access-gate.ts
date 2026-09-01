import { redirect } from "react-router";
import { type Profile, fetchMyProfile } from "@features/profile";

export async function requireOnboarded(): Promise<Profile> {
  const me = await fetchMyProfile();
  if (me.status === "unauthenticated") throw redirect("/login");
  if (me.status === "not-onboarded") throw redirect("/onboarding");
  return me.profile;
}

export async function requireAnonymous(): Promise<null> {
  const me = await fetchMyProfile();
  if (me.status === "onboarded") throw redirect("/");
  if (me.status === "not-onboarded") throw redirect("/onboarding");
  return null;
}

export async function requireOnboardedOrAnon(): Promise<null> {
  const me = await fetchMyProfile();
  if (me.status === "unauthenticated") throw redirect("/login");
  if (me.status === "onboarded") throw redirect("/");
  return null;
}
