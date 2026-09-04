import { Button, Spinner, toast } from '@shared';
import {
  type QueryClient,
  useInfiniteQuery,
  useMutation,
  useQueryClient,
} from '@tanstack/react-query';
import { type ReactNode, useCallback, useEffect, useId, useState } from 'react';
import type { Post } from './post';
import { thumbnailUrl } from './post';
import { deletePost, fetchPostsByAuthor } from './postApi';
import { PostDetailDialog } from './PostDetailDialog';
import { postsByAuthorKey } from './queryKeys';

export function PostGrid({
  authorId,
  manageable = false,
  renderLike,
  preloadLikes,
}: {
  authorId: string;
  manageable?: boolean;
  renderLike?: (postId: string) => ReactNode;
  preloadLikes?: (client: QueryClient, postIds: string[]) => Promise<void>;
}) {
  const queryClient = useQueryClient();
  const [pendingDelete, setPendingDelete] = useState<Post | null>(null);
  const [openPost, setOpenPost] = useState<Post | null>(null);
  const cancelDelete = useCallback(() => {
    setPendingDelete(null);
  }, []);

  const grid = useInfiniteQuery({
    queryKey: postsByAuthorKey(authorId),
    queryFn: async ({ pageParam }) => {
      const page = await fetchPostsByAuthor(authorId, pageParam);
      try {
        await preloadLikes?.(
          queryClient,
          page.posts.map((post) => post.postId),
        );
      } catch {
        // Best-effort: like state is decoration seeded ahead of the like controls. If the
        // batch read fails for any reason, still show the grid.
      }
      return page;
    },
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (page) => page.nextCursor ?? undefined,
  });

  const remove = useMutation({
    mutationFn: (post: Post) => deletePost(post.postId),
    onSuccess: async () => {
      setPendingDelete(null);
      toast({ variant: 'success', title: 'Post deleted' });
      await queryClient.invalidateQueries({ queryKey: postsByAuthorKey(authorId) });
    },
  });

  if (grid.isPending) {
    return (
      <div className="flex justify-center py-10">
        <Spinner label="Loading posts" />
      </div>
    );
  }

  if (grid.isError) {
    return (
      <p className="py-10 text-center text-sm text-danger-text">
        We couldn&rsquo;t load these posts. Try again in a moment.
      </p>
    );
  }

  const posts = grid.data.pages.flatMap((page) => page.posts);

  if (posts.length === 0) {
    return <p className="py-10 text-center text-sm text-foreground-subtle">No posts yet</p>;
  }

  return (
    <>
      <ul className="grid grid-cols-3 gap-1">
        {posts.map((post) => (
          <li key={post.postId} className="group relative">
            <button
              type="button"
              onClick={() => {
                setOpenPost(post);
              }}
              className="block w-full"
            >
              <img
                src={thumbnailUrl(post.mediaId)}
                alt={post.caption ?? 'A post'}
                loading="lazy"
                className="aspect-square w-full rounded-sm object-cover"
              />
            </button>
            {manageable && (
              <Button
                variant="danger"
                size="sm"
                onClick={() => {
                  remove.reset();
                  setPendingDelete(post);
                }}
                className="absolute right-1 top-1 opacity-0 transition group-hover:opacity-100 focus-visible:opacity-100"
              >
                Delete
              </Button>
            )}
          </li>
        ))}
      </ul>

      {grid.hasNextPage && (
        <div className="mt-4 text-center">
          <Button
            variant="secondary"
            onClick={() => void grid.fetchNextPage()}
            loading={grid.isFetchingNextPage}
            disabled={grid.isFetchingNextPage}
          >
            {grid.isFetchingNextPage ? 'Loading…' : 'Load more'}
          </Button>
        </div>
      )}

      {openPost && (
        <PostDetailDialog
          post={openPost}
          renderLike={renderLike}
          onClose={() => {
            setOpenPost(null);
          }}
        />
      )}

      {manageable && pendingDelete && (
        <ConfirmDelete
          deleting={remove.isPending}
          failed={remove.isError}
          onConfirm={() => {
            remove.mutate(pendingDelete);
          }}
          onCancel={cancelDelete}
        />
      )}
    </>
  );
}

// A hand-rolled modal, not the shared Radix `Dialog`: that pulls in a scroll-lock that
// writes an inline `style` on <body>, which the app's CSP (`style-src 'self'`, ADR-0011)
// blocks. `PostDetailDialog` is hand-rolled for the same reason; #137 unifies them.
function ConfirmDelete({
  deleting,
  failed,
  onConfirm,
  onCancel,
}: {
  deleting: boolean;
  failed: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  const titleId = useId();

  useEffect(() => {
    if (deleting) return;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onCancel();
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
    };
  }, [deleting, onCancel]);

  return (
    <div className="fixed inset-0 z-10 flex items-center justify-center bg-foreground/40 p-4">
      <div
        role="alertdialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="w-full max-w-sm rounded-dialog border border-border bg-surface p-6 shadow-dialog"
      >
        <h2 id={titleId} className="text-lg font-semibold tracking-tight text-foreground">
          Delete this post?
        </h2>
        <p className="mt-1.5 text-sm text-foreground-muted">This can&rsquo;t be undone.</p>

        {failed && (
          <p role="alert" className="mt-3 text-sm text-danger-text">
            That didn&rsquo;t work. Try again in a moment.
          </p>
        )}

        <div className="mt-5 flex justify-end gap-3">
          <Button variant="secondary" onClick={onCancel} disabled={deleting}>
            Cancel
          </Button>
          <Button variant="danger" onClick={onConfirm} loading={deleting} disabled={deleting}>
            {deleting ? 'Deleting…' : 'Delete'}
          </Button>
        </div>
      </div>
    </div>
  );
}
