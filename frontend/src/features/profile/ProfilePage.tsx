import type { ReactNode } from 'react';
import { Link, useLoaderData } from 'react-router';
import type { profileLoader } from './profileLoader';

function PageChrome({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-dvh bg-neutral-50">
      <header className="border-b border-neutral-200 bg-white px-4 py-3">
        <Link to="/" className="font-semibold tracking-tight text-neutral-900">
          Pictogram
        </Link>
      </header>
      {children}
    </div>
  );
}

function NotFound({ username }: { username: string }) {
  return (
    <PageChrome>
      <main className="mx-auto max-w-xl px-4 py-16 text-center">
        <h1 className="text-lg font-semibold text-neutral-900">This account doesn&rsquo;t exist</h1>
        <p className="mt-2 text-sm text-neutral-500">
          No one on Pictogram goes by{' '}
          <span className="font-medium text-neutral-700">@{username}</span>. The link may be wrong,
          or they may have changed their username.
        </p>
        <Link to="/" className="mt-6 inline-block text-sm font-medium text-neutral-900 underline">
          Back to Pictogram
        </Link>
      </main>
    </PageChrome>
  );
}

export function ProfilePage({
  renderGrid,
  renderFollowButton,
  renderFollowCounts,
}: {
  renderGrid?: (authorId: string, isOwnProfile: boolean) => ReactNode;
  renderFollowButton?: (followedUserId: string) => ReactNode;
  renderFollowCounts?: (followedUserId: string, handle: string) => ReactNode;
} = {}) {
  const data = useLoaderData<typeof profileLoader>();

  if (data.status === 'not-found') return <NotFound username={data.username} />;

  const { profile, isOwnProfile, viewerCanFollow } = data;
  const followButtonClass =
    'rounded-lg bg-neutral-900 px-3 py-1.5 text-sm font-medium text-white hover:bg-neutral-700 disabled:opacity-50';

  return (
    <PageChrome>
      <main className="mx-auto max-w-2xl px-4 py-8">
        <section className="flex flex-col gap-4 sm:flex-row sm:items-start sm:gap-8">
          <div aria-hidden className="size-20 shrink-0 rounded-full bg-neutral-200 sm:size-28" />
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
              <h1 className="text-xl font-semibold text-neutral-900">
                {profile.displayName ?? `@${profile.username}`}
              </h1>
              {isOwnProfile ? (
                <Link
                  to="/settings/profile"
                  className="rounded-lg border border-neutral-300 px-3 py-1.5 text-sm font-medium text-neutral-700 hover:bg-neutral-50"
                >
                  Edit profile
                </Link>
              ) : viewerCanFollow ? (
                renderFollowButton ? (
                  renderFollowButton(profile.userId)
                ) : (
                  <button type="button" disabled className={followButtonClass}>
                    Follow
                  </button>
                )
              ) : (
                <Link to="/login" className={followButtonClass}>
                  Follow
                </Link>
              )}
            </div>

            {profile.displayName && (
              <p className="mt-1 text-sm text-neutral-500">@{profile.username}</p>
            )}

            {renderFollowCounts ? (
              renderFollowCounts(profile.userId, profile.username)
            ) : (
              <dl className="mt-3 flex gap-6 text-sm text-neutral-700">
                <div className="flex gap-1">
                  <dt className="sr-only">Followers</dt>
                  <dd className="font-semibold">0</dd>
                  <span className="text-neutral-500">followers</span>
                </div>
                <div className="flex gap-1">
                  <dt className="sr-only">Following</dt>
                  <dd className="font-semibold">0</dd>
                  <span className="text-neutral-500">following</span>
                </div>
              </dl>
            )}

            {profile.bio && (
              <p className="mt-3 whitespace-pre-line text-sm text-neutral-800">{profile.bio}</p>
            )}
          </div>
        </section>

        <section aria-label="Posts" className="mt-8 border-t border-neutral-200 pt-6">
          {renderGrid ? (
            renderGrid(profile.userId, isOwnProfile)
          ) : (
            <>
              <div className="grid grid-cols-3 gap-1">
                {Array.from({ length: 9 }, (_, i) => (
                  <div key={i} aria-hidden className="aspect-square rounded-sm bg-neutral-100" />
                ))}
              </div>
              <p className="mt-4 text-center text-sm text-neutral-400">No posts yet</p>
            </>
          )}
        </section>
      </main>
    </PageChrome>
  );
}
