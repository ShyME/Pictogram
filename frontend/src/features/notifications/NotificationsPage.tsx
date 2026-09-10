import { Button, EmptyState, SessionExpiredError, Spinner } from '@shared';
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Bell } from 'lucide-react';
import { type ReactNode, useEffect } from 'react';
import { Navigate } from 'react-router';
import { notificationKey } from './notification';
import { NotificationRow } from './NotificationRow';
import { fetchNotificationsPage, markAllNotificationsRead } from './notificationsApi';
import { notificationsKey, unreadCountKey } from './queryKeys';

function Shell({ children }: { children: ReactNode }) {
  return (
    <main className="mx-auto min-h-dvh max-w-xl px-4 py-8">
      <h1 className="pb-4 text-lg font-semibold text-foreground">Notifications</h1>
      {children}
    </main>
  );
}

export function NotificationsPage() {
  const queryClient = useQueryClient();

  const feed = useInfiniteQuery({
    queryKey: notificationsKey(),
    queryFn: ({ pageParam }) => fetchNotificationsPage(pageParam),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (page) => page.nextCursor ?? undefined,
    retry: (count, error) => !(error instanceof SessionExpiredError) && count < 1,
  });

  // Opening the screen marks everything read and clears the bell. The list keeps its
  // unread styling until the next load — a fresh fetch here would make rows flicker.
  // Wait for the first page before marking read so this visit shows the unread rows
  // even when mark-read would otherwise win the race against the list fetch.
  const { mutate: markRead } = useMutation({
    mutationFn: markAllNotificationsRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: unreadCountKey() }),
  });
  const { isSuccess } = feed;
  useEffect(() => {
    if (isSuccess) markRead();
  }, [isSuccess, markRead]);

  if (feed.isPending) {
    return (
      <Shell>
        <div className="flex justify-center py-10">
          <Spinner label="Loading your notifications" />
        </div>
      </Shell>
    );
  }

  if (feed.isError) {
    if (feed.error instanceof SessionExpiredError) return <Navigate to="/login" replace />;
    return (
      <Shell>
        <p className="px-4 py-2 text-sm text-danger-text">
          We couldn&rsquo;t load your notifications. Try again in a moment.
        </p>
      </Shell>
    );
  }

  const cards = feed.data.pages.flatMap((page) => page.cards);

  if (cards.length === 0) {
    return (
      <Shell>
        <div className="rounded-card border border-dashed border-border-strong bg-surface">
          <EmptyState
            icon={Bell}
            title="Nothing yet"
            description="Likes, comments and new followers will show up here."
          />
        </div>
      </Shell>
    );
  }

  return (
    <Shell>
      <ul
        aria-label="Notifications"
        className="divide-y divide-border overflow-hidden rounded-card border border-border bg-surface"
      >
        {cards.map((card) => (
          <NotificationRow key={notificationKey(card)} card={card} />
        ))}
      </ul>

      {feed.hasNextPage && (
        <div className="mt-6 text-center">
          <Button
            variant="secondary"
            onClick={() => void feed.fetchNextPage()}
            loading={feed.isFetchingNextPage}
            disabled={feed.isFetchingNextPage}
          >
            {feed.isFetchingNextPage ? 'Loading…' : 'Load more'}
          </Button>
        </div>
      )}
    </Shell>
  );
}
