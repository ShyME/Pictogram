import { redirect } from "react-router";
import { type Profile, fetchMyProfile } from "@features/profile";

/**
 * The one place the "where does this visitor belong" fork lives: a single
 * `GET /api/profiles/me` sorts every visitor into no session (`/login`), a
 * session but no profile (`/onboarding`), or onboarded (stays). Route loaders
 * pick the rule they need; none of them re-derive it.
 *
 * Each gate throws `redirect(...)` on the reject paths so the pass path can
 * return a plain value. React Router honours a thrown redirect exactly like a
 * returned one.
 */

/** The visitor must be onboarded; hands their profile back. */
export async function requireOnboarded(): Promise<Profile> {
  const me = await fetchMyProfile();
  if (me.status === "unauthenticated") throw redirect("/login");
  if (me.status === "not-onboarded") throw redirect("/onboarding");
  return me.profile;
}

/** The visitor must not be signed in (the login route). */
export async function requireAnonymous(): Promise<null> {
  const me = await fetchMyProfile();
  if (me.status === "onboarded") throw redirect("/");
  if (me.status === "not-onboarded") throw redirect("/onboarding");
  // A loader may not resolve `undefined` in React Router; `null` is "no data".
  return null;
}

/**
 * The onboarding route's asymmetric case: a visitor mid-signup (session, no
 * profile) is exactly who belongs here, but a visitor with no session is sent
 * to `/login` and an onboarded one to the feed.
 */
export async function requireOnboardedOrAnon(): Promise<null> {
  const me = await fetchMyProfile();
  if (me.status === "unauthenticated") throw redirect("/login");
  if (me.status === "onboarded") throw redirect("/");
  return null;
}
