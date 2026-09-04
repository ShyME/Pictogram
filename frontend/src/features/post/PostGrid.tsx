import {
  Button,
  Modal,
  ModalContent,
  ModalDescription,
  ModalFooter,
  ModalHeader,
  ModalTitle,
  Spinner,
  toast,
} from '@shared';
import {
  type QueryClient,
  useInfiniteQuery,
  useMutation,
  useQueryClient,
} from '@tanstack/react-query';
import { type ReactNode, useState } from 'react';
import type { Post } from './post';
import { originalUrl, thumbnailUrl } from './post';
import { deletePost, fetchPostsByAuthor } from './postApi';
import { PostDetailDialog } from './PostDetailDialog';
import { postsByAuthorKey } from './queryKeys';

export function PostGrid({
  authorId,
  manageable = false,
  renderLike,
  renderComments,
  preloadLikes,
}: {
  authorId: string;
  manageable?: boolean;
  renderLike?: (postId: string) => ReactNode;
  renderComments?: (postId: string) => ReactNode;
  preloadLikes?: (client: QueryClient, postIds: string[]) => Promise<void>;
}) {
  const queryClient = useQueryClient();
  const [pendingDelete, setPendingDelete] = useState<Post | null>(null);
  const [openPost, setOpenPost] = useState<Post | null>(null);

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
          postId={openPost.postId}
          imageUrl={originalUrl(openPost.mediaId)}
          caption={openPost.caption}
          renderLike={renderLike}
          renderComments={renderComments}
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
  return (
    <Modal
      isOpen
      onOpenChange={(isOpen) => {
        if (!isOpen && !deleting) onCancel();
      }}
    >
      <ModalContent role="alertdialog" className="max-w-sm" closeOnEscape={!deleting}>
        <ModalHeader>
          <ModalTitle>Delete this post?</ModalTitle>
          <ModalDescription>This can&rsquo;t be undone.</ModalDescription>
        </ModalHeader>

        {failed && (
          <p role="alert" className="mt-3 text-sm text-danger-text">
            That didn&rsquo;t work. Try again in a moment.
          </p>
        )}

        <ModalFooter>
          <Button variant="secondary" onClick={onCancel} disabled={deleting}>
            Cancel
          </Button>
          <Button variant="danger" onClick={onConfirm} loading={deleting} disabled={deleting}>
            {deleting ? 'Deleting…' : 'Delete'}
          </Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
}
