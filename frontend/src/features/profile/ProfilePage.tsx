import { Avatar, AvatarFallback, Button } from '@shared';
import type { ReactNode } from 'react';
import { Link, useLoaderData } from 'react-router';
import type { profileLoader } from './profileLoader';

function NotFound({ username }: { username: string }) {
  return (
    <main className="mx-auto max-w-xl px-4 py-16 text-center">
      <h1 className="text-lg font-semibold text-foreground">This account doesn&rsquo;t exist</h1>
      <p className="mt-2 text-sm text-foreground-muted">
        No one on Pictogram goes by <span className="font-medium text-foreground">@{username}</span>
        . The link may be wrong, or they may have changed their username.
      </p>
      <Button variant="link" asChild className="mt-6">
        <Link to="/">Back to Pictogram</Link>
      </Button>
    </main>
  );
}

export function ProfilePage({
  renderGrid,
  renderFollowButton,
  renderFollowCounts,
}: {
  renderGrid?: (
    authorId: string,
    isOwnProfile: boolean,
    isViewerAuthenticated: boolean,
  ) => ReactNode;
  renderFollowButton?: (followedUserId: string) => ReactNode;
  renderFollowCounts?: (followedUserId: string, handle: string) => ReactNode;
} = {}) {
  const data = useLoaderData<typeof profileLoader>();

  if (data.status === 'not-found') return <NotFound username={data.username} />;

  const { profile, isOwnProfile, viewerCanFollow, viewerIsAuthenticated } = data;

  return (
    <main className="mx-auto max-w-2xl px-4 py-8">
      <section className="flex flex-col gap-4 sm:flex-row sm:items-start sm:gap-8">
        <Avatar size="lg" className="size-20 text-xl sm:size-28">
          <AvatarFallback>{profile.username.slice(0, 2).toUpperCase()}</AvatarFallback>
        </Avatar>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
            <h1 className="text-xl font-semibold text-foreground">
              {profile.displayName ?? `@${profile.username}`}
            </h1>
            {isOwnProfile ? (
              <Button variant="secondary" size="sm" asChild>
                <Link to="/settings/profile">Edit profile</Link>
              </Button>
            ) : viewerCanFollow ? (
              renderFollowButton ? (
                renderFollowButton(profile.userId)
              ) : (
                <Button size="sm" disabled>
                  Follow
                </Button>
              )
            ) : (
              <Button size="sm" asChild>
                <Link to="/login">Follow</Link>
              </Button>
            )}
          </div>

          {profile.displayName && (
            <p className="mt-1 text-sm text-foreground-muted">@{profile.username}</p>
          )}

          {renderFollowCounts ? (
            renderFollowCounts(profile.userId, profile.username)
          ) : (
            <dl className="mt-3 flex gap-6 text-sm text-foreground">
              <div className="flex gap-1">
                <dt className="sr-only">Followers</dt>
                <dd className="font-semibold">0</dd>
                <span className="text-foreground-muted">followers</span>
              </div>
              <div className="flex gap-1">
                <dt className="sr-only">Following</dt>
                <dd className="font-semibold">0</dd>
                <span className="text-foreground-muted">following</span>
              </div>
            </dl>
          )}

          {profile.bio && (
            <p className="mt-3 whitespace-pre-line text-sm text-foreground">{profile.bio}</p>
          )}
        </div>
      </section>

      <section aria-label="Posts" className="mt-8 border-t border-border pt-6">
        {renderGrid ? (
          renderGrid(profile.userId, isOwnProfile, viewerIsAuthenticated)
        ) : (
          <>
            <div className="grid grid-cols-3 gap-1">
              {Array.from({ length: 9 }, (_, i) => (
                <div key={i} aria-hidden className="aspect-square rounded-sm bg-surface-muted" />
              ))}
            </div>
            <p className="mt-4 text-center text-sm text-foreground-subtle">No posts yet</p>
          </>
        )}
      </section>
    </main>
  );
}
