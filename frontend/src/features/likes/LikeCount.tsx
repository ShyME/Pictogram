import { Heart } from 'lucide-react';
import { likeLabel } from './likes';
import { usePostLikes } from './useLikes';

export function LikeCount({ postId }: { postId: string }) {
  const { data } = usePostLikes(postId);
  const count = data?.likeCount ?? 0;

  return (
    <div className="flex items-center gap-2 text-foreground-muted">
      <Heart className="size-6" aria-hidden="true" />
      <span className="text-sm">{likeLabel(count)}</span>
    </div>
  );
}
