import { useInfiniteQuery } from "@tanstack/react-query";
import { fetchPostsByAuthor } from "../api/post-api";
import { postsByAuthorKey } from "../model/query-keys";
import { thumbnailUrl } from "../model/post";

/**
 * The signed-in author's own post grid on their profile page — newest first, "Load more"
 * paging on the keyset cursor (infinite scroll is the feed's concern, #18). A just-published
 * post appears at the top because the composer invalidates this query before navigating here.
 */
export function OwnPostGrid({ authorId }: { authorId: string }) {
  const grid = useInfiniteQuery({
    queryKey: postsByAuthorKey(authorId),
    queryFn: ({ pageParam }) => fetchPostsByAuthor(authorId, pageParam),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (page) => page.nextCursor ?? undefined,
  });

  if (grid.isPending) {
    return <p className="py-10 text-center text-sm text-neutral-400">Loading posts…</p>;
  }

  if (grid.isError) {
    return (
      <p className="py-10 text-center text-sm text-red-600">
        We couldn&rsquo;t load your posts. Try again in a moment.
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
          <li key={post.postId}>
            <img
              src={thumbnailUrl(post.mediaId)}
              alt={post.caption ?? "A post"}
              loading="lazy"
              className="aspect-square w-full rounded-sm object-cover"
            />
          </li>
        ))}
      </ul>
      {grid.hasNextPage && (
        <div className="mt-4 text-center">
          <button
            type="button"
            onClick={() => grid.fetchNextPage()}
            disabled={grid.isFetchingNextPage}
            className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 disabled:opacity-50"
          >
            {grid.isFetchingNextPage ? "Loading…" : "Load more"}
          </button>
        </div>
      )}
    </>
  );
}
