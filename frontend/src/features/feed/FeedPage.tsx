import { Button, EmptyState, SessionExpiredError, Spinner } from '@shared';
import { type QueryClient, useInfiniteQuery, useQueryClient } from '@tanstack/react-query';
import { Images } from 'lucide-react';
import { type ReactNode, useCallback, useEffect, useRef } from 'react';
import { Navigate } from 'react-router';
import { fetchFeedPage } from './feedApi';
import { toFeedCards } from './feedCards';
import { FeedCardView } from './FeedCardView';
import { feedKey } from './queryKeys';

function FeedShell({ children }: { children: ReactNode }) {
  return <main className="mx-auto min-h-dvh max-w-xl px-4 py-8">{children}</main>;
}

export function FeedPage({
  renderLike,
  preloadLikes,
}: {
  renderLike?: (postId: string) => ReactNode;
  preloadLikes?: (client: QueryClient, postIds: string[]) => Promise<void>;
} = {}) {
  const queryClient = useQueryClient();
  const feed = useInfiniteQuery({
    queryKey: feedKey(),
    queryFn: async ({ pageParam }) => {
      const page = await fetchFeedPage(pageParam);
      const cards = await toFeedCards(page.posts);
      await preloadLikes?.(
        queryClient,
        cards.map((card) => card.postId),
      );
      return { cards, nextCursor: page.nextCursor };
    },
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (page) => page.nextCursor ?? undefined,
    retry: (count, error) => !(error instanceof SessionExpiredError) && count < 1,
  });

  const loadMore = () => {
    if (feed.hasNextPage && !feed.isFetchingNextPage) void feed.fetchNextPage();
  };

  const sentinel = useInfiniteScroll(loadMore);

  if (feed.isPending) {
    return (
      <FeedShell>
        <div className="flex justify-center py-10">
          <Spinner label="Loading your feed" />
        </div>
      </FeedShell>
    );
  }

  if (feed.isError) {
    if (feed.error instanceof SessionExpiredError) return <Navigate to="/login" replace />;
    return (
      <FeedShell>
        <p className="text-center text-sm text-danger-text">
          We couldn&rsquo;t load your feed. Try again in a moment.
        </p>
      </FeedShell>
    );
  }

  const cards = feed.data.pages.flatMap((page) => page.cards);

  if (cards.length === 0) {
    return (
      <FeedShell>
        <div className="rounded-card border border-dashed border-border-strong bg-surface">
          <EmptyState
            icon={Images}
            title="Your feed is quiet"
            description="Find people to follow and their posts will show up here."
          />
        </div>
      </FeedShell>
    );
  }

  return (
    <FeedShell>
      <div className="space-y-6">
        {cards.map((card) => (
          <FeedCardView key={card.postId} card={card} renderLike={renderLike} />
        ))}
      </div>

      <div ref={sentinel} className="h-px" aria-hidden />

      {feed.hasNextPage && (
        <div className="mt-6 text-center">
          <Button
            variant="secondary"
            onClick={loadMore}
            loading={feed.isFetchingNextPage}
            disabled={feed.isFetchingNextPage}
          >
            {feed.isFetchingNextPage ? 'Loading…' : 'Load more'}
          </Button>
        </div>
      )}
    </FeedShell>
  );
}

function useInfiniteScroll(onReachEnd: () => void) {
  const callback = useRef(onReachEnd);
  useEffect(() => {
    callback.current = onReachEnd;
  });
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
