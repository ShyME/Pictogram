import { type ReactNode, useEffect, useId } from 'react';
import type { Post } from './post';
import { originalUrl } from './post';

export function PostDetailDialog({
  post,
  renderLike,
  onClose,
}: {
  post: Post;
  renderLike?: (postId: string) => ReactNode;
  onClose: () => void;
}) {
  const titleId = useId();

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
    };
  }, [onClose]);

  return (
    <div
      className="fixed inset-0 z-10 flex items-center justify-center bg-black/60 p-4"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="w-full max-w-lg overflow-hidden rounded-xl bg-white shadow-xl"
        onClick={(event) => {
          event.stopPropagation();
        }}
      >
        <h2 id={titleId} className="sr-only">
          Post
        </h2>
        <img
          src={originalUrl(post.mediaId)}
          alt={post.caption ?? 'A post'}
          className="aspect-square w-full bg-neutral-100 object-cover"
        />
        <div className="flex items-center justify-between gap-2 px-4 pb-2 pt-3 text-neutral-400">
          {renderLike?.(post.postId)}
          <button
            type="button"
            onClick={onClose}
            className="ml-auto rounded-lg px-3 py-1.5 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
          >
            Close
          </button>
        </div>
        {post.caption && <p className="px-4 pb-4 pt-1 text-sm text-neutral-800">{post.caption}</p>}
      </div>
    </div>
  );
}
