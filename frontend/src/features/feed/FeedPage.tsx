import { Button, EmptyState, SessionExpiredError, Spinner, composeCards } from '@shared';
import { type QueryClient, useInfiniteQuery, useQueryClient } from '@tanstack/react-query';
import { Images } from 'lucide-react';
import { type ReactNode, useCallback, useEffect, useRef, useState } from 'react';
import { Navigate } from 'react-router';
import type { FeedCard } from './feed';
import { type FeedSource, followingSource } from './feedApi';
import { toFeedCards } from './feedCards';
import { FeedCardView } from './FeedCardView';

function FeedShell({ children }: { children: ReactNode }) {
  return <main className="mx-auto min-h-dvh max-w-xl px-4 py-8">{children}</main>;
}

export function FeedPage({
  source = followingSource,
  toolbar,
  renderLike,
  renderCommentCount,
  preloadLikes,
  preloadComments,
  renderPostDetail,
}: {
  source?: FeedSource;
  toolbar?: ReactNode;
  renderLike?: (postId: string) => ReactNode;
  renderCommentCount?: (postId: string) => ReactNode;
  preloadLikes?: (client: QueryClient, postIds: string[]) => Promise<void>;
  preloadComments?: (client: QueryClient, postIds: string[]) => Promise<void>;
  renderPostDetail?: (
    detail: { postId: string; imageUrl: string; caption: string | null; authorId: string },
    onClose: () => void,
  ) => ReactNode;
} = {}) {
  const queryClient = useQueryClient();
  const [openCard, setOpenCard] = useState<FeedCard | null>(null);
  const feed = useInfiniteQuery({
    queryKey: source.queryKey,
    queryFn: async ({ pageParam }) => {
      const page = await source.fetchPage(pageParam);
      const cards = await composeCards(page.posts, {
        hydrate: toFeedCards,
        idOf: (card) => card.postId,
        prime: (postIds) => [
          preloadLikes?.(queryClient, postIds),
          preloadComments?.(queryClient, postIds),
        ],
      });
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
        {toolbar}
        <div className="flex justify-center py-10">
          <Spinner label={source.loadingLabel} />
        </div>
      </FeedShell>
    );
  }

  if (feed.isError) {
    if (feed.error instanceof SessionExpiredError) return <Navigate to="/login" replace />;
    return (
      <FeedShell>
        {toolbar}
        <p className="text-center text-sm text-danger-text">{source.loadErrorMessage}</p>
      </FeedShell>
    );
  }

  const cards = feed.data.pages.flatMap((page) => page.cards);

  if (cards.length === 0) {
    return (
      <FeedShell>
        {toolbar}
        <div className="rounded-card border border-dashed border-border-strong bg-surface">
          <EmptyState
            icon={Images}
            title={source.emptyTitle}
            description={source.emptyDescription}
          />
        </div>
      </FeedShell>
    );
  }

  return (
    <FeedShell>
      {toolbar}
      <div className="space-y-6">
        {cards.map((card) => (
          <FeedCardView
            key={card.postId}
            card={card}
            renderLike={renderLike}
            renderCommentCount={renderCommentCount}
            onOpenPost={
              renderPostDetail
                ? () => {
                    setOpenCard(card);
                  }
                : undefined
            }
          />
        ))}
      </div>

      {openCard &&
        renderPostDetail?.(
          {
            postId: openCard.postId,
            imageUrl: openCard.imageUrl,
            caption: openCard.caption,
            authorId: openCard.author.userId,
          },
          () => {
            setOpenCard(null);
          },
        )}

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
