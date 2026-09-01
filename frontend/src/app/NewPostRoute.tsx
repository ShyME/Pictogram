import { useLoaderData } from "react-router";
import type { Profile } from "@features/profile";
import { NewPostPage } from "@features/post";

/**
 * Bridges the composer to the app: `newPostLoader` guarantees an onboarded profile, and the
 * `post` feature can't reach the `profile` feature for the id/handle it needs after publish.
 */
export function NewPostRoute() {
  const { profile } = useLoaderData() as { profile: Profile };
  return <NewPostPage authorId={profile.userId} profileUsername={profile.username} />;
}
