import { useSignOut } from '@features/auth';
import { AppNav } from '@shared';
import { Outlet, useLoaderData } from 'react-router';
import type { viewerLoader } from './guards';

// Chrome for screens an anonymous visitor may also see (the public profile page): the same
// AppNav, in its logged-out state when there is no viewer.
export function PublicLayout() {
  const { viewer } = useLoaderData<typeof viewerLoader>();
  const { signOut, signingOut } = useSignOut();

  return (
    <div className="min-h-dvh bg-canvas">
      <AppNav
        viewer={viewer ? { username: viewer.username } : null}
        onSignOut={signOut}
        signingOut={signingOut}
      />
      <Outlet />
    </div>
  );
}
