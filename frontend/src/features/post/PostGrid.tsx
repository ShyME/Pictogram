import {
  type QueryClient,
  useInfiniteQuery,
  useMutation,
  useQueryClient,
} from '@tanstack/react-query';
import { type ReactNode, useId, useState } from 'react';
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

  const grid = useInfiniteQuery({
    queryKey: postsByAuthorKey(authorId),
    queryFn: async ({ pageParam }) => {
      const page = await fetchPostsByAuthor(authorId, pageParam);
      await preloadLikes?.(
        queryClient,
        page.posts.map((post) => post.postId),
      );
      return page;
    },
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (page) => page.nextCursor ?? undefined,
  });

  const remove = useMutation({
    mutationFn: (post: Post) => deletePost(post.postId),
    onSuccess: async () => {
      setPendingDelete(null);
      await queryClient.invalidateQueries({ queryKey: postsByAuthorKey(authorId) });
    },
  });

  if (grid.isPending) {
    return <p className="py-10 text-center text-sm text-neutral-400">Loading posts…</p>;
  }

  if (grid.isError) {
    return (
      <p className="py-10 text-center text-sm text-red-600">
        We couldn&rsquo;t load these posts. Try again in a moment.
      </p>
    );
  }

  const posts = grid.data.pages.flatMap((page) => page.posts);

  if (posts.length === 0) {
    return <p className="py-10 text-center text-sm text-neutral-400">No posts yet</p>;
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
              <button
                type="button"
                onClick={() => {
                  remove.reset();
                  setPendingDelete(post);
                }}
                className="absolute right-1 top-1 rounded-md bg-black/60 px-2 py-1 text-xs font-medium text-white opacity-0 transition group-hover:opacity-100 focus-visible:opacity-100"
              >
                Delete
              </button>
            )}
          </li>
        ))}
      </ul>

      {grid.hasNextPage && (
        <div className="mt-4 text-center">
          <button
            type="button"
            onClick={() => void grid.fetchNextPage()}
            disabled={grid.isFetchingNextPage}
            className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 disabled:opacity-50"
          >
            {grid.isFetchingNextPage ? 'Loading…' : 'Load more'}
          </button>
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
          onCancel={() => {
            setPendingDelete(null);
          }}
        />
      )}
    </>
  );
}

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
  return (
    <div className="fixed inset-0 z-10 flex items-center justify-center bg-black/40 p-4">
      <div
        role="alertdialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="w-full max-w-sm rounded-xl bg-white p-5 shadow-xl"
      >
        <h2 id={titleId} className="text-base font-semibold text-neutral-900">
          Delete this post?
        </h2>
        <p className="mt-1 text-sm text-neutral-500">This can&rsquo;t be undone.</p>

        {failed && (
          <p role="alert" className="mt-2 text-sm text-red-600">
            That didn&rsquo;t work. Try again in a moment.
          </p>
        )}

        <div className="mt-4 flex justify-end gap-3">
          <button
            type="button"
            onClick={onCancel}
            disabled={deleting}
            className="rounded-lg px-3 py-1.5 text-sm font-medium text-neutral-600 hover:bg-neutral-100 disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={deleting}
            className="rounded-lg bg-red-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
          >
            {deleting ? 'Deleting…' : 'Delete'}
          </button>
        </div>
      </div>
    </div>
  );
}
