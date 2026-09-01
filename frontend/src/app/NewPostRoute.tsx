import { NewPostPage } from '@features/post';
import type { Profile } from '@features/profile';
import { useLoaderData } from 'react-router';

export function NewPostRoute() {
  const { profile } = useLoaderData() as { profile: Profile };
  return <NewPostPage authorId={profile.userId} profileUsername={profile.username} />;
}
