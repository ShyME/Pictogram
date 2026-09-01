import { useLoaderData } from "react-router";
import type { Profile } from "@features/profile";
import { NewPostPage } from "@features/post";

export function NewPostRoute() {
  const { profile } = useLoaderData() as { profile: Profile };
  return <NewPostPage authorId={profile.userId} profileUsername={profile.username} />;
}
