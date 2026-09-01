import { redirect } from "react-router";
import { fetchMyProfile } from "@features/profile";

/**
 * The root-route guard: one `GET /api/profiles/me` decides where a visitor belongs —
 * no token → `/login`, a token but no profile → `/onboarding`, otherwise the app, with
 * the profile handed to the layout.
 */
export async function rootLoader() {
  const me = await fetchMyProfile();
  if (me.status === "unauthenticated") return redirect("/login");
  if (me.status === "not-onboarded") return redirect("/onboarding");
  return { profile: me.profile };
}

export async function loginLoader() {
  const me = await fetchMyProfile();
  if (me.status === "onboarded") return redirect("/");
  if (me.status === "not-onboarded") return redirect("/onboarding");
  return null;
}

export async function onboardingLoader() {
  const me = await fetchMyProfile();
  if (me.status === "unauthenticated") return redirect("/login");
  if (me.status === "onboarded") return redirect("/");
  return null;
}

/** Loads the post composer: you can only post as a profile you have (same fork as the root guard). */
export async function newPostLoader() {
  const me = await fetchMyProfile();
  if (me.status === "unauthenticated") return redirect("/login");
  if (me.status === "not-onboarded") return redirect("/onboarding");
  return { profile: me.profile };
}
