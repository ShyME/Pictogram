import type { LoaderFunctionArgs } from "react-router";
import { fetchProfileByUsername } from "@features/profile";
import type { FollowListData } from "@features/follow";
import { requireOnboarded } from "./access-gate";

/**
 * Loads a `/u/:username/{followers,following}` screen (#57). The list endpoints are
 * authenticated, so this first runs the access gate — a signed-out visitor who followed a
 * count link is redirected to `/login`, a session-without-profile to `/onboarding`. With a
 * viewer in hand it resolves the target profile by username (public), echoing the username
 * back on a miss so the page can show a clear "no such account".
 */
export async function followListLoader({
  params,
}: LoaderFunctionArgs): Promise<FollowListData | Response> {
  const username = params.username ?? "";
  try {
    const [viewer, lookup] = await Promise.all([
      requireOnboarded(),
      fetchProfileByUsername(username),
    ]);

    if (lookup.status === "not-found") return { status: "not-found", username };

    return {
      status: "found",
      target: { userId: lookup.profile.userId, username: lookup.profile.username },
      viewerId: viewer.userId,
    };
  } catch (thrown) {
    if (thrown instanceof Response) return thrown;
    throw thrown;
  }
}
