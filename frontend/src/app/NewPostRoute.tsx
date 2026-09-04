import { NewPostPage } from '@features/post';
import { useOutletContext } from 'react-router';
import type { AppLayoutContext } from './AppLayout';

export function NewPostRoute() {
  const { profile } = useOutletContext<AppLayoutContext>();
  return <NewPostPage authorId={profile.userId} profileUsername={profile.username} />;
}
