import { SessionExpiredError } from '@shared';
import { useInfiniteQuery } from '@tanstack/react-query';
import { type ReactNode, useCallback, useRef } from 'react';
import { Navigate } from 'react-router';
import { fetchFeedPage } from './feed-api';
import { toFeedCards } from './feed-cards';
import { FeedCardView } from './FeedCardView';
import { feedKey } from './query-keys';

function FeedShell({ children }: { children: ReactNode }) {
  return <main className="mx-auto min-h-dvh max-w-xl px-4 py-8">{children}</main>;
}

export function FeedPage() {
  const feed = useInfiniteQuery({
    queryKey: feedKey(),
    queryFn: async ({ pageParam }) => {
      const page = await fetchFeedPage(pageParam);
      return { cards: await toFeedCards(page.posts), nextCursor: page.nextCursor };
    },
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (page) => page.nextCursor ?? undefined,
    retry: (count, error) => !(error instanceof SessionExpiredError) && count < 1,
  });

  const loadMore = () => {
    if (feed.hasNextPage && !feed.isFetchingNextPage) feed.fetchNextPage();
  };

  const sentinel = useInfiniteScroll(loadMore);

  if (feed.isPending) {
    return (
      <FeedShell>
        <p className="text-center text-sm text-neutral-400">Loading your feed…</p>
      </FeedShell>
    );
  }

  if (feed.isError) {
    if (feed.error instanceof SessionExpiredError) return <Navigate to="/login" replace />;
    return (
      <FeedShell>
        <p className="text-center text-sm text-red-600">
          We couldn&rsquo;t load your feed. Try again in a moment.
        </p>
      </FeedShell>
    );
  }

  const cards = feed.data.pages.flatMap((page) => page.cards);

  if (cards.length === 0) {
    return (
      <FeedShell>
        <div className="rounded-2xl border border-dashed border-neutral-300 bg-white p-10 text-center">
          <h2 className="text-lg font-semibold text-neutral-900">Your feed is quiet</h2>
          <p className="mx-auto mt-2 max-w-xs text-sm text-neutral-500">
            Find people to follow and their posts will show up here.
          </p>
        </div>
      </FeedShell>
    );
  }

  return (
    <FeedShell>
      <div className="space-y-6">
        {cards.map((card) => (
          <FeedCardView key={card.postId} card={card} />
        ))}
      </div>

      <div ref={sentinel} className="h-px" aria-hidden />

      {feed.hasNextPage && (
        <div className="mt-6 text-center">
          <button
            type="button"
            onClick={loadMore}
            disabled={feed.isFetchingNextPage}
            className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 disabled:opacity-50"
          >
            {feed.isFetchingNextPage ? 'Loading…' : 'Load more'}
          </button>
        </div>
      )}
    </FeedShell>
  );
}

function useInfiniteScroll(onReachEnd: () => void) {
  const callback = useRef(onReachEnd);
  callback.current = onReachEnd;
  const observer = useRef<IntersectionObserver | null>(null);

  // A callback ref, so the observer re-attaches whenever the sentinel mounts —
  // it only renders once the first page has loaded.
  return useCallback((node: HTMLDivElement | null) => {
    observer.current?.disconnect();
    if (!node || typeof IntersectionObserver === 'undefined') return;

    observer.current = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) callback.current();
    });
    observer.current.observe(node);
  }, []);
}
