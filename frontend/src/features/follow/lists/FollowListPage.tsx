import { useInfiniteQuery, useQueryClient } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { Link, useLoaderData } from 'react-router';
import { fetchFollowListPage } from '../followApi';
import { followListKey } from '../queryKeys';
import { seedFollowRelationships } from '../useFollowRelationship';
import { AccountList } from './AccountList';
import type { FollowListData, FollowListMode } from './followList';

function PageChrome({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-dvh bg-neutral-50">
      <header className="border-b border-neutral-200 bg-white px-4 py-3">
        <Link to="/" className="font-semibold tracking-tight text-neutral-900">
          Pictogram
        </Link>
      </header>
      <main className="mx-auto max-w-xl px-4 py-8">{children}</main>
    </div>
  );
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
        <h1 className="text-lg font-semibold text-neutral-900">This account doesn&rsquo;t exist</h1>
        <p className="mt-2 text-sm text-neutral-500">
          No one on Pictogram goes by{' '}
          <span className="font-medium text-neutral-700">@{data.username}</span>.
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
        <h1 className="text-lg font-semibold text-neutral-900">{copy.title(target.username)}</h1>
        <Link
          to={`/u/${target.username}`}
          className="text-sm font-medium text-neutral-900 underline"
        >
          Back to profile
        </Link>
      </div>

      <div className="mt-4">
        {list.isPending ? (
          <p className="py-10 text-center text-sm text-neutral-400">Loading…</p>
        ) : list.isError ? (
          <p className="py-10 text-center text-sm text-red-600">
            We couldn&rsquo;t load this list. Try again in a moment.
          </p>
        ) : accounts.length === 0 ? (
          <p className="py-10 text-center text-sm text-neutral-400">{copy.empty}</p>
        ) : (
          <>
            <AccountList accounts={accounts} viewerId={viewerId} />
            {list.hasNextPage && (
              <div className="mt-4 text-center">
                <button
                  type="button"
                  onClick={() => void list.fetchNextPage()}
                  disabled={list.isFetchingNextPage}
                  className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 disabled:opacity-50"
                >
                  {list.isFetchingNextPage ? 'Loading…' : 'Load more'}
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </PageChrome>
  );
}
