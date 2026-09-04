import { Button, Spinner } from '@shared';
import { useInfiniteQuery, useQueryClient } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { Link, useLoaderData } from 'react-router';
import { fetchFollowListPage } from '../followApi';
import { followListKey } from '../queryKeys';
import { seedFollowRelationships } from '../useFollowRelationship';
import { AccountList } from './AccountList';
import type { FollowListData, FollowListMode } from './followList';

function PageChrome({ children }: { children: ReactNode }) {
  return <main className="mx-auto max-w-xl px-4 py-8">{children}</main>;
}

const COPY: Record<FollowListMode, { title: (handle: string) => string; empty: string }> = {
  followers: {
    title: (handle) => `People who follow @${handle}`,
    empty: 'No followers yet',
  },
  following: {
    title: (handle) => `Accounts @${handle} follows`,
    empty: 'Not following anyone yet',
  },
};

export function FollowListPage({ mode }: { mode: FollowListMode }) {
  const data = useLoaderData<FollowListData>();

  if (data.status === 'not-found') {
    return (
      <PageChrome>
        <h1 className="text-lg font-semibold text-foreground">This account doesn&rsquo;t exist</h1>
        <p className="mt-2 break-words text-sm text-foreground-muted">
          No one on Pictogram goes by{' '}
          <span className="font-medium text-foreground">@{data.username}</span>.
        </p>
      </PageChrome>
    );
  }

  return <Loaded mode={mode} target={data.target} viewerId={data.viewerId} />;
}

function Loaded({
  mode,
  target,
  viewerId,
}: {
  mode: FollowListMode;
  target: { userId: string; username: string };
  viewerId: string;
}) {
  const copy = COPY[mode];
  const queryClient = useQueryClient();
  const list = useInfiniteQuery({
    queryKey: followListKey(mode, target.userId),
    queryFn: async ({ pageParam }) => {
      const page = await fetchFollowListPage(mode, target.userId, pageParam);
      seedFollowRelationships(queryClient, page.relationships);
      return page;
    },
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (page) => page.nextCursor ?? undefined,
  });

  const accounts = list.data?.pages.flatMap((page) => page.accounts) ?? [];

  return (
    <PageChrome>
      <div className="flex items-baseline justify-between gap-4">
        <h1 className="min-w-0 break-words text-lg font-semibold text-foreground">
          {copy.title(target.username)}
        </h1>
        <Button variant="link" size="sm" asChild className="shrink-0">
          <Link to={`/u/${target.username}`}>Back to profile</Link>
        </Button>
      </div>

      <div className="mt-4">
        {list.isPending ? (
          <div className="flex justify-center py-10">
            <Spinner label="Loading" />
          </div>
        ) : list.isError ? (
          <p className="py-10 text-center text-sm text-danger-text">
            We couldn&rsquo;t load this list. Try again in a moment.
          </p>
        ) : accounts.length === 0 ? (
          <p className="py-10 text-center text-sm text-foreground-subtle">{copy.empty}</p>
        ) : (
          <>
            <AccountList accounts={accounts} viewerId={viewerId} />
            {list.hasNextPage && (
              <div className="mt-4 text-center">
                <Button
                  variant="secondary"
                  onClick={() => void list.fetchNextPage()}
                  loading={list.isFetchingNextPage}
                  disabled={list.isFetchingNextPage}
                >
                  {list.isFetchingNextPage ? 'Loading…' : 'Load more'}
                </Button>
              </div>
            )}
          </>
        )}
      </div>
    </PageChrome>
  );
}
