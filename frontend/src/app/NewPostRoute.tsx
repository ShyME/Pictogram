import { NewPostPage } from '@features/post';
import { useLoaderData } from 'react-router';
import type { newPostLoader } from './guards';

export function NewPostRoute() {
  const { profile } = useLoaderData<typeof newPostLoader>();
  return <NewPostPage authorId={profile.userId} profileUsername={profile.username} />;
}
