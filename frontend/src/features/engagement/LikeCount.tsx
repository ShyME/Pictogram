import { likeLabel } from './engagement';
import { HeartGlyph } from './HeartGlyph';
import { usePostLikes } from './useLikes';

export function LikeCount({ postId }: { postId: string }) {
  const { data } = usePostLikes(postId);
  const count = data?.likeCount ?? 0;

  return (
    <div className="flex items-center gap-2 text-neutral-500">
      <HeartGlyph filled={false} />
      <span className="text-sm">{likeLabel(count)}</span>
    </div>
  );
}
